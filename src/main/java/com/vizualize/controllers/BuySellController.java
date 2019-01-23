package com.vizualize.controllers;

import java.time.LocalDateTime;

import org.apache.spark.api.java.JavaSparkContext;
import org.datavec.api.transform.Transform;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.AbstractDataSetNormalizer;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.plot.Plot;
import com.vizualize.quandl.iterator.CSVLatestTicksIterator;
import com.vizualize.quandl.iterator.CSVRestIterator;
import com.vizualize.reader.DataSetNormalizer;
import com.vizualize.train.service.TrainService;

import spark.Route;

public class BuySellController {

    static int closeIndex = 1;
    static final int periodLength = 50;
    
    static MultiLayerNetwork pretrainNet = null;
    
    static String buyStr = "\"buy\"";
    static String sellStr = "\"sell\"";

    public static Route getLastOrder = (req, res) -> {
        ObjectMapper mapper = new ObjectMapper();
        String instrument = req.queryParams("instrument");
        boolean buy = predictBuyOrSell(instrument, 10);        
        
        String content = "\"orderType\":";    
        if (buy)
            content += buyStr;
        else
            content += sellStr;
        
        content = "{" + content + "}";
        JsonNode result = mapper.readTree(content);
        return result;
    };  
        
    public static boolean predictBuyOrSell(String instrument, int loopForward) throws Exception {
    	String modelName = instrument + "_" + loopForward;
    	CSVLatestTicksIterator csvi = new CSVLatestTicksIterator(); 
    	csvi.doNormalize = false;
    	if (pretrainNet == null) {
    		pretrainNet = NetworkSerializer.loadNetwork(modelName);
    		
    	}
//    	Transform tf = (Transform) NetworkSerializer.loadtransform(modelName);
//		csvi.setCloseTransform(tf);
//    	AbstractDataSetNormalizer dsNormalizer = (AbstractDataSetNormalizer) NetworkSerializer.loadtransform(modelName);
        
    	JavaSparkContext sc = TrainService.getSc();
        DataSetIterator iterator = csvi.getIterator(instrument, sc.toSparkContext(sc), periodLength, 0, false, closeIndex, false, "50", null);
       
        int size = iterator.numExamples() -1;
        if (size > 0)
            iterator.next(size);
        DataSet ds = iterator.next();        
        INDArray features = ds.getFeatures();
        INDArray lables = ds.getLabels();
        
        int featureSize = features.size(1);
//        INDArray nFeatures = Nd4j.ones(1,1,featureSize);
//        nFeatures.get(NDArrayIndex.createCoveringShape(nFeatures.shape())).assign(features);
//        dsNormalizer.transform(nFeatures);
//        dsNormalizer.transform(lables);
		INDArray labelsInd = lables.getRow(0);
//		INDArray featuresInd = nFeatures.getRow(0);
		INDArray featuresInd = features.getRow(0);
        INDArray predictedLabels = pretrainNet.output(featuresInd,false);
        
        
		INDArray predictedInd = predictedLabels.getRow(0);

            
        double label = lables.getDouble(0);
        double predicted = predictedLabels.getDouble(0);
        boolean buy = true;
        if (label - predicted > 0)
            buy = false;
        String title = buy ? "Buy" : "Sell";
        LocalDateTime ldt = LocalDateTime.now();
        title += ": " + ldt.toString();
		Plot.plot(featuresInd, labelsInd, predictedInd, title);    	    	
    	return buy;
    }
    
    public static void evaluateBuySellPrediction(String instrument, int loopForward) throws Exception {
    	String modelName = instrument + "_" + loopForward;
    	CSVRestIterator csvi = new CSVRestIterator(); 
    	if (pretrainNet == null) {
    		pretrainNet = NetworkSerializer.loadNetwork(modelName);
    		Transform tf = (Transform) NetworkSerializer.loadtransform(modelName);

    		csvi.setCloseTransform(tf);
    	}
        String startDate = "2018-11-02 12:00:00";
        JavaSparkContext sc = TrainService.getSc();
        DataSetIterator evalIter = csvi.getIterator(instrument, sc.toSparkContext(sc), periodLength, 0, false, closeIndex, false, startDate, null);

        while (evalIter.hasNext()) {
        	DataSet ds = evalIter.next(); 
        	INDArray features = ds.getFeatures();
        	INDArray lables = ds.getLabels();
        	INDArray predictedLabels = pretrainNet.output(features,false);

        	INDArray featuresInd = features.getRow(0); 
        	INDArray labelsInd = lables.getRow(0);
        	INDArray predictedInd = predictedLabels.getRow(0);

        	double label = lables.getDouble(0);
        	double predicted = predictedLabels.getDouble(0);
        	boolean buy = true;
        	if (label - predicted > 0)
        		buy = false;
        	String title = buy ? "Buy" : "Sell";
        	LocalDateTime ldt = LocalDateTime.now();
        	title += ": " + ldt.toString();
        	Plot.plot(featuresInd, labelsInd, predictedInd, title);
        }
    }
}