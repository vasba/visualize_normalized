package com.vizualize;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.IteratorContainer;
import com.vizualize.quandl.iterator.QuandlIterator;
import com.vizualize.quandl.iterator.TrainTestIteratorPair;

public class PredictApp {

	static DataSetIterator dataIterator = null;

	//Random number generator seed, for reproducability
	public static final int seed = 12345;

	//Number of iterations per minibatch
	public static final int iterations = 1;
	//Network learning rate
	public static final double learningRate = 0.001;
	public static final int numInputs = 150;
	public static final int numOutputs = 1;
	//Number of epochs (full passes of the data)
	public static final int nEpochs = 2000;
	   static boolean classification = false;
	
	//How frequently should we plot the network output?
	public static final int plotFrequency = 5;

    public static void main( String[] args )
    {
    	int periodLength = 50;
    	int lookForwardPeriod = 5;
    	boolean plotting = false;
    	SparkConf conf = new SparkConf();
    	conf.setMaster("local[*]");
    	conf.setAppName("DataVec Example");
    	JavaSparkContext sc = new JavaSparkContext(conf);
    	CSVIterator csvi = new CSVIterator();
    	String dateStr = "1017/11/09 24:00:00";
        dataIterator = csvi.getIterator("omxs30", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, 2, classification, dateStr, null);
//    	QuandlIterator qi = new QuandlIterator();
//    	dataIterator = qi.getIterator("NASDAQOMX/OMXS30", sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting);
    	IteratorContainer iterContainer = new IteratorContainer(dataIterator);
    	ArrayList<TrainTestIteratorPair> trainTestSplits = iterContainer.getTrainTestList();
    	final MultiLayerConfiguration mlpConf = getDeepDenseLayerNetworkConfiguration();
    	
    	//Initialize the user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);      

    	// Create the linear regression network
    	final MultiLayerConfiguration lrMlpConf = getLinearRegressionConfiguration();
    	    	    
    	final INDArray[] networkPredictions = new INDArray[nEpochs/ plotFrequency];
    	ArrayList<RegressionEvaluation> evaluations = new ArrayList<RegressionEvaluation>();
    	ArrayList<RegressionEvaluation> linearRegressionEvals = new ArrayList<RegressionEvaluation>();
    	for (TrainTestIteratorPair trainTestIteratorPair : trainTestSplits) {
    	    //Create the network
            final MultiLayerNetwork net = new MultiLayerNetwork(mlpConf);
            net.init();
            net.setListeners(new ScoreIterationListener(1));
            net.setListeners(new StatsListener(statsStorage));
    	    
    		final MultiLayerNetwork lrNet = new MultiLayerNetwork(lrMlpConf);
        	lrNet.init();
        	lrNet.setListeners(new ScoreIterationListener(1));
        	//Then add the StatsListener to collect this information from the network, as it trains
        	lrNet.setListeners(new StatsListener(statsStorage));
        	
    		DataSetIterator trainSetIterator = trainTestIteratorPair.getTrainIterator();
    		DataSetIterator testSetIterator = trainTestIteratorPair.getTestIterator();
    		for( int i=0; i<nEpochs; i++ ){
    			
    			System.out.println("training epoch " + i);
    			trainSetIterator.reset();
    			net.fit(trainSetIterator);
    			//    		if((i+1) % plotFrequency == 0) networkPredictions[i/ plotFrequency] = net.output(x, false);
    			
    			trainSetIterator.reset();
    			lrNet.fit(trainSetIterator);
    		}
    		RegressionEvaluation eval = net.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
    		evaluations.add(eval);
    		testSetIterator.reset();
    		RegressionEvaluation lrEval = lrNet.doEvaluation(testSetIterator, new RegressionEvaluation(1))[0];
    		linearRegressionEvals.add(lrEval);
    	}

    	System.out.println("Evaluation of multi layer");
    	for (RegressionEvaluation eval : evaluations) {    		
    		System.out.println(eval.stats());
    		System.out.println();
    	}
    	
    	System.out.println("Evaluation of Linear regression");
    	for (RegressionEvaluation eval : linearRegressionEvals) {    		
    		System.out.println(eval.stats());
    		System.out.println();
    	}

    	int stop = 8;
    }

    /** Returns the network configuration, 2 hidden DenseLayers of size 50.
     */
    private static MultiLayerConfiguration getDeepDenseLayerNetworkConfiguration() {
    	final int numHiddenNodes = 50;
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
    			.layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
    					.activation(Activation.IDENTITY).build())
    			.layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
    					.activation(Activation.IDENTITY).build())
    			.layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())
    			.layer(3, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())
    			.layer(4, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
    					.activation(Activation.IDENTITY)
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
//    			.layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
//    					.activation(Activation.TANH).build())
//    			.layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//    					.activation(Activation.TANH).build())
    			.layer(0, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
    					.activation(Activation.IDENTITY)
    					.nIn(numInputs).nOut(numOutputs).build())
    			.pretrain(false).backprop(true).build();
    }
}


