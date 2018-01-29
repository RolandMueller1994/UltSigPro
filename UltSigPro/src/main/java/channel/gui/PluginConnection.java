package channel.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.javafx.geom.Line2D;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
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

	private static final double LINE_OFFSET = 2;
	private static final double DIVIDER_DIAMETER = 10;

	private static final double MIN_LINE_LENGTH = 10;

	private PluginConfigGroup configGroup;

	private HashSet<LinkedList<USPPoint>> points = new HashSet<>();
	private LinkedList<USPPoint> drawingPoints = new LinkedList<>();
	private USPPoint drawingPoint;
	private HashSet<USPPoint> dividerPoints = new HashSet<>();

	private HashMap<HashSet<USPLine>, LinkedList<USPPoint>> lines = new HashMap<>();
	private HashSet<USPDivider> dividers = new HashSet<>();

	private HashSet<USPLine> deletionLines;
	private LinkedList<USPPoint> deletionPoints;

	private LinkedList<USPPoint> coordinatesPoints;
	private USPPoint firstCoordinatesPoint;
	private USPPoint secondCoordinatesPoint;
	private USPPoint dividerCoordinatesPoint;
	private boolean coordinatesHorizontal;

	private Output input;
	private USPPoint inputPoint;
	private HashMap<Input, USPPoint> outputs = new HashMap<>();

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

		if (endpoint instanceof Output) {
			input = (Output) endpoint;
			inputPoint = drawingPoints.get(0);
		} else {
			outputs.put((Input) endpoint, drawingPoints.get(0));
		}

		endpoint.addConnection(this);

		redraw();
	}

	public USPPoint getDrawingPoint() {
		return drawingPoint;
	}

	public void escapeLineDrawing() {
		drawingPoint = null;

		if (drawingPoints != null) {
			if (drawingPoints.getFirst().equals(inputPoint)) {
				inputPoint = null;
				input.removeConnection(this);
			} else {
				Input remove = null;
				for (Input output : outputs.keySet()) {
					if (drawingPoints.getFirst().equals(outputs.get(output))) {
						remove = output;
						output.removeConnection(this);
						break;
					}
				}

				if (remove != null) {
					outputs.remove(remove);
				}
			}

			drawingPoints.clear();
			drawingPoints = null;
		}

		redraw();
	}

	public PluginConnection(@Nonnull PluginConfigGroup configGroup) {
		this.configGroup = configGroup;
	}

	private void redraw() {

		for (HashSet<USPLine> part : lines.keySet()) {
			for (USPLine line : part) {
				line.clear();
			}
			configGroup.getChildren().removeAll(part);
		}

		for (USPDivider divider : dividers) {
			divider.clear();
		}
		configGroup.getChildren().removeAll(dividers);

		lines.clear();
		dividers.clear();

		if (drawingPoints != null) {
			USPPoint last = null;
			HashSet<USPLine> part = new HashSet<>();
			for (USPPoint point : drawingPoints) {
				if (last != null) {
					USPLine line = new USPLine(last, point, this, configGroup, isLineHor(last, point));
					configGroup.getChildren().add(line);
					line.setStyle(USPLineStyle.DRAWING_LINE);

					part.add(line);
				}
				last = point;
			}

			USPLine line = new USPLine(last, drawingPoint, this, configGroup, isLineHor(last, drawingPoint));
			configGroup.getChildren().add(line);
			line.setStyle(USPLineStyle.DRAWING_LINE);

			part.add(line);
			lines.put(part, drawingPoints);
		}

		for (LinkedList<USPPoint> pointList : points) {
			USPPoint last = null;
			HashSet<USPLine> part = new HashSet<>();
			for (USPPoint point : pointList) {
				if (last != null) {
					USPLine line = new USPLine(last, point, this, configGroup, isLineHor(last, point));
					configGroup.getChildren().add(line);
					line.setStyle(USPLineStyle.NORMAL_LINE);

					part.add(line);
				}
				last = point;
			}

			lines.put(part, pointList);
		}

		for (USPPoint divider : dividerPoints) {
			dividers.add(new USPDivider(divider, this, configGroup));
		}

		/*
		 * for(HashSet<USPLine> part : lines.keySet()) {
		 * configGroup.getChildren().addAll(part); }
		 */

		configGroup.getChildren().addAll(dividers);
	}

	private boolean isLineHor(USPPoint start, USPPoint end) {

		if (start.getX() == end.getX()) {
			return false;
		}
		return true;
	}

	public void removeCurrentSelection() {

		if (deletionLines != null) {
			for (USPLine deletionLine : deletionLines) {
				deletionLine.setStyle(USPLineStyle.NORMAL_LINE);
			}

			deletionLines = null;
			deletionPoints = null;
		}

	}

	private USPLineOrientation getOrientationForPoints(USPPoint end, USPPoint beforeEnd) {
		if (end.getX() == beforeEnd.getX()) {
			if (end.getY() > beforeEnd.getY()) {
				return USPLineOrientation.BOTTOM_UP;
			} else {
				return USPLineOrientation.TOP_DOWN;
			}
		} else {
			if (end.getX() > beforeEnd.getX()) {
				return USPLineOrientation.LEFT_TO_RIGHT;
			} else {
				return USPLineOrientation.RIGHT_TO_LEFT;
			}
		}
	}

	public boolean checkIfCoordinatesOnLine(double x, double y) {
		return checkIfCoordinatesOnLine(new USPPoint(x, y));
	}

	public boolean checkIfCoordinatesOnLine(USPPoint drawingPoint) {

		// Point2D localPoint = configGroup.screenToLocal(screenX, screenY);
		// double x = localPoint.getX();
		// double y = localPoint.getY();

		dividerCoordinatesPoint = null;

		double x = drawingPoint.getX();
		double y = drawingPoint.getY();

		for (USPPoint dividerPoint : dividerPoints) {

			double dividerX = dividerPoint.getX();
			double dividerY = dividerPoint.getY();

			if (x > dividerX - DIVIDER_DIAMETER / 2 && x < dividerX + DIVIDER_DIAMETER / 2
					&& y > dividerY - DIVIDER_DIAMETER / 2 && y < dividerY + DIVIDER_DIAMETER / 2) {

				if (configGroup.getWorkCon() == null) {
					dividerCoordinatesPoint = dividerPoint;
					return true;
				}

				USPLineOrientation orientation = configGroup.getWorkCon().getDrawingOrientation();

				for (LinkedList<USPPoint> part : points) {
					if (dividerPoint.equals(part.getFirst())) {
						if (orientation == getOrientationForPoints(part.getFirst(), part.get(1))) {
							return false;
						}
					} else if (dividerPoint.equals(part.getLast())) {
						if (orientation == getOrientationForPoints(part.getLast(), part.get(part.size() - 2))) {
							return false;
						}
					}
				}

				dividerCoordinatesPoint = dividerPoint;
				return true;
			}

		}

		for (LinkedList<USPPoint> pointList : points) {
			USPPoint last = null;

			for (USPPoint point : pointList) {

				if (last != null) {

					if (point.getX() == last.getX()) {
						// Vertical line
						if (checkIfCoordOnLineVert(point, last, x, y)) {
							coordinatesPoints = pointList;
							firstCoordinatesPoint = last;
							secondCoordinatesPoint = point;
							coordinatesHorizontal = false;
							return true;
						} else {
							coordinatesPoints = null;
						}

					} else {
						// Horizontal line
						if (checkIfCoordOnLineHor(point, last, x, y)) {
							coordinatesPoints = pointList;
							firstCoordinatesPoint = last;
							secondCoordinatesPoint = point;
							coordinatesHorizontal = true;
							return true;
						} else {
							coordinatesPoints = null;
						}

					}
				}

				last = point;
			}
		}

		return false;
	}

	public void dragLine(double x, double y) {

		double raster = configGroup.getRaster();
		y = Math.round(y / raster) * raster;
		x = Math.round(x / raster) * raster;

		boolean redraw = false;

		if (dividerCoordinatesPoint != null) {

			System.out.println("Drag divider");

			HashSet<LinkedList<USPPoint>> checkLists = new HashSet<>();

			for (LinkedList<USPPoint> checkList : points) {
				if (dividerCoordinatesPoint.equals(checkList.getFirst())
						|| dividerCoordinatesPoint.equals(checkList.getLast())) {
					checkLists.add(checkList);
				}
			}

			for (LinkedList<USPPoint> check : checkLists) {

				USPPoint secondEnd = null;
				boolean leftToRight = false;

				if (dividerCoordinatesPoint.equals(check.getFirst())) {
					secondEnd = check.getLast();
				} else {
					secondEnd = check.getFirst();
				}

				leftToRight = secondEnd.getX() > dividerCoordinatesPoint.getX();

				if (!checkDragNDropInt(dividerCoordinatesPoint, x, y, check, secondEnd, leftToRight)) {
					return;
				}
			}

			double savedX = dividerCoordinatesPoint.getX();
			double savedY = dividerCoordinatesPoint.getY();

			for (LinkedList<USPPoint> check : checkLists) {

				USPPoint secondEnd = null;

				if (dividerCoordinatesPoint.equals(check.getFirst())) {
					secondEnd = check.getLast();
				} else {
					secondEnd = check.getFirst();
				}

				dragNDropInt(dividerCoordinatesPoint, x, y, check, secondEnd, false);
				dividerCoordinatesPoint.setCoordinates(savedX, savedY);
			}

			dividerCoordinatesPoint.setCoordinates(x, y);
		} else if (coordinatesPoints != null) {

			if (coordinatesHorizontal && y != firstCoordinatesPoint.getY()) {
				USPPoint left = null;
				USPPoint right = null;

				if (firstCoordinatesPoint.getX() > secondCoordinatesPoint.getX()) {
					left = secondCoordinatesPoint;
					right = firstCoordinatesPoint;
				} else {
					right = secondCoordinatesPoint;
					left = firstCoordinatesPoint;
				}

				boolean toShort = false;
				boolean rightSet = false;
				boolean leftSet = false;

				if (right.getX() - left.getX() < 3 * MIN_LINE_LENGTH) {
					toShort = true;
				}

				if (left.equals(coordinatesPoints.getFirst())) {
					if (toShort) {
						return;
					}
					double divideX = left.getX() + MIN_LINE_LENGTH;
					divideX = Math.ceil(divideX / raster) * raster;

					coordinatesPoints.add(1, new USPPoint(divideX, left.getY()));
					coordinatesPoints.add(2, new USPPoint(divideX, y));
					redraw = true;

					if (left.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(2);
					}
				} else if (left.equals(coordinatesPoints.getLast())) {
					if (toShort) {
						return;
					}
					double divideX = left.getX() + MIN_LINE_LENGTH;
					divideX = Math.ceil(divideX / raster) * raster;

					int index = coordinatesPoints.size();
					index -= 1;

					coordinatesPoints.add(index, new USPPoint(divideX, left.getY()));
					coordinatesPoints.add(index, new USPPoint(divideX, y));
					redraw = true;

					if (left.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index);
					}
				} else if (left.equals(coordinatesPoints.get(1))
						&& coordinatesPoints.get(1).getX() == coordinatesPoints.getFirst().getX()
						&& (coordinatesPoints.get(1).getY() > coordinatesPoints.getFirst().getY()
								? y - coordinatesPoints.getFirst().getY() < MIN_LINE_LENGTH
								: coordinatesPoints.getFirst().getY() - y < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					double divideX = coordinatesPoints.getFirst().getX() + MIN_LINE_LENGTH;

					coordinatesPoints.add(2, new USPPoint(divideX, coordinatesPoints.get(1).getY()));
					coordinatesPoints.add(3, new USPPoint(divideX, y));
					redraw = true;

					if (left.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(3);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(3);
					}
				} else if (left.equals(coordinatesPoints.get(coordinatesPoints.size() - 2))
						&& coordinatesPoints.get(coordinatesPoints.size() - 2).getX() == coordinatesPoints.getLast()
								.getX()
						&& (coordinatesPoints.get(coordinatesPoints.size() - 2).getY() > coordinatesPoints.getLast()
								.getY() ? y - coordinatesPoints.getLast().getY() < MIN_LINE_LENGTH
										: coordinatesPoints.getLast().getY() - y < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					int index = coordinatesPoints.size();

					double divideX = coordinatesPoints.getLast().getX() + MIN_LINE_LENGTH;

					coordinatesPoints.add(index - 2, new USPPoint(divideX, coordinatesPoints.get(index - 2).getY()));
					coordinatesPoints.add(index - 2, new USPPoint(divideX, y));
					redraw = true;

					if (left.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index - 2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index - 2);
					}
				} else {
					leftSet = true;
				}

				if (right.equals(coordinatesPoints.getFirst())) {
					if (toShort) {
						return;
					}
					double divideX = right.getX() - MIN_LINE_LENGTH;
					divideX = Math.floor(divideX / raster) * raster;

					coordinatesPoints.add(1, new USPPoint(divideX, right.getY()));
					coordinatesPoints.add(2, new USPPoint(divideX, y));
					redraw = true;

					if (right.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(2);
					}
				} else if (right.equals(coordinatesPoints.getLast())) {
					if (toShort) {
						return;
					}
					double divideX = right.getX() - MIN_LINE_LENGTH;
					divideX = Math.floor(divideX / raster) * raster;

					int index = coordinatesPoints.size();
					index -= 1;

					coordinatesPoints.add(index, new USPPoint(divideX, right.getY()));
					coordinatesPoints.add(index, new USPPoint(divideX, y));
					redraw = true;

					if (right.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index);
					}
				} else if (right.equals(coordinatesPoints.get(1))
						&& coordinatesPoints.get(1).getX() == coordinatesPoints.getFirst().getX()
						&& (coordinatesPoints.get(1).getY() > coordinatesPoints.getFirst().getY()
								? y - coordinatesPoints.getFirst().getY() < MIN_LINE_LENGTH
								: coordinatesPoints.getFirst().getY() - y < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					double divideX = coordinatesPoints.getFirst().getX() - MIN_LINE_LENGTH;

					coordinatesPoints.add(2, new USPPoint(divideX, coordinatesPoints.get(1).getY()));
					coordinatesPoints.add(3, new USPPoint(divideX, y));
					redraw = true;

					if (right.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(3);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(3);
					}
				} else if (right.equals(coordinatesPoints.get(coordinatesPoints.size() - 2))
						&& coordinatesPoints.get(coordinatesPoints.size() - 2).getX() == coordinatesPoints.getLast()
								.getX()
						&& (coordinatesPoints.get(coordinatesPoints.size() - 2).getY() > coordinatesPoints.getLast()
								.getY() ? y - coordinatesPoints.getLast().getY() < MIN_LINE_LENGTH
										: coordinatesPoints.getLast().getY() - y < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					int index = coordinatesPoints.size();

					double divideX = coordinatesPoints.getLast().getX() - MIN_LINE_LENGTH;

					coordinatesPoints.add(index - 2, new USPPoint(divideX, coordinatesPoints.get(index - 2).getY()));
					coordinatesPoints.add(index - 2, new USPPoint(divideX, y));
					redraw = true;

					if (right.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index - 2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index - 2);
					}
				} else {
					rightSet = true;
				}

				if (rightSet) {
					right.setY(y);
				}

				if (leftSet) {
					left.setY(y);
				}
			} else if (!coordinatesHorizontal && x != firstCoordinatesPoint.getX()) {
				USPPoint lower = null;
				USPPoint upper = null;

				if (firstCoordinatesPoint.getY() > secondCoordinatesPoint.getY()) {
					lower = secondCoordinatesPoint;
					upper = firstCoordinatesPoint;
				} else {
					upper = secondCoordinatesPoint;
					lower = firstCoordinatesPoint;
				}

				boolean toShort = false;
				boolean lowerSet = false;
				boolean upperSet = false;

				if (upper.getY() - lower.getY() < 3 * MIN_LINE_LENGTH) {
					toShort = true;
				}

				if (lower.equals(coordinatesPoints.getFirst())) {
					if (toShort) {
						return;
					}
					double divideY = lower.getY() + MIN_LINE_LENGTH;
					divideY = Math.ceil(divideY / raster) * raster;

					coordinatesPoints.add(1, new USPPoint(lower.getX(), divideY));
					coordinatesPoints.add(2, new USPPoint(x, divideY));
					redraw = true;

					if (lower.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(2);
					}
				} else if (lower.equals(coordinatesPoints.getLast())) {
					if (toShort) {
						return;
					}
					double divideY = lower.getY() + MIN_LINE_LENGTH;
					divideY = Math.ceil(divideY / raster) * raster;

					int index = coordinatesPoints.size();
					index -= 1;

					coordinatesPoints.add(index, new USPPoint(lower.getX(), divideY));
					coordinatesPoints.add(index, new USPPoint(x, divideY));
					redraw = true;

					if (lower.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index);
					}
				} else if (lower.equals(coordinatesPoints.get(1))
						&& coordinatesPoints.get(1).getY() == coordinatesPoints.getFirst().getY()
						&& (coordinatesPoints.get(1).getX() > coordinatesPoints.getFirst().getX()
								? x - coordinatesPoints.getFirst().getX() < MIN_LINE_LENGTH
								: coordinatesPoints.getFirst().getX() - x < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					double divideY = coordinatesPoints.getFirst().getY() + MIN_LINE_LENGTH;

					coordinatesPoints.add(2, new USPPoint(coordinatesPoints.get(1).getX(), divideY));
					coordinatesPoints.add(3, new USPPoint(x, divideY));
					redraw = true;

					if (lower.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(3);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(3);
					}
				} else if (lower.equals(coordinatesPoints.get(coordinatesPoints.size() - 2))
						&& coordinatesPoints.get(coordinatesPoints.size() - 2).getY() == coordinatesPoints.getLast()
								.getY()
						&& (coordinatesPoints.get(coordinatesPoints.size() - 2).getX() > coordinatesPoints.getLast()
								.getX() ? x - coordinatesPoints.getLast().getX() < MIN_LINE_LENGTH
										: coordinatesPoints.getLast().getX() - x < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					int index = coordinatesPoints.size();

					double divideY = coordinatesPoints.getLast().getY() + MIN_LINE_LENGTH;

					coordinatesPoints.add(index - 2, new USPPoint(coordinatesPoints.get(index - 2).getX(), divideY));
					coordinatesPoints.add(index - 2, new USPPoint(x, divideY));
					redraw = true;

					if (lower.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index - 2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index - 2);
					}
				} else {
					lowerSet = true;
				}

				if (upper.equals(coordinatesPoints.getFirst())) {
					if (toShort) {
						return;
					}
					double divideY = upper.getY() - MIN_LINE_LENGTH;
					divideY = Math.floor(divideY / raster) * raster;

					coordinatesPoints.add(1, new USPPoint(upper.getX(), divideY));
					coordinatesPoints.add(2, new USPPoint(x, divideY));
					redraw = true;

					if (upper.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(2);
					}
				} else if (upper.equals(coordinatesPoints.getLast())) {
					if (toShort) {
						return;
					}
					double divideY = upper.getY() - MIN_LINE_LENGTH;
					divideY = Math.floor(divideY / raster) * raster;

					int index = coordinatesPoints.size();
					index -= 1;

					coordinatesPoints.add(index, new USPPoint(upper.getX(), divideY));
					coordinatesPoints.add(index, new USPPoint(x, divideY));
					redraw = true;

					if (upper.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index);
					}
				} else if (upper.equals(coordinatesPoints.get(1))
						&& coordinatesPoints.get(1).getY() == coordinatesPoints.getFirst().getY()
						&& (coordinatesPoints.get(1).getX() > coordinatesPoints.getFirst().getX()
								? x - coordinatesPoints.getFirst().getX() < MIN_LINE_LENGTH
								: coordinatesPoints.getFirst().getX() - x < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					double divideY = coordinatesPoints.getFirst().getY() - MIN_LINE_LENGTH;

					coordinatesPoints.add(2, new USPPoint(coordinatesPoints.get(1).getX(), divideY));
					coordinatesPoints.add(3, new USPPoint(x, divideY));
					redraw = true;

					if (upper.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(3);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(3);
					}
				} else if (upper.equals(coordinatesPoints.get(coordinatesPoints.size() - 2))
						&& coordinatesPoints.get(coordinatesPoints.size() - 2).getY() == coordinatesPoints.getLast()
								.getY()
						&& (coordinatesPoints.get(coordinatesPoints.size() - 2).getX() > coordinatesPoints.getLast()
								.getX() ? x - coordinatesPoints.getLast().getX() < MIN_LINE_LENGTH
										: coordinatesPoints.getLast().getX() - x < MIN_LINE_LENGTH)) {
					if (toShort) {
						return;
					}
					int index = coordinatesPoints.size();

					double divideY = coordinatesPoints.getLast().getY() - MIN_LINE_LENGTH;

					coordinatesPoints.add(index - 2, new USPPoint(coordinatesPoints.get(index - 2).getX(), divideY));
					coordinatesPoints.add(index - 2, new USPPoint(x, divideY));
					redraw = true;

					if (upper.equals(firstCoordinatesPoint)) {
						firstCoordinatesPoint = coordinatesPoints.get(index - 2);
					} else {
						secondCoordinatesPoint = coordinatesPoints.get(index - 2);
					}
				} else {
					upperSet = true;
				}

				if (lowerSet) {
					lower.setX(x);
				}

				if (upperSet) {
					upper.setX(x);
				}
			}
		}

		if (redraw) {
			redraw();
		}

	}

	public double getMaxX() {
		boolean first = true;
		double maxX = 0;

		for (LinkedList<USPPoint> pointList : points) {
			for (USPPoint point : pointList) {
				if (first) {
					maxX = point.getX();
					first = false;
				} else {
					maxX = point.getX() > maxX ? point.getX() : maxX;
				}
			}
		}

		if (drawingPoints != null) {
			for (USPPoint point : drawingPoints) {
				if (first) {
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

		for (LinkedList<USPPoint> pointList : points) {
			for (USPPoint point : pointList) {
				if (first) {
					maxY = point.getY();
					first = false;
				} else {
					maxY = point.getY() > maxY ? point.getY() : maxY;
				}
			}
		}

		if (drawingPoints != null) {
			for (USPPoint point : drawingPoints) {
				if (first) {
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

		for (LinkedList<USPPoint> pointList : points) {
			for (USPPoint point : pointList) {
				if (first) {
					minX = point.getX();
					first = false;
				} else {
					minX = point.getX() < minX ? point.getX() : minX;
				}
			}
		}

		if (drawingPoints != null) {
			for (USPPoint point : drawingPoints) {
				if (first) {
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

		for (LinkedList<USPPoint> pointList : points) {
			for (USPPoint point : pointList) {
				if (first) {
					minY = point.getY();
					first = false;
				} else {
					minY = point.getY() < minY ? point.getY() : minY;
				}
			}
		}

		if (drawingPoints != null) {
			for (USPPoint point : drawingPoints) {
				if (first) {
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

	public USPLineOrientation getDrawingOrientation() {

		if (drawingPoint != null) {
			return getOrientationForPoints(drawingPoint, drawingPoints.getLast());
		}
		return null;
	}

	boolean checkRekusivity(HashSet<ConnectionLineEndpointInterface> endpoints, boolean forward) {

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
					if (output.getConnection() != null && output.getConnection().checkRekusivity(endpoints, forward)) {
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
					if (input.getConnection() != null && input.getConnection().checkRekusivity(endpoints, forward)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public void collectConnectionLineInfos(Document doc, Element element) {

	}

	public void setConnectionLinesConfig(NodeList nodeList) {

	}

	public boolean unifyConnections(PluginConnection other) {

		if (other.hasInput() && hasInput()) {
			return false;
		}

		if (dividerCoordinatesPoint != null) {

			if (other.input != null) {
				input = other.input;
				inputPoint = other.inputPoint;
				input.addConnection(this);
			}

			outputs.putAll(other.outputs);

			for (Input input : other.outputs.keySet()) {
				input.addConnection(this);
			}

			LinkedList<USPPoint> otherPoints = other.drawingPoints;
			otherPoints.add(dividerCoordinatesPoint);

			points.add(otherPoints);

			other.finalizeOnDivider();
			configGroup.removeConnection(other);
			redraw();
			configGroup.finalizeDrawing();

			dividerCoordinatesPoint = null;
			return true;
		}

		double x = other.drawingPoint.getX();
		double y = other.drawingPoint.getY();

		LinkedList<USPPoint> prePoints = null;
		LinkedList<USPPoint> postPoints = null;
		LinkedList<USPPoint> otherPoints = null;
		LinkedList<USPPoint> removeList = null;

		USPPoint dividerPoint = null;

		boolean unified = false;
		boolean unifyHor = false;

		if (coordinatesPoints != null) {
			LinkedList<USPPoint> pointList = coordinatesPoints;

			USPPoint last = null;

			prePoints = new LinkedList<>();
			postPoints = new LinkedList<>();

			for (USPPoint point : pointList) {

				if (last != null && !unified) {

					if (point.getX() == last.getX()) {
						// Vertical line
						if (checkIfCoordOnLineVert(point, last, x, y)) {
							unified = true;
							dividerPoint = new USPPoint(point.getX(), other.drawingPoint.getY());
							otherPoints = other.drawingPoints;
							removeList = pointList;
							unifyHor = false;
						}

					} else {
						// Horizontal line
						if (checkIfCoordOnLineHor(point, last, x, y)) {
							unified = true;
							dividerPoint = new USPPoint(other.drawingPoint.getX(), point.getY());
							otherPoints = other.drawingPoints;
							removeList = pointList;
							unifyHor = true;
						}

					}
				}

				if (!unified) {
					prePoints.add(point);
				} else {
					postPoints.offerFirst(point);
				}

				last = point;
			}
		}

		if (unified && (unifyHor ^ other.drawingHorizontal)) {

			if (other.input != null) {
				input = other.input;
				inputPoint = other.inputPoint;
				input.addConnection(this);
			}

			outputs.putAll(other.outputs);

			for (Input input : other.outputs.keySet()) {
				input.addConnection(this);
			}

			dividerPoints.add(dividerPoint);
			prePoints.add(dividerPoint);
			postPoints.add(dividerPoint);
			otherPoints.add(dividerPoint);

			// This is necessary due to remove doesn't work here.
			HashSet<LinkedList<USPPoint>> cachedList = new HashSet<>();
			for (LinkedList<USPPoint> list : points) {
				if (list.hashCode() != removeList.hashCode()) {
					cachedList.add(list);
				}
			}
			points = cachedList;

			points.add(prePoints);
			points.add(postPoints);
			points.add(otherPoints);
			other.finalizeOnDivider();
			configGroup.removeConnection(other);
			redraw();
			configGroup.finalizeDrawing();
			return true;
		}
		return false;
	}

	private void finalizeOnDivider() {

		for (HashSet<USPLine> part : lines.keySet()) {
			for (USPLine line : part) {
				line.clear();
			}

			configGroup.getChildren().removeAll(part);
		}
	}

	private boolean checkIfCoordOnLineVert(USPPoint p1, USPPoint p2, double x, double y) {

		USPPoint upper;
		USPPoint lower;

		if (Math.abs(p1.getY() - p2.getY()) < LINE_OFFSET * 2) {
			return false;
		}

		if (p1.getY() > p2.getY()) {
			upper = p1;
			lower = p2;
		} else {
			upper = p2;
			lower = p1;
		}

		if ((p1.getX() - LINE_OFFSET) < x && (p1.getX() + LINE_OFFSET) > x) {
			// Matching x
			if ((upper.getY() - LINE_OFFSET) > y && (lower.getY() + LINE_OFFSET) < y) {
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

		if (Math.abs(p1.getX() - p2.getX()) < LINE_OFFSET * 2) {
			return false;
		}

		if (p1.getX() > p2.getX()) {
			right = p1;
			left = p2;
		} else {
			right = p2;
			left = p1;
		}

		if ((p1.getY() - LINE_OFFSET) < y && (p1.getY() + LINE_OFFSET) > y) {
			// Matching y
			if ((right.getX() - LINE_OFFSET) > x && (left.getX() + LINE_OFFSET) < x) {
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

		System.out.println("change called");

		if (drawingHorizontal) {
			y = drawingPoints.getLast().getY();
			x = Math.round(x / raster) * raster;
		} else {
			x = drawingPoints.getLast().getX();
			y = Math.round(y / raster) * raster;
		}

		drawingPoint = new USPPoint(x, y);
		drawingPoints.add(drawingPoint);
		drawingPoint = new USPPoint(x, y);

		redraw();
		drawingHorizontal = !drawingHorizontal;
	}

	/**
	 * Proves if this connection has already got a source for date.
	 * 
	 * @return True if there is already a input. Else false.
	 */
	public boolean hasInput() {

		return input != null;
	}

	/**
	 * Collects all {@link Input}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Input}s
	 */
	public HashSet<Input> getInputs() {

		HashSet<Input> retInputs = new HashSet<>();

		retInputs.addAll(outputs.keySet());

		return retInputs;
	}

	/**
	 * Collects all {@link Output}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Output}s
	 */
	public HashSet<Output> getOutputs() {

		HashSet<Output> retOutputs = new HashSet<>();

		if (input != null) {
			retOutputs.add(input);
		}

		return retOutputs;
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

		drawingPoints.add(drawingPoint);
		points.add(drawingPoints);

		if (endpoint instanceof Output) {
			input = (Output) endpoint;
			inputPoint = drawingPoints.getLast();
		} else {
			outputs.put((Input) endpoint, drawingPoints.getLast());
		}

		dragNDrop(endpoint, xCoord, yCoord);

		endpoint.addConnection(this);

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

		if (drawingHorizontal) {
			yCoord = drawingPoints.getLast().getY();
			xCoord = Math.round(xCoord / raster) * raster;

			if (drawingPoints.size() == 1) {
				if (input != null) {
					// Only drawing to right
					if (xCoord - MIN_LINE_LENGTH < drawingPoints.getFirst().getX()) {
						xCoord = drawingPoint.getX();
					}
				} else {
					// Only drawing to left
					if (xCoord + MIN_LINE_LENGTH > drawingPoints.getFirst().getX()) {
						xCoord = drawingPoint.getX();
					}
				}
			}
		} else {
			xCoord = drawingPoints.getLast().getX();
			yCoord = Math.round(yCoord / raster) * raster;
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

	private USPPoint getStartPoint(ConnectionLineEndpointInterface endpoint) {

		USPPoint startPoint = null;

		if (endpoint.equals(input)) {
			startPoint = inputPoint;
		} else {
			for (Input output : outputs.keySet()) {
				if (endpoint.equals(output)) {
					startPoint = outputs.get(output);
					break;
				}
			}
		}

		return startPoint;
	}

	private LinkedList<USPPoint> getCheckList(USPPoint startPoint) {

		LinkedList<USPPoint> check = null;

		for (LinkedList<USPPoint> subPoints : points) {
			if (subPoints.getFirst().equals(startPoint)) {
				check = subPoints;
				break;
			} else if (subPoints.getLast().equals(startPoint)) {
				check = subPoints;
				break;
			}
		}

		return check;
	}

	private USPPoint getSecondEnd(USPPoint startPoint, LinkedList<USPPoint> list) {

		USPPoint secondEnd = null;

		if (list.getFirst().equals(startPoint)) {
			secondEnd = list.getLast();
		} else if (list.getLast().equals(startPoint)) {
			secondEnd = list.getFirst();
		}

		return secondEnd;
	}

	private void addTwoPoints(USPPoint start, LinkedList<USPPoint> list, int index, boolean hor) {

		boolean forward;

		if (start.equals(list.getFirst())) {
			forward = true;
		} else {
			forward = false;
		}

		USPPoint first = null;
		USPPoint second = null;

		if (forward) {
			first = list.get(index);
			second = list.get(index + 1);
		} else {
			first = list.get(list.size() - index);
			second = list.get(list.size() - index - 1);
		}

		double x1, y1, x2, y2;

		if (hor) {
			y1 = (first.getY() - second.getY()) / 2 + second.getY();
			y2 = y1;
			x1 = first.getX();
			x2 = second.getX();
		} else {
			x1 = (first.getX() - second.getX()) / 2 + second.getX();
			x2 = x1;
			y1 = first.getY();
			y2 = second.getY();
		}

		double raster = configGroup.getRaster();

		x1 = Math.round(x1 / raster) * raster;
		y1 = Math.round(y1 / raster) * raster;
		x2 = Math.round(x2 / raster) * raster;
		y2 = Math.round(y2 / raster) * raster;

		if (forward) {
			list.add(index + 1, new USPPoint(x1, y1));
			list.add(index + 2, new USPPoint(x2, y2));
		} else {
			list.add(index, new USPPoint(x2, y2));
			list.add(index, new USPPoint(x1, y1));
		}
	}

	private boolean checkDragNDropInt(USPPoint startPoint, double x, double y, LinkedList<USPPoint> check,
			USPPoint secondEnd, boolean leftToRight) {

		if (startPoint.equals(check.getLast())) {
			LinkedList<USPPoint> invert = new LinkedList<>();

			for (USPPoint point : check) {
				invert.offerFirst(point);
			}

			check = invert;
		}

		if (check.size() == 2) {
			if (check.getLast().getY() == y) {
				if (!leftToRight) {
					if (x - secondEnd.getX() < 2 * MIN_LINE_LENGTH) {
						return false;
					}
				} else {
					if (secondEnd.getX() - x < 2 * MIN_LINE_LENGTH) {
						return false;
					}
				}
			}
		} else if (check.size() == 3) {
			if (Math.abs(check.getLast().getY() - y) <= MIN_LINE_LENGTH) {
				if (leftToRight) {
					// left to right
					if (check.get(1).getX() - x <= 2 * MIN_LINE_LENGTH) {
						return false;
					}
				} else {
					// right to left
					if (x - check.get(1).getX() <= 2 * MIN_LINE_LENGTH) {
						return false;
					}
				}
			}
		} else if (check.size() == 4) {
			if (Math.abs(check.getLast().getY() - y) <= MIN_LINE_LENGTH) {
				if (leftToRight) {
					// left to right
					if (check.getLast().getX() > check.get(2).getX()) {
						if (check.getLast().getX() - x < 3 * MIN_LINE_LENGTH) {
							return false;
						}
					}
				} else {
					// right to left
					if (check.getLast().getX() < check.get(2).getX()) {
						if (x - check.getLast().getX() < 3 * MIN_LINE_LENGTH) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public boolean checkDragNDrop(ConnectionLineEndpointInterface endpoint, double x, double y) {

		USPPoint startPoint = getStartPoint(endpoint);

		if (startPoint != null) {

			LinkedList<USPPoint> check = getCheckList(startPoint);
			USPPoint secondEnd = getSecondEnd(startPoint, check);

			if (check != null) {

				boolean leftToRight = endpoint instanceof Output;

				return checkDragNDropInt(startPoint, x, y, check, secondEnd, leftToRight);
			}
		}

		return true;
	}

	public void dragNDropInt(USPPoint startPoint, double x, double y, LinkedList<USPPoint> check, USPPoint secondEnd,
			boolean fromDivider) {

		double raster = configGroup.getRaster();

		LinkedList<USPPoint> keep = null;

		if (startPoint.equals(check.getLast())) {
			keep = check;
			check = new LinkedList<USPPoint>();

			for (USPPoint point : keep) {
				check.offerFirst(point);
			}
		}

		boolean horizontal = false;
		boolean leftToRight = false;
		boolean bottomUp = false;

		if (check.get(1).getY() == check.getFirst().getY()) {
			horizontal = true;
			if (check.get(1).getX() > check.getFirst().getX()) {
				leftToRight = true;
			} else {
				leftToRight = false;
			}
		} else {
			horizontal = false;
			if (check.get(1).getY() > check.getFirst().getY()) {
				bottomUp = true;
			} else {
				bottomUp = false;
			}
		}

		boolean redraw = false;

		if (check != null) {

			if (horizontal) {
				if (check.size() == 2) {
					// Horizontal line
					if (startPoint.getY() == y) {
						if (!fromDivider) {
							startPoint.setX(x);
						}
					} else {
						if (!fromDivider) {
							startPoint.setX(x);
							startPoint.setY(y);
						}
						addTwoPoints(startPoint, check, 0, false);
						redraw = true;
					}
				} else if (check.size() == 3) {
					// Vertical connected
					if (leftToRight) {
						if (secondEnd.getX() - x <= MIN_LINE_LENGTH) {
							check.get(1).setX(Math.ceil((x + raster) / raster) * raster);
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);

							addTwoPoints(startPoint, check, 1, true);
							redraw = true;
						} else if (secondEnd.getY() > check.get(1).getY() && secondEnd.getY() - y < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, false);
							redraw = true;
						} else if (secondEnd.getY() < check.get(1).getY() && y - secondEnd.getY() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, false);
							redraw = true;
						} else if (secondEnd.getX() - x > MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						} else {

						}
					} else {
						if (x - secondEnd.getX() <= MIN_LINE_LENGTH) {
							check.get(1).setX(Math.floor((x - raster) / raster) * raster);
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);

							addTwoPoints(startPoint, check, 1, true);
							redraw = true;
						} else if (secondEnd.getY() > check.get(1).getY() && secondEnd.getY() - y < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, false);
							redraw = true;
						} else if (secondEnd.getY() < check.get(1).getY() && y - secondEnd.getY() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, false);
							redraw = true;
						} else if (x - secondEnd.getX() > MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						} else {

						}
					}
				} else if (check.size() == 4) {
					if (leftToRight) {
						if (secondEnd.getX() - x < 3 * MIN_LINE_LENGTH && secondEnd.getX() - check.get(2).getX() > 0) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.ceil((x + raster) / raster) * raster);
							check.get(1).setY(y);

							addTwoPoints(startPoint, check, 1, true);
							redraw = true;
						} else if (check.get(1).getX() - x < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.ceil((x + raster) / raster) * raster);
							check.get(1).setY(y);
							check.get(2).setX(Math.ceil((x + raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						}
					} else {
						if (x - secondEnd.getX() < 3 * MIN_LINE_LENGTH && check.get(2).getX() - secondEnd.getX() > 0) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.floor((x - raster) / raster) * raster);
							check.get(1).setY(y);

							addTwoPoints(startPoint, check, 1, true);
							redraw = true;
						} else if (x - check.get(1).getX() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.floor((x - raster) / raster) * raster);
							check.get(1).setY(y);
							check.get(2).setX(Math.floor((x - raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						}
					}
				} else {
					if (leftToRight) {
						if (check.get(1).getX() - x < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.ceil((x + raster) / raster) * raster);
							check.get(1).setY(y);
							check.get(2).setX(Math.ceil((x + raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						}
					} else {
						if (x - check.get(1).getX() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(Math.floor((x - raster) / raster) * raster);
							check.get(1).setY(y);
							check.get(2).setX(Math.floor((x - raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(y);
						}
					}
				}
			} else {

				if (check.size() == 2) {
					// Vertical line
					if (startPoint.getX() == x) {
						if (!fromDivider) {
							startPoint.setY(y);
						}
					} else {
						if (!fromDivider) {
							startPoint.setX(x);
							startPoint.setY(y);
						}
						addTwoPoints(startPoint, check, 0, true);
						redraw = true;
					}
				} else if (check.size() == 3) {
					// Vertical connected
					if (bottomUp) {
						if (secondEnd.getY() - y <= MIN_LINE_LENGTH) {
							check.get(1).setY(Math.ceil((y + raster) / raster) * raster);
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);

							addTwoPoints(startPoint, check, 1, false);
							redraw = true;
						} else if (secondEnd.getX() > check.get(1).getX() && secondEnd.getX() - x < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, true);
							redraw = true;
						} else if (secondEnd.getX() < check.get(1).getX() && y - secondEnd.getX() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, true);
							redraw = true;
						} else if (secondEnd.getY() - y > MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						} else {

						}
					} else {
						if (y - secondEnd.getY() <= MIN_LINE_LENGTH) {
							check.get(1).setY(Math.floor((y - raster) / raster) * raster);
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);

							addTwoPoints(startPoint, check, 1, false);
							redraw = true;
						} else if (secondEnd.getX() > check.get(1).getX() && secondEnd.getX() - x < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, true);
							redraw = true;
						} else if (secondEnd.getX() < check.get(1).getX() && x - secondEnd.getX() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}

							addTwoPoints(startPoint, check, 0, true);
							redraw = true;
						} else if (y - secondEnd.getY() > MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						} else {

						}
					}
				} else if (check.size() == 4) {
					if (bottomUp) {
						if (secondEnd.getY() - y < 3 * MIN_LINE_LENGTH && secondEnd.getY() - check.get(2).getY() > 0) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.ceil((y + raster) / raster) * raster);
							check.get(1).setX(x);

							addTwoPoints(startPoint, check, 1, true);
							redraw = true;
						} else if (check.get(1).getY() - y < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.ceil((y + raster) / raster) * raster);
							check.get(1).setX(x);
							check.get(2).setY(Math.ceil((y + raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						}
					} else {
						if (y - secondEnd.getY() < 3 * MIN_LINE_LENGTH && check.get(2).getY() - secondEnd.getY() > 0) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.floor((y - raster) / raster) * raster);
							check.get(1).setX(x);

							addTwoPoints(startPoint, check, 1, false);
							redraw = true;
						} else if (y - check.get(1).getY() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.floor((y - raster) / raster) * raster);
							check.get(1).setX(x);
							check.get(2).setY(Math.floor((y - raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						}
					}
				} else {
					if (bottomUp) {
						if (check.get(1).getY() - y < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.ceil((y + raster) / raster) * raster);
							check.get(1).setX(x);
							check.get(2).setY(Math.ceil((y + raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						}
					} else {
						if (y - check.get(1).getY() < MIN_LINE_LENGTH) {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setY(Math.floor((y - raster) / raster) * raster);
							check.get(1).setX(x);
							check.get(2).setY(Math.floor((y - raster) / raster) * raster);
						} else {
							if (!fromDivider) {
								startPoint.setX(x);
								startPoint.setY(y);
							}
							check.get(1).setX(x);
						}
					}
				}
			}
		}

		if (keep != null) {
			keep.clear();

			for (USPPoint point : check) {
				keep.offerFirst(point);
			}
		}

		if (redraw) {
			redraw();
		}

	}

	public void dragNDrop(ConnectionLineEndpointInterface endpoint, double x, double y) {

		USPPoint startPoint = getStartPoint(endpoint);

		boolean leftToRight;

		if (endpoint instanceof Input) {
			leftToRight = false;
		} else {
			leftToRight = true;
		}

		if (startPoint != null) {

			LinkedList<USPPoint> check = getCheckList(startPoint);
			USPPoint secondEnd = getSecondEnd(startPoint, check);

			dragNDropInt(startPoint, x, y, check, secondEnd, false);
		}
	}

	public void setHoveredForDeletion(USPLine line) {

		configGroup.removeCurrentSelection();

		for (HashSet<USPLine> part : lines.keySet()) {
			if (part.contains(line)) {
				deletionLines = part;
				deletionPoints = lines.get(part);
				break;
			}
		}

		for (USPLine deletionLine : deletionLines) {

			deletionLine.setStyle(USPLineStyle.HOVERED_FOR_DELETION);
		}

	}

	public void deleteSelection() {

		if (deletionPoints != null) {

			if (deletionPoints.getFirst().equals(inputPoint)) {
				input.removeConnection(this);
				input = null;
				inputPoint = null;
			} else {
				for (Input out : outputs.keySet()) {
					if (outputs.get(out).equals(deletionPoints.getFirst())) {
						out.removeConnection(this);
						outputs.remove(out);
						break;
					}
				}
			}

			if (deletionPoints.getLast().equals(inputPoint)) {
				input.removeConnection(this);
				input = null;
				inputPoint = null;
			} else {
				for (Input out : outputs.keySet()) {
					if (outputs.get(out).equals(deletionPoints.getLast())) {
						out.removeConnection(this);
						outputs.remove(out);
						break;
					}
				}
			}

			HashSet<LinkedList<USPPoint>> cached = new HashSet<>();

			for (LinkedList<USPPoint> list : points) {
				if (list.hashCode() != deletionPoints.hashCode()) {
					cached.add(list);
				}
			}

			points = cached;
			deletionPoints = null;
			deletionLines = null;
			clearPoints();
			redraw();

			if (points.isEmpty()) {
				configGroup.deletePluginConnection(this);
			}
		}
	}

	public void deleteFromEndpoint(ConnectionLineEndpointInterface endpoint) {

		USPPoint point = null;

		if (endpoint instanceof Input) {
			for (Input output : outputs.keySet()) {
				if (endpoint.equals(output)) {
					point = outputs.get(output);
				}
			}
		} else {
			if (endpoint.equals(input)) {
				point = inputPoint;
			}
		}

		if (point != null) {

			for (Entry<HashSet<USPLine>, LinkedList<USPPoint>> entry : lines.entrySet()) {
				LinkedList<USPPoint> pointList = entry.getValue();

				if (point.equals(pointList.getFirst()) || point.equals(pointList.getLast())) {
					deletionLines = entry.getKey();
					deletionPoints = entry.getValue();
					break;
				}
			}

			deleteSelection();
		}
	}

	public boolean clearPoints() {

		boolean redraw = false;

		HashSet<USPPoint> removePoints = new HashSet<>();

		for (USPPoint point : dividerPoints) {
			HashSet<LinkedList<USPPoint>> connectedLists = new HashSet<>();

			for (LinkedList<USPPoint> list : points) {
				if (point.equals(list.getFirst()) || point.equals(list.getLast())) {
					connectedLists.add(list);
				}
			}

			if (connectedLists.size() == 2) {
				LinkedList<USPPoint> firstList = null;
				LinkedList<USPPoint> secondList = null;

				removePoints.add(point);

				boolean first = true;

				for (LinkedList<USPPoint> list : connectedLists) {
					if (first) {
						firstList = list;
						first = false;
					} else {
						secondList = list;
					}
				}

				HashSet<LinkedList<USPPoint>> cached = new HashSet<>();

				for (LinkedList<USPPoint> list : points) {
					if (list.hashCode() != firstList.hashCode() && list.hashCode() != secondList.hashCode()) {
						cached.add(list);
					}
				}

				points = cached;

				if (!(firstList.getFirst().equals(inputPoint) || outputs.containsValue(firstList.getFirst()))) {
					LinkedList<USPPoint> tmp = firstList;
					firstList = secondList;
					secondList = tmp;
				}

				if (point.equals(firstList.getFirst())) {
					LinkedList<USPPoint> tmp = new LinkedList<>();

					Iterator<USPPoint> revIter = firstList.descendingIterator();

					while (revIter.hasNext()) {
						tmp.add(revIter.next());
					}

					firstList = tmp;
				}

				if (point.equals(secondList.getLast())) {
					LinkedList<USPPoint> tmp = new LinkedList<>();

					Iterator<USPPoint> revIter = secondList.descendingIterator();

					while (revIter.hasNext()) {
						tmp.add(revIter.next());
					}

					secondList = tmp;
				}

				firstList.removeLast();

				if (firstList.getLast().getX() == secondList.get(1).getX()
						|| firstList.getLast().getY() == secondList.get(1).getY()) {
					secondList.removeFirst();
				}

				firstList.addAll(secondList);
				points.add(firstList);
			}
		}

		dividerPoints.removeAll(removePoints);

		for (LinkedList<USPPoint> list : points) {

			HashSet<USPPoint> removeList = new HashSet<>();
			USPPoint last = null;

			for (USPPoint point : list) {

				if (last != null) {
					if (point.equalCoordinates(last)) {
						boolean skip = false;

						if (list.indexOf(point) == list.size() - 2) {
							if (list.size() - 4 >= 0 && list.getLast().getY() < point.getY()
									&& list.get(list.size() - 4).getY() < point.getY()) {
								skip = true;
							}

							if (list.size() - 4 >= 0 && list.getLast().getY() > point.getY()
									&& list.get(list.size() - 4).getY() > point.getY()) {
								skip = true;
							}
						}

						if (list.indexOf(point) == 2) {
							if (list.size() > 3 && list.getFirst().getX() < point.getX()
									&& list.get(3).getX() < list.getFirst().getX()) {
								skip = true;
							}

							if (list.size() > 3 && list.getFirst().getX() > point.getX()
									&& list.get(3).getX() > list.getFirst().getX()) {
								skip = true;
							}
						}

						if (!skip) {
							if (!removeList.contains(last)) {
								removeList.add(last);
								removeList.add(point);
							}
						}
					}
				}
				last = point;
			}

			if (removeList.size() != 0) {

				list.removeAll(removeList);
				redraw = true;
			}
		}

		if (redraw) {
			redraw();
		}

		return redraw;
	}

	public boolean isDrawingHorizontal() {
		return drawingHorizontal;
	}

	private class USPPoint {

		private HashSet<USPLine> updateLines = new HashSet<>();
		private HashSet<USPDivider> updateDividers = new HashSet<>();

		private double x;
		private double y;

		public USPPoint(double x, double y) {

			this.x = x;
			this.y = y;
		}

		public void clearUpdateLines() {
			updateLines.clear();
		}

		public void addUpdateLine(USPLine line) {
			updateLines.add(line);
		}

		public void clearUpdateDividers() {
			updateDividers.clear();
		}

		public void addUpdateDivider(USPDivider divider) {
			updateDividers.add(divider);
		}

		public void setCoordinates(double x, double y) {
			this.x = x;
			this.y = y;

			for (USPLine line : updateLines) {
				line.updateFromUSPPoint(this);
			}

			for (USPDivider divider : updateDividers) {
				divider.updateFromUSPPoint(this);
			}
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public void setX(double x) {
			this.x = x;

			for (USPLine line : updateLines) {
				line.updateFromUSPPoint(this);
			}
		}

		public void setY(double y) {
			this.y = y;

			for (USPLine line : updateLines) {
				line.updateFromUSPPoint(this);
			}
		}

		public boolean equalCoordinates(USPPoint other) {
			if (x == other.getX() && y == other.getY()) {
				return true;
			}
			return false;
		}
	}

	public class USPDivider extends Circle {

		private USPPoint center;

		private PluginConfigGroup configGroup;
		private PluginConnection parentCon;

		public USPDivider(USPPoint center, PluginConnection parentCon, PluginConfigGroup configGroup) {
			super(center.getX(), center.getY(), DIVIDER_DIAMETER / 2);
			this.configGroup = configGroup;
			this.parentCon = parentCon;
			this.center = center;
			center.addUpdateDivider(this);
		}

		public void updateFromUSPPoint(USPPoint p) {
			if (p.equals(center)) {
				setCenterX(p.getX());
				setCenterY(p.getY());
			}
		}

		public void clear() {

			center = null;
			configGroup = null;
		}
	}

	public class USPLine extends Line {

		private USPPoint start;
		private USPPoint end;
		private MouseEventPane mouseEventPane;
		private boolean hor;

		private USPLineStyle style;

		private PluginConfigGroup configGroup;
		private PluginConnection parentCon;

		public USPLine(USPPoint start, USPPoint end, PluginConnection parentCon, PluginConfigGroup configGroup,
				boolean hor) {
			super(start.getX(), start.getY(), end.getX(), end.getY());
			this.start = start;
			this.end = end;
			this.parentCon = parentCon;
			this.configGroup = configGroup;
			this.hor = hor;
			end.addUpdateLine(this);
			start.addUpdateLine(this);
		}

		public void updateFromUSPPoint(USPPoint p) {
			if (p.equals(start)) {
				setStartX(start.getX());
				setStartY(start.getY());
			} else if (p.equals(end)) {
				setEndX(end.getX());
				setEndY(end.getY());
			}

			if (mouseEventPane != null) {
				mouseEventPane.update();
			}
		}

		public void setStyle(USPLineStyle style) {

			switch (style) {
			case NORMAL_LINE:
				setStrokeWidth(1);
				setStyle("-fx-stroke: -usp-black");
				if (mouseEventPane == null) {
					mouseEventPane = new MouseEventPane(this);
					configGroup.getChildren().add(mouseEventPane);
				}
				break;
			case DRAWING_LINE:
				setStrokeWidth(3);
				setStyle("-fx-stroke: -usp-light-blue");
				if (mouseEventPane != null) {
					configGroup.getChildren().remove(mouseEventPane);
				}
				mouseEventPane = null;
				break;
			case HOVERED_LINE:
				setStrokeWidth(3);
				setStyle("-fx-stroke: -usp-light-blue");
				break;
			case HOVERED_FOR_DELETION:
				setStrokeWidth(3);
				setStyle("-fx-stroke: -usp-light-blue");
				break;
			}

			this.style = style;
		}

		public void clear() {
			if (mouseEventPane != null) {
				configGroup.getChildren().remove(mouseEventPane);
				mouseEventPane = null;
			}

			start = null;
			end = null;
			configGroup = null;
		}

		private class MouseEventPane extends Pane {

			private USPLine parent;

			public MouseEventPane(USPLine parent) {
				this.parent = parent;
				update();

				setOnMouseEntered(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						if (parent.style != USPLineStyle.HOVERED_FOR_DELETION) {
							if (parent.configGroup.getWorkCon() != null) {
								USPPoint drawingPoint = parent.configGroup.getWorkCon().drawingPoint;
								Point2D screen = parent.configGroup.localToScreen(drawingPoint.getX(),
										drawingPoint.getY());

								Point2D local = screenToLocal(screen);

								if (local.getX() < getWidth() && local.getY() < getHeight() && local.getX() > 0
										&& local.getY() > 0) {
									parent.setStyle(USPLineStyle.HOVERED_LINE);
								}
								return;
							}

							parent.setStyle(USPLineStyle.HOVERED_LINE);
						}
					}

				});

				setOnMouseExited(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						if (parent.style != USPLineStyle.HOVERED_FOR_DELETION) {
							parent.setStyle(USPLineStyle.NORMAL_LINE);
						}
					}

				});

				setOnMouseClicked(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						if (parent.style == USPLineStyle.HOVERED_FOR_DELETION) {
							parent.parentCon.removeCurrentSelection();
							event.consume();
						} else if (parent.configGroup.getWorkCon() == null) {
							parent.parentCon.setHoveredForDeletion(parent);
							event.consume();
						}
					}

				});
			}

			public void update() {
				if (parent.end != null && parent.start != null) {
					if (parent.hor) {
						USPPoint right = null;
						USPPoint left = null;

						if (parent.end.getX() > parent.start.getX()) {
							right = parent.end;
							left = parent.start;
						} else {
							right = parent.start;
							left = parent.end;
						}

						double length = right.getX() - left.getX();
						setPrefSize(length - 2 * LINE_OFFSET, 2 * LINE_OFFSET);
						setLayoutX(left.getX() + LINE_OFFSET);
						setLayoutY(left.getY() - LINE_OFFSET);
					} else {
						USPPoint upper = null;
						USPPoint lower = null;

						if (parent.end.getY() > parent.start.getY()) {
							upper = parent.end;
							lower = parent.start;
						} else {
							upper = parent.start;
							lower = parent.end;
						}

						double length = upper.getY() - lower.getY();
						setPrefSize(2 * LINE_OFFSET, length - 2 * LINE_OFFSET);
						setLayoutX(lower.getX() - LINE_OFFSET);
						setLayoutY(lower.getY() + LINE_OFFSET);
					}
				}
			}
		}

	}

	public enum USPLineStyle {
		DRAWING_LINE, NORMAL_LINE, HOVERED_LINE, HOVERED_FOR_DELETION
	}

	private enum USPLineOrientation {
		LEFT_TO_RIGHT, RIGHT_TO_LEFT, BOTTOM_UP, TOP_DOWN
	}

}
