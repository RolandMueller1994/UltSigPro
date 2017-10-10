package channel.gui;

import java.util.HashSet;
import java.util.LinkedList;

import channel.Channel;
import channel.PluginInput;
import channel.PluginOutput;
import gui.USPGui;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.internal.GainBlock;

public class PluginConfigGroup extends Pane {

	private Channel channel;

	private HashSet<SigproPlugin> plugins = new HashSet<>();

	private PluginConnection workCon = null;
	private HashSet<PluginConnection> allConnections = new HashSet<>();

	public PluginConfigGroup(Channel channel) {
		this.channel = channel;

		addPlugin(new PluginInput(), 50, 100);
		addPlugin(new PluginOutput(), USPGui.stage.getWidth() - 50, 100);

		addPlugin(new GainBlock(), 300, 100);

		addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				drawLine(event);
			}

		});

		addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				boolean hovered = false;

				for (SigproPlugin plugin : plugins) {
					if (plugin.checkHovered()) {
						hovered = true;
						break;
					}
				}

				if (!hovered) {
					if(workCon != null) {
						workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
								sceneToLocal(event.getSceneX(), event.getSceneY()).getY());
					}
				}
			}

		});

	}
	
	public boolean checkForHover() {
		if(workCon == null || workCon.getActLine().isHorizontal()) {
			return true;
		}
		return false;
	}

	private void drawLine(MouseEvent event) {
		if (workCon != null) {
			double localX;
			double localY;

			localX = sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
			localY = sceneToLocal(event.getSceneY(), event.getSceneY()).getY();

			workCon.drawLine(localX, localY);
		}
	}

	public void connectionStartStop(ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {

		if (workCon == null) {
			workCon = new PluginConnection(this, endpoint, xCoord, yCoord);
			endpoint.addLine(workCon.getActLine());
		} else {
			if(!workCon.getActLine().checkCoordinates(xCoord, yCoord)) {
				workCon.devideActLine(xCoord, yCoord);
			}
			endpoint.addLine(workCon.getActLine());
			workCon.endPluginConnection(endpoint, xCoord, yCoord);
			allConnections.add(workCon);
			workCon = null;	
		}

	}

	private void addPlugin(SigproPlugin plugin, double xCoord, double yCoord) {

		plugins.add(plugin);

		int width = plugin.getWidth();
		int height = plugin.getHeight();

		double internalX = xCoord - width / 2;
		double internalY = yCoord - height / 2;

		Pane gui = plugin.getGUI();

		getChildren().add(gui);
		gui.setLayoutX(internalX);
		gui.setLayoutY(internalY);

		HashSet<String> inputs = plugin.getInputConfig();
		HashSet<String> outputs = plugin.getOutputConfig();

		int numberOfInputs = inputs.size();
		int numberOfOutputs = outputs.size();

		int i = 0;

		if (numberOfInputs > 0) {
			double inputOffset = height / numberOfInputs;
			for (String input : inputs) {
				Input inputGUI = new Input(this, input, internalX, internalY, i, width, height, inputOffset);
				getChildren().add(inputGUI);
				plugin.addInput(inputGUI);
				i++;
			}
		}

		if (numberOfOutputs > 0) {
			double outputOffset = height / numberOfOutputs;
			i = 0;
			for (String output : outputs) {
				Output outputGUI = new Output(this, output, internalX, internalY, i, width, height, outputOffset);
				getChildren().add(outputGUI);
				plugin.addOutput(outputGUI);
				i++;
			}
		}
	}

}
