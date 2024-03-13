package co.il.mh.event.source.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class Secret(
    //property name that holds the secret for the encryption/decryption
    val property: String
)
