package com.vizualize.quandl;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

public class CSVLatestTicksInterface {
    
    static SparkSession spark = null;
    
    public static JavaRDD<String> fetchFromDate(String name, String count, SparkContext context) {
        if (spark == null)
            spark = new SparkSession(context);
        List<String> rows = getLatestTicks(name);
        Encoder<String> stringEncoder = Encoders.STRING();
        Dataset<String> dataSet = spark.createDataset(rows, stringEncoder);
        return dataSet.toJavaRDD(); 
    }
    
    private static List<String> getLatestTicks(String instrument) {
        List<String> rows = new ArrayList<>();

        try {
            HttpGet request = new HttpGet();
            String urlString1 = "http://localhost:9000/tickscsv";
            URIBuilder builder = new URIBuilder(urlString1);
            builder.addParameter("stock", instrument);
            builder.addParameter("count", "51");
            request.setURI(builder.build());
            CloseableHttpClient client = HttpClientBuilder.create().build();

            CloseableHttpResponse httpResponse = client.execute(request);
            if(httpResponse.getEntity() != null) {
                String jsonContent = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                BufferedReader in = new BufferedReader(new StringReader(jsonContent));
                String inputLine;

                while ((inputLine = in.readLine()) != null)
                    rows.add(inputLine);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        rows.remove(rows.size()-1);
        return rows;
    }
}
