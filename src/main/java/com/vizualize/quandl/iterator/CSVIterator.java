package com.vizualize.quandl.iterator;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.analysis.DataAnalysis;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.CategoricalColumnCondition;
import org.datavec.api.transform.condition.column.DoubleColumnCondition;
import org.datavec.api.transform.filter.ConditionFilter;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.transform.normalize.Normalize;
import org.datavec.api.writable.comparator.TextWritableComparator;
import org.threeten.bp.LocalDate;

import com.vizualize.quandl.CSVInterface;

public class CSVIterator extends QuandlIterator {
    
	protected Schema getSchema() {
		return new Schema.Builder()
                .addColumnString("Date")
                .addColumnsDouble("Low","Close", "High")
                .build();
	}
	
	protected Schema getSchemaAfterTransform() {
		return new Schema.Builder()
                .addColumnString("Date")
                .addColumnsDouble("Close")
                .build();
	}
	
    protected TransformProcess getTransform() {
        Schema inputDataSchema = getSchema();
        
        TransformProcess tp = new TransformProcess.Builder(inputDataSchema)
                .filter(new ConditionFilter(
                        new CategoricalColumnCondition("High", ConditionOp.InSet, new HashSet((Arrays.asList("High", ""))))))
                .filter(new ConditionFilter(new DoubleColumnCondition("High", ConditionOp.Equal, 0)))
                .filter(new ConditionFilter(new DoubleColumnCondition("Low", ConditionOp.Equal, 0)))
                .filter(new ConditionFilter(new DoubleColumnCondition("Close", ConditionOp.Equal, 0)))
                .calculateSortedRank("testSort", "Date", new TextWritableComparator())
//                .removeColumns("Date")
//                .removeColumns("testSort")
                .removeColumns("testSort", "High", "Low")
                .build();
        
        return tp;
    }
    
    @Override
    protected JavaRDD<String> fetchFromDate(String name, String startDate, String endDate, SparkContext context) {
        return CSVInterface.fetchFromDate(name, startDate, context);
    }

}
