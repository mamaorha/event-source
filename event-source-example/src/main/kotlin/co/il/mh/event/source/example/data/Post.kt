package co.il.mh.event.source.example.data

import co.il.mh.event.source.annotations.Secret

data class Post(
    val id: String,
    @Secret(property = "MailSecret") val authorMail: String,
    val title: String,
    val content: String,
    val updateTime: Long,
    val creationTime: Long,
    val deleted: Boolean
)
