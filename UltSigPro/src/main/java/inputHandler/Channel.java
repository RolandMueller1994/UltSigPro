package inputHandler;

import java.util.List;
import java.util.Set;

public class Channel {

	public Set<String> getSubscribedDevices() {
		return InputAdministrator.getInputAdminstrator().getSubscribedDevicesName();
	}

	public void setSubscription(String deviceName) {
		InputAdministrator.getInputAdminstrator().setSubscribedDevices(deviceName);
	}
	
	public void removeSubscription(String deviceName) {
		InputAdministrator.getInputAdminstrator().removeSubscribedDevice(deviceName);
	}
}
