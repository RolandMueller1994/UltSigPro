package channel.gui;

/**
 * Interface for getting the maximum coordinates of a gui component which is
 * used in the {@link PluginConfigGroup}.
 * 
 * @author roland
 *
 */
public interface MaxCoordinatesInterface {

	/**
	 * Getting the maximum x-coordinate. This is the x-coordinate of the lower
	 * right corner of the corresponding gui component.
	 * 
	 * @return the maximum x-coordinate.
	 */
	double getMaxX();

	/**
	 * Getting the maximum y-coordinate. This is the y-coordinate of the lower
	 * right corner of the corresponding gui component.
	 * 
	 * @return the maximum y-coordinate.
	 */
	double getMaxY();

	/**
	 * Register the parent window as listener to this object. The listener will
	 * be called if the coordinates of this component change.
	 * 
	 * @param coordinatesListener
	 *            {@link PluginConfigGroup} as listener.
	 */
	void registerMaxCoordinatesUpdateListener(PluginConfigGroup coordinatesListener);

}
