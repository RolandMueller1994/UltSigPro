package channel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import inputhandler.InputAdministrator;
import outputhandler.OutputAdministrator;

public class Channel implements InputDataListener, OutputDataSpeaker {

	private InputAdministrator inputAdmin;
	private OutputAdministrator outputAdmin;
	private boolean play = false;
	private String name;
	private ChannelPane pane;
	
	private boolean firstFetch = true;
	
	private LinkedBlockingQueue<LinkedList<Integer>> outputQueue = new LinkedBlockingQueue<>();
	
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
		/*LinkedList<Integer> data = new LinkedList<> ();
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TODO replace sinus values with real sound values
		for (double i = 0; i<628*2; i+=2) {
			data.add((int) (Short.MAX_VALUE*(Math.sin(i/100))));
		}*/
		try {
			LinkedList<Integer> output;
			if(firstFetch) {
				output =  outputQueue.poll(10000, TimeUnit.MILLISECONDS);
				firstFetch = false;
			} else {
				
				output = null;
				
				int pollCount = 0;
				
				while(output == null) {
					output = outputQueue.poll();
					
					Thread.sleep(1);
					pollCount++;
					
					if(pollCount>50) {
						break;
					}
				}
				
			}
			return output;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
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
		
		LinkedList<Integer> outputPackage = new LinkedList<>();
		
		for(int i=0; i<data.length; i++) {
			outputPackage.add(data[i]);
		}
		
		//System.out.println(data.length);
		
		outputQueue.offer(outputPackage);
		
		
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
		if(play) {
			outputQueue.clear();
			firstFetch = true;
		}
		this.play = play;
	}
}
