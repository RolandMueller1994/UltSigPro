package plugins.sigproplugins;

import java.util.HashSet;
import java.util.LinkedList;
import javax.annotation.Nonnull;

import channel.OutputDataWrapper;
import channel.gui.Input;
import channel.gui.MaxCoordinatesInterface;
import channel.gui.Output;
import channel.gui.PluginConfigGroup;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import pluginframework.PluginInterface;

/**
 * Basic interface for signal processing plugins. Every plugin has to implement
 * this interface.
 * 
 * @author roland
 *
 */
public abstract class SigproPlugin implements PluginInterface, MaxCoordinatesInterface {

	protected Pane gui;
	
	private ContextMenu contextMenu;
	
	private boolean dragged = false;
	private boolean hovered = false;
	
	private HashSet<Input> inputs = new HashSet<> ();
	private HashSet<Output> outputs = new HashSet<> ();

	private double absolutPositionX;
	private double absolutPositionY; 
	private double localPositionX;
	private double localPositionY;
	private double offsetX;
	private double offsetY;
	private double mouseOffsetX;
	private double mouseOffsetY;
	
	private PluginConfigGroup coordinatesListener;
	
	protected Pane getInternalGUI() {
		if(gui == null) {
			gui = new Pane();
			
			contextMenu = new ContextMenu();
			MenuItem deleteItem = new MenuItem("Löschen");
			deleteItem.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					
					delete();
				}
				
			});
			
			contextMenu.getItems().add(deleteItem);
			
			gui.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					
					if(contextMenu.isShowing()) {
						contextMenu.hide();
					} else if(MouseButton.SECONDARY.equals(event.getButton())) {
						contextMenu.show(gui, event.getScreenX(), event.getScreenY());
					}
				}
				
			});
			
			// Get the mouse position within the plugin to move the plugin at clicked postion.
			gui.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					
					mouseOffsetX = event.getSceneX() - gui.localToScene(gui.getLayoutBounds()).getMinX();
					mouseOffsetY = event.getSceneY() - gui.localToScene(gui.getLayoutBounds()).getMinY();
				
				}
			});
			
			gui.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					
					dragged = true;
					
					drag(event.getScreenX(), event.getScreenY());
				}
				
			});
			
			gui.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					
					dragged = false;
				}
				
			});
			
			gui.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent> () {

				@Override
				public void handle(MouseEvent event) {
					
					hovered = true;
				}
				
			});

			gui.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent> () {

				@Override
				public void handle(MouseEvent event) {
					
					hovered = false;
				}
				
			});
		}
		return gui;
	}
	
	public void delete() {
		for(Input input : inputs) {
			input.delete();
		}
		for(Output output : outputs) {
			output.delete();
		}
		coordinatesListener.deletePlugin(this);
	}
	
	public void drag(double screenX, double screenY) {
		localPositionX = gui.getLayoutX();
		localPositionY = gui.getLayoutY();
		
		// Absolute position in scene
		absolutPositionX = gui.localToScreen(gui.getLayoutBounds()).getMinX();
		absolutPositionY = gui.localToScreen(gui.getLayoutBounds()).getMinY();
		
		// Difference between absolute position and local position
		offsetX = absolutPositionX - localPositionX;
		offsetY = absolutPositionY - localPositionY;

		// Calculate the local position
		double xPosition = screenX - mouseOffsetX - offsetX;
		double yPosition = screenY - mouseOffsetY - offsetY;
		
		if(xPosition < 0) {
			xPosition = 0;
		}
		if(yPosition < 0) {
			yPosition = 0;
		}
		
		gui.setLayoutX(xPosition);
		gui.setLayoutY(yPosition);
		
		fireUpdateMaxCoordinates();
		
		// Update the position of ports
		for(Input input : inputs) {
			input.updatePosition(xPosition, yPosition);
		}
		
		for(Output output : outputs) {
			output.updatePosition(xPosition, yPosition);
		}
	}
	
	private void fireUpdateMaxCoordinates() {
		coordinatesListener.updateMaxCoordinatesOfComponent(this);
	}
	
	public boolean isDragged() {
		return dragged;
	}
	
	public void addInput(Input input) {
		inputs.add(input);
	}
	
	public HashSet<Input> getInputs() {
		return inputs;
	}
	
	public void addOutput(Output output) {
		outputs.add(output);
	}
	
	public HashSet<Output> getOutputs() {
		return outputs;
	}
	
	public boolean checkHovered() {
		
		for(Input input : inputs) {
			if(input.isHovered()) {
				return true;
			}
		}
		
		for(Output output : outputs) {
			if(output.isHovered()) {
				return true;
			}
		}
		
		return hovered;
	}
	
	@Override
	public void registerMaxCoordinatesUpdateListener(PluginConfigGroup coordinatesListener) {
	
		this.coordinatesListener = coordinatesListener;
	}
	
	/**
	 * Returns the name of the plugin. The name will be displayed in the header
	 * of the external gui.
	 * 
	 * @return the name of the plugin as {@link String}. Won't be null.
	 */
	@Nonnull
	public abstract String getName();

	/**
	 * Sets the name of the plugin.
	 * 
	 * @param name
	 *            The name
	 */
	public abstract void setName(@Nonnull String name);

	/**
	 * Returns the version string of the plugin.
	 * 
	 * @return the version as {@link String}. Won't be null.
	 */
	@Nonnull
	public abstract String getVersion();

	/**
	 * Every plugin has to create its one user interface. This method will be
	 * called by the plugin framework to display the gui.
	 * 
	 * @return a {@link Pane} which contains the user interface of the plugin.
	 *         Won't be null.
	 */
	@Nonnull
	public abstract Pane getGUI();

	/**
	 * Will be called by the underlying signal processing system. The
	 * implementation of this method must execute the signal processing and
	 * write the data to the outputs.
	 * 
	 * @param input
	 * 
	 * @param data
	 *            The new data package.
	 */
	public abstract LinkedList<OutputDataWrapper> putData(String input, double[] data);

	/**
	 * Provides the config for the outputs. Each output is marked by a
	 * {@link String}.
	 * 
	 * @return a {@link HashSet} of {@link String}s which contains the markers
	 *         for the outputs. Won't be null.
	 */
	@Nonnull
	public abstract HashSet<String> getOutputConfig();

	/**
	 * Provides the config for the inputs. Each input is market by a
	 * {@link String}.
	 * 
	 * @return a {@link HashSet} of {@link String}s which contains the markers
	 *         for the outputs. Won't be null.
	 */
	@Nonnull
	public abstract HashSet<String> getInputConfig();

	/**
	 * Triggers the calculation of data in the plugin.
	 * 
	 * @param play
	 *            a boolean which starts the signal processing if true, or stops
	 *            it an discards the available data if false.
	 */
	public abstract void setPlay(boolean play);

	/**
	 * The default width of this plugins GUI.
	 * 
	 * @return the width
	 */
	public abstract int getWidth();

	/**
	 * The default height of this plugins GUI.
	 * 
	 * @return the height
	 */
	public abstract int getHeight();

}
