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


(defmacro makefn [name template]
  `(defn ~name []
     (str ~template "AAAAAA")))




(parser/add-tag!
 :query
 (fn [args context-map content]

   (let [fn-name
         (-> args first symbol)

         body
         (-> content :query :content)

         _ (println args context-map content)

         template
         (parser/parse parser/parse-input (new java.io.StringReader body) #_opts)]

     (intern *ns* fn-name
             (fn [context]
               (parser/render-template template context)))

     "OK"))
 :endquery)


#_
(parser/render "{% query my-foo %} {% verbatim %} test {{ id }} hello {% endverbatim %} {% endquery %}" {})

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
