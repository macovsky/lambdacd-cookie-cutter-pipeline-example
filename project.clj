(defproject pipeline-templates "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :dependencies [[lambdacd "0.14.1"]
                           [lambdacd-git "0.4.1"]
                           [lambdaui "1.1.0"]
                           [ring-server "0.3.1"]
                           [ring "1.7.0"]
                           [ring/ring-defaults "0.3.2"]
                           [ring-oauth2 "0.1.4"]
                           [org.clojure/clojure "1.7.0"]
                           [org.clojure/tools.logging "0.3.0"]
                           [org.slf4j/slf4j-api "1.7.5"]
                           [hiccup "1.0.5"]
                           [hickory "0.7.1"]
                           [ch.qos.logback/logback-core "1.0.13"]
                           [ch.qos.logback/logback-classic "1.0.13"]
                           [clj-http "3.7.0"]]
            :profiles {:uberjar {:aot :all}}
            :main pipeline-templates.pipeline)
