package plugins.sigproplugins.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

/**
 * Internal plugin. Proceeds a clean gain operation.
 * 
 * @author roland
 *
 */
public class GainBlock implements SigproPlugin {

	private double gain = 1.0;
	
	private String name = "Gain";

	/**
	 * Empty default constructor. Needed for instantiation by reflection. 
	 */
	public GainBlock() {

	}

	@Override
	public String getName() {

		return name;
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
	public LinkedList<OutputDataWrapper> putData(String input, double[] data) {

		if (input.contentEquals("in")) {
			for (int i = 0; i < data.length; i++) {
				data[i] = gain * data[i];
			}
		}

		LinkedList<OutputDataWrapper> output = new LinkedList<> ();
		output.add(new OutputDataWrapper(new OutputInfoWrapper(this, "out"), data));
		
		return output;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputs = new HashSet<>();
		outputs.add("out");

		return outputs;
	}

	@Override
	public void setPlay(boolean play) {
		// Nothing to do here

	}

	@Override
	public void setName(String name) {
	
		this.name = name;
	}

}
