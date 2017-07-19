package channel;

import java.util.LinkedList;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.util.Duration;

public class ChannelWaveChart {

	private long maxNumber = 1800; //44100*30;

	private NumberAxis timeAxis = new NumberAxis (0, maxNumber-1, 1);
	private NumberAxis yAxis = new NumberAxis (-1, 1, 1);
	private Series<Number, Number> series = new Series<> ();
	private Timeline timeline = new Timeline();
	
	private long count;
	
 	private LineChart<Number, Number> chart;
 	
 	LinkedList<Double> data = new LinkedList<> ();
	
	public ChannelWaveChart () {
		
		chart = new LineChart<Number, Number> (timeAxis, yAxis);
		
		chart.setAnimated(false);
		chart.setCreateSymbols(false);
		
		chart.setPrefHeight(100);
		chart.getYAxis().setTickLabelsVisible(false);
	    chart.getYAxis().setOpacity(0);
	    chart.getXAxis().setTickLabelsVisible(false);
	    chart.getXAxis().setOpacity(0);
	    chart.setLegendVisible(false);
		
		timeline.getKeyFrames()
        .add(new KeyFrame(Duration.millis(50), 
                (ActionEvent actionEvent) -> plotTime()));
		timeline.setCycleCount(Animation.INDEFINITE);
		
		chart.getData().add(series);
		
	}
	
	private synchronized void plotTime() {
		while(data.size() != 0) {
			double value = data.removeFirst();

			series.getData().add(new Data<Number, Number> (count, value));
			count++;
		}
		
		if(count > maxNumber) {
			timeAxis.setUpperBound(count);
			timeAxis.setLowerBound(count - maxNumber);
		}
	}
	
	public synchronized void setPlay(boolean play) {
		if(play) {
			count = 0;
			series.getData().clear();
			timeline.play();
		} else {
			timeline.stop();
		}
	}
	
	public synchronized void insertData(LinkedList<Double> data) {
		this.data.addAll(data);
	}
	
	public LineChart getWaveChart() {
		return chart;
	}
}
