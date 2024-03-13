package co.il.mh.event.source.core

import co.il.mh.event.source.data.EventData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class EventSourceTest {
    private val key = AtomicReference(UUID.randomUUID().toString())

    private val pkProvider = object : PkProvider<String> {
        override fun generateNewKey(): String {
            return key.get()
        }
    }

    private val echoCommandHandler = object : CommandHandler<String, String> {
        override fun execute(commands: List<String>): List<String> {
            return commands
        }
    }

    private val concatEventReducer = object : EventReducer<String, String> {
        override fun reduce(aggregate: String?, eventData: EventData<String>): String {
            return if (aggregate == null) eventData.data
            else aggregate + " " + eventData.data
        }
    }

    private val eventSource = EventSource(
        pkProvider = pkProvider,
        commandHandler = echoCommandHandler,
        eventReducer = concatEventReducer,
        eventStore = InMemoryEventStore()
    )

    private fun randomString(): String {
        return UUID.randomUUID().toString()
    }

    @Test
    fun `should generate key by given provider`() {
        for (i in 0 until 5) {
            key.set(randomString())
            Assertions.assertEquals(key.get(), eventSource.generateNewKey())
        }
    }

    @Nested
    inner class Execute {
        @Test
        fun `should return null when there is no events`() {
            Assertions.assertNull(eventSource.execute(pk = randomString(), commands = emptyList()))
        }

        @Test
        fun `should process commands and return expected result`() {
            val pksToCommands = arrayOf(
                randomString() to listOf(randomString()),
                randomString() to listOf(randomString(), randomString()),
                randomString() to listOf(randomString(), randomString(), randomString())
            )

            pksToCommands.forEach { (pk, commands) ->
                Assertions.assertEquals(
                    commands.joinToString(" "),
                    eventSource.execute(
                        pk = pk,
                        commands = commands
                    )
                )
            }
        }

        @Test
        fun `should accumulate commands and return expected result`() {
            val pk = randomString()
            val commands = listOf(randomString(), randomString())

            eventSource.execute(
                pk = pk,
                commands = commands
            )

            val newCommand = randomString()
            val expected = (commands + newCommand).joinToString(" ")

            Assertions.assertEquals(
                expected,
                eventSource.execute(
                    pk = pk,
                    commands = listOf(newCommand)
                )
            )
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `should return null for non existing pk`() {
            Assertions.assertNull(eventSource.get(pk = randomString()))
        }

        @Test
        fun `should return result for existing pk`() {
            val pk = randomString()
            val commands = listOf(randomString())

            val executeResult = eventSource.execute(
                pk = pk,
                commands = commands
            )

            Assertions.assertEquals(executeResult, eventSource.get(pk = pk))
        }

        @Test
        fun `should use filter`() {
            val pk = randomString()
            val prefix = randomString()
            val commands = listOf(prefix + randomString())

            val executeResult = eventSource.execute(
                pk = pk,
                commands = commands
            )

            Assertions.assertEquals(
                executeResult,
                eventSource.get(pk = pk, filter = { curr -> curr.startsWith(prefix) })
            )

            Assertions.assertNull(eventSource.get(pk = pk, filter = { curr -> curr.endsWith(prefix) }))
        }

        @Test
        fun `should return latest changes`() {
            val pk = randomString()
            val commands = listOf(randomString(), randomString())

            val firstResult = eventSource.execute(
                pk = pk,
                commands = commands
            )

            Assertions.assertEquals(firstResult, eventSource.get(pk = pk))

            val newCommand = randomString()
            val secondResult = eventSource.execute(
                pk = pk,
                commands = listOf(newCommand)
            )

            Assertions.assertEquals(secondResult, eventSource.get(pk = pk))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `shouldn't do nothing for non existing pk`() {
            eventSource.delete(pk = randomString())
        }

        @Test
        fun `should remove existing pk`() {
            val pk = randomString()
            val commands = listOf(randomString())

            eventSource.execute(
                pk = pk,
                commands = commands
            )

            eventSource.delete(pk = pk)
            Assertions.assertNull(eventSource.get(pk = pk))
        }

        @Test
        fun `should allow writing new events for deleted pk`() {
            val pk = randomString()
            val commands = listOf(randomString())

            val firstResult = eventSource.execute(
                pk = pk,
                commands = commands
            )

            eventSource.delete(pk = pk)

            val secondResult = eventSource.execute(
                pk = pk,
                commands = commands
            )

            Assertions.assertEquals(firstResult, secondResult)
        }
    }
}