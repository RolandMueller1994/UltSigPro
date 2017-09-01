package outputhandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import javax.sound.sampled.SourceDataLine;
import channel.OutputDataSpeaker;

public class OutputAdministrator {

	private static OutputAdministrator outputAdministrator;
	private static HashMap<String, Mixer> allSoundOutputDevices;
	private static HashMap<String, Mixer> selectedDevices;
	private static HashMap<String, SourceDataLine> sourceDataLines;
	private static boolean stopped = false;

	private long latency = 20;
	private int byteBufferSize = 100;

	// SoundOutputDevice -> Signal processing Channel -> Queue with sound values
	private HashMap<String, HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>>> distributionQueue = new HashMap<>();
	HashMap<String, LinkedBlockingQueue<Integer>> outputStream = new HashMap<>();

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

	public Set<String> getOutputDevices() {
		return allSoundOutputDevices.keySet();
	}

	public HashMap<String, Mixer> getSelectedDevices() {
		return selectedDevices;
	}

	public HashMap<String, SourceDataLine> getSourceDataLines() {
		return sourceDataLines;
	}

	public void removeSelectedDevice(String deviceName) {

		// Checks, if there is a soundOutputDevice with the delivered
		// "deviceName" in the distributionQueue
		for (String selectedDevice : distributionQueue.keySet()) {
			if (selectedDevice == deviceName) {
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

		// entry: stores every SoundOutputDevice-entry in the distributionQueue
		// Code in this for-loop is executed for every SoundOutputDevice:

		// Wait for the specified latency to start the playback
		try {
			Thread.sleep(latency);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (Map.Entry<String, HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>>> entry : distributionQueue
				.entrySet()) {

			// outputSpeakerQueue: contains all Channels for this
			// SoundOutputDevice
			// listQueue: contains a collection of LinkedBlockingQueues
			// (Datapaths) for this SoundOutputDevice
			HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>> outputSpeakerQueue = entry.getValue();
			Collection<LinkedBlockingQueue<LinkedList<Integer>>> listQueue = entry.getValue().values();

			// Clears all old data of every single LinkedBlockingQueue of this
			// SoundOutputDevice
			for (String stream : outputStream.keySet()) {
				outputStream.get(stream).clear();
			}

			Thread distributionThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// listQueue.Size() represents the number of Channels
					// connected with this SoundOutputDevice. Every element of
					// this ArrayList represents one Channel and LinkedList
					// contains the sampled values from the input
					ArrayList<LinkedList<Integer>> intBuffers = new ArrayList<>(listQueue.size());

					int i = 0, j = 0, channelSum = 0;

					while (!stopped) {

						// check every intBuffer (every integer stream from
						// Channel), if there are values left to convert into
						// bytes. fetch data if necessary
						i = 0;
						for (LinkedList<Integer> intBuffer : intBuffers) {
							if (intBuffer.isEmpty()) {
								j = 0;

								// search for the needed speaker
								for (OutputDataSpeaker speaker : outputSpeakerQueue.keySet()) {
									if (i == j) {
										intBuffers.add(i, speaker.fetchData());
										j++;
									}
								}
							}
							i++;
						}

						channelSum = 0;
						for (i = 0; i < intBuffers.size(); i++) {

							// sum up all first values from each channel
							channelSum += intBuffers.get(i).removeFirst();
						}
						
						try {
							outputStream.get(entry.getKey()).put(channelSum);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
			distributionThread.start();
		}

		for (Map.Entry<String, SourceDataLine> entry : sourceDataLines.entrySet()) {
			SourceDataLine line = entry.getValue();

			Thread playbackThread = new Thread(new Runnable() {

				@Override
				public void run() {

					while (!stopped) {

						int i = 0, intSample = 0;
						byte[] byteBuffer = new byte[byteBufferSize];
						
						// fetches 50 integer values from outputStream and
						// writes it to intBuffer
						while (i < byteBufferSize/2) {							
							
							intSample = outputStream.get(entry.getKey()).remove();
							
							// Int: 		Byte 3 : Byte 2 : Byte 1 : Byte 0
							// byteBuffer 	------ : ------ :   2*i  : 2*i+1
							byteBuffer[2*i] = (byte) ((intSample & 0xFF00) >> 8);
							byteBuffer[2*i+1] = (byte) (intSample & 0xFF);
							
							i++;
						}
						line.write(byteBuffer, 0, byteBuffer.length);
					}
				}

			});
			playbackThread.start();
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

	/**
	 * Is called, when a new Channel/OutputDataSpeaker is created. Sets entries
	 * in the distributionQueue for the new OutputDataSpeakers and
	 * SoundOutputDevices.
	 * 
	 * @param speaker
	 * @param devices
	 */
	public synchronized void registerOutputDevices(OutputDataSpeaker speaker, Collection<String> devices) {
		for (String device : devices) {
			setSelectedDevice(device);
			if (!distributionQueue.containsKey(device)) {
				distributionQueue.put(device,
						new HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>>());
				outputStream.put(device, new LinkedBlockingQueue<Integer>());
			}
			distributionQueue.get(device).put(speaker, new LinkedBlockingQueue<LinkedList<Integer>>());
		}
	}

	/***
	 * Is called, when Channel/OutputDataSpeaker adds a single SoundOutputDevice
	 * as listener.
	 * 
	 * @param speaker
	 * @param device
	 */
	public synchronized void addSoundOutputDeviceToSpeaker(OutputDataSpeaker speaker, String device) {

		// Checks, if there is already an existing entry for this
		// SoundOutputDevice
		if (!distributionQueue.containsKey(device)) {
			distributionQueue.put(device, new HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>>());
			outputStream.put(device, new LinkedBlockingQueue<Integer>());
		}
		distributionQueue.get(device).put(speaker, new LinkedBlockingQueue<LinkedList<Integer>>());
		setSelectedDevice(device);
	}

	/**
	 * Is called, when a single SoundOutputDevice gets deleted. Removes
	 * the entry of the channel from distributionQueue.
	 * 
	 * @param speaker
	 * @param device
	 */
	public synchronized void removeDeviceFromOutputDataSpeaker(OutputDataSpeaker speaker, String device) {
		distributionQueue.get(device).remove(speaker);
		removeSelectedDevice(device);

		// checks if there are any entries left for this
		// SoundOutputDevice or if the SoundOutputDevice receives
		// no more longer any data from any speaker
		if (distributionQueue.get(device).isEmpty()) {
			distributionQueue.remove(device);
			outputStream.remove(device);
		}
	}

	/**
	 * Is called, when a complete Channel/OutputDataSpeaker gets deleted.
	 * Removes the entry of the channel from the distributionQueue.
	 * 
	 * @param speaker
	 */
	public synchronized void removeOutputDevices(OutputDataSpeaker speaker) {
		for (String device : distributionQueue.keySet()) {
			// Search for all SoundOutputDevices with a queue to this speaker
			if (distributionQueue.get(device).containsKey(speaker)) {
				removeSelectedDevice(device);
				distributionQueue.get(device).remove(speaker);

				// checks if there are any entries left for this
				// SoundOutputDevice or if the SoundOutputDevice receives
				// no more longer any data from any speaker
				if (distributionQueue.get(device).isEmpty()) {
					distributionQueue.remove(device);
					outputStream.remove(device);
				}
			}
		}

	}
}
