package com.thanhnb.hocmoingay.core.sync

import com.thanhnb.hocmoingay.core.db.LearnerRow
import com.thanhnb.hocmoingay.core.net.SyncJson
import com.thanhnb.hocmoingay.core.net.parseTs
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface Remote {
    suspend fun upsert(table: String, onConflict: String, rows: List<JsonObject>)
    /** Các hàng có synced_at >= since, tăng dần theo synced_at, tối đa [max] hàng. */
    suspend fun pullSince(table: String, since: String, max: Int): List<JsonObject>
}

interface CursorStore {
    suspend fun get(table: String): String?
    suspend fun put(table: String, cursor: String)
}

interface TableSync {
    val name: String
    suspend fun push(remote: Remote) {}
    suspend fun merge(rows: List<JsonObject>)
}

/** Bảng người học: đẩy dirty, gộp LWW. */
class LearnerTable<E : LearnerRow>(
    override val name: String,
    private val onConflict: String,
    private val serializer: KSerializer<E>,
    private val key: (E) -> String,
    private val dirty: suspend () -> List<E>,
    private val byKeys: suspend (List<String>) -> List<E>,
    private val save: suspend (List<E>) -> Unit,
    private val clean: suspend (String, Long) -> Unit,
) : TableSync {
    override suspend fun push(remote: Remote) {
        for (chunk in dirty().chunked(200)) {
            remote.upsert(name, onConflict, chunk.map { SyncJson.encodeToJsonElement(serializer, it).jsonObject })
            // trigger keep_newer lo xung đột phía server; local chỉ bỏ dirty đúng bản vừa đẩy
            chunk.forEach { clean(key(it), it.updatedAt) }
        }
    }

    override suspend fun merge(rows: List<JsonObject>) {
        val incoming = rows.map { SyncJson.decodeFromJsonElement(serializer, it) }
        val local = byKeys(incoming.map(key)).associateBy(key)
        val keep = incoming.mapNotNull { pickRemote(local[key(it)], it) }
        if (keep.isNotEmpty()) save(keep)
    }
}

/** Bảng giáo trình: chỉ kéo, server luôn thắng; hàng xoá mềm thì xoá hẳn ở local. */
class CurriculumTable<E>(
    override val name: String,
    private val serializer: KSerializer<E>,
    private val key: (E) -> String,
    private val isDeleted: (E) -> Boolean,
    private val save: suspend (List<E>) -> Unit,
    private val delete: suspend (List<String>) -> Unit,
) : TableSync {
    override suspend fun merge(rows: List<JsonObject>) {
        val (gone, live) = rows.map { SyncJson.decodeFromJsonElement(serializer, it) }.partition(isDeleted)
        if (live.isNotEmpty()) save(live)
        if (gone.isNotEmpty()) delete(gone.map(key))
    }
}

class SyncEngine(
    private val remote: Remote,
    private val cursors: CursorStore,
    private val tables: List<TableSync>,
    private val pageSize: Int = 500, // ≤ 999 biến SQLite của byKeys
) {
    private val lock = Mutex() // SyncWorker định kỳ, SyncWorker một lần và realtime không chạy chồng nhau

    /** spec §7.3: đẩy dirty → kéo theo synced_at → gộp. */
    suspend fun syncAll() = lock.withLock {
        // Một hàng bị server từ chối mãi (CHECK, RLS…) không được chặn việc kéo của mọi bảng;
        // vẫn ném lỗi ở cuối để WorkManager thử lại, hàng đó giữ dirty.
        var error: Exception? = null
        for (t in tables) {
            try {
                t.push(remote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error?.addSuppressed(e) ?: run { error = e }
            }
        }
        tables.forEach { pull(it) }
        error?.let { throw it }
    }

    /** Sự kiện realtime đi qua đúng hàm gộp của lượt kéo. */
    suspend fun mergeRemote(table: String, row: JsonObject) = lock.withLock {
        tables.first { it.name == table }.merge(listOf(row))
    }

    private suspend fun pull(t: TableSync) {
        val saved = cursors.get(t.name)?.let(::parseTs)
        var best = saved
        // Lùi 5s: synced_at = clock_timestamp() lấy TRƯỚC commit, nên giao dịch commit chậm có mốc nhỏ hơn cursor đã lưu.
        // Gộp idempotent nên kéo trùng không hại gì.
        var since = saved?.minusSeconds(REWIND_SECONDS) ?: Instant.EPOCH
        while (true) {
            val page = remote.pullSince(t.name, since.toString(), pageSize)
            if (page.isEmpty()) break
            t.merge(page)
            val last = parseTs(page.last()["synced_at"]!!.jsonPrimitive.content)
            if (best == null || last > best) {
                best = last
                cursors.put(t.name, last.toString())
            }
            // Trang đầy: trang sau lấy từ `last` (gte), vì nhiều hàng có thể trùng µs ở mép trang.
            // ponytail: nếu > pageSize hàng cùng một mốc thì phần còn lại đợi lượt sau; cần thì phân trang thêm theo khoá chính.
            if (page.size < pageSize || last == since) break
            since = last
        }
        // Đánh dấu "đã kéo ít nhất một lần" kể cả khi server chưa có hàng nào (SettingsRepo dựa vào đây)
        if (best == null) cursors.put(t.name, Instant.EPOCH.toString())
    }

    private companion object {
        const val REWIND_SECONDS = 5L
    }
}
