package com.vizualize.quandl.iterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.datavec.api.split.FileSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.CategoricalColumnCondition;
import org.datavec.api.transform.condition.column.DoubleColumnCondition;
import org.datavec.api.transform.filter.ConditionFilter;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.comparator.TextWritableComparator;
import org.datavec.spark.transform.SparkTransformExecutor;
import org.datavec.spark.transform.misc.StringToWritablesFunction;
import org.datavec.spark.transform.misc.WritablesToStringFunction;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.threeten.bp.LocalDate;

import com.vizualize.quandl.QuandlInterface;
import com.vizualize.reader.CSVNLineOverlappingSequenceReader;

public class QuandlIterator {
	File tempFile = null;
	public Iterator<DataSet> getIterator(String symbol, SparkContext sc) {
		Schema inputDataSchema = new Schema.Builder()
    			.addColumnString("Trade Date")
    			.addColumnsDouble("Index Value","High", "Low", "Total Market Value", "Dividend Market Value")
    			.build();

    	TransformProcess tp = new TransformProcess.Builder(inputDataSchema)
    			//TO DO filter out zero values in columns for index value, high and low
    			.filter(new ConditionFilter(
    					new CategoricalColumnCondition("High", ConditionOp.InSet, new HashSet((Arrays.asList("High", ""))))))
    			.filter(new ConditionFilter(new DoubleColumnCondition("High", ConditionOp.Equal, 0)))
    			.calculateSortedRank("testSort", "Trade Date", new TextWritableComparator())
    			.removeColumns("Trade Date", "Total Market Value", "testSort")
    			.build();
    	CSVNLineOverlappingSequenceReader reader = new CSVNLineOverlappingSequenceReader(20);
    	LocalDate lastDate = LocalDate.parse("1017-11-09");
    	JavaRDD<String> fetchedData = QuandlInterface.fetchFromDate(symbol, lastDate, sc);
    	JavaRDD<List<Writable>> parsedInputData1 = fetchedData.map(new StringToWritablesFunction(reader));

    	//Now, let's execute the transforms we defined earlier:
        JavaRDD<List<Writable>> processedData1 = SparkTransformExecutor.execute(parsedInputData1, tp);

        //For the sake of this example, let's collect the data locally and print it:
        JavaRDD<String> processedAsString1 = processedData1.map(new WritablesToStringFunction(","));

        List<String> processedCollected1 = processedAsString1.collect();
        String[] lines = processedCollected1.toArray(new String[processedCollected1.size()]);
        
    	try {
    		tempFile = File.createTempFile("dl4j", ".tmp");
            String tempFilePath = tempFile.getAbsolutePath(); 
            writeToCsv(tempFilePath, lines);
    		File file = new File(tempFilePath);
			reader.initialize(new FileSplit(file));

			return new SequenceRecordReaderDataSetIterator(reader, 1, 1, -1, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void deleteTmpFile() {
		if (tempFile != null)
			tempFile.delete();
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
}
