package channel.gui;

import java.util.LinkedList;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

public class Input extends Pane {

	private final int yOffset = 25;
	private final int height = yOffset + 10;
	
	private String name;
	private Label nameLabel;
	private int nameWidth;
	private int width;
	
	private LinkedList<Line> lines = new LinkedList<>();
	
	public Input(String name) {
		super();
		this.name = name;
		
		nameLabel = new Label(name);
		
		Font font = nameLabel.getFont();
		FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
		
		nameWidth = (int) fontLoader.computeStringWidth(name, font);
		
		if(nameWidth > 20) {
			setPrefSize(nameWidth + 10, height);
			setMaxSize(nameWidth + 10, height);
			width = nameWidth;
		} else {
			setPrefSize(20, height);
			setMaxSize(20, height);
			width = 20;
		}
		
		getChildren().add(nameLabel);
		Line line; 
		
		line = new Line(width-10, yOffset, width, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);
		
		line = new Line(width-17, yOffset-7, width-10, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);
		
		line = new Line(width-17, yOffset+7, width-10, yOffset);
		line.setStrokeWidth(1);
		lines.add(line);
		getChildren().add(line);
		
		nameLabel.setLayoutX(0);
		nameLabel.setLayoutY(0);	
		
		addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				for(Line line : lines) {
					line.setStroke(Color.RED);
				}
			}
		});
		
		addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				for(Line line : lines) {
					line.setStroke(Color.BLACK);
				}
			}
		});
	}
}
