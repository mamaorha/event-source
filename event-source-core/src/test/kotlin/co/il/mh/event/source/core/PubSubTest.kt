package co.il.mh.event.source.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

abstract class PubSubContractTest(private val pubSub: PubSub) {
    private fun aMessageList(): List<String> {
        val messages = mutableListOf<String>()

        for (i in 0 until 10) {
            messages.add(UUID.randomUUID().toString())
        }

        return messages
    }

    @Test
    fun `should do nothing when publishing to topic that have no listeners`() {
        pubSub.publish(
            topic = UUID.randomUUID().toString(),
            partitionKey = UUID.randomUUID().toString(),
            message = UUID.randomUUID().toString()
        )
    }

    @Test
    fun `should invoke consumers`() {
        val topicA = UUID.randomUUID().toString()
        val handlerA1Messages = mutableListOf<String>()
        val handlerA2Messages = mutableListOf<String>()

        val topicB = UUID.randomUUID().toString()
        val handlerB1Messages = mutableListOf<String>()
        val handlerB2Messages = mutableListOf<String>()

        val messagesForTopicA = aMessageList()
        val messagesForTopicB = aMessageList()

        pubSub.registerConsumer(topic = topicA, group = "A1", handler = { message -> handlerA1Messages.add(message) })
        pubSub.registerConsumer(topic = topicA, group = "A2", handler = { message -> handlerA2Messages.add(message) })

        pubSub.registerConsumer(topic = topicB, group = "B1", handler = { message -> handlerB1Messages.add(message) })
        pubSub.registerConsumer(topic = topicB, group = "B2", handler = { message -> handlerB2Messages.add(message) })

        messagesForTopicA.forEach {
            pubSub.publish(
                topic = topicA,
                partitionKey = UUID.randomUUID().toString(),
                message = it
            )
        }
        messagesForTopicB.forEach {
            pubSub.publish(
                topic = topicB,
                partitionKey = UUID.randomUUID().toString(),
                message = it
            )
        }

        for (i in 0 until 10) {
            try {
                Assertions.assertEquals(messagesForTopicA, handlerA1Messages)
                Assertions.assertEquals(messagesForTopicA, handlerA2Messages)

                Assertions.assertEquals(messagesForTopicB, handlerB1Messages)
                Assertions.assertEquals(messagesForTopicB, handlerB2Messages)

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
}

class InMemoryPubSubTest : PubSubContractTest(pubSub = InMemoryPubSub)