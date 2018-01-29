package channel.gui;

import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import channel.Channel;
import channel.ChannelConfig.ChannelDeviceUpdateListener;
import channel.InputInfoWrapper;
import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import channel.PluginInput;
import channel.PluginOutput;
import channel.gui.SignalFlowConfigException.SignalFlowErrorCode;
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
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import plugins.PluginManager;
import plugins.sigproplugins.SigproPlugin;
import plugins.sigproplugins.internal.GainBlock;
import plugins.sigproplugins.signalrouting.WaveChartProbe;
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
	private ContextMenu contextMenu;

	private static final double maxBounds = 10000;
	private static final double sizeOffset = 40;
	
	private static final double maxRaster = 20;
	private double raster = 10;

	private HashSet<SigproPlugin> plugins = new HashSet<>();
	private SigproPlugin output;
	private SigproPlugin input;
	private SigproPlugin waveChartProbe;

	private PluginConnection workCon = null;
	private HashSet<PluginConnection> allConnections = new HashSet<>();

	//private ConnectionLine deletionLine;

	private boolean lineHovered = false;

	private double newPluginX;
	private double newPluginY;
	
	private double startDragX;
	private double startDragY;

	private boolean dragged = false;
	private boolean coordinatesOnLine = false;
	
	private PluginConnection coordinatesCon;
	
	private double scrollValue = 20;
	
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
		setOnScroll(new EventHandler<ScrollEvent> () {

			@Override
			public void handle(ScrollEvent event) {
				
				if(event.getDeltaY() == 0 && event.getDeltaX() == 0) {
					return;
				}
				
				boolean horizontal = false;

				if(Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())) {
					horizontal = true;
				}
				
				double delta = horizontal ? event.getDeltaX() : event.getDeltaY();
				
				
				if(USPGui.isCtrlPressed()) {			
					double scaleFactor;
					
					if(delta > 0) {
						scaleFactor = 1.05;
					} else {
						scaleFactor = 1/1.05;
					}
					
					Pane parent = (Pane) getParent();
					
					double paneOffsetX = parent.getWidth()/2;
					double paneOffsetY = parent.getHeight()/2;
					
					double centerX = getLayoutX() + maxBounds/2;
					double centerY = getLayoutY() + maxBounds/2;
					
					double diffX = centerX - paneOffsetX;
					double diffY = centerY - paneOffsetY;
					
					double scaleX1 = getScaleX();
					double scaleY1 = getScaleY();
					
					double scaleX2 = scaleX1 * scaleFactor;
					double scaleY2 = scaleY1 * scaleFactor;
					
					scaleX2 = scaleX2 > 1.0 ? 1.0 : scaleX2;
					scaleY2 = scaleY2 > 1.0 ? 1.0 : scaleY2;
					
					setLayoutX(getLayoutX() + (diffX * (scaleX2 - scaleX1)));
					setLayoutY(getLayoutY() + (diffY * (scaleY2 - scaleY1)));
					
					setScaleX(scaleX2);
					setScaleY(scaleY2);
				} else if (USPGui.isShiftPressed() || horizontal) {
					double intScrollValue = delta > 0 ? scrollValue : -scrollValue;
					
					setLayoutX(getLayoutX() - intScrollValue);
				} else {
					double intScrollValue = delta > 0 ? scrollValue : -scrollValue;
					
					setLayoutY(getLayoutY() + intScrollValue);
				}
				
			}
			
		});
		
		channel.getChannelConfig().registerChannelDeviceUpdateListener(new ChannelDeviceUpdateListener() {
			
			@Override
			public void fireDevicesUpdates() {
				if(channel.getPluginInput() != null && input == null) {
					input = channel.getPluginInput();
					addPlugin(input, maxBounds/2 + 100, maxBounds/2 + 100);
				} else if(channel.getPluginInput() == null && input != null) {
					input.delete();
					input = null;
				}
				
				if(channel.getPluginOutput() != null && output == null) {
					output = channel.getPluginOutput();
					addPlugin(output, maxBounds/2 + USPGui.stage.getWidth() - 100, maxBounds/2 + 100);
				} else if(channel.getPluginOutput() == null && output != null) {
					output.delete();
					output = null;
				}
				
				fitToScreen();
			}
		});

		contextMenu = new ContextMenu();
		try {
			contextMenu.getItems().add(new AddPluginMenuItem(this));
		} catch (ResourceProviderException e1) {
			// shouldn't happen
			e1.printStackTrace();
		}

		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);
		setMinHeight(maxBounds);
		setMinWidth(maxBounds);
		setLayoutX(-maxBounds/2);
		setLayoutY(-maxBounds/2);

		setBorder(new Border(
				new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THICK)));

		if(channel.getPluginInput() != null) {
			addPlugin(channel.getPluginInput(), maxBounds/2 + 100, maxBounds/2 + 100);
			input = channel.getPluginInput();
		}
		
		if(channel.getPluginOutput() != null) {
			addPlugin(channel.getPluginOutput(), maxBounds/2 + USPGui.stage.getWidth() - 100, maxBounds/2 + 100);		
			output = channel.getPluginOutput();
		}
		
		waveChartProbe = new WaveChartProbe(channel.getChannelPane());
		addPlugin(waveChartProbe, maxBounds/2 + USPGui.stage.getWidth() - 100, maxBounds/2 + 200);
		
		addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				drawLine(event);
			}

		});
		
		addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				if(workCon == null && coordinatesCon != null) {
					coordinatesCon.clearPoints();
				}
			}
			
		});
		
		addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				startDragX = event.getScreenX();
				startDragY = event.getScreenY();
				
				coordinatesOnLine = false;
				coordinatesCon = null;
				
				if(workCon != null) {
					for(PluginConnection con : allConnections) {
						if(con.checkIfCoordinatesOnLine(workCon.getDrawingPoint())) {
							coordinatesOnLine = true;
							coordinatesCon = con;
							break;
						}
					}					
				} else {
					for(PluginConnection con : allConnections) {
						if(con.checkIfCoordinatesOnLine(event.getX(), event.getY())) {
							coordinatesOnLine = true;
							coordinatesCon = con;
							break;
						}
					}
				}
				
			}
			
		});
		
		addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				if(event.isSecondaryButtonDown()) {
					
					dragged = true;
					
					double endDragX = event.getScreenX();
					double endDragY = event.getScreenY();
					
					double diffX = endDragX - startDragX;
					double diffY = endDragY - startDragY;
					
					setLayoutX(getLayoutX() + diffX);
					setLayoutY(getLayoutY() + diffY);
					
					startDragX = endDragX;
					startDragY = endDragY;
				} else if(event.isPrimaryButtonDown()) {
					if(coordinatesCon != null) {
						coordinatesCon.dragLine(event.getX(), event.getY());						
					}
				}
				
			}
		});

		addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				if(dragged) {
					dragged = false;
					return;
				}
				
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
						boolean coordinatesOnLine = false;
						
						if(workCon != null) {
							for(PluginConnection con : allConnections) {
								if(con.checkIfCoordinatesOnLine(workCon.getDrawingPoint())) {
									coordinatesOnLine = true;
									coordinatesCon = con;
									break;
								}
							}					
						}

						Point2D point = screenToLocal(event.getScreenX(), event.getScreenY());

						newPluginX = point.getX();
						newPluginY = point.getY();
					} else if (workCon == null && contextMenu.isShowing()) {
						contextMenu.hide();
					} else if (event.getButton().equals(MouseButton.PRIMARY) && workCon != null) {
						if(!coordinatesOnLine) {
							workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
									sceneToLocal(event.getSceneX(), event.getSceneY()).getY());							
						}
					}
				}
				
				if(hovered || !coordinatesOnLine) {
					for(PluginConnection con : allConnections) {
						con.removeCurrentSelection();
					}
				}
				
				if(coordinatesOnLine && workCon != null) {
					if(!coordinatesCon.unifyConnections(workCon)) {
						workCon.changeOrientation(sceneToLocal(event.getSceneX(), event.getSceneY()).getX(),
								sceneToLocal(event.getSceneX(), event.getSceneY()).getY());
					}
				}
			}
		});

	}
	
	public Channel getChannel() {
		return channel;
	}

	private void showContextMenu(double screenX, double screenY) {

		contextMenu.show(this, screenX, screenY);
	}
	
	public void fitToScreen() {
		
		double minX = 0;
		double maxX = 0;
		double minY = 0;
		double maxY = 0;
		boolean init = false;
		
		if(input != null) {
			minX = input.getGUI().getLayoutX();
			minY = input.getGUI().getLayoutY();
			maxX = input.getGUI().getLayoutX() + input.getGUI().getWidth();
			maxY = input.getGUI().getLayoutY() + input.getGUI().getHeight();	
			init = true;
		}
		
		if(init && output != null) {
			if(output.getGUI().getLayoutX() < minX) {
				minX = output.getGUI().getLayoutX();
			}
			if(output.getGUI().getLayoutY() < minY) {
				minY = output.getGUI().getLayoutY();
			}
			if(output.getGUI().getLayoutX() + output.getGUI().getWidth() > maxX) {
				maxX = output.getGUI().getLayoutX() + output.getGUI().getWidth();
			}
			if(output.getGUI().getLayoutY() + output.getGUI().getHeight() > maxY) {
				maxY = output.getGUI().getLayoutY() + output.getGUI().getHeight();
			}			
		} else if(output != null) {
			minX = output.getGUI().getLayoutX();
			minY = output.getGUI().getLayoutY();
			maxX = output.getGUI().getLayoutX() + output.getGUI().getWidth();
			maxY = output.getGUI().getLayoutY() + output.getGUI().getHeight();
			init = true;
		}
		
		for(PluginConnection con : allConnections) {
			if(init) {
				if(con.getMinX() < minX) {
					minX = con.getMinX();
				}
				if(con.getMaxX() > maxX) {
					maxX = con.getMaxX();
				}
				if(con.getMinY() < minY) {
					minY = con.getMinY();
				}
				if(con.getMaxY() > maxY) {
					maxY = con.getMaxY();
				}				
			} else {
				minX = con.getMinX();
				maxX = con.getMaxX();
				minY = con.getMinY();
				maxY = con.getMaxY();
				init = true;
			}
		}
		
		for(SigproPlugin plugin : plugins) {
			if(init) {
				if(plugin.getGUI().getLayoutX() < minX) {
					minX = plugin.getGUI().getLayoutX();
				}
				if(plugin.getGUI().getLayoutY() < minY) {
					minY = plugin.getGUI().getLayoutY();
				}
				if(plugin.getGUI().getLayoutX() + plugin.getGUI().getWidth() > maxX) {
					maxX = plugin.getGUI().getLayoutX() + plugin.getGUI().getWidth();
				}
				if(plugin.getGUI().getLayoutY() + plugin.getGUI().getHeight() > maxY) {
					maxY = plugin.getGUI().getLayoutY() + plugin.getGUI().getHeight();
				}				
			} else {
				minX = plugin.getGUI().getLayoutX();
				minY = plugin.getGUI().getLayoutY();
				maxX = plugin.getGUI().getLayoutX() + plugin.getGUI().getWidth();
				maxY = plugin.getGUI().getLayoutY() + plugin.getGUI().getHeight();
				init = true;
			}
		}
		
		if(init) {
			minX -= sizeOffset;
			maxX += sizeOffset;
			minY -= sizeOffset;
			maxY += sizeOffset;
			
			if(getParent() != null) {
				double parentWidth = ((Pane) getParent()).getWidth();
				double parentHeight = ((Pane) getParent()).getHeight();
				
				double width = maxX - minX;
				double height = maxY - minY;
				
				double scaleX = parentWidth/width;
				double scaleY = parentHeight/height;
				
				double scale = scaleX < scaleY ? scaleX : scaleY;
				scale = scale > 1.0 ? 1.0 : scale;
				setScaleX(scale);
				setScaleY(scale);
				
				double paneOffsetX = parentWidth/2;
				double paneOffsetY = parentHeight/2;
				
				double centerX = (minX + width/2) - maxBounds/2;
				double centerY = (minY + height/2) - maxBounds/2;
				
				double diffX = centerX * scale - paneOffsetX;
				double diffY = centerY * scale - paneOffsetY;
				
				setLayoutX(-maxBounds/2 - diffX);
				setLayoutY(-maxBounds/2 - diffY);			
			}
		}
	}
	
	public void deletePluginConnection(PluginConnection con) {
		allConnections.remove(con);
	}
	
	public void removeCurrentSelection() {
		for(PluginConnection con : allConnections) {
			con.removeCurrentSelection();
		}
	}

	public void initializePlay() throws SignalFlowConfigException {

		HashMap<OutputInfoWrapper, LinkedList<InputInfoWrapper>> dataFlowMap = new HashMap<>();
		boolean isOutput = false;
		
		System.out.println("Size: " + allConnections.size());
		
		for (PluginConnection connection : allConnections) {

			OutputInfoWrapper outputWrapper = null;

			for (Output output : connection.getOutputs()) {
				outputWrapper = new OutputInfoWrapper(output.getPlugin(), output.getName());
				break;
			}
			
			if (outputWrapper != null) {

				LinkedList<InputInfoWrapper> inputWrappers = new LinkedList<>();

				for (Input input : connection.getInputs()) {
					inputWrappers.add(new InputInfoWrapper(input.getPlugin(), input.getName()));
					
					System.out.println(input.getPlugin().getName());
					
					if(input.getPlugin().equals(output)) {
						isOutput = true;
					}
				}

				if (inputWrappers.size() > 0) {
					dataFlowMap.put(outputWrapper, inputWrappers);
				}
			}

		}
		
		boolean isConnection = false;
		
/*		for(SigproPlugin plugin : plugins) {
			if(!(plugin.equals(input)) && !(plugin.equals(output))) {
				for(Input input : plugin.getInputs()) {
					if(input.getLine() == null) {
						isConnection = true;
					}
				}
				
				for(Output output : plugin.getOutputs()) {
					if(output.getLine() == null) {
						isConnection = true;
					}
				}				
			}
		}
*/		
		LinkedList<String> messages = new LinkedList<>();
		LinkedList<SignalFlowErrorCode> errorCodes = new LinkedList<>();
		
		if(input == null) {
			messages.add("There is no input in channel.");
			errorCodes.add(SignalFlowErrorCode.INPUT_ERROR);
		}
		
		if(!isOutput) {
			messages.add("There is no connection to the output.");
			errorCodes.add(SignalFlowErrorCode.OUTPUT_ERROR);
		}
		
		if(isConnection) {
			messages.add("There is a unconnected input or output of a plugin.");
			errorCodes.add(SignalFlowErrorCode.CONNECTION_ERROR);
		}
		
		if(messages.size() != 0) {
			String message = "";
			
			for(String messagePart : messages) {
				message += messagePart + " ";
			}
			
			throw new SignalFlowConfigException(message, errorCodes);
		}
		
		channel.setDataFlowMap(dataFlowMap);
	}

	/**
	 * Sets if a {@link ConnectionLine} or {@link LineDivider} is hovered or
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
		if (workCon != null) {
			workCon.escapeLineDrawing();
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
	
	public void removeConnection(PluginConnection con) {
		allConnections.remove(con);
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
			// TODO endpoint.addLine(workCon.getActLine());
		} else {
			if (!workCon.isDrawingHorizontal()) {
				workCon.changeOrientation(xCoord, yCoord);
			}

			//TODO
			/*if (!workCon.getActLine().checkCoordinates(xCoord, yCoord)) {
				workCon.devideActLine(xCoord, yCoord);
			}*/
			endpoint.addConnection(workCon);
			workCon.endPluginConnection(endpoint, xCoord, yCoord);
			allConnections.add(workCon);
			workCon = null;
		}

	}

	public SigproPlugin createPluginFromProjectFile(String pluginName) throws InstantiationException, IllegalAccessException {
		SigproPlugin current = PluginManager.getInstance().getSigproPlugin(pluginName);
		addPlugin(current, 0, 0);
		return current;
	}
	
	public void createConnectionFromProjectFile(Element conElementParent) {
		
		PluginConnection conLine = new PluginConnection(this);
		
		NodeList conChilds = conElementParent.getChildNodes();
		
		conLine.setConnectionLinesConfig(conChilds);
		allConnections.add(conLine);
		
	}
	
	public double getRaster() {
		return raster;
	}
	
	private void addPlugin(SigproPlugin plugin, double xCoord, double yCoord) {

		xCoord = ((int) xCoord / (int) raster) * raster; 
		yCoord = ((int) yCoord / (int) raster) * raster;
		
		plugins.add(plugin);

		HashSet<String> inputs = plugin.getInputConfig();
		HashSet<String> outputs = plugin.getOutputConfig();
		
		int numberOfInputs = inputs.size();
		int numberOfOutputs = outputs.size();

		plugin.registerMaxCoordinatesUpdateListener(this);

		int width = plugin.getWidth();
		int height = plugin.getHeight();

		double internalX = xCoord - width / 2;
		double internalY = yCoord - height / 2;

		Pane gui = plugin.getGUI();

		getChildren().add(gui);
		gui.setLayoutX(internalX);
		gui.setLayoutY(internalY);

		int i = 0;

		if (numberOfInputs > 0) {
			double inputOffset = maxRaster * 2;
			for (String input : inputs) {
				Input inputGUI = new Input(plugin, this, input, internalX, internalY, i, numberOfInputs, width, height, inputOffset);
				getChildren().add(inputGUI);
				plugin.addInput(inputGUI);
				i++;
			}
		}

		if (numberOfOutputs > 0) {
			double outputOffset = maxRaster * 2;
			i = 0;
			for (String output : outputs) {
				Output outputGUI = new Output(plugin, this, output, internalX, internalY, i, numberOfOutputs, width, height,
						outputOffset);
				getChildren().add(outputGUI);
				plugin.addOutput(outputGUI);
				i++;
			}
		}
	}

	/**
	 * Checks if we currently draw lines.
	 * 
	 * @return True if we draw lines, false if not.
	 */
	public boolean isDrawing() {

		return workCon != null;
	}

	// TODO
	/*
	public void setDeletionLine(ConnectionLine deletionLine) {
		this.deletionLine = deletionLine;
	}*/

	public void deleteLine() {
		
		for(PluginConnection con : allConnections) {
			con.deleteSelection();
		}
		
	}
	
	public void clearPoints() {
		for(PluginConnection con : allConnections) {
			con.clearPoints();
		}
	}

	public void removeDeletionLine() {
		//deletionLine = null;
	}

	public void deletePlugin(SigproPlugin plugin) {
		plugins.remove(plugin);
		getChildren().remove(plugin.getGUI());
	}
	
	public SigproPlugin getWaveChartProbe() {
		return waveChartProbe;
	}

	private class AddPluginMenuItem extends MenuItem {

		private static final String TITLE = "title";

		private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();

		private AddPluginMenuItem(PluginConfigGroup parent) throws ResourceProviderException {
			super.setText(lanHandler.getLocalizedText(AddPluginMenuItem.class, TITLE));

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

			private ListView<String> listView = new ListView<>();

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
	
	public HashSet<SigproPlugin> getPlugins() {
		return plugins;
	}
	
	public void collectPluginInfos(Document doc, Element element) {
		int count = 0;
		for (SigproPlugin plugin : plugins) {
			plugin.setNumber(count);
			Element pluginElement = doc.createElement("plugin");
			Element pluginName = doc.createElement("name");
			pluginName.appendChild(doc.createTextNode(plugin.getName()));
			pluginElement.appendChild(pluginName);
			plugin.collectedPluginInfo(doc, pluginElement);
			element.appendChild(pluginElement);
			count++;
		}
		
		for (PluginConnection con : allConnections) {
			Element connection = doc.createElement("connection");
			
			con.collectConnectionLineInfos(doc, connection);
			element.appendChild(connection);
		}
	}

}
