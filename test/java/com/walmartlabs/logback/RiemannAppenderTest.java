package com.walmartlabs.logback;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.*;
import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RiemannAppenderTest {

  @Test
  public void parseCustomAttributesShouldCorrectlyParseAValidStringIntoAMap() throws Exception {
    String attributeString = "foo:bar,baz:quux";

    Map<String, String> result = getCustomAttributes(attributeString);

    assertEquals("bar", result.get("foo"));
    assertEquals("quux", result.get("baz"));
  }

  @Test
  public void parseCustomAttributesShouldCorrectlyParseKvPairsUntilItEncountersAnInvalidPair() throws Exception {

    String attributeString = "foo:bar,wheres-the-value";

    Map<String, String> customAttributes = getCustomAttributes(attributeString);

    assertEquals(1, customAttributes.keySet().size());
    assertEquals("bar", customAttributes.get("foo"));
  }

  @Test
  public void infoEventsExcluded() throws Exception {
    TestErrorEvent errorEvent = new TestErrorEvent();
    TestInfoEvent infoEvent = new TestInfoEvent();

    RiemannAppender<ILoggingEvent> appender = new RiemannAppender<ILoggingEvent>();

    assertFalse(appender.isMinimumLevel(infoEvent));
    assertTrue(appender.isMinimumLevel(errorEvent));
  }

  @Test
  public void infoEventsIncluded() throws Exception {
    TestErrorEvent errorEvent = new TestErrorEvent();
    TestInfoEvent infoEvent = new TestInfoEvent();

    RiemannAppender<ILoggingEvent> appender = new RiemannAppender<ILoggingEvent>();
    appender.setRiemannLogLevel("INFO");

    assertTrue(appender.isMinimumLevel(infoEvent));
    assertTrue(appender.isMinimumLevel(errorEvent));
  }

  private Map<String, String> getCustomAttributes(String attributeString) {
    return new RiemannAppender<ILoggingEvent>().parseCustomAttributes(attributeString);
  }

  private static class TestLoggingEvent extends LoggingEvent {
    public Level getLevel() { return Level.ALL; }
  }

  private static class TestErrorEvent extends LoggingEvent {
    public Level getLevel() { return Level.ERROR; }
  }

  private static class TestInfoEvent extends LoggingEvent {
    public Level getLevel() { return Level.INFO; }
  }

}
