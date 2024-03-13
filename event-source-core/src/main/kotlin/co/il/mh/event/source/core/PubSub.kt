package co.il.mh.event.source.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

//recommended to implement with kafka for prod
interface PubSub {
    fun publish(topic: String, partitionKey: String, message: String)
    fun registerConsumer(topic: String, group: String, handler: (message: String) -> Unit)
}

// this shouldn't be used in prod
object InMemoryPubSub : PubSub {

    private val topicAndPartitionLock = ConcurrentHashMap<Pair<String, String>, ReentrantLock>()
    private val consumers =
        ConcurrentHashMap<String, ConcurrentHashMap<String, (message: String) -> Unit>>() //topic to group to handlers

    override fun publish(topic: String, partitionKey: String, message: String) {
        val lock = topicAndPartitionLock.getOrPut(topic to partitionKey) { ReentrantLock() }

        lock.lock()

        try {
            consumers[topic]?.values?.parallelStream()?.forEach { handler ->
                handler(message)
            }
        } finally {
            lock.unlock()
        }
    }

    override fun registerConsumer(topic: String, group: String, handler: (message: String) -> Unit) {
        val topicHandlers = consumers.getOrPut(topic) { ConcurrentHashMap() }
        topicHandlers[group] = handler
    }
}