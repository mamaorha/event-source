package co.il.mh.event.source.sql

import co.il.mh.event.source.core.EventStore
import co.il.mh.event.source.data.CursorData
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.jackson.JsonEntity
import co.il.mh.event.source.utils.Pagination
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.stream.Stream
import javax.sql.DataSource

class SqlEventStore<Pk : Any, Event : Any, Snapshot : Any>(
    name: String,
    private val pkColumns: List<String>,
    private val writePkValues: (iterator: Iterator<Int>, ps: PreparedStatement, pk: Pk) -> Unit,
    private val readPk: (resultSet: ResultSet) -> Pk,
    private val objectMapper: ObjectMapper,
    private val dataSource: DataSource
) : EventStore<Pk, Event, Snapshot>() {
    private val eventsTableName = "`${name}_events`"
    private val eventsIdColumn = "id"
    private val eventsEventColumn = "event"
    private val eventsTimeColumn = "time"

    private val eventsColumns =
        listOf(eventsIdColumn) + pkColumns + listOf(eventsEventColumn, eventsTimeColumn)

    private val persistDataQuery = """
        INSERT INTO $eventsTableName SET 
        ${eventsColumns.filterNot { it == eventsIdColumn }.joinToString(", ") { """$it = ?""" }}
    """.trimIndent()

    private val getEventsByPkQuery = """
        SELECT ${eventsColumns.filterNot { pkColumns.contains(it) }.joinToString(", ")} FROM $eventsTableName 
        WHERE ${pkColumns.joinToString(" AND ") { """$it = ?""" }} 
        AND $eventsIdColumn > ? ORDER BY $eventsIdColumn ASC LIMIT ?
    """.trimIndent()

    private val getEventsQuery = """
        SELECT ${eventsColumns.joinToString(", ")} FROM $eventsTableName 
        WHERE $eventsIdColumn > ? ORDER BY $eventsIdColumn ASC LIMIT ?
    """.trimIndent()

    private val deleteEventsQuery = """
        DELETE FROM $eventsTableName WHERE ${pkColumns.joinToString(" AND ") { """$it = ?""" }}
    """.trimIndent()

    private val snapshotsTableName = "${name}_snapshots"
    private val snapshotsDataColumn = "data"
    private val snapshotsCursorColumn = "`cursor`"

    private val snapshotsColumns =
        pkColumns + listOf(snapshotsDataColumn, snapshotsCursorColumn)

    private val deleteSnapshotsQuery = """
        DELETE FROM $snapshotsTableName WHERE ${pkColumns.joinToString(" AND ") { """$it = ?""" }}
    """.trimIndent()

    private val saveSnapshotQuery = """
        INSERT INTO $snapshotsTableName SET 
        ${snapshotsColumns.joinToString(", ") { """$it = ?""" }}
        ON DUPLICATE KEY UPDATE 
        ${snapshotsColumns.filterNot { pkColumns.contains(it) }.joinToString(", ") { """$it = ?""" }}
    """.trimIndent()

    private val getSnapshotQuery = """
        SELECT ${snapshotsColumns.filterNot { pkColumns.contains(it) }.joinToString(", ")} FROM $snapshotsTableName 
        WHERE ${pkColumns.joinToString(", ") { """$it = ?""" }} LIMIT 1
    """.trimIndent()

    private val eventType = object : TypeReference<JsonEntity<Event>>() {}
    private val snapshotType = object : TypeReference<JsonEntity<Snapshot>>() {}

    override fun persistData(pk: Pk, events: List<EventData<Event>>) {
        if (events.isEmpty()) return

        dataSource.connection.use { connection ->
            val autoCommit = connection.autoCommit
            connection.autoCommit = false

            try {
                connection.prepareStatement(persistDataQuery).use { preparedStatement ->
                    events.forEach { event ->
                        val iterator = generateSequence(1) { it + 1 }.iterator()
                        writePkValues(iterator, preparedStatement, pk)
                        preparedStatement.setString(
                            iterator.next(),
                            objectMapper.writeValueAsString(JsonEntity(event.data))
                        )
                        preparedStatement.setLong(iterator.next(), event.time)

                        preparedStatement.addBatch()
                    }

                    preparedStatement.executeBatch()
                }

                connection.commit()
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }

    override fun getEvents(pk: Pk, cursor: Long, pageSize: Int): Stream<List<CursorData<EventData<Event>>>> {
        return Pagination.streamByCursor(
            extractor = { currCursor ->
                dataSource.connection.use { connection ->
                    connection.prepareStatement(getEventsByPkQuery).use { preparedStatement ->
                        val iterator = generateSequence(1) { it + 1 }.iterator()

                        writePkValues(iterator, preparedStatement, pk)
                        preparedStatement.setLong(iterator.next(), currCursor ?: cursor)
                        preparedStatement.setInt(iterator.next(), pageSize)

                        preparedStatement.executeQuery().use { resultSet ->
                            val results = mutableListOf<CursorData<EventData<Event>>>()

                            while (resultSet.next()) {
                                val id = resultSet.getLong(eventsIdColumn)
                                val event = objectMapper.readValue(
                                    resultSet.getString(eventsEventColumn),
                                    eventType
                                ).data
                                val time = resultSet.getLong(eventsTimeColumn)

                                results.add(
                                    CursorData(
                                        data = EventData(
                                            data = event,
                                            time = time
                                        ),
                                        cursor = id
                                    )
                                )
                            }

                            val nextCursor = results.maxByOrNull { it.cursor }?.cursor ?: currCursor ?: cursor
                            nextCursor to results
                        }
                    }
                }
            },
            cursor = cursor
        ).map { it.second }
    }

    override fun getEvents(cursor: Long, pageSize: Int): Stream<List<Pair<Pk, CursorData<EventData<Event>>>>> {
        return Pagination.streamByCursor(
            extractor = { currCursor ->
                dataSource.connection.use { connection ->
                    connection.prepareStatement(getEventsQuery).use { preparedStatement ->
                        val iterator = generateSequence(1) { it + 1 }.iterator()

                        preparedStatement.setLong(iterator.next(), currCursor ?: cursor)
                        preparedStatement.setInt(iterator.next(), pageSize)

                        preparedStatement.executeQuery().use { resultSet ->
                            val results = mutableListOf<Pair<Pk, CursorData<EventData<Event>>>>()

                            while (resultSet.next()) {
                                val id = resultSet.getLong(eventsIdColumn)
                                val pk = readPk(resultSet)
                                val event = objectMapper.readValue(
                                    resultSet.getString(eventsEventColumn),
                                    eventType
                                ).data
                                val time = resultSet.getLong(eventsTimeColumn)

                                results.add(
                                    pk to CursorData(
                                        data = EventData(
                                            data = event,
                                            time = time
                                        ),
                                        cursor = id
                                    )
                                )
                            }

                            val nextCursor = results.maxByOrNull { it.second.cursor }?.second?.cursor ?: cursor
                            nextCursor to results
                        }
                    }
                }
            },
            cursor = cursor
        ).map { it.second }
    }

    override fun delete(pk: Pk) {
        dataSource.connection.use { connection ->
            val autoCommit = connection.autoCommit
            connection.autoCommit = false

            try {
                connection.prepareStatement(deleteEventsQuery).use { preparedStatement ->
                    val iterator = generateSequence(1) { it + 1 }.iterator()
                    writePkValues(iterator, preparedStatement, pk)

                    preparedStatement.executeUpdate()
                }

                connection.prepareStatement(deleteSnapshotsQuery).use { preparedStatement ->
                    val iterator = generateSequence(1) { it + 1 }.iterator()
                    writePkValues(iterator, preparedStatement, pk)

                    preparedStatement.executeUpdate()
                }

                connection.commit()
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }

    override fun save(pk: Pk, snapshot: CursorData<Snapshot>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(saveSnapshotQuery).use { preparedStatement ->
                val iterator = generateSequence(1) { it + 1 }.iterator()
                writePkValues(iterator, preparedStatement, pk)

                val data = objectMapper.writeValueAsString(JsonEntity(snapshot.data))

                //insert
                preparedStatement.setString(iterator.next(), data)
                preparedStatement.setLong(iterator.next(), snapshot.cursor)

                //on duplicate - update
                preparedStatement.setString(iterator.next(), data)
                preparedStatement.setLong(iterator.next(), snapshot.cursor)

                preparedStatement.executeUpdate()
            }
        }
    }

    override fun getSnapshot(pk: Pk): CursorData<Snapshot>? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(getSnapshotQuery).use { preparedStatement ->
                val iterator = generateSequence(1) { it + 1 }.iterator()
                writePkValues(iterator, preparedStatement, pk)

                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return CursorData(
                            data = objectMapper.readValue(
                                resultSet.getString(snapshotsDataColumn),
                                snapshotType
                            ).data,
                            cursor = resultSet.getLong(snapshotsCursorColumn.replace("`", ""))
                        )
                    }
                }
            }
        }

        return null
    }
}