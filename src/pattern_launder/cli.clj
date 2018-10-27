(ns pattern-launder.cli
  (:gen-class)
  (:require [pattern-launder.core :refer [triples triple-count]]
            [pattern-launder.io :refer [read-csv serialize-csv serialize-jsonld]]
            [clojure.data.csv :as csv]
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
            (str "Options:" summary)))

(defn- add-triple-count
  [triple-pattern]
  (assoc triple-pattern :count (triple-count triple-pattern)))

(defn- main
  [{::keys [counts? verbose?]}]
  ; Initialize logging to standard error stream for verbose output.
  (timbre/merge-config! {:appenders {:println (if verbose?
                                                (appenders/println-appender {:stream :std-err})
                                                {:enabled? false})}})
  (timbre/info "Waiting for triple patterns from standard input...")
  (when-let [triple-patterns (-> *in* io/reader read-csv seq)]
    (if counts?
      (->> triple-patterns
           (map add-triple-count)
           serialize-csv
           (csv/write-csv *out*))
      (some->> triple-patterns
               (mapcat triples)
               (map (comp println serialize-jsonld))
               dorun)))
  (shutdown-agents))

(def cli-options
  [[nil "--counts" "Print estimated counts of instances of triple patterns instead of data."
    :id ::counts?]
   ["-v" "--verbose" "Switch on logging to standard error stream."
    :id ::verbose?]
   ["-h" "--help" "Display this help message"
    :id ::help?]])

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{::keys [help?]
          :as options} :options
         :keys [errors summary]} (parse-opts args cli-options)]
    (cond help? (info (usage summary))
          errors (die (error-message errors))
          :else (main options))))
