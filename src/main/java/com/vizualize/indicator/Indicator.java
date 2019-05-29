package com.vizualize.indicator;

import java.util.List;

import org.datavec.api.writable.Writable;

public abstract class Indicator {

	public abstract void appendIndicator(List<List<Writable>> sequence);
	
	public static double[] extractColumn(List<List<Writable>> sequence, int column) {
		int size = sequence.size();
		double inReal[] = new double[size];
		for(int i= 0; i< size; i++) {
			inReal[i] = sequence.get(i).get(column).toDouble();
		}
		return inReal;
	}
	
	void adjustSequence(List<List<Writable>> sequence, int endIndx) {
		for (int i= 0;i<endIndx;i++) {
			sequence.remove(0);
		}
	}

}
