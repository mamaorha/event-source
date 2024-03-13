package co.il.mh.event.source.sql

import co.il.mh.event.source.core.EventStore
import co.il.mh.event.source.core.EventView
import co.il.mh.event.source.core.PubSub
import java.sql.Connection
import javax.sql.DataSource

abstract class SqlEventView<Pk, Event, Entity>(
    viewName: String,
    pubSub: PubSub,
    eventStore: EventStore<Pk, Event, *>,
    protected val dataSource: DataSource
) : EventView<Pk, Event, Entity>(
    viewName = viewName,
    pubSub = pubSub,
    eventStore = eventStore
) {
    companion object {
        val tableName = "views"
        val nameColumn = "name"
        val cursorColumn = "`cursor`"

        val getCursorQuery = """
            SELECT $cursorColumn FROM $tableName WHERE $nameColumn = ? 
        """.trimIndent()

        val saveCursorQuery = """
            INSERT INTO $tableName SET $nameColumn = ?, $cursorColumn = ? ON DUPLICATE KEY UPDATE $cursorColumn = ?
        """.trimIndent()

        fun getCursor(dataSource: DataSource, viewName: String): Long {
            dataSource.connection.use { connection ->
                connection.prepareStatement(getCursorQuery).use { preparedStatement ->
                    preparedStatement.setString(1, viewName)

                    preparedStatement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            val cursor = resultSet.getLong(cursorColumn.replace("`", ""))
                            return cursor
                        }
                    }
                }
            }

            return -1
        }

        fun saveCursor(connection: Connection, viewName: String, cursor: Long) {
            connection.prepareStatement(saveCursorQuery).use { preparedStatement ->
                preparedStatement.setString(1, viewName)
                preparedStatement.setLong(2, cursor)
                preparedStatement.setLong(3, cursor)

                preparedStatement.executeUpdate()
            }
        }
    }

    override fun getCursor(): Long {
        return Companion.getCursor(dataSource = dataSource, viewName = viewName)
    }

    override fun save(cursor: Long, entities: List<Entity>, deletedEntities: List<Pk>) {
        dataSource.connection.use {
            val autoCommit = it.autoCommit
            it.autoCommit = false

            try {
                save(connection = it, entities = entities, deletedEntities = deletedEntities)
                saveCursor(connection = it, viewName = viewName, cursor = cursor)
                it.commit()
            } finally {
                it.autoCommit = autoCommit
            }
        }
    }

    abstract fun save(connection: Connection, entities: List<Entity>, deletedEntities: List<Pk>)
}