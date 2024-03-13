package co.il.mh.event.source.jackson

import co.il.mh.event.source.annotations.Secret
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

class SecretModuleTest {
    private val secret = UUID.randomUUID().toString().substring(0, 16)

    data class Person(
        val firstName: String,
        val lastName: String,
        val age: Double,
        @Secret(property = "secret") val address: String, //masking field
        @Secret(property = "secret") val contactInfo: ContactInfo, //masking object
        @Secret(property = "secret") val siblings: List<Person> //masking list
    )

    data class ContactInfo(
        val emails: List<String>,
        val phone: String
    )

    init {
        System.setProperty("secret", secret)
    }

    private fun aPerson(withSiblings: Boolean = true): Person {
        return Person(
            firstName = UUID.randomUUID().toString(),
            lastName = UUID.randomUUID().toString(),
            age = Random.nextDouble(),
            address = UUID.randomUUID().toString(),
            contactInfo = aContactInfo(),
            siblings = if (withSiblings) listOf(aPerson(withSiblings = false)) else emptyList()
        )
    }

    private fun aContactInfo(): ContactInfo {
        return ContactInfo(
            emails = listOf("${UUID.randomUUID()}@gmail.com"),
            phone = "972500000000"
        )
    }

    @Test
    fun `secret values should be encrypted on write and decrypted on read`() {
        val objectMapper = EventSourceObjectMapper.objectMapper

        val person = aPerson()

        val personAsJsonString = objectMapper.writeValueAsString(person)

        Assertions.assertFalse(personAsJsonString.contains(person.address))

        person.contactInfo.emails.forEach { email ->
            Assertions.assertFalse(personAsJsonString.contains(email))
        }

        Assertions.assertFalse(personAsJsonString.contains(person.contactInfo.phone))

        person.siblings.forEach { sibling ->
            Assertions.assertFalse(personAsJsonString.contains(sibling.address))

            sibling.contactInfo.emails.forEach { email ->
                Assertions.assertFalse(personAsJsonString.contains(email))
            }

            Assertions.assertFalse(personAsJsonString.contains(sibling.contactInfo.phone))
        }

        val personFromJson = objectMapper.readValue(personAsJsonString, Person::class.java)
        Assertions.assertEquals(person, personFromJson)
    }
}