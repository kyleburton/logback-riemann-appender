(defn make-appender
  ([] (make-appender {}))
  ([options]
     (doto (com.walmartlabs.logback.RiemannAppender.)
       (.setRiemannHostname (or (:riemann-host options)
                                "localhost"))
       (.setRiemannPort (or (:riemann-port options)
                            "5555"))
       (.setHostname "repl-test-appender")
       (.setServiceName (or (:service options)
                            "repl-test-service"))
       (.setDebug "true")
       (.start))))

(defn make-marker
  []
  (org.slf4j.MarkerFactory/getMarker "repl-marker"))

(def logger (org.slf4j.LoggerFactory/getLogger "repl-logger"))

(defn make-event
  ([msg] (make-event msg {}))
  ([msg fields]
   (make-event ch.qos.logback.classic.Level/ERROR msg fields))
  ([level msg fields]
   (doto (ch.qos.logback.classic.spi.LoggingEvent. "fully-qualified-class-name"
                                                   logger
                                                   level
                                                   msg
                                                   nil
                                                   nil)
     (.setTimeStamp (System/currentTimeMillis))
     (.setMarker (make-marker))
     (.setMDCPropertyMap fields))))


(defn ship-event
  "Create a LoggingEvent with the specified message and optional
  fields, then create an instance of RiemannAppender and call
  `forceAppend` on it, passing the event.

  Note that `fields` must be a map of String:String."
  ([message]
   (ship-event message {}))
  ([message fields]
   (.forceAppend (make-appender) (make-event message fields))))

(comment
  "This user namespace exists to make it a bit easier to (manually)
  test that the log event is being encoded correctly. To do that, try
  this:

  - Fire up an instance of Riemann on your local machine. You'll
    probably want a really minimal number of streams, possibly as
    little as a single `prn`
  - Start up a REPL in this project
  - Use `(ship-event)` above to ship events to Riemann" )
