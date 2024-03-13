package co.il.mh.event.source.sql

import co.il.mh.event.source.core.EventStoreContractTest
import co.il.mh.event.source.jackson.EventSourceObjectMapper

class SqlEventStoreTest : EventStoreContractTest(
    eventStore = SqlEventStore(
        name = "person",
        pkColumns = listOf("pk"),
        writePkValues = { iterator, ps, pk ->
            ps.setLong(iterator.next(), pk)
        },
        readPk = { rs -> rs.getLong("pk") },
        objectMapper = EventSourceObjectMapper.objectMapper,
        dataSource = IT.dataSource
    )
)