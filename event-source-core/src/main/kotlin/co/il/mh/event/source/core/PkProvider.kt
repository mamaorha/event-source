package co.il.mh.event.source.core

import java.util.*

interface PkProvider<Pk> {
    fun generateNewKey(): Pk
}

object UuidPkProvider : PkProvider<String> {
    override fun generateNewKey(): String {
        return UUID.randomUUID().toString()
    }
}