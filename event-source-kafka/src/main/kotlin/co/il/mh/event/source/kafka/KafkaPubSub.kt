package co.il.mh.event.source.kafka

import co.il.mh.event.source.core.PubSub
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaPubSub(private val bootstrapServers: String) : PubSub, AutoCloseable {
    private val producer = Result.run {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

        KafkaProducer<String, String>(props)
    }
    private val consumers = mutableListOf<KafkaConsumer<String, String>>()
    private val consumerThreads = mutableListOf<Thread>()

    override fun publish(topic: String, partitionKey: String, message: String) {
        val record = ProducerRecord(topic, partitionKey, message)
        producer.send(record)
    }

    override fun registerConsumer(topic: String, group: String, handler: (message: String) -> Unit) {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.GROUP_ID_CONFIG] = group
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(topic))

        consumers.add(consumer)

        consumerThreads.add(Thread.startVirtualThread {
            val isAlive = AtomicBoolean(true)

            while (isAlive.get()) {
                val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(100))
                records.forEach {
                    try {
                        handler(it.value())
                    } catch (e: InterruptedException) {
                        isAlive.set(false)
                    } catch (_: Exception) {
                    }
                }

                consumer.commitAsync()
            }
        })
    }

    override fun close() {
        producer.close()
        consumers.forEach { it.close() }
        consumerThreads.forEach { it.interrupt() }
    }
}