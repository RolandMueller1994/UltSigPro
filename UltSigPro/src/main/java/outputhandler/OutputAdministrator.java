package outputhandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.google.common.collect.Queues;

import channel.InputDataListener;
import channel.OutputDataSpeaker;

public class OutputAdministrator {

	private static OutputAdministrator outputAdministrator;
	private static HashMap<String, Mixer> allSoundOutputDevices;
	private static HashMap<String, Mixer> selectedDevices;
	private static HashMap<String, SourceDataLine> sourceDataLines;
	private static boolean stopped = false;

	private static final int distributionSize = 100;
	private HashMap<OutputDataSpeaker, HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>> distributionQueue = new HashMap<>();

	public static OutputAdministrator getOutputAdministrator() {

		if (outputAdministrator == null) {
			outputAdministrator = new OutputAdministrator();
			allSoundOutputDevices = new HashMap<>();
			selectedDevices = new HashMap<>();
			sourceDataLines = new HashMap<>();
		}
		return outputAdministrator;
	}

	private OutputAdministrator() {
	}

	public void collectSoundOutputDevices() {

		Mixer mixer;
		Mixer.Info[] allSoundDevices = AudioSystem.getMixerInfo();
		DataLine.Info info;
		int deviceCounter = 0;
		info = new DataLine.Info(SourceDataLine.class, null);

		for (Mixer.Info i : allSoundDevices) {
			mixer = AudioSystem.getMixer(i);
			if (mixer.isLineSupported(info)) {
				allSoundOutputDevices.put(allSoundDevices[deviceCounter].getName(), mixer);
			}
			deviceCounter++;
		}
	}

	public HashMap<String, Mixer> getOutputDevices() {
		return allSoundOutputDevices;
	}

	public HashMap<String, Mixer> getSelectedDevices() {
		return selectedDevices;
	}

	public HashMap<String, SourceDataLine> getSourceDataLines() {
		return sourceDataLines;
	}

	public void removeSelectedDevice(String deviceName) {

		// Checks, if there is any selectedDevice with the delivered
		// "deviceName" for every entry in distributionQueue
		for (OutputDataSpeaker outputDataSpeaker : distributionQueue.keySet()) {
			for (String device : distributionQueue.get(outputDataSpeaker).keySet()) {
				if (device.contains(deviceName))
					return;
			}
		}
		selectedDevices.remove(deviceName);
		sourceDataLines.get(deviceName).stop();
		sourceDataLines.get(deviceName).close();
		sourceDataLines.remove(deviceName);
	}

	public void startPlayback() {

		stopped = false;

		for (Map.Entry<OutputDataSpeaker, HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>> entry : distributionQueue
				.entrySet()) {
			Collection<LinkedBlockingQueue<LinkedList<Integer>>> queues = entry.getValue().values();
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
						LinkedList<Integer> value = new LinkedList<>();
						for (int a = 0; a < distributionSize; a++) {
							int[] data = new int[distributionSize];
							for (i = 0; i < size; i++) {
								if (intBuffers.get(i).size() == 0) {
									int count = 0;
									intBuffers.remove(i);
									for (LinkedBlockingQueue<LinkedList<Integer>> queue : queues) {
										if (count == i) {
											try {
												intBuffers.add(i, queue.poll(25, TimeUnit.MILLISECONDS));
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											break;
										}
										count++;
									}
								}
								data = entry.getKey().fetchData();
							}
							value.add(data[a]);
						}
						intBuffers.add(value);
					}
				}
			});
			distributionThread.start();
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (Map.Entry<String, SourceDataLine> entry : sourceDataLines.entrySet()) {
			SourceDataLine line = entry.getValue();

			Thread playbackThread = new Thread(new Runnable() {

				@Override
				public void run() {
					String device = entry.getKey();
					LinkedList<LinkedBlockingQueue<LinkedList<Integer>>> queues = new LinkedList<>();

					for (OutputDataSpeaker speaker : distributionQueue.keySet()) {
						LinkedBlockingQueue<LinkedList<Integer>> queue = distributionQueue.get(speaker).get(device);
						if (queue != null) {
							queues.add(queue);
						}
					}
					line.start();
					line.flush();
					LinkedList<Byte> byteBuffers = new LinkedList<>();

					while (!stopped) {
						//TODO create code for writing bytes to the lines
					}
					line.stop();
				}
			});
		}

	}

	public void stopPlayback() {
		stopped = true;
	}

	public void setSelectedDevice(String deviceName) {

		if (!selectedDevices.containsKey(deviceName)) {

			selectedDevices.put(deviceName, allSoundOutputDevices.get(deviceName));

			DataLine.Info info = new DataLine.Info(SourceDataLine.class, null);
			SourceDataLine line = null;

			// AudioFormat(sample rate, bits per sample, channel number
			// (stereo/mono), signed, bigEndian)
			AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, true);

			try {
				line = (SourceDataLine) allSoundOutputDevices.get(deviceName).getLine(info);
				line.open(audioFormat);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}

			if (line != null) {
				line.start();
				sourceDataLines.put(deviceName, line);
			}
		}
	}

	public synchronized void registerOutputDataSpeaker(OutputDataSpeaker speaker, Collection<String> devices) {
		distributionQueue.put(speaker, new HashMap<String, LinkedBlockingQueue<LinkedList<Integer>>>());

		for (String device : devices) {
			setSelectedDevice(device);
			distributionQueue.get(speaker).put(device, new LinkedBlockingQueue<LinkedList<Integer>>());
		}
	}

	public synchronized void addDeviceToOutputDataSpeaker(OutputDataSpeaker speaker, String device) {
		Collection<String> devices = distributionQueue.get(speaker).keySet();
		if (devices != null) {
			devices = new HashSet<>();
			devices.add(device);
			registerOutputDataSpeaker(speaker, devices);
		} else {
			distributionQueue.get(speaker).put(device, new LinkedBlockingQueue<LinkedList<Integer>>());
			setSelectedDevice(device);
		}
	}

	public synchronized void removeDeviceFromOutputDataSpeaker(OutputDataSpeaker listener, String device) {

	}

	public synchronized void removeOutputDataSpeaker(OutputDataSpeaker speaker) {
		Collection<String> devices = distributionQueue.get(speaker).keySet();
		distributionQueue.remove(speaker);
		for (String device : devices) {
			removeSelectedDevice(device);
		}

	}
}
