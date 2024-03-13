package co.il.mh.event.source.utils

import co.il.mh.event.source.utils.Pagination.partition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

class PaginationTest {
    private fun aList(): List<String> {
        val result = mutableListOf<String>()

        for (i in 0 until 50) {
            result.add(UUID.randomUUID().toString())
        }

        return result
    }

    @Nested
    inner class Stream {
        @Test
        fun `stream should return paginated results`() {
            val list = aList()
            val pageNumber = 0
            val pageSize = 3

            val extractorInvocation = AtomicInteger(0)

            val result = Pagination.stream(
                extractor = { currPageNumber, currPageSize ->
                    extractorInvocation.incrementAndGet()
                    Assertions.assertEquals(pageSize, currPageSize)

                    val from = (currPageNumber * currPageSize).coerceAtMost(list.size)
                    val to = ((currPageNumber + 1) * currPageSize).coerceAtMost(list.size)

                    list.subList(from, to)
                },
                pageSize = pageSize,
                pageNumber = pageNumber
            ).toList()

            val expectedPages = ceil(list.size / 3f).toInt()

            //there is 1 addition call to see next page is empty
            Assertions.assertEquals(expectedPages, extractorInvocation.get() - 1)
            Assertions.assertEquals(expectedPages, result.size)
            Assertions.assertEquals(list, result.flatten())
        }

        @Test
        fun `stream should account page number`() {
            val list = aList()
            val pageNumber = 1
            val pageSize = 3

            val extractorInvocation = AtomicInteger(0)

            val result = Pagination.stream(
                extractor = { currPageNumber, currPageSize ->
                    extractorInvocation.incrementAndGet()
                    Assertions.assertEquals(pageSize, currPageSize)

                    val from = (currPageNumber * currPageSize).coerceAtMost(list.size)
                    val to = ((currPageNumber + 1) * currPageSize).coerceAtMost(list.size)

                    list.subList(from, to)
                },
                pageSize = pageSize,
                pageNumber = pageNumber
            ).toList()

            val expectedPages = ceil(list.size / 3f).toInt() - 1

            //there is 1 addition call to see next page is empty
            Assertions.assertEquals(expectedPages, extractorInvocation.get() - 1)
            Assertions.assertEquals(expectedPages, result.size)
            Assertions.assertEquals(list.subList(3, list.size), result.flatten())
        }
    }

    @Nested
    inner class StreamByCursor {
        @Test
        fun `stream should return paginated results`() {
            val list = (0 until 100).toList()
            val limit = 3
            val extractorInvocation = AtomicInteger(0)

            val result = Pagination.streamByCursor<Int, Int>(
                extractor = { cursor ->
                    extractorInvocation.incrementAndGet()

                    val cursorValue = cursor ?: -1

                    val result = list.filter { it > cursorValue }.take(limit)
                    val newCursor = if (result.isNotEmpty()) result.last() else cursor ?: -1
                    newCursor to result
                },
                cursor = null
            ).toList()

            val expectedPages = ceil(list.size / 3f).toInt()

            //there is 1 addition call to see next page is empty
            Assertions.assertEquals(expectedPages, extractorInvocation.get() - 1)
            Assertions.assertEquals(expectedPages, result.size)
            Assertions.assertEquals(list, result.map { it.second }.flatten())
        }

        @Test
        fun `stream should account cursor`() {
            val list = (0 until 100).toList()
            val limit = 3
            val extractorInvocation = AtomicInteger(0)

            val result = Pagination.streamByCursor(
                extractor = { cursor ->
                    extractorInvocation.incrementAndGet()

                    val cursorValue = cursor ?: -1

                    val result = list.filter { it > cursorValue }.take(limit)
                    val newCursor = if (result.isNotEmpty()) result.last() else cursor ?: -1
                    newCursor to result
                },
                cursor = 2
            ).toList()

            val expectedPages = ceil(list.size / 3f).toInt() - 1

            //there is 1 addition call to see next page is empty
            Assertions.assertEquals(expectedPages, extractorInvocation.get() - 1)
            Assertions.assertEquals(expectedPages, result.size)
            Assertions.assertEquals(list.subList(3, list.size), result.map { it.second }.flatten())
        }
    }

    @Nested
    inner class Partition {
        @Test
        fun `should create partition based on page size`() {
            val list = aList()

            val partitioned = list.partition(
                pageNumber = 0,
                pageSize = 1
            ).toList()

            Assertions.assertEquals(list.size, partitioned.size)
            Assertions.assertEquals(list, partitioned.flatten())
        }

        @Test
        fun `should create partition accounting page number`() {
            val list = aList()

            val partitioned = list.partition(
                pageNumber = 1,
                pageSize = 1
            ).toList()

            Assertions.assertEquals(list.size - 1, partitioned.size)
            Assertions.assertEquals(list.subList(1, list.size), partitioned.flatten())
        }

        @Test
        fun `should create partition accounting page size`() {
            val list = aList()

            val partitioned = list.partition(
                pageNumber = 0,
                pageSize = list.size
            ).toList()

            Assertions.assertEquals(1, partitioned.size)
            Assertions.assertEquals(list, partitioned.flatten())
        }
    }
}