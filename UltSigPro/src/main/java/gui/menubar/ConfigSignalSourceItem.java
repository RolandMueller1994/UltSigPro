package gui.menubar;

import java.util.HashMap;

import gui.USPGui;
import guicomponents.DoubleTextField;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import iteratableinput.IteratableSignalSourceStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import resourceframework.ResourceProviderException;

public class ConfigSignalSourceItem extends MenuItem {

	private static final String TITLE = "title";
	private ConfigSignalSourceDialog dialog;

	public ConfigSignalSourceItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(ConfigSignalSourceItem.class, TITLE));

		super.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					if (dialog == null) {
						dialog = new ConfigSignalSourceDialog();
						dialog.initOwner(USPGui.stage);
						dialog.initStyle(StageStyle.UTILITY);
					}
					dialog.showAndWait();
				} catch (ResourceProviderException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private class ConfigSignalSourceDialog extends Dialog<ButtonType> {

		private final String TITLE = "title";
		private final String NAME = "name";
		private final String FREQUENCY = "frequency";
		private final String AMPLITUDE = "amplitude";
		private final String WARNING = "warning";

		private final double width = 150;

		public ConfigSignalSourceDialog() throws ResourceProviderException {

			LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
			VBox row = new VBox();
			HBox column = new HBox();

			DialogPane dialogPane = getDialogPane();
			dialogPane.setContent(row);
			setTitle(lanHandler.getLocalizedText(ConfigSignalSourceDialog.class, TITLE));

			Label nameLabel = new Label(lanHandler.getLocalizedText(ConfigSignalSourceDialog.class, NAME));
			nameLabel.setPrefWidth(width);
			Label ampLabel = new Label(lanHandler.getLocalizedText(ConfigSignalSourceDialog.class, AMPLITUDE));
			ampLabel.setPrefWidth(width);
			Label freqLabel = new Label(lanHandler.getLocalizedText(ConfigSignalSourceDialog.class, FREQUENCY));
			freqLabel.setPrefWidth(width);

			column.setPadding(new Insets(5));
			column.getChildren().add(nameLabel);
			column.getChildren().add(ampLabel);
			column.getChildren().add(freqLabel);
			row.getChildren().add(column);

			addNewLine(row);

			dialogPane.getButtonTypes().add(ButtonType.OK);
			dialogPane.getButtonTypes().add(ButtonType.CANCEL);

			Button btOk = (Button) dialogPane.lookupButton(ButtonType.OK);
			btOk.setDefaultButton(false);
			btOk.addEventFilter(ActionEvent.ACTION, e -> {
				boolean skip = true;
				boolean missing = false;
				for (Node allLines : row.getChildren()) {
					if (!skip) {
						// skip header line
						HBox lines = (HBox) allLines;
						int i = 0;
						for (Node line : lines.getChildren()) {
							if (i < 3) {
								// read only first 3 values and skip add/delete
								// button
								TextField field = (TextField) line;
								if (field.getText().isEmpty()) {
									missing = true;
								}
							}
							i++;
						}
					}
					skip = false;
				}
				if (missing) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setHeaderText(lanHandler
							.getLocalizedText(lanHandler.getLocalizedText(ConfigSignalSourceDialog.class, WARNING)));
					alert.showAndWait();
					e.consume();
				} else {
					skip = true;
					HashMap<String, IteratableSignalSourceStream> signalSource = new HashMap<>();
					for (Node allLines : row.getChildren()) {
						if (!skip) {
							// skip header line
							HBox lines = (HBox) allLines;
							int i = 0;
							String name = "";
							double amplitude = 0;
							double frequency = 0;
							for (Node line : lines.getChildren()) {
								if (i < 3) {
									// read only first 3 values and skip
									// add/delete button
									TextField field = (TextField) line;
									if (i == 0) {
										name = field.getText();
									} else if (i == 1) {
										amplitude = Double.parseDouble(field.getText());
									} else if (i == 2) {
										frequency = Double.parseDouble(field.getText());
									}
								}
								i++;
							}
							IteratableSignalSourceStream source = new IteratableSignalSourceStream(name, frequency, amplitude);
							signalSource.put(name, source);
						}
						skip = false;
					}
					InputAdministrator.getInputAdminstrator().setSoundInputDevices(signalSource);
				}
			});

		}

		private void addNewLine(VBox grid) {

			HBox hBox = new HBox();
			hBox.setPadding(new Insets(5));
			TextField textName = new TextField();
			textName.setPrefWidth(width);
			DoubleTextField textAmplitude = new DoubleTextField(0.0, 0.0, 1.0);
			textAmplitude.setPrefWidth(width);
			DoubleTextField textFrequency = new DoubleTextField(1000.0, 20.0, 20000.0);
			textFrequency.setPrefWidth(width);

			hBox.getChildren().add(textName);
			hBox.getChildren().add(textAmplitude);
			hBox.getChildren().add(textFrequency);

			if (grid.getChildren().size() == 1) {
				Button addBtn = new Button();
				addBtn.setFocusTraversable(false);
				addBtn.getStyleClass().add("plusButton");
				addBtn.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						addNewLine(grid);
					}
				});
				hBox.getChildren().add(addBtn);
			} else if (grid.getChildren().size() == 2) {
				Button minBtn = new Button();
				minBtn.setFocusTraversable(false);
				minBtn.getStyleClass().add("minusButton");
				minBtn.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						grid.getChildren().remove(grid.getChildren().size() - 1);
						grid.getScene().getWindow().sizeToScene();
					}
				});
				hBox.getChildren().add(minBtn);
			}

			grid.getChildren().add(hBox);
			grid.getScene().getWindow().sizeToScene();

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					textName.requestFocus();
				}
			});
		}
	}
}
