package com.vizualize;


import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import com.vizualize.quandl.iterator.QuandlIterator;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;


public class App extends Application
{
	static Iterator<DataSet> dataIterator = null;

    public static void main( String[] args )
    {
    	SparkConf conf = new SparkConf();
    	conf.setMaster("local[*]");
    	conf.setAppName("DataVec Example");
    	JavaSparkContext sc = new JavaSparkContext(conf);
    	QuandlIterator qi = new QuandlIterator();
    	dataIterator = qi.getIterator("NASDAQOMX/OMXS30", sc.toSparkContext(sc));    	
    	getAndScaleData(1);
    	launch(args);
		qi.deleteTmpFile();
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
