package co.il.mh.event.source.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class JsonEntityDeserializer(private val objectMapper: ObjectMapper) :
    StdDeserializer<JsonEntity<*>>(JsonEntity::class.java) {
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext?): JsonEntity<Any> {
        var classString: String? = null
        var dataString: String? = null

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = jsonParser.currentName
            jsonParser.nextToken()

            when (fieldName) {
                "clazz" -> classString = jsonParser.text
                "data" -> dataString = jsonParser.text
            }
        }

        val clazz = Class.forName(classString!!) as Class<Any>
        val data = objectMapper.readValue(dataString, clazz)

        return JsonEntity(clazz = clazz, data = data)
    }
}