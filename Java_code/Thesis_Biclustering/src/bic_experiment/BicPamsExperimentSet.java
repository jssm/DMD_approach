package bic_experiment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import bic_metrics.BiclustersEvaluator;
import bic_metrics.MetricsExperiment;
import bic_translator.BiclustersTranslator;
import bic_translator.BiclustersTranslator.TranslationMode;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import utils.BicReader;
import utils.Utils;
import utils.Utils.RemoveCriteria;
import utils.Utils.StoppingCriteria;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 1.0
 */

public class BicPamsExperimentSet {
	
	//data fields
	private String currentDate;								//String with current date (YYYY-MM-DD_hh-mm-ss)
	private String dataset; 								//String for dataset file path (ARFF)
	private String csvDataset; 								//String for dataset file path (CSV)
	private StoppingCriteria[] stoppingCriteria; 			//Stopping criteria type
	private Double[] stoppingCriteriaValue;					//Stopping criteria value
	private Integer[] minNrColumns;							//Minimum #columns
	private Integer[] nrIterations;							//Number of iterations, increase for more even space exploration 
	private Integer[] nrItems;								//Coherency Strength (#items)
	private Boolean[] symmetric;  							//Symmetries
	private Double[] minOverlapMerging; 					//Quality
	private FilteringCriteria[] filter;						//Merging procedure
	private FillingCriteria[] missingsHandler;				//Missings Handler
	private RemoveCriteria[] removeMissings;				//Type of missing values removal
	private Double[] minFilteringValue;						//Filtering arguments
	private PatternType[] patternType;						//Coherency assumption
	private ClosedImplementation[] closedImplementation;	//Pattern representation (Closed (maximal bics))
	private Orientation[] orientation;						//Coherency orientation
	private Double[] significanceLevel;						//Significance level (alpha) 	
	private Boolean[] scalability;							//Flag that indicates if scalable measures should be used
	private Double[] columnFilteringPerc;					//Percentage of the dataset columns with largest variability to keep 
	private Boolean[] discriminative;						//Flag to know if the Biclustering should be discriminative (has a target class)
	private String[] targetClassName;						//Name of the target class (column/feature name)
	private Boolean[] printPatternsOnly;					//Flag to print Bicluster patterns only; if false or PatternType is not Constant, whole bicluster is printed
	private String experimentSetFileFolder;					//Where output files are saved
	private List<BicPamsExperiment> experimentList;			//List of experiments	
	private String translationIndexToCategoryFilePath; 		//String for indexes to discretization categories translation file path
	private String translationCategoryToLabelFilePath; 		//String for categories to labels translation file path
	private BiclustersTranslator translator;				//Translator of BicPAMS indexes to discretization categories or labels
	private TranslationMode translationMode;				//Flag to know what translation modes have to be done	
	
	//auxiliary parameter strings
	private static final String stoppingCriteriaStr = "stoppingCriteria";
	private static final String stoppingCriteriaValueStr = "stoppingCriteriaValue";
	private static final String minNrColumnsStr = "minNrColumns";
	private static final String nrIterationsStr = "nrIterations";
	private static final String nrItemsStr = "nrItems";
	private static final String symmetricStr = "symmetric";
	private static final String minOverlapMergingStr = "minOverlapMerging";
	private static final String filterStr = "filter";
	private static final String missingsHandlerStr = "missingsHandler";
	private static final String removeMissingsStr = "removeMissings";
	private static final String minFilteringValueStr = "minFilteringValue";
	private static final String patternTypeStr = "patternType";
	private static final String closedImplementationStr = "closedImplementation";
	private static final String orientationStr = "orientation";
	private static final String significanceLevelStr = "significanceLevel";
	private static final String scalabilityStr = "scalability";
	private static final String columnFilteringPercStr = "columnFilteringPerc";
	private static final String discriminativeStr = "discriminative";
	private static final String targetClassNameStr = "targetClassName";
	private static final String printPatternsOnlyStr = "printPatternsOnly";

	//standard constructor
	public BicPamsExperimentSet(String currentDate, String dataset, String csvDataset, StoppingCriteria[] stoppingCriteria, 
								Double[] stoppingCriteriaValue, Integer[] minNrColumns, Integer[] nrIterations, Integer[] nrItems, 
								Boolean[] symmetric, Double[] minOverlapMerging, FilteringCriteria[] filter, 
								FillingCriteria[] missingsHandler, RemoveCriteria[] removeMissings, Double[] minFilteringValue, 
								PatternType[] patternType, ClosedImplementation[] closedImplementation, Orientation[] orientation, 
								Double[] significanceLevel, Boolean[] scalability, Double[] columnFilteringPerc, Boolean[] discriminative,
								String[] targetClassName, Boolean[] printPatternsOnly, String experimentSetFileFolder, 
								String translationIndexToCategoryFilePath, String translationCategoryToLabelFilePath, 
								TranslationMode translationMode) {
		
		this.currentDate = currentDate;
		this.dataset = dataset;
		this.csvDataset = csvDataset;
		this.stoppingCriteria = stoppingCriteria;
		this.stoppingCriteriaValue = stoppingCriteriaValue;
		this.minNrColumns = minNrColumns;
		this.nrIterations = nrIterations;
		this.nrItems = nrItems;
		this.symmetric = symmetric;
		this.minOverlapMerging = minOverlapMerging;
		this.filter = filter;
		this.missingsHandler = missingsHandler;
		this.removeMissings = removeMissings;
		this.minFilteringValue = minFilteringValue;
		this.patternType = patternType;
		this.closedImplementation = closedImplementation;
		this.orientation = orientation;
		this.significanceLevel = significanceLevel;
		this.scalability = scalability;
		this.columnFilteringPerc = columnFilteringPerc;
		this.discriminative = discriminative;
		this.targetClassName = targetClassName;
		this.printPatternsOnly = printPatternsOnly;
		
		this.experimentSetFileFolder = experimentSetFileFolder;
		this.translationIndexToCategoryFilePath = translationIndexToCategoryFilePath;	
		this.translationCategoryToLabelFilePath = translationCategoryToLabelFilePath;
		this.translationMode = translationMode;

		try {
			//setup translators
			this.translator = new BiclustersTranslator(this.translationIndexToCategoryFilePath, this.translationCategoryToLabelFilePath);
			//define experiment set
			defineExperimentSet();
		} catch (Exception e) {
			System.out.println("Definition of ExperimentSet failed.");
			e.printStackTrace();
		}
	}
	
	//default constructor
	public BicPamsExperimentSet(String dataset, String csvDataset, String experimentSetFileFolder, String translationIndexToCategoryFilePath,
			String translationCategoryToLabelFilePath, TranslationMode translationMode) {
		
		this.dataset = dataset;
		this.csvDataset = csvDataset;
			
		Object[] stoppingCriteriaArray = getDefaultParameterArray(stoppingCriteriaStr);	
		this.stoppingCriteria = Arrays.copyOf(stoppingCriteriaArray, stoppingCriteriaArray.length, StoppingCriteria[].class);
		
		Object[] stoppingCriteriaValueArray = getDefaultParameterArray(stoppingCriteriaValueStr);		
		this.stoppingCriteriaValue = Arrays.copyOf(stoppingCriteriaValueArray, stoppingCriteriaValueArray.length, Double[].class);
		
		Object[] minNrColumnsArray = getDefaultParameterArray(minNrColumnsStr);
		this.minNrColumns = Arrays.copyOf(minNrColumnsArray, minNrColumnsArray.length, Integer[].class);
		
		Object[] nrIterationsArray = getDefaultParameterArray(nrIterationsStr);
		this.nrIterations = Arrays.copyOf(nrIterationsArray, nrIterationsArray.length, Integer[].class);
		
		Object[] nrItemsArray = getDefaultParameterArray(nrItemsStr);
		this.nrItems = Arrays.copyOf(nrItemsArray, nrItemsArray.length, Integer[].class);
		
		Object[] symmetricArray = getDefaultParameterArray(symmetricStr);
		this.symmetric = Arrays.copyOf(symmetricArray, symmetricArray.length, Boolean[].class);
		
		Object[] minOverlapMergingArray = getDefaultParameterArray(minOverlapMergingStr);
		this.minOverlapMerging = Arrays.copyOf(minOverlapMergingArray, minOverlapMergingArray.length, Double[].class);
		
		Object[] filterArray = getDefaultParameterArray(filterStr);
		this.filter = Arrays.copyOf(filterArray, filterArray.length, FilteringCriteria[].class);
		
		Object[] missingsHandlerArray = getDefaultParameterArray(missingsHandlerStr);
		this.missingsHandler = Arrays.copyOf(missingsHandlerArray, missingsHandlerArray.length, FillingCriteria[].class);
		
		Object[] removeMissingsArray = getDefaultParameterArray(removeMissingsStr);
		this.removeMissings = Arrays.copyOf(removeMissingsArray, removeMissingsArray.length, RemoveCriteria[].class);
		
		Object[] minFilteringValueArray = getDefaultParameterArray(minFilteringValueStr);
		this.minFilteringValue = Arrays.copyOf(minFilteringValueArray, minFilteringValueArray.length, Double[].class);
		
		Object[] patternTypeArray = getDefaultParameterArray(patternTypeStr);
		this.patternType = Arrays.copyOf(patternTypeArray, patternTypeArray.length, PatternType[].class);
		
		Object[] closedImplementationArray = getDefaultParameterArray(closedImplementationStr);
		this.closedImplementation = Arrays.copyOf(closedImplementationArray, closedImplementationArray.length, 
																				  ClosedImplementation[].class);
		
		Object[] orientationArray = getDefaultParameterArray(orientationStr);
		this.orientation = Arrays.copyOf(orientationArray, orientationArray.length, Orientation[].class);

		Object[] significanceLevelArray = getDefaultParameterArray(significanceLevelStr);		
		this.significanceLevel = Arrays.copyOf(significanceLevelArray, significanceLevelArray.length, Double[].class);
		
		Object[] discriminativeArray = getDefaultParameterArray(discriminativeStr);
		this.discriminative = Arrays.copyOf(discriminativeArray, discriminativeArray.length, Boolean[].class);
		
		Object[] scalabilityArray = getDefaultParameterArray(scalabilityStr);
		this.scalability = Arrays.copyOf(scalabilityArray, scalabilityArray.length, Boolean[].class);
		
		Object[] columnFilteringPercArray = getDefaultParameterArray(columnFilteringPercStr);
		this.columnFilteringPerc = Arrays.copyOf(columnFilteringPercArray, columnFilteringPercArray.length, Double[].class);

		Object[] targetClassNameArray = getDefaultParameterArray(targetClassNameStr);		
		this.targetClassName = Arrays.copyOf(targetClassNameArray, targetClassNameArray.length, String[].class);
		
		Object[] printPatternsOnlyArray = getDefaultParameterArray(printPatternsOnlyStr);
		this.printPatternsOnly = Arrays.copyOf(printPatternsOnlyArray, printPatternsOnlyArray.length, Boolean[].class);
				
		this.experimentSetFileFolder = experimentSetFileFolder;
		this.translationIndexToCategoryFilePath = translationIndexToCategoryFilePath;	
		this.translationCategoryToLabelFilePath = translationCategoryToLabelFilePath;
		this.translationMode = translationMode;
		
		try {
			defineExperimentSet();
		} catch (Exception e) {
			System.out.println("Definition of ExperimentSet failed.");
			e.printStackTrace();
		}
	}
	
	//experiment methods
	public void defineExperimentSet() throws Exception {
		
		/** Define combinations of parameters -> 1 combination = 1 experiment **/ 
		this.experimentList = new LinkedList<BicPamsExperiment>();
		
		//create initial parameter structure
		LinkedHashMap<String, Object[]> paramStructure = createParameterStructure();

		//cartesian product (all unique combinations of several arrays)
		Object[][] cartesianParams = allUniqueCombinations(paramStructure);
		
		System.out.println("Number of experiments: " + (cartesianParams.length - 1));
		System.out.println("Creating experiment layouts...");
		
		//test - OK
		//ExperimentUtils.printObjectMatrix(cartesianParams);
		
		/** read csv dataset **/		
		//create BicPamsExperiments
		//(do not use the first array since it only contains the columns names)
		for (int i = 1; i < cartesianParams.length; i++) {
			
			/** Read Instances object for Dataset creation (once for every experiment since values may be removed) **/    
			Instances instances = BicReader.getInstances(dataset);
										
			/** Define individual experiments **/
			Object[] experimentParams = cartesianParams[i];
			
			//remove target class from dataset if experiment is discriminative (tends to appear on biclusters)
			int discriminativeIndexOnParams = 17; 
			int targetClassNameIndexOnParams = 18; 
			
			boolean discriminativeExperiment = (boolean) experimentParams[discriminativeIndexOnParams];
			
			if(discriminativeExperiment) {
				int targetClassIndex = -1;		
				String targetClassName = (String) experimentParams[targetClassNameIndexOnParams];
				
				//find target class attribute index
				ArrayList<Attribute> attributes = instances.getAttributes();
				for(Attribute att : attributes) {
					if(att.name().equals(targetClassName)) {
						targetClassIndex = att.index();
					}
				}
				
				//only delete attribute if it was really found
				if(targetClassIndex != -1) {
					Remove removeFilter = new Remove();
					int[] to_remove = {targetClassIndex};
					removeFilter.setAttributeIndicesArray(to_remove);
					removeFilter.setInputFormat(instances);
					instances = Filter.useFilter(instances, removeFilter);
				}
			}
			
			/** Create Dataset object */
			Dataset data = new Dataset(instances);
			
			String experimentId = "Exp_" + i;
			String outputFilename = experimentId + "_output.txt";
			
			int j = 0;	
			BicPamsExperiment experiment = new BicPamsExperiment(experimentId,									//experimentId
																 data,											//dataset
																 this.dataset,									//filePath
																 (StoppingCriteria) experimentParams[j++],		//stoppingCriteria 		(0)
																 (double) experimentParams[j++], 				//stoppingCriteriaValue (1)
																 (int) experimentParams[j++], 					//minNrColumns 			(2)
																 (int) experimentParams[j++],					//nrIterations 			(3)
																 (int) experimentParams[j++], 					//nrItems 				(4)
																 (boolean) experimentParams[j++],				//symmetric				(5)
																 (double) experimentParams[j++], 				//minOverlapMerging		(6)
																 (FilteringCriteria) experimentParams[j++], 	//filter				(7)
																 (FillingCriteria) experimentParams[j++], 		//missingsHandler		(8)
																 (RemoveCriteria) experimentParams[j++], 		//removeMissings		(9)
																 (double) experimentParams[j++], 				//minFilteringValue		(10)
																 (PatternType) experimentParams[j++],			//patternType			(11)
																 (ClosedImplementation) experimentParams[j++],	//closedImplementation	(12)
																 (Orientation) experimentParams[j++], 			//orientation			(13)
																 (double) experimentParams[j++], 				//significanceLevel		(14)
																 (boolean) experimentParams[j++],				//scalability			(15)
																 (double) experimentParams[j++],				//columnFilteringPerc	(16)
																 (boolean) experimentParams[j++],				//discriminative		(17)
																 (String) experimentParams[j++],				//targetClassName		(18)
																 (boolean) experimentParams[j++],				//printPatternsOnly		(19)
																 this.experimentSetFileFolder,					//experimentSetFileFolder
																 outputFilename);								//outputFilename
														
			this.experimentList.add(experiment);
		}
		
		System.out.println("Creating experiment layouts... DONE");
	}
	
	public void runExperimentSet() throws IOException {
				
		//create workbook
		XSSFWorkbook workbook = new XSSFWorkbook();
		//create sheets
		workbook.createSheet("Experiments");
		workbook.createSheet("Calculations");
		workbook.createSheet("PurestBiclusters");
	
		//where the start of the current experiment is (in rows of the respective XSLX sheet)
		int startingRowIndexExperiments = 0;
		int startingRowIndexCalculations = 0;	
		int startingRowIndexPurestBiclusters = 0;	
		
		//write header for calculations sheet
		startingRowIndexCalculations = MetricsExperiment.writePurityRowsAvgHeaderToExcelSheet(workbook, startingRowIndexCalculations);

		for (Iterator<BicPamsExperiment> iterator = this.experimentList.iterator(); iterator.hasNext();) {
			BicPamsExperiment experiment = (BicPamsExperiment) iterator.next();
			try {
				/**run experiment*/
				System.out.println("Running experiment with Id " + experiment.getExperimentId() + "...");
				Biclusters bics = experiment.run();
				System.out.println("Experiment with Id " + experiment.getExperimentId() + " completed.");
				
				/**check translation modes*/
				//translate Biclusters from indexes to category values
				if(this.translationMode == TranslationMode.ToCategories) {
					translator.translate(TranslationMode.ToCategories, bics, experiment, this.experimentSetFileFolder);
				}		
				//translate Biclusters from indexes to category values and then to labels
				else if(this.translationMode == TranslationMode.ToLabels) {
					translator.translate(TranslationMode.ToLabels, bics, experiment, this.experimentSetFileFolder);
				}
				//perform both translation modes
				else {
					translator.translate(TranslationMode.ToCategories, bics, experiment, this.experimentSetFileFolder);
					translator.translate(TranslationMode.ToLabels, bics, experiment, this.experimentSetFileFolder);
				}			
				/**evaluate, get csv data and metrics*/
				if(experiment.isDiscriminative()) {
					BiclustersEvaluator evaluator = new BiclustersEvaluator(this.csvDataset, bics, experiment.getTargetClassName());
					
					//write CSV for classifiers file
					System.out.println("Writing classifier CSV for experiment with Id " + experiment.getExperimentId() + "...");
					String csvClassifierData = evaluator.buildClassifierMatrixForCSVFile(experiment.getData());
					
					String csvCompleteFilepath = this.experimentSetFileFolder + "/" + experiment.getExperimentId() + "_CSV_classifier_" + this.currentDate + ".csv";
					Utils.writeFile(csvCompleteFilepath, csvClassifierData);
					
					//get experiment metrics
					System.out.println("Metrics for experiment with Id " + experiment.getExperimentId() + "...");
					
					MetricsExperiment metrics = evaluator.getAllClassMetricCalculations();			
					//System.out.println(metrics.toString());
					
					//update starting row index to be able to write all experiments in the same file
					System.out.println("Writing metrics XLSX file for experiment with Id " + experiment.getExperimentId() + "...");
					//startingRowIndex = metrics.writeAllDataToExcelSheet(sheet, startingRowIndex, experiment);
					startingRowIndexExperiments = metrics.writePurityPrecisionDataToExcelSheet(workbook, startingRowIndexExperiments, experiment, translator, this.translationMode);
					//write calculations on separate sheet
					startingRowIndexCalculations = metrics.writePurityRowsAvgToExcelSheet(workbook, startingRowIndexCalculations, experiment);
					
					//get purest bicluster ids
					Pair<String, Integer> purestBicsInfo = metrics.getPurestBiclusters(experiment);
					//if number of discriminative biclusters fits on the maximum number of columns...
					if(purestBicsInfo.getSecond().intValue() < Utils.MAX_COLS_XLSX - 1) {
						startingRowIndexPurestBiclusters = metrics.writePurestBiclustersToExcelSheet(workbook, startingRowIndexPurestBiclusters, experiment);
					}
					else {
						//failsafe for when there are more biclusters than available columns in the XLSX file
						//TO IMPROVE: this should only happen when the discriminative biclusters are too many for the excel sheet,
						//not the total number of biclusters found
						
						//write filename on metrics excel file
						String purestBicsFileName = experiment.getExperimentId() + "_Purest_Bics_" + this.currentDate + ".txt";
						startingRowIndexPurestBiclusters = metrics.writePurestBiclustersFileToExcelSheet(workbook, startingRowIndexPurestBiclusters, experiment, purestBicsFileName);
						//save file
						String purestBicsFilepath = this.experimentSetFileFolder + "/" + purestBicsFileName;					
						Utils.writeFile(purestBicsFilepath, purestBicsInfo.getFirst());
					}					
				}
				
			} catch (Exception e) {
				System.out.println("Experiment with Id " + experiment.getExperimentId() + " failed.");
				e.printStackTrace();
			} 
			
			//clear current experiment to free memory (garbage collection flag)
			iterator.remove();
		}
		
		//if something was written in the Experiments sheet, output it to a file
		if(startingRowIndexExperiments > 0) {
			
			//only call this method when there is nothing more to write on each sheet
			autoSizeColumns(workbook.getSheetAt(0));
			autoSizeColumns(workbook.getSheetAt(1));
			
			//output sheet contents to xlsx file
			String completeFilepath = this.experimentSetFileFolder + "/" + "ClassMetrics_" + this.currentDate + ".xlsx";						
			
			FileOutputStream fileOut = new FileOutputStream(completeFilepath);
			workbook.write(fileOut);
			fileOut.close();
		}
		
		//close workbook
		workbook.close();
	}
	
	private void autoSizeColumns(XSSFSheet sheet) {
		
		//autofit column width to contents 
		//(because different discriminative experiments can have a different number of columns)		
		int maxColumn = Utils.getMaximumNumberOfColumns(sheet);
		
		for(int j = 0; j < maxColumn; j++) {
			sheet.autoSizeColumn(j);
		}
	}
	
	//utility methods
	private LinkedHashMap<String, Object[]> createParameterStructure() {
		
		LinkedHashMap<String, Object[]> params = new LinkedHashMap<String, Object[]>();
		
		params.put(stoppingCriteriaStr, this.stoppingCriteria);
		params.put(stoppingCriteriaValueStr, this.stoppingCriteriaValue);
		params.put(minNrColumnsStr, this.minNrColumns);
		params.put(nrIterationsStr, this.nrIterations);
		params.put(nrItemsStr, this.nrItems);
		params.put(symmetricStr, this.symmetric);
		params.put(minOverlapMergingStr, this.minOverlapMerging);
		params.put(filterStr, this.filter);
		params.put(missingsHandlerStr, this.missingsHandler);
		params.put(removeMissingsStr, this.removeMissings);
		params.put(minFilteringValueStr, this.minFilteringValue);
		params.put(patternTypeStr, this.patternType);
		params.put(closedImplementationStr, this.closedImplementation);
		params.put(orientationStr, this.orientation);
		params.put(significanceLevelStr, this.significanceLevel);
		params.put(scalabilityStr, this.scalability);
		params.put(columnFilteringPercStr, this.columnFilteringPerc);
		params.put(discriminativeStr, this.discriminative);
		params.put(targetClassNameStr, this.targetClassName);
		params.put(printPatternsOnlyStr, this.printPatternsOnly);
		
		return params;
	}
	
	private Object[][] allUniqueCombinations(LinkedHashMap<String, Object[]> paramStructure){

		List<String> labels = new ArrayList<String>();
	    List<Object[]> lists = new ArrayList<Object[]>();

	    for (Map.Entry<String, Object[]> entry : paramStructure.entrySet()) {
	        labels.add(entry.getKey());
	        lists.add(entry.getValue());
	    }

	    List<List<Object>> combinations = product(lists);
	    int m = combinations.size() + 1;
	    int n = labels.size();
	    Object[][] answer = new Object[m][n];

	    for (int i = 0; i < n; i++)
	        answer[0][i] = labels.get(i);
	    for (int i = 1; i < m; i++)
	        for (int j = 0; j < n; j++)
	            answer[i][j] = combinations.get(i-1).get(j);

	    return answer;
    }
	
	private List<List<Object>> product(List<Object[]> lists) {

		List<List<Object>> result = new ArrayList<List<Object>>();
	    result.add(new ArrayList<Object>());

	    for (Object[] e : lists) {
	        List<List<Object>> tmp1 = new ArrayList<List<Object>>();
	        for (List<Object> x : result) {
	            for (Object y : e) {
	                List<Object> tmp2 = new ArrayList<Object>(x);
	                tmp2.add(y);
	                tmp1.add(tmp2);
	            }
	        }
	        result = tmp1;
	    }

	    return result;
	}
	
	private Object[] getDefaultParameterArray(String parameterType) {
		
		Object[] paramArray = new Object[1];
		
		switch (parameterType) {
			case stoppingCriteriaStr:
				paramArray[0] = StoppingCriteria.MinBicsBeforeMerging;
				break;
			case stoppingCriteriaValueStr:
				paramArray[0] = new Double(50);
				break;
			case minNrColumnsStr:
				paramArray[0] = new Integer(4);
				break;
			case nrIterationsStr:
				paramArray[0] = new Integer(2);
				break;
			case nrItemsStr:
				paramArray[0] = new Integer(6);
				break;
			case symmetricStr:
				paramArray[0] = new Boolean(false);
				break;
			case minOverlapMergingStr:
				paramArray[0] = new Double(0.8);
				break;
			case filterStr:
				paramArray[0] = FilteringCriteria.Overall;
				break;
			case missingsHandlerStr:
				paramArray[0] = FillingCriteria.RemoveValue;
				break;
			case removeMissingsStr:
				paramArray[0] = RemoveCriteria.RemoveZeroEntries;
				break;
			case minFilteringValueStr:
				paramArray[0] = new Double(50);
				break;
			case patternTypeStr:
				paramArray[0] = PatternType.Constant;
				break;
			case closedImplementationStr:
				paramArray[0] = ClosedImplementation.DCharm;
				break;
			case orientationStr:
				paramArray[0] = Orientation.PatternOnRows;
				break;
			case significanceLevelStr:
				paramArray[0] = new Double(0.05);
				break;
			case scalabilityStr:
				paramArray[0] = new Boolean(false);
				break;
			case columnFilteringPercStr:
				paramArray[0] = new Double(0);
				break;
			case discriminativeStr:
				paramArray[0] = new Boolean(false);
				break;
			case targetClassNameStr:
				paramArray[0] = null;
				break;
			case printPatternsOnlyStr:
				paramArray[0] = false;
				break;
			default:
				//do nothing
				break;
		}	
		
		return paramArray;
	}
}
