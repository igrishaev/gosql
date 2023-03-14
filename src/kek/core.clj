(ns kek.core
  (:import
   java.util.Map
   java.util.List
   java.util.ArrayList
   )
  (:require
   [selmer.parser :as parser]
   )
  (:gen-class))


(parser/set-resource-path! (clojure.java.io/resource "sql"))

#_
(parser/render-file "get-user-by-id.sql" {:id 1})


(def ^:dynamic ^Map *context* nil)
(def ^:dynamic ^List *params* nil)

(defmacro update! [value func & args]
  `(set! ~value (~func ~value ~@args)))


(defn param [p]
  (let [value (get *context* p)]
    (.add *params* value)
    #_(update! *params* conj value)
    "?"))


(defn foobar [context]
  (binding [*context* context
            *params* (new ArrayList)]

    (doseq [[k _] context]
      (param k))

    *params*))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
