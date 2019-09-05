from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelBinarizer
from utils.multiscorer import MultiScorer
from sklearn.metrics import confusion_matrix
from sklearn.metrics import classification_report
from sklearn.metrics import accuracy_score
from sklearn.metrics import precision_score
from sklearn.metrics import recall_score
from sklearn.metrics import f1_score
from sklearn.metrics import matthews_corrcoef
from sklearn.metrics import make_scorer
from sklearn.metrics import roc_auc_score
from sklearn.tree import export_graphviz
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from io import StringIO
import math
import re
import operator
from pathlib import Path
from subprocess import call
from utils.constants import translated_labels
from utils.constants import classifier_csv
from utils.constants import bic_str
from mlxtend.evaluate import feature_importance_permutation
from imblearn.metrics import specificity_score

'''
bicluster handling functions
'''
def getExperiencesWithPurestBiclusters(df, list_classes, data_folder, folder_date):
    exp_list = []
    # iterate through all the lines to get the experience names and the 
    # ids of its purest biclusters (purity >= 0.75, for either class)
    for _, row in df.iterrows():
        # get experience id
        exp_id = row[0]
        # create dict with counters for each class
        map_bic_classes = {}
        for c in list_classes:
            map_bic_classes[c] = []
        # check for case with purest bics file and process it
        if row[1].startswith("Exp"):
            purest_bics_file_name = exp_id + "_Purest_Bics_" + folder_date + ".txt"
            with open(data_folder / purest_bics_file_name, "r") as inFile:
                purest_info = inFile.read()
                purest_parts = purest_info.split("\t")
                #remove first (experience name) and last (empty str due to extra tab) elements
                purest_parts = purest_parts[1:-1]
                #process items
                for p in purest_parts:
                    #split each element class:bic_id
                    parts = p.split(":")
                    #update list if class exists in map
                    if parts[0] in map_bic_classes:
                        list_bics = map_bic_classes[parts[0]]
                        list_bics += [parts[1]]
                        map_bic_classes[parts[0]] = list_bics
        # regular case (bic ids in a row)
        else:
            for i in range (1, row.size):
                if type(row[i]) == str:
                    #split each element class:bic_id
                    parts = row[i].split(":")
                    #update list if class exists in map
                    if parts[0] in map_bic_classes:
                        list_bics = map_bic_classes[parts[0]]
                        list_bics += [parts[1]]
                        map_bic_classes[parts[0]] = list_bics
        # if any of the list of biclusters of a given class is not empty,
        # return experience
        for _, value in map_bic_classes.items():
            if len(value) > 0:
                exp_list += [[exp_id, map_bic_classes]]
                break
    #print(exp_list)
    return exp_list

def getNumberOfBiclustersPerExperience(exp_list):
    result = ""
    for value in exp_list:
        map_bic_classes = value[1]
        result += value[0]
        for key, list_bics in map_bic_classes.items():
            result += "\tClass " + str(key) + ": " + str(len(list_bics))
        result += '\n'
    return result

def getAllBiclustersFromExperience(exp_list, exp_id):
    result = []
    for value in exp_list:
        if value[0] == exp_id:
            map_bic_classes = value[1]
            for _, list_bics in map_bic_classes.items():
                result += list_bics
    return result
            

def writeNumBicsPerExpOutput(bics_per_exp, data_folder, output_filename):
    output_file = data_folder / output_filename
    #create file if it does not exist
    with open(output_file, 'w+') as outFile:
        outFile.write(bics_per_exp)

def getTranslatedBiclusterFileNames(data_folder, exp_list):
    #find files with translated biclusters for the respective experiences
    list_bic_file_names = []
    for value in exp_list:
        #get file with biclusters
        bic_translated_file_name = data_folder / (value[0] + translated_labels)
        list_bic_file_names += [bic_translated_file_name]
    return list_bic_file_names

def getClassificationCSVFileNames(data_folder, exp_list):
    #get folder date from data_folder name
    parts = str(data_folder).split("/")
    folder_current_date = "_".join(parts[-1].split("_")[1:])
    #print(folder_current_date)
    #find classification CSV files for the respective experiences
    list_classification_file_names = []
    for value in exp_list:
        #get file with classification matrix for the experience
        classification_file_name = data_folder / (value[0] + classifier_csv + folder_current_date + ".csv")
        list_classification_file_names += [classification_file_name]
    return list_classification_file_names

#function to process bicluster experience file
def processContent(bic_exp_file_name, exp_bic_ids_list): 
    #create list of bicluster prefix lines (start line)
    prefixes = [bic_str + str(x) for x in exp_bic_ids_list]
    read_header_flag = -1
    #list of bicluster list of strings to return
    result = []
    for prefix in prefixes:
        #open experience file and go line by line
        with open(bic_exp_file_name) as inFile:
            for line in inFile:
                #checks if line starts with any of the tuple's prefixes
                if line.startswith(prefix):
                    read_header_flag = 4
                    #first element of list, prefix with bicluster id
                    bic_content = prefix + "\n"
                #skip the first 3 lines of the bicluster info
                if read_header_flag > 0:     
                    read_header_flag -= 1
                elif read_header_flag == 0:
                    #reached empty line at the end of bicluster data
                    if line in ['\n', '\r\n']:
                        #reset flag
                        read_header_flag = -1
                        #print(bic_content + "\n") #debug
                        result += [bic_content]
                        break
                    else:
                        #process first line, avoiding labels with parenthesis (example start line: ' (9,14) ...')
                        if ("(" in line) and line.find('(') == 1:
                            #print(line)
                            #remove bicluster size
                            line = line.split(") ")[1]
                            #remove whitespaces at the beginning and the end of the line
                            line = line.strip()
                            #remove X array with the subject ids
                            line = line.split(" X=")[0]
                            #remove Y array delimiters
                            line = line.replace('Y=[','')[:-1]
                            #replace commas with tabs
                            line = line.replace(",", "\t")
                            #readd newline at the end of line
                            line += "\n"
                            #print(line)                            
                        bic_content += line
    return result

#when exp_list[i][1] is a map
def getBiclusterContents(exp_list, list_bic_file_names, sorted):
    bics_exp = {}
    for i in range(0, len(exp_list)):
        #get experience id
        exp_id = exp_list[i][0]
        #get ids from experience biclusters (append all lists
        # for all classes in one)
        map_bic_classes = exp_list[i][1]
        exp_bic_ids_list = []
        for _, list_bics in map_bic_classes.items():
            exp_bic_ids_list += list_bics
        # sort list of ids as ints
        if(sorted):
            exp_bic_ids_list = sorted(exp_bic_ids_list, key=lambda x: int(x))
        #print(exp_bic_ids_list)
        #get experience file name
        bic_exp_file_name = list_bic_file_names[i]
        #process experience file name to get bics info
        bics_exp[exp_id] = processContent(bic_exp_file_name, exp_bic_ids_list)
    return bics_exp

#when exp_list[i][1] is a list
def getBiclusterContentsFlatList(exp_list, list_bic_file_names, sorted):
    bics_exp = {}
    for i in range(0, len(exp_list)):
        #get experience id
        exp_id = exp_list[i][0]
        #get ids from experience biclusters 
        exp_bic_ids_list = exp_list[i][1]
        # sort list of ids as ints
        if(sorted):
            exp_bic_ids_list = sorted(exp_bic_ids_list, key=lambda x: int(x))
        #print(exp_bic_ids_list)
        #get experience file name
        bic_exp_file_name = list_bic_file_names[i]
        #process experience file name to get bics info
        bics_exp[exp_id] = processContent(bic_exp_file_name, exp_bic_ids_list)
    return bics_exp

def getBiclustersMostFreqPatterns(bics_exp):
    #get the most frequent pattern in the biclusters of each experience
    for key in bics_exp.keys():
        bics_list = bics_exp[key]
        #new list to replace the original (from bicluster values to most common pattern)
        new_bics_list = []
        for bic in bics_list:
            #get bicluster lines
            bic_lines = bic.split("\n")
            #get specific parts
            bic_id = bic_lines[0]
            bic_features = bic_lines[1]
            #bicluster data
            feature_values_dict = {}
            for i in range(2, len(bic_lines)):
                #remove subject ids from bic values
                parts = bic_lines[i].split("\t")
                new_line = "\t".join(parts[1:])
                #count pattern frequency
                if new_line in feature_values_dict:
                    tmp = feature_values_dict[new_line] 
                    tmp += 1
                    feature_values_dict[new_line] = tmp
                else:
                    feature_values_dict[new_line] = 1
            #get key with maximum value from dict
            max_pattern = max(feature_values_dict.items(), key=operator.itemgetter(1))[0] + "\n"
            #join all the parts and add them to the new list
            new_bics_list += [("\n".join([bic_id, bic_features, max_pattern]))]
        #print(new_bics_list)
        bics_exp[key] = new_bics_list
    return bics_exp

def getBiclustersFeatures(bics_exp):
    #get the features in the biclusters of each experience
    for key in bics_exp.keys():
        bics_list = bics_exp[key]
        #new list to replace the original (from bicluster values to most common pattern)
        new_bics_list = []
        for bic in bics_list:
            #get bicluster lines
            bic_lines = bic.split("\n")
            #get specific parts
            bic_id = bic_lines[0]
            bic_features = bic_lines[1] + "\n"
            #join all the parts and add them to the new list
            new_bics_list += [("\n".join([bic_id, bic_features]))]
        #print(new_bics_list)
        bics_exp[key] = new_bics_list
    return bics_exp

def writeBiclustersPatternsOutput(bics_exp, data_folder, output_filename):
    output_file = data_folder / output_filename
    #create file if it does not exist
    with open(output_file, 'w+') as outFile:
        sorted_keys = sorted(bics_exp.keys())
        for key in sorted_keys:
            outFile.write(key + "\n")
            bics_list = bics_exp[key]
            for bic in bics_list:
                outFile.write(bic)
            outFile.write("\n")

'''
classification functions
'''
def printShapes(X_train, X_test, y_train, y_test):
    print("********* Split Dataset shapes: *********")
    print("X_train.shape:", X_train.shape)
    print("X_test.shape:", X_test.shape)
    print("y_train.shape:", y_train.shape)
    print("y_test.shape:", y_test.shape)

def fitRFClassifier(X_train, y_train, n_estimators):
    # Fit the Random Forest classifier
    rf = RandomForestClassifier(n_estimators=n_estimators, random_state=0)
    rf.fit(X_train, y_train)
    return rf
    
def getClassificationMetrics(model, X_train, X_test, y_train, y_test):
    print("********* Classification Metrics without Cross-Validation: *********")
    # Get metrics from classifier using training set
    # Confusion matrix
    train_pred = model.predict(X_train)
    print("Training Confusion Matrix:")
    y_actu = pd.Series(y_train, name='True')
    y_pred = pd.Series(train_pred, name='Pred')
    cm_train = pd.crosstab(y_actu, y_pred)
    print(cm_train)
    # Classification Report 
    print("Training Classification Report:")
    print(classification_report(y_train, train_pred))
    # Accuracy Score
    print("Training Accuracy Score:")
    print(accuracy_score(y_train, train_pred))

    # Get metrics from classifier using test set
    # Confusion matrix
    test_pred = model.predict(X_test)
    print("Test Confusion Matrix:")
    y_actu = pd.Series(y_test, name='True')
    y_pred = pd.Series(test_pred, name='Pred')
    cm_test = pd.crosstab(y_actu, y_pred)
    print(cm_test)
    # Classification Report 
    print("Test Classification Report:")
    print(classification_report(y_test, test_pred))
    # Accuracy Score
    print("Test Accuracy Score:")
    print(accuracy_score(y_test, test_pred))

# adapted from https://medium.com/@plog397/auc-roc-curve-scoring-function-for-multi-class-classification-9822871a6659
# y_pred = model.predict_proba(X_test)
def multiclass_roc_auc_score(y_test, y_pred, average="micro"):
    lb = LabelBinarizer()
    lb.fit(y_test)
    y_test = lb.transform(y_test)
    y_pred = lb.transform(y_pred)
    return roc_auc_score(y_test, y_pred, average=average)

def getClassificationMetricsCV(model, k, X, y):
    print("********* Classification Metrics with Cross-Validation: *********")
    #calculate accuracy, precision and recall with stratified CV k-fold
    scorer = MultiScorer({
	    'Accuracy' : (accuracy_score, {}),
	    'Precision': (precision_score,{'average': 'macro'}),
	    'Recall'   : (recall_score, {'average': 'macro'}),
        'F1-score' : (f1_score, {'average': 'macro'}),
        'Specificity' : (specificity_score, {'average': 'macro'}),
        'MCC' : (matthews_corrcoef, {}),
        'AUC' : (multiclass_roc_auc_score, {'average': 'macro'})
    })
    cross_val_score(model, X, y, cv=k, scoring=scorer)
    results = scorer.get_results()
    #print(results)
    for metric_name in results.keys():
        average_metric_score = np.average(results[metric_name])
        std_metric_score = np.std(results[metric_name])
        max_metric_score = np.max(results[metric_name])
        min_metric_score = np.min(results[metric_name])
        print('Test %s -> mean: %0.3f std: +/- %0.3f max: %0.3f min: %0.3f' % (metric_name, average_metric_score, \
              std_metric_score, max_metric_score, min_metric_score))

#figure_path must be a PosixPath object
def getFeatureImportanceSklearn(n, model, X, y, feature_names, figure_path, figure_filename):
    print("*********", n, "Most Important Features: *********")
    n_features = n
    # get model feature importances
    importances = model.feature_importances_
    # get the features indexes sorted by importance
    indices = np.argsort(importances)[::-1]
    # gather and print the 10 features with greater importances
    bar_widths_ranked = []
    feature_names_ranked = []
    print("Feature ranking:")
    for f in range(n_features):
        bar_widths_ranked += [importances[indices[f]]]
        feature_names_ranked += [feature_names[indices[f]]]
        print("%d. feature %s (%f)" % (f + 1, feature_names[indices[f]], importances[indices[f]]))
    # save plot
    plt.figure(figsize=(12,7))
    plt.barh(range(n_features), bar_widths_ranked, align='center')
    plt.yticks(np.arange(n_features), feature_names_ranked)
    plt.xlabel("Feature Importance (sklearn default)")
    plt.ylabel("Feature Name")
    plt.ylim(-1, n_features)
    plt.savefig(figure_path / figure_filename, bbox_inches='tight')
    #return bicluster names
    return feature_names_ranked

#figure_path must be a PosixPath object
def getPermutationImportanceMLxtend(num_rounds, model, X_test, y_test, feature_names, width_perm_imp_plot, \
                                    figure_path, figure_filename, top_k='All'):
    
    # calculate permutation importance values (hardcoded seed for reproducibility)
    imp_vals, imp_all = feature_importance_permutation(predict_method=model.predict, 
                                                       X=X_test,
                                                       y=y_test,
                                                       metric='accuracy',
                                                       num_rounds=num_rounds,
                                                       seed=1597)
    # calculate std dev
    std = np.std(imp_all, axis=1)
    # get indices from ranking
    indices = np.argsort(imp_vals)[::-1]
    # get labels in ranking order
    labels = []
    for i in indices:
        labels += [feature_names[i]]
    # print information
    print("********* Most Important Features (Mean Permutation Importance with Std. Dev.): *********")
    n_features = len(feature_names) if top_k == 'All' else int(top_k)
    if top_k != 'All':
        print("Note: Showing only top-" + str(top_k) + " features" )
    for i in range(n_features):
        print("%d. feature %s (%f +/- %f)" % (i + 1, labels[i], imp_vals[indices[i]], std[indices[i]]))
    # create figure
    plt.figure(figsize=(width_perm_imp_plot,7))
    # title
    plt.title("RF Classifier Mean Permutation Importance (with Std. Dev.)")
    # horizontal line for y = 0
    plt.hlines(0, -1, n_features, colors='k', linestyles='dotted')
    # create bars
    if top_k != 'All':
        plt.bar(range(n_features), imp_vals[indices[:top_k]], yerr=std[indices[:top_k]])
    else:
        plt.bar(range(n_features), imp_vals[indices], yerr=std[indices])
    # set labels on features
    if top_k != 'All':
        plt.xticks(range(n_features), labels[:top_k], rotation=90)
    else:
        plt.xticks(range(n_features), labels, rotation=90)
    # set x axis limits
    plt.xlim([-1, n_features])
    # set axis labels
    plt.xlabel("Feature Name")
    plt.ylabel("Mean Feature Permutation Importance (MLxtend)")
    # save figure
    plt.savefig(figure_path / figure_filename, bbox_inches='tight')
    # return ranked feature names used in the figure
    if top_k != 'All':
        return labels[:top_k]
    else:
        return labels
     