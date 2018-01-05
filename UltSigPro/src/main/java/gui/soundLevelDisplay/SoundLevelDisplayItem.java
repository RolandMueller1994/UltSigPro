package gui.soundLevelDisplay;

import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.ProgressBar;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class SoundLevelDisplayItem extends GridPane {

	private Label deviceNameField;
	private ProgressBar soundLevelBar;
	private ProgressBar overdriveIndicator;
	private LinkedList<LinkedList<Integer>> dataQueue;

	private LinkedList<Integer> internalBuffer = new LinkedList<>();
	private ScheduledThreadPoolExecutor executor;

	public SoundLevelDisplayItem(String deviceName, LinkedList<LinkedList<Integer>> dataQueue) {

		deviceNameField = new Label(deviceName);
		deviceNameField.setPrefWidth(120);
		deviceNameField.setMaxWidth(120);
		soundLevelBar = new ProgressBar(0.1);
		soundLevelBar.setStyle("-fx-accent: -usp-dark-grey");
		overdriveIndicator = new ProgressBar(0);
		overdriveIndicator.setStyle("-fx-accent: red");
		overdriveIndicator.setMaxWidth(10);
		this.setVgap(5);
		this.setHgap(3);
		this.add(deviceNameField, 0, 0, 2, 1);
		this.add(soundLevelBar, 0, 1);
		this.add(overdriveIndicator, 1, 1);
		GridPane.setHalignment(soundLevelBar, HPos.CENTER);

		this.dataQueue = dataQueue;
	}

	private void setSoundLevel(LinkedList<Integer> soundValues) {

		internalBuffer.addAll(soundValues);

		int bufferSize = internalBuffer.size();
		if (bufferSize > 2500) {
			double maxValue = 0;
			// look for the max value
			for (int curValue : internalBuffer) {
				if (maxValue < curValue) {
					maxValue = curValue;
				}
			}

			// norm the max value
			// minimum is -30dB
			maxValue = 20 * Math.log10(maxValue / Short.MAX_VALUE);
			if (maxValue < -30) {
				maxValue = -30;
			}
			this.soundLevelBar.setProgress((30 + maxValue) / 30);
			if (maxValue > -3) {
				overdriveIndicator.setProgress(100);
			} else {
				overdriveIndicator.setProgress(0);
			}
			internalBuffer.clear();
		}
	}

	public void setPlay(boolean play) {

		internalBuffer.clear();

		if (play) {

			Runnable evaluationRunnable = new Runnable() {

				@Override
				public void run() {
					LinkedList<LinkedList<Integer>> data = new LinkedList<>();

					synchronized (dataQueue) {
						data.addAll(dataQueue);
						dataQueue.clear();
					}

					for (LinkedList<Integer> list : data) {
						setSoundLevel(list);
					}
				}

			};

			executor = new ScheduledThreadPoolExecutor(1);

			executor.scheduleAtFixedRate(evaluationRunnable, 0, 25, TimeUnit.MILLISECONDS);
		} else {
			executor.shutdownNow();
		}

	}

}
