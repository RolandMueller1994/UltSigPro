package channel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import inputHandler.InputAdministrator;

public class Channel implements InputDataListener {

	private InputAdministrator inputAdmin;
	private boolean play = false;
	private String name;
	private FileWriter writer;
	
	public Channel (ChannelConfig config) {
		this.name = config.getName();
		inputAdmin = InputAdministrator.getInputAdminstrator();
		inputAdmin.registerInputDataListener(this, config.getInputDevices());
		
		try {
			writer = new FileWriter(name + ".values");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void putData(int[] data) {
		
		for(int i=0; i<data.length; i++) {
			try {
				writer.append(name + new Integer(data[i]).toString() + System.lineSeparator());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
