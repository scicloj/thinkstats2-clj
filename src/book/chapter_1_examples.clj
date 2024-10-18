(ns book.chapter-1-examples
  (:require [tablecloth.api :as tc]
            [data.nsfg :as nsfg]))

;; # Chapter 1

;; Examples and Exercises from Think Stats, 2nd Edition
;; http://thinkstats2.com
;; Copyright 2016 Allen B. Downey
;; MIT License: https://opensource.org/licenses/MIT
;;
;; ## Exploratory data analysis
;; The thesis of this book is that data combined with practical methods can answer questions and guide decisions under uncertainty.
;;
;; As an example, I present a case study motivated by a question I heard when my wife and I were expecting our first child: do first babies tend to arrive late?
;;
;; If you Google this question, you will find plenty of discussion. Some people claim it’s true, others say it’s a myth, and some people say it’s the other way around: first babies come early.
;;
;; In many of these discussions, people provide data to support their claims. I found many examples like these:
;;
;;>“My two friends that have given birth recently to their first babies, BOTH went almost 2 weeks overdue before going into labour or being induced.”
;;
;;>“My first one came 2 weeks late and now I think the second one is going to come out two weeks early!!”
;;
;;>“I don’t think that can be true because my sister was my mother’s first and she was early, as with many of my cousins.”
;;
;;Reports like these are called anecdotal evidence because they are based on data that is unpublished and usually personal. In casual conversation, there is nothing wrong with anecdotes, so I don’t mean to pick on the people I quoted.
;;
;;But we might want evidence that is more persuasive and an answer that is more reliable. By those standards, anecdotal evidence usually fails, because:
;;
;;* Small number of observations: If pregnancy length is longer for first babies, the difference is probably small compared to natural variation. In that case, we might have to compare a large number of pregnancies to be sure that a difference exists.
;;
;;* Selection bias: People who join a discussion of this question might be interested because their first babies were late. In that case the process of selecting data would bias the results.
;;
;;* Confirmation bias: People who believe the claim might be more likely to contribute examples that confirm it. People who doubt the claim are more likely to cite counterexamples.
;;
;;* Inaccuracy: Anecdotes are often personal stories, and often misremembered, misrepresented, repeated inaccurately, etc.
;;
;;So how can we do better?
;;
;;## 1.1 A statistical approach
;;
;;To address the limitations of anecdotes, we will use the tools of statistics, which include:
;;
;;* Data collection: We will use data from a large national survey that was designed explicitly with the goal of generating statistically valid inferences about the U.S. population.
;;
;;* Descriptive statistics: We will generate statistics that summarize the data concisely, and evaluate different ways to visualize data.
;;
;;* Exploratory data analysis: We will look for patterns, differences, and other features that address the questions we are interested in.
;;At the same time we will check for inconsistencies and identify limitations.
;;
;;* Estimation: We will use data from a sample to estimate characteristics of the general population.
;;
;;* Hypothesis testing: Where we see apparent effects, like a difference between two groups, we will evaluate whether the effect might have happened by chance.
;;
;;By performing these steps with care to avoid pitfalls, we can reach conclusions that are more justifiable and more likely to be correct.
;;
;;## 1.2 The National Survey of Family Growth
;;
;;Since 1973 the U.S. Centers for Disease Control and Prevention (CDC) have conducted the National Survey of Family Growth (NSFG), which is intended to gather “information on family life,
;;marriage and divorce, pregnancy, infertility, use of contraception, and men’s and women’s health. The survey results are used...to plan health services and health education programs,
;;and to do statistical studies of families, fertility, and health.” See http://cdc.gov/nchs/nsfg.htm.
;;
;;We will use data collected by this survey to investigate whether first babies tend to come late, and other questions. In order to use this data effectively, we have to understand the design of the study.
;;
;;The NSFG is a **cross-sectional** study, which means that it captures a snapshot of a group at a point in time. The most common alternative is a **longitudinal** study, which observes a group repeatedly over a period of time.
;;
;;The NSFG has been conducted seven times; each deployment is called a **cycle**. We will use data from Cycle 6, which was conducted from January 2002 to March 2003.
;;
;;The goal of the survey is to draw conclusions about a population; the target population of the NSFG is people in the United States aged 15-44.
;;Ideally surveys would collect data from every member of the population, but that’s seldom possible. Instead we collect data from a subset of the population called a **sample**.
;;The people who participate in a survey are called **respondents**.
;;
;;In general, cross-sectional studies are meant to be **representative**, which means that every member of the target population has an equal chance of participating.
;;That ideal is hard to achieve in practice, but people who conduct surveys come as close as they can.
;;
;;The NSFG is not representative; instead it is deliberately **oversampled**. The designers of the study recruited three groups—Hispanics, African-Americans and teenagers—at rates
;;higher than their representation in the U.S. population, in order to make sure that the number of respondents in each of these groups is large enough to draw valid statistical inferences.
;;
;;Of course, the drawback of oversampling is that it is not as easy to draw conclusions about the general population based on statistics from the survey. We will come back to this point later.
;;
;;When working with this kind of data, it is important to be familiar with the **codebook**, which documents the design of the study, the survey questions, and the encoding of the responses.
;;The codebook and user’s guide for the NSFG data are available from http://www.cdc.gov/nchs/nsfg/nsfg_cycle6.htm
;;
;;## 1.3 Importing the data
;;
;;The code and data used in this book are available from https://github.com/scicloj/thinkstats2-clj (original book in Python from https://github.com/AllenDowney/ThinkStats2).
;;For information about downloading and working with this code, see Section 0.2.
;;
;;Once you download the code, you should have a file called `thinkstats2-clj/src/data/nsfg.clj`. If you run `assert-data` function from it, it should read a data file, run some tests,
;;and print a message like, “All tests passed.”
;;
;;Let’s see what it does. Pregnancy data from Cycle 6 of the NSFG is in a file called `2002FemPreg.dat.gz`; it is a gzip-compressed data file in plain text (ASCII), with fixed width columns.
;;Each line in the file is a **record** that contains data about one pregnancy.
;;
;;The format of the file is documented in `2002FemPreg.dct`, which is a Stata dictionary file. Stata is a statistical software system; a “dictionary” in this context is a list of variable names,
;;types, and indices that identify where in each line to find each variable.
;;
;;For example, here are a few lines from `2002FemPreg.dct`:
;;
;;```
;;infile dictionary {
;;   _column(1)  str12  caseid    %12s  "RESPONDENT ID NUMBER"
;;   _column(13) byte   pregordr   %2f  "PREGNANCY ORDER (NUMBER)"
;; }
;; ```
;;
;;This dictionary describes two variables: `caseid` is a 12-character string that represents the respondent ID; `pregordr` is a one-byte integer that indicates which pregnancy this record
;;describes for this respondent.
;;
;;The code you downloaded includes many functions used in this book, including functions that read the Stata dictionary
;;and the NSFG data file. Here’s how they are used in `nsfg.clj`:
;;
;;```
;; (defn read-fem-preg-dataset
;;   "Read Stata data set, return an dataset."
;;   [dict-path data-path]
;;   (let [dict-path (or dict-path "resources/2002FemPreg.dct")
;;         data-path (or data-path "resources/2002FemPreg.dat.gz")
;;         dict   (read-dictionary dict-path)
;;         header (map (comp keyword :name) dict)]
;;     (with-open [r (reader data-path)]
;;       (->> (tc/dataset (read-dictionary-data dict r)
;;                        {:layout :as-rows
;;                         :column-names header
;;                         :dataset-name "2002FemPreg"})
;;            (clean-fem-preg)))))
;;```
;;
;;`read-dictionary` takes the name of the dictionary file and returns dict, a vector that contains the column information from the dictionary file.
;;`read-dictionary-data` reads the data file using the dictionary.
;;
;;## 1.4 DataFrames
;;
;;The result of `tc/dataset` is a dataset (data frame), which is the columnar data structure provided by tablecloth, which is a Clojure data library we’ll use throughout this book.
;;A DataFrame contains a row for each record, in this case one row per pregnancy, and a column for each variable.
;;
;;In addition to the data, a DataFrame also contains the variable names and their types, and it provides methods for accessing and modifying the data.
;;If you print df you get a truncated view of the rows and columns, and the shape of the DataFrame, which is 13593 rows/records and 244 columns/variables.
(nsfg/read-fem-preg-dataset)
;;
;;The DataFrame is too big to display, so the output is truncated. The first line reports the number of rows and columns.
;;
;;The `column-names` function (do not forget to import the namespace `[tablecloth.api :as tc]`) returns a sequence of column names (`read-fem-preg-dataset` function converts strings
;;to keywords, so here we have a sequence of keywords):
(->> (tc/column-names (nsfg/read-fem-preg-dataset))
     (take 5))
;;To access a column from a DataFrame, you can use the column name as a key (you can also use `tc/column` function):
((nsfg/read-fem-preg-dataset) :pregordr)
;;The result is a `Column`, yet another tablecloth data structure. A `Column` is like a Clojure vector.
;;When you print a `Column`, you get its values.
;;The first two lines include the values type, the number of rows, and the column name. Here elements are integers (int64), but they can be any type.
;;If you run this example on a 32-bit machine you might see int32.
;;You can access the elements of a Column using integer indexes or sub sequences:
;;
(-> (tc/column (nsfg/read-fem-preg-dataset) :pregordr)
    (get 0))

(->> (tc/column (nsfg/read-fem-preg-dataset) :pregordr)
     (take 5)
     (drop 2))
;;
;;## 1.5 Variables
;;We have already seen two variables in the NSFG dataset, `caseid` and `pregordr`, and we have seen that there are 244 variables in total. For the explorations in this book, I use the
;;following variables:
;;
;;* `caseid` is the integer ID of the respondent.
;;* `prglngth` is the integer duration of the pregnancy in weeks.
;;* `outcome` is an integer code for the outcome of the pregnancy. The code 1 indicates a live birth.
;;* `pregordr` is a pregnancy serial number; for example, the code for a respondent’s first pregnancy is 1, for the second pregnancy is 2, and so on.
;;* `birthord` is a serial number for live births; the code for a respondent’s first child is 1, and so on. For outcomes other than live birth, this field is blank.
;;* `birthwgt_lb` and `birthwgt_oz` contain the pounds and ounces parts of the birth weight of the baby.
;;* `agepreg` is the mother’s age at the end of the pregnancy.
;;* `finalwgt` is the statistical weight associated with the respondent. It is a floating-point value that indicates the number of people in the U.S. population this respondent represents.
;;
;;If you read the codebook carefully, you will see that many of the variables are **recodes**, which means that they are not part of the **raw data** collected by the survey; they are
;;calculated using the raw data.
;;
;;For example, `prglngth` for live births is equal to the raw variable `wksgest` (weeks of gestation) if it is available; otherwise it is estimated using `mosgest * 4.33` (months of gestation
;;times the average number of weeks in a month).
;;
;;Recodes are often based on logic that checks the consistency and accuracy of the data. In general it is a good idea to use recodes when they are available, unless there is a compelling reason
;;to process the raw data yourself.
;;
;;## 1.6 Transformation
;;
;;When you import data like this, you often have to check for errors, deal with special values, convert data into different formats, and perform calculations. These operations
;;are called **data cleaning**.
;;
;;`nsfg.clj` includes `clean-fem-preg`, a function that cleans the variables I am planning to use.
;;
;;```
;; (defn clean-fem-preg
;;   [ds]
;;   (-> ds
;;       (tc/map-columns :agepreg                    ; target column name (new or to be updated)
;;                       [:agepreg]                  ; selected columns which values will be passed to the mapping function
;;                       (fn [v]                     ; mapping function (its arity must match the number of selected
;;                         (when-not (nil? v)        ; columns) which results to the new/updated value of the target column
;;                           (float (/ v 100)))))
;;       (tc/map-columns :birthwgt-lb
;;                       [:birthwgt-lb]
;;                       (fn [v]
;;                         (let [na-vals [51 97 98 99]] ; there is at least one bogus value (51) that needs to be addressed as well
;;                           (if (in? na-vals v) nil v))))
;;       (tc/map-columns :birthwgt-oz
;;                       [:birthwgt-oz]
;;                       (fn [v]
;;                         (let [na-vals [97 98 99]] ; address special codes [97 98 99]
;;                           (if (in? na-vals v) nil v))))
;;       (tc/map-columns :totalwgt-lb
;;                       [:birthwgt-lb :birthwgt-oz]
;;                       (fn [w-lb w-oz]
;;                         (when (and w-lb w-oz)
;;                           (-> (/ w-oz 16)
;;                               float
;;                               (+ w-lb)))))))
;;```
;;`agepreg` contains the mother’s age at the end of the pregnancy. In the data file, `agepreg` is encoded as an integer number of centiyears. So the first `map-columns` divides each element
;;of `agepreg` by 100, yielding a floating-point value in years.
;;
;;`map-columns` is used to create or update existing columns in the dataset. The first argument is the dataset, the second one
;;is the target column name (new or to be updated), the third argument is the sequence of columns which values will be passed to the mapping function, the forth argument is the mapping function
;;(its arity must match the number of selected columns) which results to the new/updated value of the target column.
;;
;;`birthwgt_lb` and `birthwgt_oz` contain the weight of the baby, in pounds and ounces, for pregnancies that end in live birth. In addition it uses several special codes:
;;```
;;97 NOT ASCERTAINED
;;98 REFUSED
;;99 DON'T KNOW
;;```
;;
;;Special values encoded as numbers are *dangerous* because if they are not handled properly, they can generate bogus results, like a 99-pound baby.
;;Here we replacing these values with `nil` to skip them in the further calculations.
;;
;;The last of `map-columns` creates a new column `totalwgt_lb` that combines pounds and ounces into a single quantity, in pounds.
;;
;;## 1.7 Validation
;;
;;When data is exported from one software environment and imported into another, errors might be introduced. And when you are getting familiar with a new dataset, you might interpret data
;;incorrectly or introduce other misunderstandings. If you take time to validate the data, you can save time later and avoid errors.
;;
;;One way to validate data is to compute basic statistics and compare them with published results. For example, the NSFG codebook includes tables that summarize each variable. Here is the
;;table for `outcome`, which encodes the outcome of each pregnancy:
;;
;;```
;; value   label                  Total
;; 1       LIVE BIRTH              9148
;; 2       INDUCED ABORTION        1862
;; 3       STILLBIRTH               120
;; 4       MISCARRIAGE             1921
;; 5       ECTOPIC PREGNANCY        190
;; 6       CURRENT PREGNANCY        352
;;```
;;
;;We can use `frequencies` function to count the number of times each value appears. If we select the `outcome` column from the DataFrame, we can use `frequencies` to compare with the
;;published data:
;;
(->> (:outcome (nsfg/read-fem-preg-dataset))
     (frequencies)
     (sort-by first))
;;
;;The result of `frequencies` is a map where keys are values and we can sort by it using `(sort-by first)`, so the values appear in order.
;;
;;Comparing the results with the published table, it looks like the values in `outcome` are correct. Similarly, here is the published table for `birthwgt_lb`
;;
;;```
;; value   label                  Total
;; .       INAPPLICABLE            4449
;; 0-5     UNDER 6 POUNDS          1125
;; 6       6 POUNDS                2223
;; 7       7 POUNDS                3049
;; 8       8 POUNDS                1889
;; 9-95    9 POUNDS OR MORE         799
;;```
;;
;;And here are the frequencies:
;;
(->> (:birthwgt-lb (nsfg/read-fem-preg-dataset))
     (frequencies)
     (sort-by first))
;;
;;The counts for 6, 7, and 8 pounds check out, and if you add up the counts for 0-5 and 9-95, they check out, too. But if you look more closely, you will notice one value that has to be
;;an error, a 51 pound baby!
;;
;;To deal with this error, I added the following logic to `clean-fem-preg`:
;;```
;; (tc/map-columns :birthwgt-lb
;;                 [:birthwgt-lb]
;;                 (fn [v]
;;                   (let [na-vals [51 97 98 99]]    ; here if the value is 51 or 97, 98, 99 then it is replaced by nil
;;                     (if (in? na-vals v) nil v))))
;;```
;;This statement replaces invalid values with `nil`.
;;
;;
;;
