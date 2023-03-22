(ns gosql.core
  (:import
   clojure.lang.Keyword
   clojure.lang.MapEntry
   clojure.lang.Namespace
   clojure.lang.Var
   java.io.File
   java.io.LineNumberReader
   java.io.Reader
   java.io.StringReader
   java.util.ArrayList
   java.util.List
   java.util.Map
   java.util.regex.Pattern
   javax.sql.DataSource)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [selmer.parser :as parser]))


(def ^:dynamic ^List *params* nil)
(def ^:dynamic ^String *file-name* nil)
(def ^:dynamic ^List *functions* nil)
(def ^:dynamic ^Namespace *namespace* nil)
(def ^:dynamic ^Var *db-var* nil)
(def ^:dynamic ^Keyword *quote-type* :ansi)


(defmacro with-params [& body]
  `(binding [*params* (new ArrayList)]
     ~@body))


(defn join-comma [coll]
  (str/join ", " coll))


(defn wrap-brackets [content]
  (str \( content \)))


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
        re #"(?s)(.+)\{\%\s+sql/endquery\s+\%\}$"]
    (loop []
      (if-let [ch (read-char rdr)]
        (do
          (.append sb ch)
          (if-let [[_ query] (re-matches re (str sb))]
            query
            (recur)))
        (error! "the reader has been closed before reaching the end of the 'sql/endquery' tag")))))


(defn clean-docstring [^String string]
  (cond-> string

    (str/starts-with? string "\"")
    (subs 1)

    (str/ends-with? string "\"")
    (subs 0 (-> string count dec dec))

    :then
    (str/trim)))


(defn args->opts [args]
  (loop [acc {}
         [^String arg & args] args]

    (case arg

      nil
      acc

      (":ansi" ":pg" ":sqlite")
      (recur (assoc acc :quote-type :ansi) args)

      ":mysql"
      (recur (assoc acc :quote-type :mysql) args)

      ":mssql"
      (recur (assoc acc :quote-type :mssql) args)

      ":count"
      (recur (assoc acc :count? true) args)

      (":1" ":one")
      (recur (assoc acc :one? true) args)

      ":as-maps"
      (recur (assoc acc
                    :builder-fn
                    jdbc.rs/as-maps)
             args)

      ":as-kebab-maps"
      (recur (assoc acc
                    :builder-fn
                    jdbc.rs/as-kebab-maps)
             args)

      ":as-unqualified-maps"
      (recur (assoc acc
                    :builder-fn
                    jdbc.rs/as-unqualified-maps)
             args)

      ":as-unqualified-kebab-maps"
      (recur (assoc acc
                    :builder-fn
                    jdbc.rs/as-unqualified-kebab-maps)
             args)

      ":as-arrays"
      (recur (assoc acc
                    :builder-fn
                    jdbc.rs/as-arrays)
             args)

      ":doc"
      (let [[doc & args] args]
        (if (string? doc)
          (let [doc-clean
                (clean-docstring doc)]
            (recur (assoc acc :doc doc-clean) args))
          (error! format "the :doc arg %s is not a string!" arg)))

      ;; else
      (recur acc args))))


(defn tag->arg ^String [^String tag-content]
  (some-> tag-content
          (str/trim)
          (str/split #"\s+")
          (second)
          (str/trim)))


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
                  quote-type
                  doc
                  builder-fn
                  count?]}
          (args->opts args-rest)

          func-name
          (-> query-name symbol)

          vars
          (->> payload
               (re-seq #"(?m)\{\{\s*([^|\} ]+)")
               (map second))

          tags
          (->> payload
               (re-seq #"(?m)\{%(.+?)%\}")
               (map second)
               (map tag->arg))

          context-keys
          (-> #{}
              (into vars)
              (into tags)
              (sort)
              (->>
               (remove nil?)
               (map symbol)))

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

          db-var
          *db-var*

          fn-var
          (intern *namespace* func-name

                  (fn -query

                    ([]
                     (if db-var
                       (-query @db-var nil)
                       (error! "the default data source is not set")))

                    ([arg]
                     (if db-var
                       (-query @db-var arg)
                       (-query arg nil)))

                    ([db {:keys [print?
                                 sqlvec?] :as context}]

                     (with-params

                       (binding [*quote-type* (or quote-type *quote-type*)]

                         (let [query
                               (parser/render-template template context)

                               _
                               (when print?
                                 (println query))

                               query-vec
                               (into [query] (vec *params*))]

                           (if sqlvec?

                             query-vec

                             (let [result
                                   (jdbc-func db query-vec jdbc-opt)]

                               (if count?
                                 (-> result first :next.jdbc/update-count)
                                 result)))))))))]

      (.add *functions* fn-var)

      (alter-meta! fn-var assoc
                   :doc doc
                   :name func-name
                   :line line
                   :column 1
                   :file *file-name*
                   :arglists
                   (list []
                         [{:as 'context}]
                         ['db]
                         ['db {:as 'context
                               :keys (into ['print? 'sqlvec?] context-keys)}]))

      (fn [_]
        nil))))


(swap! selmer.tags/expr-tags
       assoc
       :sql/query
       query-handler)


(swap! selmer.tags/closing-tags
       assoc
       :sql/query
       [:sql/endquery])


;;
;; Quoting
;;

(defn needs-quoting? ^Boolean [^String column]
  (some? (re-find #"-|/" column)))


(defn quote-with ^String [^String column ^String start ^String end]
  (when column
    (str start column end)))


(defn quote-column ^String [^String column]

  (case *quote-type*

    (:ansi :pg :sqlite)
    (quote-with column \" \")

    :mysql
    (quote-with column \` \`)

    :mssql
    (quote-with column \[ \])

    ;; else
    column))


(defn maybe-quote ^String [^String column]
  (if (needs-quoting? column)
    (quote-column column)
    column))


(defprotocol Utils
  (->column [x]))


(extend-protocol Utils

  Object
  (->column [x]
    (error! "cannot coerce value %s to a column" x))

  MapEntry
  (->column [x]
    (->column (key x)))

  String
  (->column [x]
    x)

  clojure.lang.Named
  (->column [x]
    (if-let [ns (namespace x)]
      (format "%s/%s" ns (name x))
      (name x))))


(def ->column&quote
  (comp maybe-quote ->column))


;;
;; Tags
;;

(defn get-arg-value! [context ^String arg]
  (let [value
        (get context (keyword arg) ::miss)]
    (if (identical? value ::miss)
      (error! "parameter `%s` is not set in the context" arg)
      value)))


(defn columns-handler
  [[^String arg] ^Map context]
  (let [columns (get-arg-value! context arg)]
    (when (empty? columns)
      (error! "empty columns `%s`: %s" arg columns))
    (join-comma
     (map ->column&quote columns))))


(parser/add-tag! :sql/cols columns-handler)


(defn columns*-handler
  [[^String arg] ^Map context]
  (let [^List rows (get-arg-value! context arg)]
    (when (empty? rows)
      (error! "empty rows `%s`: %s" arg rows))
    (join-comma
     (map ->column&quote (first rows)))))


(parser/add-tag! :sql/cols* columns*-handler)


(defn excluded-handler
  [[^String arg] ^Map context]
  (let [values (get-arg-value! context arg)]
    (when (empty? values)
      (error! "values `%s` are empty" arg))
    (join-comma
     (for [value values]
       (let [c (->column&quote value)]
         (format "%s = EXCLUDED.%s" c c))))))


(parser/add-tag! :sql/excluded excluded-handler)


(defn excluded*-handler
  [[^String arg] ^Map context]
  (let [^List rows (get-arg-value! context arg)]
    (when (empty? rows)
      (error! "excluded values `%s` are empty" arg))
    (join-comma
     (for [value (first rows)]
       (let [c (->column&quote value)]
         (format "%s = EXCLUDED.%s" c c))))))


(parser/add-tag! :sql/excluded* excluded*-handler)


(defn set-handler
  [[^String arg] ^Map context]
  (let [^Map value (get-arg-value! context arg)]
    (when (empty? value)
      (error! "columns `%s` are empty" arg))
    (join-comma
     (for [[k v] value]
       (do
         (.add *params* v)
         (format "%s = ?" (->column&quote k)))))))


(parser/add-tag! :sql/set set-handler)


(defn values-handler
  [[^Sting arg] ^Map context]
  (let [values
        (get-arg-value! context arg)]
    (when (empty? values)
      (error! "values `%s` are empty" arg))
    (join-comma
     (for [value values]
       (do
         (if (map-entry? value)
           (.add *params* (val value))
           (.add *params* value))
         "?")))))


(parser/add-tag! :sql/vals values-handler)


(defn values*-handler
  [[^Sting arg] ^Map context]
  (let [^List rows
        (get-arg-value! context arg)

        _
        (when (empty? rows)
          (error! "rows `%s` are empty" arg))

        row-first
        (first rows)

        fn-vals
        (if (map? row-first)
          (apply juxt (keys row-first))
          identity)]

    (join-comma
     (for [row rows]
       (let [row-vals (fn-vals row)]
         (wrap-brackets
          (join-comma
           (for [v row-vals]
             (do
               (.add *params* v)
               "?")))))))))


(parser/add-tag! :sql/vals* values*-handler)


(defn ?-handler
  [[^String arg] ^Map context]
  (let [value (get-arg-value! context arg)]
    (.add *params* value)
    "?"))


(parser/add-tag! :sql/? ?-handler)


(defn quote-handler
  [[^String arg ^String quote-type] ^Map context]
  (let [value (get-arg-value! context arg)]
    (if-let [qt (some-> quote-type keyword)]
      (binding [*quote-type* qt]
        (->column&quote value))
      (->column&quote value))))


(parser/add-tag! :sql/quote quote-handler)


(defn from-reader
  ([^Reader rdr]
   (from-reader rdr nil))
  ([^Reader rdr params]
   (binding [*functions* (new ArrayList)
             *namespace* (or (some-> params :ns the-ns)
                             *ns*)
             *db-var* (:db-var params)]
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


;;
;; Public API
;;

(defn from-resource

  {:arglists
   '([path]
     [path {:keys [ns db-var]}])}

  ([^String path]
   (from-resource path nil))

  ([^String path options]
   (from-file (-> path
                  (io/resource)
                  (or (error! "resource %s doesn't exist" path))
                  (io/file))
              options)))


(defn from-file-path

  {:arglists
   '([path]
     [path {:keys [ns db-var]}])}

  ([^String path]
   (from-file-path path nil))

  ([^String path options]
   (from-file (io/file path))))
