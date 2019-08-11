package com.vizualize.indicator;

import java.util.List;
import java.util.stream.Collectors;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.vizualize.config.ConfigProperties;

public class EMAIndicator extends SMAIndicator {

	@Override
	public List<List<Writable>> appendIndicator(List<List<Writable>> sequence) {
		List<List<Writable>> sequenceClone = sequence.stream().collect(Collectors.toList());
		double[] inReal = extractColumn(sequence, 1);
		Core taCore = new Core();
		int size = sequence.size();
		int startIdx = 0;
		int endIdx = size - 1;
		int optInTimePeriod = ConfigProperties.BBPeriod;
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] outReal = new double[size - optInTimePeriod + 1];
		taCore.ema(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNBElement, outReal);
		adjustSequence(sequenceClone, outBegIdx.value);
		appendIndicatorResult(sequenceClone, outReal);
		return sequenceClone;
	}	
}
