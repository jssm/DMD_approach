Java code notes:

For project dependencies, please see the Java_dependencies.txt file.

Source code tree:

- src
  - bic_experiment
     - BicPamsExperiment.java => class that defines one BicPAMS experiment, with a given combination of input parameters;
     - BicPamsExperimentSet.java => class that defines the set of BicPAMS experiments, from the combination of values given for all input parameters;
  - bic_metrics
     - BiclustersEvaluator.java => class that evaluates the Bicluster solution of an experiment (can calculate entropy, purity, precision, recall and f-measure for each Bicluster, plus entropy and purity for the whole solution;
     - MetricExperiment.java => container class for the metrics of a given experiment (Biclustering solution), also creates the XLSX file with all the metrics; 
  - bic_translator
     - BiclustersTranslator.java => class that allows for two exclusive Bicluster translation modes: 1) from category indexes (from ARFF file) to category values and 2) from category values to category labels (for interpretability reasons);
  - tasks
     - Task1.java => main class for Task 1, used to obtain the purest Biclusters in order to discover the most discriminative features or sets of features between both classes;
     - Task2.java => main class for Task 2, used to obtain the purest Biclusters in order to discover the most discriminative features/sets of features between the 3 progression groups (Slow, Neutral and Fast);
  - utils
     - Utils.java => class with utility methods.