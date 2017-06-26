package inputHandler;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Every SoundInputDevice object corresponds one input source (e.g. microphone,
 * wave-file, ...). Automatically opens a line to the requested mixer. However,
 * does not start to send data yet, but waits for a request of the
 * {@link InputAdministrator} to send data.
 * 
 * 
 * @author Kone
 * 
 */
public class SoundInputDevice {

	Mixer.Info[] allSoundInputDevices;
	String[] allSoundInputDevicesString;
	Mixer selectedInputDevice;
	TargetDataLine targetLine;

	/**
	 * Creates a new sound input device prototype. No specific device selected
	 * yet. Selection of the device follows later through the user.
	 */
	public SoundInputDevice() {

		// selectedInputDevice and targetLine get initialized later through user
		// selection by a list of all available sound input devices.
		this.allSoundInputDevices = AudioSystem.getMixerInfo();
		this.allSoundInputDevicesString = collectSoundInputDevices();
		this.selectedInputDevice = null;
		this.targetLine = null;
	}

	/**
	 * Opens a line to the selected sound input device.
	 * 
	 * @param deviceSelection
	 *            Element of the {@link Mixer.Info} array with all available
	 *            sound devices.
	 */
	public void reserveSoundInputDevice(int deviceSelection) {

		AudioSystem.getMixer(allSoundInputDevices[deviceSelection]);
		DataLine.Info infoTarget = new DataLine.Info(TargetDataLine.class, null);
		try {
			targetLine = (TargetDataLine) selectedInputDevice.getLine(infoTarget);
			targetLine.open();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes and stops transmitting data from the line from the sound input
	 * device.
	 */
	public void closeDataLine() {

		targetLine.stop();
		targetLine.close();
	}

	/**
	 * Checks which of the installed sound devices support
	 * {@link TargetDataLine} as lines. Those are the only one which are
	 * appropriate as input devices.
	 * 
	 * @return String array with all input devices.
	 */
	public String[] collectSoundInputDevices() {

		Mixer mixer;
		DataLine.Info info;
		int counter = 0;
		String[] inputDevices;

		info = new DataLine.Info(TargetDataLine.class, null);
		inputDevices = new String[allSoundInputDevices.length];
		for (Mixer.Info i : allSoundInputDevices) {
			mixer = AudioSystem.getMixer(i);
			if (mixer.isLineSupported(info)) {
				inputDevices[counter] = allSoundInputDevices[counter].getName();
				counter++;
			}
		}

		return inputDevices;
	}

	/**
	 * 
	 * @return The selected input device of this object.
	 */
	public Mixer getSelectedInputDevice() {
		return selectedInputDevice;
	}

	/**
	 * User can select which device he wants to use as input. Selection follows
	 * through a drop down menu on the user interface. 
	 * 
	 * @param deviceSelection Selection made by user.
	 */
	public void setSelectedInputDevice(int deviceSelection) {

		// Selection made by user through the pull down menu
		if (this.selectedInputDevice != allSoundInputDevices[deviceSelection]) {
			if (targetLine != null)
				closeDataLine();
			this.selectedInputDevice = (Mixer) allSoundInputDevices[deviceSelection];
			reserveSoundInputDevice(deviceSelection);
		}
	}

	/**
	 * 
	 * @return All sound input devices.
	 */
	public String[] getAllSoundInputDevicesString() {
		return allSoundInputDevicesString;
	}

}
