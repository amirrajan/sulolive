(ns eponai.server.log
  (:require
    [taoensso.timbre :as timbre :refer [debug]]
    [clojure.core.async :as async]
    [eponai.common.format.date :as date]
    [com.stuartsierra.component :as component]
    [clojure.string :as string])
  (:import (java.io StringWriter PrintWriter LineNumberReader StringReader)))

(defprotocol ILoggerFactory
  (make-logger [this] "Returns a new logger"))

(defprotocol ILogger
  (log [this msg]))

(defprotocol IBulkLogger
  (log-bulk [this messages])
  (bulk-size [this]))

;; ------- Log manipulation functions

(deftype SkipMessage [])

(def skip-message (SkipMessage.))

(defn with [logger f]
  (reify
    ILogger
    (log [_ msg]
      (let [m (f (:data msg))]
        (when-not (identical? skip-message m)
          (log logger (assoc msg :data m)))))))

(defn- logging-thread [logger in]
  (let [bulk? (satisfies? IBulkLogger logger)]
    (async/thread
      (loop []
        (when-some [v (if bulk?
                        (into []
                              (comp (take-while some?)
                                    (take (bulk-size logger)))
                              (cons (async/<!! in)
                                    (repeatedly #(async/poll! in))))
                        (async/<!! in))]
          (try
            (if bulk?
              (log-bulk logger v)
              (log logger v))
            (catch Throwable e
              (try
                (timbre/error ::async-error
                              e
                              {:exception-class   (.getClass e)
                               :exception-message (.getMessage e)})
                (catch Throwable _))))
          (recur))))))

(defn async-logger
  ([logger]
   (async-logger logger 1))
  ([logger threads]
   (let [capacity 100
         buf (async/sliding-buffer capacity)
         msg-chan (async/chan buf)]
     (doall (take threads (repeatedly #(logging-thread logger msg-chan))))
     (reify
       ILogger
       (log [_ msg]
         (when (== capacity (count buf))
           (timbre/debug "logging capacity has been reached. Will drop message."))
         (async/put! msg-chan msg))
       component/Lifecycle
       (stop [this]
         (async/close! msg-chan))))))

;; ------ Logging functions

(defn- log! [level logger id data]
  {:pre [(and (or (keyword? id) (symbol? id))
              (some? (namespace id)))
         (map? data)]}
  (log logger {:id          id
               :level       level
               :data        data
               :millis (System/currentTimeMillis)}))

(defn info! [logger id data]
  (log! :info logger id data))

(defn warn! [logger id data]
  (log! :warn logger id data))

(defn error! [logger id data]
  (log! :error logger id data))

;; ------ Loggers

(def no-op-logger (delay (timbre/debug "Using NoOpLogger")
                         (reify
                           ILogger
                           (log [_ _]))))

(defrecord TimbreLogger []
  ILogger
  (log [this msg]
    (timbre/log (:level msg) (:id msg) " " (:data msg))))

;; ------ Helpers

(defn- clean-ex-data
  "Returns only first level atomic values from the ex-data map."
  [data]
  (when (map? data)
    (into {}
          (filter (fn [kv]
                    (let [v (val kv)]
                      (or (string? v)
                          (number? v)
                          (keyword? v)
                          (boolean? v)
                          (symbol? v)
                          (nil? v)))))
          data)))

;; Inspired by:
;; https://github.com/kikonen/log4j-share/blob/master/src/main/java/org/apache/log4j/DefaultThrowableRenderer.java#L56
(defn- stacktrace-seq [^Throwable x]
  (let [sw (StringWriter.)
        pw (PrintWriter. sw)]
    (try
      (.printStackTrace x pw)
      (catch Exception _))
    (.flush pw)

    (let [reader (LineNumberReader. (StringReader. (.toString sw)))]
      (->> (repeatedly #(.readLine reader))
           (take-while some?)))))

(defn render-exception [^Throwable x]
  (cond-> {:exception-message (.getMessage x)
           :exception-class   (.getCanonicalName (.getClass x))
           :stacktrace        (string/join "\newline" (stacktrace-seq x))}
          (some? (ex-data x))
          (assoc :exception-data (clean-ex-data (ex-data x)))))