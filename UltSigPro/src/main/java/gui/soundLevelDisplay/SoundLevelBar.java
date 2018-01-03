package gui.soundLevelDisplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import channel.Channel;
import channel.ChannelConfig;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import resourceframework.ResourceProviderException;

public class SoundLevelBar extends GridPane implements SoundValueInterface {

	private static final String INPUT_TITLE = "inputTitle";
	private static final String OUTPUT_TITLE = "outputTitle";

	private static final int HEIGHT = 85;
	private static final double TITLE_HEIGHT = 27;

	private LanguageResourceHandler lanHandler;

	private static SoundLevelBar soundLevelBar;

	private GridPane inputDevicesBar;
	private GridPane outputDevicesBar;

	// HashMap<DeviceName, ChannelName>
	private HashMap<String, LinkedList<String>> inputDevicesList;
	private HashMap<String, LinkedList<LinkedList<Integer>>> inputQueues;
	private HashMap<String, LinkedList<String>> outputDevicesList;
	private HashMap<String, LinkedList<LinkedList<Integer>>> outputQueues;

	// HashMap<DeviceName, SoundDevice>
	private HashMap<String, SoundLevelDisplayItem> inputDeviceItems;
	private HashMap<String, SoundLevelDisplayItem> outputDeviceItems;

	public static SoundLevelBar getSoundLevelBar() {

		if (soundLevelBar == null) {
			soundLevelBar = new SoundLevelBar();
		}

		return soundLevelBar;
	}

	private SoundLevelBar() {

		try {
			lanHandler = LanguageResourceHandler.getInstance();

			inputDevicesList = new HashMap<>();
			outputDevicesList = new HashMap<>();

			inputDevicesBar = new GridPane();
			inputDevicesBar.getStyleClass().add("sound-level-bar");

			outputDevicesBar = new GridPane();
			outputDevicesBar.getStyleClass().add("sound-level-bar");

			TitledPane inputPane = new TitledPane();
			inputPane.setText(lanHandler.getLocalizedText(SoundLevelBar.class, INPUT_TITLE));
			ImageView inputIconImageView = new ImageView(new Image("file:icons/inputIconSmall.png"));
			inputPane.setCollapsible(false);
			inputPane.setGraphic(inputIconImageView);
			inputPane.setContent(inputDevicesBar);
			inputPane.setMaxWidth(Double.MAX_VALUE);
			inputPane.setMinHeight(HEIGHT);
			GridPane.setHgrow(inputPane, Priority.ALWAYS);
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					Pane inputHeader = (Pane) inputPane.lookup(".title");
					inputHeader.setPrefHeight(TITLE_HEIGHT);
				}
				
			});

			TitledPane outputPane = new TitledPane();
			outputPane.setText(lanHandler.getLocalizedText(SoundLevelBar.class, OUTPUT_TITLE));
			ImageView outputIconImageView = new ImageView(new Image("file:icons/outputIconSmall.png"));
			outputPane.setCollapsible(false);
			outputPane.setGraphic(outputIconImageView);
			outputPane.setContent(outputDevicesBar);
			outputPane.setMaxWidth(Double.MAX_VALUE);
			outputPane.setMinHeight(HEIGHT);
			GridPane.setHgrow(outputPane, Priority.ALWAYS);
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					Pane outputHeader = (Pane) outputPane.lookup(".title");
					outputHeader.setPrefHeight(TITLE_HEIGHT);
				}
				
			});
			
			ColumnConstraints inputCol = new ColumnConstraints();
			inputCol.setPercentWidth(50);
			ColumnConstraints outputCol = new ColumnConstraints();
			outputCol.setPercentWidth(50);
			
			getColumnConstraints().addAll(inputCol, outputCol);

			add(inputPane, 0, 0);
			add(outputPane, 1, 0);
			
			inputDeviceItems = new HashMap<>();
			outputDeviceItems = new HashMap<>();

			inputQueues = new HashMap<>();
			outputQueues = new HashMap<>();
		} catch (ResourceProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void updateSoundLevelItems(String deviceName, LinkedList<Integer> soundValues, boolean input) {

		if (input) {
			LinkedList<LinkedList<Integer>> queue = inputQueues.get(deviceName);
			synchronized (queue) {
				queue.add(soundValues);
			}
		} else {
			LinkedList<LinkedList<Integer>> queue = outputQueues.get(deviceName);
			synchronized (queue) {
				queue.add(soundValues);
			}
		}

	}

	/**
	 * Is called, when a {@linkplain Channel} gets created. Checks if there are
	 * already any entries for the selected devices in the Channel. For new
	 * entries appears a {@linkplain SoundLevelDisplayItem}.
	 * 
	 * @param config
	 */
	public synchronized void addNewChannelSoundDevices(ChannelConfig config) {

		for (String device : config.getInputDevices()) {

			// new input device entry
			if (!inputDevicesList.containsKey(device)) {
				inputDevicesList.put(device, new LinkedList<>());

				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();

				inputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				inputDevicesBar.addRow(0, inputDeviceItems.get(device));
				inputQueues.put(device, queue);
			}

			// add channel name to this device
			inputDevicesList.get(device).add(config.getName());
		}

		for (String device : config.getInputWaveFiles().keySet()) {
			
			// new input device entry
			if (!inputDevicesList.containsKey(device)) {
				inputDevicesList.put(device, new LinkedList<>());

				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();

				inputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				inputDevicesBar.addRow(0, inputDeviceItems.get(device));
				inputQueues.put(device, queue);
			}
		}

		for (String device : config.getOutputDevices()) {

			// new output device entry
			if (!outputDevicesList.containsKey(device)) {
				outputDevicesList.put(device, new LinkedList<>());

				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();

				outputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				outputDevicesBar.addRow(0, outputDeviceItems.get(device));
				outputQueues.put(device, queue);
			}

			// add channel name to this device
			outputDevicesList.get(device).add(config.getName());
		}
		
		for (String device : config.getOutputWaveFiles().keySet()) {
			
			// new output device entry
			if (!outputDevicesList.containsKey(device)) {
				outputDevicesList.put(device, new LinkedList<>());
				
				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();
				
				outputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				outputDevicesBar.addRow(0, outputDeviceItems.get(device));
				outputQueues.put(device, queue);
			}
		}

	}

	/**
	 * Is called, when a {@linkplain Channel} gets deleted. Checks if there are
	 * any others channels left, who share the same sound devices as the deleted
	 * channel. Removes unused {@linkplain SoundLevelDisplayItem}s.
	 * 
	 * @param config
	 */
	public void removeChannelSoundDevices(ChannelConfig config) {

		// remove the entry of the channel for this input device
		for (String device : config.getInputDevices()) {
			inputDevicesList.get(device).remove(config.getName());

			// remove the device if there are no longer any channels with this
			// device
			if (inputDevicesList.get(device).isEmpty()) {
				inputDevicesList.remove(device);
				inputDevicesBar.getChildren().remove(inputDeviceItems.get(device));
				inputQueues.remove(device);
				inputDeviceItems.remove(device);
			}
		}

		// remove the entry of the channel for this input device
		for (String device : config.getOutputDevices()) {
			outputDevicesList.get(device).remove(config.getName());

			// remove the device if there are no longer any channels with this
			// device
			if (outputDevicesList.get(device).isEmpty()) {
				outputDevicesList.remove(device);
				outputDevicesBar.getChildren().remove(outputDeviceItems.get(device));
				outputQueues.remove(device);
				outputDeviceItems.remove(device);
			}
		}
		
		for (String inputWaveFile : config.getInputWaveFiles().keySet()) {
			inputDevicesList.get(inputWaveFile).remove(config.getName());
			
			if(inputDevicesList.get(inputWaveFile).isEmpty()) {
				inputDevicesList.remove(inputWaveFile);
				inputDevicesBar.getChildren().remove(inputDeviceItems.get(inputWaveFile));
				inputQueues.remove(inputWaveFile);
				inputDeviceItems.remove(inputWaveFile);
			}
		}
		
		for (String outputWaveFile : config.getOutputWaveFiles().keySet()) {
			outputDevicesList.get(outputWaveFile).remove(config.getName());
			
			if(outputDevicesList.get(outputWaveFile).isEmpty()) {
				outputDevicesList.remove(outputWaveFile);
				outputDevicesBar.getChildren().remove(outputDeviceItems.get(outputWaveFile));
				outputQueues.remove(outputWaveFile);
				outputDeviceItems.remove(outputWaveFile);
			}
		}
	}
	
	public void addDeviceToChannel(String device, Channel channel, boolean input) {
		String channelName = channel.getChannelConfig().getName();
		
		if(input) {
			if(inputDevicesList.containsKey(device)) {
				if(!inputDevicesList.get(device).contains(channelName)) {
					inputDevicesList.get(device).add(channelName);
				}
			} else {
				LinkedList<String> channels = new LinkedList<>();
				channels.add(channelName);
				inputDevicesList.put(device, channels);
				
				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();

				inputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				inputDevicesBar.addRow(0, inputDeviceItems.get(device));
				inputQueues.put(device, queue);
			}
		} else {
			if(outputDevicesList.containsKey(device)) {
				if(!outputDevicesList.get(device).contains(channelName)) {
					outputDevicesList.get(device).add(channelName);
				}
			} else {
				LinkedList<String> channels = new LinkedList<>();
				channels.add(channelName);
				outputDevicesList.put(device, channels);

				LinkedList<LinkedList<Integer>> queue = new LinkedList<LinkedList<Integer>>();

				outputDeviceItems.put(device, new SoundLevelDisplayItem(device, queue));
				outputDevicesBar.addRow(0, outputDeviceItems.get(device));
				outputQueues.put(device, queue);
			}
		}
	}

	public void removeDeviceFromChannel(String device, Channel channel, boolean input) {
		String channelName = channel.getChannelConfig().getName();
		
		if(input) {
			if(inputDevicesList.containsKey(device)) {
				inputDevicesList.get(device).remove(channelName);
				
				if(inputDevicesList.get(device).isEmpty()) {
					inputDevicesList.remove(device);
					inputDevicesBar.getChildren().remove(inputDeviceItems.get(device));
					inputQueues.remove(device);
					inputDeviceItems.remove(device);
				}				
			}
		} else {
			if(outputDevicesList.containsKey(device)) {
				outputDevicesList.get(device).remove(channelName);
				
				if(outputDevicesList.get(device).isEmpty()) {
					outputDevicesList.remove(device);
					outputDevicesBar.getChildren().remove(outputDeviceItems.get(device));
					outputQueues.remove(device);
					outputDeviceItems.remove(device);
				}				
			}
		}
	}
	
	public void setPlay(boolean play) {

		for (SoundLevelDisplayItem item : inputDeviceItems.values()) {
			item.setPlay(play);
		}

		for (SoundLevelDisplayItem item : outputDeviceItems.values()) {
			item.setPlay(play);
		}
	}
}
