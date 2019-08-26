package com.vizualize.indicator;

import java.util.List;
import java.util.stream.Collectors;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.vizualize.config.ConfigProperties;

public class BBIndicator extends Indicator{
	
	public List<List<Writable>> appendIndicator(List<List<Writable>> sequence) {
		List<List<Writable>> sequenceClone = clone(sequence);
//		List<List<Writable>> sequenceClone = sequence.stream().collect(Collectors.toList());
//		List<List<Writable>> sequenceClone = sequence.stream().map(d -> d.clone()).collect(Collectors.toList());
		double[] inReal = extractColumn(sequence, 1);
		Core taCore = new Core();
		int size = sequence.size();
		int startIdx = 0;
		int endIdx = size - 1;
		int optInTimePeriod = ConfigProperties.BBPeriod;
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] outRealUpperBand = new double[size - optInTimePeriod + 1];
		double[] outRealMiddleBand = new double[size - optInTimePeriod + 1];
		double[] outRealLowerBand = new double[size - optInTimePeriod + 1];
		double optInNbDevUp = 2;
		double optInNbDevDn = 2;
		taCore.bbands(startIdx, endIdx, inReal, optInTimePeriod, optInNbDevUp, optInNbDevDn, MAType.Sma, outBegIdx, outNBElement, outRealUpperBand, outRealMiddleBand, outRealLowerBand);
		adjustSequence(sequenceClone, outBegIdx.value);
		appendIndicatorResult(sequenceClone, outRealUpperBand, outRealMiddleBand, outRealLowerBand);
		boolean stop = true;
		return sequenceClone;
	}
	
	
	
	void appendIndicatorResult(List<List<Writable>> sequence, double[] outRealUpperBand, double[] outRealMiddleBand, double[] outRealLowerBand) {
		int size = sequence.size();
		for(int i= 0; i< size; i++) {
			List l = sequence.get(i);
			Writable upper = new DoubleWritable(outRealUpperBand[i]);
			Writable middle = new DoubleWritable(outRealMiddleBand[i]);
			Writable lower = new DoubleWritable(outRealLowerBand[i]);
			l.add(upper);
			if (ConfigProperties.ICLUDE_BB_MIDLE)
				l.add(middle);
			l.add(lower);
		}
	}
}
