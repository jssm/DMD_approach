'''
Script to obtain classification metrics from subject ids x bicluster presence matrix
'''

#imports
from sklearn.model_selection import train_test_split
from utils.load_data_utils import readMetricsExcelData
from utils.load_data_utils import load_matrix_data_no_missings
from utils.load_data_utils import load_to_merge_matrix_data_no_missings
from utils.classification_utils import getExperiencesWithPurestBiclusters
from utils.classification_utils import getNumberOfBiclustersPerExperience
from utils.classification_utils import writeNumBicsPerExpOutput
from utils.classification_utils import getTranslatedBiclusterFileNames
from utils.classification_utils import getBiclusterContents
from utils.classification_utils import getBiclustersMostFreqPatterns
from utils.classification_utils import writeBiclustersPatternsOutput
from utils.classification_utils import getClassificationCSVFileNames
from utils.classification_utils import fitRFClassifier
from utils.classification_utils import getClassificationMetrics
from utils.classification_utils import getClassificationMetricsCV
from utils.classification_utils import getBiclusterContents
from utils.classification_utils import getBiclusterContentsFlatList
from utils.classification_utils import getBiclustersFeatures
from utils.classification_utils import writeBiclustersPatternsOutput
from utils.classification_utils import getTranslatedBiclusterFileNames
from utils.classification_utils import getAllBiclustersFromExperience
from utils.classification_utils import getPermutationImportanceMLxtend
from utils.constants import auc_model
from utils.constants import mlxtend_features
from utils.constants import most_important_bics_features
from utils.constants import num_purest_bics_per_exp_filename
from utils.constants import patterns_output_filename
from pathlib import Path
import pandas as pd
import numpy as np
import sys
import os

#numpy settings (to print whole ndarrays without omitting values)
np.set_printoptions(threshold=sys.maxsize)

#folder date
folder_date = "2019-7-13_16-15-55"
#folder path where the input metrics XLSX is 
data_folder = Path(("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_" + folder_date + "/"))
#name of the input metrics XLSX
xlsx_metrics_file_name = "ClassMetrics_" + folder_date + ".xlsx"
#name of the classification file name
classif_file_name = "Exp_14_CSV_classifier_" + folder_date + ".csv"
# Output folder for results
output_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_" + folder_date + "/classification_matrix_subjects_bics/")

#number of estimators for the RF classifier
n_estimators = 300
#number of k-folds for the CV for the RF classifier
k_fold_times = 10
#number of the most important features 
n_features = 30
#sheet index
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

    # Read XLSX with the experience's metrics (sheet PurestBiclusters)
    df = readMetricsExcelData(data_folder, xlsx_metrics_file_name, sheet_name_or_index)

    # Get the experience names and the ids of its purest biclusters
    exp_list = getExperiencesWithPurestBiclusters(df, list_classes, data_folder, folder_date)
    #print(exp_list)
    
    # Get string with number of purest biclusters by experience
    bics_per_exp = getNumberOfBiclustersPerExperience(exp_list)

    # Write the number of purest biclusters by experience to a tsv file
    writeNumBicsPerExpOutput(bics_per_exp, output_folder, num_purest_bics_per_exp_filename)

    # Get only one of the experiences (Exp_14)
    exp_list = [l for l in exp_list if l[0] == experience_name]

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
            print("done")

    #print(exp_list)

    # Get purest biclusters by experience (from translated_labels files)
    list_bic_file_names = getTranslatedBiclusterFileNames(data_folder, exp_list)
      
    print("Getting Bicluster Contents...")
    # Get Bicluster text contents per experience
    bics_exp = getBiclusterContents(exp_list, list_bic_file_names, False)
    print("done")

    print("Getting Bicluster Most Frequent Patterns...")
    # Replace the Bicluster contents on the dictionary with the the most frequent
    # pattern for each bicluster
    bics_exp = getBiclustersMostFreqPatterns(bics_exp)
    print("done")

    # Output results to a tsv file
    writeBiclustersPatternsOutput(bics_exp, output_folder, patterns_output_filename)

    # Get all discriminative biclusters from the experience
    list_all_disc_bics = getAllBiclustersFromExperience(exp_list, experience_name)

    # Add the first and last column since these columns should be there, as well as 
    # the prefix Bic_ again (if number is < 10 add a 0)
    list_features_to_keep = ['Subject ID'] + [('Bic_0' + x) if int(x) < 10 else ('Bic_' + x) for x in list_all_disc_bics] + ['group']

    # Run the scikit-learn RF classifier for the discriminative biclusters in the classification file 
    # (subject ids x bicluster presence matrix)
    print("********* Classification (Discriminative Bicluster features only) *********")

    # Get the dataframe from the subject ids x bicluster presence matrix file
    file_path = data_folder / classif_file_name
    df_discriminative = load_to_merge_matrix_data_no_missings(file_path)
    df_discriminative = df_discriminative[list_features_to_keep]

    #print(list(df_discriminative.columns))

    # Run the scikit-learn RF classifier for the classification file (subject ids x bicluster presence matrix
    # but just for the discriminative biclusters)
    classification(df_discriminative, n_estimators, k_fold_times, n_features, experience_name, data_folder, output_folder)

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

    # Fit RF classifier
    rf = fitRFClassifier(X_train, y_train, n_estimators)
    # Get classification metrics without cross validation
    getClassificationMetrics(rf, X_train, X_test, y_train, y_test)
    # Get classification metrics with stratified k-fold cross validation
    getClassificationMetricsCV(rf, k_fold_times, X, y)

    # Most important features output image file name (Random Forest Permutation Importance metric MLxtend lib)
    mlxtend_features_file_name = mlxtend_features + "_matrix_subject_bics" 

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

# script entry point
start_processing()
