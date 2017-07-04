package channel;

import java.util.Set;

import inputHandler.InputAdministrator;

public class Channel implements InputDataListener {

	private InputAdministrator inputAdmin;
	private boolean play = false;
	private String name;
	
	public Channel (ChannelConfig config) {
		this.name = config.getName();
		inputAdmin = InputAdministrator.getInputAdminstrator();
		inputAdmin.registerInputDataListener(this, config.getInputDevices());
	}

	@Override
	public void putData(int[] data) {
		
		for(int i=0; i<data.length; i++) {
			System.out.println(name + ": "+ data[i]);
		}
		
	}
	
	public void addInputDevice(String device) {
		inputAdmin.addDeviceToInputDataListener(this, device);
	}
	
	public void removeInputDevice(String device) {
		inputAdmin.removeDeviceFromInputDataListener(this, device);
	}
	
	public void delete() {
		inputAdmin.removeInputDataListener(this);
	}
	
	public void setPlay(boolean play) {
		play = false;
	}
}
