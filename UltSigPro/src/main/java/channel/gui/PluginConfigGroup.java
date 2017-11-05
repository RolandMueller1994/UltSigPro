package channel.gui;

import java.awt.MouseInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import channel.Channel;
import channel.InputInfoWrapper;
import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import channel.PluginInput;
import channel.PluginOutput;
import channel.gui.PluginConnection.ConnectionLine;
import channel.gui.PluginConnection.LineDevider;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import plugins.PluginManager;
import plugins.sigproplugins.SigproPlugin;
import resourceframework.ResourceProviderException;

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

	private ContextMenu contextMenu;

	private static final double scrollOffset = 50;
	private static final long scrollSpeed = 10;

	private HashSet<SigproPlugin> plugins = new HashSet<>();
	private SigproPlugin output;
	private SigproPlugin input;

	private PluginConnection workCon = null;
	private HashSet<PluginConnection> allConnections = new HashSet<>();

	private ConnectionLine deletionLine;

	private boolean lineHovered = false;

	private MaxCoordinatesInterface maxXComponent;
	private MaxCoordinatesInterface maxYComponent;
	private HashMap<MaxCoordinatesInterface, Point2D> componentMaxPositions = new HashMap<>();
	private double maxX;
	private double maxY;

	private double newPluginX;
	private double newPluginY;

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

		contextMenu = new ContextMenu();
		try {
			contextMenu.getItems().add(new AddPluginMenuItem(this));
		} catch (ResourceProviderException e1) {
			// shouldn't happen
			e1.printStackTrace();
		}

		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);

		setBorder(new Border(
				new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

		if(channel.getPluginInput() != null) {
			addPlugin(channel.getPluginInput(), 100, 100);
			input = channel.getPluginInput();
		}
		
		if(channel.getPluginOutput() != null) {
			addPlugin(channel.getPluginOutput(), USPGui.stage.getWidth() - 100, 100);		
			output = channel.getPluginOutput();
		}

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
					if (event.getButton().equals(MouseButton.SECONDARY) && workCon == null
							&& !contextMenu.isShowing()) {
						showContextMenu(event.getScreenX(), event.getScreenY());

						Point2D point = screenToLocal(event.getScreenX(), event.getScreenY());

						newPluginX = point.getX();
						newPluginY = point.getY();
					} else if (workCon == null && contextMenu.isShowing()) {
						contextMenu.hide();
					} else if (event.getButton().equals(MouseButton.PRIMARY) && workCon != null) {
						workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
								sceneToLocal(event.getSceneX(), event.getSceneY()).getY());
					}
				}
			}
		});

	}

	private void showContextMenu(double screenX, double screenY) {

		contextMenu.show(this, screenX, screenY);
	}

	public void initializePlay() throws SignalFlowConfigException {

		HashMap<OutputInfoWrapper, LinkedList<InputInfoWrapper>> dataFlowMap = new HashMap<>();
		boolean isOutput = false;
		boolean isInput = false;
		
		for (PluginConnection connection : allConnections) {

			OutputInfoWrapper outputWrapper = null;

			for (Output output : connection.getOutputs()) {
				outputWrapper = new OutputInfoWrapper(output.getPlugin(), output.getName());
				if(output.getPlugin().equals(input)) {
					isInput = true;
				}
				break;
			}
			
			if (outputWrapper != null) {

				LinkedList<InputInfoWrapper> inputWrappers = new LinkedList<>();

				for (Input input : connection.getInputs()) {
					inputWrappers.add(new InputInfoWrapper(input.getPlugin(), input.getName()));
					if(input.getPlugin().equals(output)) {
						isOutput = true;
					}
				}

				if (inputWrappers.size() > 0) {
					dataFlowMap.put(outputWrapper, inputWrappers);
				}
			}

		}

		if(!isInput) {
			throw new SignalFlowConfigException("There is no input!", true);
		}

		if(!isOutput && output != null) {
			throw new SignalFlowConfigException("There is no connection to the output!", false);
		}
		
		
		channel.setDataFlowMap(dataFlowMap);
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
				Input inputGUI = new Input(plugin, this, input, internalX, internalY, i, width, height, inputOffset);
				getChildren().add(inputGUI);
				plugin.addInput(inputGUI);
				i++;
			}
		}

		if (numberOfOutputs > 0) {
			double outputOffset = height / numberOfOutputs;
			i = 0;
			for (String output : outputs) {
				Output outputGUI = new Output(plugin, this, output, internalX, internalY, i, width, height,
						outputOffset);
				getChildren().add(outputGUI);
				plugin.addOutput(outputGUI);
				i++;
			}
		}

		updateMaxCoordinatesOfComponent(plugin);
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
				if (entry.getValue().getY() > maxYComponent.getMaxY()) {
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
		if (deletionLine != null) {
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

	private class AddPluginMenuItem extends MenuItem {

		private static final String TITLE = "title";

		private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();

		private PluginConfigGroup parent;

		private AddPluginMenuItem(PluginConfigGroup parent) throws ResourceProviderException {
			super.setText(lanHandler.getLocalizedText(AddPluginMenuItem.class, TITLE));

			this.parent = parent;

			super.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {

					AddPluginDialog dialog = new AddPluginDialog();

					Optional<ButtonType> result = dialog.showAndWait();

					if (result.isPresent() && result.get() == ButtonType.OK) {

						String selected = dialog.listView.getSelectionModel().getSelectedItem();

						if (selected != null) {
							try {
								parent.addPlugin(PluginManager.getInstance().getSigproPlugin(selected),
										parent.newPluginX, parent.newPluginY);
							} catch (InstantiationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}

			});
		}

		private class AddPluginDialog extends Dialog<ButtonType> {

			private static final String TITLE = "title";

			private ListView<String> listView = new ListView();

			private AddPluginDialog() {

				try {
					setTitle(LanguageResourceHandler.getInstance().getLocalizedText(AddPluginDialog.class, TITLE));
				} catch (ResourceProviderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				List<String> availPlugins = PluginManager.getInstance().getAllAvailableSigproPlugins();

				for (String plugin : availPlugins) {
					listView.getItems().add(plugin);
				}

				getDialogPane().setContent(listView);

				getDialogPane().getButtonTypes().add(ButtonType.OK);
				getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
			}

		}

	}

}
