'''
Script to obtain classification baseline with original data
'''

#imports
from sklearn.model_selection import train_test_split
from pathlib import Path
from subprocess import call
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys
from utils.load_data_utils import load_original_data_with_missings
from utils.classification_utils import printShapes
from utils.classification_utils import fitRFClassifier
from utils.classification_utils import getClassificationMetrics
from utils.classification_utils import getClassificationMetricsCV
from utils.classification_utils import getPermutationImportanceMLxtend
from utils.constants import mlxtend_features
import os

#numpy settings (to print whole ndarrays without omitting values)
np.set_printoptions(threshold=sys.maxsize)

#folder date
folder_date = "2019-7-14_9-27-01"
#folder path where the dataset input CSV is 
data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/transformations/OnWebDUALS_LISB_511_300_converted_2019-07-09/Task_2/")
#name of the dataset input CSV 
dataset_file_name = "CSV_baseline_Patients_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.csv"
# Output folder for results 
output_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_2/Task_2_results_" + folder_date + "/classification_baseline_raw_all_features/")
#number of estimators for the RF classifier
n_estimators = 300
#number of k-folds for the CV for the RF classifier
k_fold_times = 10
#number of runs to calculate permutation importances
number_perm_runs = 10
#width of permutation importance plot
width_perm_imp_plot = 15
#number of the most important features 
n_features = 30

# main function
def start_processing():

    #create folder if it doesn't exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    data_source = data_folder / dataset_file_name
    # Read X matrix (values), y column (target class) and feature names from csv file
    X, y, feature_names = load_original_data_with_missings(data_source)
    # Split data set into training and test set (75% and 25%) with stratification
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, random_state=0, stratify=y)
    printShapes(X_train, X_test, y_train, y_test)

    # Fit RF classifier
    rf = fitRFClassifier(X_train, y_train, n_estimators)
    # Get classification metrics without cross validation
    getClassificationMetrics(rf, X_train, X_test, y_train, y_test)
    # Get classification metrics with stratified k-fold cross validation
    getClassificationMetricsCV(rf, k_fold_times, X, y)

    # Most important features output image file name (Random Forest Permutation Importance metric MLxtend lib)
    mlxtend_features_file_name = mlxtend_features + "_baseline_raw" 

    # get permutation importance values for top-30 features
    getPermutationImportanceMLxtend(number_perm_runs, rf, X_test, y_test, feature_names, width_perm_imp_plot, \
                                    output_folder, mlxtend_features_file_name, n_features)

    # get permutation importance values for all features
    getPermutationImportanceMLxtend(number_perm_runs, rf, X_test, y_test, feature_names, width_perm_imp_plot * 3, \
                                    output_folder, mlxtend_features_file_name + "_all")                                

# script entry point
start_processing()