package co.il.mh.event.source.example.service

import co.il.mh.event.source.example.data.Post
import co.il.mh.event.source.example.data.PostSummary
import co.il.mh.event.source.example.matchers.PostMatcher
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.hamcrest.comparator.ComparatorMatcherBuilder
import org.hamcrest.core.IsEqual
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

class PostServiceTest : PostMatcher {
    companion object {
        private val secret = UUID.randomUUID().toString().substring(0, 16)
        private val dataSource by lazy {
            val mariaDbContainer = MariaDBContainer("${MariaDBContainer.NAME}:latest")
                .withDatabaseName("EventSource")
                .withUsername("root")
                .withPassword("")
                .withInitScript("example-schema.sql")

            mariaDbContainer.start()

            val host = mariaDbContainer.host
            val port = mariaDbContainer.firstMappedPort

            MariaDbDataSource("jdbc:mariadb://$host:$port/EventSource").apply {
                this.user = "root"
                this.setPassword("")
            }
        }
        private val kafkaBootstrapServers by lazy {
            val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            kafkaContainer.start()

            // Get the bootstrap servers for connecting to Kafka
            val bootstrapServers = kafkaContainer.bootstrapServers

            // Create Kafka topics
            val adminProperties = Properties()
            adminProperties[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
            val adminClient = AdminClient.create(adminProperties)
            val newTopic = NewTopic("event-view", 4, 1)
            adminClient.createTopics(listOf(newTopic))

            bootstrapServers
        }

        private val postService = PostService(
            dataSource = dataSource,
            kafkaBootstrapServers = kafkaBootstrapServers
        )

        init {
            System.setProperty("MailSecret", secret)
        }

        private fun randomString(): String {
            return UUID.randomUUID().toString()
        }

        private fun createPosts(authorMail: String = UUID.randomUUID().toString()): List<Post> {
            val posts = mutableListOf<Post>()

            for (i in 0 until 10) {
                posts.add(createPost(authorMail = authorMail))
                Thread.sleep(1)
            }

            return posts
        }

        private fun createPost(authorMail: String = UUID.randomUUID().toString()): Post {
            return postService.post(
                authorMail = authorMail,
                title = randomString(),
                content = randomString()
            )
        }

        private fun Post.asSummary(): PostSummary {
            return PostSummary(
                id = this.id,
                authorMail = this.authorMail,
                title = this.title,
                updateTime = this.updateTime,
                creationTime = this.creationTime
            )
        }
    }

    @Test
    fun `create a new posts`() {
        val postIds = mutableSetOf<String>()

        for (i in 0 until 10) {
            val authorMail = randomString()
            val title = randomString()
            val content = randomString()
            val now = System.currentTimeMillis()

            Thread.sleep(1)

            val post = postService.post(
                authorMail = authorMail,
                title = title,
                content = content
            )

            post.mustMatch(
                postMatcher(
                    withAuthorMail = IsEqual(authorMail),
                    withTitle = IsEqual(title),
                    withContent = IsEqual(content),
                    withUpdateTime = IsEqual(post.creationTime),
                    withCreationTime = ComparatorMatcherBuilder.usingNaturalOrdering<Long>().greaterThan(now),
                    withDeleted = IsEqual(false)
                )
            )

            Assertions.assertTrue(postIds.add(post.id))
        }
    }

    @Nested
    inner class UpdatePost {
        @Test
        fun `update non-existing post should return null`() {
            Assertions.assertNull(
                postService.updatePost(
                    postId = randomString(),
                    authorMail = randomString(),
                    title = randomString(),
                    content = randomString()
                )
            )
        }

        @Test
        fun `should update existing post`() {
            val authorMail = randomString()
            val title = randomString()
            val content = randomString()

            val post = postService.post(
                authorMail = authorMail,
                title = title,
                content = content
            )

            val newTitle = randomString()
            val newContent = randomString()
            val timeAfterCreate = System.currentTimeMillis()

            Thread.sleep(1)

            val updatedPost = postService.updatePost(
                postId = post.id,
                authorMail = authorMail,
                title = newTitle,
                content = newContent
            )

            Assertions.assertNotNull(updatedPost)

            updatedPost?.mustMatch(
                postMatcher(
                    withId = IsEqual(post.id),
                    withAuthorMail = IsEqual(post.authorMail),
                    withTitle = IsEqual(newTitle),
                    withContent = IsEqual(newContent),
                    withUpdateTime = ComparatorMatcherBuilder.usingNaturalOrdering<Long>().greaterThan(timeAfterCreate),
                    withCreationTime = IsEqual(post.creationTime),
                    withDeleted = IsEqual(false)
                )
            )
        }
    }

    @Nested
    inner class GetPost {
        @Test
        fun `non-existing post should return null`() {
            Assertions.assertNull(
                postService.getPost(
                    postId = randomString(),
                    authorMail = randomString()
                )
            )
        }

        @Test
        fun `should get existing post`() {
            val post = postService.post(
                authorMail = randomString(),
                title = randomString(),
                content = randomString()
            )

            val fetchedPost = postService.getPost(
                postId = post.id,
                authorMail = post.authorMail
            )

            Assertions.assertNotNull(fetchedPost)

            fetchedPost?.mustMatch(IsEqual(post))
        }

        @Test
        fun `should get existing post after update`() {
            val post = postService.post(
                authorMail = randomString(),
                title = randomString(),
                content = randomString()
            )

            val updatedPost = postService.updatePost(
                postId = post.id,
                authorMail = post.authorMail,
                title = randomString(),
                content = randomString()
            )

            val fetchedPost = postService.getPost(
                postId = post.id,
                authorMail = post.authorMail
            )

            Assertions.assertNotNull(fetchedPost)

            fetchedPost?.mustMatch(IsEqual(updatedPost))
        }
    }

    @Nested
    inner class DeletePost {
        @Test
        fun `delete non-existing post should return false`() {
            Assertions.assertFalse(
                postService.deletePost(
                    postId = randomString(),
                    authorMail = randomString(),
                    reason = null
                )
            )
        }

        @Test
        fun `should delete existing post`() {
            val post = postService.post(
                authorMail = randomString(),
                title = randomString(),
                content = randomString()
            )

            Assertions.assertTrue(
                postService.deletePost(
                    postId = post.id,
                    authorMail = post.authorMail,
                    reason = null
                )
            )

            Assertions.assertNull(
                postService.getPost(
                    postId = post.id,
                    authorMail = post.authorMail
                )
            )
        }
    }

    @Nested
    inner class GetPostsSummaries {
        @Test
        fun `should return empty list for author without posts`() {
            createPosts()

            Assertions.assertEquals(
                emptyList<PostSummary>(),
                postService.getPostsSummaries(authorMail = randomString())
            )
        }

        @Test
        fun `should return author posts`() {
            createPosts()

            val authorMail = randomString()
            val posts = createPosts(authorMail = authorMail)

            val expected = posts.map { it.asSummary() }.reversed()
            Assertions.assertEquals(expected, postService.getPostsSummaries(authorMail = authorMail))
        }

        @Test
        fun `summary should be affected by updates`() {
            val post = createPost()

            Assertions.assertEquals(
                listOf(post.asSummary()),
                postService.getPostsSummaries(authorMail = post.authorMail)
            )

            val updatedPost = postService.updatePost(
                postId = post.id,
                authorMail = post.authorMail,
                title = randomString(),
                content = randomString()
            )

            Assertions.assertEquals(
                listOf(updatedPost?.asSummary()),
                postService.getPostsSummaries(authorMail = post.authorMail)
            )

            postService.deletePost(
                postId = post.id,
                authorMail = post.authorMail,
                reason = ""
            )

            Assertions.assertEquals(
                emptyList<PostSummary>(),
                postService.getPostsSummaries(authorMail = post.authorMail)
            )
        }
    }
}