(ns eponai.common.validate
  (:require [clojure.string :as s])
  #?(:clj
      (:import [clojure.lang ExceptionInfo])))

(defn- error [msg data]
  (let [message (str "Validation failed, " msg)]
    (throw (ex-info message {:cause   ::validation-error
                             :data    data
                             :message message
                             #?@(:clj [:status :eponai.server.http/unprocessable-entity])}))))


(defn- validate
  "Validate with the given message and function on the input args.
  Throws an ex-info if validation failed."
  [msg f & args]
  (let [data {:fn (str f) :params (s/join " " args)}]
    (try
      (if (apply f args)
        true
        (error msg data))
      (catch #?(:clj ExceptionInfo
                :cljs cljs.core.ExceptionInfo) e
        (throw e))
      (catch #?(:clj Exception
                :cljs :default) e
        (error msg (assoc data :exception e))))))

(defn required-transaction-fields []
  #{:transaction/uuid
    :transaction/title
    :transaction/date
    :transaction/amount
    :transaction/currency
    :transaction/created-at
    :transaction/budget
    :transaction/type})

(defn transaction-keys
  "Assert that required fields are included in the transaction input.
  Returns a map with only transaction keys."
  [input]
  (let [required-fields (required-transaction-fields)
        missing-keys (filter #(nil? (get input %)) required-fields)]
    (assert (empty? missing-keys))
    (select-keys input (conj required-fields
                             :transaction/tags))))

;(defn input-transaction
;  "Given an input returns a transaction with valid keys."
;  [input]
;  (let [required-fields (required-transaction-fields)
;        transaction (select-keys input (conj required-fields
;                                       :transaction/tags))]
;    (assert (= (count required-fields) (count (select-keys input required-fields))))
;    (validate (str "input: " transaction " missing keys: " (filterv #(nil? (get transaction %)) required-fields))
;              every? #(some? (get transaction %)) required-fields)
;    transaction))