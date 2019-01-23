package com.vizualize.quandl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import com.jimmoores.quandl.DataSetRequest;
import com.jimmoores.quandl.Frequency;
import com.jimmoores.quandl.HeaderDefinition;
import com.jimmoores.quandl.Row;
import com.jimmoores.quandl.TabularResult;
import com.jimmoores.quandl.classic.ClassicQuandlSession;

public class QuandlInterface {
	
	static ClassicQuandlSession session = ClassicQuandlSession.create();
	static SparkSession spark = null;
	
	public static JavaRDD<String> fetchFromDate(String name, String startDate, SparkContext context) {
		if (spark == null)
			spark = new SparkSession(context);
		org.threeten.bp.LocalDate date = org.threeten.bp.LocalDate.parse(startDate);
		TabularResult tabularResult = session.getDataSet(
				DataSetRequest.Builder
				      .of(name) 
//				      .withColumn(3) // Last (looked up previously)
				      .withStartDate(date)
				      .withFrequency(Frequency.DAILY)
				      .build());
		List<String> rows = new ArrayList<String>();		
		HeaderDefinition headerDef = tabularResult.getHeaderDefinition();
		String header = headerDef.toString();
		StringTokenizer headerTokenizer = new StringTokenizer(header, "[]");		
		header = headerTokenizer.nextToken();
		header = headerTokenizer.nextToken();
		rows.add(header);
		for (final Row row : tabularResult) {			
			int columns = row.size();
			String csvString = "";
			for (int i=0;i<columns;i++) {
				if (i > 0)
					csvString += ",";
				csvString += row.getString(i);							
			}
//			  LocalDate date = row.getLocalDate("Date");
//			  Double value = row.getDouble("Last");
//			  System.out.println("Value on date " + date + " was " + value);
			rows.add(csvString);
		}
		
		Encoder<String> stringEncoder = Encoders.STRING();
		Dataset<String> dataSet = spark.createDataset(rows, stringEncoder);
		return dataSet.toJavaRDD();		
	}	
}
