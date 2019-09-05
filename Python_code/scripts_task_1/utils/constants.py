#constants
most_important_features = "Feature_Importance_Sklearn_Classif"
rfpimp_features = "Permutation_Importance_RFPIMP_Classif"
mlxtend_features = "Permutation_Importance_MLxtend_Classif"
auc_model = "AUC_Classif"
dt_from_rf_classifier = "DT_From_RF_Classif"
translated_categories = "_translated_categories.txt" 
translated_labels = "_translated_labels.txt" 
classifier_csv = "_CSV_classifier_"
bic_str = "BICLUSTER #"
target_class_group = 'group'
non_redundant_suffix = "_non_redundant"

java = 'java'
jar = '-jar'
run = 'run'
zart_fpclose = 'Closed_association_rules(using_fpclose)'
rule_sup = '#SUP:'
rule_conf = '#CONF:'
rule_lift = '#LIFT:'
rule_imp = '==>'
itemset = ' ITEMSET :'
emptyset = 'EMPTYSET'
java_max_memory = '-Xmx1024m'
java_max_memory_2 = '-Xmx14336m'
fpclose = 'FPClose'
fpgrowth_with_lift = "FPGrowth_association_rules_with_lift"
closed_arm_charm = "Closed_association_rules"

# spmf output file name
spmf_output_file_name = "SPMF_closed_ARM_results.txt"
# spmf translated output file name
spmf_output_translated_file_name = "SPMF_closed_ARM_results_translated.txt"
# spmf translated (and filtered class on the consequent) output file name
spmf_output_filtered_file_name = "SPMF_closed_ARM_results_filtered.txt"
# smpf file name base for splitting the rules by class
spmf_output_file_name_base = "SPMF_closed_ARM_results_class_"
# spmf file name for the intermediate file with the closed itemsets
spmf_closed_itemsets_file_name = "SPMF_closed_itemsets_results.txt"
# spmf file name for the intermediate file with the closed itemsets (without the support -> ready for ARM)
spmf_closed_itemsets_for_ARM_file_name = "SPMF_closed_itemsets_for_ARM.txt"
spmf_output_with_lift_file_name = "SPMF_closed_ARM_results_with_lift.txt"
spmf_freq_itemsets_section = "List of frequent itemsets"
txt_extension = ".txt"

#name of the output file that will contain the number of purest biclusters per experience 
num_purest_bics_per_exp_filename = "Bics_Num_Purest_Per_Exp.tsv"
#name of the output file that will contain the most frequent patterns for each purest bicluster per experience
patterns_output_filename = "Bics_Purest_Most_Freq_Patterns.tsv"
#name of the output file that will contain the features of the most important metafeatures (Biclusters)
#considered by the RF classifier
most_important_bics_features = "Bics_Most_Important_Metafeatures_Features_"
# name for the closed frequent itemsets file
closed_freq_itemsets = "Closed_Freq_Itemsets"

# ROC curve data points file
roc_curve_file_name = "ROC_Curve.txt"

# parsing labels constants
OCCUPATION_keyword = "Occupation"
LEVEL_1_keyword = "(level 1)"
ALSFRSR_keyword = "ALSFRSR"