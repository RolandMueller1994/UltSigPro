package inputhandler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import channel.Channel;
import channel.InputDataListener;
import gui.soundLevelDisplay.SoundLevelBar;

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
	private HashMap<String, IteratableAudioInputStream> inputStreams;
	private HashMap<String, Mixer> subscribedDevices;
	private HashMap<String, TargetDataLine> targetDataLines;
	private ScheduledThreadPoolExecutor executor;
	private Runnable readRunnable;

	private Lock lock = new ReentrantLock();
	private Condition startupCondition = lock.newCondition();

	private HashMap<InputDataListener, Collection<String>> distributionMap = new HashMap<>();
	private HashMap<InputDataListener, HashMap<String, Double>> inputLevelMultiplier = new HashMap<>();
	
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
		inputStreams = new HashMap<>();
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
		executor.shutdownNow();

		System.out.println("Recording stopped at: " + System.currentTimeMillis());

	}

	public synchronized void openWaveFiles(HashMap<String, File> waveFiles, InputDataListener listener) {
		if (!waveFiles.isEmpty()) {
			Collection<String> fileNames = waveFiles.keySet();
			for (String fileName : fileNames) {
				distributionMap.get(listener).add(fileName);
				IteratableAudioInputStream stream;
				stream = new IteratableAudioInputStream(waveFiles.get(fileName));
				inputStreams.put(fileName, stream);
				
				HashMap<String, Double> inputLevel = new HashMap<>();
				inputLevel.put(fileName, 1.0);
				if (inputLevelMultiplier.containsKey(listener)) {
					inputLevelMultiplier.get(listener).putAll(inputLevel);
				} else {
					inputLevelMultiplier.put(listener, inputLevel);
				}
			}
		}
	}
	
	public synchronized void removeWaveFiles(HashMap<String, File> waveFiles, InputDataListener listener) {
		
		for(String fileName : waveFiles.keySet()) {
			distributionMap.get(listener).remove(fileName);
			
			inputLevelMultiplier.get(listener).remove(fileName);
			
			boolean removeWaveFile = true;
			
			for(Collection<String> devices : distributionMap.values()) {
				if(devices.contains(fileName)) {
					removeWaveFile = false;
					break;
				}
			}
			
			if(removeWaveFile) {
				inputStreams.remove(fileName);
			}
		}
		
	}

	public synchronized void registerInputDataListener(InputDataListener listener, Collection<String> devices) {
		Collection<String> inputDevices = new HashSet<String>();
		inputDevices.addAll(devices);
		distributionMap.put(listener, inputDevices);
		if (!inputDevices.isEmpty()) {
			for (String device : inputDevices) {
				setSubscribedDevices(device);

				HashMap<String, Double> inputLevel = new HashMap<>();
				inputLevel.put(device, 1.0);
				if (inputLevelMultiplier.containsKey(listener)) {
					inputLevelMultiplier.get(listener).putAll(inputLevel);
				} else {
					inputLevelMultiplier.put(listener, inputLevel);
				}
			}
		}
	}

	public synchronized void addDeviceToInputDataListener(InputDataListener listener, String device) {
		Collection<String> devices = distributionMap.get(listener);
		if (devices != null) {
			devices.add(device);
			setSubscribedDevices(device);
			
			HashMap<String, Double> inputLevel = new HashMap<>();
			inputLevel.put(device, 1.0);
			if (inputLevelMultiplier.containsKey(listener)) {
				inputLevelMultiplier.get(listener).putAll(inputLevel);
			} else {
				inputLevelMultiplier.put(listener, inputLevel);
			}
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
		inputLevelMultiplier.remove(listener);		
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
	
	private class IteratableAudioInputStream {
		
		private AudioInputStream inputStream;
		private byte[] dataBuffer;
		private byte[] nullBuffer;
		
		private long startupTime;
		private int cursor;
		private int samplingFreq = 44100;
		
		public IteratableAudioInputStream(File waveFile) {
			try {
				// At first we capture the complete sound file.
				inputStream = AudioSystem.getAudioInputStream(waveFile);
				
				int avail = inputStream.available();
				dataBuffer = new byte[avail];
				
				inputStream.read(dataBuffer, 0, avail);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Array filled with zeros -> We can just copy this array afterwards and don't have to write zeros manually.
			nullBuffer = new byte[1000];
			
			for(int i=0; i<nullBuffer.length; i++) {
				nullBuffer[i] = 0;
			}
		}
		
		public void start() {
			// Set the cursor to start of the sound file
			startupTime = System.currentTimeMillis();
			cursor = 0;
		}
		
		public long available() {
			
			// How long the track has been played.
			long timediff = System.currentTimeMillis() - startupTime;
			
			// Devide by 500 because of 4 bytes per sample -> samplingFreq * 4 / 1000
			long curpos = (long) (timediff * ((double) samplingFreq) / 250);
			
			if(curpos > cursor) {
				return curpos - cursor;
			}
			
			return 0;
		}
		
		public byte[] read(int packageSize) {

			byte[] outData = new byte[packageSize];
			
			// Check if there are enough data from file or if we have to write zeros.
			if(cursor + packageSize < dataBuffer.length) {
				System.arraycopy(dataBuffer, cursor, outData, 0, packageSize);
				cursor += packageSize;
			} else if(cursor < dataBuffer.length) {
				// Still date to write but not enough for packageSize
				int remaining = dataBuffer.length - cursor;
				System.arraycopy(dataBuffer, cursor, outData, 0, remaining);
				System.arraycopy(nullBuffer, 0, dataBuffer, remaining, packageSize - remaining);
				cursor += packageSize;
			} else {
				// Only zeros will be written
				System.arraycopy(nullBuffer, 0, outData, 0, packageSize);
				cursor += packageSize;
			}
			
			return outData;
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

				int startCount = 0;

				@Override
				public void run() {

					for (Map.Entry<String, TargetDataLine> targetEntry : targetEntrySet) {
						if (targetEntry.getValue().available() < packageSize) {
							return;
						}
					}
					
					if(!first) {
						for(IteratableAudioInputStream inputStream : inputStreams.values()) {
							if (inputStream.available() < packageSize * 2) {
								return;
							}
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

						for(IteratableAudioInputStream inputStream : inputStreams.values()) {
							inputStream.start();
						}

						first = false;
						return;
					} else if (second) {
						lock.lock();
						startupCondition.signal();
						lock.unlock();
						second = false;
						System.out.println("First data input at: " + System.currentTimeMillis());
						System.currentTimeMillis();
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

					for (Map.Entry<String, IteratableAudioInputStream> inputEntry : inputStreams.entrySet()) {
						byte[] readData = inputEntry.getValue().read(packageSize * 2);
						int[] marshalledData = new int[outPackageSize];
						LinkedList<Integer> soundLevelData = new LinkedList<>();

						for (int i = 0; i < outPackageSize; i++) {
							int intValueLeftStereo = 0;
							int intValueRightStereo = 0;
							int intValueMono = 0;
							
							// TODO byte to integer conversion works here only for 4
							// bytes/frame and big endian
							// need different conversions implementations
							// for different coding formats

							// Checks if the wave file has ended or not (no more
							// data left to read). Left and right stereo channel
							// from the wave file are merged to a mono channel
							// (average value is calculated)
							
							intValueLeftStereo = intValueLeftStereo
									| Byte.toUnsignedInt(readData[4 * i + 1]);
							intValueLeftStereo <<= 8;
							intValueLeftStereo = intValueLeftStereo
									| Byte.toUnsignedInt(readData[4 * i]);
							intValueLeftStereo <<= 16;
							intValueLeftStereo >>= 16;

							intValueRightStereo = intValueRightStereo
									| Byte.toUnsignedInt(readData[4 * i + 3]);
							intValueRightStereo <<= 8;
							intValueRightStereo = intValueRightStereo
									| Byte.toUnsignedInt(readData[4 * i + 2]);
							intValueRightStereo <<= 16;
							intValueRightStereo >>= 16;

							intValueMono = (intValueRightStereo + intValueLeftStereo) / 2;		

							marshalledData[i] = intValueMono;
							soundLevelData.add(intValueMono);
						}
						marshalledBuffer.put(inputEntry.getKey(), marshalledData);
						SoundLevelBar.getSoundLevelBar().updateSoundLevelItems(inputEntry.getKey(), soundLevelData,
								true);
					}

					for (Map.Entry<InputDataListener, Collection<String>> destEntry : distributionMap.entrySet()) {
						boolean first = true;
						int[] destData = new int[outPackageSize];
						for (String input : destEntry.getValue()) {
							double multiplier = (inputLevelMultiplier.get(destEntry.getKey())).get(input);
							int[] inputData = marshalledBuffer.get(input);

							if (multiplier != 1) {
								if (first) {
									for (int i = 0; i < outPackageSize; i++) {
										destData[i] = (int) (multiplier * inputData[i]);
										first = false;
									}
								} else {
									for (int i = 0; i < outPackageSize; i++) {
										destData[i] = (int) (destData[i] + (multiplier * inputData[i]));
									}
								}
							} else {
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
						}

						destEntry.getKey().putData(destData);
					}
				}
			};

			executor.scheduleAtFixedRate(readRunnable, 0, 1, TimeUnit.MILLISECONDS);
		}
	}
	
	public void inputLevelMultiplierChanged(InputDataListener listener, String device, double multiplier) {
		inputLevelMultiplier.get(listener).put(device, multiplier);
	}
}
