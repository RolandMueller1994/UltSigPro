package inputhandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import channel.Channel;
import channel.InputDataListener;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;
import soundLevelDisplay.SoundLevelBar;
import soundLevelDisplay.SoundValueInterface;

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
	private static HashMap<String, Mixer> allSoundInputDevices;
	private static HashMap<String, Mixer> subscribedDevices;
	private static HashMap<String, TargetDataLine> targetDataLines;
	private static boolean stopped = false;

	private static final String ALERT_TITLE = "alertTitle";
	private static final String ALERT_HEADER = "alertHeader";
	private static final String ALERT_TEXT = "alertText";

	private static final int distributionSize = 100;

	// TODO check necessity of distributionMap
	private HashMap<InputDataListener, Collection<String>> distributionMap = new HashMap<>();
	// Map: Listener -> Device -> Queue for distribution (Listener will read
	// from this queue) -> Data packages
	private HashMap<InputDataListener, HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>> distributionQueue = new HashMap<>();

	public static InputAdministrator getInputAdminstrator() {

		if (inputAdministrator == null) {
			inputAdministrator = new InputAdministrator();
			allSoundInputDevices = new HashMap<>();
			subscribedDevices = new HashMap<>();
			targetDataLines = new HashMap<>();
			;
		}
		return inputAdministrator;
	}

	private InputAdministrator() {
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

		// Start a distribution task for every listener
		for (Map.Entry<InputDataListener, HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>> entry : distributionQueue
				.entrySet()) {

			Collection<LinkedBlockingQueue<LinkedList<Integer>>> queues = entry.getValue().values();

			// Remove all old data packages
			for (LinkedBlockingQueue<LinkedList<Integer>> queue : queues) {
				queue.clear();
			}

			Thread distributionThread = new Thread(new Runnable() {

				@Override
				public void run() {

					ArrayList<LinkedList<Integer>> intBuffers = new ArrayList<>(queues.size());

					int i = 0;

					for (LinkedBlockingQueue<LinkedList<Integer>> queue : queues) {
						try {
							intBuffers.add(i, queue.poll(10000, TimeUnit.MILLISECONDS));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						i++;
					}

					int size = queues.size();

					while (!stopped) {
						// Create a new array for distribution
						int[] data = new int[distributionSize];
						// Add all sources which shall be send to the listener
						try {
							for (int a = 0; a < distributionSize; a++) {
								int value = 0;
								// Loop over all input devices for this listener
								for (i = 0; i < size; i++) {
									// Check if buffer from data source is empty
									// -> get the next one
									if (intBuffers.get(i).size() == 0) {
										int count = 0;
										// Delete the buffer
										intBuffers.remove(i);
										// Search the device to get the next
										// package
										for (LinkedBlockingQueue<LinkedList<Integer>> queue : queues) {
											if (count == i) {
												try {
													// Read the next package
													// from source -> wait till
													// present if no data
													// available
													intBuffers.add(i, queue.poll(100, TimeUnit.MILLISECONDS));
												} catch (InterruptedException e) {
													// TODO Auto-generated catch
													// block
													e.printStackTrace();
												}
												break;
											}
											count++;
										}
									}
									// Add the values from each source
									value += intBuffers.get(i).removeFirst();
								}
								// Insert value into package for listener
								data[a] = value;
							}
							entry.getKey().putData(data);
						} catch (NullPointerException ex) {
							if (!stopped) {
								USPGui.stopExternally();
								Platform.runLater(new Runnable() {

									@Override
									public void run() {
										Alert alert = new Alert(AlertType.ERROR);
										try {
											alert.setTitle(LanguageResourceHandler.getInstance()
													.getLocalizedText(InputAdministrator.class, ALERT_TITLE));
											alert.setHeaderText(LanguageResourceHandler.getInstance()
													.getLocalizedText(InputAdministrator.class, ALERT_HEADER));

											TextArea contentText = new TextArea(LanguageResourceHandler.getInstance()
													.getLocalizedText(InputAdministrator.class, ALERT_TEXT));
											contentText.setEditable(false);
											contentText.setWrapText(true);

											alert.getDialogPane().setContent(contentText);
										} catch (ResourceProviderException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										alert.show();
									}

								});
							}
						}
					}
				}
			});
			distributionThread.start();
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		OutputAdministrator.getOutputAdministrator().startPlayback();

		for (Map.Entry<String, TargetDataLine> entry : targetDataLines.entrySet()) {

			TargetDataLine line = entry.getValue();
			SoundValueInterface soundValueInterface = SoundLevelBar.getSoundLevelBar();

			Thread recordThread = new Thread(new Runnable() {

				@Override
				public void run() {

					boolean firstRead = true;

					String device = entry.getKey();

					LinkedList<LinkedBlockingQueue<LinkedList<Integer>>> queues = new LinkedList<>();

					for (InputDataListener listener : distributionQueue.keySet()) {
						LinkedBlockingQueue<LinkedList<Integer>> queue = distributionQueue.get(listener).get(device);
						if (queue != null) {
							queues.add(queue);
						}
					}

					line.start();
					line.flush();
					System.out.println(line.getFormat());
					LinkedList<LinkedList<Integer>> intBuffers = new LinkedList<>();

					boolean first = true;

					while (!stopped) {
						// We will only read the current available data. If a
						// fixed size is read, the read()-call will block until
						// the requested data are available. This would cause
						// latency.
						// TODO Optimize sleep time after reading
						byte[] data = new byte[line.available()];
						int bytesRead = line.read(data, 0, data.length);

						int byteBuffer = 0, shiftCounter = 0;

						// We discard the first read data because this package
						// is pretty big and causes latency
						if (first) {
							first = false;
						} else if (bytesRead != 0) {

							if (firstRead) {
								System.out.println("First read at: " + System.currentTimeMillis());
								firstRead = false;
							}

							intBuffers.clear();
							for (int i = 0; i < queues.size()+1; i++) {
								intBuffers.add(new LinkedList<>());
							}

							for (shiftCounter = 0; shiftCounter < bytesRead; shiftCounter = shiftCounter + 2) {
								for (int i = 0; i < 2; i++) {
									byteBuffer = byteBuffer << 8;
									byteBuffer = byteBuffer | Byte.toUnsignedInt(data[shiftCounter + i]);
								}
								byteBuffer = byteBuffer << 16;
								byteBuffer = byteBuffer >> 16;

								for (LinkedList<Integer> intBuffer : intBuffers) {
									intBuffer.add(byteBuffer);
								}
								byteBuffer = 0;
							}

							int i = 0;
							for (LinkedBlockingQueue<LinkedList<Integer>> queue : queues) {
								queue.offer(intBuffers.get(i+1));
								i++;
							}
			
							soundValueInterface.updateSoundLevelItems(device, intBuffers.get(0));
						}
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					line.stop();
					// line.close();
					System.out.println("Recording stopped on " + line);
				}
			});
			recordThread.start();
		}

	}

	public void stopListening() {
		stopped = true;
	}

	public synchronized void registerInputDataListener(InputDataListener listener, Collection<String> devices) {
		distributionMap.put(listener, devices);
		distributionQueue.put(listener, new HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>());

		for (String device : devices) {
			setSubscribedDevices(device);
			distributionQueue.get(listener).put(device, new LinkedBlockingQueue<LinkedList<Integer>>());
		}
	}

	public synchronized void addDeviceToInputDataListener(InputDataListener listener, String device) {
		Collection<String> devices = distributionMap.get(listener);
		if (devices != null) {
			devices.add(device);
			distributionQueue.get(listener).put(device, new LinkedBlockingQueue<LinkedList<Integer>>());
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
			distributionQueue.get(listener).remove(device);
			removeSubscribedDevice(device);
		}
	}

	public synchronized void removeInputDataListener(InputDataListener listener) {
		Collection<String> devices = distributionMap.remove(listener);
		distributionQueue.remove(listener);
		for (String device : devices) {
			removeSubscribedDevice(device);
		}
	}
}
