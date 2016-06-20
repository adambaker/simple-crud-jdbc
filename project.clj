(defproject simple-crud-jdbc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/adambaker/simple-crud-jdbc"
  :license {:name "Unlicense"
            :url "http://unlicense.org/"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.6.1"]]

  :min-lein-version "2.0.0"
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "9.4-1208"]
                                  [environ "1.0.3"]]}}
  )
