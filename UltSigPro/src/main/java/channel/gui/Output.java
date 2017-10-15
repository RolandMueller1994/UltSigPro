package channel.gui;

import java.util.LinkedList;

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

public class Output extends Pane implements ConnectionLineEndpointInterface {

	private static final int yOffset = 25;
	private static final int height = yOffset + 10;

	int position;
	int parentWidth;
	int parentHeight;
	double outputOffset;
	
	private double conPosX;
	private double conPosY;

	private String name;
	private Label nameLabel;
	private int nameWidth;
	private int width;
	
	private PluginConfigGroup configGroup;
	
	private Output thisOutput;
	
	private ConnectionLine line;
	
	private boolean hovered = false;

	private LinkedList<Line> lines = new LinkedList<>();

	@SuppressWarnings("restriction")
	public Output(PluginConfigGroup configGroup, String name, double parentX, double parentY, int position, int parentWidth, int parentHeight,
			double outputOffset) {
		super();
		this.configGroup = configGroup;
		this.name = name;
		this.position = position;
		this.parentWidth = parentWidth;
		this.parentHeight = parentHeight;
		this.outputOffset = outputOffset;
		
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

				for (Line line : lines) {
					line.setStroke(Color.RED);
				}
				hovered = true;					
			
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
					configGroup.connectionStartStop(thisOutput, conPosX, conPosY);					
				}
			}
			
		});

		updatePosition(parentX, parentY);
	}

	public static double getHeightOfOutput() {
		return height;
	}

	public void updatePosition(double parentX, double parentY) {

		double xPosition = parentX + parentWidth;
		setLayoutX(xPosition);
		conPosX = xPosition + 17;

		double yPosition = parentY + position * outputOffset + outputOffset / 2 - yOffset;
		setLayoutY(yPosition);
		conPosY = yPosition + yOffset;
		
		if(line != null) {			
			line.updateCoordinates(this, conPosX, conPosY);
		}
	}

	@Override
	public boolean setCoordinates(ConnectionLine line, double x, double y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addLine(ConnectionLine line) {
		
		this.line = line;
	}
	
	public boolean isHovered() {
		return hovered;
	}
}
