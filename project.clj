(defproject com.walmartlabs/logback-riemann-appender "0.1.4"
  :description "Logback Appender that sends events to Riemann"
  :url "https://github.com/walmartlabs/logback-riemann-appender"
  :lein-release         {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java"]
  :javac-options     ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :plugins [[lein-release/lein-release "1.0.5"]
            [lein-swank                "1.4.5"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  :dependencies [
    [org.clojure/clojure            "1.5.1"]
    [com.aphyr/riemann-java-client  "0.2.8"]
    [ch.qos.logback/logback-classic "1.0.13"]
    [org.clojure/tools.logging      "0.2.6"]
  ]
  :signing {:gpg-key "dante@walmartlabs.com"})
