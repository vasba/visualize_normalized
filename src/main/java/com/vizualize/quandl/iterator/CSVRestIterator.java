package com.vizualize.quandl.iterator;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;

import com.vizualize.quandl.CSVRestInterface;

public class CSVRestIterator extends CSVIterator {
    
    @Override
    protected JavaRDD<String> fetchFromDate(String name, String startDate, String endDate, SparkContext context) {
        return CSVRestInterface.fetchFromDate(name, startDate, endDate, context);
    }

}
