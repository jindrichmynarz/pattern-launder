(ns pattern-launder.io
  (:require [clojure.data.csv :as csv]
            [clojure.string :as string]
            [slingshot.slingshot :refer [throw+]])
  (:import (java.io Reader)
           (com.github.jsonldjava.core JsonLdConsts JsonLdOptions JsonLdProcessor)))

(defn- cast-empty
  "Cast empty CSV `value` to nil."
  [value]
  (when (seq value) value))
  
(defn read-csv
  "Convert `csv` with a subject,predicate,object header to a lazy sequence of maps."
  [^Reader csv]
  (let [[head & data] (csv/read-csv csv)
        header [:subject :predicate :object]
        ->csv (comp (partial zipmap header)
                    (partial map cast-empty))]
    (when (not= (count head) 3)
      (throw+ {:type ::invalid-input
               :message "Input CSV has to have 3 columns for subject, predicate, and object."}))
    (map ->csv data)))

(defn serialize-csv
  [triple-patterns]
  (let [head (-> triple-patterns first keys)
        header (map name head)
        ->csv (apply juxt head)]
    (->> triple-patterns
         (map ->csv)
         (cons header))))

(def jsonld->nquads
  "Serialize `jsonld` to N-Quads."
  (let [options (JsonLdOptions.)]
    (set! (.format options) JsonLdConsts/APPLICATION_NQUADS)
    (fn [jsonld]
      (JsonLdProcessor/toRDF jsonld options))))

(def serialize-jsonld
  "Serialize JSON-LD objects to N-Quads and print."
  (comp string/trim jsonld->nquads))
