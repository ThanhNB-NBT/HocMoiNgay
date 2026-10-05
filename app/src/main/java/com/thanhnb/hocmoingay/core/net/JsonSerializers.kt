package com.thanhnb.hocmoingay.core.net

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

val SyncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true // deleted=false, score=null… phải gửi đủ để upsert ghi đè
}

/** timestamptz của PostgREST/realtime: "2026-10-05T02:37:12.123456+00:00" hoặc "...Z". */
fun parseTs(s: String): Instant = OffsetDateTime.parse(s).toInstant()

/** Long epoch-millis ⇄ timestamptz. App ghi millis nên đi một vòng vẫn giữ nguyên; mốc do server sinh bị cắt về millis. */
object InstantMillis : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("InstantMillis", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeString(Instant.ofEpochMilli(value).toString())
    override fun deserialize(decoder: Decoder): Long = parseTs(decoder.decodeString()).toEpochMilli()
}

/** Chuỗi JSON trong Room ⇄ jsonb thật (object/array) trên server. */
object JsonText : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("JsonText", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) =
        (encoder as JsonEncoder).encodeJsonElement(Json.parseToJsonElement(value))
    override fun deserialize(decoder: Decoder): String = (decoder as JsonDecoder).decodeJsonElement().toString()
}
