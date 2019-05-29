package com.vizualize.indicator;

import java.util.List;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;

public class EMAIndicator extends SMAIndicator {

	@Override
	public void appendIndicator(List<List<Writable>> sequence) {
		double[] inReal = extractColumn(sequence, 1);
		Core taCore = new Core();
		int size = sequence.size();
		int startIdx = 0;
		int endIdx = size - 1;
		int optInTimePeriod = 20;
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] outReal = new double[size - optInTimePeriod + 1];
		taCore.ema(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNBElement, outReal);
		adjustSequence(sequence, outBegIdx.value);
		appendIndicatorResult(sequence, outReal);
	}	
}
