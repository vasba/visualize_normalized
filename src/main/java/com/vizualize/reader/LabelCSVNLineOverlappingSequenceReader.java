package com.vizualize.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;

import com.vizualize.indicator.EMAIndicator;
import com.vizualize.indicator.Indicator;
import com.vizualize.indicator.SMAIndicator;
import com.vizualize.peaksandvaleys.PeaksAndValeys;

public class LabelCSVNLineOverlappingSequenceReader extends CSVNLineOverlappingSequenceReader {
	
	public LabelCSVNLineOverlappingSequenceReader() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LabelCSVNLineOverlappingSequenceReader(int nLinesPerSequence, int loopForwardPeriod, boolean forPlotting,
			int closeIndex, boolean classification, boolean forLstm) {
		super(nLinesPerSequence, loopForwardPeriod, forPlotting, closeIndex, classification, forLstm);
		// TODO Auto-generated constructor stub
	}

	public LabelCSVNLineOverlappingSequenceReader(int nLinesPerSequence, int loopForwardPeriod, int skipNumLines,
			String delimiter, boolean forPlotting, int closeIndex, boolean classification, boolean forLstm) {
		super(nLinesPerSequence, loopForwardPeriod, skipNumLines, delimiter, forPlotting, closeIndex, classification, forLstm);
		// TODO Auto-generated constructor stub
	}

//	@Override
//	protected List<List<Writable>> sequenceLstm(List<List<Writable>> sequence) {
//		if(loopBackPeriod > 0)
//			adjustSequence(sequence, loopBackPeriod - 1);
//    	int i = 0;
//    	ArrayList containerList = new ArrayList<>();
//    	ArrayList labelList = new ArrayList<>();
//    	Writable w1 = sequence.get(nLinesPerSequence +loopforwardPeriod -1).get(closeIndex);
//    	Writable w2 = sequence.get(nLinesPerSequence-1).get(closeIndex);
//    	double diff = w1.toDouble() - w2.toDouble();
//    	int value = 1; // keep is 1
//    	double threshold = 0;
//    	if (diff > threshold)
//    		value = 1;  // buy is 2
//    	else if (diff < threshold*-1)
//    		value = 0;  // sell is 0
//    	
//    	labelList.add(new IntWritable(value));
//    	containerList.add(labelList);
//    	return containerList;
//    }
	
//	protected void computeAllLAbels() {
//		List<List<Writable>> sequence = new ArrayList<>();
//		while (hasNext()) {
//			sequence.add(next());
//		}
//		double[] ema = Indicator.extractColumn(sequence, 3);
//		HashMap<Integer, String> resultHash = PeaksAndValeys.localMinima(ema);
//		List<List<Writable>> labels = sequenceLstmOld(sequence);
//    }
	
	@Override
	protected List<List<Writable>> sequenceLstm(List<List<Writable>> sequence) {
		double[] ema = Indicator.extractColumn(sequence, 3);
		double[] closes = Indicator.extractColumn(sequence, 1);
		HashMap<Integer, String> resultHash = PeaksAndValeys.localMinima(ema);		
		ArrayList containerList = new ArrayList<>();
		ArrayList labelList = new ArrayList<>();
		//		int i = 0;
		String lastValue = null;
		int offsetPeakOrValey = 3;
		for (Map.Entry<Integer, String> entry : resultHash.entrySet()) {
			Integer key = entry.getKey();
			String value = entry.getValue();
			//		    i = addLabels(closes, sequence, i, key, value);
			if (key < nLinesPerSequence - offsetPeakOrValey)
				lastValue = value;
		}

		//		lastValue = reverseType(lastValue);
		//		addLabelsResolute(closes, sequence, i, closes.length-1, lastValue);
		if (lastValue == null) {
			if (ema[0] > ema[nLinesPerSequence - 1])
				labelList.add(new IntWritable(0));
			else
				labelList.add(new IntWritable(1));
		} else if (lastValue.contains("bottom")) {
			labelList.add(new IntWritable(1));
		} else if (lastValue.contains("top")) {
			labelList.add(new IntWritable(0));
		}

		containerList.add(labelList);
		return containerList;
	}
	
	String reverseType(String value) {
		if (value.contains("bottom")) {
			return "top";
		} 
		if (value.contains("top")) {
			return "bottom";
		}
		return "top";
	}
	
	int addLabels(double[] closes, List<List<Writable>> sequence, int i,
			Integer key, String value) {
		if (value.contains("bottom")) {
	    	int minIdx = findMinIndex(closes, i, key);
	    	for (i=i;i<=minIdx;i++) {
	    		List<Writable> list = sequence.get(i);
	    		list.add(new IntWritable(0));
	    	}
	    }
	    if (value.contains("top")) {
	    	int maxIdx = findMaxIndex(closes, i, key);
	    	for (i=i;i<=maxIdx;i++) {
	    		List<Writable> list = sequence.get(i);
	    		list.add(new IntWritable(1));
	    	}
	    }
	    return i;
	}
	
	int addLabelsResolute(double[] closes, List<List<Writable>> sequence, int i,
			Integer key, String value) {
		if (value.contains("bottom")) {
//	    	int minIdx = findMinIndex(closes, i, key);
	    	for (i=i;i<=key;i++) {
	    		List<Writable> list = sequence.get(i);
	    		list.add(new IntWritable(0));
	    	}
	    }
	    if (value.contains("top")) {
//	    	int maxIdx = findMaxIndex(closes, i, key);
	    	for (i=i;i<=key;i++) {
	    		List<Writable> list = sequence.get(i);
	    		list.add(new IntWritable(1));
	    	}
	    }
	    return i;
	}
	
	int findMaxIndex(double [] arr, int start, int end) { 
		double max = arr[start]; 
		int maxIdx = 0; 
		for(int i = start; i < end; i++) { 
			if(arr[i] > max) { 
				max = arr[i]; 
				maxIdx = i; 
			} 
		} 
		return maxIdx; 
	}
	
	int findMinIndex(double [] arr, int start, int end) { 
		double min = arr[start]; 
		int minIdx = 0; 
		for(int i = start; i < end; i++) { 
			if(arr[i] < min) { 
				min = arr[i]; 
				minIdx = i; 
			} 
		} 
		return minIdx; 
	}
	
	@Override
	protected List<List<Writable>> appendIndicator(List<List<Writable>> sequence) {
		if (hasIndicators) {
			SMAIndicator ema = new SMAIndicator();
			return ema.appendIndicator(sequence);
		}
		return sequence;
	}
}
