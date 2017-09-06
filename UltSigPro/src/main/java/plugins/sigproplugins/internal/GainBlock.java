package plugins.sigproplugins.internal;

import java.util.HashMap;
import java.util.HashSet;

import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.signalrouting.DataDestinationInterface;

/**
 * Internal plugin. Proceeds a clean gain operation.
 * 
 * @author roland
 *
 */
public class GainBlock implements SigproPlugin {

	private double gain = 1.0;
	private DataDestinationInterface dest;

	/**
	 * Empty default constructor. Needed for instantiation by reflection. 
	 */
	public GainBlock() {

	}

	@Override
	public String getName() {

		return "Gain";
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
	public void putData(String input, int[] data) {

		if (input.contentEquals("in") && dest != null) {
			for (int i = 0; i < data.length; i++) {
				data[i] = (int) (gain * data[i]);
			}
		}

		dest.putData(data);
	}

	@Override
	public void setOutputConfig(HashMap<String, DataDestinationInterface> outputConfig) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOutputConfig(String output, DataDestinationInterface dest) {

		if (output.equals("out")) {
			this.dest = dest;
		}

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

}
