# DMD (Discriminative Meta-features Discovery) approach

A new exploratory Data Mining methodology called Discriminative Meta-features Discovery (DMD) is proposed to find disease presentation patterns in two-dimensional Electronic Health Records (EHR) data. The implementation of that approach done for the thesis named "BICLUSTERING ELECTRONIC HEALTH RECORDS TO UNRAVEL DISEASE PRESENTATION PATTERNS" has been made available in this repository. For more information please consult the Chapter 4 of said thesis.

## Motivation and Main Objective

The main objective was to design an exploratory approach that would allow to obtain discriminative, understandable and intuitive descriptions of medical concepts, which in this work have already been introduced as Meta-features. The necessity for explainable models in medicine is great since clinicians must be able to understand the reasoning behind a given model result or prediction before making a decision that can impact a patient's life. This type of models are also called "white-box" models, in contrast with "black-box" models (e.g. Artificial Neural Networks) whose internal decisions are very hard to interpret.

## General Layout
To accomplish this goal, several Machine Learning and Data Mining techniques were combined. First, Pattern Mining-based Biclustering was run on discretized and class-labelled data to find discriminative data patterns in the form of Biclusters. Afterwards those Biclusters were further used in two distinct branches:

* with Biclustering-based Classification models to understand what features (or subsets of features) were more important for a good classification, by the means of a Feature Importance metric;
* with Class Association Rules in order to find the subsets of features (and respective values) that showed greater association with each class label. 

These two approaches were used in parallel to take advantage of the explainable characteristics of the underlined models and also to validate the obtained results. 

## Data Pre-processing 
In order to clean, discretize and label the ONWebDUALS dataset for each Task data pre-processing pipelines were created using the KNIME tool. KNIME is an open source software written in Java which permits the creation of pipelines that combine the retrieval (from files, databases or the web), exploration, analysis and visualization of scientific data, while also allowing the integration of scripts written in programming languages widely used in Data Science (e.g. R, Python and Java) and abilities from other external tools (e.g. machine learning from WEKA). These pipelines are built by linking nodes with specific tasks to each other, creating an orderly pipeline with flow control, if necessary. Thus, this tool was used due to its integration and easy reproducibility of experiments capabilities. 

## Pattern Mining-based Biclustering
An ARFF file is passed to the Java code acting as a wrapper for the BicPAMS API. This code was created to facilitate the definiton of batches of experiments (Experiment Sets): for each BicPAMS (Biclustering algorithm, https://web.ist.utl.pt/rmch/bicpams/) parameter an array of possible value is passed, and an experiment is created for each possible combination of parameter values.

## Biclustering-based Classification
Random Forest (RF) classifiers from the Scikit-learn (https://scikit-learn.org/stable/) Python library were used to determine the most important features (or subsets of features). RFs were chosen for being robust against overfitting, fast to train and able to return Feature Importance metrics. Since our data had features with different numbers of categories, the Permutation Importance metric was used to prevent biases. The number of tree instances used in each RF classifier was 300, and the Permutation Importance values (using the MLxtend library, http://rasbt.github.io/mlxtend/) were calculating by performing the mean out of 10 runs. 

## Class Association Rules Mining
The Java SPMF library (http://www.philippe-fournier-viger.com/spmf/index.php) was called from Python scripts to mine regular ARs from frequent closed itemsets. Given that this generated a great amount of rules the following steps were taken to ease interpretation:

* Filtered out rules without the class as the single consequent; 
* Split the CARs to different files according to class; 
* Removed redundant rules per class.

## Available code
The code is divided by main layer of software or programming language on which they were created: KNIME, Java and Python.
A more thorough code listing shall be provided (TODO)
