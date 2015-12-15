package com.walmartlabs.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
import com.aphyr.riemann.client.SimpleUdpTransport;
import com.aphyr.riemann.client.SynchronousTransport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RiemannAppender<E> extends AppenderBase<E> {
  private static final String DEFAULT_PORT = "5555";
  private static final String DEFAULT_HOST = "localhost";
  private final String className = getClass().getSimpleName();

  private String serviceName = "*no-service-name*";
  private Level riemannLogLevel = Level.ERROR;
  private String riemannHostname = DEFAULT_HOST;
  private String riemannPort = DEFAULT_PORT;
  private String hostname = "*no-host-name*";
  private Map<String, String> customAttributes = new HashMap<String, String>();

  public static AtomicLong timesCalled = new AtomicLong(0);

  private static boolean debug = false;

  private RiemannClient riemannClient = null;

  public void start() {
    try {
      if (debug) {
        printError("%s.start()", this);
      }
      SynchronousTransport transport = new SimpleUdpTransport(riemannHostname, Integer.parseInt(riemannPort));
      riemannClient = new RiemannClient(transport);
      if (debug) {
        printError("%s.start: connecting", className);
      }
      riemannClient.connect();
      if (debug) {
        printError("%s.start: connected", className);
      }
    } catch (IOException ex) {
      if (debug) {
        printError("%s: Error initializing: %s", className, ex);
      }
      throw new RuntimeException(ex);
    }
    super.start();
  }

  public void stop() {
    if (debug) {
      printError("%s.stop()", this);
    }
    if (riemannClient != null) {
      try {
        riemannClient.disconnect();
      } catch (IOException ex) {
        // do nothing, it's ok
      }
    }
    super.stop();
  }

  public String toString() {
    return String.format(
      "RiemannAppender{hashCode=%s;serviceName=%s;riemannHostname=%s;riemannPort=%s;hostname=%s}",
      hashCode(),
      serviceName,
      riemannHostname,
      riemannPort,
      hostname);
  }

  private String getStackTraceFromEvent(ILoggingEvent logEvent) {
    String result = null;
    IThrowableProxy throwable = logEvent.getThrowableProxy();
    if (null != throwable) {
      String firstLine = String.format("%s: %s\n", throwable.getClassName(), throwable.getMessage());
      StringBuilder sb = new StringBuilder(firstLine);
      if (null != throwable.getStackTraceElementProxyArray()) {
        for (StackTraceElementProxy elt : throwable.getStackTraceElementProxyArray()) {
          sb.append("\t")
            .append(elt.toString())
            .append("\n");
        }
      }
      result = sb.toString();
    }
    return result;
  }

  // Invoked from the Clojure code, but is only used for testing.
  public void forceAppend(E event) {
    append(event);
  }

  boolean isMinimumLevel(ILoggingEvent logEvent) {
    return logEvent.getLevel().isGreaterOrEqual(riemannLogLevel);
  }

  protected synchronized void append(E event) {
    timesCalled.incrementAndGet();
    ILoggingEvent logEvent = (ILoggingEvent) event;

    if(debug) {
      printError("Original log event: %s", asString(logEvent));
    }

    if (isMinimumLevel(logEvent)) {
      EventDSL rEvent = createRiemannEvent(logEvent);
      try {
        try {
          if (debug) {
            printError("%s.append: sending riemann event: %s", className, rEvent);
          }
          rEvent.send();
          if (debug) {
            printError("%s.append(logEvent): sent to riemann %s:%s", className, riemannHostname, riemannPort);
          }
        } catch (Exception ex) {
          if (debug) {
            printError("%s: Error sending event %s", this, ex);
            ex.printStackTrace(System.err);
          }

          riemannClient.reconnect();
          rEvent.send();
        }
      } catch (Exception ex) {
        // do nothing
        if (debug) {
          printError("%s.append: Error during append(): %s", className, ex);
          ex.printStackTrace(System.err);
        }
      }
    }
  }

  private String asString(ILoggingEvent logEvent) {
    return String.format("LogEvent{level:%s, message:%s, logger:%s, thread:%s",
                         logEvent.getLevel().toString(),
                         logEvent.getMessage(),
                         logEvent.getLoggerName(),
                         logEvent.getThreadName());
  }

  private EventDSL createRiemannEvent(ILoggingEvent logEvent) {
    EventDSL event = riemannClient.event()
                                  .host(hostname)
	                          // timestamp is expressed in millis,
	                          // `time` is expressed in seconds
                                  .time(logEvent.getTimeStamp() / 1000)
                                  .description(logEvent.getMessage())
                                  .attribute("log/level", logEvent.getLevel().levelStr)
                                  .attribute("log/logger", logEvent.getLoggerName())
                                  .attribute("log/thread", logEvent.getThreadName())
	                          .attribute("log/message", logEvent.getMessage());

    if (logEvent.getThrowableProxy() != null) {
      event.attribute("log/stacktrace", getStackTraceFromEvent(logEvent));
    }
    if (logEvent.getMarker() != null) {
      event.tag("log/" + logEvent.getMarker().getName());
    }

    copyAttributes(event, logEvent.getMDCPropertyMap());
    copyAttributes(event, customAttributes);

    return event;
  }

  /**
   * Copy attributes out of the source and add them to `target`,
   * making sure to prefix the keys with `log/` -- this puts the
   * keywords in that namespace, preventing any collisions with the
   * Riemann schema.
   * @param target
   * @param source
   */
  private void copyAttributes(EventDSL target, Map<String, String> source) {
    for (String key : source.keySet()) {
       target.attribute("log/" + key, source.get(key));
    }
  }

  private void printError(String format, Object... params) {
    System.err.println(String.format(format, params));
  }

  public void setServiceName(String s) {
    serviceName = s;
  }

  public void setRiemannHostname(String s) {
    riemannHostname = s;
  }

  public void setRiemannPort(String s) {
    riemannPort = s;
  }

  public void setHostname(String s) {
    hostname = s;
  }

  public void setRiemannLogLevel(String s) {
    riemannLogLevel = Level.toLevel(s);
  }

  public void setCustomAttributes(String s) {
    customAttributes.putAll(parseCustomAttributes(s));
  }

  Map<String, String> parseCustomAttributes(String attributesString) {
    HashMap<String, String> result = new HashMap<String, String>();
    try {
      for (String kvPair : attributesString.split(",")) {
        String[] splitKvPair = kvPair.split(":");
        result.put(splitKvPair[0], splitKvPair[1]);
      }
    } catch (Throwable t) {
      printError("Encountered error while parsing attribute string: %s", attributesString);
    }
    return result;
  }

  public void setDebug(String s) {
    debug = "true".equals(s);
  }
}
