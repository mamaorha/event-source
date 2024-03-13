package co.il.mh.event.source.core

import co.il.mh.event.source.data.CursorData
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.utils.Pagination.flatten
import co.il.mh.event.source.utils.Pagination.partition
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

abstract class EventStore<Pk, Event, Snapshot> {
    private val listeners = mutableListOf<(pk: Pk, events: List<EventData<Event>>) -> Unit>()

    fun registerOnPersistListener(listener: (pk: Pk, events: List<EventData<Event>>) -> Unit) {
        synchronized(listeners) {
            listeners += listener
        }
    }

    fun persist(pk: Pk, events: List<EventData<Event>>) {
        persistData(pk = pk, events = events)
        listeners.forEach { it(pk, events) }
    }

    protected abstract fun persistData(pk: Pk, events: List<EventData<Event>>)

    abstract fun getEvents(pk: Pk, cursor: Long, pageSize: Int): Stream<List<CursorData<EventData<Event>>>>
    abstract fun getEvents(cursor: Long, pageSize: Int): Stream<List<Pair<Pk, CursorData<EventData<Event>>>>>
    abstract fun getSnapshot(pk: Pk): CursorData<Snapshot>?
    abstract fun save(pk: Pk, snapshot: CursorData<Snapshot>)
    abstract fun delete(pk: Pk)
}

// this shouldn't be used in prod
class InMemoryEventStore<Pk, Event, Snapshot> : EventStore<Pk, Event, Snapshot>() {
    private val pkStore = ConcurrentHashMap<Pk, List<CursorData<EventData<Event>>>>()
    private val eventStore = mutableListOf<Pair<Pk, CursorData<EventData<Event>>>>()
    private val snapshotStore = ConcurrentHashMap<Pk, CursorData<Snapshot>>()

    private val iterator = generateSequence(1L) { it + 1 }.iterator()

    override fun persistData(pk: Pk, events: List<EventData<Event>>) {
        val currList = pkStore.getOrPut(pk) { emptyList() }
        val newEvents = events.map { CursorData(data = it, cursor = iterator.next()) }

        pkStore[pk] = currList + newEvents
        eventStore += newEvents.map { pk to it }
    }

    //paginating results
    override fun getEvents(pk: Pk, cursor: Long, pageSize: Int): Stream<List<CursorData<EventData<Event>>>> {
        val events = (pkStore[pk]?.filter { it.cursor > cursor } ?: emptyList())
        return events.partition(pageSize = pageSize)
    }

    override fun getEvents(cursor: Long, pageSize: Int): Stream<List<Pair<Pk, CursorData<EventData<Event>>>>> {
        val events = eventStore.filter { it.second.cursor > cursor }
        return events.partition(pageSize = pageSize)
    }

    override fun getSnapshot(pk: Pk): CursorData<Snapshot>? {
        return snapshotStore[pk]
    }

    override fun save(pk: Pk, snapshot: CursorData<Snapshot>) {
        snapshotStore[pk] = snapshot
    }

    override fun delete(pk: Pk) {
        pkStore.remove(pk)
        eventStore.removeIf { it.first == pk }
        snapshotStore.remove(pk)
    }

    fun clear() {
        pkStore.clear()
        eventStore.clear()
        snapshotStore.clear()
    }
}