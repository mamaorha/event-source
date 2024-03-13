# Kotlin Event Source Library Kafka module

Implements pubSub with kafka

## Installation

You can include this module in your project by adding the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>co.il.mh</groupId>
    <artifactId>event-source-kafka</artifactId>
    <version>LATEST-SNAPSHOT</version>
</dependency>
```

### Kafka PubSub
To publish and consume event view messages, you'll need to create a Kafka topic named <b>'event-view'</b>. 
The number of partitions should be determined based on the concurrency requirement. 
Each view will publish to this topic using a partition key that corresponds to the view name. 
This approach enables asynchronous synchronization of the view.

## Examples

The codebase follows Test-Driven Development (TDD) practices, allowing you to observe the behavior of each component, understand how to initialize them, and anticipate their expected behavior.

For a practical demonstration of how to utilize this library, refer to the [event-source-example module](../event-source-example). It offers clear insights into how to integrate and leverage the functionalities provided by this library.

## Contributing

Contributions to this library are welcomed! Feel free to submit issues, feature requests, or pull requests through our [GitHub repository](https://github.com/example/event-source-library).

## License

This library is licensed under the [MIT License](LICENSE).

---

**Note**: This library is intended to provide basic functionalities for event sourcing architectures in Kotlin. For advanced use cases or specific requirements, consider extending or customizing the provided functionality.