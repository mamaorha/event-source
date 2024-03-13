package co.il.mh.event.source.jackson

import co.il.mh.event.source.annotations.Secret
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier


class SecretSerializerModifier(private val objectMapper: ObjectMapper) : BeanSerializerModifier() {
    override fun changeProperties(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        beanProperties: List<BeanPropertyWriter>
    ): List<BeanPropertyWriter> {
        beanProperties.forEach { beanProperty ->
            beanProperty.getAnnotation(Secret::class.java)?.let { secretAnnotation ->
                val secret = requireNotNull(System.getProperty(secretAnnotation.property)) {
                    "couldn't find secret by property: ${secretAnnotation.property}"
                }

                val secretSerializer = SecretSerializer(objectMapper = objectMapper, secret = secret)
                beanProperty.assignSerializer(secretSerializer)
            }
        }

        return beanProperties
    }
}