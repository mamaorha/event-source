package co.il.mh.event.source.example.post

sealed interface PostCommand

data class CreatePost(
    val id: String,
    val authorMail: String,
    val title: String,
    val content: String,
    val creationTime: Long
) : PostCommand

data class UpdatePost(
    val title: String,
    val content: String
) : PostCommand

data class SetPostTitle(val title: String) : PostCommand
data class SetPostContent(val content: String) : PostCommand
data class DeletePost(val reason: String?) : PostCommand