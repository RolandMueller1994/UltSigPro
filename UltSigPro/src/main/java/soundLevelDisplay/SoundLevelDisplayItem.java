package soundLevelDisplay;

import java.util.LinkedList;

import channel.Channel;
import javafx.scene.control.ProgressBar;
import channel.InputDataListener;
import channel.OutputDataSpeaker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class SoundLevelDisplayItem extends GridPane {

	private static Label deviceNameField;
	private static ProgressBar soundLevel;

	public SoundLevelDisplayItem(String deviceName) {

		deviceNameField = new Label(deviceName);
		soundLevel = new ProgressBar(0.1);
		soundLevel.setStyle("-fx-accent: green");
		this.setVgap(5);
		this.add(deviceNameField, 0, 0);
		this.add(soundLevel, 0, 1);
	}
	
	public void  setSoundLevel(LinkedList<Integer> soundValues) {
		
		double maxValue = 0;
		
		// look for the max value
		for (int i=0; i<soundValues.size(); i++) {
			if (maxValue < soundValues.get(i)) {
				maxValue = soundValues.get(i);
			}
		}
		
		// norm the max value
		// minimum is -40dB
		maxValue = 20*Math.log10(maxValue/Short.MAX_VALUE);
		if (maxValue < -20) {
			maxValue = -20;
		}
		soundLevel.setProgress((20+maxValue)/20);
	}

}
