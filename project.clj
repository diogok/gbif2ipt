(defproject gbif2ipt "0.0.1"
  :description "Download GBIF occurrences into local IPT"
  :url "http://github.com/diogok/gbif2ipt"
  :license {:name "MIT"}
  :main gbif2ipt.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [com.taoensso/timbre "4.3.1"]
                 [clj-http-lite "0.3.0"]
                 [environ "1.0.2"]
                 [dwc-io "0.0.58"]]
  :profiles {:uberjar {:aot :all}
             :jar {:aot :all}})
