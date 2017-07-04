package OutputHandler;

import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class OutputAdministrator {

	private static OutputAdministrator outputAdministrator;
	private static HashMap<String, Mixer> allSoundOutputDevices;
	private static HashMap<String, Mixer> selectedDevices;
	private static HashMap<String, SourceDataLine> sourceDataLines;
	private static boolean stopped = false;

	public static OutputAdministrator getOutputAdministrator() {
		
		if(outputAdministrator == null){
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
		selectedDevices.remove(deviceName);
		sourceDataLines.get(deviceName).stop();
		sourceDataLines.get(deviceName).close();
		sourceDataLines.remove(deviceName);
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
}
