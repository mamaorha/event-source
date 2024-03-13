package co.il.mh.event.source.example.data

data class PostSummary(
    val id: String,
    val authorMail: String,
    val title: String,
    val updateTime: Long,
    val creationTime: Long
)
