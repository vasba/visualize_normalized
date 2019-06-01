package com.vizualize.indicator;

import java.util.ArrayList;
import java.util.List;

import org.datavec.api.conf.Configuration;
import org.datavec.api.io.WritableUtils;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.FloatWritable;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;

public abstract class Indicator {

	public abstract List<List<Writable>> appendIndicator(List<List<Writable>> sequence);
	
	public static double[] extractColumn(List<List<Writable>> sequence, int column) {
		int size = sequence.size();
		double inReal[] = new double[size];
		for(int i= 0; i< size; i++) {
			inReal[i] = sequence.get(i).get(column).toDouble();
		}
		return inReal;
	}
	
	void adjustSequence(List<List<Writable>> sequence, int endIndx) {
		for (int i= 0;i<=endIndx;i++) {
			sequence.remove(0);
		}
	}
	
	public static List<List<Writable>> clone(List<List<Writable>> sequence) {
		ArrayList container = new ArrayList<>();	
		Configuration config = new Configuration();
		for (List<Writable> l : sequence) {
			ArrayList list = new ArrayList<>();
			for (org.datavec.api.writable.Writable w : l) {				
				list.add(cloneWritable(w));
			}
			container.add(list);
		}
		return container;
	}
	
	public static Writable cloneWritable(Writable w) {
		if (w instanceof DoubleWritable)
			return new DoubleWritable(((DoubleWritable)w).toDouble());
		if (w instanceof IntWritable)
			return new IntWritable(((IntWritable)w).toInt());
		if (w instanceof FloatWritable)
			return new FloatWritable(((FloatWritable)w).toFloat());		
		return w;
	}

}
