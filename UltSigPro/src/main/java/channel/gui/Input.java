package channel.gui;

import java.util.HashSet;
import java.util.LinkedList;

import javax.annotation.CheckReturnValue;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import channel.gui.PluginConnection.ConnectionLine;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import plugins.sigproplugins.SigproPlugin;

public class Input extends Pane implements ConnectionLineEndpointInterface {

	private static final int yOffset = 25;
	private static final int height = yOffset + 10;

	private int position;
	private int parentWidht;
	private int parentHeight;
	private int number;
	private double inputOffset;

	private String name;
	private Label nameLabel;
	private int nameWidth;
	private int width;
	
	private double conPosX;
	private double conPosY;
	
	private PluginConfigGroup configGroup;
	private Input thisInput;
	
	private SigproPlugin plugin;

	private ConnectionLine conLine;
	
	private boolean hovered = false;
	
	private LinkedList<Line> lines = new LinkedList<>();

	@SuppressWarnings("restriction")
	public Input(SigproPlugin plugin, PluginConfigGroup configGroup, String name, double parentX, double parentY, int position, int number, int parentWidth, int parentHeight,
			double inputOffset) {
		super();
		this.configGroup = configGroup;
		this.name = name;
		this.position = position;
		this.number = number;
		this.parentWidht = parentWidth;
		this.parentHeight = parentHeight;
		this.inputOffset = inputOffset;
		this.plugin = plugin;
		
		thisInput = this;

		nameLabel = new Label(name);

		Font font = nameLabel.getFont();
		FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();

		nameWidth = (int) fontLoader.computeStringWidth(name, font);

		if (nameWidth > 10) {
			setPrefSize(nameWidth + 10, height);
			setMaxSize(nameWidth + 10, height);
			width = nameWidth + 10;
		} else {
			setPrefSize(20, height);
			setMaxSize(20, height);
			width = 20;
		}

		getChildren().add(nameLabel);
		Line line;

		line = new Line(width - 10, yOffset, width, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		line = new Line(width - 17, yOffset - 7, width - 10, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		line = new Line(width - 17, yOffset + 7, width - 10, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		nameLabel.setLayoutX(0);
		nameLabel.setLayoutY(0);

		addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				if(conLine == null) {
					
					boolean recursivity = false;
					
					if(configGroup.getWorkCon() != null) {
						HashSet<Output> outputs = plugin.getOutputs();
						
						HashSet<Output> conLineOutputs = configGroup.getWorkCon().getOutputs();
						
						for(Output conLineOutput : conLineOutputs) {
							if(outputs.contains(conLineOutput)) {
								recursivity = true;
								break;
							}
						}
						
						if(!recursivity) {
							HashSet<ConnectionLineEndpointInterface> endpoints = new HashSet<>();
							
							for(Output conLineOutput : conLineOutputs) {
								endpoints.add(conLineOutput);
							}
							
							for(Output output : outputs) {
								if(output.getLine() != null) {
									if(output.getLine().getParentConnection().checkRekusivity(endpoints, false)) {
										recursivity = true;
										break;
									}
								}
							}
						}
					}
					
					if(!recursivity) {						
						for (Line line : lines) {
							line.setStroke(Color.RED);
						}
						hovered = true;						
					}
				}
			}
		});

		addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				for (Line line : lines) {
					line.setStroke(Color.BLACK);
				}
				hovered = false;
			}
		});
		
		addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				if(hovered) {
					configGroup.connectionStartStop(thisInput, conPosX, conPosY);					
				}
			}
			
		});

		updatePosition(parentX, parentY);
	}

	public SigproPlugin getPlugin() {
		return plugin;
	}
	
	public String getName() {
		return name;
	}
	
	public ConnectionLine getLine() {
		return conLine;
	}
	
	public static double getHeightOfInput() {
		return height;
	}

	public void updatePosition(double parentX, double parentY) {

		double xPosition = parentX - width;
		setLayoutX(xPosition);
		conPosX = xPosition + width - 17;

		double yPosition = parentY + parentHeight/2 + position * inputOffset - number/2 * inputOffset - yOffset;
		setLayoutY(yPosition);
		conPosY = yPosition + yOffset;
		
		if(conLine != null) {
			conLine.updateCoordinates(this, conPosX, conPosY);			
		}
	}

	@Override
	public boolean setCoordinates(ConnectionLine line, double x, double y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addLine(ConnectionLine conLine) {
		
		this.conLine = conLine;
	}
	
	public boolean isHovered() {
		return hovered;
	}

	@Override
	public void removeLine(ConnectionLine line) {
		
		if(conLine.equals(line)) {
			conLine = null;
		}
		
	}

	@Override
	public void replaceLine(ConnectionLine origin, ConnectionLine replace) {
		
		if(conLine.equals(origin)) {
			conLine = replace;
		}
		
	}
	
	public void delete() {
		configGroup.getChildren().remove(this);
		if(conLine != null) {
			conLine.delete();
		}
	}
}
