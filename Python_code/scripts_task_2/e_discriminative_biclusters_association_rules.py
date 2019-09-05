'''
Script to obtain association rules from the features found in the most discriminative biclusters
'''
# imports
from pathlib import Path
from utils.constants import target_class_group
from utils.constants import closed_freq_itemsets
from utils.constants import spmf_output_file_name
from utils.constants import spmf_output_file_name_base
from utils.constants import spmf_output_filtered_file_name
from utils.constants import spmf_output_translated_file_name
from utils.constants import txt_extension
from utils.constants import spmf_closed_itemsets_file_name
from utils.constants import spmf_closed_itemsets_for_ARM_file_name
from utils.association_rules_utils import getBiclusterFeaturesAndValues
from utils.association_rules_utils import getTranslationMapsSPMF
from utils.association_rules_utils import translateDataOriginalToSPMF
from utils.association_rules_utils import callSPMFAssociationRules
from utils.association_rules_utils import translateSPMFAssociationRules
from utils.association_rules_utils import filterTargetConsequentSPMF
from utils.association_rules_utils import splitByConsequentTargetClass
from utils.load_data_utils import readMetricsExcelData
from utils.classification_utils import getExperiencesWithPurestBiclusters
from utils.classification_utils import getNumberOfBiclustersPerExperience
from utils.classification_utils import getTranslatedBiclusterFileNames
import pandas as pd
import subprocess
import time
import os
import sys

#folder date
folder_date = "2019-7-14_9-27-01"
#folder path where the input metrics XLSX is 
xlsx_data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_2/Task_2_results_" + folder_date + "/")
#name of the input metrics XLSX
xlsx_metrics_file_name = "ClassMetrics_" + folder_date + ".xlsx"
#sheet index
sheet_name_or_index = 2
#list of class values (Slow "1", Neutral "2", Fast "3")
list_classes = ["1", "2", "3"]
# Experience name
experience_name = "Exp_9"
# Output folder for results 
output_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_2/Task_2_results_" + folder_date + "/association_rules_bics_features_values/")
# flag to control data processing phase
flag_process_data = False
# processed data for SPMF file name
processed_data_file_name = "Processed_Data_SPMF.tsv"
# translation maps file names
translation_map_original_spmf_file_name = "Translation_Original_SPMF.txt"
translation_map_spmf_original_file_name = "Translation_SPMF_Original.txt"
# SPMF jar location
SPMF_jar_path = "/Tools/SPMF/spmf.jar"
# minimum support (%)
min_sup = 2.5
# minimum confidence (%)
min_conf = 90
# minimum lift
min_lift = 1

# optional function to process data
def processData():
    # Read XLSX with the experience's metrics (sheet PurestBiclusters)
    df = readMetricsExcelData(xlsx_data_folder, xlsx_metrics_file_name, sheet_name_or_index)

    # Get the experience names and the ids of its purest biclusters
    exp_list = getExperiencesWithPurestBiclusters(df, list_classes, xlsx_data_folder, folder_date)

    # Get only one of the experiences (Exp_9)
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

    # Get purest biclusters by experience (from translated_labels files)
    list_bic_file_names = getTranslatedBiclusterFileNames(xlsx_data_folder, exp_list)

    # Get Bicluster features and values per experience (feature|value)
    # Each set of Bicluster features is n transactions (n = bicuster height), including the 
    # class target + class value as one of the items: group|1 for Slow, group|2 for Neutral, group|3 for Fast
    bics_features_values = getBiclusterFeaturesAndValues(exp_list, list_bic_file_names)

    # Get translation maps for SPMF algorithm
    translation_map_original_spmf, translation_map_spmf_original = getTranslationMapsSPMF(bics_features_values[experience_name]) 

    # Write both maps to temp files
    with open(output_folder / translation_map_original_spmf_file_name, "w") as outFile:
        for key, value in translation_map_original_spmf.items():
            outFile.write(key + '\t' + value + '\n')

    with open(output_folder / translation_map_spmf_original_file_name, "w") as outFile:
        for key, value in translation_map_spmf_original.items():
            outFile.write(key + '\t' + value + '\n')

    # Translate original data to SPMF format
    # 1) items are represented by integers
    # 2) features/items should be lexicografically ordered (e.g. 1, 10, 2, 3, ...)
    # 3) items should be separated by spaces
    translated_data = translateDataOriginalToSPMF(bics_features_values[experience_name], translation_map_original_spmf)

    # Export translated data to a tsv file
    with open(output_folder / processed_data_file_name, "w") as outFile:
        for feature_list in translated_data:
            #write a row in the file separated by spaces
            row = " ".join(feature_list) + "\n"
            outFile.write(row)
    
# main function
def start_processing():
    #create folder if it doesn't exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # check if data needs to be processed
    if flag_process_data:
        print("processing data...")
        processData()
        print("done.")

    # call SPMF on closed itemsets of features from discriminative biclusters
    print("calculating association rules...")
    callSPMFAssociationRules(SPMF_jar_path, min_sup, min_conf, min_lift, \
                             output_folder, processed_data_file_name, \
                             output_folder, spmf_output_file_name)
    print("done.")

    #translate SPMF file to original feature names and filter rules with target class as consequent
    print("translating spmf ARM results...")
    # translate association rules back to the original features
    translateSPMFAssociationRules(output_folder / translation_map_spmf_original_file_name, \
                                  output_folder / spmf_output_file_name, \
                                  output_folder / spmf_output_translated_file_name)
    print("done.")

    #filter SPMF results
    print("filtering spmf ARM results...")
    filterTargetConsequentSPMF(output_folder / spmf_output_translated_file_name, \
                               output_folder / spmf_output_filtered_file_name)
    print("done.")

    #separate SPMF filtered results by class
    print("separating filtered rules by consequent class...")
    splitByConsequentTargetClass(output_folder / spmf_output_filtered_file_name, \
                                 output_folder, spmf_output_file_name_base, txt_extension)
    print("done.")


# script entry point
start_processing()