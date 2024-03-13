package co.il.mh.event.source.example.service

import co.il.mh.event.source.core.EventSource
import co.il.mh.event.source.core.EventViewHolder
import co.il.mh.event.source.core.UuidPkProvider
import co.il.mh.event.source.example.data.Post
import co.il.mh.event.source.example.data.PostSummary
import co.il.mh.event.source.example.post.*
import co.il.mh.event.source.jackson.EventSourceObjectMapper
import co.il.mh.event.source.kafka.KafkaPubSub
import co.il.mh.event.source.sql.SqlEventStore
import javax.sql.DataSource

class PostService(
    dataSource: DataSource,
    kafkaBootstrapServers: String
) {
    private val eventStore = SqlEventStore<String, PostEvent, Post>(
        name = "post",
        pkColumns = listOf("post_id"),
        writePkValues = { iterator, ps, pk -> ps.setString(iterator.next(), pk) },
        readPk = { rs -> rs.getString("post_id") },
        objectMapper = EventSourceObjectMapper.objectMapper,
        dataSource = dataSource
    )

    private val postSummaryViewHolder = EventViewHolder(
        eventView = PostSummaryView(
            pubSub = KafkaPubSub(bootstrapServers = kafkaBootstrapServers),
            eventStore = eventStore,
            dataSource = dataSource
        )
    )

    private val postEventSource = EventSource(
        pkProvider = UuidPkProvider,
        commandHandler = PostCommandHandler,
        eventReducer = PostReducer,
        eventStore = eventStore
    )

    fun post(authorMail: String, title: String, content: String): Post {
        val pk = postEventSource.generateNewKey()

        return postEventSource.execute(
            pk = pk,
            commands = listOf(
                CreatePost(
                    id = pk,
                    authorMail = authorMail,
                    title = title,
                    content = content,
                    creationTime = System.currentTimeMillis()
                )
            )
        )!!
    }

    fun updatePost(postId: String, authorMail: String, title: String, content: String): Post? {
        return getPost(postId = postId, authorMail = authorMail)?.let {
            postEventSource.execute(
                pk = postId,
                commands = listOf(UpdatePost(title = title, content = content))
            )
        }
    }

    fun getPost(postId: String, authorMail: String): Post? {
        return postEventSource.get(
            pk = postId,
            filter = { post: Post -> post.authorMail == authorMail && !post.deleted })
    }

    fun deletePost(postId: String, authorMail: String, reason: String?): Boolean {
        return getPost(postId = postId, authorMail = authorMail)?.let {
            postEventSource.execute(
                pk = postId,
                commands = listOf(DeletePost(reason = reason))
            )
        } != null
    }

    fun getPostsSummaries(authorMail: String): List<PostSummary> {
        return postSummaryViewHolder.get().getPostsSummaries(authorMail = authorMail)
    }
}