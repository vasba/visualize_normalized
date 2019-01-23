package com.vizualize.quandl.iterator;

import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

public class TrainTestIteratorPair {
	List<DataSet> trainDataList = new ArrayList<>();
	List<DataSet> testDataList = new ArrayList<>();
	
	NormalizerStandardize normalizer = null;
	
	public DataSetIterator getTrainIterator() {
		if (trainIterator == null) {
			trainIterator =  new CustomizedListDatasetIterator(trainDataList, 1);
		}
		return trainIterator;
	}
	public void setTrainIterator(DataSetIterator trainIterator) {
		this.trainIterator = trainIterator;
	}
	public DataSetIterator getTestIterator() {
		if (testIterator == null) {
			testIterator =  new CustomizedListDatasetIterator(testDataList, 1);
		}
		return testIterator;
	}
	public void setTestIterator(DataSetIterator testIterator) {
		this.testIterator = testIterator;
	}
	DataSetIterator trainIterator = null;
	DataSetIterator testIterator = null;
	
	public void addToTrainSet(DataSet dataSet) {
		trainDataList.add(dataSet);
	}
	
	public void addToTestSet(DataSet dataSet) {
		testDataList.add(dataSet);
	}
	
	public void scale() {
		normalizer = new NormalizerStandardize();
		normalizer.fitLabel(true);
		DataSetIterator dsIterator = getTrainIterator();
		normalizer.fit(dsIterator);
		for (DataSet ds : trainDataList)  {		
			normalizer.transform(ds);
			boolean checkIt = true;
		}
		
		DataSetIterator dsTestIterator = getTestIterator();
		for (DataSet ds : testDataList)  {
			normalizer.transform(ds);
			boolean checkIt = true;
		}
		
    	dsIterator.reset();
    	dsTestIterator.reset();
	}
	
	public boolean validateUniqueSets() {
		
		for (DataSet ds : testDataList)
			if (trainDataList.contains(ds))
				return false;
		return true;
	}

}
