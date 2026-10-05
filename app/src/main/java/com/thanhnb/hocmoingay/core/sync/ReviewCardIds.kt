package com.thanhnb.hocmoingay.core.sync

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * id của review_cards là uuidv5 tất định theo (user_id, ref). Nhờ vậy hai máy, hoặc cài lại app, sinh cùng một id cho cùng một thẻ,
 * nên không bao giờ đụng unique(user_id, ref) (23505) và làm SyncWorker kẹt (Kết quả a1).
 */
object ReviewCardIds {
    // uuid5(NAMESPACE_URL, "https://hoc-api.120203.xyz/review_cards")
    private val NS = UUID.fromString("51507eb1-79c6-5f84-ae4b-6e766c6edd01")

    fun of(userId: String, ref: String): String = uuid5(NS, "$userId:$ref").toString()

    private fun uuid5(ns: UUID, name: String): UUID {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(ByteBuffer.allocate(16).putLong(ns.mostSignificantBits).putLong(ns.leastSignificantBits).array())
        val h = md.digest(name.toByteArray(Charsets.UTF_8))
        h[6] = ((h[6].toInt() and 0x0f) or 0x50).toByte()
        h[8] = ((h[8].toInt() and 0x3f) or 0x80).toByte()
        val bb = ByteBuffer.wrap(h, 0, 16)
        return UUID(bb.long, bb.long)
    }
}
