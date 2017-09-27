package inputhandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import channel.Channel;
import channel.InputDataListener;
import gui.USPGui;
import gui.soundLevelDisplay.SoundLevelBar;
import gui.soundLevelDisplay.SoundValueInterface;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;

/**
 * Administrates which input devices are available and requests their sampled
 * data, if any {@linkplain Channel} has subscribed them. Then opens a
 * {@linkplain TargetDataLine} to the {@linkplain Mixer}, converts the received
 * bytes to integer and distributes it to the different signal processing paths.
 * 
 * @author Kone
 *
 */
public class InputAdministrator {

	private static InputAdministrator inputAdministrator;
	private HashMap<String, Mixer> allSoundInputDevices;
	private HashMap<String, Clip> allInputFiles;
	private HashMap<String, Mixer> subscribedDevices;
	private HashMap<String, TargetDataLine> targetDataLines;
	private boolean stopped = false;

	private ScheduledThreadPoolExecutor executor;
	private Runnable readRunnable;

	private Lock lock = new ReentrantLock();
	private Condition startupCondition = lock.newCondition();

	private static final String ALERT_TITLE = "alertTitle";
	private static final String ALERT_HEADER = "alertHeader";
	private static final String ALERT_TEXT = "alertText";

	// TODO check necessity of distributionMap
	private HashMap<InputDataListener, Collection<String>> distributionMap = new HashMap<>();

	public static InputAdministrator getInputAdminstrator() {

		if (inputAdministrator == null) {
			inputAdministrator = new InputAdministrator();
		}
		return inputAdministrator;
	}

	private InputAdministrator() {
		allSoundInputDevices = new HashMap<>();
		subscribedDevices = new HashMap<>();
		targetDataLines = new HashMap<>();
		allInputFiles = new HashMap<>();
	}

	/**
	 * Collects all sound devices and filters the results for input devices.
	 */
	public void collectSoundInputDevices() {

		Mixer mixer;
		Mixer.Info[] allSoundDevices = AudioSystem.getMixerInfo();
		DataLine.Info info;
		int deviceCounter = 0;
		info = new DataLine.Info(TargetDataLine.class, null);

		for (Mixer.Info i : allSoundDevices) {
			mixer = AudioSystem.getMixer(i);
			if (mixer.isLineSupported(info)) {
				allSoundInputDevices.put(allSoundDevices[deviceCounter].getName(), mixer);
			}
			deviceCounter++;
		}
	}

	/**
	 * 
	 * @return A set of strings with all sound input devices.
	 */
	public Set<String> getInputDevices() {
		return allSoundInputDevices.keySet();
	}

	/**
	 * Returns which devices are subscribed. Adding/Removing of values to this
	 * HashMap is handled by the {@linkplain Channel} class.
	 * 
	 * @return A key set of all subscribed devices.
	 */
	public Set<String> getSubscribedDevicesName() {
		return subscribedDevices.keySet();
	}

	/**
	 * 
	 * @return A HashMap of all open {@linkplain TargetDataLine}s.
	 */
	public HashMap<String, TargetDataLine> getTargetDataLines() {
		return targetDataLines;
	}

	/**
	 * Removes the subscription of the device and closes its
	 * {@linkplain TargetDataLine}.
	 * 
	 * @param name
	 *            The name of the device.
	 */
	public void removeSubscribedDevice(String name) {

		for (InputDataListener listener : distributionMap.keySet()) {
			if (distributionMap.get(listener).contains(name)) {
				return;
			}
		}
		subscribedDevices.remove(name);
		targetDataLines.get(name).stop();
		targetDataLines.get(name).close();
		targetDataLines.remove(name);
	}

	/**
	 * Checks first if the device is already subscribed. In the case of a new
	 * device it opens a {@linkplain TargetDataLine} with predefined values for
	 * sampling rate, bits per sample, ... .
	 * 
	 * @param deviceName
	 *            Name of the new subscribed device.
	 */
	public void setSubscribedDevices(String deviceName) {

		if (!subscribedDevices.containsKey(deviceName)) {

			subscribedDevices.put(deviceName, allSoundInputDevices.get(deviceName));

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
			TargetDataLine line = null;

			// AudioFormat(sample rate, bits per sample, channel number
			// (stereo/mono), signed, bigEndian)
			AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, true);

			try {
				line = (TargetDataLine) allSoundInputDevices.get(deviceName).getLine(info);
				line.open(audioFormat);
				System.out.println("Buffer size: " + line.getBufferSize());
				line.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}

			if (line != null) {
				targetDataLines.put(deviceName, line);
			}

		}
	}

	/**
	 * Collects the sampled bytes of all subscribed target data lines and
	 * converts it to integer values.
	 */
	public void startListening() {

		stopped = false;

		InputThread thread = new InputThread();

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		thread.start();

	}

	public void stopListening() {
		stopped = true;

		executor.shutdownNow();

		System.out.println("Recording stopped at: " + System.currentTimeMillis());

	}
	
	public synchronized void openWaveFiles(HashMap<String, File> waveFiles, Collection<String> outputDevices) {
		HashMap<String, Mixer> selectedOutputDevices = OutputAdministrator.getOutputAdministrator().getSelectedDevices();
		Collection<String> fileNames = waveFiles.keySet();
		for (String fileName : fileNames) {
			for (String outputDevice : outputDevices) {
				try {
					Clip clip = (Clip) selectedOutputDevices.get(outputDevice).getLine(new DataLine.Info(Clip.class, null));
					if (clip != null) {
						clip.open(AudioSystem.getAudioInputStream(waveFiles.get(fileName)));
						allInputFiles.put(fileName, clip);
					}
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void registerInputDataListener(InputDataListener listener, Collection<String> devices) {
		distributionMap.put(listener, devices);

		for (String device : devices) {
			setSubscribedDevices(device);
		}
	}

	public synchronized void addDeviceToInputDataListener(InputDataListener listener, String device) {
		Collection<String> devices = distributionMap.get(listener);
		if (devices != null) {
			devices.add(device);
			setSubscribedDevices(device);
		} else {
			devices = new HashSet<>();
			devices.add(device);
			registerInputDataListener(listener, devices);
		}
	}

	public synchronized void removeDeviceFromInputDataListener(InputDataListener listener, String device) {
		Collection<String> devices = distributionMap.get(listener);
		if (devices != null) {
			devices.remove(device);
			removeSubscribedDevice(device);
		}
	}

	public synchronized void removeInputDataListener(InputDataListener listener) {
		Collection<String> devices = distributionMap.remove(listener);
		for (String device : devices) {
			removeSubscribedDevice(device);
		}
	}

	public void waitForStartup() {
		lock.lock();

		try {
			startupCondition.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	private class InputThread extends Thread {

		public InputThread() {

		}

		@Override
		public void run() {

			int packageSize = 200;
			int outPackageSize = packageSize / 2;
			HashMap<String, byte[]> data = new HashMap<>();
			HashMap<String, int[]> marshalledBuffer = new HashMap<>();
			Set<Entry<String, TargetDataLine>> targetEntrySet = targetDataLines.entrySet();
			executor = new ScheduledThreadPoolExecutor(1);

			for (Map.Entry<String, TargetDataLine> entry : targetEntrySet) {
				data.put(entry.getKey(), new byte[packageSize]);
				// entry.getValue().start();
				entry.getValue().flush();
			}

			System.out.println("Recording started: " + System.currentTimeMillis());

			readRunnable = new Runnable() {

				boolean first = true;
				boolean second = true;

				long lastRead;

				int startCount = 0;

				@Override
				public void run() {

					for (Map.Entry<String, TargetDataLine> targetEntry : targetEntrySet) {
						if (targetEntry.getValue().available() < packageSize) {
							return;
						}
					}

					if (startCount < 3000) {
						startCount++;
						return;
					}

					if (first) {
						System.gc();

						for (Map.Entry<String, TargetDataLine> targetEntry : targetEntrySet) {
							targetEntry.getValue().flush();
						}

						for (Map.Entry<String, TargetDataLine> targetEntry : targetEntrySet) {
							int avail = targetEntry.getValue().available();
							targetEntry.getValue().read(new byte[avail], 0, avail);
						}
						
						for (String inputFile : allInputFiles.keySet()) {
								allInputFiles.get(inputFile).start();
						}

						first = false;
						return;
					} else if (second) {
						lock.lock();
						startupCondition.signal();
						lock.unlock();
						second = false;
						System.out.println("First data input at: " + System.currentTimeMillis());
						lastRead = System.currentTimeMillis();
					}

					for (Map.Entry<String, TargetDataLine> targetEntry : targetEntrySet) {
						byte[] readData = data.get(targetEntry.getKey());

						targetEntry.getValue().read(readData, 0, packageSize);

						// ArrayList<Integer> marshalledData = new ArrayList<>
						// (outPackageSize);
						int[] marshalledData = new int[packageSize];
						LinkedList<Integer> soundLevelData = new LinkedList<>();

						for (int i = 0; i < outPackageSize; i++) {
							int intValue = 0;

							intValue = intValue | Byte.toUnsignedInt(readData[2 * i]);
							intValue <<= 8;
							intValue = intValue | Byte.toUnsignedInt(readData[2 * i + 1]);

							intValue <<= 16;
							intValue >>= 16;

							marshalledData[i] = intValue;
							soundLevelData.add(intValue);
						}

						SoundLevelBar.getSoundLevelBar().updateSoundLevelItems(targetEntry.getKey(), soundLevelData,
								true);

						marshalledBuffer.put(targetEntry.getKey(), marshalledData);
					}

					for (Map.Entry<InputDataListener, Collection<String>> destEntry : distributionMap.entrySet()) {

						boolean first = true;

						int[] destData = new int[outPackageSize];

						for (String input : destEntry.getValue()) {
							int[] inputData = marshalledBuffer.get(input);

							if (first) {
								for (int i = 0; i < outPackageSize; i++) {
									destData[i] = inputData[i];
									first = false;
								}
							} else {
								for (int i = 0; i < outPackageSize; i++) {
									destData[i] += inputData[i];
								}
							}
						}

						destEntry.getKey().putData(destData);
					}
				}
			};

			executor.scheduleAtFixedRate(readRunnable, 0, 1, TimeUnit.MILLISECONDS);
		}
	}
}
