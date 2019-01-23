package com.vizualize.quandl.iterator;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;

import com.vizualize.quandl.CSVLatestTicksInterface;

public class CSVLatestTicksIterator extends CSVIterator {

    @Override
    protected JavaRDD<String> fetchFromDate(String name, String count, String endDate, SparkContext context) {
        return CSVLatestTicksInterface.fetchFromDate(name, count, context);
    }
}
