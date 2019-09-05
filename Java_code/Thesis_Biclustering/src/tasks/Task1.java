package tasks;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import bic_translator.BiclustersTranslator.TranslationMode;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import generator.BicMatrixGenerator.PatternType;
import utils.Utils;
import utils.Utils.RemoveCriteria;
import utils.Utils.StoppingCriteria;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 3.0
 *  
 *  TASK 1 
 *  Data used: ONWebDUALS Lisbon Patients and Controls
 *  Objective: Experiment to obtain the purest Biclusters in order to discover the most discriminative 
 *  features or sets of features between both classes
 */

public class Task1 {

	public static void main(String[] args) throws Exception {
		
		try {		
			long startTime = System.nanoTime();
			runQuestion1();	
			long endTime   = System.nanoTime();
			long diffTime = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
			System.out.println("Total Running Time: " + diffTime + " seconds");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public static void runQuestion1() throws IOException {
		
		String currentDate = Utils.getCurrentDate();
		TranslationMode translationMode = TranslationMode.Both;
	    	    	    	
    	String mainFolder = "/Users/joana/Desktop/MCD/Thesis/Data/transformations/OnWebDUALS_LISB_511_300_converted_2019-07-09/Task_1/";
		String dataset = mainFolder + "ARFF_missings_ALL_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.arff";		
		String csvDataset = mainFolder + "CSV_missings_ALL_PT_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.csv";	
		String experimentSetFileFolder = "/Users/joana/Desktop/MCD/Thesis/Data/results/Task_1/Task_1_results_" + currentDate;
		String translationIndexesToCategoryValues = mainFolder + "indexes_to_categories_ALL_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.tsv";
		String translationCategoryValuesToLabels = mainFolder + "categories_labels_ALL_OnWebDUALS_LISB_511_300_converted_2019-07-09_controls_selected.xlsx";
		
		File directory = new File(experimentSetFileFolder);
	    if (!directory.exists()){
	        directory.mkdir();
	    }

    	StoppingCriteria[] stoppingCriteria = {StoppingCriteria.MinSupportPercentageRowsPerBic}; 						
		Double[] stoppingCriteriaValue = {0.6D, 0.55D, 0.50D, 0.45D, 0.4D, 
										  0.35D, 0.3D, 0.25D, 0.2D, 0.15D, 
										  0.1D, 0.05D, 0.025D, 0.01D};
		Integer[] minNrColumns = {3};										
		Integer[] nrIterations = {1};							
		Integer[] nrItems = {50};								
		Boolean[] symmetric = {false};  							
		Double[] minOverlapMerging = {1D}; 					
		FilteringCriteria[] filter = {FilteringCriteria.Overall};								
		FillingCriteria[] missingsHandler = {FillingCriteria.RemoveValue};						
		RemoveCriteria[] removeElements = {RemoveCriteria.RemoveNone};			//RemoveNone -> deve ter scalability a true							
		Double[] minFilteringValue = {25D};						
		PatternType[] patternType = {PatternType.Constant};							
		ClosedImplementation[] closedImplementation = {ClosedImplementation.DCharm};					
		Orientation[] orientation = {Orientation.PatternOnRows};	
		Double[] significanceLevel = {0.05D}; 		
		Boolean[] scalability = {false};  										//features already selected on KNIME
		Double[] columnFilteringPerc = {-1D}; 									//negative value forces to use the original code
		Boolean[] discriminative = {true};  
		String[] targetClassName = {"group"};
		Boolean[] printPatternsOnly = {false};
		
		Utils.createAndRunExperimentSet(currentDate, dataset, csvDataset, stoppingCriteria, stoppingCriteriaValue,
								  minNrColumns, nrIterations, nrItems, symmetric, minOverlapMerging,
								  filter, missingsHandler, removeElements, minFilteringValue, patternType,
								  closedImplementation, orientation, significanceLevel, scalability,
								  columnFilteringPerc, discriminative, targetClassName,
								  printPatternsOnly, experimentSetFileFolder, translationIndexesToCategoryValues,
								  translationCategoryValuesToLabels, translationMode);
	}
}
