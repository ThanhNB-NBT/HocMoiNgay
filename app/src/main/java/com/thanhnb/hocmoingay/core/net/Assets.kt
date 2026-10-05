package com.thanhnb.hocmoingay.core.net

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ảnh trong bài: `assets/<khoá>/x` ↔ bucket riêng tư `content`, đường dẫn `<khoá>/x`. Cache trên đĩa theo đường dẫn.
 * ponytail: không bao giờ làm mới cache; khi phải sửa ảnh cũ thì đổi tên file asset.
 */
class Assets(private val dir: File, private val download: suspend (String) -> ByteArray) {
    suspend fun bytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val key = path.removePrefix("assets/")
        if (".." in key) return@withContext null
        val f = File(dir, key.replace('/', '_'))
        if (f.exists()) return@withContext f.readBytes()
        try {
            download(key).also { dir.mkdirs(); f.writeBytes(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Assets", "tải $key lỗi", e); null
        }
    }
}
