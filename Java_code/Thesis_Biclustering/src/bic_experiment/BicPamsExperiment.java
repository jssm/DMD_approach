package bic_experiment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;

import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.constant.ConstantOverallBiclusterMiner;
import bicpam.closing.BiclusterFilter;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.gui.ChartPlotter;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import bicpam.significance.BSignificance;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import utils.Utils;
import utils.Utils.RemoveCriteria;
import utils.Utils.StoppingCriteria;
import utils.others.CopyUtils;
import utils.others.RemovalUtils;

/** @author Joana Matos
 *  @contact jmatos@lasige.di.fc.ul.pt
 *  @version 1.0
 */

public class BicPamsExperiment {
	
	//data fields
	private String experimentId;						//Experiment Id
	private Dataset data; 								//Dataset read from dataset file (default type: matrix) 
	private String inputFilePath;						//Input file path used
	private StoppingCriteria stoppingCriteria; 			//Stopping criteria type (default: minimum number of biclusters before merging)
	private double stoppingCriteriaValue;				//Stopping criteria value (default: 50)
	private int minNrColumns;							//Minimum #columns (default: 4)
	private int nrIterations;							//Number of iterations, increase for more even space exploration (default: 2)
	private int nrItems;								//Coherency Strength (#items, default: 6)
	private boolean symmetric;  						//Symmetries (default: false)
	private double minOverlapMerging; 					//Quality (default: 0.8)
	private FilteringCriteria filter;					//Merging procedure (default: FilteringCriteria.Overall)
	private FillingCriteria missingsHandler;			//Missings Handler (default: Remove -> only type supported)
	private RemoveCriteria removeElements;				//Type of uninformative values removal (default: Zero Entries)
	private double minFilteringValue;					//Filtering arguments (default: 50)
	private PatternType patternType;					//Coherency assumption (default: PatternType.Constant)
	private ClosedImplementation closedImplementation; 	//Pattern representation (Closed (maximal bics), default: DCharm)
	private Orientation orientation;					//Coherency orientation (default: Orientation.PatternOnRows)
	private double significanceLevel;					//Significance level (alpha), p-values of biclusters must be below this value 
	private boolean scalability;						//Flag that indicates if scalable measures should be used
	private double columnFilteringPerc;					//Percentage of the dataset columns with largest variability to keep 
	private boolean discriminative;						//Flag to know if the biclustering has a target class
	private String targetClassName;						//Name of the target class (null if discriminative is false)
	private boolean printPatternsOnly;					//Flag to print Bicluster patterns only; if false or PatternType is not Constant, whole bicluster is printed
	private String experimentSetFileFolder;				//Where output files are saved
	private String outputFilename;						//Name for the output file
	private Biclusters bics;							//Result of the experiment (Set of Bicluster)
	
	//standard constructor
	public BicPamsExperiment(String experimentId, Dataset data, String inputFilePath, StoppingCriteria stoppingCriteria, 
							 double stoppingCriteriaValue, int minNrColumns, int nrIterations, int nrItems, 
							 boolean symmetric, double minOverlapMerging, FilteringCriteria filter, 
							 FillingCriteria missingsHandler, RemoveCriteria removeElements, double minFilteringValue, 
							 PatternType patternType, ClosedImplementation closedImplementation, 
							 Orientation orientation, double significanceLevel, boolean scalability, 
							 double columnFilteringPerc, boolean discriminative, String targetClassName, boolean printPatternsOnly,
							 String experimentSetFileFolder, String outputFilename) {		
		
		this.experimentId = experimentId;
		this.data = data;
		this.inputFilePath = inputFilePath;
		this.stoppingCriteria = stoppingCriteria;
		this.stoppingCriteriaValue = stoppingCriteriaValue;
		this.minNrColumns = minNrColumns;
		this.nrIterations = nrIterations;
		this.nrItems = nrItems;
		this.symmetric = symmetric;
		this.minOverlapMerging = minOverlapMerging;
		this.filter = filter;
		this.missingsHandler = missingsHandler;
		this.removeElements = removeElements;
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
		this.outputFilename = outputFilename;
	}
	
	//constructor with default values
	public BicPamsExperiment(String experimentId, Dataset data, String inputFilePath, String experimentSetFileFolder, 
												  String outputFilename) {	
		
		this.data = data;
		this.inputFilePath = inputFilePath;
		this.stoppingCriteria = StoppingCriteria.MinBicsBeforeMerging;
		this.stoppingCriteriaValue = 50;
		this.minNrColumns = 4;
		this.nrIterations = 2;
		this.nrItems = 50;
		this.symmetric = false;
		this.minOverlapMerging = 0.8;
		this.filter = FilteringCriteria.Overall;
		this.missingsHandler = FillingCriteria.RemoveValue;
		this.removeElements = RemoveCriteria.RemoveZeroEntries;
		this.minFilteringValue = 50;
		this.patternType = PatternType.Constant;
		this.closedImplementation = ClosedImplementation.DCharm;
		this.orientation = Orientation.PatternOnRows;
		this.significanceLevel = 0.05;
		this.scalability = false;
		this.columnFilteringPerc = 0;
		this.discriminative = false;
		this.targetClassName = null;
		this.printPatternsOnly = false;
		this.experimentSetFileFolder = experimentSetFileFolder;
		this.outputFilename = outputFilename;
	}
	
	//other methods
	public Biclusters run() throws Exception {
		
		/** Stage 1: Preprocessing **/

		StringBuffer toPrint = new StringBuffer();	
		
		System.out.println("\n******************** EXPERIENCE PARAMETERS:\n" + this.toString() + "\n");
		toPrint.append("EXPERIENCE PARAMETERS:\n" + this.toString() + "\n");	
		
		toPrint.append("ORIGINAL DATASET:\n" + this.data.getStatistics() + "\n");
		System.out.println("ORIGINAL DATASET: " + this.data.getStatistics());
				
		/** Define Itemizer for Mapping **/		
		if(this.scalability) {
			
			//use original code
			if(this.columnFilteringPerc < 0) {
				this.data = Itemizer.run(data, 										//Matrix
										this.nrItems, 								//Coherency Strength (#items)
										this.symmetric, 							//Symmetries
										NormalizationCriteria.None, 				//Normalization
										DiscretizationCriteria.None,				//Discretization
										NoiseRelaxation.None, 						//Noise handler
										this.missingsHandler,						//Missings handler	
										this.scalability, 							//Scalability
										this.orientation);							//Orientation
			}
			//use new constructor (Joana)
			else {
				this.data = Itemizer.run(data, 										//Matrix
										this.nrItems, 								//Coherency Strength (#items)
										this.symmetric, 							//Symmetries
										NormalizationCriteria.None, 				//Normalization
										DiscretizationCriteria.None,				//Discretization
										NoiseRelaxation.None, 						//Noise handler
										this.missingsHandler,						//Missings handler	
										this.scalability, 							//Scalability
										this.orientation,							//Orientation
										this.columnFilteringPerc);					//Percentage of the dataset columns to keep 
			}
	
			toPrint.append("\nWITH SCALABILITY - REMAINING COLUMNS: \n");
			for(int i = 0; i < this.data.columns.size(); i++) {
				toPrint.append(this.data.columns.get(i) + "\n");
			}
		}
		else {
			this.data = Itemizer.run(data, 										//Matrix
									 this.nrItems, 								//Coherency Strength (#items)
									 this.symmetric, 							//Symmetries
									 NormalizationCriteria.None, 				//Normalization
									 DiscretizationCriteria.None,				//Discretization
									 NoiseRelaxation.None, 						//Noise handler
									 this.missingsHandler);						//Missings handler	
			
			toPrint.append("\nWITHOUT SCALABILITY - USED COLUMNS: \n");
			for(int i = 0; i < this.data.columns.size(); i++) {
				toPrint.append(this.data.columns.get(i) + "\n");
			}
		}
				
		//Remove elements with Zero-Entries depends on symmetries are being used and on the number of Coherency Strength (#items)
		// -> symmetry + (items > 5) = removes -1, 0 and 1 values
		// -> no symmetry + (items > 4) = removes 0 and 1 values
		// -> else = = removes 0 values
			
		//removals can be "Zero-Entries", "Non-Diff. Values" or "None"
		String removals;
		if(this.removeElements == RemoveCriteria.RemoveZeroEntries) {
			removals = "Zero-Entries";	
		} else if(this.removeElements == RemoveCriteria.RemoveNonDiffEntries) {
			removals = "Non-Diff. Values";
		} else {
			removals = "None";	
		}	
		
		List<Integer> remItems = RemovalUtils.getItemsToRemove(this.nrItems, removals, this.symmetric);

		//remove "removals" from the data matrix
		this.data = ItemMapper.remove(this.data, remItems, this.nrItems, this.symmetric);
		
		System.out.println("ALTERED DATASET (after " + remItems.toString() + " removal):\n" + this.data.getStatistics());
		toPrint.append("\nALTERED DATASET (after " + remItems.toString() + " removal):\n" + this.data.getStatistics() + "\n\n");
			
		//Scalability and Coherency orientation can be defined with another Itemizer constructor
						
		/** Stage 2: Postprocessing **/
		
		/** Define Biclusterizer for Closing **/
		//Filtering arguments
		double minFilteringValueAux = this.minFilteringValue / 100.000;
			
		//Filtering procedure	
		//BiclusterMerger.MergingStrategy default value = BiclusterMerger.MergingStrategy.Heuristic;
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(this.minOverlapMerging),
													  new BiclusterFilter(this.filter, 1.0 - minFilteringValueAux)); 
				
		/** Stage 3: Mining step **/		
		/** Define PMiner **/
		BiclusterMiner bicminer = null; 
			
		//Pattern miner (only FIM supported for now)
		if(this.patternType != PatternType.OrderPreserving){
			ClosedFIM pminer = new ClosedFIM();  
						
			pminer.setImplementation(this.closedImplementation); 
			pminer.inputMinColumns(this.minNrColumns);		
			
			if (this.stoppingCriteria == StoppingCriteria.MinBicsBeforeMerging) {
				pminer.inputMinNrBics((int) this.stoppingCriteriaValue);
			} else if (this.stoppingCriteria == StoppingCriteria.MinAreaPercentageElements) {
				pminer.inputMinArea(this.stoppingCriteriaValue);
			} else if (this.stoppingCriteria == StoppingCriteria.MinSupportPercentageRowsPerBic) {
				pminer.setSupport(this.stoppingCriteriaValue);
			}				
			
			if(this.patternType == PatternType.Additive){
				bicminer = new AdditiveBiclusterMiner(this.data,pminer,posthandler,this.orientation);
			} else if(this.patternType == PatternType.Constant){
				bicminer = new ConstantBiclusterMiner(this.data,pminer,posthandler,this.orientation); 
			} else if(this.patternType == PatternType.Symmetric){
				bicminer = new SymmetricBiclusterMiner(this.data,pminer,posthandler,this.orientation); 
			} else if(this.patternType == PatternType.ConstantOverall){
				bicminer = new ConstantOverallBiclusterMiner(this.data,pminer,posthandler,this.orientation); 
			} else {
				bicminer = new MultiplicativeBiclusterMiner(this.data,pminer,posthandler,this.orientation); 
			}
		}
		
		/** Run BicPAM **/		
		long time = System.currentTimeMillis();
		Biclusters bics = new Biclusters();
		List<List<Integer>> originalIndexes = CopyUtils.copyIntList(this.data.indexes);
		List<List<Integer>> originalScores = CopyUtils.copyIntList(this.data.intscores);
		
		if(this.nrIterations > 1) {
			double removePercentage = 0.3;
			for(int i = 0; i < this.nrIterations; i++){
				System.out.println("Mining Biclusters for iteration " + i);
				Biclusters iBics = bicminer.mineBiclusters();
				this.data.remove(iBics.getElementCounts(), removePercentage);
				bicminer.setData(this.data);
				bics.addAll(iBics);
			}
		}
		else {
			System.out.println("Mining Biclusters"); 
			Biclusters iBics = bicminer.mineBiclusters();
			bics.addAll(iBics);
		}

		this.data.indexes = originalIndexes;
		this.data.intscores = originalScores;
		time = System.currentTimeMillis() - time;
		
		/** Output and Evaluation **/			
		bics.computePatterns(this.data, this.orientation);
		if(this.patternType == PatternType.OrderPreserving) {
			BSignificance.runOrderPreserving(this.data, bics);
		}
		else {
			//calculate p-value considering that each feature may have a different distribution (non-iid)
			BSignificance.runConstantFreqColumn(this.data, bics);
		}
		//order biclusters by p-value
		bics.orderPValue();
		
		toPrint.append("NUMBER OF FOUND BICS (total): " + bics.size() + "\n");		
		
		//remove biclusters with p-values above the significance level
		bics = removeNonStatSignificant(bics, this.significanceLevel);
		this.bics = bics;
		
		System.out.println("Time:" +((double)time/(double)1000) + "ms");
		
		//check for Bicluster overlaps
//		String overlapInfo = biclusterOverlaps(bics);
//		toPrint.append("\nOVERLAPPING BICLUSTERS:\n");
//		toPrint.append(overlapInfo + "\n");
//		System.out.println(overlapInfo);
//		
		//print individual bicluster info 
		toPrint.append("NUMBER OF FOUND BICS (significant): " + bics.size());		
		toPrint.append("\nINDIVIDUAL BICLUSTERS:\n");
				
		int bicNumber = 1;
		for(Bicluster bic : bics.getBiclusters()) {
			toPrint.append("\nBICLUSTER #" + bicNumber + ":\n");
			toPrint.append("p-value = " + bic.pvalue + "\n");	
			toPrint.append("area = " + bic.area() + "\n\n");	
			
			if(this.printPatternsOnly) {
				//print columns and row pattern
				if(this.patternType == PatternType.Constant) {	
					toPrint.append("pattern: \n");
					for(int idx : bic.columns) {
						if(this.scalability) {
							toPrint.append(this.data.originalColumns.get(idx) + "\t");
						}
						else {
							toPrint.append(this.data.columns.get(idx) + "\t");
						}
					} 
					toPrint.append("\n");	
					
					int[] matrixFirstLine = this.data.getBicluster(bic.columns,bic.rows)[0]; 
					for(int j = 0; j < matrixFirstLine.length; j++) {
						toPrint.append(String.valueOf(matrixFirstLine[j]) + "\t");
					}
					toPrint.append("\n");			
				}
				//failsafe in the case PatternType is not Constant
				else {
					toPrint.append(bic.toString(this.data));
				}			
			}
			else {
				toPrint.append(bic.toString(this.data));
			}
	
			//save Bicluster chart and heatmap on experimentSetFileFolder
			//saveBicGraphAndHeatMap(this.experimentId, bicNumber, bic, this.data, this.orientation, this.experimentSetFileFolder);
			//update bicNumber
			bicNumber++;
		}
				
		//output to a concrete location
		String completeFilepath = this.experimentSetFileFolder + "/" + this.outputFilename;
		Utils.writeFile(completeFilepath, toPrint.toString());
		System.out.println("Results for " + this.experimentId + " -> " + completeFilepath);
		
		return this.bics;
	}

	//getters 
	public String getExperimentId() {
		return experimentId;
	}
	
	public Dataset getData() {
		return data;
	}
	
	public String getFilePath() {
		return inputFilePath;
	}

	public StoppingCriteria getStoppingCriteria() {
		return stoppingCriteria;
	}

	public double getStoppingCriteriaValue() {
		return stoppingCriteriaValue;
	}

	public int getMinNrColumns() {
		return minNrColumns;
	}

	public int getNrIterations() {
		return nrIterations;
	}

	public int getNrItems() {
		return nrItems;
	}

	public boolean isSymmetric() {
		return symmetric;
	}

	public double getMinOverlapMerging() {
		return minOverlapMerging;
	}

	public FilteringCriteria getFilter() {
		return filter;
	}

	public FillingCriteria getMissingsHandler() {
		return missingsHandler;
	}

	public RemoveCriteria getRemoveElements() {
		return removeElements;
	}

	public double getMinFilteringValue() {
		return minFilteringValue;
	}

	public PatternType getPatternType() {
		return patternType;
	}

	public ClosedImplementation getClosedImplementation() {
		return closedImplementation;
	}

	public Orientation getOrientation() {
		return orientation;
	}
	
	public double getSignificanceLevel() {
		return significanceLevel;
	}
	
	public boolean getScalability() {
		return scalability;
	}
	
	public double getColumnFilteringPerc() {
		return columnFilteringPerc;
	}

	public boolean isDiscriminative() {
		return discriminative;
	}

	public String getTargetClassName() {
		return targetClassName;
	}
	
	public boolean getPrintPatternsOnly() {
		return printPatternsOnly;
	}

	public String getExperimentSetFileFolder() {
		return experimentSetFileFolder;
	}

	public String getOutputFilename() {
		return outputFilename;
	}

	public Biclusters getBics() {
		return bics;
	}

	//setters
	public void setExperimentId(String experimentId) {
		this.experimentId = experimentId;
	}

	public void setData(Dataset data) {
		this.data = data;
	}
	
	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	public void setStoppingCriteria(StoppingCriteria stoppingCriteria) {
		this.stoppingCriteria = stoppingCriteria;
	}

	public void setStoppingCriteriaValue(double stoppingCriteriaValue) {
		this.stoppingCriteriaValue = stoppingCriteriaValue;
	}

	public void setMinNrColumns(int minNrColumns) {
		this.minNrColumns = minNrColumns;
	}

	public void setNrIterations(int nrIterations) {
		this.nrIterations = nrIterations;
	}

	public void setNrItems(int nrItems) {
		this.nrItems = nrItems;
	}

	public void setSymmetric(boolean symmetric) {
		this.symmetric = symmetric;
	}

	public void setMinOverlapMerging(double minOverlapMerging) {
		this.minOverlapMerging = minOverlapMerging;
	}

	public void setFilter(FilteringCriteria filter) {
		this.filter = filter;
	}

	public void setMissingsHandler(FillingCriteria missingsHandler) {
		this.missingsHandler = missingsHandler;
	}

	public void setRemoveMissings(RemoveCriteria removeElements) {
		this.removeElements = removeElements;
	}

	public void setMinFilteringValue(double minFilteringValue) {
		this.minFilteringValue = minFilteringValue;
	}

	public void setPatternType(PatternType patternType) {
		this.patternType = patternType;
	}

	public void setClosedImplementation(ClosedImplementation closedImplementation) {
		this.closedImplementation = closedImplementation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}
	
	public void setSignificanceLevel(double significanceLevel) {
		this.significanceLevel = significanceLevel;
	}
	
	public void setScalability(boolean scalability) {
		this.scalability = scalability;
	}
	
	public void setColumnFilteringPerc(double columnFilteringPerc) {
		this.columnFilteringPerc = columnFilteringPerc;
	}

	public void setDiscriminative(boolean discriminative) {
		this.discriminative = discriminative;
	}

	public void setTargetClassName(String targetClassName) {
		this.targetClassName = targetClassName;
	}

	public void setPrintPatternsOnly(boolean printPatternsOnly) {
		this.printPatternsOnly = printPatternsOnly;
	}
	
	public void setExperimentSetFileFolder(String experimentSetFileFolder) {
		this.experimentSetFileFolder = experimentSetFileFolder;
	}

	public void setOutputFilename(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	//Utility methods
	
	//toString
	public String toString() {
				
		StringBuffer result = new StringBuffer();	
		
		result.append("Experiment ID: " + this.experimentId + "\n");
		result.append("\tInput File Path: " + this.inputFilePath + "\n");
		result.append("\tStopping Criteria: " + this.stoppingCriteria + "\n");
		result.append("\tStopping Criteria Value: " + this.stoppingCriteriaValue + "\n");	
		result.append("\tMin Nr Columns: " + this.minNrColumns + "\n");	
		result.append("\tNr Iterations: " + this.nrIterations + "\n");
		result.append("\tNr Items: " + this.nrItems + "\n");
		result.append("\tSymmetric: " + this.symmetric + "\n");	
		result.append("\tMin Overlap Merging: " + this.minOverlapMerging + "\n");	
		result.append("\tFilter: " + this.filter + "\n");
		result.append("\tMissings Handler: " + this.missingsHandler + "\n");
		result.append("\tRemove Elements: " + this.removeElements + "\n");	
		result.append("\tMin Filtering Value: " + this.minFilteringValue + "\n");	
		result.append("\tPattern Type: " + this.patternType + "\n");
		result.append("\tClosed Implementation: " + this.closedImplementation + "\n");
		result.append("\tOrientation: " + this.orientation + "\n");		
		result.append("\tSignificance Level: " + this.significanceLevel + "\n");	
		result.append("\tScalability: " + this.scalability + "\n");
		result.append("\tColumn Filtering %: " + (this.columnFilteringPerc * 100) + "\n");
		result.append("\tDiscriminative: " + this.discriminative + "\n");		
		result.append("\tTarget Class Name: " + this.targetClassName + "\n");
		result.append("\tPrint patterns only: " + this.printPatternsOnly + "\n");
		result.append("\tExperiment Set File Folder: " + this.experimentSetFileFolder + "\n");	
		result.append("\tOutput Filename: " + this.outputFilename + "\n");
		
		return result.toString();
	}
	
	//toString method to insert info on XLSX XSSFRow
	public List<Pair<String,String>> getExperimentInfo() {
		
		List<Pair<String,String>> result = new LinkedList<Pair<String,String>>();	
		
		result.add(new Pair<String, String>("Stopping Criteria:", "" + this.stoppingCriteria));
		result.add(new Pair<String, String>("Stopping Criteria Value:", "" + this.stoppingCriteriaValue));	
		result.add(new Pair<String, String>("Min Nr Columns:", "" + this.minNrColumns));	
		result.add(new Pair<String, String>("Nr Iterations:", "" + this.nrIterations));
		result.add(new Pair<String, String>("Nr Items:", "" + this.nrItems));
		result.add(new Pair<String, String>("Symmetric:", "" + this.symmetric));	
		result.add(new Pair<String, String>("Min Overlap Merging:", "" + this.minOverlapMerging));	
		result.add(new Pair<String, String>("Filter:", "" + this.filter));
		result.add(new Pair<String, String>("Missings Handler:", "" + this.missingsHandler));
		result.add(new Pair<String, String>("Remove Elements:", "" + this.removeElements));	
		result.add(new Pair<String, String>("Min Filtering Value:", "" + this.minFilteringValue));	
		result.add(new Pair<String, String>("Pattern Type:", "" + this.patternType));
		result.add(new Pair<String, String>("Closed Implementation:", "" + this.closedImplementation));
		result.add(new Pair<String, String>("Orientation:", "" + this.orientation));		
		result.add(new Pair<String, String>("Significance Level:", "" + this.significanceLevel));	
		result.add(new Pair<String, String>("Scalability:", "" + this.scalability));
		result.add(new Pair<String, String>("Column Filtering %: ", "" + (this.columnFilteringPerc * 100)));
		result.add(new Pair<String, String>("Discriminative:", "" + this.discriminative));		
		result.add(new Pair<String, String>("Target Class Name:", this.targetClassName));		
		
		return result;
	}
	
	private void saveBicGraphAndHeatMap(String experimentId, int bicNumber, Bicluster bic, Dataset data, 
										Orientation orientation, String experimentSetFileFolder) {
		//create Bicluster graph
		ChartPanel graph = ChartPlotter.getGraph(bic, data, orientation);
		//set filename
		String graphFilename = experimentSetFileFolder + "/" + experimentId + "_graph_bicluster_" + bicNumber + ".png";
		//save graphic
		try {
			OutputStream out = new FileOutputStream(graphFilename);
			ChartUtilities.writeChartAsPNG(out, graph.getChart(), 600, 400);
		} catch (IOException ex) {
			System.out.println("ERROR: Chart for Bicluster " + bicNumber + " could not be saved -> " + ex.getMessage());
		}

		/*//create Bicluster heatmap
		ChartPanel heatmap = ChartPlotter.getHeatMap(bic, data, orientation);
		//set filename
		String heatmapFilename = experimentSetFileFolder + "/" + experimentId + "_heatmap_bicluster_" + bicNumber + ".png";
		//save graphic
		try {
			OutputStream out = new FileOutputStream(heatmapFilename);
			ChartUtilities.writeChartAsPNG(out, heatmap.getChart(), 600, 400);
		} catch (IOException ex) {
			System.out.println("ERROR: Heatmap for Bicluster " + bicNumber + " could not be saved -> " + ex.getMessage());
		}*/
	}

	private Biclusters removeNonStatSignificant(Biclusters bics, double significanceLevel) {
		//remove biclusters with p-value larger than a given significance level 
		//(not statistically relevant)
		List<Bicluster> bicList = bics.getBiclusters();
		for (Iterator<Bicluster> iterator = bicList.iterator(); iterator.hasNext();) {
			Bicluster bicluster = (Bicluster) iterator.next();			
			if(bicluster.pvalue >= significanceLevel) {
				//removes current bicluster (to avoid concurrent modifications)
				iterator.remove(); 
			}
		}

		Biclusters significantBiclusters = new Biclusters();
		for (Bicluster bicluster : bicList) {		
			significantBiclusters.add(bicluster);
		}	

		return significantBiclusters;
	}
	
	private String biclusterOverlaps(Biclusters bics) {
		
		StringBuffer result = new StringBuffer();	
		
		for(int i = 0; i < bics.size(); i++) {
			for(int j = 0; j < bics.size(); j++) {
				if(j > i) {
					double overlap = bics.get(j).overlapArea(bics.get(i));
					if(overlap > 0) {
						result.append("Bic #" + (i + 1) + " + Bic #" + (j + 1) + " overlap: " + overlap + "\n");
					}
				} 
			}
		}

		return result.toString();
	}
}
