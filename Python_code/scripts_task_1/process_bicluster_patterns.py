'''
Script to print Bicluster patterns in association rule format (feature|value)
'''

#imports
from pathlib import Path
from subprocess import call
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys
import os
from utils.constants import bic_str

#numpy settings (to print whole ndarrays without omitting values)
np.set_printoptions(threshold=sys.maxsize)

# folder date
folder_date = "2019-7-13_16-15-55"
# string folder path
str_path = "/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_"
# folders where the files are 
folder_c = Path(str_path + folder_date + "/classification_merged_data/")
# file name
file_name = "Permutation_Importance_MLxtend_Classif_merged_data.txt"

# main function
def start_processing():
    file_to_open = folder_c / file_name
    # open file
    with open(file_to_open, "r") as inFile:
        #ignore first line (experience name)
        inFile.readline()
        #read the rest of the lines
        first = True
        features = [] 
        values = []
        for line in inFile:
            # print bicluster id
            if line.startswith(bic_str):
                print(line[:-1])
            else:
                #read features list
                if first:
                    features = line[:-1].split("\t")
                    #print("features:", features)
                    first = False
                #read values list
                else:
                    values = line[:-1].split("\t")[:-1]
                    #print("values:", values)
                    first = True
                #after reading both lists, process it as an association rule
                #(feature|value, feature|value...)
                if len(features) > 0 and len(values) > 0:
                    #print("pattern building")
                    pattern = ""
                    for i in range(len(features)):
                        if i < (len(features) - 1):   
                            pattern += features[i] + "|" + values[i] + ", "
                        else:
                            pattern += features[i] + "|" + values[i]
                    #print pattern
                    print(pattern.strip() + "\n")
                    #reset lists for next bicluster
                    features = [] 
                    values = []

# script entry point
start_processing()