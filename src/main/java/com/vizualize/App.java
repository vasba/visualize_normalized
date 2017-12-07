package com.vizualize;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.codehaus.janino.Java.ArrayLength;
import org.datavec.api.split.FileSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.CategoricalColumnCondition;
import org.datavec.api.transform.condition.column.DoubleColumnCondition;
import org.datavec.api.transform.filter.ConditionFilter;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.transform.column.DuplicateColumnsTransform;
import org.datavec.api.transform.transform.sequence.SequenceOffsetTransform.OperationType;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.comparator.TextWritableComparator;
import org.datavec.spark.transform.SparkTransformExecutor;
import org.datavec.spark.transform.misc.StringToWritablesFunction;
import org.datavec.spark.transform.misc.WritablesToStringFunction;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import com.vizualize.reader.CSVNLineOverlappingSequenceReader;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

/**
 * Hello world!
 *
 */
public class App extends Application
{
	static SequenceRecordReaderDataSetIterator dataIterator = null;

    public static void main( String[] args )
    {
    	Schema inputDataSchema = new Schema.Builder()
    			.addColumnString("Trade Date")
    			.addColumnsDouble("Index Value","High", "Low", "Total Market Value", "Dividend Market Value")
    			.build();

    	TransformProcess tp = new TransformProcess.Builder(inputDataSchema)
    			//TO DO filter out zero values in columns for index value, high and low
    			.filter(new ConditionFilter(
    					new CategoricalColumnCondition("High", ConditionOp.InSet, new HashSet((Arrays.asList("High", ""))))))
    			.filter(new ConditionFilter(new DoubleColumnCondition("High", ConditionOp.Equal, 0)))
    			.calculateSortedRank("testSort", "Trade Date", new TextWritableComparator())
    			.removeColumns("Trade Date", "Total Market Value", "testSort")
    			.build();
    	CSVNLineOverlappingSequenceReader reader = new CSVNLineOverlappingSequenceReader(20);
    	SparkConf conf = new SparkConf();
    	conf.setMaster("local[*]");
    	conf.setAppName("DataVec Example");
    	JavaSparkContext sc = new JavaSparkContext(conf);
    	String path = args[0];
    	String pathProcessed = args[1]; 
    	JavaRDD<String> stringData = sc.textFile(path);

    	JavaRDD<List<Writable>> parsedInputData = stringData.map(new StringToWritablesFunction(reader));

    	//Now, let's execute the transforms we defined earlier:
        JavaRDD<List<Writable>> processedData = SparkTransformExecutor.execute(parsedInputData, tp);

        //For the sake of this example, let's collect the data locally and print it:
        JavaRDD<String> processedAsString = processedData.map(new WritablesToStringFunction(","));

        List<String> processedCollected = processedAsString.collect();
        String[] lines = processedCollected.toArray(new String[processedCollected.size()]);
        writeToCsv(pathProcessed, lines);
    	try {
    		File file = new File(pathProcessed);
			reader.initialize(new FileSplit(file));
			dataIterator = new SequenceRecordReaderDataSetIterator(reader, 1, 1, -1, true);
			getAndScaleData(1);
			launch(args);
			int breakIt = 1;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void getAndScaleData(int period) {
    	int count = 1;
    	if (dataIterator.hasNext()) {
    		DataSet dataSequence = dataIterator.next();
    		while (count++ < period && dataIterator.hasNext()) {
    			dataSequence = dataIterator.next();
    		}

    		INDArray unscaledFeatures = dataSequence.getFeatures().dup();
    		//We need to normalize our data. We'll use NormalizeStandardize (which gives us mean 0, unit variance):
    		INDArray da = dataSequence.getFeatures();
//    		DataNormalization normalizer = getMinMaxScaler(da);
    		DataNormalization normalizer = getStandardizedNormalizer(da);
    		normalizer.transform(da);
    		addScaledSeries(da);
    		addUnscaledSeries(unscaledFeatures);
    	}
    }

    public static void addScaledSeries(INDArray array) {
    	// first row
    	INDArray firstRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(0));
    	closeSeries.getData().remove(0, closeSeries.getData().size());
    	addDataItems(closeSeries, firstRow);
    	// second row
    	INDArray secondRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(1));
    	highSeries.getData().remove(0, highSeries.getData().size());
    	addDataItems(highSeries, secondRow);
    	// third row
    	INDArray thirdRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(2));
    	lowSeries.getData().remove(0, lowSeries.getData().size());
    	addDataItems(lowSeries, thirdRow);
    }

    public static void addUnscaledSeries(INDArray array) {
    	// first row
    	INDArray firstRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(0));
    	unscaledCloseSeries.getData().remove(0, unscaledCloseSeries.getData().size());
    	addDataItems(unscaledCloseSeries, firstRow);
    	// second row
    	INDArray secondRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(1));
    	unscaledHighSeries.getData().remove(0, unscaledHighSeries.getData().size());
    	addDataItems(unscaledHighSeries, secondRow);
    	// third row
    	INDArray thirdRow = array.get(NDArrayIndex.all(), NDArrayIndex.point(2));
    	unscaledLowSeries.getData().remove(0, unscaledLowSeries.getData().size());
    	addDataItems(unscaledLowSeries, thirdRow);
    }

    public static DataNormalization getMinMaxScaler(INDArray array) {
		NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler();
		double min = array.minNumber().doubleValue();
		double max = array.maxNumber().doubleValue();
		double[] mins = {min, min, min};
		INDArray featureMin = Nd4j.create(mins,new int[]{3,1});
		double[] maxes = {max, max, max};
		INDArray featureMax = Nd4j.create(maxes,new int[]{3,1});
		normalizer.setFeatureStats(featureMin, featureMax);
		return normalizer;
    }

    public static DataNormalization getStandardizedNormalizer(INDArray array) {
		double mean = array.meanNumber().doubleValue();
		double std = array.stdNumber().doubleValue();
		double[] means = {mean, mean, mean};
		INDArray featureMean = Nd4j.create(means,new int[]{3,1});
		double[] stds = {std, std, std};
		INDArray featureStd = Nd4j.create(stds,new int[]{3,1});
		NormalizerStandardize normalizer = new NormalizerStandardize(featureMean, featureStd);
		return normalizer;
    }

    public static void writeToCsv(String path, String[] content) {
    	java.io.File destinationCSV = new java.io.File(path);
    	try {
			java.io.PrintWriter outfile = new java.io.PrintWriter(destinationCSV);
			int size = content.length;
			int index = 0;
			for (String line : content) {
				if (index != size){
					outfile.write(line + "\n");
					index++;
				}
	    	}
			outfile.flush();
			outfile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    final XYChart.Series<String, Number> series = new XYChart.Series<>();
    final static XYChart.Series<Number, Number> closeSeries = new XYChart.Series<>();
    final static XYChart.Series<Number, Number> highSeries = new XYChart.Series<>();
    final static XYChart.Series<Number, Number> lowSeries = new XYChart.Series<>();

    final static TextField txtField = new TextField();
    final static Button btn = new Button("Next Frame");


    final static XYChart.Series<Number, Number> unscaledCloseSeries = new XYChart.Series<>();
    final static XYChart.Series<Number, Number> unscaledHighSeries = new XYChart.Series<>();
    final static XYChart.Series<Number, Number> unscaledLowSeries = new XYChart.Series<>();


    final static NumberAxis xAxis1 = new NumberAxis();
    final static NumberAxis yAxis1 = new NumberAxis();
//    final static NumberAxis yAxis2 = new NumberAxis(750,1000,10);
    final static NumberAxis yAxis2 = new NumberAxis();
    final static LineChart<Number, Number> lineChartScaled
    = new LineChart<>(xAxis1, yAxis1);

    final static LineChart<Number, Number> lineChartUnscaled
    = new LineChart<>(xAxis1, yAxis2);

    final static FlowPane flowPane = new FlowPane();

    @Override
    public void start(Stage stage) {
        simpleIndexChart(stage);
    }

    public void simpleIndexChart(Stage stage) {
        stage.setTitle("OMXS30 Stock Index Chart");
        lineChartScaled.setTitle("Scaled OMXS30 Stock Index values");
        yAxis1.setLabel("Price");
        yAxis1.setForceZeroInRange(false);

        lineChartUnscaled.setTitle("Unscaled OMXS30 Stock Index values");
        yAxis2.setLabel("Price");
        yAxis2.setForceZeroInRange(false);

        flowPane.getChildren().addAll(lineChartScaled, lineChartUnscaled,
        		txtField, btn);

        btn.setOnAction((ActionEvent e) -> nextFrameAction(e));

        Scene scene = new Scene(flowPane, 1000, 500);

        closeSeries.setName("Close");
        highSeries.setName("High");
        lowSeries.setName("Low");
        lineChartScaled.getData().add(closeSeries);
        lineChartScaled.getData().add(highSeries);
        lineChartScaled.getData().add(lowSeries);

        unscaledCloseSeries.setName("Close");
        unscaledHighSeries.setName("High");
        unscaledLowSeries.setName("Low");
        lineChartUnscaled.getData().add(unscaledCloseSeries);
        lineChartUnscaled.getData().add(unscaledHighSeries);
        lineChartUnscaled.getData().add(unscaledLowSeries);

        stage.setScene(scene);
        stage.show();

    }

    public static void nextFrameAction(ActionEvent e) {
    	if ((txtField.getText() != null && !txtField.getText().isEmpty())) {
    		String framesStr = txtField.getText();
    		int frames = Integer.parseInt(framesStr);
    		getAndScaleData(frames);
    		flowPane.requestLayout();
    	}
    }

    public static void addDataItem(XYChart.Series<Number, Number> series,
    		Number x, Number y) {
        series.getData().add(new XYChart.Data<>(x, y));
    }

    public static void addDataItems(XYChart.Series<Number, Number> series, INDArray data) {
    	DataBuffer dbuff = data.data();
		double size = data.size(1);
		for (int i = 0;i<size;i++)
			addDataItem(series, i, data.getDouble(i));
    }

}
