package co.il.mh.event.source.example.post

import co.il.mh.event.source.core.CommandHandler

object PostCommandHandler : CommandHandler<PostCommand, PostEvent> {
    override fun execute(commands: List<PostCommand>): List<PostEvent> {
        return commands.flatMap { command ->
            when (command) {
                is CreatePost -> listOf(
                    PostCreated(
                        id = command.id,
                        authorMail = command.authorMail,
                        title = command.title,
                        content = command.content,
                        creationTime = command.creationTime
                    )
                )

                is UpdatePost -> listOf(
                    PostTitleSet(title = command.title),
                    PostContentSet(content = command.content)
                )

                is SetPostTitle -> listOf(PostTitleSet(title = command.title))
                is SetPostContent -> listOf(PostContentSet(content = command.content))
                is DeletePost -> listOf(PostDeleted(reason = command.reason))
            }
        }
    }
}