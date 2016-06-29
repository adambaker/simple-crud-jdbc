(defproject simple-crud-jdbc "0.2.0"
  :description "Generate simple CRUD fns for any sql table"
  :url "https://github.com/adambaker/simple-crud-jdbc"
  :license {:name "Unlicense"
            :url "http://unlicense.org/"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [honeysql "0.7.0"]
                 [org.clojure/java.jdbc "0.6.1"]]

  :min-lein-version "2.0.0"
  :profiles
  {:dev [:project/dev :profiles/dev]
   :project/dev {:dependencies [[org.postgresql/postgresql "9.4.1208.jre7"]
                                [conman "0.5.8"]
                                [environ "1.0.3"]]
                 :plugins [[lein-environ "1.0.0"]]}}
  )
