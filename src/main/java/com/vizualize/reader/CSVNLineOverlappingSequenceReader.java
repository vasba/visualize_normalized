package com.vizualize.reader;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import org.datavec.api.records.SequenceRecord;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataLineInterval;
import org.datavec.api.records.reader.impl.csv.CSVNLinesSequenceRecordReader;
import org.datavec.api.writable.Writable;

public class CSVNLineOverlappingSequenceReader extends CSVNLinesSequenceRecordReader {
	public CSVNLineOverlappingSequenceReader() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CSVNLineOverlappingSequenceReader(int nLinesPerSequence, int skipNumLines, String delimiter) {
		super(nLinesPerSequence, skipNumLines, delimiter);
		this.nLinesPerSequence = nLinesPerSequence;
		// TODO Auto-generated constructor stub
	}

	public CSVNLineOverlappingSequenceReader(int nLinesPerSequence) {
		super(nLinesPerSequence);
		this.nLinesPerSequence = nLinesPerSequence;
	}

	List<List<Writable>> lastSequenceRecord = null;
	int nLinesPerSequence;

	@Override
	public List<List<Writable>> sequenceRecord() {
		if (lastSequenceRecord == null) {
			lastSequenceRecord = super.sequenceRecord();
		} else {
			if (!super.hasNext()) {
				throw new NoSuchElementException("No next element");
			}
			lastSequenceRecord.remove(0);
			List<Writable> entry = super.next();
			lastSequenceRecord.add(entry);
		}
		return lastSequenceRecord;
	}

    @Override
    public SequenceRecord nextSequence() {
		if (lastSequenceRecord == null) {
			return super.nextSequence();
		}

        List<List<Writable>> record = sequenceRecord();
        int lineBefore = lineIndex-nLinesPerSequence;
        int lineAfter = lineIndex;
        URI uri = (locations == null || locations.length < 1 ? null : locations[splitIndex]);
        RecordMetaData meta = new RecordMetaDataLineInterval(lineBefore, lineAfter - 1, uri,
                        CSVNLinesSequenceRecordReader.class);
        return new org.datavec.api.records.impl.SequenceRecord(record, meta);
    }

    @Override
    public void reset() {
        super.reset();
        lastSequenceRecord = null;
    }
}
