(ns simple-crud-jdbc.core
  (:require [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc])
  (:import [clojure.lang IPersistentMap IPersistentVector]))

(defn- add-where-item [where-vec [k v]] (conj where-vec [:= k v]))
(defn where-map [where]
  (cond
    (and (instance? IPersistentMap where) (seq where))
      (reduce add-where-item [:and] (seq where))
    (instance? IPersistentVector where) where
    :default true))

(defn- method-opts [opt]
  (cond
    (map? opt) [(or (opt :var) (throw (IllegalArgumentException.
                                        "Metadata must have a :var key")))
                (dissoc opt :var)]
    (symbol? opt) [opt {}]
    :default (throw (IllegalArgumentException.
                      (str "Method value must be a symbol, a metadata map, "
                           "or nil/false")))))
(def ^:private default-opts '{:create create!,
                              :read-one read-one, :read-all read-all
                              :update update!
                              :delete delete!
                              :connection nil})
(defn- with-conn [conn opts [args :as impl]]
  (let [[sym md] (method-opts opts)
        unbound-def `(defn ~sym ~md ~impl)]
    (if conn
      (let [bound-args (vec (rest args))
            bound-impl `(~bound-args (~sym ~conn ~@bound-args))]
        (concat unbound-def [bound-impl]))
      unbound-def)))

(defmacro crud-for [table & [opts]]
  (assert (keyword? table))
  (let [{:keys [create read-one read-all update delete connection]}
        (merge default-opts opts)]
    `(do
       ~(when create
          (with-conn connection create
            `([conn# attrs#] (first (jdbc/insert! conn# ~table attrs#)))))
       ~(when read-one
          (with-conn connection read-one
            `([conn# where#]
              (->> {:select [:*] :from [~table] :where (where-map where#)
                    :limit 1}
                   sql/format
                   (jdbc/query conn#)
                   first))))
       ~(when read-all
          (with-conn connection read-all
            `([conn# where#]
              (->> {:select [:*] :from [~table] :where (where-map where#)}
                   sql/format
                   (jdbc/query conn#)))))
       ~(when update
          (with-conn connection update
            `([conn# attrs# where#]
              (->> {:update ~table :set attrs# :where (where-map where#)}
                   sql/format
                   (jdbc/execute! conn#)
                   first))))
       ~(when delete
          (with-conn connection delete
            `([conn# where#]
              (->> {:delete-from ~table :where (where-map where#)}
                   sql/format
                   (jdbc/execute! conn#)
                   first))))
       )))
