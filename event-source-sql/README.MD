# Kotlin Event Source Library SQL module

Implements persistence with sql

## Installation

You can include this module in your project by adding the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>co.il.mh</groupId>
    <artifactId>event-source-sql</artifactId>
    <version>LATEST-SNAPSHOT</version>
</dependency>
```

### Sql Event Store
in order to store events and their snapshots in sql you will need to create 2 tables per store.

in the below example the store name is <b>"example"</b> and the record identifier is <b>"pk"</b> from type <b>"bigint"</b>

<b>note you can have multiple fields that represents a "pk" and the type can vary.</b>

```sql
CREATE TABLE `example_events`
(
    `id`    BIGINT  NOT NULL AUTO_INCREMENT,
    `pk`    BIGINT  NOT NULL,
    `event` JSON    NOT NULL,
    `time`  BIGINT  NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `example_snapshots`
(
    `pk`        BIGINT  NOT NULL,
    `data`      JSON    NOT NULL,
    `cursor`    BIGINT  NOT NULL,
    PRIMARY KEY (`pk`)
);
```

### Sql Views
In order to keep track on the view cursor in sql you will need to create the following table

```sql
CREATE TABLE `views`
(
    `name`      VARCHAR(50) NOT NULL,
    `cursor`    BIGINT      NOT NULL,
    PRIMARY KEY (`name`)
);
```

This table will be used when extending SqlEventView for materialized views

## Examples

The codebase follows Test-Driven Development (TDD) practices, allowing you to observe the behavior of each component, understand how to initialize them, and anticipate their expected behavior.

For a practical demonstration of how to utilize this library, refer to the [event-source-example module](../event-source-example). It offers clear insights into how to integrate and leverage the functionalities provided by this library.

## Contributing

Contributions to this library are welcomed! Feel free to submit issues, feature requests, or pull requests through our [GitHub repository](https://github.com/example/event-source-library).

## License

This library is licensed under the [MIT License](LICENSE).

---

**Note**: This library is intended to provide basic functionalities for event sourcing architectures in Kotlin. For advanced use cases or specific requirements, consider extending or customizing the provided functionality.