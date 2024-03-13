package co.il.mh.event.source.example.post

import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.example.data.Post

object PostReducer : co.il.mh.event.source.core.EventReducer<PostEvent, Post> {
    override fun reduce(aggregate: Post?, eventData: EventData<PostEvent>): Post {
        when (val event = eventData.data) {
            is PostCreated -> {
                require(aggregate == null) { "can't process event [${event}], it was already created before" }

                return Post(
                    id = event.id,
                    authorMail = event.authorMail,
                    title = event.title,
                    content = event.content,
                    updateTime = event.creationTime,
                    creationTime = event.creationTime,
                    deleted = false
                )
            }

            is PostUpdateEvent -> {
                requireNotNull(aggregate) { "can't process event [${event}] before PostCreated event" }
                require(!aggregate.deleted) { "post is already deleted" }

                return when (event) {
                    is PostTitleSet -> aggregate.copy(
                        title = event.title,
                        updateTime = eventData.time
                    )

                    is PostContentSet -> aggregate.copy(
                        content = event.content,
                        updateTime = eventData.time
                    )

                    is PostDeleted -> aggregate.copy(
                        deleted = true,
                        updateTime = eventData.time
                    )
                }
            }
        }
    }
}