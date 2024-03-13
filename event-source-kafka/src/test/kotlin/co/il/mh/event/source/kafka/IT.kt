package co.il.mh.event.source.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

object IT {
    val kafkaPubSub: KafkaPubSub

    init {
        val kafkaBootstrapServers = kafka()
        kafkaPubSub = KafkaPubSub(bootstrapServers = kafkaBootstrapServers)
    }

    private fun kafka(): String {
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

        return bootstrapServers
    }
}