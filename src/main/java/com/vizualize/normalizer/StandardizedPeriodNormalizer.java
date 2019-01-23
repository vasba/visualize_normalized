package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

public class StandardizedPeriodNormalizer implements PeriodNormalizer {
	
	public static DataNormalization getNormalizer(INDArray array) {
		double mean = array.meanNumber().doubleValue();
		double std = array.stdNumber().doubleValue();
//		double std = 1.2148705128382802;
		int firstDim = array.size(0);
//		firstDim = 3;
		INDArray featureMean = createParameterArray(firstDim, mean);
		INDArray featureStd = createParameterArray(firstDim, std);
//		double[] means = new double[firstDim]; 
//		for (int i = 0;i<firstDim;i++)
//			means[i] = mean;
////			{mean, mean, mean};
//		INDArray featureMean = Nd4j.create(means,new int[]{3,1});
//		double[] stds = {std, std, std};
//		INDArray featureStd = Nd4j.create(stds,new int[]{3,1});
		NormalizerStandardize normalizer = new NormalizerStandardize(featureMean, featureStd);
		return normalizer;
	}
	
	private static INDArray createParameterArray(int dim, double value) {		
		double[] values = new double[dim]; 
		for (int i = 0;i<dim;i++)
			values[i] = value;
		return Nd4j.create(values,new int[]{dim,1});
		
	}

}
