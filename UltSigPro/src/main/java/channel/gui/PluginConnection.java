package channel.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import channel.PluginInput;
import channel.PluginOutput;
import channel.gui.PluginConnection.ConnectionLine;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import plugins.sigproplugins.SigproPlugin;

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

	private HashSet<ConnectionLine> lines = new HashSet<>();
	private HashSet<LineDivider> dividers = new HashSet<>();

	private ConnectionLine actLine = null;
	private boolean actHorizontal = true;

	private boolean hovered = false;

	private PluginConfigGroup configGroup;

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
		actLine = new ConnectionLine(configGroup, this, endpoint, startX, startY, true);

		addLine(actLine);

		configGroup.getChildren().add(actLine);
	}

	public PluginConnection(@Nonnull PluginConfigGroup configGroup) {
		this.configGroup = configGroup;
	}
	
	public double getMaxX() {
		boolean first = true;
		double maxX = 0;
		
		for(ConnectionLine line : lines) {
			double max = line.getStartX() > line.getEndX() ? line.getStartX() : line.getEndX();
			
			if(first) {
				maxX = max;
				first = false;
			} else if(max > maxX) {
				maxX = max;
			}
		}
		
		return maxX;
	}
	
	public double getMaxY() {
		boolean first = true;
		double maxY = 0;
		
		for(ConnectionLine line : lines) {
			double max = line.getStartY() > line.getEndY() ? line.getStartY() : line.getEndY();
			
			if(first) {
				maxY = max;
				first = false;
			} else if(max > maxY) {
				maxY = max;
			}
		}
		
		return maxY;
	}
	
	public double getMinX() {
		boolean first = true;
		double minX = 0;
		
		for(ConnectionLine line : lines) {
			double min = line.getStartX() < line.getEndX() ? line.getStartX() : line.getEndX();
			
			if(first) {
				minX = min;
				first = false;
			} else if(min > minX) {
				minX = min;
			}
		}
		
		return minX;
	}

	public double getMinY() {
		boolean first = true;
		double minY = 0;
		
		for(ConnectionLine line : lines) {
			double min = line.getStartY() < line.getEndY() ? line.getStartY() : line.getEndY();
			
			if(first) {
				minY = min;
				first = false;
			} else if(min > minY) {
				minY = min;
			}
		}
		
		return minY;
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

		return false;
	}

	public void collectConnectionLineInfos(Document doc, Element element) {

		Element lineCount = doc.createElement("lineCount");
		lineCount.appendChild(doc.createTextNode(new Integer(lines.size()).toString()));
		element.appendChild(lineCount);

		Element dividerCount = doc.createElement("dividerCount");
		dividerCount.appendChild(doc.createTextNode(new Integer(dividers.size()).toString()));
		element.appendChild(dividerCount);

		int count = 0;
		for (ConnectionLine line : lines) {
			line.setNumber(count);
			count++;
		}

		count = 0;
		for (LineDivider divider : dividers) {
			divider.setNumber(count);
		}

		for (ConnectionLine line : lines) {
			line.collectedLineInfo(doc, element);
		}

		for (LineDivider divider : dividers) {
			divider.collectedDividerInfo(doc, element);
		}
	}

	public void setConnectionLinesConfig(NodeList nodeList) {
		
		int lineCount;
		int dividerCount;
		LinkedList<ConnectionLine> newLines = new LinkedList<>();
		LinkedList<LineDivider> newDividers = new LinkedList<>();

		for(int i=0; i<nodeList.getLength(); i++) {
			Node conNode = nodeList.item(i);
			
			if(conNode.getNodeType() == Node.ELEMENT_NODE) {
				Element conElement = (Element) conNode;
				String tagName = conElement.getTagName();
				
				if(tagName.equals("lineCount")) {
					lineCount = new Integer(conElement.getTextContent()).intValue();
				} else if (tagName.equals("dividerCount")) {
					dividerCount = new Integer(conElement.getTextContent()).intValue();
				} else if (tagName.equals("connectionLine")) {
					NodeList conLineNodes = conElement.getChildNodes();
					ConnectionLine conLine = new ConnectionLine(configGroup, this, conLineNodes);
					newLines.add(conLine);
				} else if (tagName.equals("divider")) {
					NodeList dividerNodes = conElement.getChildNodes();
					LineDivider divider = new LineDivider(configGroup, this, dividerNodes);
					newDividers.add(divider);
				}			
			}
		}
		
		HashSet<SigproPlugin> plugins = configGroup.getPlugins();
		
		for(ConnectionLine line : newLines) {
			line.finalizeXMLConfig(plugins, newLines, newDividers);
			addLine(line);
			configGroup.getChildren().add(line);
		}
		
		for(LineDivider divider : newDividers) {
			divider.finalizeXMLConfig(plugins, newLines, newDividers);
			addDevider(divider);
		}
	}

	private void unifyConnectionsDevider() {

		HashSet<ConnectionLine> removeLines = new HashSet<>();

		for (ConnectionLine line : lines) {

			if (!removeLines.contains(line)) {
				if (line.firstLine != null && line.horizontal == line.firstLine.horizontal) {
					removeLines.add(line.firstLine);

					if (line.equals(line.firstLine.firstLine)) {
						// We are the first line of the line to delete
						line.setStartX(line.firstLine.getEndX());
						line.setStartY(line.firstLine.getEndY());
						// Now we are at the right coordinates

						if (line.firstLine.secondLine != null) {
							// There's another line
							ConnectionLine nextLine = line.firstLine.secondLine;
							if (line.firstLine.equals(nextLine.firstLine)) {
								// We are the firstLine of the nextLine
								nextLine.firstLine = line;
							} else {
								// We are the secondLIne of the nextLine
								nextLine.secondLine = line;
							}

							// Our first line will be nextLine
							line.firstLine = nextLine;
						} else {
							// There has to be a endpoint at our firstLine ->
							// this will be our endpoint
							line.firstEnd = line.firstLine.secondEnd;
							line.firstEnd.replaceLine(line.firstLine, line);
							line.firstLine = null;
						}

						line.updateDragPane();
					} else if (line.equals(line.firstLine.secondLine)) {
						// We are the second line of the line to delete
						line.setStartX(line.firstLine.getStartX());
						line.setStartY(line.firstLine.getStartY());
						// Now we are at the right coordinates

						if (line.firstLine.firstLine != null) {
							// There's another line
							ConnectionLine nextLine = line.firstLine.firstLine;
							if (line.firstLine.equals(nextLine.firstLine)) {
								// We are the firstLine of the nextLine
								nextLine.firstLine = line;
							} else {
								// We are the secondLine of the nextLine
								nextLine.secondLine = line;
							}

							// Our first line will be nextLine
							line.firstLine = nextLine;
						} else {

							line.firstEnd = line.firstLine.firstEnd;
							line.firstEnd.replaceLine(line.firstLine, line);
							line.firstLine = null;
						}

						line.updateDragPane();
					}
				} else if (line.secondLine != null && line.horizontal == line.secondLine.horizontal) {
					removeLines.add(line.secondLine);

					if (line.equals(line.secondLine.firstLine)) {
						// We are the first line of the line to delete
						line.setEndX(line.secondLine.getEndX());
						line.setEndY(line.secondLine.getEndY());
						// Now we are at the right coordinates

						if (line.secondLine.secondLine != null) {
							// There's another line
							ConnectionLine nextLine = line.secondLine.secondLine;
							if (line.secondLine.equals(nextLine.firstLine)) {
								// We are the firstLine of the nextLine
								nextLine.firstLine = line;
							} else {
								// We are the secondLine of the firstLine
								nextLine.secondLine = line;
							}

							// Our first line will be nextLine
							line.secondLine = nextLine;
						} else {
							// There has to be a endpoint at our secondLine ->
							// this will be our endpoint
							line.secondEnd = line.secondLine.secondEnd;
							line.secondEnd.replaceLine(line.secondLine, line);
							line.secondLine = null;
						}

						// There has to be a endpoint at our firstLine -> this
						// will be our endpoint
						line.updateDragPane();
					} else if (line.equals(line.secondLine.secondLine)) {
						// We are the second line of the line to delete
						line.setEndX(line.secondLine.getStartX());
						line.setEndY(line.secondLine.getStartY());
						// Now we are at the right coordinates

						if (line.secondLine.firstLine != null) {
							// There's another line
							ConnectionLine nextLine = line.secondLine.firstLine;
							if (line.secondLine.equals(nextLine.firstLine)) {
								// We are the firstLine of the nextLine
								nextLine.firstLine = line;
							} else {
								// We are the secondLine of the nextLine
								nextLine.secondLine = line;
							}

							// Our second line will be nextLine
							line.secondLine = nextLine;
						} else {
							// There has to be a endpoint at our secondLine ->
							// this will be our endpoint
							line.secondEnd = line.secondLine.firstEnd;
							line.secondEnd.replaceLine(line.secondLine, line);
							line.secondLine = null;
						}

						line.updateDragPane();
					}
				}
			}
		}

		for (ConnectionLine delete : removeLines) {
			lines.remove(delete);
			configGroup.getChildren().remove(delete);
			configGroup.getChildren().remove(delete.dragPane);
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
		// Change the current orientation flag and finalize the current line.
		actHorizontal = !actHorizontal;
		actLine.setCoordinatesLine(actLine, x, y);

		// Create new lines
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

	/**
	 * Return the current line which is used for drawing and hasn't been
	 * finalized.
	 * 
	 * @return The current {@link ConnectionLine}
	 */
	public ConnectionLine getActLine() {
		return actLine;
	}

	private void addLine(ConnectionLine line) {

		lines.add(line);
		line.registerMaxCoordinatesUpdateListener(configGroup);
	}

	/**
	 * Proves if this connection has already got a source for date.
	 * 
	 * @return True if there is already a input. Else false.
	 */
	public boolean hasInput() {

		for (ConnectionLine actLine : lines) {
			if (actLine.getFirstEnd() != null && actLine.getFirstEnd() instanceof Output) {
				return true;
			}

			if (actLine.getSecondEnd() != null && actLine.getSecondEnd() instanceof Output) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Collects all {@link Input}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Input}s
	 */
	public HashSet<Input> getInputs() {

		HashSet<Input> inputs = new HashSet<>();

		for (ConnectionLine line : lines) {
			if (line.firstEnd != null && line.firstEnd instanceof Input) {
				inputs.add((Input) line.firstEnd);
			}

			if (line.secondEnd != null && line.secondEnd instanceof Input) {
				inputs.add((Input) line.secondEnd);
			}
		}

		return inputs;
	}

	/**
	 * Collects all {@link Output}s from this connection.
	 * 
	 * @return a {@link HashSet} of {@link Output}s
	 */
	public HashSet<Output> getOutputs() {

		HashSet<Output> outputs = new HashSet<>();

		for (ConnectionLine line : lines) {
			if (line.firstEnd != null && line.firstEnd instanceof Output) {
				outputs.add((Output) line.firstEnd);
			}

			if (line.secondEnd != null && line.secondEnd instanceof Output) {
				outputs.add((Output) line.secondEnd);
			}
		}

		return outputs;
	}

	private void addDevider(LineDivider divider) {
		dividers.add(divider);
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
		addLine(actLine);
		actLine.setCoordinatesFinal(endpoint, xCoord, yCoord);
		actLine = null;
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
		actLine.setCoordinates(xCoord, yCoord);
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
		
		double raster = configGroup.getRaster();
		
		if (actHorizontal) {
			double stepX = actLine.getStartX() + (x - actLine.getStartX()) / 2;
			double stepY = actLine.getStartY();
			
			stepX = Math.round(stepX/raster) * raster;

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
		private boolean hoveredForDeletion = false;
		
		private NodeList xmlConfig;

		private PluginConnection parent;

		private int number;

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

		public ConnectionLine(PluginConfigGroup parentPane, PluginConnection parent, NodeList xmlConfig) {
			this.parent = parent;
			this.parentPane = parentPane;
			this.xmlConfig = xmlConfig;
			
			for(int i=0; i<xmlConfig.getLength(); i++) {
				Node xmlNode = xmlConfig.item(i);
				if(xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					Element xmlElement = (Element) xmlNode;
					String tagName = xmlElement.getTagName();
					
					if(tagName.equals("number")) {
						number = new Integer(xmlElement.getTextContent()).intValue();
					} else if (tagName.equals("horizontal")) {
						String textContent = xmlElement.getTextContent();
						
						if(textContent.equals("true")) {
							horizontal = true;
						} else {
							horizontal = false;
						}
					} else if (tagName.equals("startCoords")) {
						NodeList coordsNodes = xmlElement.getChildNodes();
						
						for(int j=0; j<coordsNodes.getLength(); j++) {
							Node coordNode = coordsNodes.item(j);
							if(coordNode.getNodeType() == Node.ELEMENT_NODE) {
								Element coordElement = (Element) coordNode;
								String coordTag = coordElement.getTagName();
								
								if(coordTag.equals("startX")) {
									setStartX(new Double(coordElement.getTextContent()));
								} else if(coordTag.equals("startY")) {
									setStartY(new Double(coordElement.getTextContent()));
								}
							}
						}
					} else if (tagName.equals("endCoords")) {
						NodeList coordsNodes = xmlElement.getChildNodes();
						
						for(int j=0; j<coordsNodes.getLength(); j++) {
							Node coordNode = coordsNodes.item(j);
							if(coordNode.getNodeType() == Node.ELEMENT_NODE) {
								Element coordElement = (Element) coordNode;
								String coordTag = coordElement.getTagName();
								
								if(coordTag.equals("endX")) {
									setEndX(new Double(coordElement.getTextContent()));
								} else if(coordTag.equals("endY")) {
									setEndY(new Double(coordElement.getTextContent()));
								}
							}
						}
					}
				}
			}
		}

		public void finalizeXMLConfig(HashSet<SigproPlugin> plugins, LinkedList<ConnectionLine> lines, LinkedList<LineDivider> dividers) {
			
			for(int i=0; i<xmlConfig.getLength(); i++) {
				Node configNode = xmlConfig.item(i);
				
				if(configNode.getNodeType() == Node.ELEMENT_NODE) {
					Element configElement = (Element) configNode;
					String tagName = configElement.getTagName();
					
					if(tagName.equals("firstLine")) {
						int firstLineNumber = new Integer(configElement.getTextContent());
						
						for(ConnectionLine line : lines) {
							if(line.getNumber() == firstLineNumber) {
								firstLine = line;
								break;
							}
						}
					} else if(tagName.equals("secondLine")) {
						int secondLineNumber = new Integer(configElement.getTextContent());
						
						for(ConnectionLine line : lines) {
							if(line.getNumber() == secondLineNumber) {
								secondLine = line;
								break;
							}
						}
					} else if(tagName.equals("firstEnd")) {
						NodeList firstEndNodes = configElement.getChildNodes();
						
						for(int j=0; j<firstEndNodes.getLength(); j++) {
							Node firstEndNode = firstEndNodes.item(j);
							
							if(firstEndNode.getNodeType() == Node.ELEMENT_NODE) {
								Element firstEndElement = (Element) firstEndNode;
								String firstEndTagName = firstEndElement.getTagName();
								
								if(firstEndTagName.equals("divider")) {
									NodeList dividerNodes = firstEndElement.getChildNodes();
									
									for(int a=0; a<dividerNodes.getLength(); a++) {
										Node dividerNode = dividerNodes.item(a);
										
										if(dividerNode.getNodeType() == Node.ELEMENT_NODE) {
											Element dividerElement = (Element) dividerNode;
											String dividerTag = dividerElement.getTagName();
											if(dividerTag.equals("dividerNumber")) {
												int dividerNumber = new Integer(dividerElement.getTextContent());
												
												for(LineDivider divider : dividers) {
													if(divider.getNumber() == dividerNumber) {
														firstEnd = divider;
														break;
													}
												}
												break;
											}
										}
									}
									break;
								} else if(firstEndTagName.equals("pluginInput")) {
									NodeList inputNodes = firstEndElement.getChildNodes();
									String inputName = null;
									int inputPluginNumber = 0;
									for(int a=0; a<inputNodes.getLength(); a++) {
										Node inputNode = inputNodes.item(a);
										
										if(inputNode.getNodeType() == Node.ELEMENT_NODE) {
											Element inputElement = (Element) inputNode;
											String inputTag = inputElement.getTagName();
											if(inputTag.equals("inputName")) {
												inputName = inputElement.getTextContent();
											} else if(inputTag.equals("pluginNumber")) {
												inputPluginNumber = new Integer(inputElement.getTextContent());
											}
										}
									}
									
									for(SigproPlugin plugin : plugins) {
										if(plugin.getNumber() == inputPluginNumber) {
											HashSet<Input> inputs = plugin.getInputs();
											
											for(Input input : inputs) {
												if(input.getName().equals(inputName)) {
													firstEnd = input;
													input.addLine(this);
												}
											}
										}
									}
									
									break;
								} else if(firstEndTagName.equals("pluginOutput")) {
									NodeList outputNodes = firstEndElement.getChildNodes();
									String outputName = null;
									int outputPluginNumber = 0;
									for(int a=0; a<outputNodes.getLength(); a++) {
										Node outputNode = outputNodes.item(a);
										
										if(outputNode.getNodeType() == Node.ELEMENT_NODE) {
											Element outputElement = (Element) outputNode;
											String outputTag = outputElement.getTagName();
											if(outputTag.equals("outputName")) {
												outputName = outputElement.getTextContent();
											} else if(outputTag.equals("pluginNumber")) {
												outputPluginNumber = new Integer(outputElement.getTextContent());
											}
										}
									}
									
									for(SigproPlugin plugin : plugins) {
										if(plugin.getNumber() == outputPluginNumber) {
											HashSet<Output> outputs = plugin.getOutputs();
											
											for(Output output : outputs) {
												if(output.getName().equals(outputName)) {
													firstEnd = output;
													output.addLine(this);
												}
											}
										}
									}
									
									break;
								}
							}
						}
					} else if(tagName.equals("secondEnd")) {
						NodeList secondEndNodes = configElement.getChildNodes();
						
						for(int j=0; j<secondEndNodes.getLength(); j++) {
							Node secondEndNode = secondEndNodes.item(j);
							
							if(secondEndNode.getNodeType() == Node.ELEMENT_NODE) {
								Element secondEndElement = (Element) secondEndNode;
								String secondEndTagName = secondEndElement.getTagName();
								
								if(secondEndTagName.equals("divider")) {
									NodeList dividerNodes = secondEndElement.getChildNodes();
									
									for(int a=0; a<dividerNodes.getLength(); a++) {
										Node dividerNode = dividerNodes.item(a);
										
										if(dividerNode.getNodeType() == Node.ELEMENT_NODE) {
											Element dividerElement = (Element) dividerNode;
											String dividerTag = dividerElement.getTagName();
											if(dividerTag.equals("dividerNumber")) {
												int dividerNumber = new Integer(dividerElement.getTextContent());
												
												for(LineDivider divider : dividers) {
													if(divider.getNumber() == dividerNumber) {
														secondEnd = divider;
														break;
													}
												}
												break;
											}
										}
									}
									break;
								} else if(secondEndTagName.equals("pluginInput")) {
									NodeList inputNodes = secondEndElement.getChildNodes();
									String inputName = null;
									int inputPluginNumber = 0;
									for(int a=0; a<inputNodes.getLength(); a++) {
										Node inputNode = inputNodes.item(a);
										
										if(inputNode.getNodeType() == Node.ELEMENT_NODE) {
											Element inputElement = (Element) inputNode;
											String inputTag = inputElement.getTagName();
											if(inputTag.equals("inputName")) {
												inputName = inputElement.getTextContent();
											} else if(inputTag.equals("pluginNumber")) {
												inputPluginNumber = new Integer(inputElement.getTextContent());
											}
										}
									}
									
									for(SigproPlugin plugin : plugins) {
										if(plugin.getNumber() == inputPluginNumber) {
											HashSet<Input> inputs = plugin.getInputs();
											
											for(Input input : inputs) {
												if(input.getName().equals(inputName)) {
													secondEnd = input;
													input.addLine(this);
												}
											}
										}
									}
									
									break;
								} else if(secondEndTagName.equals("pluginOutput")) {
									NodeList outputNodes = secondEndElement.getChildNodes();
									String outputName = null;
									int outputPluginNumber = 0;
									for(int a=0; a<outputNodes.getLength(); a++) {
										Node outputNode = outputNodes.item(a);
										
										if(outputNode.getNodeType() == Node.ELEMENT_NODE) {
											Element outputElement = (Element) outputNode;
											String outputTag = outputElement.getTagName();
											if(outputTag.equals("outputName")) {
												outputName = outputElement.getTextContent();
											} else if(outputTag.equals("pluginNumber")) {
												outputPluginNumber = new Integer(outputElement.getTextContent());
											}
										}
									}
									
									for(SigproPlugin plugin : plugins) {
										if(plugin.getNumber() == outputPluginNumber) {
											HashSet<Output> outputs = plugin.getOutputs();
											
											for(Output output : outputs) {
												if(output.getName().equals(outputName)) {
													secondEnd = output;
													output.addLine(this);
												}
											}
										}
									}
									
									break;
								}
							}
						}
					}
				}
			}
		}

		public void collectedLineInfo(Document doc, Element element) {
			Element line = doc.createElement("connectionLine");

			Element numberElement = doc.createElement("number");
			numberElement.appendChild(doc.createTextNode(new Integer(number).toString()));
			line.appendChild(numberElement);

			Element horizontalElement = doc.createElement("horizontal");
			horizontalElement.appendChild(doc.createTextNode(new Boolean(horizontal).toString()));
			line.appendChild(horizontalElement);

			Element startCoords = doc.createElement("startCoords");
			Element startX = doc.createElement("startX");
			startX.appendChild(doc.createTextNode(new Double(getStartX()).toString()));
			Element startY = doc.createElement("startY");
			startY.appendChild(doc.createTextNode(new Double(getStartY()).toString()));

			startCoords.appendChild(startX);
			startCoords.appendChild(startY);
			line.appendChild(startCoords);

			Element endCoords = doc.createElement("endCoords");
			Element endX = doc.createElement("endX");
			endX.appendChild(doc.createTextNode(new Double(getEndX()).toString()));
			Element endY = doc.createElement("endY");
			endY.appendChild(doc.createTextNode(new Double(getEndY()).toString()));

			endCoords.appendChild(endX);
			endCoords.appendChild(endY);
			line.appendChild(endCoords);

			if (firstLine != null) {
				Element firstLineElement = doc.createElement("firstLine");
				firstLineElement.appendChild(doc.createTextNode(new Integer(firstLine.getNumber()).toString()));
				line.appendChild(firstLineElement);
			} else if (firstEnd != null) {
				Element firstEndElement = doc.createElement("firstEnd");
				if (firstEnd instanceof LineDivider) {
					Element dividerElement = doc.createElement("divider");
					Element dividerNumberElement = doc.createElement("dividerNumber");
					dividerNumberElement.appendChild(
							doc.createTextNode(new Integer(((LineDivider) firstEnd).getNumber()).toString()));
					dividerElement.appendChild(dividerNumberElement);
					firstEndElement.appendChild(dividerElement);
				} else if (firstEnd instanceof Input) {
					Input input = (Input) firstEnd;
					Element inputElement = doc.createElement("pluginInput");
					Element pluginNumberElement = doc.createElement("pluginNumber");
					pluginNumberElement
							.appendChild(doc.createTextNode(new Integer(input.getPlugin().getNumber()).toString()));
					Element inputNameElement = doc.createElement("inputName");
					inputNameElement.appendChild(doc.createTextNode(input.getName()));
					inputElement.appendChild(inputNameElement);
					inputElement.appendChild(pluginNumberElement);
					firstEndElement.appendChild(inputElement);
				} else if (firstEnd instanceof Output) {
					Output output = (Output) firstEnd;
					Element outputElement = doc.createElement("pluginOutput");
					Element pluginNumberElement = doc.createElement("pluginNumber");
					pluginNumberElement
							.appendChild(doc.createTextNode(new Integer(output.getPlugin().getNumber()).toString()));
					Element outputNameElement = doc.createElement("outputName");
					outputNameElement.appendChild(doc.createTextNode(output.getName()));
					outputElement.appendChild(outputNameElement);
					outputElement.appendChild(pluginNumberElement);
					firstEndElement.appendChild(outputElement);
				}
				line.appendChild(firstEndElement);
			}

			if (secondLine != null) {
				Element secondLineElement = doc.createElement("secondLine");
				secondLineElement.appendChild(doc.createTextNode(new Integer(secondLine.getNumber()).toString()));
				line.appendChild(secondLineElement);
			} else if (secondEnd != null) {
				Element secondEndElement = doc.createElement("secondEnd");
				if (secondEnd instanceof LineDivider) {
					Element dividerElement = doc.createElement("divider");
					Element dividerNumberElement = doc.createElement("dividerNumber");
					dividerNumberElement.appendChild(
							doc.createTextNode(new Integer(((LineDivider) secondEnd).getNumber()).toString()));
					dividerElement.appendChild(dividerNumberElement);
					secondEndElement.appendChild(dividerElement);
				} else if (secondEnd instanceof Input) {
					Input input = (Input) secondEnd;
					Element inputElement = doc.createElement("pluginInput");
					Element pluginNumberElement = doc.createElement("pluginNumber");
					pluginNumberElement
							.appendChild(doc.createTextNode(new Integer(input.getPlugin().getNumber()).toString()));
					Element inputNameElement = doc.createElement("inputName");
					inputNameElement.appendChild(doc.createTextNode(input.getName()));
					inputElement.appendChild(inputNameElement);
					inputElement.appendChild(pluginNumberElement);
					secondEndElement.appendChild(inputElement);
				} else if (secondEnd instanceof Output) {
					Output output = (Output) secondEnd;
					Element outputElement = doc.createElement("pluginOutput");
					Element pluginNumberElement = doc.createElement("pluginNumber");
					pluginNumberElement
							.appendChild(doc.createTextNode(new Integer(output.getPlugin().getNumber()).toString()));
					Element outputNameElement = doc.createElement("outputName");
					outputNameElement.appendChild(doc.createTextNode(output.getName()));
					outputElement.appendChild(outputNameElement);
					outputElement.appendChild(pluginNumberElement);
					secondEndElement.appendChild(outputElement);
				}
				line.appendChild(secondEndElement);
			}

			element.appendChild(line);
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public int getNumber() {
			return number;
		}

		public PluginConnection getParentConnection() {
			return parent;
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
			parentPane.removeDeletionLine();

			lines.remove(this);
			parentPane.getChildren().remove(dragPane);
			dragPane = null;
			parentPane.getChildren().remove(this);
			parentPane = null;

			if (firstEnd != null) {
				firstEnd.removeLine(this);
				firstEnd = null;
			}
			if (secondEnd != null) {
				secondEnd.removeLine(this);
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

						secondEnd.replaceLine(this, newLine);

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

						firstEnd.replaceLine(this, newLine);

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

				double raster = configGroup.getRaster();
				
				localX = Math.round(localX/raster) * raster;
				localY = Math.round(localY/raster) * raster;
				
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

				if (firstLine == secondLine || firstLine == null || secondLine == null) {
					System.out.println("Wrong config");
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

							HashSet<ConnectionLineEndpointInterface> endpoints = new HashSet<>();
							if (!parentPane.getWorkCon().hasInput()) {
								HashSet<Input> inputs = parentPane.getWorkCon().getInputs();

								for (Input input : inputs) {
									endpoints.add(input);
								}
							} else {
								HashSet<Output> outputs = parentPane.getWorkCon().getOutputs();

								for (Output output : outputs) {
									endpoints.add(output);
								}
							}

							if (!checkRekusivity(endpoints, !parentPane.getWorkCon().hasInput())) {
								parentPane.setLineHovered(true);
								setStroke(Color.RED);
								hovered = true;
							}

						}
					}
				});

				dragPane.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						if (!hoveredForDeletion) {
							parentPane.setLineHovered(false);
							hovered = false;
							setStroke(Color.BLACK);
						}
					}
				});

				dragPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {

						if (!addLineDevider(event.getScreenX(), event.getScreenY())) {
							setHoveredForDeletion(null, !hoveredForDeletion);
						}
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

		public void setHoveredForDeletion(ConnectionLine caller, boolean hoverDel) {

			if (parentPane.getWorkCon() == null) {
				if (caller == null) {
					if (!hoveredForDeletion) {
						parentPane.setDeletionLine(this);
					} else {
						parentPane.removeDeletionLine();
					}
				}

				if (firstLine != null && !firstLine.equals(caller)) {
					firstLine.setHoveredForDeletion(this, hoverDel);
				}
				if (secondLine != null && !secondLine.equals(caller)) {
					secondLine.setHoveredForDeletion(this, hoverDel);
				}

				if (hoverDel) {
					setStroke(Color.RED);
					hoveredForDeletion = true;
				} else {
					setStroke(Color.BLACK);
					hoveredForDeletion = false;
				}
			}
		}

		private boolean addLineDevider(double x, double y) {

			double localX = screenToLocal(x, y).getX();
			double localY = screenToLocal(x, y).getY();

			double raster = parentPane.getRaster();
			
			localX = Math.round(localX/raster) * raster;
			localY = Math.round(localY/raster) * raster;
			
			if (hovered) {
				if (horizontal) {
					ConnectionLine leftLine;
					ConnectionLine rightLine;
					ConnectionLine verticalLine;
					LineDivider divider = new LineDivider(parentPane, parent, null, null, null, null, localX,
							getStartY());
					addDevider(divider);

					if (getEndX() > getStartX()) {
						// Left to right
						rightLine = new ConnectionLine(parentPane, parent, divider, localX, getEndY(), horizontal);
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
						secondEnd = divider;

						parentPane.getChildren().add(rightLine);
					} else {
						// Right to left
						leftLine = new ConnectionLine(parentPane, parent, divider, localX, getEndY(), horizontal);
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
						secondEnd = divider;

						parentPane.getChildren().add(leftLine);
					}

					verticalLine = parentPane.getWorkCon().getActLine();
					verticalLine.setCoordinatesFinal(divider, localX, getEndY());

					if (verticalLine.isUpDown()) {
						divider.addLineWithPos(LineDividerPosition.NORTH, verticalLine);
					} else {
						divider.addLineWithPos(LineDividerPosition.SOUTH, verticalLine);
					}

					divider.addLineWithPos(LineDividerPosition.WEST, leftLine);
					divider.addLineWithPos(LineDividerPosition.EAST, rightLine);

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
					LineDivider divider = new LineDivider(parentPane, parent, null, null, null, null, getStartX(),
							localY);
					addDevider(divider);

					if (getEndY() > getStartY()) {
						// Top to bottom
						lowerLine = new ConnectionLine(parentPane, parent, divider, getEndX(), localY, horizontal);
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
						secondEnd = divider;

						parentPane.getChildren().add(lowerLine);
					} else {
						// Bottom to top
						upperLine = new ConnectionLine(parentPane, parent, divider, getEndX(), localY, horizontal);
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
						secondEnd = divider;

						parentPane.getChildren().add(upperLine);
					}

					horizontalLine = parentPane.getWorkCon().getActLine();
					horizontalLine.setCoordinatesFinal(divider, getEndX(), localY);

					if (parentPane.getWorkCon().getActLine().isLeftRight()) {
						divider.addLineWithPos(LineDividerPosition.WEST, horizontalLine);
					} else {
						divider.addLineWithPos(LineDividerPosition.EAST, horizontalLine);
					}

					divider.addLineWithPos(LineDividerPosition.NORTH, upperLine);
					divider.addLineWithPos(LineDividerPosition.SOUTH, lowerLine);

					addLine(upperLine);
					addLine(lowerLine);
					addLine(horizontalLine);

					for (ConnectionLine workLine : parentPane.getWorkCon().lines) {
						addLine(workLine);
						workLine.parent = parent;
					}

					parentPane.finalizeDrawing();
				}
				return true;
			}
			return false;
		}

	}

	class LineDivider extends Circle implements ConnectionLineEndpointInterface {

		private HashMap<LineDividerPosition, ConnectionLine> connectionLines = new HashMap<>();

		private static final double DIAMETER = 10;

		private PluginConnection parent;
		private PluginConfigGroup parentPane;
		private NodeList xmlConfig;

		private boolean hovered = false;

		private Pane dragPane;

		private int number;

		public LineDivider(PluginConfigGroup parentPane, PluginConnection parent, ConnectionLine north,
				ConnectionLine east, ConnectionLine south, ConnectionLine west, double x, double y) {
			this.parentPane = parentPane;
			this.parent = parent;
			addConnectionInternally(north, LineDividerPosition.NORTH);
			addConnectionInternally(east, LineDividerPosition.EAST);
			addConnectionInternally(south, LineDividerPosition.SOUTH);
			addConnectionInternally(west, LineDividerPosition.WEST);

			setCenterX(x);
			setCenterY(y);
			setRadius(DIAMETER / 2);

			parentPane.getChildren().addAll(this);

			updateDragPane();
		}

		public LineDivider(PluginConfigGroup parentPane, PluginConnection parent, NodeList xmlConfig) {
			this.parentPane = parentPane;
			this.parent = parent;
			this.xmlConfig = xmlConfig;
			
			for(int i=0; i<xmlConfig.getLength(); i++) {
				Node xmlNode = xmlConfig.item(i);
				if(xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					Element xmlElement = (Element) xmlNode;
					String tagName = xmlElement.getTagName();
					
					if(tagName.equals("number")) {
						number = new Integer(xmlElement.getTextContent()).intValue();
					} else if (tagName.equals("xCoord")) {
						setCenterX(new Double(xmlElement.getTextContent()));
					} else if (tagName.equals("yCoord")) {
						setCenterY(new Double(xmlElement.getTextContent()));
					}
				}
			}
			
			setRadius(DIAMETER / 2);

			parentPane.getChildren().addAll(this);

			updateDragPane();
		}

		public void finalizeXMLConfig(HashSet<SigproPlugin> plugins, LinkedList<ConnectionLine> lines,
				LinkedList<LineDivider> dividers) {
			
			for(int a=0; a<xmlConfig.getLength(); a++) {
				Node xmlNode = xmlConfig.item(a);
				
				if(xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					Element xmlElement = (Element) xmlNode;
					String tagName = xmlElement.getTagName();
					
					if(tagName.equals("lines")) {
						NodeList lineNodes = xmlElement.getChildNodes();
						
						for(int b=0; b<lineNodes.getLength(); b++) {
							Node lineNode = lineNodes.item(b);
							
							if(lineNode.getNodeType() == Node.ELEMENT_NODE) {
								Element lineElement = (Element) lineNode;
								String lineTag = lineElement.getTagName();
								
								LineDividerPosition pos = null;
								int lineNumber = 0;
								
								if(lineTag.equals("north")) {
									pos = LineDividerPosition.NORTH;
									lineNumber = new Integer(lineElement.getTextContent());
								} else if(lineTag.equals("south")) {
									pos = LineDividerPosition.SOUTH;
									lineNumber = new Integer(lineElement.getTextContent());									
								} else if(lineTag.equals("east")) {
									pos = LineDividerPosition.EAST;
									lineNumber = new Integer(lineElement.getTextContent());
								} else if(lineTag.equals("west")) {
									pos = LineDividerPosition.WEST;
									lineNumber = new Integer(lineElement.getTextContent());
								}
								
								ConnectionLine line = null;
								
								for(ConnectionLine curLine : lines) {
									if(curLine.getNumber() == lineNumber) {
										line = curLine;
										break;
									}
								}
								addLineWithPos(pos, line);
							}
							
						}
						
						break;
					}
				}
			}
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public int getNumber() {
			return number;
		}

		public void collectedDividerInfo(Document doc, Element element) {
			Element dividerElement = doc.createElement("divider");
			Element numberElement = doc.createElement("number");
			numberElement.appendChild(doc.createTextNode(new Integer(number).toString()));
			Element xCoord = doc.createElement("xCoord");
			xCoord.appendChild(doc.createTextNode(new Double(getCenterX()).toString()));
			Element yCoord = doc.createElement("yCoord");
			yCoord.appendChild(doc.createTextNode(new Double(getCenterY()).toString()));

			Element lines = doc.createElement("lines");
			collectLineInfo(doc, lines, LineDividerPosition.EAST);
			collectLineInfo(doc, lines, LineDividerPosition.NORTH);
			collectLineInfo(doc, lines, LineDividerPosition.SOUTH);
			collectLineInfo(doc, lines, LineDividerPosition.WEST);

			dividerElement.appendChild(numberElement);
			dividerElement.appendChild(xCoord);
			dividerElement.appendChild(yCoord);
			dividerElement.appendChild(lines);
			element.appendChild(dividerElement);
		}

		private void collectLineInfo(Document doc, Element element, LineDividerPosition pos) {
			if (connectionLines.containsKey(pos)) {
				String posStr = pos.toString();
				Element lineElement = doc.createElement(posStr);
				lineElement.appendChild(doc.createTextNode(new Integer(connectionLines.get(pos).getNumber()).toString()));
				element.appendChild(lineElement);
			}
		}

		private void updateCoordinatesInternal(double screenX, double screenY) {
			double localX = parentPane.screenToLocal(screenX, screenY).getX();
			double localY = parentPane.screenToLocal(screenX, screenY).getY();

			double raster = parentPane.getRaster();
			
			localX = Math.round(localX/raster) * raster;
			localY = Math.round(localY/raster) * raster;
			
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

						if (parentPane.getWorkCon() != null) {

							ConnectionLine workingLine = parentPane.getWorkCon().getActLine();

							if (workingLine != null) {
								if (workingLine.isHorizontal()) {
									if (workingLine.isLeftRight()
											&& connectionLines.containsKey(LineDividerPosition.WEST)) {
										return;
									} else if (!workingLine.isLeftRight()
											&& connectionLines.containsKey(LineDividerPosition.EAST)) {
										return;
									}
								} else {
									if (workingLine.isUpDown()
											&& connectionLines.containsKey(LineDividerPosition.NORTH)) {
										return;
									} else if (!workingLine.isUpDown()
											&& connectionLines.containsKey(LineDividerPosition.SOUTH)) {
										return;
									}
								}

								HashSet<ConnectionLineEndpointInterface> endpoints = new HashSet<>();
								if (!parentPane.getWorkCon().hasInput()) {
									HashSet<Input> inputs = parentPane.getWorkCon().getInputs();

									for (Input input : inputs) {
										endpoints.add(input);
									}
								} else {
									HashSet<Output> outputs = parentPane.getWorkCon().getOutputs();

									for (Output output : outputs) {
										endpoints.add(output);
									}
								}

								if (!checkRekusivity(endpoints, !parentPane.getWorkCon().hasInput())
										&& !(parent.hasInput() && parentPane.getWorkCon().hasInput())) {
									hovered = true;
									parentPane.setLineHovered(true);
									setFill(Color.RED);
								}
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
			if (hovered) {
				ConnectionLine workingLine = parentPane.getWorkCon().getActLine();

				if (!workingLine.isHorizontal()) {
					if (workingLine.isUpDown()) {
						connectionLines.put(LineDividerPosition.NORTH, workingLine);
					} else {
						connectionLines.put(LineDividerPosition.SOUTH, workingLine);
					}
				} else {
					if (workingLine.isLeftRight()) {
						connectionLines.put(LineDividerPosition.WEST, workingLine);
					} else {
						connectionLines.put(LineDividerPosition.EAST, workingLine);
					}
				}

				workingLine.setCoordinatesFinal(this, getCenterX(), getCenterY());

				for (ConnectionLine workLine : parentPane.getWorkCon().lines) {
					addLine(workLine);
					workLine.parent = parent;
				}

				for (LineDivider divider : parentPane.getWorkCon().dividers) {
					addDevider(divider);
					divider.parent = parent;
				}

				parentPane.finalizeDrawing();
			}
		}

		public void setParent(PluginConnection parent) {
			this.parent = parent;
		}

		private void addConnectionInternally(ConnectionLine line, LineDividerPosition position) {
			if (line != null) {
				connectionLines.put(position, line);
			}
		}

		private void removeConnection(ConnectionLine line) {
			LineDividerPosition removePos = null;

			for (LineDividerPosition pos : connectionLines.keySet()) {
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

			// Nothing to do, won't be called.
		}

		public void addLineWithPos(LineDividerPosition pos, ConnectionLine line) {

			connectionLines.put(pos, line);
			line.updateCoordinates(this, getCenterX(), getCenterY());
		}

		@Override
		public void removeLine(ConnectionLine line) {

			LineDividerPosition deletePos = null;

			for (LineDividerPosition pos : connectionLines.keySet()) {
				if (connectionLines.get(pos).equals(line)) {
					deletePos = pos;
				}
			}

			if (deletePos != null) {
				connectionLines.remove(deletePos);
			}

			if (connectionLines.size() == 2) {
				// Unify to lines
				ConnectionLine firstLine = null;
				ConnectionLine secondLine = null;
				boolean first = true;

				for (ConnectionLine curLine : connectionLines.values()) {
					if (first) {
						firstLine = curLine;
						first = false;
					} else {
						secondLine = curLine;
					}

				}

				if (firstLine != null && secondLine != null) {
					if (this.equals(firstLine.firstEnd)) {
						firstLine.firstLine = secondLine;
						firstLine.firstEnd = null;
					} else {
						firstLine.secondLine = secondLine;
						firstLine.secondEnd = null;
					}

					if (this.equals(secondLine.firstEnd)) {
						secondLine.firstLine = firstLine;
						secondLine.firstEnd = null;
					} else {
						secondLine.secondLine = firstLine;
						secondLine.secondEnd = null;
					}
				}

				parentPane.getChildren().remove(this);
				parentPane.getChildren().remove(dragPane);

				parent.dividers.remove(this);
				parent.unifyConnectionsDevider();

				parentPane = null;
				parent = null;
			}
		}

		@Override
		public void replaceLine(ConnectionLine origin, ConnectionLine replace) {

			for (LineDividerPosition pos : connectionLines.keySet()) {
				if (connectionLines.get(pos).equals(origin)) {
					connectionLines.replace(pos, replace);
					break;
				}
			}
		}
	}

	private enum LineDividerPosition {
		NORTH, EAST, SOUTH, WEST;

		public String toString() {
			if (this.equals(EAST)) {
				return "east";
			} else if (this.equals(WEST)) {
				return "west";
			} else if (this.equals(SOUTH)) {
				return "south";
			} else {
				return "north";
			}
		}
	}

}
