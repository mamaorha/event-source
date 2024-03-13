package co.il.mh.event.source.example.post

sealed interface PostEvent
data class PostCreated(
    val id: String,
    val authorMail: String,
    val title: String,
    val content: String,
    val creationTime: Long
) : PostEvent

sealed interface PostUpdateEvent : PostEvent
data class PostTitleSet(val title: String) : PostUpdateEvent
data class PostContentSet(val content: String) : PostUpdateEvent
data class PostDeleted(val reason: String?) : PostUpdateEvent