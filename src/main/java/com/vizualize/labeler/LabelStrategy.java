package com.vizualize.labeler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;

import com.vizualize.config.ConfigProperties;
import com.vizualize.indicator.Indicator;
import com.vizualize.peaksandvaleys.PeaksAndValeys;

public class LabelStrategy {
	
	
	/**
	 * EMA back strategy on close's peak/valeys
	 * @param sequence
	 * @param nLinesPerSequence
	 * @return
	 */
	public List<List<Writable>> createLabels(List<List<Writable>> sequence, int nLinesPerSequence) {
		double[] ema = Indicator.extractColumn(sequence, 3);
		double[] closes = Indicator.extractColumn(sequence, 1);
		Map<Integer, String> resultHash = PeaksAndValeys.localMinimaStrong(ema);		
		ArrayList containerList = new ArrayList<>();
		ArrayList labelList = new ArrayList<>();
		//		int i = 0;
		String lastValue = null;
		int lastKey = 0;
//		int prevKey = 0;
		int offsetPeakOrValey = ConfigProperties.OFFSET_PEAK_VALEY;
		for (Map.Entry<Integer, String> entry : resultHash.entrySet()) {
			Integer key = entry.getKey();
			String value = entry.getValue();
			int index = 0;
			if (value.contains("bottom")) {
				index = findMinIndex(closes, lastKey, key);
			} else if (value.contains("top")) {
				index = findMaxIndex(closes, lastKey, key);
			}
			
			if (index > nLinesPerSequence - 1 - offsetPeakOrValey)
				break;
			
			//		    i = addLabels(closes, sequence, i, key, value);
//			if (key < nLinesPerSequence - offsetPeakOrValey)
			lastValue = value;
//				prevKey = lastKey;
			lastKey = key;				
		}
		
		//		lastValue = reverseType(lastValue);
		//		addLabelsResolute(closes, sequence, i, closes.length-1, lastValue);
//		Plot.plot(ema, closes);
		if (lastValue == null) {
			if (ema[0] > ema[nLinesPerSequence - 1])
				labelList.add(new IntWritable(0));
			else
				labelList.add(new IntWritable(1));
		} else if (lastValue.contains("bottom")) {
//			int index = findMinIndex(closes, prevKey, lastKey);
//			if (index < nLinesPerSequence)
				labelList.add(new IntWritable(1));
//			else 
//				labelList.add(new IntWritable(0));
		} else if (lastValue.contains("top")) {
//			int index = findMaxIndex(closes, prevKey, lastKey);
//			if (index < nLinesPerSequence)
				labelList.add(new IntWritable(0));
//			else 
//				labelList.add(new IntWritable(1));
		}

		containerList.add(labelList);
		return containerList;

	}

	public static int findMaxIndex(double [] arr, int start, int end) { 
		double max = arr[start]; 
		int maxIdx = start; 
		for(int i = start; i < end; i++) { 
			if(arr[i] > max) { 
				max = arr[i]; 
				maxIdx = i; 
			} 
		} 
		return maxIdx; 
	}
	
	public static int findMinIndex(double [] arr, int start, int end) { 
		double min = arr[start]; 
		int minIdx = start; 
		for(int i = start; i < end; i++) { 
			if(arr[i] < min) { 
				min = arr[i]; 
				minIdx = i; 
			} 
		} 
		return minIdx; 
	}

	public static int addLabels(double[] closes, List<List<Writable>> sequence, int i,
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
}