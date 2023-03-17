(ns kek.quote
  (:require
   [selmer.parser :as parser]))


(def ^:dynamic *quote-type* nil)


(defmacro with-quote-type [quote-type & body]
  `(binding [*quote-type* ~quote-type]
     ~@body))


(defn quote-with [value start end]
  (when value
    [:safe
     (str start (name value) end)]))


(parser/add-filter!
 :quote (fn [value]
          (case *quote-type*

            (:ansi :pg)
            (quote-with value \" \")

            :mysql
            (quote-with value \` \`)

            :mssql
            (quote-with value \[ \])

            ;; else
            value)))


(parser/add-filter!
 :quote/ansi (fn [value]
               (quote-with value \" \")))


(parser/add-filter!
 :quote/pg (fn [value]
             (quote-with value \" \")))


(parser/add-filter!
 :quote/mysql (fn [value]
                (quote-with value \` \`)))


(parser/add-filter!
 :quote/mssql (fn [value]
                (quote-with value \[ \])))
