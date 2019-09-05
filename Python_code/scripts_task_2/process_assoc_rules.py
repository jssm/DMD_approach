'''
Script to further process the Association Rules obtained from SPMF AR mining;
the idea is to decrease the redundancy between rules found for a given class 
(since all found rules are Class Association Rules, where the class is on the
consequent); 
'''
# imports

from pathlib import Path
import subprocess
import time
import os
from operator import itemgetter
from utils.constants import spmf_output_file_name_base
from utils.constants import txt_extension
from utils.constants import non_redundant_suffix

#constants for indexes on rule tuples
rule_id_idx = 0
antecedents_idx = 1
consequent_idx = 2
sup_idx = 3
conf_idx = 4
lift_idx = 5

#folder date
folder_date = "2019-7-14_9-27-01"
#folders where the ARM files are
data_folder = Path("/Users/joana/Desktop/MCD/Thesis/Data/results/Task_2/Task_2_results_" + folder_date)
data_folder_d = data_folder / "association_rules_baseline"
data_folder_e = data_folder / "association_rules_bics_features_values"

'''
ARM Redundancy (R and Data Mining: Examples and Case Studies, Yanchang Zhao, Chap 9.3):
Generally speaking, when a rule is a super rule of another rule and the 
former has the same or a lower lift, the former rule is considered to be redundant. 
For example: 
   lhs             rhs                support confidence     lift
1  {Class=2nd,
    Age=Child}  => {Survived=Yes} 0.010904134  1.0000000 3.095640
2  {Class=2nd,
    Sex=Female,
    Age=Child}  => {Survived=Yes} 0.005906406  1.0000000 3.095640
Here the rule 2 is a super rule of rule 1 and since they have the same lift value, 
rule 2 is considered to be redundant.
--------------------------------------------------------------------------------
ARM Visualization (R and Data Mining: Examples and Case Studies, Yanchang Zhao, Chap 9.4 ):
http://chris35wills.github.io/courses/PythonPackages_matplotlib/matplotlib_scatter/
'''

# read a rule line and parse the different parts
# e.g. Hypercholesterolemia|No, NSAID|No, Was there ALS in the family|Yes ==> 
# group|1 #SUP: 20 #CONF: 1.0 #LIFT: 1.6355932203389831
def parseRuleLine(rule_line, rule_id):
    # remove newline
    rule_line = rule_line[:-1]
    # get lift value
    tmp_split = rule_line.split(" #LIFT: ")
    lift = float(tmp_split[1])
    rule_line = tmp_split[0]
    # get confidence value
    tmp_split = rule_line.split(" #CONF: ")
    conf = float(tmp_split[1])
    rule_line = tmp_split[0]
    # get support value
    tmp_split = rule_line.split(" #SUP: ")
    sup = int(tmp_split[1])
    rule_line = tmp_split[0]
    # get rule consequent
    tmp_split = rule_line.split(" ==> ")
    consequent = tmp_split[1]
    # get rule antecedents in a set 
    # (might split some labels too much, but still works to perform the subset comparison)
    antecedents = set(tmp_split[0].split(", "))
    # return all in a tuple (rule_id, antecedents, consequent, sup, conf, lift)
    return (rule_id, antecedents, consequent, sup, conf, lift)

# read rules from file
def parseRules(file_path, file_name):
    file_to_open = file_path / file_name
    # list to gather the rules
    tuple_rule_list = []
    # open file
    with open(file_to_open, "r") as inFile:
        # counter for rule id
        i = 1
        #read and parse rule lines
        for rule_line in inFile:
            tuple_rule_list += [parseRuleLine(rule_line, i)]
            i += 1
    # order rules by descending lift value (last item of the tuples)
    tuple_rule_list = sorted(tuple_rule_list, \
                             key=itemgetter(-1), \
                             reverse=True)
    # debug
    # print(tuple_rule_list)
    # return the parsed rule list of tuples
    return tuple_rule_list

# When a rule is a super rule of another rule and the former has 
# the same or a lower lift, the former rule is considered to be 
# redundant
def findRedundantRules(tuple_rule_list):
    # save list of tuples with ids of redundant rules (and the ids of the ones they're redundant to)
    redundant_rules = []
    # go through original list
    for i in range(len(tuple_rule_list)):
        current = tuple_rule_list[i]
        for j in range(i + 1, len(tuple_rule_list)):
            other = tuple_rule_list[j]
            # check if other.antecedents is a superset of current.antecendents
            # (if other is a super rule of current)
            if other[antecedents_idx].issuperset(current[antecedents_idx]):
                # if other has the same or a lower lift, other is redundant
                if other[lift_idx] <= current[lift_idx]:
                    #print("Rule", str(other[rule_id_idx]), "is redundant because of rule", str(current[rule_id_idx]))
                    #print("Rule", str(other[rule_id_idx]), ":", other)
                    #print("Rule", str(current[rule_id_idx]), ":", current)
                    redundant_rules += [other[rule_id_idx]]
    #return tuples
    return redundant_rules

def saveNonRedundantRules(ids_rules_to_keep, input_folder_path, input_file_name, output_folder_path, output_file_name):
    # read from original file and write only the non-redundant rules to the final file
    with open(input_folder_path / input_file_name, 'r') as inFile:
        with open(output_folder_path / output_file_name, "w") as outFile:
            rules = inFile.readlines()
            for i in range(len(rules)):
                rule = rules[i]
                if (i + 1) in ids_rules_to_keep:
                    outFile.write(rule)

def removeRedundantRules(folder_path, file_name):
    # parse rules from file
    tuple_rule_list = parseRules(folder_path, file_name)    
    # get ids from redundant rules
    redundant_rules = findRedundantRules(tuple_rule_list)
    # copy list
    final_list = tuple_rule_list[:]
    # remove redundant rules
    final_list = [tup for tup in tuple_rule_list if tup[rule_id_idx] not in redundant_rules]
    # get only ids of rules to keep
    ids_rules_to_keep = [tup[rule_id_idx] for tup in final_list]
    # create file name for the filtered file
    non_redundant_rules_file_name = file_name.replace(txt_extension, non_redundant_suffix + txt_extension)
    # reread original file and keep the rules that are not redundant
    # (workaround due to problems with splitting antecedent labels with commas)
    saveNonRedundantRules(ids_rules_to_keep, folder_path, file_name, folder_path, non_redundant_rules_file_name)
    #return filtered list and list of ids from redundant rules
    return final_list, redundant_rules
    
# main function
def start_processing():
    # define folders to process
    folders_to_process = [data_folder_d, data_folder_e]
    # for each folder, find files to process
    for folder in folders_to_process:
        print("Processing folder:", folder)
        # find files to process in the folder
        file_names = [fn for fn in os.listdir(folder) if (fn.startswith(spmf_output_file_name_base) \
                                                      and non_redundant_suffix not in fn ) ]
        # process each file individually
        for f in file_names:
            print("Processing file:", f)
            _, redundant_rules = removeRedundantRules(folder, f)
            print("Created filtered file with less", str(len(redundant_rules)))
    
# script entry point
start_processing()