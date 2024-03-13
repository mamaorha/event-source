package co.il.mh.event.source.core

import co.il.mh.event.source.data.CursorData
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.utils.Pagination.flatten
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

abstract class EventStoreContractTest(private val eventStore: EventStore<Long, PersonEvent, Person>) {
    data class Person(val id: Long, val name: String, val age: Double, val address: String?)

    sealed interface PersonEvent
    data class PersonCreatedEvent(val person: Person) : PersonEvent
    data class PersonNameChangedEvent(val name: String) : PersonEvent
    data class PersonAgeChangedEvent(val age: Double) : PersonEvent
    data class PersonAddressChangedEvent(val address: String?) : PersonEvent

    private fun aEventData(): EventData<PersonEvent> {
        val data = when (Random.nextInt(0, 3)) {
            0 -> PersonCreatedEvent(
                person = Person(
                    id = Random.nextLong(),
                    name = UUID.randomUUID().toString(),
                    age = Random.nextDouble(),
                    address = null
                )
            )

            1 -> PersonNameChangedEvent(name = UUID.randomUUID().toString())
            2 -> PersonAgeChangedEvent(age = Random.nextDouble())
            else -> PersonAddressChangedEvent(address = UUID.randomUUID().toString())
        }

        return EventData(
            data = data,
            time = System.currentTimeMillis()
        )
    }

    companion object {
        fun aPerson(): Person {
            return Person(
                id = Random.nextLong(),
                name = UUID.randomUUID().toString(),
                age = Random.nextDouble(),
                address = UUID.randomUUID().toString()
            )
        }
    }

    @Test
    fun `persist data should notify listeners`() {
        val dummyListener = mutableMapOf<Long, List<EventData<PersonEvent>>>()
        eventStore.registerOnPersistListener { pk, events ->
            val currEvents = dummyListener[pk] ?: emptyList()
            dummyListener[pk] = currEvents + events
        }

        val pk = Random.nextLong()
        val events = listOf(aEventData())

        eventStore.persist(
            pk = pk,
            events = events
        )

        Assertions.assertEquals(events, dummyListener[pk])

        val additionalEvents = listOf(aEventData())

        eventStore.persist(
            pk = pk,
            events = additionalEvents
        )

        Assertions.assertEquals(events + additionalEvents, dummyListener[pk])
    }

    @Nested
    inner class GetEventsByPk {
        @Test
        fun `shouldn't return anything for unknown pk`() {
            eventStore.persist(
                pk = Random.nextLong(),
                events = listOf(aEventData())
            )

            Assertions.assertTrue(
                eventStore.getEvents(
                    pk = Random.nextLong(),
                    cursor = -1,
                    pageSize = 50
                ).toList().isEmpty()
            )
        }

        @Test
        fun `should return events based on known pk and cursor`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val eventsCursorData = eventStore.getEvents(
                pk = pk,
                cursor = -1,
                pageSize = 1
            ).flatten().toList()

            Assertions.assertEquals(events, eventsCursorData.map { it.data })

            for (i in 0 until eventsCursorData.size) {
                val cursor = eventsCursorData[i].cursor
                val expected = events.subList(i + 1, events.size)
                val result = eventStore.getEvents(
                    pk = pk,
                    cursor = cursor,
                    pageSize = 1
                ).flatten().toList()

                Assertions.assertEquals(expected, result.map { it.data })
            }
        }

        @Test
        fun `should paginate using smaller page size than actual events`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val eventsCursorData = eventStore.getEvents(
                pk = pk,
                cursor = -1,
                pageSize = 1
            ).toList()

            Assertions.assertEquals(events.size, eventsCursorData.size)

            for (i in events.indices) {
                Assertions.assertEquals(listOf(events[i]), eventsCursorData[i].map { it.data })
            }
        }

        @Test
        fun `should paginate using bigger page size than actual events`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val eventsCursorData = eventStore.getEvents(
                pk = pk,
                cursor = -1,
                pageSize = events.size + 1
            ).toList()

            Assertions.assertEquals(1, eventsCursorData.size)
            Assertions.assertEquals(events, eventsCursorData[0].map { it.data })
        }
    }

    @Nested
    inner class GetEventsByCursor {
        @Test
        fun `should get all events`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val secondPk = Random.nextLong()
            val secondEvents = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = secondPk,
                events = secondEvents
            )

            val pkToCursorDataEvents = eventStore.getEvents(cursor = -1L, pageSize = 50).flatten().toList()
            val pkToEventDataList =
                pkToCursorDataEvents.groupBy(keySelector = { it.first }, valueTransform = { it.second.data })

            Assertions.assertEquals(events, pkToEventDataList[pk])
            Assertions.assertEquals(secondEvents, pkToEventDataList[secondPk])
        }

        @Test
        fun `should return events based on cursor`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val pkToCursorDataEvents = eventStore.getEvents(
                cursor = -1,
                pageSize = 1
            ).flatten().toList()

            val relevantEvents = pkToCursorDataEvents.filter { it.first == pk }
            Assertions.assertEquals(events, relevantEvents.map { it.second.data })

            for (i in relevantEvents.indices) {
                val cursor = relevantEvents[i].second.cursor
                val expected = events.subList(i + 1, events.size)
                val result = eventStore.getEvents(
                    cursor = cursor,
                    pageSize = 1
                ).flatten().toList()

                Assertions.assertEquals(expected, result.map { it.second.data })
            }
        }

        @Test
        fun `should paginate using smaller page size than actual events`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val cursor = eventStore.getEvents(
                pk = pk,
                cursor = -1,
                pageSize = 1
            ).flatten().findFirst().map { it.cursor - 1 }.get()

            val pkToCursorDataEvents = eventStore.getEvents(
                cursor = cursor,
                pageSize = 1
            ).toList()

            Assertions.assertEquals(events.size, pkToCursorDataEvents.size)

            for (i in events.indices) {
                Assertions.assertEquals(listOf(events[i]), pkToCursorDataEvents[i].map { it.second.data })
            }
        }

        @Test
        fun `should paginate using bigger page size than actual events`() {
            val pk = Random.nextLong()
            val events = listOf(aEventData(), aEventData(), aEventData())

            eventStore.persist(
                pk = pk,
                events = events
            )

            val cursor = eventStore.getEvents(
                pk = pk,
                cursor = -1,
                pageSize = 1
            ).flatten().findFirst().map { it.cursor - 1 }.get()

            val pkToCursorDataEvents = eventStore.getEvents(
                cursor = cursor,
                pageSize = events.size + 1
            ).toList()

            Assertions.assertEquals(1, pkToCursorDataEvents.size)
            Assertions.assertEquals(events, pkToCursorDataEvents[0].map { it.second.data })
        }
    }

    @Test
    fun `get snapshot shouldn't return anything for unknown pk`() {
        eventStore.save(pk = Random.nextLong(), snapshot = CursorData(data = aPerson(), cursor = Random.nextLong()))
        Assertions.assertNull(eventStore.getSnapshot(pk = Random.nextLong()))
    }

    @Nested
    inner class Save {
        @Test
        fun `save snapshot for new pk`() {
            val pk = Random.nextLong()
            val snapshot = CursorData(data = aPerson(), cursor = Random.nextLong())

            eventStore.save(pk = pk, snapshot = snapshot)
            Assertions.assertEquals(snapshot, eventStore.getSnapshot(pk = pk))
        }

        @Test
        fun `save snapshot for existing pk`() {
            val pk = Random.nextLong()
            val snapshot = CursorData(data = aPerson(), cursor = Random.nextLong())

            eventStore.save(pk = pk, snapshot = snapshot)

            val newSnapshot = CursorData(data = aPerson(), cursor = Random.nextLong())
            eventStore.save(pk = pk, snapshot = newSnapshot)

            Assertions.assertEquals(newSnapshot, eventStore.getSnapshot(pk = pk))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete snapshot for non-existing pk shouldn't affect other pks`() {
            val pk = Random.nextLong()
            val snapshot = CursorData(data = aPerson(), cursor = Random.nextLong())
            eventStore.save(pk = pk, snapshot = snapshot)

            eventStore.delete(pk = Random.nextLong())
            Assertions.assertEquals(snapshot, eventStore.getSnapshot(pk = pk))
        }

        @Test
        fun `delete snapshot for existing pk`() {
            val pk = Random.nextLong()
            eventStore.save(pk = pk, snapshot = CursorData(data = aPerson(), cursor = Random.nextLong()))
            eventStore.delete(pk = pk)
            Assertions.assertNull(eventStore.getSnapshot(pk = pk))
        }
    }
}

class InMemoryEventStoreTest : EventStoreContractTest(eventStore = InMemoryEventStore())