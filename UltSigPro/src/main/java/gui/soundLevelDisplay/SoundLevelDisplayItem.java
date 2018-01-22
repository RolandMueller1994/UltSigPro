package gui.soundLevelDisplay;

import java.util.LinkedList;

import javafx.scene.control.ProgressBar;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class SoundLevelDisplayItem extends GridPane {

	private Label deviceNameField;
	private ProgressBar soundLevelBar;
	private final int itemWidth = 120;

	private LinkedList<Integer> internalBuffer = new LinkedList<>();

	private AnimationTimer animationTimer;

	public SoundLevelDisplayItem(String deviceName, LinkedList<LinkedList<Integer>> dataQueue) {

		deviceNameField = new Label(deviceName);
		deviceNameField.setPrefWidth(itemWidth);
		deviceNameField.setMaxWidth(itemWidth);

		soundLevelBar = new ProgressBar(0.1);
		soundLevelBar.setStyle("-fx-accent: -usp-dark-grey");
		soundLevelBar.setPrefWidth(itemWidth);
		soundLevelBar.setMaxWidth(itemWidth);

		this.setVgap(5);
		this.setHgap(3);
		this.add(deviceNameField, 0, 0);
		this.add(soundLevelBar, 0, 1);

		animationTimer = new AnimationTimer() {

			@Override
			public void handle(long now) {
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
	}

	/**
	 * Calculates the level of the progress bar for the last 2500 values. Scales
	 * it logarithmic: -30dB equals 0% and 0dB equals 100%. Colors the bar red,
	 * if the value is greater than -3dB.
	 * 
	 * @param soundValues
	 */
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
			soundLevelBar.setProgress((30 + maxValue) / 30);

			if (maxValue > -3) {
				soundLevelBar.setStyle("-fx-accent: red");
			} else {
				soundLevelBar.setStyle("-fx-accent: -usp-dark-grey");
			}
			internalBuffer.clear();
		}
	}

	/**
	 * Starts or stops the timer for the animation of the progress bars.
	 * 
	 * @param play
	 *            start (true) or stop (false)
	 */
	public void setPlay(boolean play) {

		internalBuffer.clear();

		if (play) {
			animationTimer.start();
		} else {
			animationTimer.stop();
		}
	}
}
