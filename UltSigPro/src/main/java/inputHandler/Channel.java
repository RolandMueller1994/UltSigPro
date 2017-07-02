package inputHandler;

import java.util.Set;

public class Channel {

	/**
	 * 
	 * @return All available input devices for recording.
	 */
	public Set<String> getInputDevices() {
		return InputAdministrator.getInputAdminstrator().getInputDevices();
	}

	/**
	 * TODO Just for testing at the moment. Maybe delete it later.
	 * 
	 * @return
	 */
	public Set<String> getSubscribedDevices() {
		return InputAdministrator.getInputAdminstrator().getSubscribedDevicesName();
	}

	/**
	 * Deletes the entry in the list of subscribed devices and closes the line.
	 * 
	 * @param deviceName
	 *            which has to be removed
	 */
	public void removeSubscription(String deviceName) {
		InputAdministrator.getInputAdminstrator().removeSubscribedDevice(deviceName);
	}

	/**
	 * Creates an entry in the list of subscribed devices and opens a line.
	 * 
	 * @param deviceName
	 *            which will be subscribed
	 */
	public void setSubscription(String deviceName) {
		InputAdministrator.getInputAdminstrator().setSubscribedDevices(deviceName);
	}
}
