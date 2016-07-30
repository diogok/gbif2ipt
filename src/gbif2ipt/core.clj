(ns gbif2ipt.core
  (:require [clojure.java.io :as io])
  (:require [clj-http.lite.client :as http])
  (:require [clojure.data.json :as json])
  (:require [clojure.core.async :refer [<! <!! >! >!! chan close! go-loop]])
  (:require [taoensso.timbre :as log])
  (:require [environ.core :refer [env]])
  (:require [dwc-io.gbif :as gbif])
  (:gen-class))

(def taxadata (or (env :taxadata-url) "http://taxadata/api/v2"))

(def data-dir (or (env :data-dir) "data"))

(def country  (or (env :country) "BR"))

(def user     (or (env :gbif-user) ""))
(def email    (or (env :gbif-email) ""))
(def password (or (env :gbif-password) ""))

(defn get-json
  [& url] 
  (log/info "Get JSON"(apply str url ))
  (try
    (:result (json/read-str (:body (http/get (apply str url))) :key-fn keyword))
    (catch Exception e 
      (log/warn (str "Failled get JSON " (apply str url)  (.getMessage e))))))

(defn sources->families
  [sources families]
    (go-loop [src (<! sources)]
      (when-not (nil? src)
        (log/info "Got source" src)
        (doseq [family (get-json taxadata "/" src "/families")]
          (>! families family))
         (recur (<! sources))))) 

(defn wait-and-download
  [dfile dkey]
    (log/info "Got download key " dkey)
    (loop []
      (let [r (http/get (gbif/download-url dkey) { :throw-exceptions false :as :stream})]
        (log/info (:status r))
        (if (not (= 404 (:status r)))
          (io/copy (:body r) (io/file data-dir (str dfile ".zip")))
          (do
            (Thread/sleep 30000)
            (recur))))))

(defn families->download
  [families done]  
    (go-loop [family (<! families)]
      (if (nil? family)
        (>! done true)
        (do
          (log/info "Got family" family)
          (let [taxon (-> (gbif/get-a-taxon family) :results first)]
            (wait-and-download
               (if (not (nil? country)) (str country "_" family) family)
               (gbif/request-download
                 {:user user 
                  :password password 
                  :email email
                  :filters (if-not (nil? country) 
                             [[:FAMILY_KEY :equals (:key taxon)]
                              [:COUNTRY :equals country]] 
                             [[:FAMILY_KEY :equals (:key taxon)]])})))
          (recur (<! families))))))

(defn run
  [] 
  (let [sources     (chan 1)
        families    (chan 1)
        done        (chan 1)]
  (sources->families sources families)
  (families->download families done)
  (doseq [source (get-json (str taxadata "/sources" ))]
    (>!! sources source))
  (<!! done)))

(defn wait-taxadata
  "Wait for Taxadata to be ready"
  []
  (let [done (atom false)]
    (while (not @done)
      (try 
        (log/info (str "Waiting: " taxadata))
        (let [r (http/get (str taxadata "/status") {:throw-exceptions false})]
          (if (= "done" (:status (json/read-str (:body r) :key-fn keyword)))
            (reset! done true)
            (do
              (log/info (json/read-str (:body r)))
              (Thread/sleep 1000))))
        (catch Exception e 
          (do
            (log/warn (.toString e))
            (Thread/sleep 1000)))))
    (log/info (str "Done: " taxadata))))

(defn -main 
  [ & args ]
  (log/info "Starting...")
  (log/info taxadata)
  (log/info data-dir)
  (log/info country)
  (log/info user)
  (spit (str data-dir "/.testwrite") "testwrite")
  (wait-taxadata)
  (log/info "Will start now")
  (run))

