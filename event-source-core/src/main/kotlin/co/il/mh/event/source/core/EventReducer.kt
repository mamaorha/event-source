package co.il.mh.event.source.core

import co.il.mh.event.source.data.EventData

interface EventReducer<Event, Aggregate> {
    fun reduce(aggregate: Aggregate?, eventData: EventData<Event>): Aggregate
}