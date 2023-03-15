(ns kek.core
  (:import
   java.io.StringReader
   java.io.Reader
   java.io.LineNumberReader
   java.util.Map
   java.util.List
   java.util.ArrayList)
  (:require
   [selmer.parser :as parser]))


(parser/set-resource-path!
 (clojure.java.io/resource "sql"))


(def ^:dynamic ^Map *context* nil)
(def ^:dynamic ^List *params* nil)
(def ^:dynamic ^String *file-name* nil)


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


(defn args->opts [args]
  (loop [acc {}
         [arg & args] args]

    (if arg

      (cond

        (= arg ":1")
        (recur (assoc acc :one? true) args)

        (= arg ":doc")
        (let [[arg & args] args]
          (if (string? arg)
            (recur (assoc acc :doc arg) args)
            (throw (new Exception (format "The arg %s is not a string!" arg)))))

        :else
        (recur acc args))

      acc)))


(defn query-handler [args tag-content render rdr]

  (let [payload
        (consume-query rdr)

        line
        (.getLineNumber ^LineNumberReader rdr)]

    (println payload)

    (let [[query-name & args-rest]
          args

          {:keys [one? doc]}
          (args->opts args-rest)

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
                   :doc doc
                   :name func-name
                   :line line
                   :column 1
                   :file *file-name*
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


(defn declare-queries [string]
  (binding [*file-name* "resources/sql/get-user-by-id.sql"]
    (parser/render-template (parser/parse parser/parse-input
                                          (-> string
                                              (StringReader.)
                                              (LineNumberReader.))
                                          #_opts)

                            #_context-map nil)))



#_
(declare-queries "{% query kek-aaa %}  test {{ id|? }} hello {% endquery %}   {% query kek-bbb %} SSS {{ foo }} XXX {% endquery %}")
