package inputHandler;

import java.util.List;

public class Listener {

	public List<String> getRegisteredDevices() {
		return InputAdministrator.getInputAdminstrator().getRegisteredDevicesName();
	}

	public void setSubscription(String deviceName) {
		InputAdministrator.getInputAdminstrator().setSubscribedDevices(deviceName);
	}
}
