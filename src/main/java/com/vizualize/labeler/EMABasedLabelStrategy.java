package com.vizualize.labeler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;

import com.vizualize.indicator.Indicator;
import com.vizualize.peaksandvaleys.PeaksAndValeys;

public class EMABasedLabelStrategy extends LabelStrategy {
	
	@Override
	public List<List<Writable>> createLabels(List<List<Writable>> sequence, int nLinesPerSequence) {
		double[] ema = Indicator.extractColumn(sequence, 3);
		double[] closes = Indicator.extractColumn(sequence, 1);
		Map<Integer, String> resultHash = PeaksAndValeys.localMinima(ema);		
		ArrayList containerList = new ArrayList<>();
		ArrayList labelList = new ArrayList<>();
		//		int i = 0;
		String lastValue = null;
		int offsetPeakOrValey = 5;
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

}
