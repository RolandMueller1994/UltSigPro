package channel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import inputhandler.InputAdministrator;
import outputhandler.OutputAdministrator;

public class Channel implements InputDataListener, OutputDataSpeaker {

	private InputAdministrator inputAdmin;
	private OutputAdministrator outputAdmin;
	private boolean play = false;
	private String name;
	private ChannelPane pane;
	
	private int remaining = 0;
	int distance = (int) (44100/44);
	
	public Channel (ChannelPane pane, ChannelConfig config) {
		this.name = config.getName();
		this.pane = pane;
		inputAdmin = InputAdministrator.getInputAdminstrator();
		inputAdmin.registerInputDataListener(this, config.getInputDevices());
		outputAdmin = OutputAdministrator.getOutputAdministrator();
		outputAdmin.registerOutputDevices(this, config.getOutputDevices());
	}
	
	@Override
	public LinkedList<Integer> fetchData() {
		// passes data from channel to OutputAdmin
		LinkedList<Integer> data = new LinkedList<> ();
		
		//TODO replace sinus values with real sound values
		for (double i = 0; i<628; i+=2) {
			data.add((int) (Short.MAX_VALUE*(Math.sin(i/100))));
		}
		return data;
	}

	@Override
	public void putData(int[] data) {
		
		LinkedList<Double> waveChartData = new LinkedList<> ();
		
		int pos = 0;
		
		if(distance-remaining < data.length) {
			waveChartData.add(((double) data[distance-remaining])/(double) Short.MAX_VALUE);
			pos = distance-remaining;
			
			while(pos + distance < data.length) {
				waveChartData.add(((double) data[pos + distance])/(double) Short.MAX_VALUE);
				pos = pos + distance;
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
	
	public void addOutputDevice(String device) {
		outputAdmin.addSoundOutputDeviceToSpeaker(this, device);
	}
	
	public void removeOutputDevice(String device) {
		outputAdmin.removeDeviceFromOutputDataSpeaker(this, device);
	}
	
	public void delete() {
		inputAdmin.removeInputDataListener(this);
	}
	
	public void setPlay(boolean play) {
		
		play = false;
		remaining = 0;
		
	}
}
