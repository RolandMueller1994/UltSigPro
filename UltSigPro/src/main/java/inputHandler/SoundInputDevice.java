package inputHandler;

import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Every SoundInputDevice object corresponds one input source (e.g. microphone,
 * wave-file, ...). Automatically opens a line to the requested mixer. However,
 * does not start to send data yet, but waits for a request of the
 * {@linkplain InputAdministrator} to send data.
 * 
 * 
 * @author Kone
 * 
 */
public class SoundInputDevice {

	private Mixer.Info[] allSoundDevices;
	private List<String> allSoundInputDevicesList = new LinkedList<>();
	private int selectedDeviceNumber;
	private Mixer selectedInputDevice;
	private String selectedInputDeviceName;

	/**
	 * Creates a new sound input device prototype. No specific device selected
	 * yet. Selection of the device follows later through the user.
	 */
	public SoundInputDevice() {

		// selectedInputDevice and targetLine get initialized later through user
		// selection by a list of all available sound input devices.
		this.allSoundDevices = AudioSystem.getMixerInfo();
		this.allSoundInputDevicesList = collectSoundInputDevices();
		this.selectedInputDevice = null;
		this.selectedInputDeviceName = null;
	}

	/*	*//**
			 * Closes and stops transmitting data from the line from the sound
			 * input device.
			 *//*
			 * public void closeDataLine() {
			 * 
			 * targetLine.stop(); targetLine.close(); }
			 */

	/**
	 * Checks which of the installed sound devices support
	 * {@linkplain TargetDataLine} as lines. Those are the only one which are
	 * appropriate as input devices.
	 * 
	 * @return String array with all input devices.
	 */
	public List<String> collectSoundInputDevices() {

		Mixer mixer;
		DataLine.Info info;
		int deviceCounter = 0;
		List<String> inputDevices = new LinkedList<>();

		info = new DataLine.Info(TargetDataLine.class, null);

		for (Mixer.Info i : allSoundDevices) {
			mixer = AudioSystem.getMixer(i);
			if (mixer.isLineSupported(info)) {
				inputDevices.add(allSoundDevices[deviceCounter].toString());
			}
			deviceCounter++;
		}
		return inputDevices;
	}

	/**
	 * 
	 * @return All sound input devices as String.
	 */
	public List<String> getAllSoundInputDevicesString() {

		return allSoundInputDevicesList;
	}

	/**
	 * 
	 * @return The selected input device of this object.
	 */
	public Mixer getSelectedInputDevice() {
		return selectedInputDevice;
	}

	public String getSelectedInputDeviceName() {
		return selectedInputDeviceName;
	}

	/**
	 * User can select which device he wants to use as input. Selection follows
	 * through a drop down menu on the user interface.
	 * 
	 * @param deviceSelection
	 *            Selection made by user.
	 */
	public void setSelectedInputDevice(int deviceSelection) {

		// Selection made by user through the pull down menu
		if (this.selectedDeviceNumber != deviceSelection) {
			Mixer mixer = AudioSystem.getMixer(allSoundDevices[deviceSelection]);
			/*
			 * if (targetLine != null) closeDataLine();
			 */
			this.selectedDeviceNumber = deviceSelection;
			this.selectedInputDevice = mixer;
			this.selectedInputDeviceName = allSoundDevices[deviceSelection].getName();
			// reserveSoundInputDevice();
			InputAdministrator.getInputAdminstrator().setRegisteredDevice(selectedInputDeviceName, mixer);
		}
	}

	/**
	 * Opens a line to the selected sound input device.
	 * 
	 * @param deviceSelection
	 *            Element of the {@linkplain Mixer.Info} array with all
	 *            available sound devices.
	 */
	/*
	 * public void reserveSoundInputDevice() {
	 * 
	 * AudioSystem.getMixer(allSoundDevices[selectedDeviceNumber]);
	 * DataLine.Info infoTarget = new DataLine.Info(TargetDataLine.class, null);
	 * try { targetLine = (TargetDataLine)
	 * selectedInputDevice.getLine(infoTarget); targetLine.open(); } catch
	 * (LineUnavailableException e) { e.printStackTrace(); } }
	 */

}
