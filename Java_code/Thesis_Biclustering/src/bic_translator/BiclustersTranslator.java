package bic_translator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import bic_experiment.BicPamsExperiment;
import bic_metrics.BiclustersEvaluator;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import utils.Utils;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 1.0
 *  
 *  Generic class that allows for two exclusive Bicluster translation modes:
 *  1) from category indexes (from ARFF file) to category values;
 *  2) from category values to category labels (for interpretability reasons)
 *  
 *  This was done in such fashion because the first type of translation has to be 
 *  done always when running a BicPamsExperimentSet (for each Bicluster 
 *  in each separate experiment), but the second type may be necessary or not.
 *  Also see getTranslationToLabels flag in BicPamsExperimentSet class for
 *  more information.
 */

public class BiclustersTranslator {
	
	private String indexToCategoriesFilePath;
	private String categoriesToLabelsFilePath;
	private Map<String, List<Pair<String, String>>> indexToCategoriesMap;
	private Map<String, List<Pair<String, String>>> categoriesToLabelsMap;
	private boolean printPatternsOnly;
	
	public static enum TranslationMode
	{
		ToCategories, ToLabels, Both;
		
		private TranslationMode() {}
	}
	
	//constructor
	public BiclustersTranslator(String indexToCategoriesFilePath, String categoriesToLabelsFilePath) throws Exception {
		
		this.indexToCategoriesFilePath = indexToCategoriesFilePath;
		this.categoriesToLabelsFilePath = categoriesToLabelsFilePath;
		
		//fill BicPAMS translation maps
		this.indexToCategoriesMap = createIndexToCategoriesMap(this.indexToCategoriesFilePath);	
		this.categoriesToLabelsMap = createCategoriesToLabelsMap(this.categoriesToLabelsFilePath);	
		
		//default value
		this.printPatternsOnly = false;
	}
	
	public boolean getPrintPatternsOnly() {
		return printPatternsOnly;
	}
	
	public void setPrintPatternsOnly(boolean printPatternsOnly) {
		this.printPatternsOnly = printPatternsOnly;
	}

	//index to category value
	private Map<String, List<Pair<String, String>>> createIndexToCategoriesMap(String translationFilePath) throws Exception {
		
		 FileReader in = new FileReader(translationFilePath);
		 BufferedReader br = new BufferedReader(in);
		 
		 Map<String, List<Pair<String, String>>> map = new TreeMap<String, List<Pair<String, String>>>();
		 
		 //process the translation indexes and respective categories for each dataset column 
		 //(a line of the translation file)
		 String line;
		 while ((line = br.readLine()) != null) {			 		
			 //split each line by tabs
			 String[] lineParts = line.split("\t");
			 //first part is the columns name
			 String columnName = lineParts[0].replaceAll("\"", "");
			 //create list
			 List<Pair<String, String>> listTranslations = new LinkedList<Pair<String, String>>();
			 //other existing parts are index to category convertions
			 for(int i = 1; i < lineParts.length; i++) {
				 String current = lineParts[i].replaceAll("\"", "");
				 String[] elementParts = current.split(" -> ");
				 Pair<String, String> pair = new Pair<String, String>(elementParts[0], elementParts[1]);
				 listTranslations.add(pair);
			 }
			 //save column translation data on map
			 map.put(columnName, listTranslations);
		 }
		 in.close();
		 
		 return map;
	}
		
	//category value to label
	private Map<String, List<Pair<String, String>>> createCategoriesToLabelsMap(String translationFilePath) throws IOException {
		
		Map<String, List<Pair<String, String>>> map = new TreeMap<String, List<Pair<String, String>>>();
		
		//read XLSX file
		File excelFile = new File(translationFilePath);
	    FileInputStream fis = new FileInputStream(excelFile);

	    //create an XSSF Workbook object for XLSX File
	    XSSFWorkbook workbook = new XSSFWorkbook(fis);
	    //get first and only sheet
	    XSSFSheet sheet = workbook.getSheetAt(0);
	    
	    //number of columns
	    int numberColumns = sheet.getRow(0).getPhysicalNumberOfCells();
	    
	    //iterate through pairs of columns to get the labels that correspond to each category value
	    for (int i = 0; i < numberColumns; i += 2) {
	    	
	    	//create list
			List<Pair<String, String>> listTranslations = new LinkedList<Pair<String, String>>();
			
	    	//get feature name for map
	    	String featureName = sheet.getRow(0).getCell(i).getStringCellValue();
	    	//System.out.println(featureName);
	    	
	    	//iterate through both columns to get labels (index i) and category values (index i + 1)
	    	int j = 1;
	    	while(sheet.getRow(j) != null && sheet.getRow(j).getCell(i) != null) {
	    		
	    		String label = sheet.getRow(j).getCell(i).getStringCellValue();
	    		String categoryValue = sheet.getRow(j).getCell(i + 1).getStringCellValue();
	    		
	    		Pair<String, String> pair = new Pair<String, String>(categoryValue, label);
	    		listTranslations.add(pair);
	    		j++;
	    	}
	    	
	    	//add NA case to each feature
	    	Pair<String, String> notApplicable = new Pair<String, String>("NA", "Not Applicable");
    		listTranslations.add(notApplicable);
	    	
	    	//save column translation data on map
			map.put(featureName, listTranslations);
		}
	    
	    //close workbook when done
	    workbook.close();
		
	    //System.out.println(map);
	    
		return map;
	}
	
	//common methods
	/**
	 * method translate
	 * @param mode -> here it should only be TranslationMode.ToCategories or TranslationMode.ToLabels
	 * @param bics
	 * @param experiment
	 * @param outputFolder
	 * @throws Exception
	 * 
	 */
	public void translate(TranslationMode mode, Biclusters bics, BicPamsExperiment experiment, String outputFolder) throws Exception {
		
		//create new file with the translated biclusters
		StringBuffer header = new StringBuffer();	
		header.append("EXPERIENCE PARAMETERS:\n" + experiment.toString() + "\n");			
		header.append("DATASET:\n" + experiment.getData().getStatistics() + "\n");
		
		header.append("\nINDIVIDUAL BICLUSTERS:\n");
				
		//output to a concrete location
		if(mode != TranslationMode.Both) {
			
			StringBuffer completeFilepath = new StringBuffer();	
			completeFilepath.append(outputFolder + "/" + experiment.getExperimentId());
			
			if(mode == TranslationMode.ToCategories) {
				completeFilepath.append("_translated_categories.txt");
			}
			else {
				completeFilepath.append("_translated_labels.txt");
			}
			
			header.append(translateBiclusters(mode, bics, experiment));
			Utils.writeFile(completeFilepath.toString(), header.toString());
		} 
		//both types of translation
		else {
			//toCategories
			String completeFilepath = outputFolder + "/" + experiment.getExperimentId() + "_translated_categories.txt";						
			String allText = header.toString() + translateBiclusters(TranslationMode.ToCategories, bics, experiment);
			Utils.writeFile(completeFilepath.toString(), allText);
			
			//toLabels
			completeFilepath = outputFolder + "/" + experiment.getExperimentId() + "_translated_labels.txt";						
			allText = header.toString() + translateBiclusters(TranslationMode.ToLabels, bics, experiment);
			Utils.writeFile(completeFilepath.toString(), allText);
		}
	}
	
	private String translateBiclusters(TranslationMode mode, Biclusters bics, BicPamsExperiment experiment) {
		
		StringBuffer toPrint = new StringBuffer();	

		int i = 1;
		for(Bicluster bic : bics.getBiclusters()) {
			toPrint.append("\nBICLUSTER #" + i++ + ":\n");
			toPrint.append("p-value = " + bic.pvalue + "\n");	
			toPrint.append("area = " + bic.area() + "\n\n");	
			
			if(experiment.getPrintPatternsOnly()) {
				//print columns and row pattern
				Dataset data = experiment.getData();
				
				if(experiment.getPatternType() == PatternType.Constant) {	
					toPrint.append("pattern: \n");
					
					List<String> bicColumnNames = new LinkedList<String>();

					//get column names
					for(int idx : bic.columns) {
						if(experiment.getScalability()) {
							bicColumnNames.add(data.originalColumns.get(idx));
							toPrint.append(data.originalColumns.get(idx) + "\t");
						}
						else {
							bicColumnNames.add(data.columns.get(idx));
							toPrint.append(data.columns.get(idx) + "\t");
						}
					} 
					toPrint.append("\n");	
					
					int[] matrixFirstLine = data.getBicluster(bic.columns,bic.rows)[0]; 
					for(int j = 0; j < matrixFirstLine.length; j++) {
						toPrint.append(getTranslatedValue(mode, bicColumnNames.get(j), String.valueOf(matrixFirstLine[j])) + "\t");
					}
					toPrint.append("\n");			
				}
				//failsafe in the case PatternType is not Constant
				else {
					toPrint.append(toStringTranslated(mode, experiment.getData(), bic, experiment.getScalability()) + "\n");
				}			
			}
			else {
				toPrint.append(toStringTranslated(mode, experiment.getData(), bic, experiment.getScalability()) + "\n");
			}
		}
		
		return toPrint.toString();
	}
		
	//adapted from Bicluster.toString(Dataset)
	private String toStringTranslated(TranslationMode mode, Dataset data, Bicluster bic, boolean scalability) {
		
		//build string part with the columns and rows that compose the bicluster
		StringBuffer res = new StringBuffer(bic.key != null ? "ID:" + bic.key : "");
		res.append(" (" + bic.columns.size() + "," + bic.rows.size() + ") Y=[");
		
		List<String> columns = new LinkedList<String>();

		int i;
		if(scalability) {
			for (Iterator<Integer> it = bic.columns.iterator(); it.hasNext(); res.append((String)data.originalColumns.get(i) + ",")) {
				i = ((Integer)it.next()).intValue();
				//get names from original columns array
				columns.add((String)data.originalColumns.get(i));
			}
		}
		else {
			for (Iterator<Integer> it = bic.columns.iterator(); it.hasNext(); res.append((String)data.columns.get(i) + ",")) {
				i = ((Integer)it.next()).intValue();
				//collect column names
				columns.add((String)data.columns.get(i));
			}
		}
		res.append("] X=[");
		
		//System.out.println(columns);

		int j;
		for (Iterator<Integer> it = bic.rows.iterator(); it.hasNext(); res.append((String)data.rows.get(j) + ",")) {
			j = ((Integer)it.next()).intValue();
		}
		res.append("]");

		//translate the indexes that compose the bicluster
		int[][] matrix = data.getBicluster(bic.columns, bic.rows);
		int k = 0;
		for (Iterator<Integer> it = bic.rows.iterator(); it.hasNext();)
		{
			int x = ((Integer)it.next()).intValue();
			res.append("\n");
			res.append((String)data.rows.get(x) + "\t");
			int y = 0;
			for (int l = matrix[k].length; y < l; y++) {				
				//get translated value
				String columnName = columns.get(y);
				if(mode == TranslationMode.ToCategories) {
					res.append(getTranslatedValue(indexToCategoriesMap, columnName, String.valueOf(matrix[k][y])) + "\t"); 
				} 
				else if(mode == TranslationMode.ToLabels) {
					String tempCategory = getTranslatedValue(indexToCategoriesMap, columnName, String.valueOf(matrix[k][y]));
					res.append(getTranslatedValue(categoriesToLabelsMap, columnName, tempCategory) + "\t"); 
				}
			}
			k++;
		}
		return res.toString().replace(",]", "]");
	}
	
	//method to print translated bicluster with class (PROBABLY DOES NOT WORK WITH SCABILITY)
	public String toStringTranslatedWithClass(TranslationMode mode, Dataset data, Bicluster bic, BiclustersEvaluator evaluator, String targetClassName) {
		
		//build string part with the columns and rows that compose the bicluster
		StringBuffer res = new StringBuffer(bic.key != null ? "ID:" + bic.key : "");
		res.append(" (" + bic.columns.size() + "," + bic.rows.size() + ") Y=[");
		
		List<String> columns = new LinkedList<String>();

		int i;
		for (Iterator<Integer> it = bic.columns.iterator(); it.hasNext(); res.append((String)data.columns.get(i) + ",")) {
			i = ((Integer)it.next()).intValue();
			//collect column names
			columns.add((String)data.columns.get(i));
		}
		//add target class to column list
		res.append("Target Class (" + targetClassName + ")");
		res.append("] X=[");

		int j;
		for (Iterator<Integer> it = bic.rows.iterator(); it.hasNext(); res.append((String)data.rows.get(j) + ",")) {
			j = ((Integer)it.next()).intValue();
		}
		res.append("]");
		
		//get target class values for each Bicluster row
		List<String> targetClassValues = evaluator.getTargetClassValues(bic, targetClassName);

		//translate the indexes that compose the bicluster
		int[][] matrix = data.getBicluster(bic.columns, bic.rows);
		int k = 0;
		for (Iterator<Integer> it = bic.rows.iterator(); it.hasNext();)
		{
			int x = ((Integer)it.next()).intValue();
			res.append("\n");
			res.append((String)data.rows.get(x) + "\t");
			int y = 0;
			for (int l = matrix[k].length; y < l; y++) {				
				//get translated value
				String columnName = columns.get(y);
				if(mode == TranslationMode.ToCategories) {
					res.append(getTranslatedValue(indexToCategoriesMap, columnName, String.valueOf(matrix[k][y])) + "\t"); 
				} 
				else if(mode == TranslationMode.ToLabels) {
					String tempCategory = getTranslatedValue(indexToCategoriesMap, columnName, String.valueOf(matrix[k][y]));
					res.append(getTranslatedValue(categoriesToLabelsMap, columnName, tempCategory) + "\t"); 
				}
			}
			//append class value for the respective row
			res.append(targetClassValues.get(k));
			k++;
		}
		return res.toString().replace(",]", "]");
	}
	
	//auxiliary function to get the correct translation
	private String getTranslatedValue(Map<String, List<Pair<String, String>>> translationMap, String columnName, String key) {
		
		List<Pair<String, String>> values = translationMap.get(columnName);
		
		for(Pair<String, String> pair : values) {
			if(pair.getKey().equals(key)) {
				return pair.getValue();
			}
		}
		return "Not found (key: " + key + " for category " + columnName +" )";
	}
	
	/** TranslationMode.Both for this method behaves as TranslationMode.ToLabels */
	public String getTranslatedValue(TranslationMode mode, String columnName, String key) {
		
		if(mode == TranslationMode.ToCategories) {
			return getTranslatedValue(indexToCategoriesMap, columnName, key); 
		} 
		else { // if(mode == TranslationMode.ToLabels) {
			String tempCategory = getTranslatedValue(indexToCategoriesMap, columnName, key);
			return getTranslatedValue(categoriesToLabelsMap, columnName, tempCategory); 
		}
	}
}
