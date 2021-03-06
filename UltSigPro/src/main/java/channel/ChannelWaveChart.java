package channel;

import java.util.Iterator;
import java.util.LinkedList;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ChannelWaveChart extends Pane {
	
	private int count;
	private int seconds = 20;
	private int samplingFreq = 44100;
	
	private int verticalSize;
	private int horizontalSize;
	private int maxSliceWidth = 50;
	private int slices;
	private int sliceCount;
	private int pixelsPerBar = 3;
	private int samplesPerBar;
	private int barsPerSlice;
	private int barCount;
	
	private double rms;
	private final double sqrt2 = Math.sqrt(2);
	
	private Pane curPane;
	
	private boolean play;
	private boolean firstLayout = true;
	
	private HBox hBox = new HBox();
	
	public ChannelWaveChart () {
		widthProperty().addListener(new ResizeListener());
		heightProperty().addListener(new ResizeListener());
		getChildren().add(hBox);
	}
	
	public synchronized void setPlay(boolean play) {
		this.play = play;
		hBox.getChildren().clear();
		if(play) {
			curPane = null;
			sliceCount = 0;
			sliceCount = 0;
			barCount = 0;
			rms = 0;
			
			executeResize();
		}
	}
	
	public synchronized void insertData(int[] data) {
		
		try{
			for(int i=0; i<data.length; i++) {
				if(curPane == null) {
					curPane = new Pane();
					curPane.setPrefHeight(verticalSize);
					curPane.setPrefWidth(barsPerSlice * pixelsPerBar);
					barCount = 0;
					
					Platform.runLater(new SliceAddRunnable(curPane));
				}
				
				double normValue = ((double)data[i]) / Short.MAX_VALUE;
				rms += normValue * normValue;

				count++;
				
				if(count>=samplesPerBar) {
					count = 0;
					
					rms /= (double) samplesPerBar;
					rms = Math.sqrt(rms) / sqrt2;;
					
					int length;				
					double log = Math.log10(rms);
					
					if(log > 0) {
						log = 0;
					} else if(log < -2) {
						log = -2;
					}
					
					log = (log + 2)/2;
					length = (int) (1.1 * log * verticalSize);
					
					length = length > verticalSize ? verticalSize : length;
					
					int offset = (verticalSize - length) / 2;
					
					//Rectangle rect = new Rectangle(barCount*pixelsPerBar, offset, pixelsPerBar, length);;
					Line line = new Line(barCount*pixelsPerBar + 1, offset, barCount*pixelsPerBar + 1, offset + length);		
					
					Platform.runLater(new DrawRunnalbe(curPane, line));
					
					rms = 0;
					barCount++;
					if(barCount >= barsPerSlice) {
						curPane = null;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	private synchronized void resize() {
		executeResize();
	}
	
	private void executeResize() {
		horizontalSize = (int) getWidth();
		
		if(getHeight() > verticalSize) {
			verticalSize = (int) getHeight();
		}
		
		if(verticalSize == 0 || horizontalSize == 0) {
			return;
		}
		
		if(play) {
			slices = horizontalSize / (barsPerSlice * pixelsPerBar);
		} else {
			slices = ((int)((double) horizontalSize) / maxSliceWidth) + 1;			
		}
		
		ObservableList<Node> nodes = hBox.getChildren();
		if(nodes.size() > slices && play) {
			nodes.remove(0, nodes.size() - slices);
			sliceCount = nodes.size();
		} else if(!play) {
			nodes.clear();
			sliceCount = 0;
		}
		
		if(!play) {
			barsPerSlice = horizontalSize / (slices * pixelsPerBar);			
			samplesPerBar = (seconds * samplingFreq) / (slices * barsPerSlice);
		}
		
		System.out.println("VerticalSize: " + verticalSize);
		System.out.println("HorizontalSize: " + horizontalSize);
		System.out.println("Slices: " + slices);
		System.out.println("SamplesPerBar: " + samplesPerBar);
		System.out.println("BarsPerSlice: " + barsPerSlice);
	}
	
	private synchronized boolean checkSliceRemove(Pane slice) {
		if(sliceCount<slices) {
			hBox.getChildren().add(slice);
			sliceCount++;
			return false;
		}
		hBox.getChildren().remove(0);
		hBox.getChildren().add(slice);
		return true;
	}
	
	private class ResizeListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			resize();				
		}
		
	}
	
	private class DrawRunnalbe implements Runnable {

		private Line line;
		private Pane curPane;
		
		public DrawRunnalbe(Pane curPane, Line line) {
			this.line = line;
			this.curPane = curPane;
		}
		
		@Override
		public void run() {
			curPane.getChildren().add(line);
		}
		
	}
	
	private class SliceAddRunnable implements Runnable {

		private Pane slice;
		
		public SliceAddRunnable(Pane slice) {
			this.slice = slice;
		}
		
		@Override
		public void run() {
			checkSliceRemove(slice);
		}
		
	}
}
