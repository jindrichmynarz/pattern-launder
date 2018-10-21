(ns pattern-launder.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :refer [intersection]]
            [clojure.string :as string]
            [taoensso.timbre :refer [error]])
  (:import (java.net URL URLEncoder)))

; ----- Constants -----

(def r2d "http://index.lodlaundromat.org/r2d/")

(def ldf-ns "http://ldf.lodlaundromat.org/")

; ----- Private functions -----

(defn- url-encode
  [^String s]
  (URLEncoder/encode s "utf-8"))

(defn- serialize-param
  "Serialize HTTP query parameter."
  [[k v]]
  (str (name k) "=" (url-encode (str v))))

(defn- serialize-params
  "Serialize HTTP query parameters."
  [params]
  (when (seq params)
    (->> params
        (map serialize-param)
        (string/join "&")
        (str "?"))))

(defn- take-until
  "Returns a lazy sequence of successive items from coll until
   (pred item) returns true, including that item. pred must be
   free of side-effects."
  ; Taken from <https://groups.google.com/d/msg/clojure-dev/NaAuBz6SpkY/_aIDyyke9b0J>.
  [pred coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [[head & tail] s]
        (if (pred head)
          (list head)
          (cons head (take-until pred tail)))))))

(defn- parse-json
  [^String s]
  (json/parse-string s true))

(defn- resource->datasets'
  "Find a set of LOD Laundromat datasets containing a `resource`."
  [^String resource & {:keys [limit max-threshold]
                       :or {limit 10
                            max-threshold 10e4}}]
  (let [query-params {:limit limit
                      :uri resource}
        get-page (comp parse-json
                       slurp
                       io/as-url
                       (partial str r2d)
                       serialize-params
                       (partial assoc query-params :page))
        stop? (fn [{:keys [page pageSize totalResults]}]
                (or (> totalResults max-threshold)
                    (-> (dec page)
                        (* limit)
                        (+ pageSize)
                        (= totalResults))))
        results (->> (range)
                     rest
                     (map get-page)
                     (take-until stop?))]
    (when (-> results first :totalResults (< max-threshold))
      (->> results
           (mapcat :results)
           (map (partial str ldf-ns))
           (into #{})))))

(def resource->datasets
  (memoize resource->datasets'))

(defn- triple-pattern->datasets
  "Find datasets where `triple-pattern` appears."
  [{:keys [subject predicate object]
    :as triple-pattern}]
  (let [datasets (->> (vals triple-pattern)
                      (remove nil?)
                      (map resource->datasets))]
    (if (every? nil? datasets)
      (error (format "Pattern %s, %s, %s is too unspecific. Ignoring..." subject predicate object))
      (reduce intersection datasets))))

(defn- get-jsonld
  [^URL url]
  (let [conn (doto (.openConnection url)
               (.setRequestMethod "GET")
               (.setRequestProperty "Accept" "application/ld+json"))
        response (with-open [reader (io/reader (.getInputStream conn))]
                   (json/parse-stream reader))]
    (.disconnect conn)
    response))

(defn- hypermedia-controls?
  [{hypermedia-controls "@graph"
    named-graph "@id"}]
  (and hypermedia-controls named-graph (string/ends-with? named-graph "#metadata")))

(defn- split-hypermedia-controls
  "We assume that hypermedia controls are in the first named graph of `results`.
  Returns a collection starting with the hypermedia controls."
  [{graph "@graph"
    :as results}]
  (if (hypermedia-controls? results)
    [graph]
    (loop [[mid & tail] graph
           head (list)]
      (if (hypermedia-controls? mid)
        (cons (get mid "@graph") (concat head tail))
        (recur tail (cons mid head))))))

(defn- prefix
  [prefix-ns]
  (partial str prefix-ns))

(def hydra
  (prefix "http://www.w3.org/ns/hydra/core#"))

(defn- get-property
  "Get value of `property` in `data`."
  [data property]
  (letfn [(property-> [m] (get m property))]
    (->> data
         (map property->)
         (remove nil?)
         first)))

(defn- get-next-page
  "Extract URL of the next page of results."
  [hypermedia-controls]
  (some-> hypermedia-controls
          (get-property (hydra "nextPage"))
          (get "@id")
          URL.))

(defn- page-through
  "Page through LDF results starting from `url`."
  [^URL url]
  (let [[hypermedia-controls & data] (-> url get-jsonld split-hypermedia-controls)]
    (if-let [next-page (get-next-page hypermedia-controls)]
      (lazy-cat data (page-through next-page))
      data)))

(defn- triple-pattern->data
  "Retrieve instances of `triple-pattern` from an `ldf-endpoint`."
  [triple-pattern ldf-endpoint]
  (->> triple-pattern
       (remove (comp nil? second))
       (into {})
       serialize-params
       (str ldf-endpoint)
       URL.
       page-through))

(defn triples
  "Get RDF triples from LOD Laundromat for a given `triple-pattern`."
  [triple-pattern]
  (some->> (seq (triple-pattern->datasets triple-pattern))
           (map (partial triple-pattern->data triple-pattern))))

(comment
  (def data (triple-pattern->data "http://ldf.lodlaundromat.org/0054cf2f6fc701883bc8db19e969709a"
                                  {:subject nil 
                                   :predicate "http://www.w3.org/2000/01/rdf-schema#subClassOf"
                                   :object nil})))
