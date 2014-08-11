# logback-riemann-appender

Forked from: https://github.com/kyleburton/logback-riemann-appender

## Logback appender for [Riemann](http://riemann.io/):

- Sends logging events to Riemann via UDP
- Sets the `:description` field of the Riemann event to the rendered log message
- Sets the `:state` field of the Riemann event to the `level` of the log event
- Sets the `:tags` field of the Riemann event to the name of the Marker on the log event, if it exists
- Sets an attribute on the Riemann event for every key-value pair in the MDC associated with the log event
- Sets a `:stacktrace` attribute if an exception is present on the log event
- Sets a `:logger` attribute to the name of the Logback logger

## Usage

Artifacts are available through
[clojars](https://clojars.org/com.walmartlabs/logback-riemann-appender) which you can add
to your maven repository like so:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

See `resources/logback.xml` for a full example configuration.

    <configuration scan="true">
      <appender name="R1" class="com.walmartlabs.logback.RiemannAppender">
        <serviceName>Test Service</serviceName>
        <riemannHostname>127.0.0.1</riemannHostname>
        <riemannPort>5555</riemannPort>
        <hostname>graphene</hostname>
      </appender>
      <root level="DEBUG">
        <appender-ref ref="R1"/>
      </root>
    </configuration>

## License

Copyright © 2013 Kyle Burton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
