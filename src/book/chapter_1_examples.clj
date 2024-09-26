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
;;
;;
;;
;;
;;
