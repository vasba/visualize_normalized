package com.vizualize.reader;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.preprocessor.AbstractDataSetNormalizer;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

import com.vizualize.normalizer.MinMaxPeriodNormalizer;
import com.vizualize.normalizer.NormalizedDataSet;
import com.vizualize.normalizer.StandardizedPeriodNormalizer;

public class DataSetNormalizer implements DataSetPreProcessor {
    
    AbstractDataSetNormalizer normalizer;

	public AbstractDataSetNormalizer getNormalizer() {
        return normalizer;
    }

    @Override
	public void preProcess(DataSet toPreProcess) {
		INDArray da = toPreProcess.getFeatures();		
		INDArray dsl = toPreProcess.getLabels();
//		INDArray daCopy = toPreProcess.getFeatures().dup();
		INDArray mergedData =  Nd4j.toFlattened(da);
//		normalizer = (NormalizerStandardize) StandardizedPeriodNormalizer.getNormalizer(mergedData);
		
//		INDArray mergedData1 =  Nd4j.toFlattened(da, dsl);
//		AbstractDataSetNormalizer normalizerN = (NormalizerStandardize) StandardizedPeriodNormalizer.getNormalizer(mergedData1);
		normalizer = (NormalizerMinMaxScaler) MinMaxPeriodNormalizer.getNormalizer(mergedData);
		normalizer.transform(da);
//		Object obj = da.getDouble(1);
//		normalizer.transform(dsl);
//		double value = dsl.getDouble(0); 
//		if (value > 1) {
//			value = 0.999;
//		} else if (value < 0) {
//			value = 0.001;
//		}
//		dsl.putScalar(0, value);
		toPreProcess = new NormalizedDataSet(toPreProcess.getFeatures(), toPreProcess.getLabels(), normalizer);
		boolean waitHere = true;
	}
}
