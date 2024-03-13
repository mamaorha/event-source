package co.il.mh.event.source.jackson

data class JsonEntity<T : Any>(val clazz: Class<T>, val data: T) {
    constructor(data: T) : this(clazz = data.javaClass, data = data)
}