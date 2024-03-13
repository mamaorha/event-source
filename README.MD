# Kotlin Event Source Library

This Kotlin Event Source Library provides support for managing event sourcing architectures. It facilitates the creation of event-driven systems by offering features such as snapshots, views, and the capability to encrypt sensitive fields within events.

## Features

- **Event Sourcing**: Implement event sourcing patterns to manage application state changes over time.
- **Snapshots**: Efficiently capture and restore the state of aggregates by utilizing snapshotting mechanisms.
- **Views**: Easily create materialized views for querying and reporting purposes based on event streams.
- **Encryption**: Secure sensitive data within events/snapshots/views using encryption mechanisms to protect sensitive information.

## Installation

You can include this library in your project by adding the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>co.il.mh</groupId>
    <artifactId>event-source-core</artifactId>
    <version>LATEST-SNAPSHOT</version>
</dependency>
```

for production use you may also choose implementations for the pub-sub and event-store persistence, example:

```xml
<dependencies>
    <dependency>
        <groupId>co.il.mh</groupId>
        <artifactId>event-source-sql</artifactId>
        <version>LATEST-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>co.il.mh</groupId>
        <artifactId>event-source-kafka</artifactId>
        <version>LATEST-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Examples

The codebase follows Test-Driven Development (TDD) practices, allowing you to observe the behavior of each component, understand how to initialize them, and anticipate their expected behavior.

For a practical demonstration of how to utilize this library, refer to the [event-source-example module](event-source-example). It offers clear insights into how to integrate and leverage the functionalities provided by this library.

## Contributing

Contributions to this library are welcomed! Feel free to submit issues, feature requests, or pull requests through our [GitHub repository](https://github.com/example/event-source-library).

## License

This library is licensed under the [MIT License](LICENSE).

---

**Note**: This library is intended to provide basic functionalities for event sourcing architectures in Kotlin. For advanced use cases or specific requirements, consider extending or customizing the provided functionality.