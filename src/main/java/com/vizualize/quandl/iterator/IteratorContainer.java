package com.vizualize.quandl.iterator;

import java.util.ArrayList;
import java.util.Iterator;

import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

public class IteratorContainer {
	
	public IteratorContainer(DataSetIterator completeIterator) {
		super();
		this.completeIterators.add(completeIterator);
		split();
	}
	
	public IteratorContainer(DataSetIterator completeIterator, int splits) {
		super();
		this.completeIterators.add(completeIterator);
		this.splits = splits;
		split();
	}
	
	public IteratorContainer(ArrayList<DataSetIterator> completeIterators) {
        super();
        this.completeIterators = completeIterators;
        split();
    }
    
    public IteratorContainer(ArrayList<DataSetIterator> completeIterators, int splits) {
        super();
        this.completeIterators = completeIterators;
        this.splits = splits;
        split();
    }
	
	ArrayList<TrainTestIteratorPair> trainTestList = new ArrayList<TrainTestIteratorPair>();
	
	public ArrayList<TrainTestIteratorPair> getTrainTestList() {
		return trainTestList;
	}

	ArrayList<DataSetIterator> completeIterators = new ArrayList<>();
	
	int splits = 5;	
	
	private double getTestSplitPercentage() {
	    if (splits == 1)
	        return 0.0;
	    return 1.0/splits;
	}	 
	
	private void split() {
		initTrainTestPairs();
		
		int i = 0;
		
		Iterator<TrainTestIteratorPair> setsIterator = trainTestList.iterator();
		while (setsIterator.hasNext()) {						
			TrainTestIteratorPair trainTestIteratorPair = setsIterator.next();			
			for (DataSetIterator completeIterator : completeIterators) {
			    int j = 0;
			    int totalExamples = completeIterator.totalExamples();
//		        double testExampleD = totalExamples*testSplitPercentage;
		        int testExamples = (int) (totalExamples*getTestSplitPercentage());
			    int rangeLow = i * testExamples;
	            int rangeHigh = rangeLow + testExamples;
			    completeIterator.reset();
			    while(completeIterator.hasNext()) {
			        DataSet entry = completeIterator.next();
			        if(j>= rangeLow && j< rangeHigh) {
			            trainTestIteratorPair.addToTestSet(entry);
			        } else {			            			            
			            trainTestIteratorPair.addToTrainSet(entry);
			        }
			        j = j + entry.numExamples();
			    }
			}
			i++;
		}
	}
	
	private void initTrainTestPairs() {
		int i = 0;
		while (i < splits) {
			trainTestList.add(new TrainTestIteratorPair());
			i++;
		}
	}
}
