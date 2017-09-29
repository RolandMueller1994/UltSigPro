package plugins.sigproplugins.signalrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import gui.USPGui;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

/**
 * Class which is used as {@link SigproPlugin}. It has multiple inputs and one
 * output. The values of the inputs will be added and written to the output.
 * 
 * @author roland
 *
 */
public class SignalAdder implements SigproPlugin {

	private HashMap<String, LinkedBlockingQueue<int[]>> inputs = new HashMap<>();
	private HashMap<String, double[]> dataBuffer = new HashMap<>();

	private boolean play = false;
	
	private int distributionSize = 100;
	
	private String name = "Add";

	/**
	 * Empty default constructor. Needed for instantiation by reflection. 
	 */
	public SignalAdder() {
		
	}
	
	/**
	 * Creates a new add block.
	 * 
	 * @param size
	 *            The number of inputs.
	 */
	public SignalAdder(int size) {

		for (int i = 0; i < size; i++) {
			inputs.put(new Integer(i).toString(), new LinkedBlockingQueue<int[]>());
		}

	}

	@Override
	public LinkedList<OutputDataWrapper> putData(String input, double[] data) {

		dataBuffer.put(input, data);
		
		boolean first = true;
		
		if(dataBuffer.size() == inputs.size()) {
			double[] outputData = new double[distributionSize];
			
			for(double[] addData : dataBuffer.values()) {
				for(int i=0; i<distributionSize; i++) {
					if(first) {
						outputData[i] = addData[i];
					} else {
						outputData[i] += addData[i];
					}
				}				
				first = false;
			}
			
			LinkedList<OutputDataWrapper> outputWrappers = new LinkedList<>();
			outputWrappers.add(new OutputDataWrapper(new OutputInfoWrapper(this, "out"), outputData));
			
			return outputWrappers;
		}
		
		return null;
	}

	private void play() {
		
	}

	private void stop() {
		play = false;

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (LinkedBlockingQueue<int[]> queue : inputs.values()) {
			queue.clear();
		}
	}

	@Override
	public String getName() {

		return "Add";
	}

	@Override
	public String getVersion() {

		return "1.0.0";
	}

	@Override
	public Pane getGUI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputConfig = new HashSet<>();
		outputConfig.add("out");

		return outputConfig;
	}

	@Override
	public void setPlay(boolean play) {
		if (play) {
			play();
		} else {
			stop();
		}

	}

	@Override
	public void setName(String name) {
		
		this.name = name;
	}

}
