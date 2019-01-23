package com.vizualize.quandl.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;

public class CustomizedListDatasetIterator extends ListDataSetIterator<DataSet> {
    
    private List<DataSet> list;

    public CustomizedListDatasetIterator(Collection<DataSet> coll, int batch) {
        super(coll, batch);
        list = new ArrayList<>(coll);                
    }
    
    @Override
    public DataSet next(int num) {               
        DataSet ds = super.next(num);
        if (num == 1) {
            int index = cursor()-1;
            return list.get(index);
        } else {
            return ds;
        }
    }
}
