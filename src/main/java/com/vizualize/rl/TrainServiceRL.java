package com.vizualize.rl;

import java.util.ArrayList;
import java.util.Random;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
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

import com.vizualize.network.NetworkConfigurationLstm;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.CSVIteratorLstm;
import com.vizualize.reader.DataSetNormalizer;
import com.vizualize.train.service.TrainService;
import com.vizualize.writer.FilePrinter;

public class TrainServiceRL extends TrainService {

	private static int nIn = 6;
	private static int nOut = 2;
	private static int lstmLayerSize = 50;
	static double epsilon = 1;
	static double profit = 0;
	static ArrayList<Double> profits = new ArrayList<>();
	static Random random = new Random();
	static DataSetNormalizer normalizer = new DataSetNormalizer();

	static int selectedAction = -1;
	// the latest state the last action was taken
	static DataSet selectedDataSet = null;
	static DataSet currentDataSet = null;
	static ArrayList<DataSet> previousDataSets = new ArrayList<>();
	static ArrayList<DataSet> evaluationDataSets = new ArrayList<>();
	static DataSet previousDataSet = null;
	static Evaluation eval = null;

	public static void train(String instrumentName, int lookForwardPeriod) throws Exception {
		System.setProperty(PlayUIServer.UI_SERVER_PORT_PROPERTY, "9005");

		UIServer uiServer = UIServer.getInstance();
		StatsStorage mlnStatsStorage1 = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

		//Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
		uiServer.attach(mlnStatsStorage1);    

		boolean plotting = false;

		getSc();
		String modelName = getModelName(instrumentName, periodLength);
		MultiLayerNetwork pretrainNet = NetworkSerializer.loadNetwork(modelName);

		String date = NetworkSerializer.lastTrainedDate(modelName);

		CSVIterator csvi = new CSVIteratorLstm();
		csvi.forLstm = true;

		String endDate = null;

		DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, true, date, endDate);
		ArrayList<DataSetIterator> preTrainDataIterators = new ArrayList<>();
		preTrainDataIterators.add(iter);

		//Create the network
		if (pretrainNet == null) {
			MultiLayerConfiguration mlpConf;
			mlpConf = NetworkConfigurationLstm.getLstmConfiguration(nIn, nOut, lstmLayerSize);
			pretrainNet = new MultiLayerNetwork(mlpConf);
			pretrainNet.init();
		}
		FilePrinter.write("rlTrainReport.txt", "RL train report", false);
		pretrainNet.setListeners(new StatsListener(mlnStatsStorage1));
		ArrayList evaluations = new ArrayList<>();
		double averageDuration = 0;
		int durationSampleCounter = 0;
		for( int i=0; i<nEpochs; i++ ) {
			previousDataSets = new ArrayList<>();
			evaluationDataSets = new ArrayList<>();
			previousDataSet = null;
			selectedDataSet = null;
			selectedAction = -1;
			currentDataSet = null;
			iter.reset();					
			profits.add(profit);
			String reportContent = "\nProfit for epoch " + (i-1) + " is: " + profit ;
			FilePrinter.write("rlTrainReport.txt", reportContent, true);
			profit = 0;
			int iteration = 0;
			while(iter.hasNext()) {
				long startTime = System.currentTimeMillis();
				DataSet ds = iter.next();
				currentDataSet = ds;
				INDArray predictedActionArray = pretrainNet.output(ds.getFeatures());
				int actualPredictedAction = 1;
				if (i > 20) {
					actualPredictedAction = getActualPredictedAction(predictedActionArray);
					actualPredictedAction = getOtherAction(actualPredictedAction, i);
				} else {
					actualPredictedAction = getActualPredictedAction(ds.getLabels());
				}
				if (selectedAction < 0) {
					selectedAction = actualPredictedAction;
					selectedDataSet = ds;
				} else if (actualPredictedAction != selectedAction) {
					trainOnpreviousDataSets(pretrainNet, actualPredictedAction);
					selectedAction = actualPredictedAction;
					selectedDataSet = ds;
				}
				previousDataSets.add(ds);  
				previousDataSet = ds;
				durationSampleCounter++;
				long endTime = System.currentTimeMillis();
				double duration = (endTime - startTime);
				averageDuration = averageDuration + (duration -averageDuration)/durationSampleCounter;
				iteration++;
				String iterationCounterStr = "Done iteration: " + iteration + "of "; 
				iterationCounterStr += "in epoch: " + i + ".";
				iterationCounterStr += "\n Average iteration time: " + averageDuration;
				FilePrinter.write("iterationReport.txt", iterationCounterStr, false);
			}
			evaluate2(pretrainNet);
			evaluations.add(eval);	
		}      		

		String dateStr = csvi.getLastIteratedDate();
//		NetworkSerializer.saveModel(pretrainNet, modelName, dateStr);    
	}
	
	 public static String getModelName(String instrumentName, int periodLength) {
		 return instrumentName + "_" + periodLength + "_lstm_rl";
	 }
	 
	 static int getActualPredictedAction(INDArray predictedActionArray) {
		 Double sellProbability = predictedActionArray.getDouble(0, 0, periodLength -1);
		 Double buyProbaility = predictedActionArray.getDouble(0, 1, periodLength -1);
		 return sellProbability > buyProbaility ? 0 : 1;
	 }
	 
	 static int getOtherAction(int actualPredictedAction, int epoch) {
		 double probability = random.nextDouble();
		 if (probability > (1 -epsilon) * 1/nEpochs * (epoch + 1) + epsilon) {
			 if (actualPredictedAction == 1)
				 return 0;
			 else
				 return 1;
		 }
		 return actualPredictedAction;
	 }
	 	 
	 static INDArray createLabelForLatestSelectedAction() {
		 INDArray currentFeatures = currentDataSet.getFeatures();
		 INDArray selectedFeatures = selectedDataSet.getFeatures();
		 Double selectedClose = selectedFeatures.getDouble(0, 1, periodLength -1);
		 Double currentClose = currentFeatures.getDouble(0, 1, periodLength -1);
		 double diff = currentClose - selectedClose;
		 
		 if (selectedAction == 0)
			 profit -= diff;
		 else if (selectedAction == 1)
			 profit += diff;
		 
		 INDArray labelsArray = Nd4j.zeros(1, 2, periodLength);
		 if (diff >= 0) {
			 labelsArray.putScalar(new int[] {0, 1,periodLength-1}, 1);
		 } else {
			 labelsArray.putScalar(new int[] {0, 0,periodLength-1}, 1);
		 }
		 return labelsArray;
	 }
	 
	 static void trainOnpreviousDataSets(MultiLayerNetwork pretrainNet, int predictedAction) {
		 if (previousDataSet != null && selectedDataSet != null && currentDataSet != null) {
			 INDArray labelsArray = createLabelForLatestSelectedAction();
			 for (DataSet previousDs : previousDataSets) {
				 INDArray features = previousDs.getFeatures();
				 normalizer.preProcess(previousDs);
				 pretrainNet.fit(features, labelsArray);				 
				 DataSet pds = new DataSet(features, labelsArray);
				 evaluationDataSets.add(pds);				 
				 int waitForPrediction = 2;
			 }
			 previousDataSets = new ArrayList<>();
		 }
	 }
	 
	 static void evaluate(MultiLayerNetwork pretrainNet) {
		 eval = new Evaluation(nOut);	
		 for (DataSet ds : evaluationDataSets) {
			 INDArray predictionBig = pretrainNet.output(ds.getFeatures()).getRow(0);
			 INDArray labelsArray = ds.getLabels();
			 INDArray prediction = predictionBig.getColumn(periodLength - 1);
			 INDArray labels = labelsArray.getRow(0).getColumn(periodLength - 1);
			 INDArray pp = getPredictedVector(prediction);
			 INDArray ll = getPredictedVector(labels);
			 eval.eval(ll, pp);
		 }		 
	 }
	 
	 static void evaluate2(MultiLayerNetwork pretrainNet) {
		 ListDataSetIterator<DataSet> lds = new ListDataSetIterator<>(evaluationDataSets);
		 eval = pretrainNet.evaluate(lds);
	 }
	 
	 public static INDArray getPredictedVector(INDArray prediction) {
//	        double actualDifference = future - actual
//	        double actualDifference = future; // for percentage change
	        double[] array = {1, 0};
	        double buy = prediction.getDouble(0);
	        double sell = prediction.getDouble(1);
	        if (buy < sell) {
	            double[] narray = {0, 1};
	            array = narray;
	        }
	        return Nd4j.create(array, new int[]{1,2});
	    }
}
