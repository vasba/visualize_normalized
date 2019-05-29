package com.vizualize.indicator;

import java.util.List;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;

public class BBIndicator extends Indicator{
	
	public void appendIndicator(List<List<Writable>> sequence) {
		double[] inReal = extractColumn(sequence, 1);
		Core taCore = new Core();
		int size = sequence.size();
		int startIdx = 0;
		int endIdx = size - 1;
		int optInTimePeriod = 20;
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] outRealUpperBand = new double[size - optInTimePeriod + 1];
		double[] outRealMiddleBand = new double[size - optInTimePeriod + 1];
		double[] outRealLowerBand = new double[size - optInTimePeriod + 1];
		double optInNbDevUp = 2;
		double optInNbDevDn = 2;
		taCore.bbands(startIdx, endIdx, inReal, optInTimePeriod, optInNbDevUp, optInNbDevDn, MAType.Sma, outBegIdx, outNBElement, outRealUpperBand, outRealMiddleBand, outRealLowerBand);
		adjustSequence(sequence, outBegIdx.value);
		appendIndicatorResult(sequence, outRealUpperBand, outRealMiddleBand, outRealLowerBand);
		boolean stop = true;
	}
	
	
	
	void appendIndicatorResult(List<List<Writable>> sequence, double[] outRealUpperBand, double[] outRealMiddleBand, double[] outRealLowerBand) {
		int size = sequence.size();
		for(int i= 0; i< size; i++) {
			List l = sequence.get(i);
			Writable upper = new DoubleWritable(outRealUpperBand[i]);
			Writable middle = new DoubleWritable(outRealMiddleBand[i]);
			Writable lower = new DoubleWritable(outRealLowerBand[i]);
			l.add(upper);
			l.add(middle);
			l.add(lower);
		}
	}
}
