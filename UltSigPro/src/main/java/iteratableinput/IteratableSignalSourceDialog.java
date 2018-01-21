package iteratableinput;

import guicomponents.DoubleTextField;
import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import resourceframework.ResourceProviderException;

public class IteratableSignalSourceDialog extends Dialog<ButtonType> {

	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	private static final String TITLE = "title";
	private static final String NAME = "name";
	private static final String FREQUENCY = "frequency";
	private static final String AMPLITUDE = "amplitude";

	private TextField nameField;
	private DoubleTextField amplitude;
	private DoubleTextField frequency;


	public IteratableSignalSourceDialog() throws ResourceProviderException {
		
		Label nameLabel;
		
		Label ampLabel;
		final double initAmp = 0.5;
		final double minAmp = 0;
		final double maxAmp = 1;

		Label freqLabel;
		final double initFreq = 1000;
		final double minFreq = 20;
		final double maxFreq = 20000;

		setTitle(lanHandler.getLocalizedText(IteratableSignalSourceDialog.class, TITLE));

		nameField = new TextField();
		nameLabel = new Label(lanHandler.getLocalizedText(IteratableSignalSourceDialog.class, NAME));

		ampLabel = new Label(lanHandler.getLocalizedText(IteratableSignalSourceDialog.class, AMPLITUDE) + " (" + minAmp
				+ " - " + maxAmp + ")");
		amplitude = new DoubleTextField(initAmp, minAmp, maxAmp);

		freqLabel = new Label(lanHandler.getLocalizedText(IteratableSignalSourceDialog.class, FREQUENCY) + " ("
				+ minFreq + " - " + maxFreq + ") [Hz]");
		frequency = new DoubleTextField(initFreq, minFreq, maxFreq);

		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				double width = freqLabel.getWidth();
				nameField.requestFocus();
				nameField.setPrefWidth(width);
				amplitude.setPrefWidth(width);
				amplitude.setMaxWidth(width);
				frequency.setPrefWidth(width);
				frequency.setMaxWidth(width);
			}

		});

		GridPane pane;
		pane = new GridPane();
		pane.setVgap(10);

		pane.add(nameLabel, 0, 0);
		pane.add(nameField, 0, 1);

		pane.add(ampLabel, 0, 2);
		pane.add(amplitude, 0, 3);

		pane.add(freqLabel, 0, 4);
		pane.add(frequency, 0, 5);

		getDialogPane().setContent(pane);

		getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		getDialogPane().getButtonTypes().add(ButtonType.OK);

		
	}

	public double getFrequency() {
		return frequency.getValue();
	}

	public double getAmplitude() {
		return amplitude.getValue();
	}

	public String getName() {
		return nameField.getText();
	}

	public boolean inputsMissing() {
		if (nameField.getText().isEmpty() || amplitude.getText().isEmpty() || frequency.getText().isEmpty()) {
			return true;
		}
		return false;
	}
}
