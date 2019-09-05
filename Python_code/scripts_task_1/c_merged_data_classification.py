'''
Script to obtain classification metrics from merging the original data + subject ids x bicluster presence matrix
'''

#imports
from sklearn.model_selection import train_test_split
from utils.load_data_utils import readMetricsExcelData
from utils.load_data_utils import load_to_merge_original_data_with_missings
from utils.load_data_utils import load_to_merge_matrix_data_no_missings
from utils.classification_utils import printShapes
from utils.classification_utils import fitRFClassifier
from utils.classification_utils import getClassificationMetrics
from utils.classification_utils import getClassificationMetricsCV
from utils.classification_utils import getAllBiclustersFromExperience
from utils.classification_utils import getExperiencesWithPurestBiclusters
from utils.classification_utils import getTranslatedBiclusterFileNames
from utils.classification_utils import getBiclusterContentsFlatList
from utils.classification_utils import getBiclustersMostFreqPatterns
from utils.classification_utils import writeBiclustersPatternsOutput
from utils.classification_utils import getPermutationImportanceMLxtend
from utils.constants import auc_model
from utils.constants import mlxtend_features
from utils.constants import most_important_features
from utils.constants import most_important_bics_features
from pathlib import Path
from subprocess import call
import pandas as pd
import numpy as np
import sys
import os
 
# numpy settings (to print whole ndarrays without omitting values)
np.set_printoptions(threshold=sys.maxsize)

#folder date
folder_date = "2019-7-13_16-15-55"

# folder path where the dataset input CSV is 
original_data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/transformations/OnWebDUALS_LISB_511_300_converted_2019-07-09/Task_1/")
# name of the dataset input CSV 
original_dataset_file_name = "CSV_missings_ALL_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.csv"

# folder path where the classification file is 
matrix_data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_" + folder_date + "/")
# name of the classification file name
matrix_classif_file_name = "Exp_14_CSV_classifier_" + folder_date + ".csv"
# Output folder for results 
output_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_" + folder_date + "/classification_merged_data/")
# name of the input metrics XLSX
xlsx_metrics_file_name = "ClassMetrics_" + folder_date + ".xlsx"

# number of estimators for the RF classifier
n_estimators = 300
# number of k-folds for the CV for the RF classifier
k_fold_times = 10
# number of the most important features to be returned by the RF classifier
n_features = 30
# sheet index
sheet_name_or_index = 2
#list of class values (Patient "1", Control "2")
list_classes = ["1", "2"]
# Experience name
experience_name = "Exp_14"
#number of runs to calculate permutation importances
number_perm_runs = 10
#width of permutation importance plot
width_perm_imp_plot = 15

# main function
def start_processing():

    #create folder if it doesn't exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Read original data
    original_data_source = original_data_folder / original_dataset_file_name
    # Get dataframe 
    df_original = load_to_merge_original_data_with_missings(original_data_source)

    # Read subject ids x bicluster presence matrix
    matrix_data_source = matrix_data_folder / matrix_classif_file_name
    # Get dataframe
    df_matrix = load_to_merge_matrix_data_no_missings(matrix_data_source)

    # debug
    # print("df_original.shape:", df_original.shape)
    # print("df_matrix.shape:", df_matrix.shape)
    # print(df_original.columns, df_matrix.columns)
    # print(df_original['Subject ID'].values == df_matrix['Subject ID'].values)
    # print(df_original.index, df_matrix.index)

    # Remove the unnecessary target column from the original data (it will be present on the matrix data)
    df_original = df_original.drop('group', axis='columns')
    # Remove the unnecessary subject id column from the matrix data (it will be present on the original data)
    df_matrix = df_matrix.drop('Subject ID', axis='columns')

    # debug
    #print("df_final.shape:", df_final.shape) #df_final.shape: (770, 6966) -> 6821 + 147 - 2 (removed subject id and group)
    #print(df_final.columns)
    #print(df_final.head)

    # Run the scikit-learn RF classifier for the discriminative biclusters in the classification file 
    # (subject ids x bicluster presence matrix)
    print("********* Classification (Discriminative Bicluster features only) *********")
    
    # Read XLSX with the experience's metrics (sheet PurestBiclusters)
    df_purest_bics = readMetricsExcelData(matrix_data_folder, xlsx_metrics_file_name, sheet_name_or_index)
    # Get the experience names and the ids of its purest biclusters
    exp_list = getExperiencesWithPurestBiclusters(df_purest_bics, list_classes, matrix_data_folder, folder_date)

    # Get only one of the experiences (Exp_14)
    exp_list = [l for l in exp_list if l[0] == experience_name]

    #print(exp_list)

    # Get minimum number of discriminative Biclusters between all classes
    min_bics = sys.maxsize
    for c in list_classes:
        num_bics_class = len(exp_list[0][1][c])
        if num_bics_class < min_bics:
            min_bics = num_bics_class
        print("class", c, "->", num_bics_class, "biclusters")

    # Randomly sample the Biclusters from the classes with more discriminative Biclusters
    # to equalize the number of Biclusters considered from each class
    for c in list_classes:
        num_bics_class = len(exp_list[0][1][c])
        if num_bics_class > min_bics:
            print("Sampling class", c)
            df_to_sample = pd.DataFrame(exp_list[0][1][c], columns=['Samp'])  
            # sampling fixed seed -> Fibonacci prime number 1597
            df_to_sample = df_to_sample.sample(n=min_bics, random_state=1597)
            # sort bicluster ids
            df_to_sample = df_to_sample.astype(int)
            df_to_sample = df_to_sample.sort_values('Samp')
            df_to_sample = df_to_sample.astype(str)
            # replace list
            sampled_list = df_to_sample['Samp'].values.tolist()
            exp_list[0][1][c] = sampled_list

    #print(exp_list)

    # Get all discriminative biclusters from the experience
    list_all_bics = getAllBiclustersFromExperience(exp_list, experience_name)
    # Add the prefix Bic_ again (if number is < 10 add a 0)
    list_all_bics = [('Bic_0' + x) if int(x) < 10 else ('Bic_' + x) for x in list_all_bics]
    # Add the last column since that column should be there
    list_features_to_keep = list_all_bics + ['group']

    # Get the dataframe from the subject ids x bicluster presence matrix df
    df_discriminative = df_matrix[list_features_to_keep]

    # Glue together both dataframes
    df_final2 = pd.concat([df_original, df_discriminative], axis=1, sort=False)

    # Run the scikit-learn RF classifier for the classification file (subject ids x bicluster presence matrix
    # but just for the discriminative biclusters)
    classification(df_final2, n_estimators, k_fold_times, n_features, experience_name, matrix_data_folder, output_folder)

#In stratified k-fold cross-validation, the folds are selected so that the mean 
#response value is approximately equal in all the folds. In the case of binary 
#classification, this means that each fold contains roughly the same proportions 
#of the two types of class labels. 
# - Cross-validation (Wikipedia)

#cv : int, cross-validation generator or an iterable, optional
#Determines the cross-validation splitting strategy. Possible inputs for cv are:
#   None, to use the default 3-fold cross validation,
#   integer, to specify the number of folds in a (Stratified)KFold, 
#   CV splitter,
#   An iterable yielding (train, test) splits as arrays of indices.
#   
#For integer/None inputs, if the estimator is a classifier and y is either binary 
#or multiclass, StratifiedKFold is used. In all other cases, KFold is used.
# - sklearn.model_selection.cross_validate

def classification(df, n_estimators, k_fold_times, n_features, experience_name, data_folder, output_folder):
    # Read X matrix (values), y column (target class) and feature names from dataframe   
    #convert the data frame to a matrix X
    X = df.values
    #y: last column of the file, the target class column
    y = X[:, -1]
    #cast all y elements to int
    y = y.astype(int)
    #remove first (subject id) and last (target class) columns of the data matrix to only keep the data
    X = X[:, list(range(1,len(df.columns) - 1))]
    #cast all X elements to int
    X = X.astype(int)
    #get list of feature names (also removing first and last columns)
    feature_names = list(df)[1:-1]
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
    mlxtend_features_file_name = mlxtend_features + "_merged_data" 

    # get permutation importance values for top-30 features
    most_important_features_perm = getPermutationImportanceMLxtend(number_perm_runs, rf, X_test, y_test, \
                                                                   feature_names, width_perm_imp_plot, \
                                                                   output_folder, mlxtend_features_file_name, n_features)

    # get patterns of the most important Biclusters on a separate file
    # Remove Bic_0 and Bic_ parts from features to get bicluster ids
    most_important_features_perm = [str(int(x.split("_")[1])) for x in most_important_features_perm if x.startswith("Bic_")]
    # Create dummy exp_list
    exp_list = [[experience_name, most_important_features_perm]]
    # Get purest biclusters by experience (from translated_labels files)
    list_bic_file_names = getTranslatedBiclusterFileNames(data_folder, exp_list)
    # Get Bicluster text contents per experience
    bics_exp = getBiclusterContentsFlatList(exp_list, list_bic_file_names, False)
    # Replace the Bicluster contents on the dictionary with the the most frequent
    # pattern for each bicluster
    bics_exp = getBiclustersMostFreqPatterns(bics_exp)
    # Export the most important N feature (biclusters/meta-features) patterns to a tsv file
    writeBiclustersPatternsOutput(bics_exp, output_folder, mlxtend_features_file_name + ".txt")

    # get permutation importance values for all features
    # getPermutationImportanceMLxtend(number_perm_runs, rf, X_test, y_test, feature_names, width_perm_imp_plot * 2, \
    #                                 output_folder, mlxtend_features_file_name + "_all")  

# script entry point
start_processing()
