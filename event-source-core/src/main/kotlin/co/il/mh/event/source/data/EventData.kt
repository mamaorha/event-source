package co.il.mh.event.source.data

data class EventData<Data>(
    val data: Data,
    val time: Long
)
