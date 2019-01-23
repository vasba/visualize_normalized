package com.vizualize.quandl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import com.vizualize.normalizer.defaults.Defaults;

public class CSVRestInterface {
    
static SparkSession spark = null;
    
    public static JavaRDD<String> fetchFromDate(String name, String startDate, String endDate, SparkContext context) {
        if (spark == null)
            spark = new SparkSession(context);
        
        if (startDate == null) {
            startDate = Defaults.startDate;
        }
        
        List<String> rows = new ArrayList<String>();

        try {
            rows = callRest(name, startDate, endDate);            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Encoder<String> stringEncoder = Encoders.STRING();
        Dataset<String> dataSet = spark.createDataset(rows, stringEncoder);
        return dataSet.toJavaRDD(); 
    }
    
    private static List<String> callRest(String name, String startDate, String endDate) throws Exception {
        HttpGet request = new HttpGet();
        String urlString1 = "http://localhost:9000/tickscsvdate";
        URIBuilder builder = new URIBuilder(urlString1);
        builder.addParameter("stock", name);
        builder.addParameter("startDate", startDate);
        request.setURI(builder.build());
        CloseableHttpClient client = HttpClientBuilder.create().build();
        List<String> rows = new ArrayList<String>();
        try(CloseableHttpResponse httpResponse = client.execute(request)) {
            if(httpResponse.getEntity() != null) {
                String jsonContent = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                BufferedReader in = new BufferedReader(new StringReader(jsonContent));
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) {
                	if (endDate != null && inputLine.contains(endDate))
                		break;
                	rows.add(inputLine);
                }
                    
            }
        }
        
        return rows;       
    }

}
