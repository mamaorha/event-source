package co.il.mh.event.source.core

import co.il.mh.event.source.data.EventData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class EventViewTest {
    @Test
    fun `should publish catch-up sync on init`() {
        val pubSub = InMemoryPubSub
        val viewName = "post-summary"

        val consumedCatchUp = AtomicBoolean(false)

        pubSub.registerConsumer(topic = eventViewTopic, group = "test", handler = {
            if (it == "catch-up") consumedCatchUp.set(true)
        })

        object : EventView<Long, EventViewHolderTest.PostEvent, EventViewHolderTest.PostSummary>(
            viewName = viewName,
            pubSub = pubSub,
            eventStore = InMemoryEventStore<Long, EventViewHolderTest.PostEvent, EventViewHolderTest.Post>()
        ) {
            override fun getCursor(): Long {
                return 0L
            }

            override fun save(
                cursor: Long,
                entities: List<EventViewHolderTest.PostSummary>,
                deletedEntities: List<Long>
            ) {
            }

            override fun reduce(
                aggregate: EventViewHolderTest.PostSummary?,
                eventData: EventData<EventViewHolderTest.PostEvent>
            ): EventViewHolderTest.PostSummary? {
                return null
            }

            override fun getCurrentEntity(pk: Long): EventViewHolderTest.PostSummary? {
                return null
            }
        }

        for (i in 0 until 10) {
            try {
                Assertions.assertTrue(consumedCatchUp.get())
                return
            } catch (e: AssertionError) {
                if (i + 1 < 10) {
                    Thread.sleep(100)
                } else {
                    throw e
                }
            }
        }
    }

    //example-module covers this e2e
}