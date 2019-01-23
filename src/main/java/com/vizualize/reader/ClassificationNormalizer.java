package com.vizualize.reader;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

import com.vizualize.normalizer.MinMaxPeriodNormalizer;
import com.vizualize.normalizer.NormalizedDataSet;
import com.vizualize.normalizer.StandardizedPeriodNormalizer;

public class ClassificationNormalizer extends DataSetNormalizer {

    @Override
    public void preProcess(DataSet toPreProcess) {
        INDArray da = toPreProcess.getFeatures();       
        INDArray mergedData =  Nd4j.toFlattened(da);
//        normalizer = (NormalizerStandardize) StandardizedPeriodNormalizer.getNormalizer(mergedData);
        normalizer = (NormalizerMinMaxScaler) MinMaxPeriodNormalizer.getNormalizer(mergedData);
        normalizer.transform(da);
        
        toPreProcess = new NormalizedDataSet(toPreProcess.getFeatures(), toPreProcess.getLabels(), normalizer);
//        Object obj = da.getDouble(1);        
        boolean waitHere = true;
    }
}
