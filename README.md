# logback-riemann-appender

Logback appender for [Riemann](http://riemann.io/).  Sends logging events to Riemann over a UDP transport.  Sets a `message` attribute with the rendered log message and a `stacktrace` attribute if caller data (a stacktrace) was present on the logged event.

## Usage

    [com.github.kyleburton/logback-riemann-appender "0.1.0"]

See `resources/logback.xml` for a full example configuration.

    <configuration scan="true">
      <appender name="R1" class="com.github.kyleburton.logback.RiemannAppender">
        <ServiceName>Test Service</ServiceName>
        <RiemannHostname>127.0.0.1</RiemannHostname>
        <RiemannPort>5555</RiemannPort>
        <Hostname>graphene</Hostname>
      </appender>
      <root level="DEBUG">
        <appender-ref ref="R1"/>
      </root>
    </configuration>

## License

Copyright © 2013 Kyle Burton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
