package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import bic_experiment.BicPamsExperimentSet;
import bic_translator.BiclustersTranslator.TranslationMode;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import generator.BicMatrixGenerator.PatternType;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 2.0
 *  
 *  Class for utility constants, enums and methods
 */

public class Utils {
	
	//constants
	public static int MAX_COLS_XLSX = 16384;
	
	//enums
	public static enum StoppingCriteria
	{
	    MinBicsBeforeMerging, MinAreaPercentageElements, MinSupportPercentageRowsPerBic;
	    
	    private StoppingCriteria() {}
	} 
	
	public static enum RemoveCriteria
	{
		RemoveZeroEntries, RemoveNonDiffEntries, RemoveNone;
		
		private RemoveCriteria() {}
	}
	
	//auxiliary method to write a file
	public static void writeFile(String absolutePath, String content) throws Exception {
		FileWriter fstream = new FileWriter(absolutePath);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(content);
		out.close();
	}
	
	public static void printObjectMatrix(Object[][] objMatrix) {
		
		for (int i = 0; i < objMatrix.length; i++) {
			System.out.println(Arrays.toString(objMatrix[i]));
		}
	}
	
	public static void printProbabilityMatrix(String categories, Double[][] matrix) {
		
		System.out.println(categories);
		for (int i = 0; i < matrix.length; i++) {
			System.out.print("Bicluster #" + i + ":\t");
		    for (int j = 0; j < matrix[i].length; j++) {
		        System.out.print(matrix[i][j] + "\t");
		    }
		    System.out.println();
		}
	}
	
	public static String getCurrentDate() {
		Calendar c = Calendar.getInstance();
		
		StringBuffer result = new StringBuffer();		
		
		result.append(c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH));
		result.append("_");
		result.append(c.get(Calendar.HOUR_OF_DAY) + "-" + (c.get(Calendar.MINUTE) + 1) + "-");
		
		int secondTemp = c.get(Calendar.SECOND);
		
		result.append(secondTemp< 10 ? "0" + secondTemp : secondTemp);
		
		return result.toString();
	}
	
	//log base 2
	public static double log2(double value) {
		
		return Math.log10(value) / Math.log10(2);
	}
	
	//this returns the number of the column (not the index)
	public static int getMaximumNumberOfColumns(XSSFSheet sheet) {
		
		//get maximum number of columns for all rows
		int maxColumn = -1;
		for(int i = 0; i < sheet.getLastRowNum(); i++) {
			XSSFRow row = sheet.getRow(i);
			//added failsafe for null rows (due to empty row between experiments)
			if(row != null && row.getLastCellNum() > maxColumn) {
				maxColumn = row.getLastCellNum();
			}
		}	
		
		return maxColumn;
	}
	
	//wrapper function for tests
	public static void createAndRunExperimentSet(String currentDate, String dataset, String csvDataset, StoppingCriteria[] stoppingCriteria, 
			Double[] stoppingCriteriaValue, Integer[] minNrColumns, Integer[] nrIterations, Integer[] nrItems, 
			Boolean[] symmetric, Double[] minOverlapMerging, FilteringCriteria[] filter, 
			FillingCriteria[] missingsHandler, RemoveCriteria[] removeMissings, Double[] minFilteringValue, 
			PatternType[] patternType, ClosedImplementation[] closedImplementation, Orientation[] orientation, 
			Double[] significanceLevel, Boolean[] scalability, Double[] columnFilteringPerc, Boolean[] discriminative,
			String[] targetClassName, Boolean[] printPatternsOnly, String experimentSetFileFolder, 
			String translationIndexToCategoryFilePath, String translationCategoryToLabelFilePath, 
			TranslationMode translationMode) throws IOException {

		BicPamsExperimentSet expSet = new BicPamsExperimentSet(currentDate, dataset, csvDataset, stoppingCriteria, stoppingCriteriaValue,
								   minNrColumns, nrIterations, nrItems, symmetric, minOverlapMerging,
								   filter, missingsHandler, removeMissings, minFilteringValue, patternType,
								   closedImplementation, orientation, significanceLevel, scalability,
								   columnFilteringPerc, discriminative, targetClassName,
								   printPatternsOnly, experimentSetFileFolder, translationIndexToCategoryFilePath,
								   translationCategoryToLabelFilePath, translationMode);
		
		expSet.runExperimentSet();
	}
}
