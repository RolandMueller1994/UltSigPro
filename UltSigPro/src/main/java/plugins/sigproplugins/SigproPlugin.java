package plugins.sigproplugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

import channel.OutputDataWrapper;
import channel.gui.Input;
import channel.gui.Output;
import javafx.event.EventHandler;
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
public abstract class SigproPlugin implements PluginInterface {

	protected Pane gui;
	
	private LinkedList<Input> inputs = new LinkedList<> ();
	private LinkedList<Output> outputs = new LinkedList<> ();

	private double absolutPositionX;
	private double absolutPositionY; 
	private double localPositionX;
	private double localPositionY;
	private double offsetX;
	private double offsetY;
	private double mouseOffsetX;
	private double mouseOffsetY;
	
	protected Pane getInternalGUI() {
		if(gui == null) {
			gui = new Pane();
			
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
					
					localPositionX = gui.getLayoutX();
					localPositionY = gui.getLayoutY();
					
					// Absolute position in scene
					absolutPositionX = gui.localToScene(gui.getLayoutBounds()).getMinX();
					absolutPositionY = gui.localToScene(gui.getLayoutBounds()).getMinY();
					
					// Difference between absolute position and local position
					offsetX = absolutPositionX - localPositionX;
					offsetY = absolutPositionY - localPositionY;
	
					// Calculate the local position
					double xPosition = event.getSceneX() - mouseOffsetX - offsetX;
					double yPosition = event.getSceneY() - mouseOffsetY - offsetY;
					
					if(xPosition < 0) {
						xPosition = 0;
					}
					if(yPosition < 0) {
						yPosition = 0;
					}
					
					gui.setLayoutX(xPosition);
					gui.setLayoutY(yPosition);
					
					// Update the position of ports
					for(Input input : inputs) {
						input.updatePosition(xPosition, yPosition);
					}
					
					for(Output output : outputs) {
						output.updatePosition(xPosition, yPosition);
					}
					
				}
				
			});
		}
		return gui;
	}
	
	public void addInput(Input input) {
		inputs.add(input);
	}
	
	public void addOutput(Output output) {
		outputs.add(output);
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
		
		return false;
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
