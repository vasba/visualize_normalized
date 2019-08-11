package com.vizualize.indicator;

import java.util.List;
import java.util.stream.Collectors;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.vizualize.config.ConfigProperties;

public class SMAIndicator extends Indicator {

	@Override
	public List<List<Writable>> appendIndicator(List<List<Writable>> sequence) {
		List<List<Writable>> sequenceClone = clone(sequence);
		double[] inReal = extractColumn(sequence, 1);
		Core taCore = new Core();
		int size = sequence.size();
		int startIdx = 0;
		int endIdx = size - 1;
		int optInTimePeriod = ConfigProperties.BBPeriod;
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] outReal = new double[size - optInTimePeriod + 1];
		taCore.sma(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNBElement, outReal);
		adjustSequence(sequenceClone, outBegIdx.value);
		appendIndicatorResult(sequenceClone, outReal);
		return sequenceClone;
	}

	void appendIndicatorResult(List<List<Writable>> sequence, double[] outReal) {
		int size = sequence.size();
		for(int i= 0; i< size; i++) {
			List l = sequence.get(i);
			Writable ema = new DoubleWritable(outReal[i]);
			l.add(ema);
		}
	}
}
