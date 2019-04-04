package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;

public class MinMaxPeriodNormalizer implements PeriodNormalizer {
	
//	static double offset = 0.0002;
	static double offset = 0.00;

	public static DataNormalization getNormalizer(INDArray array) {
		NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler();
		double min = array.minNumber().doubleValue();		
		double max = array.maxNumber().doubleValue();
		min = min * (1 - offset);
		max = max * (1 + offset);
		double[] mins = {min, min, min};
		INDArray featureMin = Nd4j.create(mins,new int[]{3,1});
		double[] maxes = {max, max, max};
		INDArray featureMax = Nd4j.create(maxes,new int[]{3,1});
		
//		double[] mins = {min};
//        INDArray featureMin = Nd4j.create(mins,new int[]{1,1});
//        double[] maxes = {max};
//        INDArray featureMax = Nd4j.create(maxes,new int[]{1,1});        
		normalizer.setFeatureStats(featureMin, featureMax);
		return normalizer;
	}

}
