(ns kek.core
  (:import
   java.util.regex.Pattern
   java.io.StringReader
   java.io.Reader
   java.io.File
   java.io.LineNumberReader
   java.util.Map
   clojure.lang.Namespace
   java.util.List
   javax.sql.DataSource
   java.util.ArrayList)
  (:require
   [kek.quote :as quote]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [selmer.parser :as parser]))


(def ^:dynamic ^Map *context* nil)
(def ^:dynamic ^List *params* nil)
(def ^:dynamic ^String *file-name* nil)
(def ^:dynamic ^List *functions* nil)
(def ^:dynamic ^Namespace *namespace* nil)
(def ^:dynamic ^DataSource *datasource* nil)


(defn join-comma [coll]
  (str/join ", " coll))


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

        (= arg ":pg")
        (recur (assoc acc :quote :pg) args)

        (= arg ":ansi")
        (recur (assoc acc :quote :ansi) args)

        (= arg ":mysql")
        (recur (assoc acc :quote :mysql) args)

        (= arg ":mssql")
        (recur (assoc acc :quote :mssql) args)

        (= arg ":count")
        (recur (assoc acc :count? true) args)

        (or (= arg ":1") (= arg ":one"))
        (recur (assoc acc :one? true) args)

        (= arg ":as-maps")
        (recur (assoc acc
                      :builder-fn
                      jdbc.rs/as-maps)
               args)

        (= arg ":as-unqualified-maps")
        (recur (assoc acc
                      :builder-fn
                      jdbc.rs/as-unqualified-maps)
               args)

        (= arg ":as-unqualified-kebab-maps")
        (recur (assoc acc
                      :builder-fn
                      jdbc.rs/as-unqualified-kebab-maps)
               args)

        (= arg ":as-arrays")
        (recur (assoc acc
                      :builder-fn
                      jdbc.rs/as-arrays)
               args)

        (= arg ":doc")
        (let [[doc & args] args]
          (if (string? doc)
            (let [doc-clean
                  (clean-docstring doc)]
              (recur (assoc acc :doc doc-clean) args))
            (error! format "the :doc arg %s is not a string!" arg)))

        :else
        (recur acc args))

      acc)))



(defn datasource? [x]
  (instance? javax.sql.DataSource x))


(defn query-handler [args tag-content render rdr]

  (let [line
        (.getLineNumber ^LineNumberReader rdr)

        payload
        (-> rdr
            read-query
            str/trim)]

    (let [[query-name & args-rest]
          args

          {:keys [one?
                  doc
                  builder-fn
                  count?]
           quote-type :quote}
          (args->opts args-rest)

          func-name
          (-> query-name symbol)

          vars
          (->> payload
               (re-seq #"(?m)\{\{\s*([^|\} ]+)")
               (mapv second)
               (mapv symbol)
               (set))

          ;; TODO: {% ? sku %}

          template
          (parser/parse parser/parse-input (new StringReader payload))

          jdbc-func
          (if one?
            jdbc/execute-one!
            jdbc/execute!)

          jdbc-opt
          (cond-> nil
            builder-fn
            (assoc :builder-fn builder-fn))

          DB
          *datasource*

          fn-var
          (intern *namespace* func-name

                  (fn -query

                    ([]
                     (if DB
                       (-query DB nil)
                       (error! "the default data source is not set")))

                    ([arg]
                     (if (datasource? arg)
                       (-query arg nil)
                       (if DB
                         (-query DB arg)
                         (error! "the default data source is not set"))))

                    ;; the main entry point
                    ([db {:keys [debug?] :as context}]
                     (binding [*context* context
                               *params* (new ArrayList)]

                       (quote/with-quote-type quote-type

                         (let [query
                               (parser/render-template template context)

                               _
                               (when debug?
                                 (println query))

                               query-vec
                               (into [query] (vec *params*))

                               result
                               (jdbc-func db query-vec jdbc-opt)]

                           (if count?
                             (-> result first :next.jdbc/update-count)
                             result)))))))]

      (.add *functions* fn-var)

      (alter-meta! fn-var assoc
                   :doc doc
                   :name func-name
                   :line line
                   :column 1
                   :file *file-name*
                   :arglists
                   (list []
                         [{:as 'context
                           :keys (into ['debug?] vars)}]
                         ['db]
                         ['db {:as 'context
                               :keys (into ['debug?] vars)}]))

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
 :? (fn [value]
      (.add *params* value)
      "?"))


(parser/add-filter!
 :! (fn [value]
      (when (nil? value)
        (error! "one of the required parameters is nil"))
      (.add *params* value)
      "?"))


(parser/add-tag!
 :! (fn [[arg] context]
      (if-some [value (get context (keyword arg))]
        (do
          (.add *params* value)
          "?")
        (error! "the `%s` required parameter is nil" arg))))


(parser/add-tag!
 :? (fn [[arg] context]
      (let [value
            (get context (keyword arg))]
        (.add *params* value)
        "?")))


(parser/add-filter!
 :SET (fn [mapping]
        (join-comma
         (for [[k v] mapping]
           (do
             (.add *params* v)
             (format "%s = ?" (name k)))))))


(parser/add-filter!
 :VALUES (fn [mapping]
           (join-comma
            (for [[k v] mapping]
              (do
                (.add *params* v)
                "?")))))


(defn wrap-brackets [content]
  (str \( content \)))


(parser/add-filter!
 :IN (fn [items]
       (wrap-brackets
        (join-comma
         (for [item items]
           (do
             (.add *params* item)
             "?"))))))


(parser/add-tag!
 :IN (fn [[arg] context]
       (let [items
             (get context (keyword arg))]
         (str "IN" \space
              (wrap-brackets
               (join-comma
                (for [item items]
                  (do
                    (.add *params* item)
                    "?"))))))))


(parser/add-filter!
 :MVALUES (fn [rows]
            (join-comma
             (for [row rows]
               (join-comma
                (for [[_ v] row]
                  (do
                    (.add *params* v)
                    "?")))))))

(parser/add-filter!
 :EXCLUDED (fn [mapping]
             (join-comma
              (for [[k v] mapping]
                (format "%s = EXCLUDED.%s"
                        (name k)
                        (name k))))))


(parser/add-filter!
 :FIELDS
 (fn [coll]
   (join-comma
    (for [item coll]
      (cond

        (map-entry? item)
        (-> item key name)

        (keyword? item)
        (name item)

        (string? item)
        item

        :else
        (error! "Wrong value type for comma join: %s" item))))))


(defn from-reader
  ([^Reader rdr]
   (from-reader rdr nil))
  ([^Reader rdr params]
   (binding [*functions* (new ArrayList)
             *namespace* (or (some-> params :ns the-ns)
                             *ns*)
             *datasource* (:db params)]
     (parser/render-template
      (parser/parse parser/parse-input
                    (new LineNumberReader rdr))
      nil)
     (vec *functions*))))


(defn from-file
  ([^File file]
   (from-file file nil))

  ([^File file options]
   (when-not (.exists file)
     (error! "file %s doesn't exist" (str file)))
   (binding [*file-name* (.getAbsolutePath file)]
     (from-reader (io/reader file) options))))


(defn from-string [string]
  (from-reader (new StringReader string)))


(defn from-resource
  ([path]
   (from-resource nil))

  ([path options]
   (from-file (-> path
                  (io/resource)
                  (or (error! "resource %s doesn't exist" path))
                  (io/file))
              options)))


(defn from-file-path
  ([path]
   (from-file-path path nil))

  ([path options]
   (from-file (io/file path))))


#_
(from-resource "queries.sql")

#_
(from-file-path "resources/queries.sql")
