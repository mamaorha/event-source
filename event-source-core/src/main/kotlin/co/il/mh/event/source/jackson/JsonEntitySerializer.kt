package co.il.mh.event.source.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class JsonEntitySerializer(private val objectMapper: ObjectMapper) :
    StdSerializer<JsonEntity<*>>(JsonEntity::class.java) {
    override fun serialize(value: JsonEntity<*>?, jsonGenerator: JsonGenerator, provider: SerializerProvider?) {
        value?.let {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("clazz", it.clazz.name)
            jsonGenerator.writeStringField("data", objectMapper.writeValueAsString(it.data))
            jsonGenerator.writeEndObject()
        }
    }
}