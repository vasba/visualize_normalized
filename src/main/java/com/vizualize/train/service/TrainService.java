package com.vizualize.train.service;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.play.PlayUIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import com.vizualize.network.NetworkConfiguration;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.plot.Plot;
import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.CSVRestIterator;
import com.vizualize.quandl.iterator.IteratorContainer;
import com.vizualize.quandl.iterator.TrainTestIteratorPair;
import com.vizualize.serialize.SerializableUtils;

public class TrainService {
    
    public static JavaSparkContext sc;
    public static int periodLength = 50;
    //Number of epochs (full passes of the data)
    public static final int nEpochs = 50;
    public static int closeIndex = 1;
    static boolean classification = true;
    static final int numInputs = 1 * periodLength;
//  number of outputs is 1 if not for classification, 
//  predict close price 
    static int numOutputs = 3;
    static int numHidenNodes = numInputs/3;
        
    public static JavaSparkContext getSc() {        
        if (sc == null) {
            SparkConf conf = new SparkConf();
            conf.setMaster("local[*]");
            conf.setAppName("DataVec Example");
            sc = new JavaSparkContext(conf);
        }
        return sc;
    }

    public static String getModelName(String instrumentName, int lookForwardPeriod) {
    	String name = instrumentName + "_" + lookForwardPeriod; 
    	if (classification)
    		name += "_classification";
    	return name;
    }
    
    
    public static void train(String instrumentName, int lookForwardPeriod) throws Exception {
    	System.setProperty(PlayUIServer.UI_SERVER_PORT_PROPERTY, "9005");
    	
//    	UIServer uiServer = UIServer.getInstance();
//    	StatsStorage mlnStatsStorage1 = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

    	//Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
//    	uiServer.attach(mlnStatsStorage1);    

        boolean plotting = false;
        
        getSc();
        String modelName = getModelName(instrumentName, lookForwardPeriod);
        MultiLayerNetwork pretrainNet = NetworkSerializer.loadNetwork(modelName);
        
        String date = NetworkSerializer.lastTrainedDate(modelName);
 
        CSVIterator csvi = new CSVIterator();
        String endDate = null;
        
//        CSVRestIterator csvi = new CSVRestIterator();
//        String endDate = "2018-11-02 12:00:00";
        
        DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, date, endDate);
        ArrayList<DataSetIterator> preTrainDataIterators = new ArrayList<>();
        preTrainDataIterators.add(iter);       
        
        //Create the network
        if (pretrainNet == null) {
            MultiLayerConfiguration mlpConf;
            if (classification)
            	mlpConf = NetworkConfiguration.getDeepDenseLayerNetworkConfigurationClassification(numHidenNodes, numInputs, numOutputs);
            else
            	mlpConf = NetworkConfiguration.getDeepDenseLayerNetworkConfiguration(numHidenNodes, numInputs, numOutputs);
            pretrainNet = new MultiLayerNetwork(mlpConf);
            pretrainNet.init();
        }
        
//        pretrainNet.setListeners(new StatsListener(mlnStatsStorage1));
        
        IteratorContainer preTrainContainer = new IteratorContainer(preTrainDataIterators, 5);
        ArrayList<TrainTestIteratorPair> pretrainTestSplits = preTrainContainer.getTrainTestList();

        
        for (TrainTestIteratorPair trainTestIteratorPair : pretrainTestSplits) {      
//        	trainTestIteratorPair.scale();
        	DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();
        	DataSetIterator testSetIterator = trainTestIteratorPair.getTestIterator();
        	
        	trainSetIterator.reset();
//        	int keepPatterns = 0;
//        	int upPattern = 0;
//        	int downPattern = 0;
//        	ArrayList<Double> changes = new ArrayList<>();
//        	while(trainSetIterator.hasNext()) {
//        		DataSet ds = trainSetIterator.next();
//        		INDArray features = ds.getFeatures();
//        		INDArray lables = ds.getLabels();
//        		int featuresSize = features.size(1);
//        		double lastFeature = features.getDouble(featuresSize -1);
//        		double label = lables.getDouble(0);
////        		double diff = (label - lastFeature)*100/lastFeature;
//        		double diff = (label - lastFeature);
//        		double absDiff = Math.abs(diff);
//        		changes.add(diff);        		
//        		if (absDiff < 3)
//        			keepPatterns++;
//        		else if (label > lastFeature)
//        			upPattern++;
//        		else
//        			downPattern++;
//        	}
//        	
//        	Double[] changesArray = new Double[1];
//        	changesArray = changes.toArray(changesArray);
//    		double[] doubles = ArrayUtils.toPrimitive(changesArray);
//    		Plot.plotHistogram(doubles, 100);
        		
        	for( int i=0; i<nEpochs; i++ ){
        		trainSetIterator.reset();
        		pretrainNet.fit(trainSetIterator);
        	}
        	evaluate(trainSetIterator, pretrainNet);
        	evaluate(testSetIterator, pretrainNet);
        	boolean waitIt = true;
        }                
		
//        File pretrainNetFile = File.createTempFile("pretrainNet", ".zip");
        String dateStr = csvi.getLastIteratedDate();
        NetworkSerializer.saveModel(pretrainNet, modelName, dateStr);    
        Serializable serializable = csvi.getDSNormalizer();
        NetworkSerializer.saveTransform(modelName, serializable);
//        DataSetIterator evalIter = csvi.getIterator(instrumentName + "_test", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, endDate, null);       
//        ArrayList<DataSetIterator> evalDataIterators = new ArrayList<>();
//        evalDataIterators.add(evalIter);
//        IteratorContainer evalContainer = new IteratorContainer(evalDataIterators, 1);
//        ArrayList<TrainTestIteratorPair> evalSplits = evalContainer.getTrainTestList();
//
//        MultiLayerNetwork loadedNet = NetworkSerializer.loadNetwork(modelName);
//        for (TrainTestIteratorPair trainTestIteratorPair : evalSplits) {        	
//        	DataSetIterator testSetIterator = trainTestIteratorPair.getTrainIterator();
//        	evaluate(testSetIterator, loadedNet);
//        }
    }

    
    public static void evaluate(DataSetIterator testSetIterator, MultiLayerNetwork net) {
    	testSetIterator.reset();
    	Evaluation ceval = new Evaluation(3);
    	int testCount = 0;
    	while(testSetIterator.hasNext()){
    		DataSet t = testSetIterator.next(1);
    		INDArray features = t.getFeatures();
    		INDArray lables = t.getLabels();
    		INDArray predicted = net.output(features,false);
    		int featuresSize = features.size(1);
    		double lastFeature = features.getDouble(featuresSize -1);
    		double label = lables.getDouble(0);
    		double predictedD = predicted.getDouble(0);                   

    		INDArray actualBuySell = getBuySellVector(lastFeature, label);
    		INDArray predictedBuySell = getBuySellVector(lastFeature, predictedD);

    		//            for (int i=0; i<featuresSize; i++) {
    			//                Plot.plot(features.getRow(i), lables.getRow(i), predicted.getRow(i), testCount);
    		
//    		INDArray featuresInd = features.getRow(0); 
//    		INDArray labelsInd = lables.getRow(0);
//    		INDArray predictedInd = predicted.getRow(0);
//    		Plot.plot(featuresInd, labelsInd, predictedInd, testCount);

    		testCount++;
    		//            }
    		//            int i = 0;

//    		ceval.eval(actualBuySell, predictedBuySell);
    		ceval.eval(lables, predicted);
//    		if (testCount%1000 == 1) {
//    			Plot.plot(featuresInd, labelsInd, predictedInd, testCount);
//    			int breakIt = 2;
//    		}
    	}
    	System.out.println("Evaluation: ");
    	System.out.print(ceval);
    	boolean wait = true;
    }
    
    public static INDArray getBuySellVector(double actual, double future) {
        double actualDifference = future - actual;
//        double actualDifference = future; // for percentage change
        double[] array = {1, 0};
        if (actualDifference < 0) {
            double[] narray = {0, 1};
            array = narray;
        }
        return Nd4j.create(array, new int[]{1,2});
    }
}
