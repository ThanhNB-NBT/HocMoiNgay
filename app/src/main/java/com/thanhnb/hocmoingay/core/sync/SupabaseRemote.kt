package com.thanhnb.hocmoingay.core.sync

import com.thanhnb.hocmoingay.core.db.SyncStateDao
import com.thanhnb.hocmoingay.core.db.SyncStateEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject

class SupabaseRemote(private val pg: Postgrest) : Remote {
    override suspend fun upsert(table: String, onConflict: String, rows: List<JsonObject>) {
        pg.from(table).upsert(rows) { this.onConflict = onConflict }
    }

    override suspend fun pullSince(table: String, since: String, max: Int): List<JsonObject> =
        pg.from(table).select {
            filter { gte("synced_at", since) }
            order("synced_at", Order.ASCENDING)
            limit(max.toLong())
        }.decodeList<JsonObject>()
}

class RoomCursors(private val dao: SyncStateDao) : CursorStore {
    override suspend fun get(table: String) = dao.cursor(table)
    override suspend fun put(table: String, cursor: String) = dao.put(SyncStateEntity(table, cursor))
}
