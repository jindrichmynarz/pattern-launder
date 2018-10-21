(ns pattern-launder.cli
  (:gen-class)
  (:require [pattern-launder.core :refer [triples]]
            [pattern-launder.io :refer [read-csv serialize]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as timbre] 
            [taoensso.timbre.appenders.core :as appenders]))

; ----- Private functions -----

(defn- long-str
  [& lines]
  (string/join \newline lines))

(defn- error-message
  [errors]
  (long-str "The following errors occurred while parsing your command:"
            \newline (apply long-str errors)))

(defn- die
  "Fail and exit with `message`."
  [message]
  (binding [*out* *err*] (println message))
  (System/exit 1))

(defn- info
  "Exit with `message`."
  [message]
  (println message)
  (System/exit 0))

(defn- usage
  [summary]
  (long-str "Retrieves instances of triple patterns from LOD Laundromat."
            "Reads triples patterns from standard input, formatted as CSV"
            "with subject,predicate,object header."
            "Writes results to standard output."
            "Options:" \newline summary))

(def cli-options
  [[nil "--counts" "Print counts of instances of triple patterns instead of data."
    :id ::counts]
   ["-v" "--verbose" "Switch on logging to standard error stream."
    :id ::verbose?]
   ["-h" "--help" "Display this help message"
    :id ::help?]])

(defn- main
  [{::keys [counts verbose?]}]
  ; Initialize logging to standard error stream.
  (when verbose?
    (timbre/merge-config! {:appenders {:println (appenders/println-appender {:stream :std-err})}}))
  (letfn [(get-triples [triple-pattern] (-> triple-pattern timbre/spy triples))]
    (some->> (io/reader *in*)
            read-csv
            (mapcat get-triples)
            (map serialize)
            dorun)))

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{::keys [help?]
          :as options} :options
         :keys [errors summary]} (parse-opts args cli-options)]
    (cond help? (info (usage summary))
          errors (die (error-message errors))
          :else (main options))))
