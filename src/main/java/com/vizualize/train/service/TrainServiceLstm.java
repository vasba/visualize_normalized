package com.vizualize.train.service;

import java.util.ArrayList;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.play.PlayUIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import com.vizualize.network.NetworkConfigurationLstm;
import com.vizualize.network.NetworkSerializer;
import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.CSVIteratorLstm;
import com.vizualize.reader.DataSetNormalizer;
import com.vizualize.writer.FilePrinter;

public class TrainServiceLstm extends TrainService {
	
	private static int nIn = 6;
	private static int nOut = 2;
	private static int lstmLayerSize = 50;
		
	public static void train(String instrumentName, int lookForwardPeriod) throws Exception {
    	System.setProperty(PlayUIServer.UI_SERVER_PORT_PROPERTY, "9005");
    	
    	UIServer uiServer = UIServer.getInstance();
    	StatsStorage mlnStatsStorage1 = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

    	//Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
    	uiServer.attach(mlnStatsStorage1);    

        boolean plotting = false;
        
        getSc();
        String modelName = getModelName(instrumentName, lookForwardPeriod);
        MultiLayerNetwork pretrainNet = NetworkSerializer.loadNetwork(modelName);
        
        String date = NetworkSerializer.lastTrainedDate(modelName);
 
        CSVIterator csvi = new CSVIteratorLstm();
        csvi.forLstm = true;
        
        String endDate = null;
        DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, true, date, endDate);
        DataSetNormalizer normalizer = new DataSetNormalizer();
        iter.setPreProcessor(normalizer);
        ArrayList<DataSetIterator> preTrainDataIterators = new ArrayList<>();
        preTrainDataIterators.add(iter);
        
      //Create the network
        if (pretrainNet == null) {
            MultiLayerConfiguration mlpConf;
            mlpConf = NetworkConfigurationLstm.getLstmConfiguration(nIn, nOut, lstmLayerSize);
            pretrainNet = new MultiLayerNetwork(mlpConf);
            pretrainNet.init();
        }
        
        pretrainNet.setListeners(new StatsListener(mlnStatsStorage1));
        ArrayList evaluations = new ArrayList<>();
        for( int i=0; i<nEpochs; i++ ) {
    		iter.reset();
    		pretrainNet.fit(iter);
    		iter.reset();
    		Evaluation evaluation = pretrainNet.evaluate(iter);
    		evaluations.add(evaluation);  
    		String evaluationStr = "Evaluation at iteration: " + i + "\n";
    		evaluationStr += evaluation.stats() + "\n\n";
    		FilePrinter.write("lstmEvaluations.txt", evaluationStr, i == 0 ? false : true);
        }      
        
		iter.reset(); 
		Evaluation evaluation = pretrainNet.evaluate(iter);
		evaluations.add(evaluation);
        
//        String dateStr = csvi.getLastIteratedDate();
		String dateStr = null;
        NetworkSerializer.saveModel(pretrainNet, modelName, dateStr);    

	}
	
	public static String getModelName(String instrumentName, int lookForwardPeriod) {
		return instrumentName + "_" + lookForwardPeriod + "_lstm";
	}

} 
