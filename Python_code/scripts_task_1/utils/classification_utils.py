from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.model_selection import cross_validate
from sklearn.preprocessing import LabelBinarizer
from sklearn.metrics import confusion_matrix
from sklearn.metrics import classification_report
from sklearn.metrics import accuracy_score
from sklearn.metrics import precision_score
from sklearn.metrics import recall_score
from sklearn.metrics import f1_score
from sklearn.metrics import matthews_corrcoef
from sklearn.metrics import make_scorer
from sklearn.metrics import roc_curve
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
from utils.constants import roc_curve_file_name
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
                        #process first line
                        if "(" in line:
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

def calculateAUC_ROC_curve(model, X_test, y_test, figure_path, figure_filename):
    print("********* Classification AUC: *********")
    # predict probabilities
    probs = model.predict_proba(X_test)
    #print(probs)
    # keep probabilities for the positive outcome only
    probs = probs[:, 1]
    # calculate AUC
    auc = roc_auc_score(y_test, probs)
    print('AUC: %.3f' % auc)
    # calculate roc curve: pos_label=2 because we are using 1 and 2 as class values
    fpr, tpr, _ = roc_curve(y_test, probs, pos_label=2)
    # export roc curve data
    with open(figure_path / roc_curve_file_name, "w") as outFile: 
        #save FPR data
        outFile.write("FPR\t")
        for i in range(len(fpr)):
            outFile.write(str(fpr[i]))
            # last element
            if i == (len(fpr) - 1):
                outFile.write("\n")
            else:
                outFile.write("\t")
        #save TPR data
        outFile.write("TPR\t")
        for i in range(len(tpr)):
            outFile.write(str(tpr[i]))
            # last element
            if i == (len(tpr) - 1):
                outFile.write("\n")
            else:
                outFile.write("\t")
        #save AUC (for label)
        outFile.write("AUC\t" + str(auc))
    # plot no skill
    plt.plot([0, 1], [0, 1], linestyle='--')
    # plot the roc curve for the model
    plt.plot(fpr, tpr, marker='.')
    # save the plot
    plt.savefig(figure_path / figure_filename, bbox_inches='tight')
    
def getClassificationMetrics(model, X_train, X_test, y_train, y_test):
    print("********* Classification Metrics without Cross-Validation: *********")
    # Get metrics from classifier using training set
    # Confusion matrix
    train_pred = model.predict(X_train)
    print("Training Confusion Matrix:")
    cm_train = confusion_matrix(y_train, train_pred)
    tn, fp, fn, tp = cm_train.ravel()
    #print(cm_train)
    print("TP:", str(tp), "FN:", str(fn)) 
    print("FP:", str(fp), "TN:", str(tn))
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
    cm_test = confusion_matrix(y_test, test_pred)
    tn, fp, fn, tp = cm_test.ravel()
    #print(cm_test)
    print("TP:", str(tp), "FN:", str(fn)) 
    print("FP:", str(fp), "TN:", str(tn))
    # Classification Report 
    print("Test Classification Report:")
    print(classification_report(y_test, test_pred))
    # Accuracy Score
    print("Test Accuracy Score:")
    print(accuracy_score(y_test, test_pred))

def getClassificationMetricsCV(model, k, X, y):
    print("********* Classification Metrics with Cross-Validation: *********")
    #Binarize the class label vector y
    lb = LabelBinarizer()
    y_binary = lb.fit_transform(y).flatten()
    #scoring functions definition
    scoring = {'accuracy': make_scorer(accuracy_score),
               'precision': make_scorer(precision_score),
               'recall': make_scorer(recall_score),
               'f1_score': make_scorer(f1_score),
               'specificity': make_scorer(specificity_score),
               'matt_coef': make_scorer(matthews_corrcoef),
               'auc': make_scorer(roc_auc_score)}
    #calculate accuracy, precision and recall with stratified CV k-fold
    scores = cross_validate(model, X, y_binary, cv=k, scoring=scoring, return_train_score=True)
    print("Train accuracy: %0.3f (+/- %0.3f)" % (scores['train_accuracy'].mean(), scores['train_accuracy'].std()))
    print("Train precision: %0.3f (+/- %0.3f)" % (scores['train_precision'].mean(), scores['train_precision'].std()))
    print("Train recall: %0.3f (+/- %0.3f)" % (scores['train_recall'].mean(), scores['train_recall'].std()))
    print("Train f1_score: %0.3f (+/- %0.3f)" % (scores['train_f1_score'].mean(), scores['train_f1_score'].std()))
    print("Train specificity: %0.3f (+/- %0.3f)" % (scores['train_specificity'].mean(), scores['train_specificity'].std()))
    print("Train matt_coef: %0.3f (+/- %0.3f)" % (scores['train_matt_coef'].mean(), scores['train_matt_coef'].std()))
    print("Train auc: %0.3f (+/- %0.3f)" % (scores['train_auc'].mean(), scores['train_auc'].std()))
    print("Test accuracy: %0.3f (+/- %0.3f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std()))
    print("Test precision: %0.3f (+/- %0.3f)" % (scores['test_precision'].mean(), scores['test_precision'].std()))
    print("Test recall: %0.3f (+/- %0.3f)" % (scores['test_recall'].mean(), scores['test_recall'].std()))
    print("Test f1_score: %0.3f (+/- %0.3f)" % (scores['test_f1_score'].mean(), scores['test_f1_score'].std()))
    print("Test specificity: %0.3f (+/- %0.3f)" % (scores['test_specificity'].mean(), scores['test_specificity'].std()))
    print("Test matt_coef: %0.3f (+/- %0.3f)" % (scores['test_matt_coef'].mean(), scores['test_matt_coef'].std()))
    print("Test auc: %0.3f (+/- %0.3f)" % (scores['test_auc'].mean(), scores['test_auc'].std()))

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
     