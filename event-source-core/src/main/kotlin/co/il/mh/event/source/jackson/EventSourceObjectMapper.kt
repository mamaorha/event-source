package co.il.mh.event.source.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object EventSourceObjectMapper {
    val objectMapper by lazy {
        val result = ObjectMapper().registerKotlinModule()
        result.registerModule(SecretModule.build(objectMapper = result))
        result.registerModule(SimpleModule().apply {
            this.addSerializer(JsonEntitySerializer(objectMapper = result))
            this.addDeserializer(JsonEntity::class.java, JsonEntityDeserializer(objectMapper = result))
        })
        result
    }
}