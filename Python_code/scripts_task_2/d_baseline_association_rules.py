'''
Script to obtain association rules from the original data
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
from utils.association_rules_utils import getTranslationMapsSPMF
from utils.association_rules_utils import translateDataOriginalToSPMF
from utils.association_rules_utils import callSPMFAssociationRules
from utils.association_rules_utils import translateSPMFAssociationRules
from utils.association_rules_utils import filterTargetConsequentSPMF
from utils.association_rules_utils import splitByConsequentTargetClass
from utils.load_data_utils import getTranslationMapCategoriesLabels
from utils.load_data_utils import load_to_merge_original_data_with_missings
from pathlib import Path
import subprocess
import time
import os

#folder date
folder_date = "2019-7-14_9-27-01"
# folder path where the label XLSX is 
data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/transformations/OnWebDUALS_LISB_511_300_converted_2019-07-09/Task_2/")
# name of the dataset input CSV 
dataset_file_name = "CSV_baseline_Patients_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.csv"
# name of the labels xlsx
xslx_translation_categories_labels = "categories_labels_Patients_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.xlsx"
# Output folder for results 
output_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_2/Task_2_results_" + folder_date + "/association_rules_baseline/")
# processed data for SPMF file name
processed_data_file_name = "Processed_Data_SPMF.tsv"
# flag to control data processing phase
flag_process_data = False
# translation maps file names
translation_map_original_spmf_file_name = "Translation_Original_SPMF.txt"
translation_map_spmf_original_file_name = "Translation_SPMF_Original.txt"
# SPMF jar location
SPMF_jar_path = "/Tools/SPMF/spmf.jar"
# minimum support (%)
min_sup = 2
# minimum confidence (%)
min_conf = 90
# minimum lift
min_lift = 1
# FS remaining columns + target class
to_keep_task_2 = ['Gender','Age (1st Symptoms)','Diagnostic delay',\
                  '1st region (Pattern of spreading)','3rd region (Pattern of spreading)',\
                  '4th region (Pattern of spreading)','5th region (Pattern of spreading)',\
                  'Timing of transition from region 1 to 2','Region3 (Region progression)',\
                  'Timing of transition from region 2 to 3','ALSFRSR number 1',\
                  'ALSFRSR number 2','ALSFRSR number 3','ALSFRSR number 4',\
                  'ALSFRSR number 5','ALSFRSR number 6','ALSFRSR number 7',\
                  'ALSFRSR number 8','ALSFRSR number 9','ALSFRSR total',\
                  'Main Occupation in the last 5 years (level 1)',\
                  'Time gap between first medical observation and diagnosis','group']

def processRowWithValues(matrix_row, feature_names, translation_map):  
    # list to return
    transaction = []
    # pipe all features with their values, separated by tabs: f1|v1    f2|v2   ... fn|vn
    for i in range(len(matrix_row)):
        #if the value is different from missing value ('-1')
        if matrix_row[i] != '-1':
            # get label from category value for the given feature
            feature_name = feature_names[i]
            #do not translate group feature
            if feature_name != "group":
                #print(feature_name, matrix_row[i])
                translated_label = translation_map[feature_name][matrix_row[i]]
            else:
                translated_label = matrix_row[i]
            # save translated transaction
            transaction += [feature_name + '|' + translated_label]
            #print(transaction)
    return transaction

def processData():
    # read dataset from alt folder
    file_path = data_folder / dataset_file_name
    # read file as dataframe
    df = load_to_merge_original_data_with_missings(file_path)
    # drop Subject ID
    df = df.drop('Subject ID', axis='columns')
    # keep only columns in to_keep
    df = df[to_keep_task_2]
    # get feature names
    original_feature_names = df.columns
    # change dataframe to matrix (removing header in the process)
    matrix = df.values
    # create translation map
    translation_map = getTranslationMapCategoriesLabels(data_folder, xslx_translation_categories_labels)
    #print(translation_map)
    # transaction database
    transaction_db = []
    # iterate through matrix rows to create transactions 
    for i in range(matrix.shape[0]):
        transaction_db += [processRowWithValues(matrix[i], original_feature_names, translation_map)] 

    # Get translation maps for SPMF algorithm
    translation_map_original_spmf, translation_map_spmf_original = getTranslationMapsSPMF(transaction_db) 

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
    translated_data = translateDataOriginalToSPMF(transaction_db, translation_map_original_spmf)

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