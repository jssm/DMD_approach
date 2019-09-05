import pandas as pd
from utils.constants import LEVEL_1_keyword, OCCUPATION_keyword, ALSFRSR_keyword

'''
file and data loading functions
'''
def readMetricsExcelData(data_folder, file_name, sheet_name_or_index):
    file_to_open = data_folder / file_name
    #read sheet
    df = pd.read_excel(file_to_open, sheet_name=sheet_name_or_index, header=None)
    #convert all feature values to string
    df = df.astype(str)
    return df

# for script a_baseline_classification.py

#remove .0 from values
def removeFloatPartOfString(s):   
    #forceful convertion to str
    if type(s) != "str":
        s = str(s)       
    #no point, nothing to do
    if s.find(".") == -1:
        return s
    else:
        #remove all after the dot (inclusive)
        return s[:s.find(".")]

#returns ndarrays and a list of features
def load_original_data_with_missings(file_name, features_to_keep=['ALL']):
    df = pd.read_csv(file_name, engine='python')
    # keep only the features in features_to_keep
    if features_to_keep[0] != 'ALL':
        df = df[features_to_keep]
    #convert missings to -1
    df = df.fillna(-1) 
    #convert all feature values to string
    df = df.astype(str)
    #sort values by group class
    df = df.sort_values(by=['group'])
    #remove all decimal parts 
    for i in range(0, df.columns.size):
        df[df.columns[i]] = df[df.columns[i]].apply(removeFloatPartOfString)     
    #convert the data frame read from the csv file to a matrix
    table_X = df.values
    #table_y: last column of the file, the target class column
    table_y = table_X[:, -1]
    #cast all table_y elements to int
    table_y = table_y.astype(int)
    #remove first (subject id) and last (target class) columns of the data matrix 
    #to only keep the data
    table_X = table_X[:, list(range(1,len(df.columns) - 1))]
    #cast all table_X elements to int
    table_X = table_X.astype(int)
    #get list of feature names (also removing first and last columns)
    feature_names = list(df)[1:-1]
    #return the feature data and the target data
    return table_X, table_y, feature_names

# for script b_subject_biclusters_classification.py

#returns ndarrays and a list of features
def load_matrix_data_no_missings(file_name):
    df = pd.read_csv(file_name, engine='python')
    #convert the data frame read from the csv file to a matrix
    table_X = df.values
    #table_y: last column of the file, the target class column
    table_y = table_X[:, -1]
    #cast all table_y elements to int
    table_y = table_y.astype(int)
    #remove first (subject id) and last (target class) columns of the data matrix 
    #to only keep the data
    table_X = table_X[:, list(range(1,len(df.columns) - 1))]
    #cast all table_X elements to int
    table_X = table_X.astype(int)
    #get list of feature names (also removing first and last columns)
    feature_names = list(df)[1:-1]
    #return the feature data and the target data
    return table_X, table_y, feature_names

# for script c_merged_data_classification.py

#returns dataframe
def load_to_merge_original_data_with_missings(file_name, features_to_keep=['ALL']):
    df = pd.read_csv(file_name, engine='python')
    # keep only the features in features_to_keep
    if features_to_keep[0] != 'ALL':
        df = df[features_to_keep]
    #convert missings to -1
    df = df.fillna(-1) 
    #convert all to string
    df = df.astype(str)
    #remove all decimal parts 
    for i in range(0, df.columns.size):
        df[df.columns[i]] = df[df.columns[i]].apply(removeFloatPartOfString)     
    return df

#returns dataframe
def load_to_merge_matrix_data_no_missings(file_name):
    df = pd.read_csv(file_name, engine='python')
    return df

# for baseline scripts (to translate from category values to labels)
def correctZeroPrefixCategories(value):
    if value not in ["NA", "nan", "NaN"] and 0 <= int(value) < 10:
        return "0" + value
    else:
        return value

def getTranslationMapCategoriesLabels(data_folder, xslx_translation_categories_labels):
    # create map
    label_map = {}
    # read file
    file_to_open = data_folder / xslx_translation_categories_labels
    # read first and only sheet (with header in the first line)
    label_data = pd.read_excel(file_to_open, sheet_name=0, header=0)
    # convert everything to strings (NaN are "nan" now and integers gain a decimal case :S) 
    label_data = label_data.astype(str)
    #for each second column, remove all decimal parts 
    for i in range(0, label_data.columns.size, 2):
        label_data[label_data.columns[i+1]] = label_data[label_data.columns[i+1]].apply(removeFloatPartOfString) 
        # if column is one of ALSFRSR, remove floats from first column too
        if (ALSFRSR_keyword in label_data.columns[i]):
            label_data[label_data.columns[i]] = label_data[label_data.columns[i]].apply(removeFloatPartOfString) 
    # #failsafe for Occupation (level 1) columns (that have categories like 01)
    # for i in range(0, label_data.columns.size, 2):
    #     if (OCCUPATION_keyword in label_data.columns[i]) and (LEVEL_1_keyword in label_data.columns[i]):
    #         label_data[label_data.columns[i+1]] = label_data[label_data.columns[i+1]].apply(correctZeroPrefixCategories)  
    # iterate through all columns, two by two (first column: labels, second column: category values)
    for i in range(0, label_data.columns.size, 2):
        # get column name
        columnName = label_data.columns[i]
        # remove nan's from labels and category values' columns
        temp_labels = [x for x in label_data[label_data.columns[i]].tolist() if x != 'nan']
        temp_cat_values = [x for x in label_data[label_data.columns[i + 1]].tolist() if x != 'nan']
        # create new map
        feature_map = {}
        # add category values as keys, labels as values
        for j in range(0, len(temp_labels)):
            feature_map[temp_cat_values[j]] = temp_labels[j]
        # add feature map to label map
        label_map[columnName] = feature_map

    #return map
    return label_map



