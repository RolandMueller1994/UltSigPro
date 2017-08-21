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
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ChannelWaveChart extends Pane {
	
	private long count;
 	
 	LinkedList<Double> data = new LinkedList<> ();
	
	public ChannelWaveChart () {
		super();
		
	}
	
	private synchronized void plotTime() {
		
	}
	
	public synchronized void setPlay(boolean play) {

	}
	
	public synchronized void insertData(LinkedList<Double> data) {
		this.data.addAll(data);
	}
}
