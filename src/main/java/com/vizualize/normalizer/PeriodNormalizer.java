package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;

public interface PeriodNormalizer {
	
	public static DataNormalization getNormalizer(INDArray array){
		return null;
	}

}
