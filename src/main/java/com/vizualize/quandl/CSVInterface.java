package com.vizualize.quandl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import com.vizualize.normalizer.defaults.Defaults;

public class CSVInterface {
    
static SparkSession spark = null;
    
    public static JavaRDD<String> fetchFromDate(String name, String startDate, SparkContext context) {
        if (spark == null)
            spark = new SparkSession(context);
        
        DateTimeFormatter formatter = Defaults.dateFormatter;

        LocalDate lastDate = LocalDate.parse(Defaults.startDate, formatter);
        
        if (startDate != null) {            
            lastDate = LocalDate.parse(startDate, formatter);
        }
        
        String csvFile = name + ".csv";
        String line = "";
        List<String> rows = new ArrayList<String>();
        ClassLoader classLoader = CSVInterface.class.getClassLoader();
        InputStream input = classLoader.getResourceAsStream(csvFile.toLowerCase());      

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {

            while ((line = br.readLine()) != null) {
                rows.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Encoder<String> stringEncoder = Encoders.STRING();
        Dataset<String> dataSet = spark.createDataset(rows, stringEncoder);
        return dataSet.toJavaRDD(); 
    }

}
