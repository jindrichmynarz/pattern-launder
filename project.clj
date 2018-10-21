(defproject pattern-launder "0.1.0-SNAPSHOT"
  :description "Get instances of RDF triple patterns from LOD Laundromat"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.4.1"]
                 [cheshire "5.8.1"]
                 [com.github.jsonld-java/jsonld-java "0.12.1"]
                 [slingshot "0.12.2"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [com.taoensso/timbre "4.10.0"]]
  :main pattern-launder.cli)
