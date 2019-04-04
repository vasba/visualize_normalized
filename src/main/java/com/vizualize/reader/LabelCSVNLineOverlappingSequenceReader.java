package com.vizualize.reader;

import java.util.ArrayList;
import java.util.List;

import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;

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

	@Override
	protected List<List<Writable>> sequenceLstm(List<List<Writable>> sequence) {
    	int i = 0;
    	ArrayList containerList = new ArrayList<>();
    	ArrayList labelList = new ArrayList<>();
    	Writable w1 = sequence.get(nLinesPerSequence +loopforwardPeriod -1).get(closeIndex);
    	Writable w2 = sequence.get(nLinesPerSequence-1).get(closeIndex);
    	double diff = w1.toDouble() - w2.toDouble();
    	int value = 1; // keep is 1
    	double threshold = 0;
    	if (diff > threshold)
    		value = 1;  // buy is 2
    	else if (diff < threshold*-1)
    		value = 0;  // sell is 0
    	
    	labelList.add(new IntWritable(value));
    	containerList.add(labelList);
    	return containerList;
    }
}
