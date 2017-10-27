package channel.gui;

import java.awt.MouseInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import channel.Channel;
import channel.PluginInput;
import channel.PluginOutput;
import channel.gui.PluginConnection.ConnectionLine;
import channel.gui.PluginConnection.LineDevider;
import gui.USPGui;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.internal.GainBlock;

/**
 * This class is a container for plugins, their gui and connections between the
 * plugins.
 * 
 * @author roland
 *
 */
public class PluginConfigGroup extends Pane {

	private Channel channel;
	private ScrollPane parent;

	private static final double scrollOffset = 10;
	private static final long scrollSpeed = 10;

	private HashSet<SigproPlugin> plugins = new HashSet<>();

	private PluginConnection workCon = null;
	private HashSet<PluginConnection> allConnections = new HashSet<>();
	
	private ConnectionLine deletionLine;

	private boolean lineHovered = false;

	private MaxCoordinatesInterface maxXComponent;
	private MaxCoordinatesInterface maxYComponent;
	private HashMap<MaxCoordinatesInterface, Point2D> componentMaxPositions = new HashMap<>();
	double maxX;
	double maxY;

	/**
	 * Creates a new {@line PluginConfigGroup}
	 * 
	 * @param channel
	 *            The {@link Channel} which will get plugins and their
	 *            connections from this class. Must not be null.
	 * @param parent
	 *            The parent {@link ScrollPane}. Must not be null.
	 */
	public PluginConfigGroup(@Nonnull Channel channel, @Nonnull ScrollPane parent) {
		this.channel = channel;
		this.parent = parent;

		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);

		addPlugin(new PluginInput(), 50, 100);
		addPlugin(new PluginOutput(), USPGui.stage.getWidth() - 50, 100);

		addPlugin(new GainBlock(), 300, 100);
		addPlugin(new GainBlock(), 600, 100);

		heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				try {
					Thread.sleep(scrollSpeed);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				parent.setVvalue(parent.getVmax());
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						drawLine(MouseInfo.getPointerInfo().getLocation().getX(),
								MouseInfo.getPointerInfo().getLocation().getY());

						for (SigproPlugin plugin : plugins) {
							if (plugin.isDragged()) {
								plugin.drag(MouseInfo.getPointerInfo().getLocation().getX(),
										MouseInfo.getPointerInfo().getLocation().getY());
								return;
							}
						}
					}

				});
			}
		});

		widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				try {
					Thread.sleep(scrollSpeed);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				parent.setHvalue(parent.getHmax());
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						drawLine(MouseInfo.getPointerInfo().getLocation().getX(),
								MouseInfo.getPointerInfo().getLocation().getY());

						for (SigproPlugin plugin : plugins) {
							if (plugin.isDragged()) {
								plugin.drag(MouseInfo.getPointerInfo().getLocation().getX(),
										MouseInfo.getPointerInfo().getLocation().getY());
								return;
							}
						}
					}

				});
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

				if (!hovered && !lineHovered) {
					if (workCon != null) {
						workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
								sceneToLocal(event.getSceneX(), event.getSceneY()).getY());
					}
				}
			}
		});

	}

	/**
	 * Sets if a {@link ConnectionLine} or {@link LineDevider} is hovered or
	 * not. Events at mouse clicks won't cause any actions.
	 * 
	 * @param hovered
	 *            True if a line or divider is hovered, false if not.
	 */
	public void setLineHovered(boolean hovered) {
		lineHovered = hovered;
	}

	/**
	 * Cancel the drawing of the current {@link ConnectionLine}.
	 */
	public void escapeLineDrawing() {
		if (workCon != null && workCon.getActLine() != null) {
			workCon.getActLine().delete();
			workCon = null;
		}
	}

	/**
	 * Gets the current {@link PluginConnection} which is used for drawing.
	 * 
	 * @return The current {@link PluginConnection} or null if no new lines will
	 *         be drawn.
	 */
	@CheckForNull
	public PluginConnection getWorkCon() {

		return workCon;
	}

	/**
	 * Stops the drawing of lines externally.
	 */
	public void finalizeDrawing() {
		workCon = null;
	}

	private void drawLine(MouseEvent event) {
		drawLine(event.getScreenX(), event.getScreenY());
	}

	private void drawLine(double x, double y) {
		if (workCon != null) {
			double localX;
			double localY;

			localX = screenToLocal(x, y).getX();
			localY = screenToLocal(x, y).getY();

			workCon.drawLine(localX, localY);
		}
	}

	/**
	 * Starts or stops the drawing of {@link PluginConnection}s. If we currently
	 * draw lines we will stop the drawing, if not we will create a new
	 * connection and start drawing.
	 * 
	 * @param endpoint
	 *            The {@link ConnectionLineEndpointInterface} at which the
	 *            connection should start or end. Must not be null.
	 * @param xCoord
	 *            The x-coordinate to start or end.
	 * @param yCoord
	 *            The y-coordinate to start or end.
	 */
	public void connectionStartStop(@Nonnull ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {

		if (workCon == null) {
			workCon = new PluginConnection(this, endpoint, xCoord, yCoord);
			endpoint.addLine(workCon.getActLine());
		} else {
			if (!workCon.getActLine().isHorizontal()) {
				workCon.changeOrientation(xCoord, yCoord);
			}

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

	/**
	 * Will be called if a component within this pane changed its coordinates.
	 * 
	 * @param component
	 *            The {@link MaxCoordinatesInterface} which changed its
	 *            coordinates. Must not be null.
	 */
	public void updateMaxCoordinatesOfComponent(@Nonnull MaxCoordinatesInterface component) {

		updateMaxCoordinatesInternal(component);

		componentMaxPositions.put(component, new Point2D(component.getMaxX(), component.getMaxY()));
	}

	private void updateMaxCoordinatesInternal(MaxCoordinatesInterface component) {

		boolean checkMaxX = false;
		boolean checkMaxY = false;
		boolean checkGreaterX = false;
		boolean checkGreaterY = false;

		if (maxXComponent == null) {
			maxXComponent = component;
			setPrefWidth(component.getMaxX() + scrollOffset);
			maxX = maxXComponent.getMaxX();
		} else {
			checkMaxX = true;
		}

		if (maxYComponent == null) {
			maxYComponent = component;
			setPrefHeight(component.getMaxY() + scrollOffset);
			maxY = maxYComponent.getMaxY();
		} else {
			checkMaxY = true;
		}

		if (checkMaxX && maxXComponent.equals(component)) {
			checkMaxX();
		} else {
			checkGreaterX = true;
		}

		if (checkMaxY && maxYComponent.equals(component)) {
			checkMaxY();
		} else {
			checkGreaterY = true;
		}

		if (checkMaxX && checkGreaterX && component.getMaxX() > maxX) {
			setPrefWidth(component.getMaxX() + scrollOffset);
			maxXComponent = component;
			maxX = maxXComponent.getMaxX();
		}

		if (checkMaxY && checkGreaterY && component.getMaxY() > maxY) {
			setPrefHeight(component.getMaxY() + scrollOffset);
			maxYComponent = component;
			maxY = maxYComponent.getMaxY();
		}
	}

	private void checkMaxX() {

		if (!(maxXComponent.getMaxX() > maxX)) {
			for (Entry<MaxCoordinatesInterface, Point2D> entry : componentMaxPositions.entrySet()) {
				if (entry.getValue().getX() > maxXComponent.getMaxX()) {
					maxXComponent = entry.getKey();
				}
			}
		}

		setPrefWidth(maxXComponent.getMaxX() + scrollOffset);
		maxX = maxXComponent.getMaxX();
	}

	private void checkMaxY() {

		if (!(maxYComponent.getMaxY() > maxY)) {
			for (Entry<MaxCoordinatesInterface, Point2D> entry : componentMaxPositions.entrySet()) {
				if (entry.getValue().getY() > maxXComponent.getMaxY()) {
					maxYComponent = entry.getKey();
				}
			}
		}

		setPrefHeight(maxYComponent.getMaxY() + scrollOffset);
		maxY = maxYComponent.getMaxY();
	}

	/**
	 * Checks if we currently draw lines.
	 * 
	 * @return True if we draw lines, false if not.
	 */
	public boolean isDrawing() {

		return workCon != null;
	}
	
	public void setDeletionLine(ConnectionLine deletionLine) {
		this.deletionLine = deletionLine;
	}
	
	public void deleteLine() {
		if(deletionLine != null) {
			deletionLine.delete();
		}
	}
	
	public void removeDeletionLine() {
		deletionLine = null;
	}
	
	public void deletePlugin(SigproPlugin plugin) {
		plugins.remove(plugin);
		getChildren().remove(plugin.getGUI());
	}

}
