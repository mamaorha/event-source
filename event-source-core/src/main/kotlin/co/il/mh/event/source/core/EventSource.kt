package co.il.mh.event.source.core

import co.il.mh.event.source.data.CursorData
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.utils.Pagination.flatten

class EventSource<Pk, Command, Event, Snapshot>(
    private val pkProvider: PkProvider<Pk>,
    private val commandHandler: CommandHandler<Command, Event>,
    private val eventReducer: EventReducer<Event, Snapshot>,
    private val eventStore: EventStore<Pk, Event, Snapshot>
) {
    fun generateNewKey(): Pk {
        return pkProvider.generateNewKey()
    }

    fun execute(pk: Pk, commands: List<Command>): Snapshot? {
        val now = System.currentTimeMillis()
        val events = commandHandler.execute(commands = commands).map { EventData(data = it, time = now) }

        eventStore.persist(pk = pk, events = events)

        return get(pk = pk)
    }

    //TODO - this flow is idempotent so thread-safe / multi-process is fine but might want to adjust it to avoid duplicate actions
    fun get(pk: Pk, filter: (Snapshot) -> Boolean = { true }): Snapshot? {
        val snapshotCursorData = eventStore.getSnapshot(pk = pk)

        val cursor = snapshotCursorData?.cursor ?: -1L
        var snapshot = snapshotCursorData?.data
        var newCursor = cursor

        eventStore.getEvents(pk = pk, cursor = cursor, pageSize = 50).flatten().forEach { eventDataCursor ->
            snapshot = eventReducer.reduce(aggregate = snapshot, eventData = eventDataCursor.data)
            newCursor = eventDataCursor.cursor
        }

        if (cursor != newCursor) {
            snapshot?.let {
                eventStore.save(pk = pk, snapshot = CursorData(data = it, cursor = newCursor))
            }
        }

        return if (snapshot == null || !filter(snapshot!!)) null
        else snapshot
    }

    fun delete(pk: Pk) {
        eventStore.delete(pk = pk)
    }
}