package co.il.mh.event.source.core

class EventViewHolder<Pk, Event, Entity, out Response : EventView<Pk, Event, Entity>>(private val eventView: Response) {
    @Suppress("UNCHECKED_CAST")
    fun get(): Response {
        return eventView.sync() as Response
    }
}