package com.vizualize.train.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.vizualize.controllers.BuySellController;
import com.vizualize.predictor.TopBottomPredictor;
import com.vizualize.predictor.TopBottomPredictor.TradingActions;

public class PeriodicTrainer  extends Thread {
    
    @Override
    public void run()
    {
        while(true) {            
            try {
            	String instrument = "OMXS30_5M"; 
//            	String instrument = "US500_5M";
            	int loopForward = 30;
            	
//            	double gain = TopBottomPredictor.evaluateStrategy(instrument);
            	
//            	LocalDateTime beforeTime = LocalDateTime.now();
//            	TradingActions action = TopBottomPredictor.predictBuyOrSell(instrument);
//            	System.out.println(action);
//            	if (action.equals(TradingActions.BUY))
//            		postOrder(true, instrument);
//            	else if (action.equals(TradingActions.SELL))
//            		postOrder(false, instrument);
//            	LocalDateTime afterTime = LocalDateTime.now();
//            	long duration = beforeTime.until(afterTime, ChronoUnit.MILLIS);  
//            	Thread.sleep(60000 - duration);
//            	
                TrainService.train(instrument, loopForward);
//                int wait = 0;
//                BuySellController.evaluateBuySellPrediction(instrument, loopForward);
//                boolean buy = BuySellController.predictBuyOrSell(instrument, loopForward);
//                System.out.println("buy is: " + buy);
//                postOrder(buy, instrument);
                
//                Thread.sleep(5000);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
        
    public void postOrder(boolean buy, String instrument) {
    	 String url = "http://localhost:9010";
    	 if (buy)
    		 url += "/buy";
    	 else
    		 url += "/sell";
    	 
    	 String shortName = getUnderlayingShortName(instrument);
         String urlParameters = "underlying=" + shortName;
         byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

         HttpURLConnection con = null;
         
         try {

             URL myurl = new URL(url);
             con = (HttpURLConnection) myurl.openConnection();

             con.setDoOutput(true);
             con.setRequestMethod("POST");
//             con.setRequestProperty("User-Agent", "Java client");
             con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

             try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                 wr.write(postData);
             }

             StringBuilder content;

             try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(con.getInputStream()))) {

                 String line;
                 content = new StringBuilder();

                 while ((line = in.readLine()) != null) {
                     content.append(line);
                     content.append(System.lineSeparator());
                 }
             }

             System.out.println(content.toString());

         } catch(Exception e) {
        	 e.printStackTrace();
         } finally {
         
             
             con.disconnect();
         }
    }
    
    public String getUnderlayingShortName(String underlying) {
    	switch (underlying) {
		case "OMXS30_M5":
			return "OMX";			

		default:
			return "";			
		}
    	
    }

}
