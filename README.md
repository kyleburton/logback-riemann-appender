# logback-riemann-appender

Forked from: https://github.com/kyleburton/logback-riemann-appender

## Logback appender for [Riemann](http://riemann.io/):

- Sends logging events to Riemann via UDP
- Maps attributes of the log event to a Riemann event as follows:

Log Event                       | Riemann Event
------------------------------- | -------------
rendered message                | `:description`
`level`                         | `:state`
Marker name                     | added to `:tags`
each key-value pair in the MDC  | added as a custom attribute
throwableProxy, if it exists    | custom attribute: `:stacktrace`
name of the logger              | custom attribute: `:logger`

:host and :service are set via configuring `logback.xml` as below.

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
