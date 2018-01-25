package channel;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Wrapper class for informations about one channel.
 * 
 * @author roland
 *
 */
public class ChannelConfig {

	private String name;
	private Collection<String> inputDevices;
	private Collection<String> outputDevices;
	private HashMap<String, File> inputWaveFiles;
	private HashMap<String, File> outputWaveFiles;
	private Collection<String> signalSources;

	private HashSet<ChannelDeviceUpdateListener> listeners = new HashSet<>();

	/**
	 * Creates a new ChannelConfig with the given values.
	 * 
	 * @param name
	 *            The name of this channel. Must not be null.
	 * @param inputDevices
	 *            The input devices on which this channel should listen. Can be
	 *            null.
	 * @param outputDevices
	 *            The output devices on which this channel will write data. Can
	 *            be null.
	 * @param inputWaveFiles
	 *            The file paths to the wave files which should be read.
	 * @param outputWaveFiles
	 *            The file paths of the wave files to be created.
	 */
	public ChannelConfig(@Nonnull String name, @Nullable Collection<String> inputDevices,
			@Nullable Collection<String> outputDevices, HashMap<String, File> inputWaveFiles,
			HashMap<String, File> outputWaveFiles, Collection<String> signalSources) {
		this.name = name;
		if (inputDevices != null) {
			this.inputDevices = inputDevices;
		} else {
			this.inputDevices = new HashSet<String>();
		}
		if (outputDevices != null) {
			this.outputDevices = outputDevices;
		} else {
			this.outputDevices = new HashSet<String>();
		}
		if (!inputWaveFiles.isEmpty()) {
			this.inputWaveFiles = inputWaveFiles;
		} else {
			this.inputWaveFiles = new HashMap<String, File>();
		}
		if (!outputWaveFiles.isEmpty()) {
			this.outputWaveFiles = outputWaveFiles;
		} else {
			this.outputWaveFiles = new HashMap<String, File>();
		}
		if (!signalSources.isEmpty()) {
			this.signalSources = signalSources;
		} else {
			this.signalSources = new HashSet<String>();
		}
	}

	/**
	 * Returns the name of the channel.
	 * 
	 * @return The name. Won't be null.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the config.
	 * 
	 * @param name
	 *            The name as {@link String}. Must not be null.
	 */
	public void setName(@Nonnull String name) {
		this.name = name;
	}

	/**
	 * Get the selected wave files for playback.
	 * 
	 * @return {@linkplain HashMap} with file name as key and file path as
	 *         object.
	 */
	public HashMap<String, File> getInputWaveFiles() {
		return inputWaveFiles;
	}

	/**
	 * Get the selected to be created wave files.
	 * 
	 * @return {@linkplain HashMap} with file name as key and file path as
	 *         object.
	 */
	public HashMap<String, File> getOutputWaveFiles() {
		return outputWaveFiles;
	}

	/**
	 * Get the input devices.
	 * 
	 * @return A {@link HashSet} of {@link String}s. Won't be null.
	 */
	@Nonnull
	public Collection<String> getInputDevices() {
		return inputDevices;
	}

	/**
	 * Get the signal sources.
	 * 
	 * @return A {@link HashSet} of {@link String}s.
	 */
	public Collection<String> getSignalSources() {
		return signalSources;
	}

	/**
	 * Adds the given device to the input device list.
	 * 
	 * @param device
	 *            The device to add as {@link String}. Must not be null.
	 */
	public void addInputDevice(@Nonnull String device) {
		inputDevices.add(device);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void removeInputDevice(@Nonnull String device) {
		inputDevices.remove(device);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void addInputWaveFile(String name, File file) {
		inputWaveFiles.put(name, file);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void removeInputWaveFile(String name) {
		inputWaveFiles.remove(name);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	
	public void removeSignalSource(String name) {
		signalSources.remove(name);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	/**
	 * Get the output devices.
	 * 
	 * @return A {@link HashSet} of {@link String}s. Won't be null.
	 */
	@Nonnull
	public Collection<String> getOutputDevices() {
		return outputDevices;
	}

	/**
	 * Adds the given device to the input device list.
	 * 
	 * @param device
	 *            The device to add as {@link String}. Must not be null.
	 */
	public void addOutputDevice(@Nonnull String device) {
		outputDevices.add(device);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void removeOutputDevice(@Nonnull String device) {
		outputDevices.remove(device);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void addOutputWaveFile(String name, File file) {
		outputWaveFiles.put(name, file);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void removeOutputWaveFile(String name) {
		outputWaveFiles.remove(name);
		for (ChannelDeviceUpdateListener listener : listeners) {
			listener.fireDevicesUpdates();
		}
	}

	public void registerChannelDeviceUpdateListener(ChannelDeviceUpdateListener listener) {
		listeners.add(listener);
	}

	public interface ChannelDeviceUpdateListener {

		void fireDevicesUpdates();
	}

}
