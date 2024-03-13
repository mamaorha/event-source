package co.il.mh.event.source.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

object SecretModule {
    fun build(objectMapper: ObjectMapper): SimpleModule {
        val module = object : SimpleModule("SecretModule") {
            override fun setupModule(context: SetupContext) {
                super.setupModule(context)
                context.addBeanSerializerModifier(SecretSerializerModifier(objectMapper = objectMapper))
                context.addBeanDeserializerModifier(SecretDeserializerModifier(objectMapper = objectMapper))
            }
        }

        return module
    }
}