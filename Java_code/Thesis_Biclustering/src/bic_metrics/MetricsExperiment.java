package bic_metrics;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import bic_experiment.BicPamsExperiment;
import bic_translator.BiclustersTranslator;
import bic_translator.BiclustersTranslator.TranslationMode;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 1.0
 *  
 *  Container class for the metrics from a given BicPamsExperiment (Biclustering solution)
 */
public class MetricsExperiment {

	//data fields
	private Set<String> targetClassValues; 	//unique target class values 
	private Double entropyBiclustering;
	private Double purityBiclustering;
	private List<Double> pValueBiclusters;
	private List<Double> entropyBiclusters;
	private List<Double> purityBiclusters;
	Map<Integer, List<Pair<String, Double>>> precisionClassBiclusters;
	Map<Integer, List<Pair<String, Double>>> recallClassBiclusters;
	Map<Integer, List<Pair<String, Double>>> fMeasureClassBiclusters;
	//array with purity levels to calculate the number of biclusters with purity higher than those levels
	private static Double[] purityLevels = {0.5D, 0.75D, 0.8D, 0.85D, 0.9D, 0.95D, 1D};
	
	//constructor	
	public MetricsExperiment(Set<String> targetClassValues, Double entropyBiclustering, Double purityBiclustering, 
							 List<Double> pValueBiclusters, List<Double> entropyBiclusters, List<Double> purityBiclusters, 
							 Map<Integer, List<Pair<String, Double>>> precisionClassBiclusters,
							 Map<Integer, List<Pair<String, Double>>> recallClassBiclusters,
							 Map<Integer, List<Pair<String, Double>>> fMeasureClassBiclusters) {

		this.targetClassValues = targetClassValues;
		this.entropyBiclustering = entropyBiclustering;
		this.purityBiclustering = purityBiclustering;
		this.pValueBiclusters = pValueBiclusters;
		this.entropyBiclusters = entropyBiclusters;
		this.purityBiclusters = purityBiclusters;
		this.precisionClassBiclusters = precisionClassBiclusters;
		this.recallClassBiclusters = recallClassBiclusters;
		this.fMeasureClassBiclusters = fMeasureClassBiclusters;
	}

	//getters
	public Set<String> getTargetClassValues() {
		return targetClassValues;
	}
	
	public Double getEntropyBiclustering() {
		return entropyBiclustering;
	}

	public Double getPurityBiclustering() {
		return purityBiclustering;
	}

	public List<Double> getPValueBiclusters() {
		return pValueBiclusters;
	}

	public List<Double> getEntropyBiclusters() {
		return entropyBiclusters;
	}

	public List<Double> getPurityBiclusters() {
		return purityBiclusters;
	}

	public Map<Integer, List<Pair<String, Double>>> getPrecisionClassBiclusters() {
		return precisionClassBiclusters;
	}

	public Map<Integer, List<Pair<String, Double>>> getRecallClassBiclusters() {
		return recallClassBiclusters;
	}

	public Map<Integer, List<Pair<String, Double>>> getfMeasureClassBiclusters() {
		return fMeasureClassBiclusters;
	}	
	
	public Double[] getPurityLevels() {
		return purityLevels;
	}
		
	//utility methods
	public Map<Double, Integer> getNumberBiclustersPurityLevels() {
		
		Map<Double, Integer> numberBicsPurityLevels = new TreeMap<Double, Integer>();
		
		//init counter map
		for(Double purityLevel : purityLevels) {
			numberBicsPurityLevels.put(purityLevel, 0);
		}
		
		//get number of biclusters with purity equal or greater than each defined level
		for(int i = 0; i < this.purityBiclusters.size(); i++) {			
			
			Double purity = this.purityBiclusters.get(i);
			
			for(Double purityLevel : purityLevels) {
				if(purity >= purityLevel) {
					//increase the count for the purity level
					int count = numberBicsPurityLevels.get(purityLevel);
					numberBicsPurityLevels.put(purityLevel, ++count);
				}
			}
		}
		
		return numberBicsPurityLevels;
	}
	
	public int getNumberSignificativeBiclusters(Double significanceLevel) {
		
		int count = 0;
		
		//get number of significant biclusters
		for(int i = 0; i < this.pValueBiclusters.size(); i++) {			
			//if the bicluster p-value is less than the significance level, it is significant
			if(this.pValueBiclusters.get(i) < significanceLevel) {
				count += 1;
			}
		}
		
		return count;
	}
	
	//print to string all calculated metrics for the biclustering solution
	public String toString() {
		
		StringBuilder toPrint = new StringBuilder();
		
		//entropy
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			toPrint.append("Entropy for Bicluster " + i + ": " + this.entropyBiclusters.get(i) + "\n");
		} 
		
		toPrint.append("Entropy for Biclustering: " + this.entropyBiclustering + "\n");
		
		//purity
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			toPrint.append("Purity for Bicluster " + i + ": " + this.purityBiclusters.get(i) + "\n");
		} 
		
		toPrint.append("Purity for Biclustering: " + this.purityBiclustering + "\n");
		
		//precision
		Set<Entry<Integer, List<Pair<String, Double>>>> entryPrecision = this.precisionClassBiclusters.entrySet();			
		Iterator<Entry<Integer, List<Pair<String, Double>>>> itPrecision = entryPrecision.iterator();
		
		while(itPrecision.hasNext()) {
			
			Entry<Integer, List<Pair<String, Double>>> entry = itPrecision.next();
			toPrint.append("Precision for Bicluster " + entry.getKey() + ":" + "\n");
			
			for (Pair<String, Double> pair : entry.getValue()) {
				toPrint.append("\tPrecision for class " + pair.getFirst() + ": " + pair.getSecond() + "\n");
			}
		}
		
		//recall
		Set<Entry<Integer, List<Pair<String, Double>>>> entryRecall = this.recallClassBiclusters.entrySet();			
		Iterator<Entry<Integer, List<Pair<String, Double>>>> itRecall = entryRecall.iterator();
		
		while(itRecall.hasNext()) {
			
			Entry<Integer, List<Pair<String, Double>>> entry = itRecall.next();
			toPrint.append("Recall for Bicluster " + entry.getKey() + ":" + "\n");
			
			for (Pair<String, Double> pair : entry.getValue()) {
				toPrint.append("\tRecall for class " + pair.getFirst() + ": " + pair.getSecond() + "\n");
			}
		}
			
		//f-measure
		Set<Entry<Integer, List<Pair<String, Double>>>> entryFMeasure = this.fMeasureClassBiclusters.entrySet();			
		Iterator<Entry<Integer, List<Pair<String, Double>>>> itFMeasure = entryFMeasure.iterator();
		
		while(itFMeasure.hasNext()) {
			
			Entry<Integer, List<Pair<String, Double>>> entry = itFMeasure.next();
			toPrint.append("F-Measure for Bicluster " + entry.getKey() + ":" + "\n");
			
			for (Pair<String, Double> pair : entry.getValue()) {
				toPrint.append("\tF-Measure for class " + pair.getFirst() + ": " + pair.getSecond() + "\n");
			}
		}
		
		return toPrint.toString();
	}
	
	//write all data to excel file sheet
	public int writeAllDataToExcelSheet(XSSFSheet sheet, int startingRowIndex, BicPamsExperiment experiment) {
		
		int rowIndex = startingRowIndex;
		int numberClasses = this.targetClassValues.size();
		
		//Create font to highlight the start of a given experiment data
		XSSFFont experimentFont = sheet.getWorkbook().createFont();
		experimentFont.setFontHeightInPoints((short)14);
		experimentFont.setBold(true);
		//set style with that font
		CellStyle experimentStyle = sheet.getWorkbook().createCellStyle(); 
		experimentStyle.setFont(experimentFont);
		
		//Create style to just set bold text in cells
		XSSFFont boldFont = sheet.getWorkbook().createFont();
		boldFont.setBold(true);
		//Set text in bold
		CellStyle boldStyle = sheet.getWorkbook().createCellStyle(); 
		boldStyle.setFont(boldFont);
		
		/**first row (experiment id)*/
		XSSFRow row = sheet.createRow(rowIndex++);
		
		XSSFCell cell = row.createCell(0);
		cell.setCellValue("Experiment " + experiment.getExperimentId());
		cell.setCellStyle(experimentStyle);
		
		/**second + third rows (experiment data)*/			
		List<Pair<String,String>> experimentData = experiment.getExperimentInfo();
		
		XSSFRow row2 = sheet.createRow(rowIndex++);
		
		int colIndex = 0;
		for(Pair<String,String> pair : experimentData) {		
			row2.createCell(colIndex++).setCellValue(pair.getFirst());
		}	
		
		XSSFRow row3 = sheet.createRow(rowIndex++);
		
		colIndex = 0;
		for(Pair<String,String> pair : experimentData) {
			row3.createCell(colIndex++).setCellValue(pair.getSecond());
		}	
		
		/**update row counter to skip one line*/
		++rowIndex;
		
		/**fourth row (headers)*/
		colIndex = 1;
		XSSFRow row4 = sheet.createRow(rowIndex++);
		
		//Bicluster ID
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Bicluster ID");
		cell.setCellStyle(boldStyle);
		
		//Entropy
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Entropy");
		cell.setCellStyle(boldStyle);
		
		//Purity
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Purity");
		cell.setCellStyle(boldStyle);
		
		//p-value
		cell = row4.createCell(colIndex++);
		cell.setCellValue("p-value");
		cell.setCellStyle(boldStyle);
			
		//Precision
		cell = row4.createCell(colIndex);
		cell.setCellValue("Precision");
		cell.setCellStyle(boldStyle);
		
		//advance the colIndex according the number of categories in the target class
		colIndex += numberClasses;		
		
		//Recall
		cell = row4.createCell(colIndex);
		cell.setCellValue("Recall");
		cell.setCellStyle(boldStyle);
		
		//advance the colIndex according the number of categories in the target class
		colIndex += numberClasses;	
			
		//F-Measure
		cell = row4.createCell(colIndex);
		cell.setCellValue("F-Measure");
		cell.setCellStyle(boldStyle);
		
//		row4.createCell(colIndex++).setCellValue("Bicluster ID");
//		row4.createCell(colIndex++).setCellValue("Entropy");
//		row4.createCell(colIndex++).setCellValue("Purity");
//		row4.createCell(colIndex++).setCellValue("p-value");
//		row4.createCell(colIndex).setCellValue("Precision");
//		//advance the colIndex according the number of categories in the target class
//		colIndex += numberClasses;		
//		row4.createCell(colIndex).setCellValue("Recall");
//		//advance the colIndex according the number of categories in the target class
//		colIndex += numberClasses;		
//		row4.createCell(colIndex).setCellValue("F-Measure");
		
		/**fifth row (target class categories)*/
		colIndex = 0;
		XSSFRow row5 = sheet.createRow(rowIndex++);
		
		row5.createCell(colIndex).setCellValue("Categories ->");
		//skip the bicluster id, entropy, purity and p-value columns, go to the first precision column
		colIndex += 5;	
		//put all target class categories (one per column) 3 times, for precision, recall and f-measure	blocks
		for(int i = 0; i < 3; i++) {
			Iterator<String> it = this.targetClassValues.iterator();
			
			while(it.hasNext()) {			
				row5.createCell(colIndex++).setCellValue(it.next());
			}
		}
		
		/**biclusters data rows*/
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			
			colIndex = 1;
			XSSFRow dataRow = sheet.createRow(rowIndex++);
			
			//bicluster id (bicluster numbers start in 1)
			dataRow.createCell(colIndex++).setCellValue(i + 1);	
			//bicluster entropy
			dataRow.createCell(colIndex++).setCellValue(this.entropyBiclusters.get(i));
			//bicluster purity
			dataRow.createCell(colIndex++).setCellValue(this.purityBiclusters.get(i));
			//bicluster p-value
			dataRow.createCell(colIndex++).setCellValue(this.pValueBiclusters.get(i));
			
			//bicluster precision (for all target class values)
			List<Pair<String, Double>> listPrecision = this.precisionClassBiclusters.get(i);
			
			for (int j = 0; j < listPrecision.size(); j++) {
				dataRow.createCell(colIndex++).setCellValue(listPrecision.get(j).getSecond());
			}
			
			//bicluster recall (for all target class values)
			List<Pair<String, Double>> listRecall = this.recallClassBiclusters.get(i);
			
			for (int j = 0; j < listRecall.size(); j++) {
				dataRow.createCell(colIndex++).setCellValue(listRecall.get(j).getSecond());
			}
			
			//bicluster f-measure (for all target class values)
			List<Pair<String, Double>> listFMeasure = this.fMeasureClassBiclusters.get(i);
			
			for (int j = 0; j < listFMeasure.size(); j++) {
				dataRow.createCell(colIndex++).setCellValue(listFMeasure.get(j).getSecond());
			}
		}
		
		/**biclustering data row*/
		colIndex = 1;
		XSSFRow bicsRow = sheet.createRow(rowIndex++);
		
		//solution id
		bicsRow.createCell(colIndex++).setCellValue("Solution");
		//biclustering entropy
		bicsRow.createCell(colIndex++).setCellValue(this.entropyBiclustering);
		//biclustering purity
		bicsRow.createCell(colIndex++).setCellValue(this.purityBiclustering);
		
		/**update row counter to skip one line*/
		++rowIndex;
		
		/**number of significant biclusters row*/
		int numberSigBiclusters = getNumberSignificativeBiclusters(experiment.getSignificanceLevel());
		
		colIndex = 1;
		XSSFRow sigBicsRow = sheet.createRow(rowIndex++);
		//No. Sig. Bics
		cell = sigBicsRow.createCell(colIndex++);
		cell.setCellValue("No. Sig. Bics (< " + experiment.getSignificanceLevel() + ")");
		cell.setCellStyle(boldStyle);
		//number of significant biclusters
		sigBicsRow.createCell(colIndex++).setCellValue(numberSigBiclusters);
		
		/**number of pure biclusters rows*/
		Map<Double, Integer> numberBicsPurityLevels = getNumberBiclustersPurityLevels();		
		Set<Entry<Double, Integer>> entrySet = numberBicsPurityLevels.entrySet();
		
		Iterator<Entry<Double, Integer>> it = entrySet.iterator();
		
		while(it.hasNext()) {		
			Entry<Double, Integer> current = it.next();
			
			colIndex = 1;
			XSSFRow pureRow = sheet.createRow(rowIndex++);
			//No. Pure Bics
			cell = pureRow.createCell(colIndex++);
			
			if(current.getKey() == 1D) {
				cell.setCellValue("No. Pure Bics (= " + current.getKey() + ")");
			}
			else {
				cell.setCellValue("No. Pure Bics (> " + current.getKey() + ")");
			}
			cell.setCellStyle(boldStyle);
			//number of pure biclusters for the current level
			pureRow.createCell(colIndex++).setCellValue(current.getValue());
		}
				
		//return index + 1 (to leave an empty row between experiments)
		return ++rowIndex;
	}
	
	public int writePurityPrecisionDataToExcelSheet(XSSFWorkbook workbook, int startingRowIndex, BicPamsExperiment experiment, BiclustersTranslator translator, TranslationMode mode) {
		
		int rowIndex = startingRowIndex;
		int numberClasses = this.targetClassValues.size();
		
		/**Get Experiments sheet*/
		XSSFSheet sheet = workbook.getSheetAt(0);
		
		//Create font to highlight the start of a given experiment data
		XSSFFont experimentFont = sheet.getWorkbook().createFont();
		experimentFont.setFontHeightInPoints((short)14);
		experimentFont.setBold(true);
		//set style with that font
		CellStyle experimentStyle = sheet.getWorkbook().createCellStyle(); 
		experimentStyle.setFont(experimentFont);
		
		//Create style to just set bold text in cells
		XSSFFont boldFont = sheet.getWorkbook().createFont();
		boldFont.setBold(true);
		//Set text in bold
		CellStyle boldStyle = sheet.getWorkbook().createCellStyle(); 
		boldStyle.setFont(boldFont);
		
		//Set green background
		CellStyle greenBackground = sheet.getWorkbook().createCellStyle(); 
		greenBackground.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		greenBackground.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		CellStyle yellowBackground = sheet.getWorkbook().createCellStyle(); 
		yellowBackground.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		yellowBackground.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		CellStyle orangeBackground = sheet.getWorkbook().createCellStyle(); 
		orangeBackground.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		orangeBackground.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		/**first row (experiment id)*/
		XSSFRow row = sheet.createRow(rowIndex++);
		
		XSSFCell cell = row.createCell(0);
		cell.setCellValue("Experiment " + experiment.getExperimentId());
		cell.setCellStyle(experimentStyle);
		
		/**second + third rows (experiment data)*/			
		List<Pair<String,String>> experimentData = experiment.getExperimentInfo();
		
		XSSFRow row2 = sheet.createRow(rowIndex++);
		
		int colIndex = 0;
		for(Pair<String,String> pair : experimentData) {		
			row2.createCell(colIndex++).setCellValue(pair.getFirst());
		}	
		
		XSSFRow row3 = sheet.createRow(rowIndex++);
		
		colIndex = 0;
		for(Pair<String,String> pair : experimentData) {
			row3.createCell(colIndex++).setCellValue(pair.getSecond());
		}	
		
		/**update row counter to skip one line*/
		++rowIndex;
		
		/**fourth row (headers)*/
		colIndex = 1;
		XSSFRow row4 = sheet.createRow(rowIndex++);
		
		//Bicluster ID
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Bicluster ID");
		cell.setCellStyle(boldStyle);
		
		//Entropy
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Entropy");
		cell.setCellStyle(boldStyle);
		
		//Purity
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Purity");
		cell.setCellStyle(boldStyle);
		
		//p-value
		cell = row4.createCell(colIndex++);
		cell.setCellValue("p-value");
		cell.setCellStyle(boldStyle);
		
		//Number of Rows
		cell = row4.createCell(colIndex++);
		cell.setCellValue("Number Rows");
		cell.setCellStyle(boldStyle);
		
		//% of Rows
		cell = row4.createCell(colIndex++);
		cell.setCellValue("% of Dataset Rows");
		cell.setCellStyle(boldStyle);
			
		//Precision
		cell = row4.createCell(colIndex);
		cell.setCellValue("Precision");
		cell.setCellStyle(boldStyle);
		
		//advance the colIndex according the number of categories in the target class
		colIndex += numberClasses;		
		
		//Pattern
//		cell = row4.createCell(colIndex);
//		cell.setCellValue("Pattern");
//		cell.setCellStyle(boldStyle);

		/**fifth row (target class categories)*/
		colIndex = 0;
		XSSFRow row5 = sheet.createRow(rowIndex++);
		
		row5.createCell(colIndex).setCellValue("Categories ->");
		//skip the first empty column, bicluster id, entropy, purity, 
		//p-value and number and % of rows columns, to go to the first precision column
		colIndex += 7;	
		//put all target class categories (one per column) 1 time, for precision block only
		Iterator<String> it = this.targetClassValues.iterator();
		
		while(it.hasNext()) {			
			row5.createCell(colIndex++).setCellValue(it.next());
		}
		
		/**biclusters data rows*/
		DecimalFormat df = new DecimalFormat("#.00"); 
		
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			
			colIndex = 1;
			XSSFRow dataRow = sheet.createRow(rowIndex++);
			
			//bicluster id (bicluster numbers start in 1)
			dataRow.createCell(colIndex++).setCellValue(i + 1);	
			//bicluster entropy
			dataRow.createCell(colIndex++).setCellValue(this.entropyBiclusters.get(i));
			//bicluster purity
			dataRow.createCell(colIndex++).setCellValue(this.purityBiclusters.get(i));
			//bicluster p-value
			dataRow.createCell(colIndex++).setCellValue(this.pValueBiclusters.get(i));
			//bicluster number of rows
			dataRow.createCell(colIndex++).setCellValue(experiment.getBics().get(i).numRows());
			//bicluster % of dataset rows
			double percDatasetRows = (double) experiment.getBics().get(i).numRows() / experiment.getData().rows.size();
			dataRow.createCell(colIndex++).setCellValue(Double.valueOf(df.format(percDatasetRows)));
			
			//bicluster precision (for all target class values)
			List<Pair<String, Double>> listPrecision = this.precisionClassBiclusters.get(i);
			
			for (int j = 0; j < listPrecision.size(); j++) {
				dataRow.createCell(colIndex++).setCellValue(listPrecision.get(j).getSecond());
			}
			
			//set green row for biclusters with purity = 1 and precision for class 1 = 1
			if(this.purityBiclusters.get(i) == 1D && this.precisionClassBiclusters.get(i).get(0).getSecond() == 1D) {
				for(int j = 0; j < dataRow.getLastCellNum(); j++){//For each cell in the row 
					if(dataRow.getCell(j) != null) {
						dataRow.getCell(j).setCellStyle(greenBackground);//Set the style
					} 
			    }
			}
			
			//set yellow row for biclusters with purity between 0.75 and 1 and precision for class 1 between 0.75 and 1
			if(this.purityBiclusters.get(i) >= 0.75D && this.purityBiclusters.get(i) < 1D &&
			   this.precisionClassBiclusters.get(i).get(0).getSecond() >= 0.75D && this.precisionClassBiclusters.get(i).get(0).getSecond() < 1D) {
				for(int j = 0; j < dataRow.getLastCellNum(); j++){//For each cell in the row 
					if(dataRow.getCell(j) != null) {
						dataRow.getCell(j).setCellStyle(yellowBackground);//Set the style
					} 
			    }
			}
			
			//set orange row for biclusters with purity = 1 and precision for class 2 = 1
			if(this.purityBiclusters.get(i) == 1D && this.precisionClassBiclusters.get(i).get(1).getSecond() == 1D) {
				for(int j = 0; j < dataRow.getLastCellNum(); j++){//For each cell in the row 
					if(dataRow.getCell(j) != null) {
						dataRow.getCell(j).setCellStyle(orangeBackground);//Set the style
					} 
			    }
			}
			
//			//bicluster pattern (column and respective label)
//			Bicluster currentBic = experiment.getBics().get(i);
//			List<String> bicColumnNames = new LinkedList<String>();
//			Dataset data = experiment.getData();
//			
//			//get column names
//			for(int idx : currentBic.columns) {
//				bicColumnNames.add(data.originalColumns.get(idx));
//			} 
//			
//			//get translated pattern values for perfect constant biclusters
//			List<String> translatedPattern = new LinkedList<String>();
//			int[] matrixFirstLine = data.getBicluster(currentBic.columns,currentBic.rows)[0];
//
//			//(all rows are equal so we can look at the first)
//			for(int j = 0; j < matrixFirstLine.length; j++) {
//				translatedPattern.add(translator.getTranslatedValue(mode, bicColumnNames.get(j), String.valueOf(matrixFirstLine[j])));
//			}
//			
//			for(int k = 0; k < bicColumnNames.size(); k++) {
//				dataRow.createCell(colIndex++).setCellValue(bicColumnNames.get(k) + " : " + translatedPattern.get(k));  
//			}
		}
		
		/**biclustering data row*/
		colIndex = 1;
		XSSFRow bicsRow = sheet.createRow(rowIndex++);
		
		//solution id
		bicsRow.createCell(colIndex++).setCellValue("Solution");
		//biclustering entropy
		bicsRow.createCell(colIndex++).setCellValue(this.entropyBiclustering);
		//biclustering purity
		bicsRow.createCell(colIndex++).setCellValue(this.purityBiclustering);
		
		/**update row counter to skip one line*/
		++rowIndex;
		
		/**number of significant biclusters row*/
		int numberSigBiclusters = getNumberSignificativeBiclusters(experiment.getSignificanceLevel());
		
		colIndex = 1;
		XSSFRow sigBicsRow = sheet.createRow(rowIndex++);
		//No. Sig. Bics
		cell = sigBicsRow.createCell(colIndex++);
		cell.setCellValue("No. Sig. Bics (< " + experiment.getSignificanceLevel() + ")");
		cell.setCellStyle(boldStyle);
		//number of significant biclusters
		sigBicsRow.createCell(colIndex++).setCellValue(numberSigBiclusters);
		
		/**number of pure biclusters rows*/
		Map<Double, Integer> numberBicsPurityLevels = getNumberBiclustersPurityLevels();		
		Set<Entry<Double, Integer>> entrySet = numberBicsPurityLevels.entrySet();
		
		Iterator<Entry<Double, Integer>> it2 = entrySet.iterator();
		
		while(it2.hasNext()) {		
			Entry<Double, Integer> current = it2.next();
			
			colIndex = 1;
			XSSFRow pureRow = sheet.createRow(rowIndex++);
			//No. Pure Bics
			cell = pureRow.createCell(colIndex++);
			
			if(current.getKey() == 1D) {
				cell.setCellValue("No. Pure Bics (= " + current.getKey() + ")");
			}
			else {
				cell.setCellValue("No. Pure Bics (> " + current.getKey() + ")");
			}
			cell.setCellStyle(boldStyle);
			//number of pure biclusters for the current level
			pureRow.createCell(colIndex++).setCellValue(current.getValue());
		}
				
		//return index + 1 (to leave an empty row between experiments)
		return ++rowIndex;
	}
	
	public static int writePurityRowsAvgHeaderToExcelSheet(XSSFWorkbook workbook, int startingRowIndex) {
		
		int rowIndex = startingRowIndex;
	
		/**Get Calculations sheet*/
		XSSFSheet sheet2 = workbook.getSheetAt(1);
		
		//header
		int colIndex = 0;
		
		XSSFRow row = sheet2.createRow(rowIndex++);
		
		row.createCell(colIndex++).setCellValue("Experiment Id");
		row.createCell(colIndex++).setCellValue("Stopping Criteria Value");
		row.createCell(colIndex++).setCellValue("Stopping Criteria Value (%)");
		row.createCell(colIndex++).setCellValue("Quality");
		row.createCell(colIndex++).setCellValue("Min. Nr. Columns");
		row.createCell(colIndex++).setCellValue("Biclustering Purity");
		row.createCell(colIndex++).setCellValue("Avg. Nr. Bic. Lines");
		row.createCell(colIndex++).setCellValue("Avg. % Bic. Lines");
		row.createCell(colIndex++).setCellValue("No. Found Bics");
		row.createCell(colIndex++).setCellValue("No. Purest Bics (purity >= 0.75)");
		
		for(int i = 0; i < purityLevels.length; i++) {
			double current = purityLevels[i];

			if(current == 1D) {
				row.createCell(colIndex++).setCellValue("No. Pure Bics (= " + current + ")");
			}
			else {
				row.createCell(colIndex++).setCellValue("No. Pure Bics (> " + current + ")");
			}
		}
		
		row.createCell(colIndex++).setCellValue("No. Pure Patient Bics (= 1.0)");
		
		return rowIndex;
	}
	
	public int writePurityRowsAvgToExcelSheet(XSSFWorkbook workbook, int startingRowIndex, BicPamsExperiment experiment) {
			
		int rowIndex = startingRowIndex;
	
		/**Get Calculations sheet*/
		XSSFSheet sheet2 = workbook.getSheetAt(1);

		//data
		int colIndex = 0;
		XSSFRow row = sheet2.createRow(rowIndex++);
		
		//write experiment id
		row.createCell(colIndex++).setCellValue(experiment.getExperimentId());
		//write stopping criteria value
		row.createCell(colIndex++).setCellValue(experiment.getStoppingCriteriaValue());
		//write stopping criteria value (%)
		row.createCell(colIndex++).setCellValue(experiment.getStoppingCriteriaValue()*100);
		//write quality
		row.createCell(colIndex++).setCellValue(experiment.getMinOverlapMerging());
		//write experiment minimum number of bicluster columns
		row.createCell(colIndex++).setCellValue(experiment.getMinNrColumns());
		//write biclustering purity 
		row.createCell(colIndex++).setCellValue(this.purityBiclustering);
		//calculate average number of lines and percentage of lines for all biclusters in the biclustering
		double avgCount = 0;
		double avgPerc = 0;
		//number of pure bics for patient class
		int purePatientClassBics = 0; 
		//number of purest biclusters in total (purity >= 0.75)
		int purestBics = 0; 
		
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
		
			//bicluster number of rows
			avgCount += experiment.getBics().get(i).numRows();
			//bicluster % of dataset rows
			double percDatasetRows = (double) experiment.getBics().get(i).numRows() / experiment.getData().rows.size();
			avgPerc += percDatasetRows;
			
			//get number of pure biclusters for Patient class
			if(this.purityBiclusters.get(i) == 1D && this.precisionClassBiclusters.get(i).get(0).getSecond() == 1D) {
				purePatientClassBics++;
			}
			
			//get purest biclusters
			if(this.purityBiclusters.get(i) >= 0.75D) {
				purestBics++;
			}
		}
		
		//get count and percentage mean for each biclustering
		avgCount = avgCount / this.entropyBiclusters.size();
		avgPerc = avgPerc / this.entropyBiclusters.size();
		
		//write average number of lines and percentage of lines for all biclusters in the biclustering
		row.createCell(colIndex++).setCellValue(avgCount);
		row.createCell(colIndex++).setCellValue(avgPerc);	
		
		//write number of found biclusters in total
		row.createCell(colIndex++).setCellValue(this.entropyBiclusters.size());	
		
		//write number of purest (purity >= 0.75) biclusters
		row.createCell(colIndex++).setCellValue(purestBics);	
		
		//write number of biclusters with various levels of purity	
		Map<Double, Integer> numberBicsPurityLevels = getNumberBiclustersPurityLevels();		
		Set<Entry<Double, Integer>> entrySet = numberBicsPurityLevels.entrySet();
		Iterator<Entry<Double, Integer>> it = entrySet.iterator();
		
		while(it.hasNext()) {		
			Entry<Double, Integer> current = it.next();

			//number of pure biclusters for the current level
			row.createCell(colIndex++).setCellValue(current.getValue());
		}
		
		//add pure patient class bics
		row.createCell(colIndex++).setCellValue(purePatientClassBics);
		
		//already been iterated (next line will go in the row right below)
		return rowIndex;
	}

	//obtain the ids (index + 1) of purest patient bics of each Experiment and write them on an new sheet
	public int writePurestBiclustersToExcelSheet(XSSFWorkbook workbook, int startingRowIndex, BicPamsExperiment experiment) {
	
		int rowIndex = startingRowIndex;
		
		/**Get PurestBiclusters sheet*/
		XSSFSheet sheet3 = workbook.getSheetAt(2);
		
		//start a new row
		int colIndex = 0;
		XSSFRow row = sheet3.createRow(rowIndex++);
		
		//add experiment name on first cell
		row.createCell(colIndex++).setCellValue(experiment.getExperimentId());
		
		//find the purest bics and write their ids (index + 1) on the row
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			if(this.purityBiclusters.get(i) >= 0.75D) {
				//since we can have multiclass problems beyond binary classification,
				//it is easier to identify the bicluster id with its respective class
				//in order to support any kind of target class values => class:bic_id
				
				//get class with highest precision
				List<Pair<String, Double>> listClassPrecisionValues = this.precisionClassBiclusters.get(i);
				String maxPrecisionClass = "";
				double temp_value = -1;
				for(Pair<String, Double> pair : listClassPrecisionValues) {
					if(pair.getSecond().doubleValue() > temp_value) {
						maxPrecisionClass = pair.getFirst();
						temp_value = pair.getSecond().doubleValue();
					}
				}
				//if the maximum precision found is >= 0.75, write it down
				if(temp_value >= 0.75D) {
					row.createCell(colIndex++).setCellValue(maxPrecisionClass + ":" + (i + 1));
				}
			}
		}
		
		//already been iterated (next line will go in the row right below)
		return rowIndex;
	}
	
	//when too many discriminative biclusters were found (more than the maximum number of XLSX columns) 
	//write the name of the file on the PurestBiclusters sheet
	public int writePurestBiclustersFileToExcelSheet(XSSFWorkbook workbook, int startingRowIndex, BicPamsExperiment experiment, String fileName) {
	
		int rowIndex = startingRowIndex;
		
		/**Get PurestBiclusters sheet*/
		XSSFSheet sheet3 = workbook.getSheetAt(2);
		
		//start a new row
		int colIndex = 0;
		XSSFRow row = sheet3.createRow(rowIndex++);
		
		//add experiment name on first cell
		row.createCell(colIndex++).setCellValue(experiment.getExperimentId());
		//add file name on second cell
		row.createCell(colIndex++).setCellValue(fileName);
		
		//already been iterated (next line will go in the row right below)
		return rowIndex;
	}

	public Pair<String, Integer> getPurestBiclusters(BicPamsExperiment experiment) {
		
		StringBuffer buf = new StringBuffer();
		
		int purestBics = 0; 
		
		buf.append(experiment.getExperimentId() + '\t');
		
		//find the purest bics and write their ids (index + 1) on the row
		for(int i = 0; i < this.entropyBiclusters.size(); i++) {
			if(this.purityBiclusters.get(i) >= 0.75D) {
				//since we can have multiclass problems beyond binary classification,
				//it is easier to identify the bicluster id with its respective class
				//in order to support any kind of target class values => class:bic_id
				
				//get class with highest precision
				List<Pair<String, Double>> listClassPrecisionValues = this.precisionClassBiclusters.get(i);
				String maxPrecisionClass = "";
				double temp_value = -1;
				for(Pair<String, Double> pair : listClassPrecisionValues) {
					if(pair.getSecond().doubleValue() > temp_value) {
						maxPrecisionClass = pair.getFirst();
						temp_value = pair.getSecond().doubleValue();
					}
				}
				//if the maximum precision found is >= 0.75, write it down
				if(temp_value >= 0.75D) {
					buf.append(maxPrecisionClass + ":" + (i + 1) + '\t');
					purestBics++;
				}
			}
		}
		
		return new Pair<String, Integer>(buf.toString(), purestBics);
	}
}
