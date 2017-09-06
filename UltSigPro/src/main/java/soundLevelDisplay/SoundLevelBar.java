package soundLevelDisplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import channel.ChannelConfig;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class SoundLevelBar extends GridPane implements SoundValueInterface {

	private static SoundLevelBar soundLevelBar;

	private static GridPane inputDevicesBar;
	private static GridPane outputDevicesBar;

	// HashMap<DeviceName, ChannelName>
	private static HashMap<String, LinkedList<String>> inputDevicesList;
	private static HashMap<String, LinkedList<String>> outputDevicesList;

	// HashMap<DeviceName, SoundDevice>
	private static HashMap<String, SoundLevelDisplayItem> deviceItems;

	public static SoundLevelBar getSoundLevelBar() {

		if (soundLevelBar == null) {
			soundLevelBar = new SoundLevelBar();
			inputDevicesList = new HashMap<>();
			outputDevicesList = new HashMap<>();

			inputDevicesBar = new GridPane();
			outputDevicesBar = new GridPane();

			soundLevelBar.setPadding(new Insets(5));
			soundLevelBar.setHgap(5);
			soundLevelBar.setVgap(5);
			soundLevelBar.add(inputDevicesBar, 0, 0);
			soundLevelBar.add(outputDevicesBar, 1, 0);

			deviceItems = new HashMap<>();
		}

		return soundLevelBar;
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
