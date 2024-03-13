package co.il.mh.event.source.sql

import co.il.mh.event.source.core.*
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.utils.Pagination
import co.il.mh.event.source.utils.Pagination.flatten
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.stream.Stream
import javax.sql.DataSource

class SqlEventViewTest {
    class PersonAggregateView(
        pubSub: PubSub,
        eventStore: EventStore<Long, EventStoreContractTest.PersonEvent, *>,
        dataSource: DataSource
    ) :
        SqlEventView<Long, EventStoreContractTest.PersonEvent, PersonAggregateView.PersonSummary>(
            viewName = tableName,
            pubSub = pubSub,
            eventStore = eventStore,
            dataSource = dataSource
        ) {
        data class PersonSummary(val id: Long, val name: String)

        companion object {
            private val tableName = "person_summary_view"
            private val idColumn = "id"
            private val nameColumn = "name"

            private val getCurrentEntityQuery = """
                SELECT $idColumn, $nameColumn FROM $tableName WHERE $idColumn = ?
            """.trimIndent()

            private val saveQuery = """
                INSERT INTO $tableName SET $idColumn = ?, $nameColumn = ? ON DUPLICATE KEY UPDATE $nameColumn = ?
            """.trimIndent()

            private val deleteQuery = """
                DELETE FROM $tableName WHERE $idColumn = ?
            """.trimIndent()

            private val getPersonSummariesQuery = """
                SELECT $idColumn, $nameColumn FROM $tableName WHERE $idColumn > ? ORDER BY $idColumn ASC LIMIT ?
            """.trimIndent()
        }

        override fun getCurrentEntity(pk: Long): PersonSummary? {
            dataSource.connection.use { connection ->
                connection.prepareStatement(getCurrentEntityQuery).use { preparedStatement ->
                    preparedStatement.setLong(1, pk)

                    preparedStatement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            val id = resultSet.getLong(idColumn)
                            val name = resultSet.getString(SqlEventView.nameColumn)

                            return PersonSummary(
                                id = id,
                                name = name
                            )
                        }
                    }
                }
            }

            return null
        }

        override fun reduce(
            aggregate: PersonSummary?,
            eventData: EventData<EventStoreContractTest.PersonEvent>
        ): PersonSummary? {
            return when (val data = eventData.data) {
                is EventStoreContractTest.PersonCreatedEvent -> PersonSummary(
                    id = data.person.id,
                    name = data.person.name
                )

                is EventStoreContractTest.PersonNameChangedEvent -> aggregate?.copy(name = data.name)
                else -> aggregate
            }
        }

        override fun save(connection: Connection, entities: List<PersonSummary>, deletedEntities: List<Long>) {
            if (entities.isNotEmpty()) {
                connection.prepareStatement(saveQuery).use { preparedStatement ->
                    entities.forEach { entity ->
                        preparedStatement.setLong(1, entity.id)
                        preparedStatement.setString(2, entity.name)
                        preparedStatement.setString(3, entity.name)

                        preparedStatement.addBatch()
                    }

                    preparedStatement.executeBatch()
                }
            }

            if (deletedEntities.isNotEmpty()) {
                connection.prepareStatement(deleteQuery).use { preparedStatement ->
                    entities.forEach { entity ->
                        preparedStatement.setLong(1, entity.id)

                        preparedStatement.addBatch()
                    }

                    preparedStatement.executeBatch()
                }
            }
        }

        fun getPersonSummaries(fromCursor: Long = -1L, limit: Long = 50): Stream<PersonSummary> {
            return Pagination.streamByCursor(
                extractor = { cursor ->
                    dataSource.connection.use { connection ->
                        connection.prepareStatement(getPersonSummariesQuery).use { preparedStatement ->
                            preparedStatement.setLong(1, cursor ?: fromCursor)
                            preparedStatement.setLong(2, limit)

                            var newCursor = cursor ?: fromCursor
                            val results = mutableListOf<PersonSummary>()

                            preparedStatement.executeQuery().use { resultSet ->
                                while (resultSet.next()) {
                                    val id = resultSet.getLong(idColumn)
                                    val name = resultSet.getString(nameColumn)

                                    results.add(PersonSummary(id = id, name = name))

                                    if (id > newCursor) {
                                        newCursor = id
                                    }
                                }
                            }

                            newCursor to results
                        }
                    }
                },
                cursor = fromCursor
            ).map { it.second }.flatten()
        }
    }

    companion object {
        private val iterator = generateSequence(1L) { it + 1 }.iterator()
        private val eventStore =
            InMemoryEventStore<Long, EventStoreContractTest.PersonEvent, EventStoreContractTest.Person>()
        private val exampleView = PersonAggregateView(
            pubSub = InMemoryPubSub,
            eventStore = eventStore,
            dataSource = IT.dataSource
        )
    }

    private fun getLastCursor(): Long {
        return exampleView.getPersonSummaries().toList().lastOrNull()?.id ?: -1
    }

    @Test
    fun `fetching from last cursor should return empty list`() {
        val cursor = getLastCursor()
        Assertions.assertTrue(exampleView.getPersonSummaries(fromCursor = cursor).toList().isEmpty())
    }

    @Test
    fun `fetching should return new records`() {
        val cursor = getLastCursor()
        val person = EventStoreContractTest.aPerson()

        eventStore.persist(
            pk = iterator.next(),
            events = listOf(
                EventData(
                    data = EventStoreContractTest.PersonCreatedEvent(person = person),
                    time = System.currentTimeMillis()
                )
            )
        )

        Assertions.assertEquals(
            listOf(person.name),
            exampleView.getPersonSummaries(fromCursor = cursor).map { it.name }.toList()
        )
    }
}