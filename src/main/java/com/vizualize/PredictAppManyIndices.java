package com.vizualize;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.vizualize.network.NetworkConfiguration;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.plot.Plot;
import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.IteratorContainer;
import com.vizualize.quandl.iterator.TrainTestIteratorPair;

public class PredictAppManyIndices {

	static ArrayList<DataSetIterator> preTrainDataIterators = new ArrayList<>();
	static ArrayList<DataSetIterator> dataIterators = new ArrayList<>();
	//Random number generator seed, for reproducability
	public static final int seed = 12345;

	
	
	//Number of iterations per minibatch
	public static final int iterations = 1;
	//Network learning rate
	public static double learningRate = 0.001;
	public static final int periodLength = 50;
	public static final int numInputs = 1 * periodLength;

	//Number of epochs (full passes of the data)
	public static final int nEpochs = 100;
	
	//How frequently should we plot the network output?
	public static final int plotFrequency = 5;
	static JavaSparkContext sc;
	
	static int lookForwardPeriod = 10;
	static int closeIndex = 1;
	static boolean classification = false;
//	number of outputs is 1 if not for classification, 
//	predict close price 
	public static int numOutputs = 1;
	
	//Initialize the user interface backend
    static UIServer uiServer = UIServer.getInstance();

    public static void main( String[] args ) throws Exception
    {    	    	    	
    	boolean plotting = false;
    	SparkConf conf = new SparkConf();
    	conf.setMaster("local[*]");
    	conf.setAppName("DataVec Example");
    	sc = new JavaSparkContext(conf);
    	String instrumentName = "omxs30_5M";
//    	String instrumentName = "omxs30_1W";
    	MultiLayerNetwork pretrainNet = NetworkSerializer.loadNetwork(instrumentName);
    	String date = NetworkSerializer.lastTrainedDate(instrumentName);
    	CSVIterator csvi = new CSVIterator();        
//    	preTrainDataIterators.add(csvi.getIterator("dax_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, null));
//    	preTrainDataIterators.add(csvi.getIterator("sp500_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, null));
//    	preTrainDataIterators.add(csvi.getIterator("omxs30_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, null));
//    	preTrainDataIterators.add(csvi.getIterator("omxs30_5M", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, null));
//    	DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, date);
//    	preTrainDataIterators.add(iter);
        
//    	QuandlIterator qi = new QuandlIterator();
//    	dataIterator = qi.getIterator("NASDAQOMX/OMXS30", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting);
    	IteratorContainer preTrainContainer = new IteratorContainer(preTrainDataIterators, 1);
    	ArrayList<TrainTestIteratorPair> pretrainTestSplits = preTrainContainer.getTrainTestList();
    	
    	
//    	dataIterators.add(csvi.getIterator("dax_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification));
//    	dataIterators.add(csvi.getIterator("sp500_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification));
//    	dataIterators.add(csvi.getIterator("omxs30_1D", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification));
        
//    	IteratorContainer iterContainer = new IteratorContainer(dataIterators);
//        ArrayList<TrainTestIteratorPair> trainTestSplits = iterContainer.getTrainTestList();
        MultiLayerConfiguration mlpConf;
        if (classification) {
            numOutputs = 2;
            // Network configuration for classification
            mlpConf = getDeepDenseLayerNetworkConfigurationClassification(50);
        } else {
            // Network configuration for regression    	    	      
            mlpConf = NetworkConfiguration.getDeepDenseLayerNetworkConfiguration(20, numInputs, numOutputs);            
        }

    	// Create the linear regression network
    	final MultiLayerConfiguration lrMlpConf = getLinearRegressionConfiguration();
    	    	    
    	ArrayList<RegressionEvaluation> evaluations = new ArrayList<RegressionEvaluation>();
    	ArrayList<RegressionEvaluation> linearRegressionEvals = new ArrayList<RegressionEvaluation>();
    	
    	
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
        //Create the network
    	if (pretrainNet == null) {
    	    pretrainNet = new MultiLayerNetwork(mlpConf);
    	    pretrainNet.init();
    	}
        
//        final MultiLayerNetwork pretrainlrNet = new MultiLayerNetwork(lrMlpConf);
//        pretrainlrNet.init();
//        
//      net.setListeners(new ScoreIterationListener(1));
        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage mlnStatsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(mlnStatsStorage);    
        pretrainNet.setListeners(new StatsListener(mlnStatsStorage));   
        
        for( int i=0; i<nEpochs; i++ ){                 
            System.out.println("pretraining epoch " + i);
            for (TrainTestIteratorPair trainTestIteratorPair : pretrainTestSplits) {    	                     	   
                DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();
                //    		DataSetIterator testSetIterator = trainTestIteratorPair.getTestIterator();

                trainSetIterator.reset();
                pretrainNet.fit(trainSetIterator);

//                trainSetIterator.reset();
//                pretrainlrNet.fit(trainSetIterator);
            }

            int stop = 0;
        }

//        INDArray features = ds.getFeatures();       
//        INDArray prediction = pretrainNet.output(features);
        
    	File pretrainNetFile = File.createTempFile("pretrainNet", ".zip");
//    	File pretrainlrNetFile = File.createTempFile("pretrainlrNet", ".zip");    	
    	
//    	ModelSerializer.writeModel(pretrainNet, pretrainNetFile, true);
//    	ModelSerializer.writeModel(pretrainlrNet, pretrainlrNetFile, true);
//        iter.next(iter.numExamples() -1);
//        NormalizedDataSet ds = (NormalizedDataSet) iter.next();
        String dateStr = csvi.getLastIteratedDate();
        NetworkSerializer.saveModel(pretrainNet, instrumentName, dateStr);
    	
//        DataSetIterator iterator = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, 0, plotting, closeIndex, classification, date);
//        iterator.next(iterator.numExamples() -1);
//        DataSet ds = iterator.next(); 
//        INDArray features = ds.getFeatures();
//        INDArray lables = ds.getLabels();
//        INDArray predicted = pretrainNet.output(features,false);
//        int featuresSize = features.size(0);
//        int j = 0;
        //      for (int j=0; j<featuresSize; j++) 
//        Plot.plot(features.getRow(j), lables.getRow(j), predicted.getRow(j), 1);
        dataIterators.add(csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, classification, date, null));
    	crossValidationTrain(instrumentName, classification);
//        crossValidationTrain(instrumentName, pretrainlrNetFile, classification);
    	
//    	for (TrainTestIteratorPair trainTestIteratorPair : trainTestSplits) {
//            
////          net.setListeners(new ScoreIterationListener(1));
//          //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
//          StatsStorage mlnStatsStorage1 = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later
//
//          //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
//          uiServer.attach(mlnStatsStorage1);    
////        Create the network
//          final MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(pretrainNetFile);
//          net.setListeners(new StatsListener(mlnStatsStorage1));     
//          
//          final MultiLayerNetwork lrNet = ModelSerializer.restoreMultiLayerNetwork(pretrainlrNetFile);
//          
//          DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();
//          DataSetIterator testSetIterator = trainTestIteratorPair.getTestIterator();
//          for( int i=0; i<nEpochs; i++ ){
//                  
//              System.out.println("training epoch " + i);
//              trainSetIterator.reset();
//              net.fit(trainSetIterator);
//              
//              trainSetIterator.reset();
//              lrNet.fit(trainSetIterator);
//          }
//          testSetIterator.reset();
//          RegressionEvaluation eval = net.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
//          evaluations.add(eval);
//          testSetIterator.reset();
//          RegressionEvaluation lrEval = lrNet.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
//          linearRegressionEvals.add(lrEval);
//          int stop = 0;
//      }
//
//    	System.out.println("Evaluation of multi layer");
//    	for (RegressionEvaluation eval : evaluations) {    		
//    		System.out.println(eval.stats());
//    		System.out.println();
//    	}
//    	
//    	System.out.println("Evaluation of Linear regression");
//    	for (RegressionEvaluation eval : linearRegressionEvals) {    		
//    		System.out.println(eval.stats());
//    		System.out.println();
//    	}

    	
    	int stop = 8;
    	
    	uiServer.stop();
    }

    
    

    /** Returns the network configuration
     */
    private static MultiLayerConfiguration getDeepDenseLayerNetworkConfigurationClassification(int numHiddenNodes) {
        return new NeuralNetConfiguration.Builder()
                .seed(seed)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .updater(new Nesterovs(0.9))
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation(Activation.RELU).build())
                .layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.RELU).build())
                .layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.RELU).build())
                .layer(3, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.RELU).build())
                .layer(4, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();
    }
    
    private static MultiLayerConfiguration getLinearRegressionConfiguration() { 
    	final int numHiddenNodes = 1;
    	return new NeuralNetConfiguration.Builder()
    			.seed(seed)
    			.iterations(iterations)
    			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    			.learningRate(learningRate)
    			.weightInit(WeightInit.XAVIER)
    			.updater(new Nesterovs(0.9))
    			.l2(0.001)    		
    			.regularization(true)
    			.list()
    			.layer(0, new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
    					.activation(Activation.IDENTITY)
    					.nIn(numInputs).nOut(numOutputs).build())
    			.pretrain(false).backprop(true).build();
    }
    
    private static void crossValidationTrain(String instrumentName, boolean classification) throws Exception {
        ArrayList<RegressionEvaluation> evaluations = new ArrayList<RegressionEvaluation>();
        ArrayList<Evaluation> evaluations1 = new ArrayList<Evaluation>();
        
        IteratorContainer iterContainer = new IteratorContainer(dataIterators);
        ArrayList<TrainTestIteratorPair> trainTestSplits = iterContainer.getTrainTestList();


        for (TrainTestIteratorPair trainTestIteratorPair : trainTestSplits) {

            //          net.setListeners(new ScoreIterationListener(1));
            //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
            StatsStorage mlnStatsStorage1 = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

            //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
            uiServer.attach(mlnStatsStorage1);    
            //        Create the network
            final MultiLayerNetwork net = NetworkSerializer.loadNetwork(instrumentName);
            net.setListeners(new StatsListener(mlnStatsStorage1));     

//            final MultiLayerNetwork lrNet = ModelSerializer.restoreMultiLayerNetwork(pretrainlrNetFile);

            DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();
            DataSetIterator testSetIterator = trainTestIteratorPair.getTestIterator();
            for( int i=0; i<nEpochs; i++ ){

                System.out.println("training epoch " + i);
                trainSetIterator.reset();
                net.fit(trainSetIterator);
//                testSetIterator.reset();
//                if (classification) {
//                    Evaluation eval = evaluateClassification(net, testSetIterator);
//                } else {
//                    int testcount = 0;
//                    Evaluation ceval = new Evaluation(2);
//                    while(testSetIterator.hasNext()){
//                        DataSet t = testSetIterator.next(1);
//                        INDArray features = t.getFeatures();
//                        INDArray lables = t.getLabels();
//                        INDArray predicted = net.output(features,false);
//                        int featuresSize = features.size(0);
////                        for (int j=0; j<featuresSize; j++) 
////                            Plot.plot(features.getRow(j), lables.getRow(j), predicted.getRow(j), testcount);
////
////                        //                        eval.eval(lables, predicted);
////                        testcount++;
//                        
//                        double lastFeature = features.getDouble(featuresSize -1);
//                        double label = lables.getDouble(0);
//                        double predictedD = predicted.getDouble(0);                   
//                        
//                        INDArray actualBuySell = getBySellVector(lastFeature, label);
//                        INDArray predictedBuySell = getBySellVector(lastFeature, predictedD);
//                        ceval.eval(actualBuySell, predictedBuySell);
//                    }
//                }

//                trainSetIterator.reset();
//                lrNet.fit(trainSetIterator);
            }
            
            testSetIterator.reset();
            if (classification) {
                //evaluate the model on the test set
                Evaluation eval = evaluateClassification(net, testSetIterator);
                evaluations1.add(eval);
            } else {
                int testCount = 0;
                Evaluation ceval = new Evaluation(2);
                while(testSetIterator.hasNext()){
                    DataSet t = testSetIterator.next(1);
                    INDArray features = t.getFeatures();
                    INDArray lables = t.getLabels();
                    INDArray predicted = net.output(features,false);
                    int featuresSize = features.size(1);
                    double lastFeature = features.getDouble(featuresSize -1);
                    double label = lables.getDouble(0);
                    double predictedD = predicted.getDouble(0);                   
                    
                    INDArray actualBuySell = getBySellVector(lastFeature, label);
                    INDArray predictedBuySell = getBySellVector(lastFeature, predictedD);
                    
//                    for (int i=0; i<featuresSize; i++) {
//                        Plot.plot(features.getRow(i), lables.getRow(i), predicted.getRow(i), testCount);
                        INDArray featuresInd = features.getRow(0); 
                        INDArray labelsInd = lables.getRow(0);
                        INDArray predictedInd = predicted.getRow(0);
                        Plot.plot(featuresInd, labelsInd, predictedInd, testCount);
                        testCount++;
//                    }
//                    int i = 0;
                    
                    ceval.eval(actualBuySell, predictedBuySell);
                }
                testSetIterator.reset();
                RegressionEvaluation eval = net.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
                evaluations.add(eval);
            }
//            testSetIterator.reset();
//            RegressionEvaluation lrEval = lrNet.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
//            linearRegressionEvals.add(lrEval);
            
            int stop = 0;
        }

        System.out.println("Evaluation: ");
        if (classification) {
            for (Evaluation eval : evaluations1) {         
                System.out.println(eval.stats());
                System.out.println();
            }
        } else {
            for (RegressionEvaluation eval : evaluations) {         
                System.out.println(eval.stats());
                System.out.println();
            }
        }
    }
    
    public static INDArray getBySellVector(double actual, double future) {
        double actualDifference = future - actual;
        double[] array = {1, 0};
        if (actualDifference < 0) {
            double[] narray = {0, 1};
            array = narray;
        }
        return Nd4j.create(array, new int[]{1,2});
    }
    
    public static Evaluation evaluateClassification(MultiLayerNetwork net, DataSetIterator testSetIterator) {
      //evaluate the model on the test set
        Evaluation eval = new Evaluation(2);
        int testcount = 0; 
        while(testSetIterator.hasNext()){
            DataSet t = testSetIterator.next(1);
            INDArray features = t.getFeatures();
            INDArray lables = t.getLabels();
            INDArray predicted = net.output(features,false);
            int featuresSize = features.size(0);
//            for (int i=0; i<featuresSize; i++) 
//                Plot.plot(features.getRow(i), lables.getRow(i), predicted.getRow(i), testcount);
//            testcount ++ ;
            eval.eval(lables, predicted);
        }
        return eval;
    }
    
   
}


