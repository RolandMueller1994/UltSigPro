package channel.gui;

import java.util.HashSet;
import java.util.LinkedList;

import channel.Channel;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.internal.GainBlock;

public class PluginConfigGroup extends Pane {

	private Channel channel;
	
	private HashSet<SigproPlugin> plugins = new HashSet<>();
	
	public PluginConfigGroup(Channel channel) {
		this.channel = channel;
		
		addPlugin(new GainBlock(), 100, 100);
	}
	
	private void addPlugin(SigproPlugin plugin, double xCoord, double yCoord) {
		
		plugins.add(plugin);
		
		int width = plugin.getWidth();
		int height = plugin.getHeight();
		
		double internalX = xCoord - width/2;
		double internalY = yCoord - height/2;
		
		Pane gui = plugin.getGUI();
		
		getChildren().add(gui);
		gui.setLayoutX(internalX);
		gui.setLayoutY(internalY);
		
		HashSet<String> inputs = plugin.getInputConfig();
		HashSet<String> outputs = plugin.getOutputConfig();
				
		int numberOfInputs = inputs.size();
		int numberOfOutputs = outputs.size();
		
		double inputOffset = height / numberOfInputs;
		double outputOffset = height / numberOfOutputs;
		
		int i = 0;
		
		for(String input : inputs) {
			Input inputGUI = new Input(input, internalX, internalY, i, width, height, inputOffset);
			getChildren().add(inputGUI);
			i++;
		}
		
		i = 0;
		for(String output : outputs) {
			Output outputGUI = new Output(output, internalX, internalY, i, width, height, outputOffset);
			getChildren().add(outputGUI);
		}
	}
	
}
