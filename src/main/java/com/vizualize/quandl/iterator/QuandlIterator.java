package com.vizualize.quandl.iterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.transform.Transform;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.analysis.DataAnalysis;
import org.datavec.api.transform.analysis.columns.ColumnAnalysis;
import org.datavec.api.transform.analysis.columns.NumericalColumnAnalysis;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.CategoricalColumnCondition;
import org.datavec.api.transform.condition.column.DoubleColumnCondition;
import org.datavec.api.transform.filter.ConditionFilter;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.transform.doubletransform.MinMaxNormalizer;
import org.datavec.api.transform.transform.doubletransform.StandardizeNormalizer;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.comparator.TextWritableComparator;
import org.datavec.spark.transform.AnalyzeSpark;
import org.datavec.spark.transform.SparkTransformExecutor;
import org.datavec.spark.transform.misc.StringToWritablesFunction;
import org.datavec.spark.transform.misc.WritablesToStringFunction;
import org.datavec.spark.transform.transform.SparkTransformFunction;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.AbstractDataSetNormalizer;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import com.vizualize.normalizer.NormalizedDataSet;
import com.vizualize.normalizer.defaults.Defaults;
import com.vizualize.quandl.QuandlInterface;
import com.vizualize.reader.CSVNLineOverlappingSequenceReader;
import com.vizualize.reader.ClassificationNormalizer;
import com.vizualize.reader.DataSetNormalizer;
import com.vizualize.reader.LabelCSVNLineOverlappingSequenceReader;

public class QuandlIterator {
	File tempFile = null;
	
	CSVNLineOverlappingSequenceReader reader1 = null;
	Transform closeTransform = null;
	AbstractDataSetNormalizer dsNormalizer = null;
	public boolean doNormalize = true;
	public boolean forLstm = false;
	
	public DataSetIterator getIterator(String symbol, SparkContext sc, 
			int periodLength, int lookForwardPeriod, boolean forPlotting, 
			int closeIndex, boolean classification, String startDate,
			String endDate) {
			    
    	CSVNLineOverlappingSequenceReader reader = new CSVNLineOverlappingSequenceReader(periodLength, lookForwardPeriod, forPlotting, closeIndex, false, forLstm);
    	    	    	
    	JavaRDD<String> fetchedData = fetchFromDate(symbol, startDate, endDate, sc);
    	JavaRDD<List<Writable>> parsedInputData1 = fetchedData.map(new StringToWritablesFunction(reader));
    	
    	TransformProcess tp = getTransform();
    	//Now, let's execute the transforms we defined earlier:
        JavaRDD<List<Writable>> processedData1 = SparkTransformExecutor.execute(parsedInputData1, tp);
//        DataAnalysis da = AnalyzeSpark.analyze(getSchemaAfterTransform(), processedData1);
//        Transform t = getCloseTransform(da, "Close");
//        Function<List<Writable>, List<Writable>> function = new SparkTransformFunction(t);
//        processedData1 = processedData1.map(function);
        JavaRDD<String> processedAsString1 = processedData1.map(new WritablesToStringFunction(","));

        List<String> processedCollected1 = processedAsString1.collect();
        String[] lines = processedCollected1.toArray(new String[processedCollected1.size()]);
        
    	try {
    		tempFile = File.createTempFile("dl4j", ".tmp");
    		tempFile.deleteOnExit();
            String tempFilePath = tempFile.getAbsolutePath(); 
            writeToCsv(tempFilePath, lines);
    		File file = new File(tempFilePath);    		
    		reader1 = new CSVNLineOverlappingSequenceReader(periodLength, lookForwardPeriod, forPlotting, closeIndex, classification, forLstm);
    		((CSVNLineOverlappingSequenceReader)reader1).setWritableType("double");
    		reader1.initialize(new FileSplit(file));

    		SequenceRecordReaderDataSetIterator iterator;
    		if (!classification) {
    		    iterator = new SequenceRecordReaderDataSetIterator(reader1, 1, 1, -1, true);
    		} else {
    			if (forLstm) {
    				CSVNLineOverlappingSequenceReader reader2 = new LabelCSVNLineOverlappingSequenceReader(periodLength, lookForwardPeriod, forPlotting, closeIndex, classification, forLstm);
    				((CSVNLineOverlappingSequenceReader)reader2).setWritableType("double");
    				reader2.initialize(new FileSplit(file));
    				iterator = new SequenceRecordReaderDataSetIterator(reader1, reader2, 1, 2,
    						false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
    			} else {
    				iterator = new SequenceRecordReaderDataSetIterator(reader1, 1, 3, 3, false);
    			}
    		}
    		
			if (!forPlotting) {
//				iterator.setPreProcessor(new DataSetNormalizer());
				if (forLstm) {
//					iterator.setPreProcessor(new DataSetNormalizer());
					return iterator;
//					return createLstmIterator(iterator, classification);
				} else {
					return createMLPIterator(iterator, classification);
				}
			}
			return iterator;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	protected Schema getSchema() {
		return new Schema.Builder()
        .addColumnString("Trade Date")
        .addColumnsDouble("Index Value","High", "Low", "Total Market Value", "Dividend Market Value")
        .build();
	}
	
	protected Schema getSchemaAfterTransform() {
		return new Schema.Builder()        
        .addColumnsDouble("Index Value","High", "Low", "Dividend Market Value")
        .build();
	}
	
	protected TransformProcess getTransform() {
	    Schema inputDataSchema = getSchema();
        
        TransformProcess tp = new TransformProcess.Builder(inputDataSchema)
                .filter(new ConditionFilter(
                        new CategoricalColumnCondition("High", ConditionOp.InSet, new HashSet((Arrays.asList("High", ""))))))
                .filter(new ConditionFilter(new DoubleColumnCondition("High", ConditionOp.Equal, 0)))
                .filter(new ConditionFilter(new DoubleColumnCondition("Low", ConditionOp.Equal, 0)))
                .filter(new ConditionFilter(new DoubleColumnCondition("Index Value", ConditionOp.Equal, 0)))
                .calculateSortedRank("testSort", "Trade Date", new TextWritableComparator())
                .removeColumns("Trade Date", "Total Market Value", "testSort")
                .build();
        
        return tp;
	}
	
	public Transform getCloseTransform(DataAnalysis da, String column) {
		
		if (closeTransform != null)
			return closeTransform;
		
		ColumnAnalysis ca = da.getColumnAnalysis(column);
		NumericalColumnAnalysis nca = (NumericalColumnAnalysis) ca;
        double min = nca.getMinDouble();
        double max = nca.getMaxDouble();
        double mean = nca.getMean();
        double sigma = nca.getSampleStdev();
        StandardizeNormalizer normalizer = new StandardizeNormalizer(column, mean, sigma);
        normalizer.setInputSchema(da.getSchema());
        closeTransform = normalizer;
        return normalizer;
	}
	
	public Transform getCloseTransform() {
		return closeTransform;
	}
	
	public void setCloseTransform(Transform nt) {
		closeTransform = nt;
	}
	
	public AbstractDataSetNormalizer getDSNormalizer() {
		return dsNormalizer;
	}
	
	public void setDSNormalizer(AbstractDataSetNormalizer dsNormalizer) {
		this.dsNormalizer = dsNormalizer;
	}
	
	protected JavaRDD<String> fetchFromDate(String name, String startDate, String endDate, SparkContext context) {
	    return QuandlInterface.fetchFromDate(name, startDate, context);
	}
	
	private DataSetIterator createLstmIterator(SequenceRecordReaderDataSetIterator sequenceIterator,
	        boolean classification) {
		
		while (sequenceIterator.hasNext()) {
			DataSet ds = sequenceIterator.next();
		}
		return null;
	}
	
	private DataSetIterator createMLPIterator(SequenceRecordReaderDataSetIterator sequenceIterator,
	        boolean classification) {
		final List<DataSet> list = new ArrayList<>();
		INDArray allFeatures = null;
		INDArray allLabels = null;
		
		DataSetNormalizer normalizer;
//		if (classification) {
//			normalizer = new ClassificationNormalizer();                
//		} else {
			normalizer = new DataSetNormalizer();         
//		}              		

		double stdAverage = 0;
		double minStd = Double.MAX_VALUE;
		double maxStd = Double.MIN_VALUE;
		double meanAverage = 0;
		double minMean = Double.MAX_VALUE;
		double maxMean = Double.MIN_VALUE;
		int index = 1;
		while (sequenceIterator.hasNext()) {
			DataSet ds = sequenceIterator.next();
			
			INDArray features = ds.getFeatures().getRow(0);
            INDArray labels = ds.getLabels();
            // just for debugging purposes to compare normalized vs not normalized values
            INDArray featuresDuplicate = features.dup(); 
            INDArray labelsDuplicate = labels.dup();
            
            if (doNormalize) {
            	normalizer.preProcess(ds); 
//            	setDSNormalizer(normalizer.getNormalizer());
//            	// calculate statistics for standardization
//            	double std = ((NormalizerStandardize) normalizer.getNormalizer()).getStd().getDouble(0);
//            	stdAverage = stdAverage * (index -1)/index + std/index;
//            	
//            	if (std > maxStd)
//            		maxStd = std;
//            	if (std < minStd)
//            		minStd = std;
//            	double mean = ((NormalizerStandardize) normalizer.getNormalizer()).getMean().getDouble(0);
//            	meanAverage = meanAverage * (index -1)/index + mean/index;
//            	
//            	if (mean > maxMean)
//            		maxMean = mean;
//            	if (mean < minMean)
//            		minMean = mean;
            }
            
			int featureSize = features.size(0);
			INDArray nFeatures = Nd4j.create(1,featureSize);
			nFeatures.get(NDArrayIndex.createCoveringShape(nFeatures.shape())).assign(features);
			
			int labelSize = labels.size(1);
			INDArray nLabels = Nd4j.create(1,labelSize);
			nLabels.get(NDArrayIndex.createCoveringShape(nLabels.shape())).assign(labels);
			
//			DataSet nds = new DataSet(nFeatures, nLabels);
			DataSet nds = new NormalizedDataSet(nFeatures, nLabels, normalizer.getNormalizer());
//			nds.normalize();
//			DataSet subDs = ds.get(0); 
			list.add(nds);
			index++;			
		}
		
//		Collections.shuffle(list,new Random(345));		
		CustomizedListDatasetIterator iterator = new CustomizedListDatasetIterator(list, 1);
//		iterator.setPreProcessor(normalizer);
		return iterator;
	}
		
	public static void writeToCsv(String path, String[] content) {
		java.io.File destinationCSV = new java.io.File(path);
		try {
			java.io.PrintWriter outfile = new java.io.PrintWriter(destinationCSV);
			int size = content.length;
			int index = 0;
			for (String line : content) {
				if (index != size){
					outfile.write(line + "\n");
					index++;
				}
			}
			outfile.flush();
			outfile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getLastIteratedDate() {
	    if (reader1 != null)
	        return reader1.getDateStr();
	    return Defaults.startDate;
	}
}
