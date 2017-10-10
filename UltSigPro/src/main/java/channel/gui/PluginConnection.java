package channel.gui;

import java.util.HashMap;
import java.util.HashSet;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;

public class PluginConnection {

	private HashSet<ConnectionLine> lines = new HashSet<>();
	private HashSet<LineDevider> deviders = new HashSet<>();

	private ConnectionLine actLine = null;
	private boolean actHorizontal = true;;

	private PluginConnection snappedLine = null;

	private HashSet<PluginConnection> snapListeners = new HashSet<>();

	private PluginConfigGroup configGroup;

	public PluginConnection(PluginConfigGroup configGroup, ConnectionLineEndpointInterface endpoint, double startX,
			double startY) {
		this.configGroup = configGroup;
		actLine = new ConnectionLine(this, endpoint, startX, startY, true);

		configGroup.getChildren().add(actLine);
	}

	public void changeOrientation(double x, double y) {
		actHorizontal = !actHorizontal;
		actLine.setCoordinatesLine(actLine, x, y);
		if (actHorizontal) {
			ConnectionLine newLine = new ConnectionLine(this, actLine, actLine.getEndX(), y, actHorizontal);
			actLine.setCoordinatesLine(newLine, x, y);
			actLine = newLine;
		} else {
			ConnectionLine newLine = new ConnectionLine(this, actLine, x, actLine.getEndY(), actHorizontal);
			actLine.setCoordinatesLine(newLine, x, y);
			;
			actLine = newLine;
		}

		configGroup.getChildren().add(actLine);
	}

	public ConnectionLine getActLine() {
		return actLine;
	}

	public void endPluginConnection(ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {
		lines.add(actLine);
		actLine.setCoordinatesFinal(endpoint, xCoord, yCoord);
		actLine = null;
	}

	public void drawLine(double xCoord, double yCoord) {
		actLine.setCoordinates(xCoord, yCoord);
	}

	public void unifyConnections(PluginConnection other) {

	}

	public boolean checkForInput() {

		return false;
	}
	
	public void devideActLine(double x, double y) {
		
		if(actHorizontal) {
			double stepX = actLine.getStartX() + (x - actLine.getStartX()) / 2;
			double stepY = actLine.getStartY();
			
			actLine.setEndX(stepX);
			
			ConnectionLine newLine = new ConnectionLine(this, actLine, stepX, stepY, false);
			actLine.setCoordinatesLine(newLine, stepX, stepY);
			configGroup.getChildren().add(newLine);
			
			actLine = new ConnectionLine(this, newLine, stepX, y, true);
			actLine.setCoordinates(x, y);
			configGroup.getChildren().add(actLine);
			newLine.setCoordinatesLine(actLine, stepX, y);
		} else {
			ConnectionLine newLine = new ConnectionLine(this, actLine, actLine.getEndX(), y, true);
			actLine.setCoordinatesLine(newLine, x, y);
			
			newLine.setCoordinates(x, y);
			actLine = newLine;
			configGroup.getChildren().add(actLine);
		}		
	}

	class ConnectionLine extends Line {

		private static final double minLength = 10;

		private ConnectionLineEndpointInterface firstEnd;
		private ConnectionLineEndpointInterface secondEnd;

		private ConnectionLine firstLine;
		private ConnectionLine secondLine;

		private boolean horizontal;

		private PluginConnection parent;

		public ConnectionLine(PluginConnection parent, ConnectionLineEndpointInterface firstEnd, double x, double y,
				boolean horizontal) {
			this.parent = parent;
			this.firstEnd = firstEnd;
			this.horizontal = horizontal;
			setStartX(x);
			setStartY(y);
			setEndX(x);
			setEndY(y);
		}

		public ConnectionLine(PluginConnection parent, ConnectionLine line, double x, double y, boolean horizontal) {
			this.parent = parent;
			this.firstLine = line;
			this.horizontal = horizontal;
			setStartX(x);
			setStartY(y);
			setEndX(x);
			setEndY(y);
		}
		
		public boolean isHorizontal() {
			return horizontal;
		}

		/**
		 * Just for drawing a line.
		 * 
		 * @param x
		 *            the x coordinate
		 * @param y
		 *            the y coordinate
		 */
		public void setCoordinates(double x, double y) {
			if (horizontal) {
				if (x > getStartX() + 3) {
					setEndX(x - 3);
				} else if (x < getStartX() - 3) {
					setEndX(x + 3);
				} else {
					setEndX(getStartX());
				}
			} else {
				setEndX(getStartX());
			}

			if (!horizontal) {
				if (y > getStartY() + 3) {
					setEndY(y - 3);
				} else if (y < getStartY() - 3) {
					setEndY(y + 3);
				} else {
					setEndY(getEndY());
				}
			} else {
				setEndY(getStartY());
			}
		}

		public void updateCoordinates(ConnectionLine line, double x, double y) {
			if(line.equals(firstLine)) {
				if(horizontal) {
					setStartX(x);
				} else {
					setStartY(y);
				}
			} else if (line.equals(secondLine)) {
				if(horizontal) {
					setEndX(x);
				} else {
					setEndY(y);
				}
			}
		}
		
		/**
		 * Update a connections coordinates from an endpoint.
		 * 
		 * @param endpoint
		 *            the endpoint as end this line
		 * @param x
		 *            the x coordinate
		 * @param y
		 *            the y coordinate
		 */
		public void updateCoordinates(ConnectionLineEndpointInterface endpoint, double x, double y) {

			if (firstEnd != null && endpoint.equals(firstEnd)) {
				if(secondEnd != null) {
					if(!(y == getStartY())) {
						double length = getEndX() - getStartX();
						
						ConnectionLine newLine = new ConnectionLine(parent, secondEnd, getEndX(), getStartY(), true);
						ConnectionLine vertLine = new ConnectionLine(parent, newLine, getStartX() + length/2, getEndY(), false);
						newLine.setCoordinatesLine(vertLine, getStartX() + length/2, getEndY());
						vertLine.setCoordinatesLine(this, getStartX() + length/2, y);
						
						parent.configGroup.getChildren().addAll(newLine, vertLine);
						
						secondEnd.addLine(newLine);
						
						secondLine = vertLine;
						secondEnd = null;
						setEndX(getStartX() + length / 2);
					}
				} else {
				}
				
				if(secondLine != null) {
					secondLine.updateCoordinates(this, x, y);
				}
				
				if (horizontal) {
					setStartX(x);
					setStartY(y);
					setEndY(y);
				} else {
					setStartY(y);
					setStartX(getStartX());
				}
			} else if (secondEnd != null && endpoint.equals(secondEnd)) {
				if(firstEnd != null) {
					if(!(y == getStartY())) {
						double length = getEndX() - getStartX();
						
						ConnectionLine newLine = new ConnectionLine(parent, firstEnd, getStartX(), getStartY(), true);
						ConnectionLine vertLine = new ConnectionLine(parent, newLine, getStartX() + length/2, getEndY(), false);
						newLine.setCoordinatesLine(vertLine, getStartX() + length/2, getEndY());
						vertLine.setCoordinatesLine(this, getStartX() + length/2, y);
						
						parent.configGroup.getChildren().addAll(newLine, vertLine);
						
						firstEnd.addLine(newLine);
						
						firstLine = vertLine;
						firstEnd = null;
						setStartX(getStartX() + length / 2);
					}
				}
				
				if(firstLine != null) {
					firstLine.updateCoordinates(this, x, y);
				}
				
				if (horizontal) {
					setEndX(x);
					setEndY(y);
					setStartY(y);
				} else {
					setEndY(y);
					setEndX(getEndX());
				}
			}
		}

		/**
		 * End a connection at an endpoint
		 * 
		 * @param secondEnd
		 *            the endpoint to end the line at
		 * @param x
		 *            the x coordinate
		 * @param y
		 *            the y coordinate
		 */
		public void setCoordinatesFinal(ConnectionLineEndpointInterface secondEnd, double x, double y) {
			this.secondEnd = secondEnd;
			
			updateCoordinates(secondEnd, x, y);
		}
		
		public boolean checkCoordinates(double x, double y) {
			if(horizontal) {
				if(y == getEndY()) {
					return true;
				} else {
					if(firstEnd != null) {
						return false;
					} else if (firstLine != null) {
						return firstLine.checkCoordinates(x, y);
					}
					return true;
				}
			} else {
				return true;
			}
		}

		/**
		 * End a connection at another connection line
		 * 
		 * @param line
		 *            the line to end
		 * @param x
		 *            the x coordinate
		 * @param y
		 *            the y coordinate
		 */
		public void setCoordinatesLine(ConnectionLine line, double x, double y) {
			secondLine = line;
			if (horizontal) {
				setEndX(x);
				setEndY(getEndY());
			} else {
				setEndY(y);
				setEndX(getEndX());
			}
		}

		public void setParent(PluginConnection parent) {
			this.parent = parent;
		}

	}

	private class LineDevider implements ConnectionLineEndpointInterface {

		private HashMap<LineDeviderPosition, ConnectionLine> connectionLines = new HashMap<>();

		private PluginConnection parent;

		public LineDevider(PluginConnection parent, ConnectionLine north, ConnectionLine east, ConnectionLine south,
				ConnectionLine west) {
			this.parent = parent;
			addConnectionInternally(north, LineDeviderPosition.NORTH);
			addConnectionInternally(east, LineDeviderPosition.EAST);
			addConnectionInternally(south, LineDeviderPosition.SOUTH);
			addConnectionInternally(west, LineDeviderPosition.WEST);
		}

		public void setParent(PluginConnection parent) {
			this.parent = parent;
		}

		private void addConnectionInternally(ConnectionLine line, LineDeviderPosition position) {

		}

		private void removeConnection(ConnectionLine line) {

		}

		@Override
		public boolean setCoordinates(ConnectionLine line, double x, double y) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addLine(ConnectionLine line) {
			// TODO Auto-generated method stub

		}
	}

	private enum LineDeviderPosition {
		NORTH, EAST, SOUTH, WEST
	}

}
