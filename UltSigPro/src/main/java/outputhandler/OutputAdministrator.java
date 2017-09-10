package outputhandler;

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

import channel.Channel;
import channel.OutputDataSpeaker;
import gui.USPGui;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import resourceframework.ResourceProviderException;

/**
 * Administrates which output devices are available and sends received processed
 * data from {@linkplainplain Channel} to output devices. Converts it from
 * integers to bytes before writing it on the lines. Handles the complete data
 * distribution from {@linkplainplain Channel} to output.
 * 
 * @author Kone
 *
 */
public class OutputAdministrator {

	private static OutputAdministrator outputAdministrator;
	private static HashMap<String, Mixer> allSoundOutputDevices;
	private static HashMap<String, Mixer> selectedDevices;
	private static HashMap<String, SourceDataLine> sourceDataLines;
	private static boolean stopped = false;
	
	private LinkedList<PlaybackThread> playbackThreads;

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

	/**
	 * Collects all sound devices and filters the results for output devices.
	 */
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

	/**
	 * Returns all selected sound output devices (as Strings), who receive sound
	 * data from {@linkplain Channel}s.
	 * 
	 * @return
	 */
	public Set<String> getOutputDevices() {
		return allSoundOutputDevices.keySet();
	}

	/**
	 * Returns all selected sound output devices (as {@linkplain Mixer}s), who
	 * receive sound data from {@linkplain Channel}s.
	 * 
	 * @return all selected Devices
	 */
	public HashMap<String, Mixer> getSelectedDevices() {
		return selectedDevices;
	}

	/**
	 * Returns all open {@linkplain SourceDataLines}.
	 * 
	 * @return Map of all open lines.
	 */
	public HashMap<String, SourceDataLine> getSourceDataLines() {
		return sourceDataLines;
	}

	/**
	 * Removes entries and closes the {@linkplain SourceDataLine} of the given
	 * device, if there are no others channels who use this device as a output
	 * device.
	 * 
	 * @param deviceName
	 *            name of the output device
	 */
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

	/**
	 * Starts two threads which are collecting data from {@linkplain Channel}s
	 * and write it to the determined sound output devices.
	 */

	public void startDistribution() {

		stopped = false;
		
		LinkedList<DistributionThread> distributionThreads = new LinkedList<> ();
		
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
			
			distributionThreads.add(new DistributionThread(entry.getKey(), outputSpeakerQueue, listQueue));
		}
		
		for(DistributionThread thread : distributionThreads) {
			thread.start();
		}
		
		playbackThreads = new LinkedList<> ();
		
		for (Map.Entry<String, SourceDataLine> entry : sourceDataLines.entrySet()) {
			SourceDataLine line = entry.getValue();
			
			playbackThreads.add(new PlaybackThread(entry.getKey(), line));
		}
	}

	public void startPlayback() {

		// entry: stores every SoundOutputDevice-entry in the distributionQueue
		// Code in this for-loop is executed for every SoundOutputDevice:

		// Wait for the specified latency to start the playback
		try {
			Thread.sleep(latency);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		System.out.println("Pre playback start: " + System.currentTimeMillis());
		
		for(PlaybackThread thread : playbackThreads) {
			thread.start();
		}
	}

	public void stopPlayback() {
		stopped = true;
	}

	/**
	 * Checks, if there is already a {@linkplain SourceDataLine} open for the
	 * given device name. If not, opens a line and allows it to engage in data
	 * I/O.
	 * 
	 * @param deviceName
	 *            name of the sound output device
	 */
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

				// We have to write a few data at the beginning, because the
				// first write operation causes a delay at playback.
				byte[] firstData = new byte[2];
				firstData[0] = 0;
				firstData[1] = 0;
				line.write(firstData, 0, 2);
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
	 * Is called, when a new {@linkplain Channel}/{@linkplain OutputDataSpeaker}
	 * is created. Sets entries in the distributionQueue for the new
	 * OutputDataSpeakers and sound output devices.
	 * 
	 * @param speaker
	 *            means the Channel/OutputDataSpeaker which has to be added
	 * @param devices
	 *            means the names of the sound output devices which has to be
	 *            added
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
	 * Is called, when {@linkplain Channel}/{@linkplain OutputDataSpeaker} adds
	 * a single sound output device as a listener.
	 * 
	 * @param speaker
	 *            means the Channel/OutputDataSpeaker which has to be added
	 * @param device
	 *            means the name of the sound output device which has to be
	 *            added
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
	 * Is called, when a single sound output device gets deleted. Removes the
	 * entry of the {@linkplain Channel} from distributionQueue.
	 * 
	 * @param speaker
	 *            means the Channel/{@linkplain OutputDataSpeaker} which has to
	 *            be removed
	 * @param device
	 *            means the name of the sound output device which has to be
	 *            removed
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
	 * Is called, when a complete
	 * {@linkplain Channel}/{@linkplain OutputDataSpeaker} gets deleted. Removes
	 * the entry of the channel from the distributionQueue.
	 * 
	 * @param speaker
	 *            means the Channel/OutputDataSpeaker which has to be removed
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

	private class OutputAlert extends Alert {

		private static final String ALERT_TITLE = "alertTitle";
		private static final String ALERT_HEADER = "alertHeader";
		private static final String ALERT_TEXT = "alertText";

		public OutputAlert() {
			super(AlertType.ERROR);

			try {
				LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();

				setTitle(lanHandler.getLocalizedText(OutputAlert.class, ALERT_TITLE));
				setHeaderText(lanHandler.getLocalizedText(OutputAlert.class, ALERT_HEADER));

				TextArea contentText = new TextArea(
						LanguageResourceHandler.getInstance().getLocalizedText(OutputAlert.class, ALERT_TEXT));
				contentText.setEditable(false);
				contentText.setWrapText(true);

				getDialogPane().setContent(contentText);

			} catch (ResourceProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	// This thread collects the integer values from every channel for a
	// single sound output device and sums it up.
	private class DistributionThread extends Thread {

		private String device;
		private HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>> outputSpeakerQueue;
		private Collection<LinkedBlockingQueue<LinkedList<Integer>>> listQueue;

		public DistributionThread(String device,
				HashMap<OutputDataSpeaker, LinkedBlockingQueue<LinkedList<Integer>>> outputSpeakerQueue,
				Collection<LinkedBlockingQueue<LinkedList<Integer>>> listQueue) {
			this.device = device;
			this.outputSpeakerQueue = outputSpeakerQueue;
			this.listQueue = listQueue;
		}

		@Override
		public void run() {
			// listQueue.Size() represents the number of Channels
			// connected with this SoundOutputDevice. Every element of
			// this ArrayList represents one Channel and LinkedList
			// contains the sampled values from the input
			ArrayList<LinkedList<Integer>> intBuffers = new ArrayList<>(listQueue.size());

			int a = 0;

			for (OutputDataSpeaker speaker : outputSpeakerQueue.keySet()) {
				intBuffers.add(a, speaker.fetchData());
				a++;
			}

			int j = 0, channelSum = 0;

			LinkedList<Integer> soundLevelData = null;

			try {
				while (!stopped) {

					if (soundLevelData == null) {
						soundLevelData = new LinkedList<>();
					}

					// check every intBuffer (every integer stream from
					// Channel), if there are values left to convert
					// into
					// bytes. fetch data if necessary
					for (int i = 0; i < intBuffers.size(); i++) {
						if (intBuffers.get(i).isEmpty()) {
							j = 0;

							intBuffers.remove(i);

							// search for the needed speaker
							for (OutputDataSpeaker speaker : outputSpeakerQueue.keySet()) {
								if (i == j) {
									intBuffers.add(i, speaker.fetchData());
									j++;
								}
							}
						}
					}

					channelSum = 0;
					for (int i = 0; i < intBuffers.size(); i++) {

						// sum up all first values from each channel
						channelSum += intBuffers.get(i).removeFirst();
					}

					// limit the values of the sound to its max/min
					// values
					if (channelSum > Short.MAX_VALUE) {
						channelSum = Short.MAX_VALUE;
					} else if (channelSum < Short.MIN_VALUE) {
						channelSum = Short.MIN_VALUE;
					}

					try {
						outputStream.get(device).put(channelSum);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					soundLevelData.add(channelSum);

					if (soundLevelData.size() > 800) {
						SoundLevelBar.getSoundLevelBar().updateSoundLevelItems(device, soundLevelData, false);
						soundLevelData = null;
					}
				}
			} catch (NullPointerException ex) {
				if (!stopped) {
					USPGui.stopExternally();
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							OutputAlert alert = new OutputAlert();
							alert.show();
						}

					});
				}
			}
		}

	}

	private class PlaybackThread extends Thread {

		private SourceDataLine line;
		private String device;
		
		public PlaybackThread(String device, SourceDataLine line) {
			this.device = device;
			this.line = line;
		}

		@Override
		public void run() {

			System.out.println("Started playback on: " + line);
			
			try {

				boolean firstWrite = true;

				while (!stopped) {

					int i = 0, intSample = 0;
					byte[] byteBuffer = new byte[byteBufferSize];

					// fetches 50 integer values from outputStream and
					// writes it to intBuffer
					while (i < byteBufferSize / 2) {

						try {
							intSample = outputStream.get(device).poll(100, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// Int: Byte 3 : Byte 2 : Byte 1 : Byte 0
						// byteBuffer ------ : ------ : 2*i : 2*i+1
						byteBuffer[2 * i] = (byte) ((intSample & 0xFF00) >> 8);
						byteBuffer[2 * i + 1] = (byte) (intSample & 0xFF);

						i++;
					}
					line.write(byteBuffer, 0, byteBuffer.length);
					if (firstWrite) {
						System.out.println("First write at: " + System.currentTimeMillis());
						firstWrite = false;
					}
				}
			} catch (NullPointerException e) {
				// Nullpointer is ok, when stopped button has
				// been pressed
				if (!stopped) {
					USPGui.stopExternally();

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							OutputAlert alert = new OutputAlert();
							alert.show();
						}

					});
				}
			}
			System.out.println("Stopped playback on: " + line);
		}

	}

}
