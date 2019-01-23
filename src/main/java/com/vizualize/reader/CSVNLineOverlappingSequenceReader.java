package com.vizualize.reader;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.datavec.api.records.SequenceRecord;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataLineInterval;
import org.datavec.api.records.reader.impl.csv.CSVNLinesSequenceRecordReader;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;


public class CSVNLineOverlappingSequenceReader extends CSVNLinesSequenceRecordReader {
	
    public static double min;
    public static double max;
    
	public CSVNLineOverlappingSequenceReader() {
		super();
	}

	public CSVNLineOverlappingSequenceReader(int nLinesPerSequence, int loopForwardPeriod,
			int skipNumLines, String delimiter, boolean forPlotting, int closeIndex,
			boolean classification) {
		super(nLinesPerSequence + loopForwardPeriod, skipNumLines, delimiter);
		this.nLinesPerSequence = nLinesPerSequence;
		this.forPlotting = forPlotting;
		this.closeIndex = closeIndex;
		this.loopforwardPeriod = loopForwardPeriod;
		this.classification = classification;
	}

	public CSVNLineOverlappingSequenceReader(int nLinesPerSequence, int loopForwardPeriod, 
			boolean forPlotting, int closeIndex, boolean classification) {
		super(nLinesPerSequence + loopForwardPeriod);
		this.nLinesPerSequence = nLinesPerSequence;
		this.forPlotting = forPlotting;
		this.closeIndex = closeIndex;
		this.loopforwardPeriod = loopForwardPeriod;
		this.classification = classification;
	}

	List<List<Writable>> lastSequenceRecord = null;
	int nLinesPerSequence;
	int loopforwardPeriod;
	boolean forPlotting;
	boolean classification;
	int closeIndex;
	String writableType = "";
	String dateStr;

	public String getDateStr() {
        return dateStr;
    }

    public void setWritableType(String writableType) {
        this.writableType = writableType;
    }

    @Override
    public List<Writable> next() {
	    List<Writable> res = super.next();
	    if (writableType.equals("double")) {
	        List<Writable> finalList = new ArrayList<>();
	        for (Writable w : res) {
	            String s = w.toString();
	            try {
	                double dv = Double.parseDouble(s);
	                DoubleWritable nw = new DoubleWritable(dv);
	                finalList.add(nw);
	            } catch (Exception e) {
	                finalList.add(w);
                }
	            
	        }
	        return finalList;
	    }
	    return res;
	}
	
	@Override
	public List<List<Writable>> sequenceRecord() {
	    if (lastSequenceRecord == null) {

	        if (!hasNext()) {
	            throw new NoSuchElementException("No next element");
	        }

	        List<List<Writable>> sequence = new ArrayList<>();
	        int count = 0;
	        while (count++ < nLinesPerSequence + loopforwardPeriod && hasNext()) {
	            sequence.add(next());
	        }

	        lastSequenceRecord = sequence;
	    } else {
			if (!super.hasNext()) {
				throw new NoSuchElementException("No next element");
			}
			lastSequenceRecord.remove(0);
			List<Writable> entry = next();
			lastSequenceRecord.add(entry);
		}
	    
        List<Writable> first = lastSequenceRecord.get(0);
        Writable date = first.get(0);
        dateStr = date.toString();
        
		if (forPlotting) {
			return lastSequenceRecord;
		} else {
			return flattenSequence(lastSequenceRecord);
		}
	}

    @Override
    public SequenceRecord nextSequence() {
		if (lastSequenceRecord == null) {
			return super.nextSequence();
		}

        List<List<Writable>> record = sequenceRecord();
        int lineBefore = lineIndex - nLinesPerSequence - loopforwardPeriod;
        int lineAfter = lineIndex;
        URI uri = (locations == null || locations.length < 1 ? null : locations[splitIndex]);
        RecordMetaData meta = new RecordMetaDataLineInterval(lineBefore, lineAfter - 1, uri,
                        CSVNLinesSequenceRecordReader.class);
        return new org.datavec.api.records.impl.SequenceRecord(record, meta);
    }
    
    private List<List<Writable>> flattenSequence(List<List<Writable>> sequence) {
    	ArrayList containerList = new ArrayList<>();
    	ArrayList<Writable> flattenList = new ArrayList<>();
    	
    	Iterator iter = sequence.iterator();
    	for (int i = 0; i<nLinesPerSequence;i++) {
    		ArrayList<Writable> list = (ArrayList<Writable>) sequence.get(i);
    		for (int j = 1; j<list.size(); j++)
    			flattenList.add(list.get(j));
    	}
    	Writable lastClose = (Writable) flattenList.get(nLinesPerSequence - 1);
    	List lastList = sequence.get(sequence.size()-1);
    	Writable label = (Writable) lastList.get(closeIndex);
//    	double percentageChange = (label.toDouble() - lastClose.toDouble())/lastClose.toDouble();
//    	label = new DoubleWritable(percentageChange*1000);
    	if (classification) {
    	    List lastForTrain = sequence.get(sequence.size()-loopforwardPeriod - 1);
    	    
    	    double difference = label.toDouble() - lastClose.toDouble();
    	    if (difference > 0) {
    	        label = new IntWritable(1);
    	    } else {
    	        label = new IntWritable(0);
    	    }
    	    int stop = 1;
    	}
    	
    	flattenList.add(label);
    	containerList.add(flattenList);
    	return containerList;
    }

    @Override
    public void reset() {
        super.reset();
        lastSequenceRecord = null;
    }
}
