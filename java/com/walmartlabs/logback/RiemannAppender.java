package com.walmartlabs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
import com.aphyr.riemann.client.SimpleUdpTransport;
import com.aphyr.riemann.client.SynchronousTransport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RiemannAppender<E> extends AppenderBase<E> {
  private static final String DEFAULT_PORT = "5555";
  private static final String DEFAULT_HOST = "localhost";

  private String serviceName     = "*no-service-name*";
  private String riemannHostname = DEFAULT_HOST;
  private String riemannPort     = DEFAULT_PORT;
  private String hostname        = "*no-host-name*";

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
        printError("%s.start: connecting", this);
      }
      riemannClient.connect();
      if (debug) {
        printError("%s.start: connected", this);
      }
    }
    catch (IOException ex) {
      if (debug) {
        printError("%s: Error initializing: %s", this, ex);
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
      }
      catch (IOException ex) {
        // do nothing, it's ok
      }
    }
    super.stop();
  }

  public String toString () {
    return String.format(
        "RiemannAppender{hashCode=%s;serviceName=%s;riemannHostname=%s;riemannPort=%s;hostname=%s}",
        hashCode(),
        serviceName,
        riemannHostname,
        riemannPort,
        hostname);
  }

  private String getStackTraceFromEvent( ILoggingEvent logEvent ) {
    String result = null;
    IThrowableProxy throwable = logEvent.getThrowableProxy();
    if (null != throwable) {
      String firstLine = String.format("%s: %s\n", throwable.getClassName(), throwable.getMessage());
      StringBuilder sb = new StringBuilder(firstLine);
      if (null != throwable.getStackTraceElementProxyArray()) {
        for ( StackTraceElementProxy elt : throwable.getStackTraceElementProxyArray() ) {
          sb.append("\t")
            .append(elt.toString())
            .append("\n");
        }
      }
      result = sb.toString();
    }
    return result;
  }

  protected synchronized void append(E event) {
    timesCalled.incrementAndGet();
    ILoggingEvent logEvent = (ILoggingEvent) event;
    EventDSL rEvent;
    try {
      rEvent = riemannClient.event().
        service(serviceName).
        host(hostname).
        state(logEvent.getLevel().levelStr).
        attribute("logger", logEvent.getLoggerName()).
        description(logEvent.getFormattedMessage());

      if (logEvent.getMarker() != null) {
        rEvent.tags(logEvent.getMarker().getName());
      }

      for (Map.Entry<String, String> entry : logEvent.getMDCPropertyMap().entrySet()) {
        rEvent.attribute(entry.getKey(), entry.getValue());
      }

      String stInfo = getStackTraceFromEvent(logEvent);
      if (null != stInfo) {
        rEvent.attribute("stacktrace", stInfo);
      }

      try {
        if (debug) {
          printError("%s.append: sending riemann event: %s", this, rEvent);
        }
        rEvent.send();
        if (debug) {
          printError("%s.append(logEvent): sent to riemann %s:%s", this, riemannHostname, riemannPort);
        }
      }
      catch (Exception ex) {
        if (debug) {
          printError("%s: Error sending event %s", this, ex);
          ex.printStackTrace(System.err);
        }

        riemannClient.reconnect();
        if (null != rEvent) {
          rEvent.send();
        }
      }
    }
    catch (Exception ex) {
      // do nothing
      if (debug) {
        printError("RiemannAppender.append: Error during append(): %s", ex);
        ex.printStackTrace(System.err);
      }
    }
  }

  private void printError(String format, Object... params) {
    System.err.println(String.format(format, params));
  }

  public void setServiceName (String s) {
    serviceName = s;
  }

  public void setRiemannHostname (String s) {
    riemannHostname = s;
  }

  public void setRiemannPort (String s) {
    riemannPort = s;
  }

  public void setHostname (String s) {
    hostname = s;
  }

  public void setDebug (String s) {
    debug = "true".equals(s);
  }
}
