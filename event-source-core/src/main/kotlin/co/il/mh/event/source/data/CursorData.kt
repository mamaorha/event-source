package co.il.mh.event.source.data

data class CursorData<Data>(
    val data: Data,
    val cursor: Long
)
