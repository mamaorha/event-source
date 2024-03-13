package co.il.mh.event.source.core

import co.il.mh.event.source.data.CursorData
import co.il.mh.event.source.data.EventData

const val eventViewTopic = "event-view"

abstract class EventView<Pk, Event, Entity>(
    //the view name is used for the partitionKey used in the pubSub - messages are published to topic: "event-view"
    protected val viewName: String,
    private val pubSub: PubSub,
    private val eventStore: EventStore<Pk, Event, *>
) {
    init {
        pubSub.registerConsumer(topic = eventViewTopic, group = viewName, handler = { syncHandler() })
        eventStore.registerOnPersistListener { _, _ -> sync() }

        //in case this is a new view, lets help it try to catch up before we actually need the data
        Thread.startVirtualThread {
            pubSub.publish(
                topic = eventViewTopic,
                partitionKey = viewName,
                message = "catch-up"
            )
        }
    }

    fun sync(): EventView<Pk, Event, Entity> {
        pubSub.publish(topic = eventViewTopic, partitionKey = viewName, message = "sync")
        return this
    }

    private fun syncHandler() {
        eventStore.getEvents(cursor = getCursor(), pageSize = 50).forEach { eventsPage ->
            val pageResults = eventsPage.groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map { (pk, events) ->
                    var currentEntity = getCurrentEntity(pk = pk)
                    var cursor = 0L

                    events.forEach {
                        currentEntity = reduce(aggregate = currentEntity, eventData = it.data)
                        cursor = it.cursor
                    }

                    pk to CursorData(data = currentEntity, cursor = cursor)
                }

            val newCursor = pageResults.maxBy { it.second.cursor }.second.cursor

            save(
                cursor = newCursor,
                entities = pageResults.mapNotNull { it.second.data },
                deletedEntities = pageResults.filter { it.second.data == null }.map { it.first }
            )
        }
    }

    protected abstract fun getCursor(): Long

    protected abstract fun getCurrentEntity(pk: Pk): Entity?

    protected abstract fun reduce(aggregate: Entity?, eventData: EventData<Event>): Entity?

    protected abstract fun save(cursor: Long, entities: List<Entity>, deletedEntities: List<Pk>)
}