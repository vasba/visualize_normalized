package com.vizualize.plot;

import java.awt.BasicStroke;
import java.awt.Stroke;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.nd4j.linalg.api.ndarray.INDArray;

public class Plot {

    //Plot the data
    public static void plot(final INDArray x, final INDArray y, final INDArray predicted, int step) {
    	String title = "Classification Example - " + step;
        plot(x, y, predicted, title);
    }
    
    public static void plot(final double[] x, final int step) {
    	String title = "Example - " + step;
    	final XYSeriesCollection dataSet = new XYSeriesCollection();
    	addSeries(dataSet, x, "Data", 0);
    	plot(dataSet, title);
    }
    
    public static void plot(final double[]... sets) {
    	String title = "Multiple sets";
    	final XYSeriesCollection dataSet = new XYSeriesCollection();
    	int index = 1;
    	for (double[] x : sets) {
    		addSeries(dataSet, x, "Data " + index, 0);
    		index++;
    	}
    	plot(dataSet, title);
    }
    
    public static void plot(final INDArray x, final INDArray y, final INDArray predicted, String title) {
        final XYSeriesCollection dataSet = new XYSeriesCollection();
        addSeries(dataSet,x,"Features", 0);
        addSeries(dataSet,y,"Labels", x.size(1));

//        for( int i=0; i<predicted.length; i++ ){
        if(predicted != null) {
        	addSeries(dataSet,predicted,"Predicted", x.size(1));
        }

        plot(dataSet, title);
    }
    
    public static void plot(final XYSeriesCollection dataSet, final String title) {
    	final JFreeChart chart = ChartFactory.createXYLineChart(
                title,      // chart title
                "X",                        // x axis label
                "Y", // y axis label
                dataSet,                    // data
                PlotOrientation.VERTICAL,
                true,                       // include legend
                true,                       // tooltips
                false                       // urls
                );

        addDots(chart);
        ((NumberAxis)((XYPlot)chart.getPlot()).getRangeAxis(0)).setAutoRangeIncludesZero(false);
        
        final ChartPanel panel = new ChartPanel(chart);

        final JFrame f = new JFrame();
        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.pack();

        f.setVisible(true);
    }
    
    public static void addSeries(final XYSeriesCollection dataSet, final INDArray x, final String label, int startPosition){
        final double[] xd = x.data().asDouble();
//        final double[] yd = y.data().asDouble();
        addSeries(dataSet, xd, label, startPosition);
    }
    
    public static void addSeries(final XYSeriesCollection dataSet, final double[] x, final String label, int startPosition){
    	final XYSeries s = new XYSeries(label);
        for( int j=startPosition; j<x.length + startPosition; j++ ) s.add(j,x[j-startPosition]);
        
        dataSet.addSeries(s);
    }
    
    public static void addDots(JFreeChart chart) {
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        XYDataset dataset = plot.getDataset();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(){
//            Stroke soild = new BasicStroke(2.0f);
            Stroke dashed =  new BasicStroke(1.0f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {10.0f}, 0.0f);
            @Override
            public Stroke getItemStroke(int row, int column) {
                if (row == 2){
                    double x = dataset.getXValue(row, column);
//                    if ( x > 4){
                        return dashed;
//                    } else {
//                        return soild;
//                    } 
                } else
                    return super.getItemStroke(row, column);
            }
        };

        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
        renderer.setDefaultStroke(new BasicStroke(3));
        
        XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator()
        {
            public String generateToolTip(XYDataset dataset, int series, int item)
            {
                Number x1 = dataset.getX(series, item);
                Number y1 = dataset.getY(series, item);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("<html><p style='color:#0000ff;'>Serie: '%s'</p>", dataset.getSeriesKey(series)));
                stringBuilder.append(String.format("X:'%d'<br/>", x1.intValue()));
                stringBuilder.append(String.format("Y:'%1$,.6f'", y1.doubleValue()));
                stringBuilder.append("</html>");
                return stringBuilder.toString();
            }
        };
        renderer.setDefaultToolTipGenerator(xyToolTipGenerator);
        plot.setRenderer(renderer);
        
    }
    
    public static void plotHistogram(final INDArray x, int number) {
    	final double[] xd = x.data().asDouble();
    	plotHistogram(xd, number);
    }
    
    public static void plotHistogram(double[] xd, int number) {
    	HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.FREQUENCY);
        dataset.addSeries("Histogram",xd,number);
        String plotTitle = "Histogram"; 
        String xaxis = "number";
        String yaxis = "value"; 
        PlotOrientation orientation = PlotOrientation.VERTICAL; 
        boolean show = false; 
        boolean toolTips = false;
        boolean urls = false; 
        JFreeChart chart = ChartFactory.createHistogram( plotTitle, xaxis, yaxis, 
                 dataset, orientation, show, toolTips, urls);
        
        final ChartPanel panel = new ChartPanel(chart);

        final JFrame f = new JFrame();
        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.pack();

        f.setVisible(true);
    }
}
