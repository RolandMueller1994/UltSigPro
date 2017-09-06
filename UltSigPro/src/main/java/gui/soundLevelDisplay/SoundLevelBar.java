package gui.soundLevelDisplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import channel.ChannelConfig;
import i18n.LanguageResourceHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import resourceframework.ResourceProviderException;

public class SoundLevelBar extends GridPane implements SoundValueInterface {
	
	private static final String INPUT_TITLE = "inputTitle";
	private static final String OUTPUT_TITLE = "outputTitle";
	
	private static final int HEIGHT = 85;
	
	private LanguageResourceHandler lanHandler;

	private static SoundLevelBar soundLevelBar;

	private GridPane inputDevicesBar;
	private GridPane outputDevicesBar;

	// HashMap<DeviceName, ChannelName>
	private HashMap<String, LinkedList<String>> inputDevicesList;
	private HashMap<String, LinkedList<String>> outputDevicesList;

	// HashMap<DeviceName, SoundDevice>
	private HashMap<String, SoundLevelDisplayItem> deviceItems;

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
			inputDevicesBar.setPadding(new Insets(5));
			inputDevicesBar.setHgap(15);
			
			outputDevicesBar = new GridPane();
			outputDevicesBar.setPadding(new Insets(5));
			outputDevicesBar.setHgap(15);
			
			TitledPane inputPane = new TitledPane();
			inputPane.setText(lanHandler.getLocalizedText(SoundLevelBar.class, INPUT_TITLE));
			inputPane.setCollapsible(false);
			inputPane.setContent(inputDevicesBar);
			inputPane.setMaxWidth(Double.MAX_VALUE);
			inputPane.setMinHeight(HEIGHT);
			GridPane.setHgrow(inputPane, Priority.ALWAYS);
			
			TitledPane outputPane = new TitledPane();
			outputPane.setText(lanHandler.getLocalizedText(SoundLevelBar.class, OUTPUT_TITLE));
			outputPane.setCollapsible(false);
			outputPane.setContent(outputDevicesBar);
			outputPane.setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(outputPane, Priority.ALWAYS);
			outputPane.setMinHeight(HEIGHT);
			
			add(inputPane, 0, 0);
			add(outputPane, 1, 0);
			
			deviceItems = new HashMap<>();
		} catch (ResourceProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void updateSoundLevelItems(String deviceName, LinkedList<Integer> soundValues) {

		Thread updateSoundLevel = new Thread(new Runnable() {

			@Override
			public void run() {
				deviceItems.get(deviceName).setSoundLevel(new LinkedList<>(soundValues));
			}
		});
		updateSoundLevel.start();
	}

	/**
	 * Is called, when a {@linkplain Channel} gets created. Checks if there are
	 * already any entries for the selected devices in the Channel. For new
	 * entries appears a {@linkplain SoundLevelDisplayItem}.
	 * 
	 * @param config
	 */
	public void addNewChannelSoundDevices(ChannelConfig config) {

		for (String device : config.getInputDevices()) {

			// new input device entry
			if (!inputDevicesList.containsKey(device)) {
				inputDevicesList.put(device, new LinkedList<>());
				deviceItems.put(device, new SoundLevelDisplayItem(device));
				inputDevicesBar.addRow(0, deviceItems.get(device));
			}

			// add channel name to this device
			inputDevicesList.get(device).add(config.getName());
		}

		for (String device : config.getOutputDevices()) {

			// new output device entry
			if (!outputDevicesList.containsKey(device)) {
				outputDevicesList.put(device, new LinkedList<>());
				deviceItems.put(device, new SoundLevelDisplayItem(device));
				outputDevicesBar.addRow(0, deviceItems.get(device));
			}

			// add channel name to this device
			outputDevicesList.get(device).add(config.getName());
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
				inputDevicesBar.getChildren().remove(device);
			}
		}

		// remove the entry of the channel for this input device
		for (String device : config.getOutputDevices()) {
			outputDevicesList.get(device).remove(config.getName());

			// remove the device if there are no longer any channels with this
			// device
			if (outputDevicesList.get(device).isEmpty()) {
				outputDevicesList.remove(device);
				outputDevicesBar.getChildren().remove(device);
			}
		}
	}
}
