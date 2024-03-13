package co.il.mh.event.source.jackson

import co.il.mh.event.source.utils.Encryption
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class SecretSerializer(
    private val objectMapper: ObjectMapper,
    private val secret: String
) : StdSerializer<Any>(Any::class.java) {
    override fun serialize(value: Any?, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        value?.let {
            jsonGenerator.writeString(
                Encryption.aesEncryptAsBase64String(
                    keyBytes = secret.toByteArray(),
                    data = objectMapper.writeValueAsBytes(it)
                )
            )
        }
    }
}