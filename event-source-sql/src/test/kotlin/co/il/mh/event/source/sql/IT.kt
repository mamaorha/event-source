package co.il.mh.event.source.sql

import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.containers.MariaDBContainer
import javax.sql.DataSource

object IT {
    val dataSource: DataSource

    init {
        dataSource = mariaDB()
    }

    private fun mariaDB(): DataSource {
        val mariaDbContainer = MariaDBContainer("${MariaDBContainer.NAME}:latest")
            .withDatabaseName("EventSource")
            .withUsername("root")
            .withPassword("")
            .withInitScript("schema.sql")

        mariaDbContainer.start()

        val host = mariaDbContainer.host
        val port = mariaDbContainer.firstMappedPort

        return MariaDbDataSource("jdbc:mariadb://$host:$port/EventSource").apply {
            this.user = "root"
            this.setPassword("")
        }
    }
}