package channel;

import java.util.HashSet;
import java.util.LinkedList;

import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

public class PluginInput implements SigproPlugin {

	private String name = "Input";
	
	@Override
	public String getName() {

		return name;
	}

	@Override
	public void setName(String name) {
		
		this.name = name;
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
		
		if(input.equals("Input")) {
			
			LinkedList<OutputDataWrapper> output = new LinkedList<> ();
			
			output.add(new OutputDataWrapper(new OutputInfoWrapper(this, "Input"), data));
			
			return output;
		}
		
		return null;
	}

	@Override
	public HashSet<String> getOutputConfig() {
		
		HashSet<String> outputs = new HashSet<>();
		outputs.add("Input");
		
		return outputs;
	}

	@Override
	public void setPlay(boolean play) {
		// TODO Auto-generated method stub

	}

	@Override
	public HashSet<String> getInputConfig() {
		
		return null;
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

}
