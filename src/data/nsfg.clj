(ns data.nsfg
  ^{:doc "Translation of python code to generate dataset used in ThinkStats2 book. Original source here: https://raw.githubusercontent.com/AllenDowney/ThinkStats2/master/code/nsfg.py"}
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [tablecloth.api :as tc])
  (:import java.util.zip.GZIPInputStream))


(def dict-line-rx #"^\s+_column\((\d+)\)\s+(\S+)\s+(\S+)\s+%(\d+)(\S)\s+\"([^\"]+)\"")

(defn parse-dict-line
  [line]
  (let [[_ col type name f-len f-spec descr] (re-find dict-line-rx line)]
    {:col    (dec (Integer/parseInt col))
     :type   type
     :name   (str/replace name "_" "-")
     :f-len  (Integer/parseInt f-len)
     :f-spec f-spec
     :descr  descr}))

(defn read-dict-defn
  "Read a Stata dictionary file, return a vector of column definitions."
  [path]
  (with-open [r (io/reader path)]
    (mapv parse-dict-line (butlast (rest (line-seq r))))))

(defn parse-value
  [type raw-value]
  (when (not (empty? raw-value))
    (case type
      ("str12")          raw-value
      ("byte" "int")     (Long/parseLong raw-value)
      ("float" "double") (Double/parseDouble raw-value))))

(defn make-row-parser
  "Parse a row from a Stata data file according to the specification in `dict`.
   Return a vector of columns."
  [dict]
  (fn [row]
    (reduce (fn [accum {:keys [col type name f-len]}]
              (let [raw-value (str/trim (subs row col (+ col f-len)))]
                (conj accum (parse-value type raw-value))))
            []
            dict)))

(defn reader
  "Open path with io/reader; coerce to a GZIPInputStream if suffix is .gz"
  [path]
  (if (.endsWith path ".gz")
    (io/reader (GZIPInputStream. (io/input-stream path)))
    (io/reader path)))

(defn read-dct-data
  "Parse lines from `rdr` according to the specification in `dict`.
   Return a lazy sequence of parsed rows."
  [dict rdr]
  (let [parse-fn (make-row-parser dict)]
    (map parse-fn (line-seq rdr))))

(defn as-dataset
  "Read Stata data set, return an dataset."
  [dict-path data-path]
  (let [dict   (read-dict-defn dict-path)
        header (map (comp keyword :name) dict)]
    (with-open [r (reader data-path)]
      (tc/dataset (read-dct-data dict r)
                   {:layout :as-rows
                    :column-names header
                    :dataset-name "2002FemPreg"}))))

(defn get-column-frequency-by-index [ds col index]
  (-> (tc/column ds col)
      frequencies
      (get index)
      ))

(defn get-column-max-by-value [ds col]
  (let [freq (-> (tc/column ds col) frequencies)
        max-key (->> freq keys (apply max))]
    (get freq max-key)))

(defn assert-data
  "Check data after loading according to import script https://github.com/AllenDowney/ThinkStats2/blob/master/workshop/nsfg.py
    assert len(df) == 13593
    assert df.caseid[13592] == 12571
    assert df.pregordr.value_counts()[1] == 5033
    assert df.nbrnaliv.value_counts()[1] == 8981
    assert df.babysex.value_counts()[1] == 4641
    assert df.birthwgt_lb.value_counts()[7] == 3049
    assert df.birthwgt_oz.value_counts()[0] == 1037
    assert df.prglngth.value_counts()[39] == 4744
    assert df.outcome.value_counts()[1] == 9148
    assert df.birthord.value_counts()[1] == 4413
    assert df.agepreg.value_counts()[22.75] == 100
    assert df.totalwgt_lb.value_counts()[7.5] == 302

    weights = df.finalwgt.value_counts()
    key = max(weights.keys())
    assert df.finalwgt.value_counts()[key] == 6"
  []
  (let [ds (as-dataset "resources/2002FemPreg.dct" "resources/2002FemPreg.dat.gz")]
    (try
      (assert (= (tc/row-count ds) 13593)  "Numbers of rows should be 13593")
      (assert (= (tc/get-entry ds :caseid 13592) "12571")  "The caseid entry with inder 13592 should be 12571")
      (assert (= (get-column-frequency-by-index ds :pregordr 1) 5033) "The number of entries with value 1 in pregordr column should be 5033")
      (assert (= (get-column-frequency-by-index ds :nbrnaliv 1) 8981) "The number of entries with value 1 in :nbrnaliv column should be 8981")
      (assert (= (get-column-frequency-by-index ds :babysex 1) 4641) "The number of entries with value 1 in :babysex column should be 4641")
      (assert (= (get-column-frequency-by-index ds :birthwgt-lb 7) 3049) "The number of entries with value 7 in :birthwgt-lb column should be 3049")
      (assert (= (get-column-frequency-by-index ds :birthwgt-oz 0) 1037) "The number of entries with value 0 in :birthwgt-oz column should be 1037")
      (assert (= (get-column-frequency-by-index ds :prglngth 39) 4744) "The number of entries with value 39 in :prglngth column should be 4744")
      (assert (= (get-column-frequency-by-index ds :outcome 1) 9148) "The number of entries with value 1 in :outcome column should be 9148")
      (assert (= (get-column-frequency-by-index ds :birthord 1) 4413) "The number of entries with value 1 in :birthord column should be 4413")
      ;; (assert (= (get-column-frequency-by-index ds :agepreg 22.75) 100) "The number of entries with value 22.75 in :agepreg column should be 100")
      ;; (assert (= (get-column-frequency-by-index ds :totalwgt-lb 7.5) 302) "The number of entries with value 7.5 in :totalwgt-lb column should be 302")
      (assert (= (get-column-max-by-value ds :finalwgt) 6) "The value of the max entry in :finalwgt column should be 6")

      (catch AssertionError e "Assert data failed" (.getMessage e)))))

(comment
  (-> (as-dataset "resources/2002FemPreg.dct" "resources/2002FemPreg.dat.gz")
      (tc/column :caseid))

;;
  )
