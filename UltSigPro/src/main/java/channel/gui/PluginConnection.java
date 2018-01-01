package channel.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.javafx.geom.Line2D;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * This class manages the drawing of lines between plugins. Each connection is
 * connected to exactly one {@link Output} and multiple {@link Input}s. The
 * signal flow configuration for this connection will be generated from the
 * connection itself.
 * 
 * @author roland
 *
 */
public class PluginConnection {
	
	private static final double LINE_OFFSET = 6;
	private static final double DIVIDER_DIAMETER = 10;

	private PluginConfigGroup configGroup;
	
	private HashSet<LinkedList<USPPoint>> points = new HashSet<>();
	private LinkedList<USPPoint> drawingPoints = new LinkedList<>();
	private USPPoint drawingPoint;
	private HashSet<USPPoint> dividerPoints = new HashSet<>();
	
	private HashSet<Line> lines = new HashSet<>();
	private HashSet<Circle> dividers = new HashSet<>();

	private boolean drawingHorizontal;
	
	/**
	 * Creates a new connection which starts at a
	 * {@link ConnectionLineEndpointInterface}.
	 * 
	 * @param configGroup
	 *            The parent {@link Pane} to which lines will be added. Must not
	 *            be null.
	 * @param endpoint
	 *            The {@link ConnectionLineEndpointInterface} where the
	 *            connection will start. Must not be null.
	 * @param startX
	 *            The x-coordinate at which the connection should start.
	 * @param startY
	 *            The y-coordinate at which teh connection should start.
	 */
	public PluginConnection(@Nonnull PluginConfigGroup configGroup, @Nonnull ConnectionLineEndpointInterface endpoint,
			double startX, double startY) {
		this.configGroup = configGroup;
		
		drawingPoints = new LinkedList<>();
		drawingPoint = new USPPoint(startX, startY);
		drawingPoints.add(new USPPoint(startX, startY));
		drawingHorizontal = true;
		
		redraw();
	}

	public PluginConnection(@Nonnull PluginConfigGroup configGroup) {
		this.configGroup = configGroup;
	}

	private void redraw() {
		configGroup.getChildren().removeAll(lines);
		configGroup.getChildren().removeAll(dividers);
		
		lines.clear();
		dividers.clear();
		
		for(LinkedList<USPPoint> pointList : points) {
			USPPoint last = null;
			for(USPPoint point : pointList) {
				if(last != null) {
					Line line = new Line(last.getX(), last.getY(), point.getX(), point.getY());
					
					last.addUpdateLine(line, true);
					point.addUpdateLine(line, false);
					
					lines.add(line);					
				}
				last = point;
			}
		}
		
		if(drawingPoints != null) {
			USPPoint last = null;
			for(USPPoint point : drawingPoints) {
				if(last != null) {
					Line line = new Line(last.getX(), last.getY(), point.getX(), point.getY());
					
					last.addUpdateLine(line, true);
					point.addUpdateLine(line, false);
					
					lines.add(line);
				}
				last = point;
			}
			
			Line line = new Line(last.getX(), last.getY(), drawingPoint.getX(), drawingPoint.getY());
			last.addUpdateLine(line, true);
			drawingPoint.addUpdateLine(line, false);
			
			lines.add(line);
		}
		
		for(USPPoint divider : dividerPoints) {
			dividers.add(new Circle(divider.getX(), divider.getY(), DIVIDER_DIAMETER/2));
		}
		
		configGroup.getChildren().addAll(lines);
		configGroup.getChildren().addAll(dividers);
	}
	
	public void removeCurrentSelection() {

	}

	public boolean checkIfCoordinatesOnLine(double screenX, double screenY) {

		Point2D localPoint = configGroup.screenToLocal(screenX, screenY);
		double x = localPoint.getX();
		double y = localPoint.getY();
		
		for(LinkedList<USPPoint> pointList : points) {
			USPPoint last = null;
			
			for(USPPoint point : pointList) {
				
				if(last != null) {
					
					if(point.getX() == last.getX()) {
						// Vertical line
						if(checkIfCoordOnLineVert(point, last, x, y)) {
							return true;
						}
						
					} else {
						// Horizontal line
						if(checkIfCoordOnLineHor(point, last, x, y)) {
							return true;
						}
						
					}
				}

				last = point;
			}
		}
		
		return false;
	}

	public double getMaxX() {
		boolean first = true;
		double maxX = 0;

		for(LinkedList<USPPoint> pointList : points) {
			for(USPPoint point : pointList) {
				if(first) {
					maxX = point.getX();
					first = false;
				} else {
					maxX = point.getX() > maxX ? point.getX() : maxX;
				}
			}
		}
		
		if(drawingPoints != null) {
			for(USPPoint point : drawingPoints) {
				if(first) {
					maxX = point.getX();
					first = false;
				} else {
					maxX = point.getX() > maxX ? point.getX() : maxX;
				}
			}
			
			maxX = drawingPoint.getX() > maxX ? drawingPoint.getX() : maxX;
		}

		return maxX;
	}

	public double getMaxY() {
		boolean first = true;
		double maxY = 0;

		for(LinkedList<USPPoint> pointList : points) {
			for(USPPoint point : pointList) {
				if(first) {
					maxY = point.getY();
					first = false;
				} else {
					maxY = point.getY() > maxY ? point.getY() : maxY;
				}
			}
		}
		
		if(drawingPoints != null) {
			for(USPPoint point : drawingPoints) {
				if(first) {
					maxY = point.getY();
					first = false;
				} else {
					maxY = point.getX() > maxY ? point.getY() : maxY;
				}
			}
			
			maxY = drawingPoint.getY() > maxY ? drawingPoint.getY() : maxY;
		}

		return maxY;
	}

	public double getMinX() {
		boolean first = true;
		double minX = 0;

		for(LinkedList<USPPoint> pointList : points) {
			for(USPPoint point : pointList) {
				if(first) {
					minX = point.getX();
					first = false;
				} else {
					minX = point.getX() < minX ? point.getX() : minX;
				}
			}
		}
		
		if(drawingPoints != null) {
			for(USPPoint point : drawingPoints) {
				if(first) {
					minX = point.getX();
					first = false;
				} else {
					minX = point.getX() < minX ? point.getX() : minX;
				}
			}
			
			minX = drawingPoint.getX() < minX ? drawingPoint.getX() : minX;
		}

		return minX;
	}

	public double getMinY() {
		boolean first = true;
		double minY = 0;

		for(LinkedList<USPPoint> pointList : points) {
			for(USPPoint point : pointList) {
				if(first) {
					minY = point.getY();
					first = false;
				} else {
					minY = point.getY() < minY ? point.getY() : minY;
				}
			}
		}
		
		if(drawingPoints != null) {
			for(USPPoint point : drawingPoints) {
				if(first) {
					minY = point.getY();
					first = false;
				} else {
					minY = point.getY() < minY ? point.getY() : minY;
				}
			}
			
			minY = drawingPoint.getY() < minY ? drawingPoint.getY() : minY;
		}

		return minY;
	}

	boolean checkRekusivity(HashSet<ConnectionLineEndpointInterface> endpoints, boolean forward) {
/*
		if (!forward) {

			HashSet<Input> inputs = getInputs();

			for (Input input : inputs) {
				HashSet<Output> nextOutputs = input.getPlugin().getOutputs();

				for (Output output : nextOutputs) {
					if (endpoints.contains(output)) {
						return true;
					}
				}

				for (Output output : nextOutputs) {
					if (output.getLine() != null && output.getLine().parent.checkRekusivity(endpoints, forward)) {
						return true;
					}
				}
			}

		} else {
			HashSet<Output> outputs = getOutputs();

			for (Output output : outputs) {
				HashSet<Input> nextInputs = output.getPlugin().getInputs();

				for (Input input : nextInputs) {
					if (endpoints.contains(input)) {
						return true;
					}
				}

				for (Input input : nextInputs) {
					if (input.getLine() != null && input.getLine().parent.checkRekusivity(endpoints, forward)) {
						return true;
					}
				}
			}
		}
*/
		return false;
	}

	public void collectConnectionLineInfos(Document doc, Element element) {


	}

	public void setConnectionLinesConfig(NodeList nodeList) {

	}

	public void unifyConnections(PluginConnection other, double screenX, double screenY) {
		
		Point2D localPoint = configGroup.screenToLocal(screenX, screenY);
		double x = localPoint.getX();
		double y = localPoint.getY();
		
		LinkedList<USPPoint> prePoints = null;
		LinkedList<USPPoint> postPoints = null;
		LinkedList<USPPoint> otherPoints = null;
		LinkedList<USPPoint> removeList = null;
		
		USPPoint dividerPoint = null;
		
		boolean unified = false;
		boolean unifyHor = false;
		
		for(LinkedList<USPPoint> pointList : points) {
			USPPoint last = null;
			
			prePoints = new LinkedList<>();
			postPoints = new LinkedList<>();
			
			for(USPPoint point : pointList) {
				
				if(last != null && !unified) {
					
					if(point.getX() == last.getX()) {
						// Vertical line
						if(checkIfCoordOnLineVert(point, last, x, y)) {
							unified = true;
							dividerPoint = new USPPoint(point.getX(), other.drawingPoint.getY());
							otherPoints = other.drawingPoints;
							unifyHor = false;
						}
						
					} else {
						// Horizontal line
						if(checkIfCoordOnLineHor(point, last, x, y)) {
							unified = true;
							dividerPoint = new USPPoint(other.drawingPoint.getX(), point.getY());
							otherPoints = other.drawingPoints;
							unifyHor = true;
						}
						
					}
				}
				
				if(!unified) {
					prePoints.add(point);
				} else {
					postPoints.offerFirst(point);
				}
				
				last = point;
			}
			
			if(unified) {
				break;
			}
		}
		
		if(unified && !(unifyHor ^ other.drawingHorizontal)) {
			dividerPoints.add(dividerPoint);
			prePoints.add(dividerPoint);
			postPoints.add(dividerPoint);
			otherPoints.add(dividerPoint);
			points.remove(removeList);
			points.add(prePoints);
			points.add(postPoints);
			points.add(otherPoints);
			other.finalizeOnDivider();
			redraw();
			configGroup.finalizeDrawing();
		}

	}
	
	private void finalizeOnDivider() {
		configGroup.getChildren().removeAll(lines);
	}
	
	private boolean checkIfCoordOnLineVert(USPPoint p1, USPPoint p2, double x, double y) {
		
		USPPoint upper;
		USPPoint lower;
		
		if(Math.abs(p1.getY() - p2.getY()) < LINE_OFFSET*2) {
			return false;
		}
		
		if(p1.getY() > p2.getY()) {
			upper = p1;
			lower = p2;
		} else {
			upper = p2;
			lower = p1;
		}
		
		if((p1.getX() - LINE_OFFSET) < x && (p1.getX() + LINE_OFFSET) > x) {
			// Matching x
			if((upper.getY() - LINE_OFFSET) > y && (lower.getY() + LINE_OFFSET) < y) {
				// Matching y
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private boolean checkIfCoordOnLineHor(USPPoint p1, USPPoint p2, double x, double y) {
		USPPoint left;
		USPPoint right;
		
		if(Math.abs(p1.getX() - p2.getX()) < LINE_OFFSET*2) {
			return false;
		}
		
		if(p1.getX() > p2.getX()) {
			right = p1;
			left = p2;
		} else {
			right = p2;
			left = p1;
		}
		
		if((p1.getY() - LINE_OFFSET) < y && (p1.getY() + LINE_OFFSET) > y) {
			// Matching y
			if((right.getX() - LINE_OFFSET) > x && (left.getX() + LINE_OFFSET) < x) {
				// Matching x
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Changes the orientation of the current line for horizontal to vertical
	 * and vertical to horizontal at the given coordinates if we draw a new
	 * line.
	 * 
	 * @param x
	 *            The x-coordinate where the orientation should be changed.
	 * @param y
	 *            The y-coordinate where the orientation should be changed.
	 */
	public void changeOrientation(double x, double y) {
		double raster = configGroup.getRaster();
		
		if(drawingHorizontal) {
			y = drawingPoints.getLast().getY();
			x = Math.round(x/raster) * raster;
		} else {
			x = drawingPoints.getLast().getX();
			y = Math.round(y/raster) * raster;
		}
		
		drawingPoint = new USPPoint(x,y);
		drawingPoints.add(drawingPoint);
		drawingPoint = new USPPoint(x,y);
		
		redraw();
		drawingHorizontal = !drawingHorizontal;
	}

	/**
	 * Proves if this connection has already got a source for date.
	 * 
	 * @return True if there is already a input. Else false.
	 */
	public boolean hasInput() {

		

		return false;
	}

	/**
	 * Collects all {@link Input}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Input}s
	 */
	public HashSet<Input> getInputs() {

		HashSet<Input> inputs = new HashSet<>();

		
		return inputs;
	}

	/**
	 * Collects all {@link Output}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Output}s
	 */
	public HashSet<Output> getOutputs() {

		HashSet<Output> outputs = new HashSet<>();

		
		return outputs;
	}

	/**
	 * Finalize the drawing of a line at a
	 * {@link ConnectionLineEndpointInterface}.
	 * 
	 * @param endpoint
	 *            The {@link ConnectionLineEndpointInterface} at which this
	 *            connection should end. Must not be null.
	 * @param xCoord
	 *            The x-coordinate at which the connection should end.
	 * @param yCoord
	 *            The y-coordinate at which the connection should end.
	 */
	public void endPluginConnection(@Nonnull ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {
		
		if(drawingHorizontal && yCoord != drawingPoints.getFirst().getY()) {
			double startX = drawingPoints.getLast().getX();
			double startY = drawingPoints.getLast().getY();
			
			double middleX = (xCoord - startX)/2;
			double raster = configGroup.getRaster();
			
			middleX += startX;
			middleX = Math.round(middleX/raster) * raster;
			
			drawingPoints.add(new USPPoint(middleX, startY));
			drawingPoints.add(new USPPoint(middleX, yCoord));
		} else if(yCoord != drawingPoints.getFirst().getY()) {
			drawingPoints.add(new USPPoint(drawingPoints.getLast().getX(), yCoord));
		}

		drawingPoints.add(new USPPoint(xCoord, yCoord));
		points.add(drawingPoints);
		drawingPoint = null;
		drawingPoints = null;
		
		redraw();
	}

	/**
	 * Updates the coordinates of the line which is the current line for
	 * drawing.
	 * 
	 * @param xCoord
	 *            The new x-coordinate.
	 * @param yCoord
	 *            The new y-coordinate.
	 */
	public void drawLine(double xCoord, double yCoord) {
		
		double raster = configGroup.getRaster();
		
		if(drawingHorizontal) {
			yCoord = drawingPoints.getLast().getY();
			xCoord = Math.round(xCoord/raster) * raster;
		} else {
			xCoord = drawingPoints.getLast().getX();
			yCoord = Math.round(yCoord/raster) * raster;
		}
		
		drawingPoint.setCoordinates(xCoord, yCoord);
	}

	/**
	 * Divides the {@link ConnectionLine} which is used for drawing into tree
	 * lines where the last line ends at the given coordinates.
	 * 
	 * @param x
	 *            The x-coordinate where the current line should end.
	 * @param y
	 *            the y-coordinate where the current line should end.
	 */
	public void devideActLine(double x, double y) {

	}
	
	public boolean isDrawingHorizontal() {
		return drawingHorizontal;
	}

	private class USPPoint {

		private HashMap<Line, Boolean> updateLines = new HashMap<>();
		
		private double x;
		private double y;
		
		public USPPoint(double x, double y) {
			
			this.x = x;
			this.y = y;
		}
		
		public void clearUpdateLines() {
			updateLines.clear();
		}
		
		public void addUpdateLine(Line line, boolean first) {
			updateLines.put(line, first);
		}
		
		public void setCoordinates(double x, double y) {
			this.x = x;
			this.y = y;
			
			for(Line line : updateLines.keySet()) {
				if(updateLines.get(line).booleanValue()) {
					line.setStartX(x);
					line.setStartY(y);
				} else {
					line.setEndX(x);
					line.setEndY(y);
				}
			}
		}
		
		public double getX() {
			return x;
		}
		
		public double getY() {
			return y;
		}
	}
	
	
}
