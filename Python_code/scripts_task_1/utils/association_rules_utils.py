from pathlib import Path
import pandas as pd
import subprocess
import time
import re
from utils.load_data_utils import load_to_merge_original_data_with_missings
from utils.constants import target_class_group
from utils.constants import bic_str
from utils.constants import java, java_max_memory, jar, run, zart_fpclose
from utils.constants import rule_imp, rule_sup, rule_conf, rule_lift
from utils.constants import java_max_memory_2 
from utils.constants import emptyset, itemset
from utils.constants import fpgrowth_with_lift, fpclose, closed_arm_charm

'''
Functions to get bicluster's features (or features and values, assuming that the bicluster
pattern is always the same for all lines of the bicluster)
'''

def listToTuplesWithClass(values_list, class_value):
    result = []
    for element in values_list:
        result += [(element, class_value)]
    return result

#when exp_list[i][1] is a map
def getBiclusterFeatures(exp_list, list_bic_file_names):
    bics_exp = {}
    for i in range(0, len(exp_list)):
        #get experience id
        exp_id = exp_list[i][0]
        #get ids from experience biclusters (append all lists
        # for all classes in one)
        map_bic_classes = exp_list[i][1]
        exp_bic_ids_list = []
        # convert list of bicluster ids to list of tuples (bic_id, class)
        for class_value, list_bics in map_bic_classes.items():
            exp_bic_ids_list += listToTuplesWithClass(list_bics, class_value)
        #print(exp_bic_ids_list)
        #get experience file name
        bic_exp_file_name = list_bic_file_names[i]
        #process experience file name to get bics info
        bics_exp[exp_id] = processFeatures(bic_exp_file_name, exp_bic_ids_list)
    return bics_exp

#function to get bicluster features
def processFeatures(bic_exp_file_name, exp_bic_ids_list): 
    #create list of bicluster prefix lines (start line)
    prefixes = [(bic_str + str(tup[0]), tup[1]) for tup in exp_bic_ids_list]
    read_header_flag = -1
    #list of bicluster list of strings to return
    result = []
    for prefix, class_value in prefixes:
        #open experience file and go line by line
        with open(bic_exp_file_name) as inFile:
            for line in inFile:
                #checks if line starts with any of the tuple's prefixes
                if line.startswith(prefix):
                    print("Processing prefix " + prefix)
                    read_header_flag = 4
                    #init bic features list
                    bic_features = []
                    #init bic number of rows
                    bic_num_rows = 0
                #skip the first 3 lines of the bicluster info
                if read_header_flag > 0:     
                    read_header_flag -= 1
                elif read_header_flag == 0:
                    #reached empty line at the end of bicluster data (save bic_features)
                    if line in ['\n', '\r\n']:
                        #reset flag
                        read_header_flag = -1
                        #print(bic_content + "\n") #debug
                        # add the features to the result the number of times
                        # correspondent to the number of rows of the bicluster
                        # to maintain the correct levels of support
                        result += [bic_features] * bic_num_rows
                        break
                    else:
                        #process features line
                        if "(" in line:
                            #get bicluster size
                            parts = line.split(") ")
                            # parts[0] = "(#features,#rows" 
                            bic_num_rows = int(parts[0].split(",")[1])
                            #process rest of the line
                            line = parts[1]
                            #remove whitespaces at the beginning and the end of the line
                            line = line.strip()
                            #remove X array with the subject ids
                            line = line.split(" X=")[0]
                            #remove Y array delimiters
                            line = line.replace('Y=[','')[:-1]
                            #replace commas with tabs
                            line = line.replace(",", "\t")
                            #add class value
                            line += "\t" + target_class_group + "|" + class_value
                            #print(line)                            
                            bic_features += line.split("\t")

    return result

#when exp_list[i][1] is a map
def getBiclusterFeaturesAndValues(exp_list, list_bic_file_names):
    bics_exp = {}
    for i in range(0, len(exp_list)):
        #get experience id
        exp_id = exp_list[i][0]
        #get ids from experience biclusters (append all lists
        # for all classes in one)
        map_bic_classes = exp_list[i][1]
        exp_bic_ids_list = []
        # convert list of bicluster ids to list of tuples (bic_id, class)
        for class_value, list_bics in map_bic_classes.items():
            exp_bic_ids_list += listToTuplesWithClass(list_bics, class_value)
        #print(exp_bic_ids_list)
        #get experience file name
        bic_exp_file_name = list_bic_file_names[i]
        #process experience file name to get bics info
        bics_exp[exp_id] = processFeaturesAndValues(bic_exp_file_name, exp_bic_ids_list)
    return bics_exp

#function to get bicluster features and values, assuming that the bicluster pattern
#is pattern is always the same for all lines of the bicluster)
def processFeaturesAndValues(bic_exp_file_name, exp_bic_ids_list): 
    #create list of bicluster prefix lines (start line)
    prefixes = [(bic_str + str(tup[0]), tup[1]) for tup in exp_bic_ids_list]
    read_header_flag = -1
    #list of bicluster list of strings to return
    result = []
    for prefix, class_value in prefixes:
        #open experience file and go line by line
        with open(bic_exp_file_name) as inFile:
            for line in inFile:
                #checks if line starts with any of the tuple's prefixes
                if line.startswith(prefix):
                    print("Processing prefix " + prefix)
                    read_header_flag = 4
                    #init bic features and values lists
                    bic_features = []
                    bic_values = []
                    #init bic number of rows
                    bic_num_rows = 0
                    #flag to process only one line of values
                    flag_first_pattern_processed = False
                #skip the first 3 lines of the bicluster info
                if read_header_flag > 0:     
                    read_header_flag -= 1
                elif read_header_flag == 0:
                    #reached empty line at the end of bicluster data (save bic_features)
                    if line in ['\n', '\r\n']:
                        #reset flag
                        read_header_flag = -1
                        # join features with respective values
                        bic_features_values = []
                        for i in range(len(bic_features)):
                            bic_features_values += [bic_features[i] + "|" + bic_values[i]] 
                        # add the features + values to the result the number of times
                        # correspondent to the number of rows of the bicluster
                        # to maintain the correct levels of support
                        result += [bic_features_values] * bic_num_rows
                        break
                    else:
                        #process features line
                        if "(" in line:
                            #get bicluster size
                            parts = line.split(") ")
                            # parts[0] = "(#features,#rows" 
                            bic_num_rows = int(parts[0].split(",")[1])
                            #process rest of the line
                            line = parts[1]
                            #remove whitespaces at the beginning and the end of the line
                            line = line.strip()
                            #remove X array with the subject ids
                            line = line.split(" X=")[0]
                            #remove Y array delimiters
                            line = line.replace('Y=[','')[:-1]
                            #replace commas with tabs
                            line = line.replace(",", "\t")
                            #add class feature
                            line += "\t" + target_class_group
                            #print(line)                            
                            bic_features += line.split("\t")
                        #process first pattern row
                        else:
                            if not flag_first_pattern_processed:
                                #remove last tab and \n
                                line = line.replace('\t\n', '')
                                #add class value
                                line += "\t" + class_value
                                #get list of values (and remove first value, the subject id)
                                bic_values += line.split("\t")[1:]
                                #update flag so no more bicluster lines are processed
                                flag_first_pattern_processed = True

    return result

'''
SPMF Association Rules Preprocessing and Postprocessing
'''
def getTranslationMapsSPMF(data_list_of_lists):
    # data_list_of_lists is a list of lists with the features present in the biclusters
    # as many times as the respective bicluster height; the idea here is to obtain
    # the unique values of those features in all the lists so they can be translated
    # to the integer values that the SPMF algorithm expects as transaction items;

    # get maps to translate from the original features to spmf and back
    translation_map_original_spmf = {}
    translation_map_spmf_original = {}

    # get unique values in a set
    translation_set = set()
    for data_list in data_list_of_lists:
        for element in data_list:
            translation_set.add(element)

    # convert to list to be iterable (and sort it so the conversions are always the same)
    translation_list = sorted(list(translation_set))

    # get new "names" for the features (strings of integers -> easier to order later on)
    for i in range(len(translation_list)):
        translation_map_original_spmf[translation_list[i]] = str(i + 1)
        translation_map_spmf_original[str(i + 1)] = translation_list[i]
    
    return translation_map_original_spmf, translation_map_spmf_original

def translateDataOriginalToSPMF(data_list_of_lists, translation_map_original_spmf):
    # translate features according to the map
    translated_data = []

    for data_list in data_list_of_lists:
        temp_list = []
        for element in data_list:
            temp_list += [translation_map_original_spmf[element]]
        #sort the transaction data lexicographically while we're at it
        translated_data += [sorted(temp_list)]

    return translated_data

'''
SPMF Closed Association Rules
'''

def callSPMFAssociationRules(SPMF_jar_path, min_sup, min_conf, min_lift, input_folder, input_file_name, \
                             output_folder, output_file_name):
    start = time.time()
    #java -jar spmf.jar run Closed_association_rules <inputFile> <outputFile> <minSup>% <minConf>% <minLift>
    call_fpgrowth = subprocess.run([java, java_max_memory_2, jar, SPMF_jar_path, run, closed_arm_charm, \
                                input_folder / input_file_name, output_folder / output_file_name, \
                                str(min_sup) + '%', str(min_conf) + '%', str(min_lift)], capture_output=True)
    print("args:", call_fpgrowth.args)
    print("return code:", call_fpgrowth.returncode)
    print("stdout:", call_fpgrowth.stdout)
    print("stderr:", call_fpgrowth.stderr)
    end = time.time()
    print(end - start, "seconds")

# examples of association rule lines:
# 21 2 ==> 23 #SUP: 12904 #CONF: 1.0
# 2 ==> 21 23 #SUP: 12904 #CONF: 0.9580518227039869
def translateSPMFAssociationRuleLine(translation_map_spmf_original, spmf_line):
    # parse read line from input file
    parts = spmf_line.split(rule_sup)
    # remove trailing spaces on rule part
    rule = parts[0].strip()
    # split rule into antecedents and consequents 
    rule_parts = rule.split(rule_imp)
    antecedents = rule_parts[0].strip().split(" ")
    consequents = rule_parts[1].strip().split(" ")
    # create translated line and translate both ends of the rule
    translatedLine = ""
    # antecedents
    for i in range(len(antecedents)):
        ant = antecedents[i]
        translatedLine += translation_map_spmf_original[ant] 
        if i < len(antecedents) - 1: 
            translatedLine += ", "
    # add implication back (with surrounding spaces)
    translatedLine += " " + rule_imp + " "
    # consequents
    for i in range(len(consequents)):
        con = consequents[i]
        translatedLine += translation_map_spmf_original[con]
        if i < len(consequents) - 1: 
            translatedLine += ", "
    # add the first #SUP: used to split the spmf line + the rest of the data (already has \n)
    translatedLine += " " + rule_sup + parts[1]
    # return translated line
    return translatedLine

def translateSPMFAssociationRules(translation_map_spmf_original_file_path, spmf_output_file_path, \
                                  spmf_output_translated_file_path):

    # get translation map from spmf to original features from file
    translation_map_spmf_original = {}
    with open(translation_map_spmf_original_file_path, "r") as inFile:
        for line in inFile:
            # remove newline
            line = line.rstrip('\n')
            # split by tab
            parts = line.split('\t')
            #add to map
            translation_map_spmf_original[parts[0]] = parts[1]
    
    # read from spmf features file, translate and write to the final translated file
    with open(spmf_output_file_path, 'r') as inFile:
        with open(spmf_output_translated_file_path, "w") as outFileAll:
            for spmf_line in inFile:
                line = translateSPMFAssociationRuleLine(translation_map_spmf_original, spmf_line)
                outFileAll.write(line)

def filterTargetConsequentSPMF(spmf_output_translated_file_path, spmf_output_translated_filtered_file_path):
    count = 0
    # read from spmf features file, translate and write to the final translated file
    with open(spmf_output_translated_file_path, 'r') as inFile:
        with open(spmf_output_translated_filtered_file_path, "w") as outFileFiltered:
            for spmf_line in inFile:
                # parse read line from input file
                parts = spmf_line.split(rule_sup)
                # remove trailing spaces on rule part
                rule = parts[0].strip()
                # split rule into antecedents and consequents 
                rule_parts = rule.split(rule_imp)
                consequents = rule_parts[1].strip().split(" ")
                # check for consequent which is the target class 
                if len(consequents) == 1 and target_class_group in consequents[0]:
                    outFileFiltered.write(spmf_line)
                    count += 1
    #print how many lines were filtered
    print("filtered lines:", count)

def splitByConsequentTargetClass(spmf_output_translated_filtered_file_path, output_folder, spmf_output_file_name_base, spmf_file_extension):
    map_consequents = {}
    # read from spmf final translated file
    with open(spmf_output_translated_filtered_file_path, 'r') as inFile:        
        for spmf_line in inFile:
            # parse read line from input file
            parts = spmf_line.split(rule_sup)
            # remove trailing spaces on rule part
            rule = parts[0].strip()
            # split rule to get the consequent
            rule_parts = rule.split(rule_imp)
            consequent = rule_parts[1].strip().split(" ")[0]
            #print(consequent)
            # if it exists in the map
            if consequent in map_consequents:
                tmp_list = map_consequents[consequent]
                tmp_list += [spmf_line]
                map_consequents[consequent] = tmp_list
            # else, add it to the map
            else:
                map_consequents[consequent] = [spmf_line]
            
    if len(map_consequents.items()) == 0:
        print("warn: no rules with target class as consequent were found.")
    else:
        # for every key in the map (for every class), write a different file with
        # the lines in the map value
        for key, list_lines in map_consequents.items():
            print("class", key, "->", len(list_lines), "rules")
            # file names can't have pipes in them, so replace it with underscore
            file_name = output_folder / (spmf_output_file_name_base + key.replace('|', '_') + spmf_file_extension)
            # write the lines of the given class to a file
            with open(file_name, 'w') as outFile:
                for line in list_lines:
                    outFile.write(line)
