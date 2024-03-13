package co.il.mh.event.source.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class UuidPkProviderTest {
    @Test
    fun `should produce unique uuids`() {
        val keys = mutableSetOf<UUID>()

        for (i in 0 until 50) {
            val key = UuidPkProvider.generateNewKey()
            Assertions.assertTrue(keys.add(UUID.fromString(key)))
        }

        Assertions.assertEquals(50, keys.size)
    }
}