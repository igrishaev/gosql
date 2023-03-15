(ns kek.core
  (:import
   java.io.StringReader
   java.io.Reader
   java.io.LineNumberReader
   java.util.Map
   java.util.List
   java.util.ArrayList
   )
  (:require
   [selmer.parser :as parser]
   )
  (:gen-class))


(parser/set-resource-path! (clojure.java.io/resource "sql"))


(def ^:dynamic ^Map *context* nil)
(def ^:dynamic ^List *params* nil)


(defn read-char ^Character [^Reader rdr]
  (let [ch-int (.read rdr)]
    (when-not (neg? ch-int)
      (char ch-int))))


(defn consume-query [^Reader rdr]
  (let [sb (new StringBuilder)]
    (loop []
      (if-let [ch (read-char rdr)]
        (do
          (.append sb ch)
          (if (clojure.string/ends-with? sb "{% endquery %}")
            (-> sb str (subs 0 (-> sb count (- 14))))
            (recur)))
        (str sb)))))


(defn query-handler [args tag-content render rdr]

  (println (type rdr) args tag-content)

  (let [payload
        (consume-query rdr)

        line
        (.getLineNumber ^LineNumberReader rdr)]

    (println payload)

    (let [[query-name]
          args

          func-name
          (-> query-name symbol)

          template
          (parser/parse parser/parse-input (new StringReader payload) #_opts)

          fn-var
          (intern *ns* func-name

                  (fn -query

                    ([db]
                     (-query db nil))

                    ([db context]
                     (binding [*context* context
                               *params* (new ArrayList)]

                       (let [query
                             (parser/render-template template context)

                             params
                             (vec *params*)]

                         [query params])))))]

      (alter-meta! fn-var assoc
                   :doc "foo kek 123"
                   :name func-name
                   :line line
                   :column 1
                   ;; :file "clojure/core.clj"
                   :arglists '([db]
                               [db context]))

      (fn [_]
        (println (format "function %s has been created" func-name))
        nil))))


(swap! selmer.tags/expr-tags
       assoc
       :query
       query-handler)


(swap! selmer.tags/closing-tags
       assoc
       :query
       [:endquery])


(parser/add-filter!
 "?" (fn [value]
       (.add *params* value)
       "?"))


#_
(declare-queries "{% query kek-aaa %}  test {{ id|? }} hello {% endquery %}   {% query kek-bbb %} SSS {{ foo }} XXX {% endquery %}")

(defn declare-queries [string]
  (parser/render-template (parser/parse parser/parse-input
                                        (-> string
                                            (StringReader.)
                                            (LineNumberReader.))
                                        #_opts)

                          #_context-map nil)
  #_
  (parser/render string nil))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
