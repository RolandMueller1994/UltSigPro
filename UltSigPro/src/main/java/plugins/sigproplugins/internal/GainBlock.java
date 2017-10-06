package plugins.sigproplugins.internal;


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import plugins.sigproplugins.SigproPlugin;

/**
 * Internal plugin. Proceeds a clean gain operation.
 * 
 * @author roland
 *
 */
public class GainBlock implements SigproPlugin {

	private double gain = 1.0;
	
	private String name = "Gain";
	
	private final int width = 100;
	private final int height = 120;
	
	private Label nameLabel = new Label(name);
	private TextField gainTextField = new TextField();
	private Button onButton = new Button("On");
	private Rectangle onRect = new Rectangle(width - 10, 25);
	
	private boolean on = false;
	
	private Pane gui;

	/**
	 * Empty default constructor. Needed for instantiation by reflection. 
	 */
	public GainBlock() {
		onButton.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				on = !on;
				
				if(on) {
					onRect.setFill(Color.GREEN);
				} else {
					onRect.setFill(Color.GREY);
				}
				
			}
		});
		
		nameLabel.setMaxWidth(width - 10);
		onButton.setMaxWidth(width - 10);
		gainTextField.setMaxWidth(width - 10);
		onRect.setFill(Color.GREY);
		onRect.setArcHeight(3);
		onRect.setArcWidth(3);
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public String getVersion() {

		return "1.0.0";
	}

	@Override
	public Pane getGUI() {
		
		if(gui == null) {
			gui = new Pane();
			gui.setPrefSize(width, height);
			gui.setMaxSize(width, height);
			
			GridPane grid = new GridPane();
			
			grid.add(nameLabel, 0, 0);
			grid.add(gainTextField, 0, 1);
			grid.add(onButton, 0, 2);
			grid.add(onRect, 0, 3);
			grid.setPadding(new Insets(5));
			grid.setVgap(5);
			
			gui.getChildren().add(grid);
			gui.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, new CornerRadii(3), Insets.EMPTY)));
		}
		
		return gui;
	}

	@Override
	public LinkedList<OutputDataWrapper> putData(String input, double[] data) {

		if(on) {
			if (input.contentEquals("in")) {
				for (int i = 0; i < data.length; i++) {
					data[i] = gain * data[i];
				}
			}			
		}

		LinkedList<OutputDataWrapper> output = new LinkedList<> ();
		output.add(new OutputDataWrapper(new OutputInfoWrapper(this, "out"), data));
		
		return output;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputs = new HashSet<>();
		outputs.add("out");

		return outputs;
	}

	@Override
	public void setPlay(boolean play) {
		// Nothing to do here

	}

	@Override
	public void setName(String name) {
	
		this.name = name;
	}

	@Override
	public HashSet<String> getInputConfig() {
		
		HashSet<String> inputInfo = new HashSet<>();
		inputInfo.add("In");
		
		return inputInfo;
	}

	@Override
	public int getWidth() {
		
		return width;
	}

	@Override
	public int getHeight() {
		
		return height;
	}

}
