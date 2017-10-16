package channel;

import java.util.HashSet;
import java.util.LinkedList;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import plugins.sigproplugins.SigproPlugin;

public class PluginInput extends SigproPlugin {

	private String name = "Input";

	private final int height = 30;
	private final int width = 55;

	private Label label = new Label(name);

	public PluginInput() {
		label.setPrefWidth(width - 10);
		label.setMaxWidth(width - 10);
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public void setName(String name) {

		this.name = name;
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
			grid.setPadding(new Insets(5));
			grid.add(label, 0, 0);

			gui.getChildren().add(grid);
			gui.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, new CornerRadii(3), Insets.EMPTY)));
		}
		return gui;
	}

	@Override
	public LinkedList<OutputDataWrapper> putData(String input, double[] data) {

		if (input.equals("Input")) {

			LinkedList<OutputDataWrapper> output = new LinkedList<>();

			output.add(new OutputDataWrapper(new OutputInfoWrapper(this, "Input"), data));

			return output;
		}

		return null;
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputs = new HashSet<>();
		outputs.add("Input");

		return outputs;
	}

	@Override
	public void setPlay(boolean play) {
		// TODO Auto-generated method stub

	}

	@Override
	public HashSet<String> getInputConfig() {

		return new HashSet<String>();
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
