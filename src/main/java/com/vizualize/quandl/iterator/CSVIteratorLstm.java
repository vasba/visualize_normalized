package com.vizualize.quandl.iterator;

public class CSVIteratorLstm extends CSVIterator {
	
	protected String[] columnsToRemove() {
    	return new String[]{"Date", "testSort"};
    }
}
