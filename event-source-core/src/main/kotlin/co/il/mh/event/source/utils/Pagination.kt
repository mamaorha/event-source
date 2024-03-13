package co.il.mh.event.source.utils

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import java.util.stream.StreamSupport

object Pagination {
    fun <Data> stream(
        extractor: (pageNumber: Int, pageSize: Int) -> List<Data>,
        pageNumber: Int = 0,
        pageSize: Int = 500
    ): Stream<List<Data>> {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(object : Iterator<List<Data>> {
                val currPage = AtomicInteger(pageNumber)
                val nextResult = AtomicReference<List<Data>>()

                override fun hasNext(): Boolean {
                    if (nextResult.get() == null) {
                        nextResult.set(extractor(currPage.get(), pageSize))
                    }

                    return nextResult.get().isNotEmpty()
                }

                override fun next(): List<Data> {
                    if (!hasNext()) {
                        return nextResult.get()
                    }

                    val result = nextResult.get()
                    nextResult.set(null)

                    currPage.incrementAndGet()
                    return result
                }
            }, Spliterator.IMMUTABLE), false
        )
    }

    //this is useful when querying a db filtering by cursor, example: where id > cursor order by id asc
    fun <Data, Cursor> streamByCursor(
        extractor: (cursor: Cursor?) -> Pair<Cursor, List<Data>>,
        cursor: Cursor? = null
    ): Stream<Pair<Cursor, List<Data>>> {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(object : Iterator<Pair<Cursor, List<Data>>> {
                val currCursor = AtomicReference<Cursor>(cursor)
                val nextResult = AtomicReference<Pair<Cursor, List<Data>>>()

                override fun hasNext(): Boolean {
                    if (nextResult.get() == null) {
                        nextResult.set(extractor(currCursor.get()))
                    }

                    return nextResult.get().second.isNotEmpty()
                }

                override fun next(): Pair<Cursor, List<Data>> {
                    if (!hasNext()) {
                        return nextResult.get()
                    }

                    val result = nextResult.get()
                    currCursor.set(result.first)
                    nextResult.set(null)

                    return result
                }
            }, Spliterator.IMMUTABLE), false
        )
    }

    fun <Data> List<Data>.partition(pageNumber: Int = 0, pageSize: Int = 500): Stream<List<Data>> {
        return stream(
            extractor = { currPageNumber, _ ->
                this.subList(
                    (currPageNumber * pageSize).coerceAtMost(this.size),
                    ((currPageNumber + 1) * pageSize).coerceAtMost(this.size)
                )
            },
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }

    fun <Data> Stream<List<Data>>.flatten(): Stream<Data> {
        return this.flatMap { it.stream() }
    }
}