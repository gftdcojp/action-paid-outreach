(ns paid-outreach.contracts-test
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.test :refer [deftest is run-tests]]))

(def lexicon-files
  ["audience" "campaign" "creative" "performanceReport" "proposal" "spendRecord"])

(defn load-edn [path] (edn/read-string (slurp path)))
(defn lexicon [name] (load-edn (str "lex/" name ".edn")))
(defn properties [name] (get-in (lexicon name) [:defs :main :record :properties]))

(deftest lexicons-are-structured-single-form-edn
  (doseq [name lexicon-files]
    (let [value (lexicon name)]
      (is (map? value))
      (is (= 1 (:lexicon value)))
      (is (map? (:defs value)))
      (is (string? (:id value))))))

(deftest audience-contract-has-no-individual-targeting-surface
  (let [props (set (keys (properties "audience")))]
    (is (every? props [:cohortId :isco :country :source :consentDid]))
    (is (empty? (set/intersection
                 props
                 #{:person :individual :religion :ethnicity :health :politics
                   :retargetingId :trackingPixel})))))

(deftest actuation-and-spend-remain-human-authorized
  (is (contains? (properties "campaign") :approvedBy))
  (is (some #{"signedBy"} (get-in (lexicon "spendRecord")
                                   [:defs :main :record :required])))
  (is (some #{"governorVerdict"} (get-in (lexicon "proposal")
                                          [:defs :main :record :required]))))

(deftest domain-schema-is-edn-and-excludes-person-tracking
  (let [schema (load-edn "kotoba/schema.edn")
        idents (set (map :db/ident schema))]
    (is (vector? schema))
    (is (empty? (set/intersection
                 idents
                 #{:person/id :person/profile :person/location
                   :audience/tracking-pixel :audience/retargeting-id})))))

(deftest manifest-and-repository-contracts-are-canonical-edn
  (let [manifest (first (load-edn "manifest.edn"))
        contracts (load-edn "repository-contracts.edn")]
    (is (= "com-google-ads" (:actor/id manifest)))
    (is (= "github.com/gftdcojp/action-paid-outreach" (:actor/repo manifest)))
    (is (= 8 (count (:contracts contracts))))
    (is (= 8 (count (set (map :contract/id (:contracts contracts))))))))

(let [{:keys [fail error]} (run-tests 'paid-outreach.contracts-test)]
  (when (pos? (+ fail error)) (System/exit 1)))
