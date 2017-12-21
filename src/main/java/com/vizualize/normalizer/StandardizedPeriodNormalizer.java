package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

public class StandardizedPeriodNormalizer implements PeriodNormalizer {
	
	public static DataNormalization getNormalizer(INDArray array) {
		double mean = array.meanNumber().doubleValue();
		double std = array.stdNumber().doubleValue();
		double[] means = {mean, mean, mean};
		INDArray featureMean = Nd4j.create(means,new int[]{3,1});
		double[] stds = {std, std, std};
		INDArray featureStd = Nd4j.create(stds,new int[]{3,1});
		NormalizerStandardize normalizer = new NormalizerStandardize(featureMean, featureStd);
		return normalizer;
	}

}
