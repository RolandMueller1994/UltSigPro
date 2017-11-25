package outputhandler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import channel.Channel;
import channel.InputDataListener;
import channel.OutputDataSpeaker;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import resourceframework.GlobalResourceProvider;
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
	private static HashMap<String, FileOutputStream> waveFileStreams;

	private ScheduledThreadPoolExecutor executor;

	private long latency;
	private int byteBufferSize = 100;

	// SoundOutputDevice -> Signal processing Channel -> Queue with sound values
	private HashMap<String, HashSet<OutputDataSpeaker>> distributionQueue = new HashMap<>();
	private HashSet<OutputDataSpeaker> allSpeaker = new HashSet<>();

	private HashMap<OutputDataSpeaker, Collection<String>> distributionMap = new HashMap<>();
	private HashMap<String, LinkedList<byte[]>> waveData = new HashMap<>();

	private HashMap<OutputDataSpeaker, HashMap<String, Double>> outputLevelMultiplier = new HashMap<>();

	public static OutputAdministrator getOutputAdministrator() {

		if (outputAdministrator == null) {
			outputAdministrator = new OutputAdministrator();
			allSoundOutputDevices = new HashMap<>();
			selectedDevices = new HashMap<>();
			sourceDataLines = new HashMap<>();
			waveFileStreams = new HashMap<>();
		}
		return outputAdministrator;
	}

	private OutputAdministrator() {
		GlobalResourceProvider resProv = GlobalResourceProvider.getInstance();

		if (resProv.checkRegistered("latency")) {
			try {
				latency = (long) resProv.getResource("latency");
			} catch (ResourceProviderException e) {
				// Won't happen due to previous check
			}
		} else {
			latency = 10;
		}
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

	public void startOutput() {

		executor = new ScheduledThreadPoolExecutor(1);

		executor.scheduleAtFixedRate(new OutputRunnable(), 0, 1, TimeUnit.MILLISECONDS);
	}

	private void writeInt(final DataOutputStream output, final int value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
		output.write(value >> 16);
		output.write(value >> 24);
	}

	private void writeShort(final DataOutputStream output, final short value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
	}

	private void writeString(final DataOutputStream output, final String value) throws IOException {
		for (int i = 0; i < value.length(); i++) {
			output.write(value.charAt(i));
		}
	}

	private void createWaveFiles() {

		for (Map.Entry<String, FileOutputStream> entry : waveFileStreams.entrySet()) {
			DataOutputStream output = new DataOutputStream(entry.getValue());
			try {

				final int numberOfChannels = 2;
				final int bytesPerSample = 2;
				final int sampleRate = 44100;
				final int totalByteNumber = waveData.get(entry.getKey()).size() * byteBufferSize;
				final int frameSizePerSample = numberOfChannels * bytesPerSample;

				// RIFF section ---------------------
				writeString(output, "RIFF");

				// 36 for the following format section
				writeInt(output, totalByteNumber * frameSizePerSample + 36);
				writeString(output, "WAVE");
				// ----------------------------------

				// format section -------------------
				// header signature (space after fmt necessary)
				writeString(output, "fmt ");

				// following format section size
				writeInt(output, 16);

				// audio format (1 = PCM)
				writeShort(output, (short) 1);

				// number of channels
				writeShort(output, (short) numberOfChannels);

				// sample rate
				writeInt(output, sampleRate);

				// byte rate (byte/second)
				writeInt(output, sampleRate * frameSizePerSample);

				// frame size
				writeShort(output, (short) frameSizePerSample);

				// bits per sammple
				writeShort(output, (short) (8 * bytesPerSample));
				// ----------------------------------

				// data section ---------------------
				// header signature
				writeString(output, "data");

				// following data section size
				writeInt(output, totalByteNumber * frameSizePerSample);

				while (!waveData.get(entry.getKey()).isEmpty()) {
					byte[] b = waveData.get(entry.getKey()).removeFirst();
					output.write(b);
				}
				// ----------------------------------

				entry.getValue().close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void stopPlayback() {
		executor.shutdownNow();
		createWaveFiles();
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

	public synchronized void setWaveFileEntries(HashMap<String, File> waveFiles, OutputDataSpeaker speaker) {

		Collection<String> fileNames = waveFiles.keySet();
		distributionMap.put(speaker, fileNames);
		for (String fileName : fileNames) {
			if (!distributionQueue.containsKey(fileName)) {
				distributionQueue.put(fileName, new HashSet<OutputDataSpeaker>());
			}
			distributionQueue.get(fileName).add(speaker);
			waveData.put(fileName, new LinkedList<>());
			try {
				waveFileStreams.put(fileName, new FileOutputStream(waveFiles.get(fileName)));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			HashMap<String, Double> entry = new HashMap<>();
			entry.put(fileName, 1.0);
			if (outputLevelMultiplier.containsKey(speaker)) {
				outputLevelMultiplier.get(speaker).put(fileName, 1.0);
			} else {
				outputLevelMultiplier.put(speaker, entry);
			}
			allSpeaker.add(speaker);
		}
	}

	public synchronized void removeWaveFileEntries(HashMap<String, File> waveFiles, OutputDataSpeaker speaker) {

		for (String fileName : waveFiles.keySet()) {
			distributionQueue.get(fileName).remove(speaker);
			if (distributionQueue.get(fileName).isEmpty()) {
				distributionQueue.remove(fileName);
				waveData.remove(fileName);
				waveFileStreams.remove(fileName);
			}

			outputLevelMultiplier.get(speaker).remove(fileName);
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
				distributionQueue.put(device, new HashSet<OutputDataSpeaker>());
			}
			distributionQueue.get(device).add(speaker);
			allSpeaker.add(speaker);

			HashMap<String, Double> inputLevel = new HashMap<>();
			inputLevel.put(device, 1.0);
			if (outputLevelMultiplier.containsKey(speaker)) {
				outputLevelMultiplier.get(speaker).putAll(inputLevel);
			} else {
				outputLevelMultiplier.put(speaker, inputLevel);
			}
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
			distributionQueue.put(device, new HashSet<OutputDataSpeaker>());
			outputLevelMultiplier.put(speaker, new HashMap<String, Double>());
		}
		distributionQueue.get(device).add(speaker);
		setSelectedDevice(device);
		allSpeaker.add(speaker);
		outputLevelMultiplier.get(speaker).put(device, 1.0);
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
		}
		outputLevelMultiplier.get(speaker).remove(device);
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
		allSpeaker.remove(speaker);
		for (String device : distributionQueue.keySet()) {
			// Search for all SoundOutputDevices with a queue to this speaker
			if (distributionQueue.get(device).contains(speaker)) {
				removeSelectedDevice(device);
				distributionQueue.get(device).remove(speaker);

				// checks if there are any entries left for this
				// SoundOutputDevice or if the SoundOutputDevice receives
				// no more longer any data from any speaker
				if (distributionQueue.get(device).isEmpty()) {
					distributionQueue.remove(device);
				}
			}
		}
		outputLevelMultiplier.remove(speaker);

	}

	public void outputLevelMultiplierChanged(OutputDataSpeaker speaker, String device, double multiplier) {
		outputLevelMultiplier.get(speaker).put(device, multiplier);
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

	private class OutputRunnable implements Runnable {

		private boolean first = true;
		private boolean firstOutput = true;
		private int inputPackageSize = byteBufferSize;
		private int outputPackageSize = inputPackageSize * 2;

		private HashMap<OutputDataSpeaker, int[]> data = new HashMap<>();

		public OutputRunnable() {

		}

		@Override
		public void run() {

			if (first) {
				InputAdministrator.getInputAdminstrator().waitForStartup();
				first = false;
				try {
					Thread.sleep(latency);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			boolean missing = false;
			while (!missing) {

				for (OutputDataSpeaker speaker : allSpeaker) {
					if (!data.containsKey(speaker)) {
						int[] speakerData = speaker.fetchData();

						if (speakerData == null) {
							missing = true;
						} else {
							data.put(speaker, speakerData);
						}
					}
				}

				if (missing) {
					break;
				}

				if (firstOutput) {
					System.out.println("First data output at: " + System.currentTimeMillis());
					firstOutput = false;
				}

				for (Map.Entry<String, HashSet<OutputDataSpeaker>> entry : distributionQueue.entrySet()) {
					boolean firstData = true;

					int[] outData = new int[inputPackageSize];
					for (OutputDataSpeaker speaker : entry.getValue()) {
						int[] inData = data.get(speaker);
						double multiplier = outputLevelMultiplier.get(speaker).get(entry.getKey());
						if (multiplier != 1) {
							if (firstData) {
								for (int i = 0; i < inputPackageSize; i++) {
									outData[i] = (int) (multiplier * inData[i]);
									firstData = false;
								}
							} else {
								for (int i = 0; i < inputPackageSize; i++) {
									outData[i] = (int) (outData[i] + (multiplier * inData[i]));
								}
							}
						} else {
							if (firstData) {
								for (int i = 0; i < inputPackageSize; i++) {
									outData[i] = inData[i];
									firstData = false;
								}
							} else {
								for (int i = 0; i < inputPackageSize; i++) {
									outData[i] += inData[i];
								}
							}
						}

						LinkedList<Integer> soundValueData = new LinkedList<>();

						byte[] outByteData = new byte[outputPackageSize];
						byte[] waveByteData = new byte[2 * outputPackageSize];

						for (int i = 0; i < inputPackageSize; i++) {
							int intSample = outData[i];
							soundValueData.add(intSample);

							outByteData[2 * i] = (byte) ((intSample & 0xFF00) >> 8);
							outByteData[2 * i + 1] = (byte) (intSample & 0xFF);

							waveByteData[4 * i + 1] = outByteData[2 * i];
							waveByteData[4 * i] = outByteData[2 * i + 1];
							waveByteData[4 * i + 3] = outByteData[2 * i];
							waveByteData[4 * i + 2] = outByteData[2 * i + 1];
						}

						if (waveFileStreams.containsKey(entry.getKey())) {
							waveData.get(entry.getKey()).add(waveByteData);
							SoundLevelBar.getSoundLevelBar().updateSoundLevelItems(entry.getKey(), soundValueData,
									false);
						} else {
							sourceDataLines.get(entry.getKey()).write(outByteData, 0, outputPackageSize);
							SoundLevelBar.getSoundLevelBar().updateSoundLevelItems(entry.getKey(), soundValueData,
									false);
						}
					}
				}
				data.clear();
			}
		}
	}
	
	public boolean deviceAvailable(String deviceName) {
		collectSoundOutputDevices();
		if (allSoundOutputDevices.containsKey(deviceName)) {
			return true;
		}
		return false;
	}
}
