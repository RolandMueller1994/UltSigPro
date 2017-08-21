package channel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import inputhandler.InputAdministrator;

public class Channel implements InputDataListener {

	private InputAdministrator inputAdmin;
	private boolean play = false;
	private String name;
	private ChannelPane pane;
	
	private int remaining = 0;
	int distanz = (int) (44100/44);
	
	public Channel (ChannelPane pane, ChannelConfig config) {
		this.name = config.getName();
		this.pane = pane;
		inputAdmin = InputAdministrator.getInputAdminstrator();
		inputAdmin.registerInputDataListener(this, config.getInputDevices());
	}

	@Override
	public void putData(int[] data) {
		
		LinkedList<Double> waveChartData = new LinkedList<> ();
		
		int pos = 0;
		
		if(distanz-remaining < data.length) {
			waveChartData.add(((double) data[distanz-remaining])/(double) Short.MAX_VALUE);
			pos = distanz-remaining;
			
			while(pos + distanz < data.length) {
				waveChartData.add(((double) data[pos + distanz])/(double) Short.MAX_VALUE);
				pos = pos + distanz;
			}
			remaining = data.length - pos;			
		} else {
			remaining = remaining + (data.length - pos);
		}
		
		
		pane.insertWaveChartData(waveChartData);
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
		remaining = 0;
		
	}
}
