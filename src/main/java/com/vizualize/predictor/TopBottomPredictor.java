package com.vizualize.predictor;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.apache.spark.api.java.JavaSparkContext;
import org.codehaus.janino.Java.EnumConstant;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.plot.Plot;
import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.CSVLatestTicksIterator;
import com.vizualize.quandl.iterator.IteratorContainer;
import com.vizualize.quandl.iterator.TrainTestIteratorPair;
import com.vizualize.train.service.TrainService;

public class TopBottomPredictor {

	static double lastTradedPrice = 0;
	static double loseFactor = 0.5/100;
	static TradingActions lastTradeAction = null;
	static double investingReturn = 0;
	static int buys =0, sells = 0;
	static boolean evaluate = false;
	
	enum TopBottom {
		TOP, BOTTOM, CONTINUATION;
	}
	
	public static enum TradingActions {
		BUY, SELL, KEEP;
	}
	
	static int closeIndex = 1;
	
	public static TradingActions predictBuyOrSell(String instrumentName) {
		evaluate = false;
    	CSVLatestTicksIterator csvi = new CSVLatestTicksIterator(); 
    	csvi.doNormalize = false;
    	JavaSparkContext sc = TrainService.getSc();
        DataSetIterator iterator = csvi.getIterator(instrumentName, sc.toSparkContext(sc), 50, 0, false, closeIndex, false, "50", null);
       
        int size = iterator.numExamples() -1;
        if (size > 0)
            iterator.next(size);
        DataSet ds = iterator.next();        
        return predictBuy(ds);
	}
	
	public static double evaluateStrategy(String instrumentName) {
		evaluate = true;
		CSVIterator csvi = new CSVIterator();
		csvi.doNormalize = false;
		
        JavaSparkContext sc = TrainService.getSc();
        DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), 50, 1, false, closeIndex, false, null, null);
        ArrayList<DataSetIterator> preTrainDataIterators = new ArrayList<>();
        preTrainDataIterators.add(iter);
        
        IteratorContainer preTrainContainer = new IteratorContainer(preTrainDataIterators, 1);
        ArrayList<TrainTestIteratorPair> pretrainTestSplits = preTrainContainer.getTrainTestList();
        
        for (TrainTestIteratorPair trainTestIteratorPair : pretrainTestSplits) {      
        	DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();        	
        	while(trainSetIterator.hasNext()){
        		DataSet t = trainSetIterator.next(1);
        		predictBuy(t);
            	
        		boolean waitit = true;
        	}
        }
		return investingReturn;
	}
	
	
	public static TradingActions predictBuy(DataSet dataSet) {
		INDArray features = dataSet.getFeatures();
		INDArray lables = dataSet.getLabels();
		INDArray featuresInd = features.getRow(0); 
    	INDArray labelsInd = lables.getRow(0);            	
//    	TradingActions action = predictSeriesBuy(features);
    	TradingActions action = predictSeriesBuyMA(featuresInd);
    	
    	if(!action.equals(TradingActions.KEEP) && !action.equals(lastTradeAction)) {
    		LocalDateTime time = LocalDateTime.now();
//    		Plot.plot(featuresInd, labelsInd, null, time.toString()+ " : " +action);
    		double lastPrice = features.getDouble(features.size(1)-1);
    		if (evaluate) {
    			if (lastTradedPrice != 0) {
    				if (lastTradeAction.equals(TradingActions.BUY)) {
    					//    				Plot.plot(featuresInd, labelsInd, null, "");
    					investingReturn += (lastPrice - lastTradedPrice) * (1 - loseFactor) ;
    					buys++;
    					boolean waitit = true;
    				} else if (lastTradeAction.equals(TradingActions.SELL)) {
    					//    				Plot.plot(featuresInd, labelsInd, null, "");
    					investingReturn += (lastTradedPrice - lastPrice) * (1 - loseFactor) ;
    					sells++;
    					boolean waitit = true;
    				}
    			}
    		}
    		lastTradedPrice = lastPrice;
    		lastTradeAction = action;
    		return action;
    	}
    	return TradingActions.KEEP;
	}
	
	public static TradingActions predictSeriesBuy(INDArray features) {
		int size = features.size(1);
		TopBottom last = null, previous = null;
		double lastTBPrice = features.getDouble(0);
		double previousTBPrice = features.getDouble(0);
		for (int i = 0;i<size - 2;i++) {
			double first = features.getDouble(i);
			double second = features.getDouble(i + 1);
			double third = features.getDouble(i + 2);
//			double fourth = features.getDouble(i + 3);
//			double fifth = features.getDouble(i + 4);
//			double lastP = third;
//			TopBottom computed = computeTopBottom(first, second, third, fourth, fifth);
			double lastP = second;
			TopBottom computed = computeTopBottom(first, second, third);

			if (computed != TopBottom.CONTINUATION) {								
				if (!computed.equals(last)) {
					previous = last;				
					last = computed;
					previousTBPrice = lastTBPrice;
					lastTBPrice = lastP;
				} else if (computed.equals(TopBottom.BOTTOM)) {
					if(lastTBPrice > lastP)
						lastTBPrice = lastP;
				} else if (computed.equals(TopBottom.TOP)) {
					if (lastTBPrice < lastP)
						lastTBPrice = lastP;
				}
			}
			boolean waitit = true;
		}
		
		double lastPrice = features.getDouble(size-1);
		double tolerance = 0.0; // for under one hour
//		double tolerance = 1; // for equal to one day
		double minP = Double.min(lastTBPrice, previousTBPrice);
		double maxP = Double.max(lastTBPrice, previousTBPrice);
		System.out.println("Last top: " + maxP);
		System.out.println("Last bottom: " + minP);
		if (lastPrice < minP - tolerance)
			return TradingActions.SELL;	
		else if (lastPrice > maxP + tolerance)
			return TradingActions.BUY;
		
//		if (last.equals(TopBottom.BOTTOM)) {
//			if (lastPrice < previousTBPrice)
//				return TradingActions.SELL;			
//		} else if (last.equals(TopBottom.TOP)) {
//			if (lastPrice > previousTBPrice)
//				return TradingActions.BUY;
//		}
		return TradingActions.KEEP; 
	}

	public static TradingActions predictSeriesBuyMA(INDArray features) {
		Core taCore = new Core();
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double outReal[] = new double[48];
		DataBuffer dataBuffer = features.data();
		double[] inReal = dataBuffer.asDouble();

		taCore.movingAverage(0, inReal.length-1, inReal, 3, MAType.Sma, outBegIdx, outNBElement, outReal);
		double last = outReal[outReal.length-1];
		double beforeLast = outReal[outReal.length-2];
		TradingActions action = TradingActions.KEEP; 
		if (last > beforeLast)
			action = TradingActions.BUY;
		else if (last < beforeLast)
			action = TradingActions.SELL;
//		Plot.plot(outReal, 0);
		return action;
	}
	
	
	public static TopBottom computeTopBottom(double first, double second, double third, double fourth, double fifth) {
		if (first > third && second > third && fifth > third && fourth > third)
			return TopBottom.BOTTOM;
		
		if (first < third && second < third && third > fourth && third > fifth)
			return TopBottom.TOP;
		
		return TopBottom.CONTINUATION;
	}
	
	// 
	public static TopBottom computeTopBottom(double first, double second, double third) {
		double tolerance = 0.2;
		if (first > second && second < third)
			return TopBottom.BOTTOM;
		
		if (first < second && second > third)
			return TopBottom.TOP;
		
		return TopBottom.CONTINUATION;
	}
}
