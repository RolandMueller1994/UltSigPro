package gui.soundLevelDisplay;

import java.util.LinkedList;

import channel.Channel;
import javafx.scene.control.ProgressBar;
import channel.InputDataListener;
import channel.OutputDataSpeaker;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class SoundLevelDisplayItem extends GridPane {

	private Label deviceNameField;
	private ProgressBar soundLevel;

	public SoundLevelDisplayItem(String deviceName) {

		deviceNameField = new Label(deviceName);
		soundLevel = new ProgressBar(0.1);
		soundLevel.setStyle("-fx-accent: green");
		this.setVgap(5);
		this.add(deviceNameField, 0, 0);
		this.add(soundLevel, 0, 1);
		GridPane.setHalignment(soundLevel, HPos.CENTER);
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
		// minimum is -30dB
		maxValue = 20*Math.log10(maxValue/Short.MAX_VALUE);
		if (maxValue < -30) {
			maxValue = -30;
		}
		this.soundLevel.setProgress((30+maxValue)/30);
		if (maxValue > -3)
			this.soundLevel.setStyle("-fx-accent: red");
		else if (maxValue > -6)
			this.soundLevel.setStyle("-fx-accent: orange");
		else
			this.soundLevel.setStyle("-fx-accent: green");
	}

}
