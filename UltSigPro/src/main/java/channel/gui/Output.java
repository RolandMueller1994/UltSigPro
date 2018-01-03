package channel.gui;

import java.util.HashSet;
import java.util.LinkedList;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import plugins.sigproplugins.SigproPlugin;

public class Output extends Pane implements ConnectionLineEndpointInterface {

	private static final int yOffset = 25;
	private static final int height = yOffset + 10;

	int position;
	int parentWidth;
	int parentHeight;
	int number;
	double outputOffset;
	
	private double conPosX;
	private double conPosY;

	private String name;
	private Label nameLabel;
	private int nameWidth;
	private int width;
	
	private PluginConfigGroup configGroup;
	
	private SigproPlugin plugin;
	
	private Output thisOutput;
	
	private PluginConnection con;
	
	private boolean hovered = false;

	private LinkedList<Line> lines = new LinkedList<>();

	@SuppressWarnings("restriction")
	public Output(SigproPlugin plugin, PluginConfigGroup configGroup, String name, double parentX, double parentY, int position, int number, int parentWidth, int parentHeight,
			double outputOffset) {
		super();
		this.configGroup = configGroup;
		this.name = name;
		this.position = position;
		this.number = number;
		this.parentWidth = parentWidth;
		this.parentHeight = parentHeight;
		this.outputOffset = outputOffset;
		this.plugin = plugin;
		
		thisOutput = this;

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

		line = new Line(0, yOffset, 10, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		line = new Line(10, yOffset, 17, yOffset - 7);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		line = new Line(10, yOffset, 17, yOffset + 7);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);

		nameLabel.setLayoutX(10);
		nameLabel.setLayoutY(0);

		addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				if(con == null) {
					System.out.println("Con null on mouse entered");
				}
				
				if(con == null && (configGroup.getWorkCon() == null || !configGroup.getWorkCon().hasInput())) {
					
					boolean recursivity = false;
					
					if(configGroup.getWorkCon() != null) {
						HashSet<Input> inputs = plugin.getInputs();
						
						HashSet<Input> conLineInputs = configGroup.getWorkCon().getInputs();
						
						for(Input conLineInput : conLineInputs) {
							if(inputs.contains(conLineInput)) {
								recursivity = true;
								break;
							}
						}
						
						if(!recursivity) {
							HashSet<ConnectionLineEndpointInterface> endpoints = new HashSet<>();
							
							for(Input conLineInput : conLineInputs) {
								endpoints.add(conLineInput);
							}
							
							for(Input input : inputs) {
								if(input.getConnection() != null) {
									if(input.getConnection().checkRekusivity(endpoints, true)) {
										recursivity = true;
										break;
									}
								}
							}
						}
					}
					
					if(!recursivity) {
						for (Line line : lines) {
							line.setStyle("-fx-stroke: -usp-light-blue");
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
					line.setStyle("-fx-stroke: black");
				}
				hovered = false;
			}
		});
		
		addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				if(hovered) {
					configGroup.connectionStartStop(thisOutput, conPosX, conPosY);					
				}
			}
			
		});

		updatePosition(parentX, parentY);
	}

	public String getName() {
		return name;
	}
	
	public SigproPlugin getPlugin() {
		return plugin;
	}
	
	public PluginConnection getConnection() {
		return con;
	}
	
	public static double getHeightOfOutput() {
		return height;
	}
	
	private Point2D calculatePositionFromParent(double parentX, double parentY) {
		
		double xPosition = parentX + parentWidth;
		double yPosition = parentY + parentHeight/2 + position * outputOffset - number/2 * outputOffset - yOffset;
		
		return new Point2D(xPosition, yPosition);
	}

	public void updatePosition(double parentX, double parentY) {

		Point2D calculated = calculatePositionFromParent(parentX, parentY);
		
		double xPosition = calculated.getX();
		setLayoutX(xPosition);
		conPosX = xPosition + 17;

		double yPosition = calculated.getY();
		setLayoutY(yPosition);
		conPosY = yPosition + yOffset;
		
		if(con != null) {
			con.dragNDrop(this, conPosX, conPosY);
		}
	}
	
	public boolean checkUpdatePosition(double parentX, double parentY) {
		
		if(con != null) {
			Point2D calculated = calculatePositionFromParent(parentX, parentY);
			return con.checkDragNDrop(this, calculated.getX() + 17, calculated.getY() + yOffset);
		}
		
		return true;
	}

	@Override
	public boolean setCoordinates(PluginConnection line, double x, double y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addConnection(PluginConnection con) {
		 
		this.con = con;
	}
	
	public boolean isHovered() {
		return hovered;
	}

	@Override
	public void removeConnection(PluginConnection con) {
		
		if(con.equals(this.con)) {
			this.con = null;
		}
		
	}
	
	@Override
	public void replaceConnection(PluginConnection origin, PluginConnection replace) {
		
		if(con.equals(origin)) {
			this.con = replace;
		}
		
	}
	
	public void delete() {
		configGroup.getChildren().remove(this);
		// TODO
		/*if(conLine != null) {
			conLine.delete();
		}*/
	}
}
