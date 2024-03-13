package co.il.mh.event.source.jackson

import co.il.mh.event.source.annotations.Secret
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator


class SecretDeserializerModifier(private val objectMapper: ObjectMapper) : BeanDeserializerModifier() {
    override fun updateBuilder(
        config: DeserializationConfig?,
        beanDescription: BeanDescription?,
        builder: BeanDeserializerBuilder
    ): BeanDeserializerBuilder {
        when (val valueInstantiator = builder.valueInstantiator) {
            is StdValueInstantiator -> {
                if (builder.valueInstantiator.canCreateFromObjectWith()) {
                    modifySecertProperties(builder, config, valueInstantiator)
                }
            }
        }

        return super.updateBuilder(config, beanDescription, builder)
    }

    private fun modifySecertProperties(
        builder: BeanDeserializerBuilder,
        config: DeserializationConfig?,
        valueInstantiator: StdValueInstantiator
    ) {
        val instantiatorProperties = builder.valueInstantiator.getFromObjectArguments(config)

        if (instantiatorProperties.isNotEmpty()) {
            var haveSecret = false

            val modifiedProperties = instantiatorProperties.map { e: SettableBeanProperty ->
                e.getAnnotation(Secret::class.java)?.let { secretAnnotation ->
                    haveSecret = true

                    val secret = requireNotNull(System.getProperty(secretAnnotation.property)) {
                        "couldn't find secret by property: ${secretAnnotation.property}"
                    }

                    val secretDeserializer = SecretDeserializer(
                        objectMapper = objectMapper,
                        secret = secret,
                        type = e.type
                    )

                    e.withValueDeserializer(secretDeserializer)
                } ?: e
            }.toTypedArray()

            if (haveSecret) {
                valueInstantiator.configureFromObjectSettings(
                    valueInstantiator.defaultCreator,
                    valueInstantiator.delegateCreator,
                    valueInstantiator.getDelegateType(config),
                    arrayOfNulls<SettableBeanProperty>(0),
                    valueInstantiator.withArgsCreator,
                    modifiedProperties
                )
            }
        }
    }
}