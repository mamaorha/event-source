package co.il.mh.event.source.example.post

import co.il.mh.event.source.core.EventStore
import co.il.mh.event.source.core.PubSub
import co.il.mh.event.source.data.EventData
import co.il.mh.event.source.example.data.PostSummary
import co.il.mh.event.source.sql.SqlEventView
import co.il.mh.event.source.utils.Pagination
import co.il.mh.event.source.utils.Pagination.flatten
import java.sql.Connection
import javax.sql.DataSource

class PostSummaryView(
    pubSub: PubSub,
    eventStore: EventStore<String, PostEvent, *>,
    dataSource: DataSource
) : SqlEventView<String, PostEvent, PostSummary>(
    viewName = tableName,
    pubSub = pubSub,
    eventStore = eventStore,
    dataSource = dataSource
) {
    companion object {
        private val tableName = "post_summary_view"
        private val cursorColunn = "`cursor`"
        private val postIdColumn = "post_id"
        private val authorMailColumn = "author_mail"
        private val titleColumn = "title"
        private val updateTimeColumn = "update_time"
        private val creationTimeColumn = "creation_time"

        private val columns =
            listOf(cursorColunn, postIdColumn, authorMailColumn, titleColumn, updateTimeColumn, creationTimeColumn)

        private val getCurrentEntityQuery = """
            SELECT ${columns.joinToString(", ")} FROM $tableName WHERE $postIdColumn = ?
        """.trimIndent()

        private val saveQuery = """
            INSERT INTO $tableName 
            SET ${columns.filterNot { it == cursorColunn }.joinToString(", ") { "$it = ?" }} 
            ON DUPLICATE KEY UPDATE $titleColumn = ?, $updateTimeColumn = ?
        """.trimIndent()

        private val deleteQuery = """
            DELETE FROM $tableName WHERE $postIdColumn = ?
        """.trimIndent()

        private val getPostsSummariesQuery = """
            SELECT ${columns.joinToString(", ")} FROM $tableName WHERE $authorMailColumn = ? AND $cursorColumn < ? ORDER BY $cursorColunn DESC LIMIT ?
        """.trimIndent()
    }

    override fun getCurrentEntity(pk: String): PostSummary? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(getCurrentEntityQuery).use { preparedStatement ->
                preparedStatement.setString(1, pk)

                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return PostSummary(
                            id = resultSet.getString(postIdColumn),
                            authorMail = resultSet.getString(authorMailColumn),
                            title = resultSet.getString(titleColumn),
                            updateTime = resultSet.getLong(updateTimeColumn),
                            creationTime = resultSet.getLong(creationTimeColumn)
                        )
                    }
                }
            }
        }

        return null
    }

    override fun reduce(aggregate: PostSummary?, eventData: EventData<PostEvent>): PostSummary? {
        return when (val event = eventData.data) {
            is PostCreated -> {
                require(aggregate == null) { "can't process event [${event}], it was already created before" }
                PostSummary(
                    id = event.id,
                    authorMail = event.authorMail,
                    title = event.title,
                    updateTime = event.creationTime,
                    creationTime = event.creationTime
                )
            }

            is PostUpdateEvent -> {
                requireNotNull(aggregate) { "can't process event [${event}] before PostCreated event" }

                when (event) {
                    is PostTitleSet -> aggregate.copy(
                        title = event.title,
                        updateTime = eventData.time
                    )

                    is PostDeleted -> null

                    else -> aggregate.copy(updateTime = eventData.time)
                }
            }
        }
    }

    override fun save(connection: Connection, entities: List<PostSummary>, deletedEntities: List<String>) {
        if (entities.isNotEmpty()) {
            connection.prepareStatement(saveQuery).use { preparedStatement ->
                entities.forEach { entity ->
                    val iterator = generateSequence(1) { it + 1 }.iterator()

                    //insert
                    preparedStatement.setString(iterator.next(), entity.id)
                    preparedStatement.setString(iterator.next(), entity.authorMail)
                    preparedStatement.setString(iterator.next(), entity.title)
                    preparedStatement.setLong(iterator.next(), entity.updateTime)
                    preparedStatement.setLong(iterator.next(), entity.creationTime)

                    //update
                    preparedStatement.setString(iterator.next(), entity.title)
                    preparedStatement.setLong(iterator.next(), entity.updateTime)

                    preparedStatement.addBatch()
                }

                preparedStatement.executeBatch()
            }
        }

        if (deletedEntities.isNotEmpty()) {
            connection.prepareStatement(deleteQuery).use { preparedStatement ->
                deletedEntities.forEach { postId ->
                    preparedStatement.setString(1, postId)
                    preparedStatement.addBatch()
                }

                preparedStatement.executeBatch()
            }
        }
    }

    fun getPostsSummaries(authorMail: String, pageSize: Long = 50): List<PostSummary> {
        return Pagination.streamByCursor(
            extractor = { cursor ->
                var newCursor = cursor ?: Long.MAX_VALUE
                val result = mutableListOf<PostSummary>()

                dataSource.connection.use { connection ->
                    connection.prepareStatement(getPostsSummariesQuery).use { preparedStatement ->
                        val iterator = generateSequence(1) { it + 1 }.iterator()

                        preparedStatement.setString(iterator.next(), authorMail)
                        preparedStatement.setLong(iterator.next(), newCursor)
                        preparedStatement.setLong(iterator.next(), pageSize)

                        preparedStatement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                val currCursor = resultSet.getLong(cursorColumn.replace("`", ""))

                                if (newCursor > currCursor) {
                                    newCursor = currCursor
                                }

                                result.add(
                                    PostSummary(
                                        id = resultSet.getString(postIdColumn),
                                        authorMail = resultSet.getString(authorMailColumn),
                                        title = resultSet.getString(titleColumn),
                                        updateTime = resultSet.getLong(updateTimeColumn),
                                        creationTime = resultSet.getLong(creationTimeColumn)
                                    )
                                )
                            }
                        }
                    }
                }

                newCursor to result
            },
            cursor = Long.MAX_VALUE
        ).map { it.second }.flatten().toList()
    }
}