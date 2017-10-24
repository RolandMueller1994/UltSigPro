package channel.gui;

import java.util.HashMap;
import java.util.HashSet;

import channel.PluginInput;
import channel.PluginOutput;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class PluginConnection {

	private HashSet<ConnectionLine> lines = new HashSet<>();
	private HashSet<LineDevider> deviders = new HashSet<>();

	private ConnectionLine actLine = null;
	private boolean actHorizontal = true;

	private boolean hovered = false;

	private PluginConfigGroup configGroup;

	public PluginConnection(PluginConfigGroup configGroup, ConnectionLineEndpointInterface endpoint, double startX,
			double startY) {
		this.configGroup = configGroup;
		actLine = new ConnectionLine(configGroup, this, endpoint, startX, startY, true);

		addLine(actLine);

		configGroup.getChildren().add(actLine);
	}

	public void changeOrientation(double x, double y) {
		actHorizontal = !actHorizontal;
		actLine.setCoordinatesLine(actLine, x, y);
		if (actHorizontal) {
			ConnectionLine newLine = new ConnectionLine(configGroup, this, actLine, actLine.getEndX(), y,
					actHorizontal);
			actLine.setCoordinatesLine(newLine, x, y);
			actLine = newLine;
			addLine(newLine);
		} else {
			ConnectionLine newLine = new ConnectionLine(configGroup, this, actLine, x, actLine.getEndY(),
					actHorizontal);
			actLine.setCoordinatesLine(newLine, x, y);
			actLine = newLine;
			addLine(newLine);
		}

		configGroup.getChildren().add(actLine);
	}

	public ConnectionLine getActLine() {
		return actLine;
	}

	private void addLine(ConnectionLine line) {

		lines.add(line);
		line.registerMaxCoordinatesUpdateListener(configGroup);
	}

	public boolean hasInput() {
		
		for(ConnectionLine actLine : lines) {
			if(actLine.getFirstEnd() != null && actLine.getFirstEnd() instanceof Output) {
				return true;
			}
			
			if(actLine.getSecondEnd() != null && actLine.getSecondEnd() instanceof Output) {
				return true;
			}
		}
		 
		return false;
	}

	private void addDevider(LineDevider devider) {
		deviders.add(devider);
	}

	public void endPluginConnection(ConnectionLineEndpointInterface endpoint, double xCoord, double yCoord) {
		addLine(actLine);
		actLine.setCoordinatesFinal(endpoint, xCoord, yCoord);
		actLine = null;
	}

	public void drawLine(double xCoord, double yCoord) {
		actLine.setCoordinates(xCoord, yCoord);
	}

	public void devideActLine(double x, double y) {

		if (actHorizontal) {
			double stepX = actLine.getStartX() + (x - actLine.getStartX()) / 2;
			double stepY = actLine.getStartY();

			actLine.setEndX(stepX);

			ConnectionLine newLine = new ConnectionLine(configGroup, this, actLine, stepX, stepY, false);
			actLine.setCoordinatesLine(newLine, stepX, stepY);
			configGroup.getChildren().add(newLine);
			addLine(newLine);

			actLine = new ConnectionLine(configGroup, this, newLine, stepX, y, true);
			actLine.setCoordinates(x, y);
			configGroup.getChildren().add(actLine);
			addLine(actLine);
			newLine.setCoordinatesLine(actLine, stepX, y);
		} else {
			ConnectionLine newLine = new ConnectionLine(configGroup, this, actLine, actLine.getEndX(), y, true);
			actLine.setCoordinatesLine(newLine, x, y);

			newLine.setCoordinates(x, y);
			actLine = newLine;
			configGroup.getChildren().add(actLine);
			addLine(actLine);
		}
	}

	class ConnectionLine extends Line implements MaxCoordinatesInterface {

		private static final double minLength = 10;

		private ConnectionLineEndpointInterface firstEnd;
		private ConnectionLineEndpointInterface secondEnd;

		private ConnectionLine firstLine;
		private ConnectionLine secondLine;

		private Pane dragPane;

		private boolean horizontal;

		private PluginConnection parent;

		private PluginConfigGroup parentPane;

		public ConnectionLine(PluginConfigGroup parentPane, PluginConnection parent,
				ConnectionLineEndpointInterface firstEnd, double x, double y, boolean horizontal) {
			this.parent = parent;
			this.firstEnd = firstEnd;
			this.horizontal = horizontal;
			this.parentPane = parentPane;
			setStartX(x);
			setStartY(y);
			setEndX(x);
			setEndY(y);
		}

		public ConnectionLine(PluginConfigGroup parentPane, PluginConnection parent, ConnectionLine line, double x,
				double y, boolean horizontal) {
			this.parent = parent;
			this.firstLine = line;
			this.horizontal = horizontal;
			this.parentPane = parentPane;
			setStartX(x);
			setStartY(y);
			setEndX(x);
			setEndY(y);
		}

		public ConnectionLineEndpointInterface getFirstEnd() {
			return firstEnd;
		}

		public ConnectionLineEndpointInterface getSecondEnd() {
			return secondEnd;
		}

		public boolean isHorizontal() {
			return horizontal;
		}

		public void delete() {
			lines.remove(this);
			parentPane.getChildren().remove(dragPane);
			dragPane = null;
			parentPane.getChildren().remove(this);
			parentPane = null;

			if (firstEnd != null) {
				firstEnd.addLine(null);
				firstEnd = null;
			}
			if (secondEnd != null) {
				secondEnd.addLine(null);
				secondEnd = null;
			}
			if (firstLine != null) {
				firstLine.removeLine(this);
				firstLine.delete();
				firstLine = null;
			}
			if (secondLine != null) {
				secondLine.removeLine(this);
				secondLine.delete();
				secondLine = null;
			}
		}

		public void removeLine(ConnectionLine line) {
			if (firstLine != null && firstLine.equals(line)) {
				firstLine = null;
			} else if (secondLine != null && secondLine.equals(line)) {
				secondLine = null;
			}
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

			if (parentPane != null) {
				parentPane.updateMaxCoordinatesOfComponent(this);
			}
		}

		public void updateCoordinates(ConnectionLine line, double x, double y) {
			if (line.equals(firstLine)) {
				if (horizontal) {
					setStartX(x);
				} else {
					setStartY(y);
				}
			} else if (line.equals(secondLine)) {
				if (horizontal) {
					setEndX(x);
				} else {
					setEndY(y);
				}
			}

			if (parentPane != null) {
				parentPane.updateMaxCoordinatesOfComponent(this);
			}

			updateDragPane();
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
				if (secondEnd != null) {
					if (!(y == getStartY())) {
						double length = getEndX() - getStartX();

						ConnectionLine newLine = new ConnectionLine(parentPane, parent, secondEnd, getEndX(),
								getStartY(), true);
						ConnectionLine vertLine = new ConnectionLine(parentPane, parent, newLine,
								getStartX() + length / 2, getEndY(), false);
						newLine.setCoordinatesLine(vertLine, getStartX() + length / 2, getEndY());
						vertLine.setCoordinatesLine(this, getStartX() + length / 2, y);

						parent.configGroup.getChildren().addAll(newLine, vertLine);
						addLine(newLine);
						addLine(vertLine);

						secondEnd.addLine(newLine);

						secondLine = vertLine;
						secondEnd = null;
						setEndX(getStartX() + length / 2);
					}
				} else {
				}

				if (secondLine != null) {
					secondLine.updateCoordinates(this, x, y);
				}

				if (horizontal) {
					setStartX(x);
					setStartY(y);
					setEndY(y);
				} else {
					setStartY(y);
					setStartX(x);
					setEndX(x);
				}
			} else if (secondEnd != null && endpoint.equals(secondEnd)) {
				if (firstEnd != null) {
					if (!(y == getStartY())) {
						double length = getEndX() - getStartX();

						ConnectionLine newLine = new ConnectionLine(parentPane, parent, firstEnd, getStartX(),
								getStartY(), true);
						ConnectionLine vertLine = new ConnectionLine(parentPane, parent, newLine,
								getStartX() + length / 2, getEndY(), false);
						newLine.setCoordinatesLine(vertLine, getStartX() + length / 2, getEndY());
						vertLine.setCoordinatesLine(this, getStartX() + length / 2, y);

						parent.configGroup.getChildren().addAll(newLine, vertLine);
						addLine(newLine);
						addLine(vertLine);

						firstEnd.addLine(newLine);

						firstLine = vertLine;
						firstEnd = null;
						setStartX(getStartX() + length / 2);
					}
				}

				if (firstLine != null) {
					firstLine.updateCoordinates(this, x, y);
				}

				if (horizontal) {
					setEndX(x);
					setEndY(y);
					setStartY(y);
				} else {
					setEndY(y);
					setEndX(x);
					setStartX(x);
				}
			}

			if (parentPane != null) {
				parentPane.updateMaxCoordinatesOfComponent(this);
			}

			updateDragPane();
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
			updateDragPane();
		}

		public boolean checkCoordinates(double x, double y) {
			if (horizontal) {
				if (y == getEndY()) {
					return true;
				} else {
					if (firstEnd != null) {
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

			if (parentPane != null) {
				parentPane.updateMaxCoordinatesOfComponent(this);
			}
			updateDragPane();
		}

		public void setParent(PluginConnection parent) {
			this.parent = parent;
		}

		@Override
		public double getMaxX() {

			return getStartX() > getEndX() ? getStartX() : getEndX();
		}

		@Override
		public double getMaxY() {

			return getStartY() > getEndY() ? getStartY() : getEndY();
		}

		@Override
		public void registerMaxCoordinatesUpdateListener(PluginConfigGroup coordinatesListener) {

			this.parentPane = parentPane;
		}

		public boolean isUpDown() {

			return getEndY() > getStartY();
		}

		public boolean isLeftRight() {

			return getEndX() > getStartX();
		}

		private void updateCoordinatesInternal(double x, double y) {
			if (firstEnd == null && secondEnd == null) {
				double localX = parentPane.screenToLocal(x, y).getX();
				double localY = parentPane.screenToLocal(x, y).getY();

				if (horizontal) {
					setStartY(localY);
					setEndY(localY);
				} else {
					setStartX(localX);
					setEndX(localX);
				}

				if (firstLine != null) {
					firstLine.updateCoordinates(this, localX, localY);
				}
				if (secondLine != null) {
					secondLine.updateCoordinates(this, localX, localY);
				}

				updateDragPane();
			}
		}

		private void updateDragPane() {

			if (dragPane == null) {
				dragPane = new Pane();
				parentPane.getChildren().add(dragPane);

				dragPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						updateCoordinatesInternal(event.getScreenX(), event.getScreenY());
					}
				});

				dragPane.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						if (parentPane.getWorkCon() != null && !parentPane.getWorkCon().equals(parent)
								&& parentPane.getWorkCon().getActLine().isHorizontal() != horizontal
								&& !(parent.hasInput() && parentPane.getWorkCon().hasInput())) {
							parentPane.setLineHovered(true);
							setStroke(Color.RED);
							hovered = true;
						}
					}
				});

				dragPane.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						parentPane.setLineHovered(false);
						hovered = false;
						setStroke(Color.BLACK);
					}
				});

				dragPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						addLineDevider(event.getScreenX(), event.getScreenY());
					}

				});
			}

			double layoutX = getStartX() < getEndX() ? getStartX() : getEndX();
			double layoutY = getStartY() < getEndY() ? getStartY() : getEndY();
			double width = horizontal ? Math.abs(getStartX() - getEndX()) - 6 : 6;
			double height = !horizontal ? Math.abs(getStartY() - getEndY()) - 6 : 6;

			if (horizontal) {
				dragPane.setLayoutX(layoutX + 3);
				dragPane.setLayoutY(layoutY - 3);
			} else {
				dragPane.setLayoutX(layoutX - 3);
				dragPane.setLayoutY(layoutY + 3);
			}

			dragPane.setPrefSize(width, height);
		}

		private void addLineDevider(double x, double y) {

			double localX = screenToLocal(x, y).getX();
			double localY = screenToLocal(x, y).getY();

			if (hovered) {
				if (horizontal) {
					ConnectionLine leftLine;
					ConnectionLine rightLine;
					ConnectionLine verticalLine;
					LineDevider devider = new LineDevider(parentPane, parent, null, null, null, null, localX,
							getStartY());
					addDevider(devider);

					if (getEndX() > getStartX()) {
						// Left to right
						rightLine = new ConnectionLine(parentPane, parent, devider, localX, getEndY(), horizontal);
						if (secondLine != null) {
							rightLine.setCoordinatesLine(secondLine, getEndX(), getEndY());
							if (this.equals(secondLine.firstLine)) {
								secondLine.firstLine = rightLine;
							} else {
								secondLine.secondLine = rightLine;
							}
						} else {
							rightLine.setCoordinatesFinal(secondEnd, getEndX(), getEndY());
							secondEnd.addLine(rightLine);
						}
						leftLine = this;
						setEndX(localX);
						secondLine = null;
						secondEnd = devider;

						parentPane.getChildren().add(rightLine);
					} else {
						// Right to left
						leftLine = new ConnectionLine(parentPane, parent, devider, localX, getEndY(), horizontal);
						if (secondLine != null) {
							leftLine.setCoordinatesLine(secondLine, getEndX(), getEndY());
							if (this.equals(secondLine.firstLine)) {
								secondLine.firstLine = leftLine;
							} else {
								secondLine.secondLine = leftLine;
							}
						} else {
							leftLine.setCoordinatesFinal(secondEnd, getEndX(), getEndY());
							secondEnd.addLine(leftLine);
						}
						rightLine = this;
						setEndX(localX);
						secondLine = null;
						secondEnd = devider;

						parentPane.getChildren().add(leftLine);
					}

					verticalLine = parentPane.getWorkCon().getActLine();
					verticalLine.setCoordinatesFinal(devider, localX, getEndY());

					if (verticalLine.isUpDown()) {
						devider.addLineWithPos(LineDeviderPosition.NORTH, verticalLine);
					} else {
						devider.addLineWithPos(LineDeviderPosition.SOUTH, verticalLine);
					}

					devider.addLineWithPos(LineDeviderPosition.WEST, leftLine);
					devider.addLineWithPos(LineDeviderPosition.EAST, rightLine);

					addLine(leftLine);
					addLine(rightLine);
					addLine(verticalLine);

					for (ConnectionLine workLine : parentPane.getWorkCon().lines) {
						addLine(workLine);
						workLine.parent = parent;
					}

					parentPane.finalizeDrawing();
				} else {
					ConnectionLine upperLine;
					ConnectionLine lowerLine;
					ConnectionLine horizontalLine;
					LineDevider devider = new LineDevider(parentPane, parent, null, null, null, null, getStartX(),
							localY);
					addDevider(devider);

					if (getEndY() > getStartY()) {
						// Top to bottom
						lowerLine = new ConnectionLine(parentPane, parent, devider, getEndX(), localY, horizontal);
						if (secondLine != null) {
							lowerLine.setCoordinatesLine(secondLine, getEndX(), getEndY());
							if (this.equals(secondLine.firstLine)) {
								secondLine.firstLine = lowerLine;
							} else {
								secondLine.secondLine = lowerLine;
							}
						} else {
							lowerLine.setCoordinatesFinal(secondEnd, getEndX(), getEndY());
							secondEnd.addLine(lowerLine);
						}
						upperLine = this;
						setEndY(localY);
						secondLine = null;
						secondEnd = devider;

						parentPane.getChildren().add(lowerLine);
					} else {
						// Bottom to top
						upperLine = new ConnectionLine(parentPane, parent, devider, getEndX(), localY, horizontal);
						if (secondLine != null) {
							upperLine.setCoordinatesLine(secondLine, getEndX(), getEndY());
							if (this.equals(secondLine.firstLine)) {
								secondLine.firstLine = upperLine;
							} else {
								secondLine.secondLine = upperLine;
							}
						} else {
							upperLine.setCoordinatesFinal(secondEnd, getEndX(), getEndY());
							secondEnd.addLine(upperLine);
						}
						lowerLine = this;
						setEndY(localY);
						secondLine = null;
						secondEnd = devider;

						parentPane.getChildren().add(upperLine);
					}

					horizontalLine = parentPane.getWorkCon().getActLine();
					horizontalLine.setCoordinatesFinal(devider, getEndX(), localY);

					if (parentPane.getWorkCon().getActLine().isLeftRight()) {
						devider.addLineWithPos(LineDeviderPosition.WEST, horizontalLine);
					} else {
						devider.addLineWithPos(LineDeviderPosition.EAST, horizontalLine);
					}

					devider.addLineWithPos(LineDeviderPosition.NORTH, upperLine);
					devider.addLineWithPos(LineDeviderPosition.SOUTH, lowerLine);

					addLine(upperLine);
					addLine(lowerLine);
					addLine(horizontalLine);

					for (ConnectionLine workLine : parentPane.getWorkCon().lines) {
						addLine(workLine);
						workLine.parent = parent;
					}

					parentPane.finalizeDrawing();
				}
			}
		}

	}

	private class LineDevider extends Circle implements ConnectionLineEndpointInterface {

		private HashMap<LineDeviderPosition, ConnectionLine> connectionLines = new HashMap<>();

		private static final double DIAMETER = 10;

		private PluginConnection parent;
		private PluginConfigGroup parentPane;

		private boolean hovered = false;
		
		private Pane dragPane;

		public LineDevider(PluginConfigGroup parentPane, PluginConnection parent, ConnectionLine north,
				ConnectionLine east, ConnectionLine south, ConnectionLine west, double x, double y) {
			this.parentPane = parentPane;
			this.parent = parent;
			addConnectionInternally(north, LineDeviderPosition.NORTH);
			addConnectionInternally(east, LineDeviderPosition.EAST);
			addConnectionInternally(south, LineDeviderPosition.SOUTH);
			addConnectionInternally(west, LineDeviderPosition.WEST);

			setCenterX(x);
			setCenterY(y);
			setRadius(DIAMETER / 2);

			parentPane.getChildren().addAll(this);

			updateDragPane();
		}

		private void updateCoordinatesInternal(double screenX, double screenY) {
			double localX = parentPane.screenToLocal(screenX, screenY).getX();
			double localY = parentPane.screenToLocal(screenX, screenY).getY();

			setCenterX(localX);
			setCenterY(localY);

			updateDragPane();

			for (ConnectionLine line : connectionLines.values()) {
				line.updateCoordinates(this, localX, localY);
			}
		}

		private void updateDragPane() {
			if (dragPane == null) {
				dragPane = new Pane();
				parentPane.getChildren().add(dragPane);
				dragPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						updateCoordinatesInternal(event.getScreenX(), event.getScreenY());
					}

				});
				
				dragPane.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						
						if(parentPane.getWorkCon() != null) {
							
							ConnectionLine workingLine = parentPane.getWorkCon().getActLine();
							
							if(workingLine != null) {
								if(workingLine.isHorizontal()) {
									if(workingLine.isLeftRight() && connectionLines.containsKey(LineDeviderPosition.WEST)) {
										return;
									} else if(connectionLines.containsKey(LineDeviderPosition.EAST)) {
										return;
									}
								} else {
									if(workingLine.isUpDown() && connectionLines.containsKey(LineDeviderPosition.NORTH)) {
										return;
									} else if(connectionLines.containsKey(LineDeviderPosition.SOUTH)) {
										return;
									}
								}
								
								hovered = true;
								parentPane.setLineHovered(true);
								setFill(Color.RED);							
							}
						}
					}
				});
				
				dragPane.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						hovered = false;
						parentPane.setLineHovered(false);
						setFill(Color.BLACK);
					}
				});
				
				dragPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						
						addExternalLine();
						
					}
					
				});
			}

			dragPane.setPrefSize(DIAMETER, DIAMETER);
			dragPane.setLayoutX(getCenterX() - DIAMETER / 2);
			dragPane.setLayoutY(getCenterY() - DIAMETER / 2);
		}
		
		private void addExternalLine() {
			if(hovered) {
				ConnectionLine workingLine = parentPane.getWorkCon().getActLine();
				
				if(!workingLine.isHorizontal()) {
					if(workingLine.isUpDown()) {
						connectionLines.put(LineDeviderPosition.NORTH, workingLine);
					} else {
						connectionLines.put(LineDeviderPosition.SOUTH, workingLine);
					}									
				} else {
					if(workingLine.isLeftRight()) {
						connectionLines.put(LineDeviderPosition.WEST, workingLine);
					} else {
						connectionLines.put(LineDeviderPosition.EAST, workingLine);
					}
				}
				
				workingLine.setCoordinatesFinal(this, getCenterX(), getCenterY());
				
				for(ConnectionLine workLine : parentPane.getWorkCon().lines) {
					addLine(workLine);
					workLine.parent = parent;
				}
				
				for(LineDevider devider : parentPane.getWorkCon().deviders) {
					addDevider(devider);
					devider.parent = parent;
				}
				
				parentPane.finalizeDrawing();
			}
		}

		public void setParent(PluginConnection parent) {
			this.parent = parent;
		}

		private void addConnectionInternally(ConnectionLine line, LineDeviderPosition position) {
			if (line != null) {
				connectionLines.put(position, line);
			}
		}

		private void removeConnection(ConnectionLine line) {
			LineDeviderPosition removePos = null;

			for (LineDeviderPosition pos : connectionLines.keySet()) {
				if (connectionLines.get(pos).equals(line)) {
					removePos = pos;
					break;
				}
			}

			if (removePos != null) {
				connectionLines.remove(removePos);
			}
		}

		@Override
		public boolean setCoordinates(ConnectionLine line, double x, double y) {
			// Can't move this object from outside -> so we return false
			return false;
		}

		@Override
		public void addLine(ConnectionLine line) {
			// TODO Auto-generated method stub

		}

		public void addLineWithPos(LineDeviderPosition pos, ConnectionLine line) {

			connectionLines.put(pos, line);
			line.updateCoordinates(this, getCenterX(), getCenterY());
		}
	}

	private enum LineDeviderPosition {
		NORTH, EAST, SOUTH, WEST
	}

}
