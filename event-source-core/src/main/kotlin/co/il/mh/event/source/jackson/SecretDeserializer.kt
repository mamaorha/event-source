package co.il.mh.event.source.jackson

import co.il.mh.event.source.utils.Encryption
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class SecretDeserializer(
    private val objectMapper: ObjectMapper,
    private val secret: String,
    private val type: JavaType
) : StdDeserializer<Any>(Any::class.java) {
    override fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext): Any? {
        if (jsonParser == null) {
            return null
        }

        return objectMapper.readValue(
            Encryption.aesDecryptAsBase64String(
                keyBytes = secret.toByteArray(),
                base64Data = jsonParser.text
            ), type
        )
    }
}