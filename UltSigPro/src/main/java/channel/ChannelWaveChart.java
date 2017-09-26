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
	private int seconds = 120;
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
	private int curMax;
	private long avg;
	private int avgMax;
	private int avgMaxValue;
	private int avgCount;
	private int avgCount2;
	
	private Pane curPane;
	
	private boolean first;
	
	private HBox hBox = new HBox();
	
	public ChannelWaveChart () {
		widthProperty().addListener(new ResizeListener());
		heightProperty().addListener(new ResizeListener());
	}
	
	public synchronized void setPlay(boolean play) {
		first = play;
		if(play) {
			curPane = null;
			sliceCount = 0;
			curMax = 0;
			sliceCount = 0;
			barCount = 0;
			avg = 0;
			avgCount = 0;
			avgCount2 = 0;
			avgMaxValue = 0;
			avgMax = 0;
			ObservableList<Node> nodes = getChildren();
			Iterator<Node> iter = nodes.iterator();
			
			LinkedList<Node> removeList = new LinkedList<>();
			
			while(iter.hasNext()) {
				removeList.add(iter.next());
			}
			nodes.removeAll(removeList);
			
			hBox = new HBox();
			
			getChildren().add(hBox);
			
			executeResize();
		}
	}
	
	public synchronized void insertData(int[] data) {
		
		try{
			for(int i=0; i<data.length; i++) {
				if(curPane == null) {
					curPane = new Pane();
					curPane.setPrefHeight(verticalSize);
					curPane.setPrefWidth(horizontalSize/slices);
					barCount = 0;
					
					if(sliceCount>=slices) {
						Platform.runLater(new SliceAddRunnable(curPane, true));
					} else {
						Platform.runLater(new SliceAddRunnable(curPane, false));
						sliceCount++;
					}
				}
				
				if(data[i] > curMax) {
					curMax = data[i];
					//avg += data[i];
				} else if(-data[i] > curMax) {
					curMax = - data[i];
					//avg -= data[i];
				}
				
				if(data[i] > avgMaxValue) {
					avgMaxValue = data[i];
				} else if (-data[i] > avgMaxValue) {
					avgMaxValue = -data[i];
				}
				
				if(avgCount >= avgMax) {
					avg += avgMaxValue;
					avgMaxValue = 0;
					avgCount2++;
					avgCount = 0;
				}
				
				avgCount++;

				count++;
				
				if(count>=samplesPerBar) {
					count = 0;
					
					avg /= avgCount2;
					avgCount2 = 0;
					
					int length = (int) ((0.95 * ((double) avg) / Short.MAX_VALUE) * verticalSize);
					int offset = (verticalSize - length) / 2;
					
					System.out.println("Length: " + length + " Offset: " + offset + " CurMax: " + curMax + " Avg: " + avg);
					
					//Rectangle rect = new Rectangle(barCount*pixelsPerBar, offset, pixelsPerBar, length);;
					Line line = new Line(barCount*pixelsPerBar, offset, barCount*pixelsPerBar, offset + length);		
					
					Platform.runLater(new DrawRunnalbe(curPane, line));
					
					curMax = 0;
					avg = 0;
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
		verticalSize = (int) getHeight();
		horizontalSize = (int) getWidth();
		
		if(verticalSize == 0 || horizontalSize == 0) {
			return;
		}
		
		slices = ((int)((double) horizontalSize) / maxSliceWidth) + 1;
		
		ObservableList<Node> nodes = hBox.getChildren();
		if(nodes != null && nodes.size() > slices) {
			nodes.remove(0, nodes.size() - slices);
		}
		
		samplesPerBar = (seconds*samplingFreq) / (verticalSize * pixelsPerBar);
		barsPerSlice = horizontalSize / (slices * pixelsPerBar);
		avgMax = 100;
		
		System.out.println("VerticalSize: " + verticalSize);
		System.out.println("HorizontalSize: " + horizontalSize);
		System.out.println("Slices: " + slices);
		System.out.println("SamplesPerBar: " + samplesPerBar);
		System.out.println("BarsPerSlice: " + barsPerSlice);
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
		private boolean remove;
		
		public SliceAddRunnable(Pane slice, boolean remove) {
			this.slice = slice;
			this.remove = remove;
		}
		
		@Override
		public void run() {
			if(remove) {
				hBox.getChildren().remove(0);
				hBox.getChildren().add(slice);
			} else {
				hBox.getChildren().add(slice);
			}
		}
		
	}
}
