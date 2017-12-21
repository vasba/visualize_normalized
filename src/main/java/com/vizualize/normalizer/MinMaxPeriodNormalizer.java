package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;

public class MinMaxPeriodNormalizer implements PeriodNormalizer {

	public static DataNormalization getNormalizer(INDArray array) {
		NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler();
		double min = array.minNumber().doubleValue();
		double max = array.maxNumber().doubleValue();
		double[] mins = {min, min, min};
		INDArray featureMin = Nd4j.create(mins,new int[]{3,1});
		double[] maxes = {max, max, max};
		INDArray featureMax = Nd4j.create(maxes,new int[]{3,1});
		normalizer.setFeatureStats(featureMin, featureMax);
		return normalizer;
	}

}
