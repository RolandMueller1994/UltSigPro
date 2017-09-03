package plugins.sigproplugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

import channel.DataDestinationInterface;
import javafx.scene.layout.Pane;
import pluginframework.PluginInterface;

/**
 * Basic interface for signal processing plugins. Every plugin has to implement
 * this interface.
 * 
 * @author roland
 *
 */
public interface SigproPlugin extends PluginInterface {

	/**
	 * Returns the name of the plugin. The name will be displayed in the header
	 * of the external gui.
	 * 
	 * @return the name of the plugin as {@link String}. Won't be null.
	 */
	@Nonnull
	String getName();

	/**
	 * Returns the version string of the plugin.
	 * 
	 * @return the version as {@link String}. Won't be null.
	 */
	@Nonnull
	String getVersion();

	/**
	 * Every plugin has to create its one user interface. This method will be
	 * called by the plugin framework to display the gui.
	 * 
	 * @return a {@link Pane} which contains the user interface of the plugin.
	 *         Won't be null.
	 */
	@Nonnull
	Pane getGUI();

	/**
	 * Will be called by the underlying signal processing system. The
	 * implementation of this method must execute the signal processing and
	 * write the data to the outputs.
	 * @param input 
	 * 
	 * @param data
	 *            The new data package.
	 */
	public void putData(String input, int[] data);

	/**
	 * Sets the configuration of the outputs. Each output will get a
	 * {@link LinkedBlockingQueue} which will be used as
	 * 
	 * @param outputConfig
	 *            a {@link HashMap} of {@link String}s and
	 *            {@link DataDestinationInterface}. Must not be null.
	 */
	void setOutputConfig(@Nonnull HashMap<String, DataDestinationInterface> outputConfig);

	/**
	 * Adds a {@link DataDestinationInterface} as output.
	 * 
	 * @param output
	 *            the name of the output of the plugin.
	 * @param dest
	 *            the destination for data.
	 */
	void addOutputConfig(@Nonnull String output, @Nonnull DataDestinationInterface dest);

	/**
	 * Provides the config for the outputs. Each output is marked by a
	 * {@link String}.
	 * 
	 * @return a {@link HashSet} of {@link String}s which contains the markers
	 *         for the outputs. Won't be null.
	 */
	@Nonnull
	HashSet<String> getOutputConfig();

	/**
	 * Triggers the calculation of data in the plugin.
	 * 
	 * @param play
	 *            a boolean which starts the signal processing if true, or stops
	 *            it an discards the available data if false.
	 */
	void setPlay(boolean play);

}
