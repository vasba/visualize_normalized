package com.vizualize.normalizer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.AbstractDataSetNormalizer;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

public class NormalizedDataSet extends DataSet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    AbstractDataSetNormalizer normalizer;
    
    public NormalizedDataSet(INDArray first, INDArray second, AbstractDataSetNormalizer normalizer) {
        super(first, second);
        this.normalizer = normalizer;
    }
    
    
    @Override
    public void normalize() {
        INDArray da = this.getFeatures();       
        INDArray dsl = this.getLabels();
        INDArray mergedData =  Nd4j.toFlattened(da, dsl);
        normalizer = (NormalizerStandardize) StandardizedPeriodNormalizer.getNormalizer(mergedData);

        if (normalizer != null) {
            normalizer.transform(da);
            normalizer.transform(dsl);
        }
    }
}
