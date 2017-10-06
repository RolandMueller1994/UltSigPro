package channel;

import java.util.HashSet;
import java.util.LinkedList;

import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

public class PluginOutput extends SigproPlugin {

	private String name = "Output";
	
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
		
		
		if(input.equals("Output")) {
			LinkedList<OutputDataWrapper> outputData = new LinkedList<>();
			
			outputData.add(new OutputDataWrapper(new OutputInfoWrapper(this, "Output"), data));
			return outputData;
		}
		
		return null;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		return null;
	}

	@Override
	public void setPlay(boolean play) {

	}

	@Override
	public HashSet<String> getInputConfig() {
		
		HashSet<String> inputs = new HashSet<>();
		inputs.add("Output");
		
		return inputs;
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
