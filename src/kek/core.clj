(ns kek.core
  (:import
   java.util.regex.Pattern
   java.io.StringReader
   java.io.Reader
   java.io.File
   java.io.LineNumberReader
   java.util.Map
   java.util.List
   java.util.ArrayList)
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [selmer.parser :as parser]))


(def ^:dynamic ^Map *context* nil)
(def ^:dynamic ^List *params* nil)
(def ^:dynamic ^String *file-name* nil)
(def ^:dynamic ^List *functions* nil)


(defn error!
  ([^String message]
   (throw (new Exception message)))

  ([^String template & args]
   (throw (new Exception ^String (apply format template args)))))


(defn read-char ^Character [^Reader rdr]
  (let [ch-int (.read rdr)]
    (when-not (neg? ch-int)
      (char ch-int))))


(defn read-query [^Reader rdr]
  (let [sb (new StringBuilder)
        re #"(?s)(.+)\{\%\s+endquery\s+\%\}$"]
    (loop []
      (if-let [ch (read-char rdr)]
        (do
          (.append sb ch)
          (if-let [[_ query] (re-matches re (str sb))]
            query
            (recur)))
        (error! "the reader has been closed before reaching the end of the query")))))


(defn clean-docstring [^String string]
  (cond-> string

    (str/starts-with? string "\"")
    (subs 1)

    (str/ends-with? string "\"")
    (subs 0 (-> string count dec dec))))


(defn args->opts [args]
  (loop [acc {}
         [arg & args] args]

    (if arg

      (cond

        (= arg ":1")
        (recur (assoc acc :one? true) args)

        (= arg ":doc")
        (let [[doc & args] args]
          (if (string? doc)
            (recur (assoc acc :doc (clean-docstring doc)) args)
            (error! format "the :doc arg %s is not a string!" arg)))

        :else
        (recur acc args))

      acc)))


(defn query-handler [args tag-content render rdr]

  (let [line
        (.getLineNumber ^LineNumberReader rdr)

        payload
        (read-query rdr)]

    (let [[query-name & args-rest]
          args

          {:keys [one? doc]}
          (args->opts args-rest)

          func-name
          (-> query-name symbol)

          template
          (parser/parse parser/parse-input (new StringReader payload))

          fn-var
          (intern *ns* func-name

                  (fn -query

                    ([db]
                     (-query db nil))

                    ([db context]
                     (binding [*context* context
                               *params* (new ArrayList)]
                       (let [query
                             (parser/render-template template context)]
                         (into [query] (vec *params*)))))))]

      (.add *functions* fn-var)

      (alter-meta! fn-var assoc
                   :doc doc
                   :name func-name
                   :line line
                   :column 1
                   :file *file-name*
                   :arglists '([db]
                               [db context]))

      (fn [_]
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


(defn from-reader [rdr]
  (binding [*functions* (new ArrayList)]
    (parser/render-template
     (parser/parse parser/parse-input
                   (new LineNumberReader rdr))
     nil)
    (vec *functions*)))


(defn from-file [^File file]
  (when-not (.exists file)
    (error! "file %s doesn't exist" (str file)))
  (binding [*file-name* (.getAbsolutePath file)]
    (from-reader (io/reader file))))


(defn from-string [string]
  (from-reader (new StringReader string)))


(defn from-resource [path]
  (from-file (-> path
                 (io/resource)
                 (or (error! "resource %s doesn't exist" path))
                 (io/file))))


(defn from-file-path [path]
  (from-file (io/file path)))


#_
(from-resource "queries.sql")

#_
(from-file-path "resources/queries.sql")
