package bic_metrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.util.Pair;

import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.Utils;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 1.0
 *  
 *  Entropy, purity, precision, recall and f-measure calculations adapted from 
 *  the book "Introduction to Data Mining", 2nd Ed, Chapter 7, from Pang-Ning Tan,
 *  Michael Steinbach, Anuj Karpatne and Vipin Kumar, 2019, Pearson.
 */

public class BiclustersEvaluator {

	//path to the input csv dataset file
	private String csvDatasetFilePath;
	//target class name
	private String targetClassName;
	//map to get lists of values per column
	private Map<String, List<String>> datasetColumnValuesMap;
	//pointer to the set of Biclusters
	private Biclusters biclusters;
	//pij = probability that a member of bicluster i (row) belongs to class j (column)
	private Double[][] probabilityMatrix;
	//probability matrix sizes
	private int numberBiclusters; 				//rows
	private int numberUniqueCategoriesTarget; 	//columns
	//probability matrix target class values for each column (j)
	private Set<String> targetClassValues;

	/**CONSTRUCTOR*/
	public BiclustersEvaluator(String csvDatasetFilePath, Biclusters bics, String targetClassName) throws Exception {

		this.csvDatasetFilePath = csvDatasetFilePath;
		this.targetClassName = targetClassName;
		//fill dataset map 
		this.datasetColumnValuesMap = createDatasetColumnValuesMap(this.csvDatasetFilePath);
		//set calculations in motion
		setBiclusters(bics, this.targetClassName);
	}
	
	/**GETTERS*/
	public Map<String, List<String>> getDatasetColumnValuesMap() {
		return datasetColumnValuesMap;
	}
	
	public Double[][] getProbabilityMatrix() {
		return probabilityMatrix;
	}
	
	public Set<String> getTargetClassValues() {
		return targetClassValues;
	}

	/**DATA INITIALIZATION*/
	private Map<String, List<String>> createDatasetColumnValuesMap(String csvDatasetFilePath) throws Exception {

		FileReader in = new FileReader(csvDatasetFilePath);
		BufferedReader br = new BufferedReader(in);

		Map<String, List<String>> map = new TreeMap<String, List<String>>();

		//read column line (first line)
		String columnLine = br.readLine();
		String[] columnNames = columnLine.split(",");

		//read data lines
		String line;
		while ((line = br.readLine()) != null) {			 		
			//split each line by commas
			String[] lineParts = line.split(",");
			//go through all columns by each row
			for(int i = 0; i < columnNames.length; i++) {
				String currentColumn = columnNames[i];

				//value list pointer for column values
				List<String> valueList;
				//if current column still does not have a value list, create one
				if(!map.containsKey(currentColumn)) {
					valueList = new ArrayList<String>();
				}
				//if value list exists, get it
				else {
					valueList = map.get(currentColumn);
				}			 
				//add column value to the list
				//account for missing values in csv lines as a class "?"
				String value = lineParts[i].equals("") ? "?" : lineParts[i];				
				valueList.add(value);
				//save the list on the map
				map.put(currentColumn, valueList);
			}			 
		}
		in.close();

		return map;
	}

	/**PROBABILITY CALCULATIONS*/	
	//calculate probabilities for a set of Biclusters for a given target class;
	//left targetClassName as parameter so this allows the change of target class for 
	//the same Biclustering solution
	public void setBiclusters(Biclusters biclusters, String targetClassName) {
		
		this.biclusters = biclusters;
		calculateClassProbabilities(targetClassName);
	}
	
	//calculate probabilities of each class in each bicluster
	private void calculateClassProbabilities(String targetClassName) {
		
		//DEBUG
		boolean debug = false;
		StringBuffer categories = new StringBuffer();
		
		//get list of values for target class column
		List<String> valueList = this.datasetColumnValuesMap.get(targetClassName);		
		//get unique values for target class 
		Set<String> uniqueValues = new TreeSet<String>(valueList);		
		//save them for later
		this.targetClassValues = uniqueValues;
				
		//init double matrix
		int numberBiclusters = this.biclusters.size(); 			//number of biclusters
		int numberUniqueCategoriesTarget = uniqueValues.size(); //number of distinct classes to consider
		
		this.numberBiclusters = numberBiclusters; 							//rows
		this.numberUniqueCategoriesTarget = numberUniqueCategoriesTarget; 	//columns
		
		Double[][] probMatrix = new Double[numberBiclusters][numberUniqueCategoriesTarget];
		
		//mij = number of objects of class j in bicluster i
		//mi  = number of objects in bicluster i
		//pij = probability that a member of bicluster i (row) belongs to class j (column)
		//pij = mij / mi 
		
		for (int i = 0; i < numberBiclusters; i++) {
			
			Bicluster currentBicluster = this.biclusters.get(i); 			
			Map<String, Integer> currentCategoryClassCount = getCategoryClassCount(currentBicluster, targetClassName);
			
			int j = 0;
			Iterator<String> it = uniqueValues.iterator();
			
			while(it.hasNext()) {			
				String currentCategory = it.next();
				
				//DEBUG
				if(debug) {
					if(i == 0) {
						categories.append(currentCategory + "\t");
					}
				}	
			
				double probability_ij;
				//if category exists in bicluster, calculate its probability
				if(currentCategoryClassCount.containsKey(currentCategory)) {									
					//mij
					double countCurrentCategoryOnCurrentBicluster = currentCategoryClassCount.get(currentCategory);
					//mi
					double bicSize = currentBicluster.numRows();
					//pij = mij / mi 
					probability_ij = countCurrentCategoryOnCurrentBicluster / bicSize;
				} 
				//if not, the probability for the class j in bicluster i is 0
				else {
					probability_ij = 0;
				}
				
				//save calculated probability
				probMatrix[i][j] = probability_ij;	
				//update counter
				j++;
			}
		}
		
		//DEBUG
		if(debug) {
			Utils.printProbabilityMatrix(categories.toString(), probMatrix);
		}
		
		this.probabilityMatrix = probMatrix;
	}

	//category class count method for a single Bicluster
	public Map<String, Integer> getCategoryClassCount(Bicluster bic, String targetClassName) {

		//create map which will contain the number of counts of each category in the target class
		Map<String, Integer> categoryClassCounter = new HashMap<String, Integer>();
	
		//get list of values for target class column
		List<String> valueList = this.datasetColumnValuesMap.get(targetClassName);

		Integer counter;
		for (Integer row : bic.rows) {		
			//find category for the current row
			String targetValue = valueList.get(row);
			//if current category still does not have a value, create one
			if(!categoryClassCounter.containsKey(targetValue)) {
				counter = 1;
			}
			//if value exists, get it and update it
			else {
				counter = categoryClassCounter.get(targetValue);
				counter++;
			}			 		
			//save the value back on the map
			categoryClassCounter.put(targetValue, counter.intValue());
		}

		return categoryClassCounter;
	}
	
	//get column of target class values for a given Bicluster
	public List<String> getTargetClassValues(Bicluster bicluster, String targetClassName) {
		
		List<String> targetClassValues = new ArrayList<String>(bicluster.numRows());
		
		//get list of values for target class column
		List<String> valueList = this.datasetColumnValuesMap.get(targetClassName);

		for (Integer row : bicluster.rows) {		
			//find category for the current row
			targetClassValues.add(valueList.get(row));
		}
		
		return targetClassValues;
	}
	
	/**METRIC CALCULATIONS*/
	public MetricsExperiment getAllClassMetricCalculations() {
		
		//Calculate entropy and purity for the biclustering solution
		Double entropyBiclustering = getBiclusteringEntropy(this.targetClassName);
		Double purityBiclustering = getBiclusteringPurity();
		
		//Calculate entropy and purity for each individual bicluster (and gather p-values)
		List<Double> pValueBiclusters = new ArrayList<Double>();
		List<Double> entropyBiclusters = new ArrayList<Double>();
		List<Double> purityBiclusters = new ArrayList<Double>();
				
		for (int i = 0; i < this.biclusters.size(); i++) {
				
			pValueBiclusters.add(this.biclusters.get(i).pvalue);
			entropyBiclusters.add(getBiclusterEntropy(i, this.targetClassName));
			purityBiclusters.add(getBiclusterPurity(i));
		}
		
		//Calculate precision, recall and f-measure for each class in each individual bicluster
		Map<Integer, List<Pair<String, Double>>> precisionClassBiclusters = new TreeMap<Integer, List<Pair<String, Double>>>();
		Map<Integer, List<Pair<String, Double>>> recallClassBiclusters = new TreeMap<Integer, List<Pair<String, Double>>>();
		Map<Integer, List<Pair<String, Double>>> fMeasureClassBiclusters = new TreeMap<Integer, List<Pair<String, Double>>>();
		
		for (int i = 0; i < this.biclusters.size(); i++) {
			
			List<Pair<String, Double>> listClassValuesPrecision = new ArrayList<Pair<String, Double>>(this.numberUniqueCategoriesTarget);
			List<Pair<String, Double>> listClassValuesRecall = new ArrayList<Pair<String, Double>>(this.numberUniqueCategoriesTarget);
			List<Pair<String, Double>> listClassValuesFMeasure = new ArrayList<Pair<String, Double>>(this.numberUniqueCategoriesTarget);
			
			for(int j = 0; j < this.numberUniqueCategoriesTarget; j++) {	
				
				String classValue = getTargetClassValue(j);
				
				//precision
				Double precision = getBiclusterPrecision(i, j);				
				listClassValuesPrecision.add(new Pair<String, Double>(classValue, precision));
				
				//recall
				Double recall = getBiclusterRecall(i, j);
				listClassValuesRecall.add(new Pair<String, Double>(classValue, recall));
				
				//f-measure
				Double fMeasure = getBiclusterFMeasure(i, j);
				listClassValuesFMeasure.add(new Pair<String, Double>(classValue, fMeasure));
			}
			
			precisionClassBiclusters.put(i, listClassValuesPrecision);
			recallClassBiclusters.put(i, listClassValuesRecall);
			fMeasureClassBiclusters.put(i, listClassValuesFMeasure);
		}
		
		//create object with all metrics
		MetricsExperiment metrics = new MetricsExperiment(this.targetClassValues, 
														  entropyBiclustering, 
														  purityBiclustering, 
														  pValueBiclusters,
														  entropyBiclusters, 
														  purityBiclusters, 
														  precisionClassBiclusters, 
														  recallClassBiclusters, 
														  fMeasureClassBiclusters);
		
		return metrics;
	}

	/**Entropy calculation*/
	//the entropy of bicluster i is the degree to which the bicluster consists of objects of a single class:
	//ei = - sum(from j = 1 to L (number of classes) pij * log2(pij)) 
	//Note: if all objects in a bicluster are of the same class -> 0 entropy
	public double getBiclusterEntropy(int biclusterIndex, String targetClassName) {
		
		double entropy = 0;
		
		for(int j = 0; j < this.numberUniqueCategoriesTarget; j++) {	
			
			double prob = this.probabilityMatrix[biclusterIndex][j];
			
			//failsafe for the case when pij = 0:
			//pij * log2(pij) parcel should be considered as 0
			if(prob > 0) {
				entropy -= prob * Utils.log2(prob);
			}
		}
		
		return entropy;
	}
	
	//the entropy of a biclustering is the the sum of the entropies of each bicluster weighted by their size:
	//e = - sum(from i = 1 to K (number of biclusters) (mi/m)*ei)
	public double getBiclusteringEntropy(String targetClassName) {
				
		//m = total number of data points		
		int totalNumberDataPoints = 0;	
		for (int i = 0; i < this.numberBiclusters; i++) {			
			totalNumberDataPoints += this.biclusters.get(i).numRows();
		}
		
		//calculate biclustering entropy
		double entropy = 0;
		
		for (int i = 0; i < this.numberBiclusters; i++) {
			//mi = number of objects in bicluster i
			double bicSize = this.biclusters.get(i).numRows();			
			entropy += (bicSize / totalNumberDataPoints) * getBiclusterEntropy(i, targetClassName);
		}
		
		return entropy;
	}
	
	/**Purity calculation*/
	//the purity of bicluster i is another measure of the extent to which a bicluster contains 
	//objects of the same class: purity(i) = max of pij for all values of j
	//Note: if all objects in a bicluster are of the same class -> 1 purity
	public double getBiclusterPurity(int biclusterIndex) {
		
		double purity = 0;
		
		for(int j = 0; j < this.numberUniqueCategoriesTarget; j++) {	
			
			double prob = this.probabilityMatrix[biclusterIndex][j];
			
			//get maximum probability for all j's
			if(prob > purity) {
				purity = prob;
			}
		}
		
		return purity;
	}
	
	//the purity of a biclustering is the the sum of the purities of each bicluster weighted by their size:
	//p = sum(from i = 1 to K (number of biclusters) (mi/m)*purity(i))
	public double getBiclusteringPurity() {
				
		//m = total number of data points		
		int totalNumberDataPoints = 0;	
		for (int i = 0; i < this.numberBiclusters; i++) {			
			totalNumberDataPoints += this.biclusters.get(i).numRows();
		}
		
		//calculate biclustering purity
		double purity = 0;
		
		for (int i = 0; i < this.numberBiclusters; i++) {
			//mi = number of objects in bicluster i
			double bicSize = this.biclusters.get(i).numRows();			
			purity += (bicSize / totalNumberDataPoints) * getBiclusterPurity(i);		
		}
		
		return purity;
	}
	
	/**Precision calculation*/
	//precision is the fraction of a bicluster that consists of objects of a specified class:
	//precision of bicluster i with respect to class j is precision(i,j) = pij
	public double getBiclusterPrecision(int biclusterIndex, int targetClassIndex) {
		
		return this.probabilityMatrix[biclusterIndex][targetClassIndex];
	}
	
	//alternative method that uses target class value instead of index
	public double getBiclusterPrecision(int biclusterIndex, String targetClassValue) {
		
		int targetClassIndex = getTargetClassIndex(targetClassValue);
				
		//failsafe if target class value is not found 
		if(targetClassIndex == -1) {
			//System.out.println("WARNING: Returning precision 0 for Bicluster with index " + biclusterIndex + 
			//				     " since target class value (" + targetClassValue + ") was not found." );
			return 0;
		}
		
		return getBiclusterPrecision(biclusterIndex, targetClassIndex);
	}
	
	/**Recall calculation*/
	//recall is the extent to each a Bicluster contains all objects of a specific class;
	//recall of bicluster i with respect to class j is recall(i,j) = mij/mj, where mj is
	//the number of objects in class j
	public double getBiclusterRecall(int biclusterIndex, int targetClassIndex) {
		
		Bicluster currentBicluster = this.biclusters.get(biclusterIndex);
		String targetClassValue = getTargetClassValue(targetClassIndex);
		
		Map<String, Integer> categoryClassCount = getCategoryClassCount(currentBicluster, this.targetClassName);
		
		double recall = 0;
		
		//if category exists in bicluster, calculate the recall
		if(categoryClassCount.containsKey(targetClassValue)) {									
			//mij
			double countCurrentCategoryOnCurrentBicluster = categoryClassCount.get(targetClassValue);
			//mj
			double categoryCount = getDatasetTargetClassCount(targetClassValue);
			//recall(i,j) = mij/mj
			recall = countCurrentCategoryOnCurrentBicluster / categoryCount;
		} 
		//if not, just display message to return 0
//		else {
//			System.out.println("WARNING: Returning recall 0 for Bicluster with index " + biclusterIndex + 
//					   " since target class index (" + targetClassIndex + ") was not found." );
//		}
		
		return recall;
	}
	
	//alternative method that uses target class value instead of index
	public double getBiclusterRecall(int biclusterIndex, String targetClassValue) {
		
		int targetClassIndex = getTargetClassIndex(targetClassValue);
				
		//failsafe if target class value is not found 
		if(targetClassIndex == -1) {
			//System.out.println("WARNING: Returning recall 0 for Bicluster with index " + biclusterIndex + 
			//				     " since target class value (" + targetClassValue + ") was not found." );
			return 0;
		}
		
		return getBiclusterRecall(biclusterIndex, targetClassIndex);
	}
	
	/**F-measure*/
	//F-Measure is a combination of precision and recall that measures the extent to which a bicluster
	//contains only objects of a particular class and all objects of that class.
	//F(i,j) = (2 * precision(i,j) * recall(i,j)) / (precision(i,j) + recall(i,j))
	public double getBiclusterFMeasure(int biclusterIndex, int targetClassIndex) {
		
		double precision = getBiclusterPrecision(biclusterIndex, targetClassIndex);
		double recall = getBiclusterRecall(biclusterIndex, targetClassIndex);
		
		if(precision > 0 || recall > 0) {
			return (2 * precision * recall) / (precision + recall);
		}
		else
			return 0;
	}
	
	//alternative method that uses target class value instead of index
	public double getBiclusterFMeasure(int biclusterIndex, String targetClassValue) {
		
		int targetClassIndex = getTargetClassIndex(targetClassValue);
		
		//failsafe if target class value is not found 
		if(targetClassIndex == -1) {
			//System.out.println("WARNING: Returning F-measure 0 for Bicluster with index " + biclusterIndex + 
			// 				     " since target class value (" + targetClassValue + ") was not found." );
			return 0;
		}
		
		return getBiclusterFMeasure(biclusterIndex, targetClassIndex);
	}
	
	/**UTILITY METHODS*/
	private int getTargetClassIndex(String targetClassValue) {
		
		int targetClassIndex = -1;
		
		//find the index of the given target class value on the unique target class values set
		Iterator<String> it = this.targetClassValues.iterator();
		
		int j = 0;
		while(it.hasNext()) {			
			String tempClassValue = it.next();
			if(tempClassValue.equals(targetClassValue)) {
				targetClassIndex = j;
			}
			j++;
		}
		
		return targetClassIndex;
	}
	
	private String getTargetClassValue(int targetClassIndex) throws IndexOutOfBoundsException {
		
		if(targetClassIndex >= this.targetClassValues.size()) {
			throw new IndexOutOfBoundsException("BiclustersEvaluator.getTargetClassValue");
		}
		
		String targetClassValue = "";
		
		//find the name of the given target class index on the unique target class values set
		Iterator<String> it = this.targetClassValues.iterator();
		
		int j = 0;
		while(it.hasNext()) {			
			String tempClassValue = it.next();
			if(j == targetClassIndex) {
				targetClassValue = tempClassValue;
			}
			j++;
		}
		
		return targetClassValue;
	}
	
	private int getDatasetTargetClassCount(String targetClassValue) {
		
		List<String> datasetTargetClassValues = this.datasetColumnValuesMap.get(this.targetClassName);
		
		int counter = 0;
		
		for (String value : datasetTargetClassValues) {
			if(value.equals(targetClassValue)) {
				counter++;
			}
		}
		
		return counter;
	}
	
	//method to build a classifier matrix (in CSV format) to feed to a Weka classifier
	//rows: subject ids; columns: bicluster ids (index + 1); 
	//elements: 1 if the subject id appears in a given bicluster, 0 otherwise;
	//last column is target class
	public String buildClassifierMatrixForCSVFile(Dataset data) {
		
		String subjectIDStr = "Subject ID";
		
		//get data in a convenient format
		List<String> subjectIds = datasetColumnValuesMap.get(subjectIDStr);
		List<String> targetClassValues = datasetColumnValuesMap.get(this.targetClassName);

		/**Map of maps*/
		//first level key (String) = Bicluster Id (e.g. Bic_01)
		//second level key (String) = Subject id
		Map<String, Map<String, Integer>> mapSubjectsInBics = new TreeMap<String, Map<String, Integer>>();
		
		for(int i = 0; i < numberBiclusters; i++) {
			
			Bicluster currentBic = this.biclusters.get(i);	
			
			//initialize inner map for the current bicluster
			Map<String, Integer> tempMap = new TreeMap<String, Integer>();
			
			//get subject Ids present on the current bicluster			
			List<String> subjectIdsCurrentBic = new LinkedList<String>();
			for(int rowIdx : currentBic.rows) { 
				subjectIdsCurrentBic.add(data.rows.get(rowIdx));
			}
			
			//1 if the subject id appears in the current bicluster, 0 otherwise;
			for(String subjectId : subjectIds) {
				if(subjectIdsCurrentBic.contains(subjectId)) {
					tempMap.put(subjectId, 1);
				}
				else {
					tempMap.put(subjectId, 0);
				}
			} 
			
			//put a zero behind the bicluster number if i < 10
			mapSubjectsInBics.put("Bic_" + (i + 1 < 10 ? "0" : "") + (i + 1), tempMap);
		}
				
		/**build string that will compose the csv file*/
		StringBuilder builder = new StringBuilder();	
		
		String separator = ",";
		String newline = "\n";
		
		/**CSV header*/
		//subject ID column
		builder.append(subjectIDStr + separator);
		
		//bicluster Ids columns
		Set<Entry<String, Map<String, Integer>>> entrySet = mapSubjectsInBics.entrySet();		
		Iterator<Entry<String, Map<String, Integer>>> it = entrySet.iterator();
		
		while(it.hasNext()) {
			builder.append(it.next().getKey() + separator);
		}
		
		//target class name
		builder.append(this.targetClassName + newline);
		
		/**CSV data rows*/		
		for(int i = 0; i < subjectIds.size(); i++) {
			
			String currentSubjectId = subjectIds.get(i);
			
			//subject id for the given row
			builder.append(currentSubjectId + separator);
			
			//get new iterator over entrySet
			it = entrySet.iterator();
			
			while(it.hasNext()) {
				//get value of the subject id presence in the bicluster
				builder.append(it.next().getValue().get(currentSubjectId) + separator);
			}
			
			//target class value for the current subject id
			builder.append(targetClassValues.get(i) + newline);
		}
			
		return builder.toString();
	}
		
	/**DEBUG METHODS*/
	public String getTargetClassStats(Bicluster bic, String targetClassName) {

		//necessary objects for calculations
		NumberFormat formatter = new DecimalFormat("#0.00");  
		int numberRows = bic.numRows();
		
		//get category class counter
		Map<String, Integer> categoryClassCounter = getCategoryClassCount(bic, targetClassName);

		//iterate through all classes to get stats and return
		Iterator<Map.Entry<String, Integer>> it = categoryClassCounter.entrySet().iterator(); 	
			   
		StringBuffer buff = new StringBuffer();	
		buff.append("** Stats for target class (" + targetClassName + "):");
		
		buff.append("\nTotal number of rows: " + numberRows + "\n");
		
		while(it.hasNext()) 
		{ 
			Map.Entry<String, Integer> entry = it.next(); 
			double relFreq = (entry.getValue().doubleValue() / numberRows);		
			buff.append("\t - Category " + entry.getKey() + " | " + entry.getValue() + " (count) | " + formatter.format(relFreq) + " (rel. freq.))");
			buff.append("\n");
		} 

		return buff.toString();
	}
	
	//method to print bicluster with class (class is translated, bic content is not - DEBUG purposes);
	//to get the whole bicluster and class translated, use toStringTranslatedWithClass from BiclustersTranslator class
	public String toStringWithClass(Bicluster bic, Dataset data, String targetClassName) {
		
		//build string part with the columns and rows that compose the bicluster
		StringBuffer res = new StringBuffer(bic.key != null ? "ID:" + bic.key : "");
		res.append(" (" + bic.columns.size() + "," + bic.rows.size() + ") Y=[");
	
		for(int i : bic.columns) {
			res.append(data.columns.get(i)+",");
		}
		
		//add target class to column list
		res.append("Target Class (" + targetClassName + ")");
		res.append("] X=[");
		
		for(int i : bic.rows) {
			res.append(data.rows.get(i)+",");
		}
		
		res.append("]");

		//get target class values for each Bicluster row
		List<String> targetClassValues = getTargetClassValues(bic, targetClassName);
		
		int[][] matrix = data.getBicluster(bic.columns, bic.rows);
		int k = 0;
		for (Iterator<Integer> it = bic.rows.iterator(); it.hasNext();)
		{
			int x = ((Integer)it.next()).intValue();
			res.append("\n");
			res.append((String)data.rows.get(x) + "\t");
			int y = 0;
			for (int l = matrix[k].length; y < l; y++) {						
				res.append(matrix[k][y]+"\t");								
			}
			//append class value for the respective row
			res.append(targetClassValues.get(k));			
			k++;
		}
		return res.toString().replace(",]", "]");
	}
}
