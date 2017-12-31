package channel.gui;

/**
 * Marker interface for endpoints of plugin connection lines. Just to store them
 * in one collection.
 * 
 * @author roland
 *
 */
public interface ConnectionLineEndpointInterface {

	boolean setCoordinates(PluginConnection con, double x, double y);
	
	void addConnection(PluginConnection con);
	
	void removeConnection(PluginConnection con);
	
	void replaceConnection(PluginConnection origin, PluginConnection replace);
	
}
