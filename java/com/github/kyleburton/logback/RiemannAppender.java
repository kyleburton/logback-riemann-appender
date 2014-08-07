package com.github.kyleburton.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
import com.aphyr.riemann.client.SynchronousTransport;

import java.io.IOException;
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
        System.err.println(String.format("%s.start()", this));
      }
      SynchronousTransport transport = new SimpleUdpTransport(riemannHostname, Integer.parseInt(riemannPort));
      riemannClient = new RiemannClient(transport);
      if (debug) {
        System.err.println(String.format("%s.start: connecting", this));
      }
      riemannClient.connect();
      if (debug) {
        System.err.println(String.format("%s.start: connected", this));
      }
    }
    catch (IOException ex) {
      if (debug) {
        System.err.println(String.format("%s: Error initializing: %s", this, ex));
      }
      throw new RuntimeException(ex);
    }
    super.start();
  }

  public void stop() {
    if (debug) {
      System.err.println(String.format("%s.stop()", this));
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
    IThrowableProxy throwable = logEvent.getThrowableProxy();
    if ( null != throwable && null != throwable.getStackTraceElementProxyArray() ) {
      StringBuilder sb = new StringBuilder();
      for ( StackTraceElementProxy elt : throwable.getStackTraceElementProxyArray() ) {
        sb.append(elt.toString());
        sb.append("\n");
      }
      return sb.toString();
    }

    if (logEvent.getCallerData() != null) {
      if (debug) {
        System.err.println(String.format("%s.append: falling back to appender stacktrace: ", this));
      }
      StringBuilder sb = new StringBuilder();
      for ( StackTraceElement elt : logEvent.getCallerData()) {
        sb.append(elt);
        sb.append("\n");
      }
      return sb.toString();
    }

    return null;
  }

  protected synchronized void append(E event) /* throws LogbackException */ {
    timesCalled.incrementAndGet();
    ILoggingEvent logEvent = (ILoggingEvent) event;
    EventDSL rEvent;
    try {
      rEvent = riemannClient.event().
        service(serviceName).
        host(hostname).
        state("error").
        attribute("message", logEvent.getFormattedMessage());

      String stInfo = getStackTraceFromEvent(logEvent);
      if (null != stInfo) {
        rEvent.attribute("stacktrace", stInfo);
      }

      try {
        if (debug) {
          System.err.println(String.format("%s.append: sending riemann event: %s", this, rEvent));
        }
        rEvent.send();
        if (debug) {
          System.err.println(String.format("%s.append(logEvent): sent to riemann %s:%s", this, riemannHostname, riemannPort));
        }
      }
      catch (Exception ex) {
        if (debug) {
          System.err.println(String.format(
                "%s: Error sending event %s",
                this,
                ex));
          ex.printStackTrace();
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
        System.err.println(String.format("RiemannAppender.append: Error during append(): %s", ex));
        ex.printStackTrace();
      }
    }

    //System.err.println(String.format(
    //      "RiemannAppender{serviceName=%s;riemannHostname=%s;riemannPort=%s;hostname=%s}",
    //      serviceName,
    //      riemannHostname,
    //      riemannPort,
    //      hostname));

    //System.err.println("RiemannAppender: event: " + event);
    //System.err.println("RiemannAppender: event.getClass(): " + event.getClass());
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
