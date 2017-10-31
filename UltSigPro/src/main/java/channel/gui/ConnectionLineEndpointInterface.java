package channel.gui;

import channel.gui.PluginConnection.ConnectionLine;

/**
 * Marker interface for endpoints of plugin connection lines. Just to store them
 * in one collection.
 * 
 * @author roland
 *
 */
public interface ConnectionLineEndpointInterface {

	boolean setCoordinates(ConnectionLine line, double x, double y);
	
	void addLine(ConnectionLine line);
	
	void removeLine(ConnectionLine line);
	
	void replaceLine(ConnectionLine origin, ConnectionLine replace);
	
}
