Python code notes:

For project dependencies, please see the Python_dependencies.txt file.

The letters associated with the scripts are slightly different from what was written in the thesis. 
Each task includes separate scripts for the same task, since some parameters had to be adjusted depending on the task (e.g. minimum support). Thus the scripts described below exist in the two task folders, unless indicated otherwise.

For the Biclustering-based classification part we have the experiments and their respective scripts:

  - a) Baseline with all features (Raw) => a_baseline_classification_raw.py script;
  - b) Baseline with a subset of the features (FS) => a_baseline_classification.py script;
  - c) Matrix of subject ids x discriminative Bicluster ids (Meta-features) => b_subject_biclusters_classification.py script;
  - d) Merged data (joining the data from b) + c)) => c_merged_data_classification.py script.

For the Class Association Rule Mining we have:

  - e) Baseline with a subset of the features (FS) and their values => d_baseline_association_rules.py script; 
  - f) Bicluster Features and Values (Meta-features) => e_discriminative_biclusters_association_rules.py script.

Utility scripts:

  - process_assoc_rules.py script => script to decrease the redundancy between rules found for a given class for the Class Association Rule Mining experiments;
  - process_bicluster_patterns.py script => script used to print Bicluster patterns in the same line.

Utility modules:

  - constants.py script => script with global constants;
  - load_data_utils.py script => script with functions to load data from files;
  - association_rules_utils.py script => script with functions used in the Class Association Rule Mining part;
  - classification_utils.py script => script with functions used in the Biclustering-based Classification part;
  - multiscorer.py (only for Task 2) script => additional library to obtain evaluation metrics for multiclass case with cross-validation due to Scikit-learn limitations on the cross_val_score function.