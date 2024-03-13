package co.il.mh.event.source.core

import co.il.mh.event.source.data.EventData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class EventViewHolderTest {
    data class Post(val id: Long, val author: String, val title: String, val content: String)
    data class PostSummary(val id: Long, val author: String, val title: String)

    sealed interface PostEvent

    @Test
    fun `event view holder should return the view after calling sync`() {
        val pubSub = InMemoryPubSub
        val viewName = "post-summary"

        val consumedSync = AtomicBoolean(false)

        pubSub.registerConsumer(topic = eventViewTopic, group = "test", handler = {
            if (it == "sync") consumedSync.set(true)
        })

        val eventView = object : EventView<Long, PostEvent, PostSummary>(
            viewName = viewName,
            pubSub = pubSub,
            eventStore = InMemoryEventStore<Long, PostEvent, Post>()
        ) {
            override fun getCursor(): Long {
                return 0L
            }

            override fun save(cursor: Long, entities: List<PostSummary>, deletedEntities: List<Long>) {
            }

            override fun reduce(aggregate: PostSummary?, eventData: EventData<PostEvent>): PostSummary? {
                return null
            }

            override fun getCurrentEntity(pk: Long): PostSummary? {
                return null
            }
        }

        val eventViewHolder = EventViewHolder(eventView = eventView)
        Assertions.assertEquals(eventView, eventViewHolder.get())
        Assertions.assertTrue(consumedSync.get())
    }
}