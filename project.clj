(defproject poc.cml "0.1.0-SNAPSHOT3"
  :description "Proof-of-Concept CML-style composable first-class events on top of core.async."
  :url "https://github.com/VesaKarvonen/poc.cml"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.5"]]
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :cljsbuild {:builds [{:source-paths ["src"]}]})
