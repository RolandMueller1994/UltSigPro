package channel.gui;

import java.awt.MouseInfo;
import java.util.HashSet;
import java.util.LinkedList;

import channel.Channel;
import channel.PluginInput;
import channel.PluginOutput;
import gui.USPGui;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.internal.GainBlock;

public class PluginConfigGroup extends Pane {

	private Channel channel;
	private ScrollPane parent;

	private static double scrollOffset = 40;

	private HashSet<SigproPlugin> plugins = new HashSet<>();

	private PluginConnection workCon = null;
	private HashSet<PluginConnection> allConnections = new HashSet<>();

	public PluginConfigGroup(Channel channel, ScrollPane parent) {
		this.channel = channel;
		this.parent = parent;

		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);
		// setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

		addPlugin(new PluginInput(), 50, 100);
		addPlugin(new PluginOutput(), USPGui.stage.getWidth() - 50, 100);

		addPlugin(new GainBlock(), 300, 100);

		heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				parent.setVvalue(parent.getVmax());
				drawLine(MouseInfo.getPointerInfo().getLocation().getX(),
						MouseInfo.getPointerInfo().getLocation().getY());
			}

		});

		widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				parent.setHvalue(parent.getHmax());
				drawLine(MouseInfo.getPointerInfo().getLocation().getX(),
						MouseInfo.getPointerInfo().getLocation().getY());
			}

		});

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
					if (workCon != null) {
						workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
								sceneToLocal(event.getSceneX(), event.getSceneY()).getY());
					}
				}
			}

		});

	}

	public boolean checkForHover() {
		if (workCon == null || workCon.getActLine().isHorizontal()) {
			return true;
		}
		return false;
	}

	private void drawLine(MouseEvent event) {
		drawLine(event.getScreenX(), event.getScreenY());
	}

	private void drawLine(double x, double y) {
		if(workCon != null) {
			double localX;
			double localY;
			
			localX = screenToLocal(x, y).getX();
			localY = screenToLocal(x, y).getY();
			
			workCon.drawLine(localX, localY);			
		}
	}

	public void connectionStartStop(ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {

		if (workCon == null) {
			workCon = new PluginConnection(this, endpoint, xCoord, yCoord);
			endpoint.addLine(workCon.getActLine());
		} else {
			if (!workCon.getActLine().checkCoordinates(xCoord, yCoord)) {
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

		plugin.registerMaxCoordinatesUpdateListener(this);

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

	public void updateMaxCoordinatesOfComponent(MaxCoordinatesInterface component) {

		if (component.getMaxX() + scrollOffset > getWidth()) {
			setPrefWidth(component.getMaxX() + scrollOffset);
		}

		if (component.getMaxY() + scrollOffset > getHeight()) {
			setPrefHeight(component.getMaxY() + scrollOffset);
		}
	}

	public boolean isDrawing() {

		return workCon != null;
	}

}
