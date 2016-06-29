(ns simple-crud-jdbc.core-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [conman.core :refer [connect! disconnect! with-transaction]]
            [simple-crud-jdbc.core]))

(def ^:dynamic *db* nil)

(use-fixtures :once
  (fn [tests]
    (binding [*db* (connect! {:jdbc-url (env :db-url)})]
      (jdbc/db-do-commands *db*
                           (jdbc/create-table-ddl :fruit
                                                  [[:id "serial" :primary :key]
                                                   [:name "varchar(32)"]
                                                   [:appearance "varchar(32)"]
                                                   [:cost :int]]))
      (tests)
      (jdbc/db-do-commands *db* (jdbc/drop-table-ddl :fruit))
      (disconnect! *db*))))

(create-ns 'default-conn)
(create-ns 'default-no-conn)
(in-ns 'default-conn)
(simple-crud-jdbc.core/crud-for
  :fruit {:connection simple-crud-jdbc.core-test/*db*})
(in-ns 'default-no-conn)
(simple-crud-jdbc.core/crud-for :fruit)

(in-ns 'simple-crud-jdbc.core-test)

(use-fixtures :each (fn [tests]
                     (with-transaction [*db*]
                       (jdbc/db-set-rollback-only! *db*)
                       (tests))))

(deftest default-conn-create-read!
  (let [orange (default-conn/create!
                 {:name "orange", :appearance "orange", :cost 120})
        pear (default-conn/create!
               {:name "pear", :appearance "pear shaped", :cost 230})
        grape (default-conn/create!
                {:name "grape", :appearance "purple", :cost 210})
        grapefruit (default-conn/create!
                     {:name "grapefruit", :appearance "grapefruit shaped"
                      :cost 100})]
    (is (= (:count (first (jdbc/query *db* "select count(*) from fruit")))
           4))
    (is (= (default-conn/read-one {:id (orange :id)})
           orange))
    (is (= (set (default-conn/read-all [:or
                                        [:like :name "grape%"]
                                        [:like :appearance "%shaped"]]))
           #{pear grape grapefruit}))

    ;call in different txn, returns nil
    (is (= (default-conn/read-one {:connection-uri (env :db-url)}
                                  {:id (orange :id)})
           nil) "can still specify a connection to override default")
    ))

(deftest default-conn-update!
  (let [orange (default-conn/create!
                 {:name "orange", :appearance "orange", :cost 120})
        pear (default-conn/create! {:name "pear" :appearance "pear shaped"})
        grape (default-conn/create! {:name "grape"})]
    (is (= (default-conn/update! {:appearance "more of a yellowish-orange"}
                                 {:id (orange :id)})
           1))
    (is (= (default-conn/read-one {:name "orange"})
           (assoc orange :appearance "more of a yellowish-orange")))
    (is (= (default-conn/update! {:cost 100} {:cost nil})
           2))
    (is (= (set (default-conn/read-all true))
           #{(assoc orange :appearance "more of a yellowish-orange")
             (assoc pear :cost 100)
             (assoc grape :cost 100)}))
    (is (= (default-conn/update! {:appearance nil} [:<> :appearance nil])
           2))
    (is (= (set (default-conn/read-all true))
           #{(assoc orange :appearance nil)
             (assoc pear :cost 100 :appearance nil)
             (assoc grape :cost 100)}))
    ))

(deftest default-conn-delete!
  (let [orange (default-conn/create!
                 {:name "orange", :appearance "orange", :cost 120})
        pear (default-conn/create! {:name "pear" :appearance "pear shaped"})
        grape (default-conn/create! {:name "grape"})]
    (is (= (default-conn/delete! {:cost nil})
           2))
    (is (= (default-conn/read-all true) [orange]))
    (is (= (default-conn/delete! {:id (orange :id)})
           1))
    (is (= (default-conn/read-all true) []))))
