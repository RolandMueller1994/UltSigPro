package plugins.sigproplugins.internal;

import java.util.HashSet;
import java.util.LinkedList;

import channel.OutputDataWrapper;
import channel.OutputInfoWrapper;
import guicomponents.AbstractNumberTextField.ValueChangedInterface;
import guicomponents.DoubleTextField;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import plugins.sigproplugins.SigproPlugin;

/**
 * Internal plugin. Proceeds a clean gain operation.
 * 
 * @author roland
 *
 */
public class GainBlock2 extends SigproPlugin {

	private double gain = 1.0;

	private String name = "Gain2";

	private final int width = 100;
	private final int height = 100;

	private Label gainLabel = new Label("", new ImageView(new Image("file:icons/gainIcon.png")));
	private DoubleTextField gainTextField = new DoubleTextField(gain, 0.0, 20.0);
	private Button onOffButton = new Button();
	private Circle onOffIndicator = new Circle(5);

	private boolean on = false;

	/**
	 * Empty default constructor. Needed for instantiation by reflection.
	 */
	public GainBlock2() {
		onOffButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				on = !on;

				if (on) {
					onOffIndicator.setStyle("-fx-fill: -usp-light-blue2; -fx-effect: -effect-highlight-light");
				} else {
					onOffIndicator.setStyle("-fx-fill: grey");
				}

			}
		});

		gainTextField.registerValueChangedListener(new ValueChangedInterface() {

			@Override
			public void valueChanged(Number value) {

				gain = ((Double) value).doubleValue();
			}

		});

		gainTextField.setMaxWidth(40);
		onOffButton.getStyleClass().add("onOffButton");
		onOffIndicator.setFill(Color.GREY);
		gainTextField.setValue(1.0);
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

		if (gui == null) {
			gui = getInternalGUI();
			gui.setPrefSize(width, height);
			gui.setMaxSize(width, height);

			GridPane grid = new GridPane();
			RowConstraints column1 = new RowConstraints();
			column1.setPercentHeight(50);
			column1.setVgrow(Priority.ALWAYS);

			RowConstraints column2 = new RowConstraints();
			column2.setPercentHeight(50);
			column2.setVgrow(Priority.ALWAYS);
			grid.getRowConstraints().addAll(column1, column2);

			grid.add(onOffButton, 0, 0);
			grid.add(onOffIndicator, 1, 0);
			grid.add(gainLabel, 0, 1);
			grid.add(gainTextField, 1, 1);

			GridPane.setHalignment(gainLabel, HPos.CENTER);
			GridPane.setHalignment(onOffIndicator, HPos.CENTER);
			GridPane.setHalignment(onOffButton, HPos.CENTER);
			GridPane.setHalignment(gainTextField, HPos.CENTER);
			grid.setPadding(new Insets(0));

			gui.getChildren().add(grid);
//			gui.setStyle("-fx-background-color: -usp-dark-grey; -fx-background-radius: 4; -fx-border-color: white;");
			gui.setStyle("-fx-padding: 1px; -fx-background-color: -usp-dark-grey; -fx-background-radius: 4;");
		}

		return gui;
	}

	@Override
	public LinkedList<OutputDataWrapper> putData(String input, double[] data) {

		if (on) {
			if (input.contentEquals("In")) {
				for (int i = 0; i < data.length; i++) {
					data[i] = gain * data[i];
				}
			}
		}

		LinkedList<OutputDataWrapper> output = new LinkedList<>();
		output.add(new OutputDataWrapper(new OutputInfoWrapper(this, "Out"), data));

		return output;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputs = new HashSet<>();
		outputs.add("Out");

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

	@Override
	public double getMaxX() {

		return gui.getLayoutX() + width;
	}

	@Override
	public double getMaxY() {

		return gui.getLayoutY() + height;
	}

}
