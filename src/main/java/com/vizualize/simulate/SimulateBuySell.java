package com.vizualize.simulate;

import java.util.ArrayList;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import com.vizualize.quandl.iterator.CSVIterator;
import com.vizualize.quandl.iterator.CSVIteratorLstm;
import com.vizualize.train.service.TrainService;

public class SimulateBuySell extends TrainService {
	
	static String instrumentName = "OMXS30_5M"; 
	static int lookForwardPeriod = 4;
	
	public static void main(String[] args) {
		CSVIterator csvi = new CSVIteratorLstm();
		csvi.forLstm = true;
		boolean plotting = false;
		String date = null, endDate = null;
		getSc();

		
		DataSetIterator iter = csvi.getIterator(instrumentName, sc.toSparkContext(sc), periodLength, lookForwardPeriod, plotting, closeIndex, true, date, endDate);
		iter.reset();
		ArrayList dsArrayList = getDsArray(iter);
//		int[] actionTrace = new int [iter.numExamples()];
		ArrayList actionTrace = new ArrayList<>();
		int i = 0; 
		
		ArrayList buyActionTrace = new ArrayList<>();
		ArrayList sellActionTrace = new ArrayList<>();
		double maxStartBuyReward = computeReward(dsArrayList, 0, 0, buyActionTrace);
		double maxStartSellReward = computeReward(dsArrayList, 0, 1, sellActionTrace);
	}
	
	static ArrayList getDsArray(DataSetIterator iter) {
		ArrayList dsArrayList = new ArrayList<>();
		while (iter.hasNext()) {
			dsArrayList.add(iter.next());
		}
		return dsArrayList;
	}
	
	static double computeReward(ArrayList dsArray, int index, int action, ArrayList actionTrace) {
		double reward = 0;
		if (index < dsArray.size()) {
			index++;
			
			DataSet ds = (DataSet) dsArray.get(index);
			INDArray features = ds.getFeatures();
//			features.shape();
			int sze = features.shape()[2];
			DataSet prevDs = (DataSet) dsArray.get(index -1);
			ArrayList buyActionTrace = new ArrayList<>();
			ArrayList sellActionTrace = new ArrayList<>();
			double buyReward = computeReward(dsArray, index, 1, buyActionTrace);
			double sellReward = computeReward(dsArray, index, 0, sellActionTrace);
			if (buyReward > sellReward) {
				reward += buyReward;
				actionTrace = buyActionTrace;
				actionTrace.add(index, 1);
			} else {
				reward += sellReward;
				actionTrace = sellActionTrace;
				actionTrace.add(index, 0);
			}
		} else {
			int breakit = 0;
		}
		return reward;
	}
}
