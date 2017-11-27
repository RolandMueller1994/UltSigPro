package gui.menubar;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import channel.ChannelConfig;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;

/**
 * This {@link MenuItem} creates a dialog which is used to add a new channel to
 * the actual project.
 * 
 * @author roland
 *
 */
public class AddChannelMenuItem extends MenuItem {

	private static final String TITLE = "title";

	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	private InputAdministrator inputAdmin = InputAdministrator.getInputAdminstrator();
	private OutputAdministrator outputAdmin = OutputAdministrator.getOutputAdministrator();

	private DialogPane dialogPane;
	private AddChannelDialog dialog;
	private GridPane inputPane;
	private GridPane outputPane;

	/**
	 * Creates a new {@link MenuItem}.
	 * 
	 * @throws ResourceProviderException
	 *             if something went wrong in the
	 *             {@link LanguageResourceHandler}.
	 */
	public AddChannelMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(AddChannelMenuItem.class, TITLE));

		inputAdmin.collectSoundInputDevices();
		outputAdmin.collectSoundOutputDevices();

		super.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				dialog = new AddChannelDialog();
				dialog.initOwner(USPGui.stage);
				dialog.initStyle(StageStyle.UTILITY);

				Optional<ButtonType> result = dialog.showAndWait();

				if (result.isPresent() && result.get() == ButtonType.OK) {
					USPGui.addChannel(dialog.getConfig());
				}
			}

		});

	}

	private class AddChannelDialog extends Dialog<ButtonType> {

		private static final String TITLE = "title";
		private static final String TITLE_LABEL = "titleLabel";
		private static final String INPUT_LABEL = "inputLabel";
		private static final String OUTPUT_LABEL = "outputLabel";
		private static final String INOUT_ALERT = "inOutAlert";
		private static final String IN_ALERT = "inAlert";
		private static final String OUT_ALERT = "outAlert";
		private static final String INOUT_ALERT_TITLE = "inOutAlertTitle";
		private static final String INOUT_ALERT_HEADER = "inOutAlertHeader";

		private static final double IN_OUT_WIDTH = 70;
		private static final String WARNING = "warning";
		private static final String PRESENT_WARNING = "presentWarning";

		private List<ChoiceBox<String>> inputBoxes = new LinkedList<>();
		private List<ChoiceBox<String>> outputBoxes = new LinkedList<>();
		private HashMap<String, File> inputWaveFiles = new HashMap<>();
		private HashMap<String, File> outputWaveFiles = new HashMap<>();
		private TextField titleTextField;

		private GridPane gridPane;

		public AddChannelDialog() {

			setTitle(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE));

			gridPane = new GridPane();

			Label titleLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE_LABEL) + ": *");
			titleTextField = new TextField();
			
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					titleTextField.requestFocus();
				}
				
			});

			inputBoxes.add(new ChoiceBox<String>());
			outputBoxes.add(new ChoiceBox<String>());

			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);

			inputPane = getInputPane();
			outputPane = getOutputPane();

			gridPane.add(titleLabel, 0, 0);
			gridPane.add(titleTextField, 1, 0);
			gridPane.add(inputPane, 0, 1, 2, 1);
			gridPane.add(outputPane, 0, 2, 2, 1);

			inputPane.getColumnConstraints().add(0, new ColumnConstraints(IN_OUT_WIDTH));
			outputPane.getColumnConstraints().add(0, new ColumnConstraints(IN_OUT_WIDTH));

			gridPane.setMaxWidth(Double.MAX_VALUE);
			gridPane.setMaxHeight(Double.MAX_VALUE);

			GridPane.setHgrow(titleTextField, Priority.ALWAYS);

			dialogPane = getDialogPane();
			dialogPane.setContent(gridPane);

			dialogPane.getButtonTypes().add(ButtonType.OK);
			dialogPane.getButtonTypes().add(ButtonType.CANCEL);

			Button btOk = (Button) dialogPane.lookupButton(ButtonType.OK);
			btOk.addEventFilter(ActionEvent.ACTION, e -> {
				if (titleTextField.getText().isEmpty()) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setHeaderText(lanHandler.getLocalizedText(AddChannelDialog.class, WARNING));
					alert.showAndWait();
					e.consume();
				} else if (checkForDuplicates()) {
					e.consume();
				} else if (USPGui.checkIfPresent(titleTextField.getText())) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setHeaderText(lanHandler.getLocalizedText(AddChannelDialog.class, PRESENT_WARNING));
					alert.showAndWait();
					e.consume();
				}
			});
		}

		private boolean checkForDuplicates() {
			HashSet<String> devices = new HashSet<>();

			boolean inputDuplicate = false;
			boolean outputDuplicate = false;

			for (ChoiceBox<String> box : inputBoxes) {
				String cur = box.getSelectionModel().getSelectedItem();
				if (devices.contains(cur)) {
					inputDuplicate = true;
					break;
				}
				devices.add(cur);
			}

			devices.clear();
			for (ChoiceBox<String> box : outputBoxes) {
				String cur = box.getSelectionModel().getSelectedItem();
				if (devices.contains(cur)) {
					outputDuplicate = true;
					break;
				}
				devices.add(cur);
			}

			if (!inputDuplicate && !outputDuplicate) {
				return false;
			} else {
				String alertText;
				if (inputDuplicate && outputDuplicate) {
					alertText = lanHandler.getLocalizedText(AddChannelDialog.class, INOUT_ALERT);
				} else if (inputDuplicate) {
					alertText = lanHandler.getLocalizedText(AddChannelDialog.class, IN_ALERT);
				} else {
					alertText = lanHandler.getLocalizedText(AddChannelDialog.class, OUT_ALERT);
				}
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle(lanHandler.getLocalizedText(AddChannelDialog.class, INOUT_ALERT_TITLE));
				alert.setHeaderText(lanHandler.getLocalizedText(AddChannelDialog.class, INOUT_ALERT_HEADER));
				alert.setContentText(alertText);
				alert.showAndWait();
				return true;
			}
		}

		public ChannelConfig getConfig() {
			List<String> inputDevices = new LinkedList<>();
			List<String> outputDevices = new LinkedList<>();
			HashMap<String, File> choosedInputWaveFiles = new HashMap<>();
			HashMap<String, File> choosedOutputWaveFiles = new HashMap<>();

			for (ChoiceBox<String> choiceBox : inputBoxes) {
				String cur;
				if ((cur = choiceBox.getSelectionModel().getSelectedItem()) != null) {
					if (inputWaveFiles.containsKey(cur)) {
						choosedInputWaveFiles.put(cur, inputWaveFiles.get(cur));
					} else {
						inputDevices.add(cur);
					}
				}
			}

			for (ChoiceBox<String> choiceBox : outputBoxes) {
				String cur;
				if ((cur = choiceBox.getSelectionModel().getSelectedItem()) != null) {
					if (outputWaveFiles.containsKey(cur)) {
						choosedOutputWaveFiles.put(cur, outputWaveFiles.get(cur));
					} else {
						outputDevices.add(cur);
					}
				}
			}

			return new ChannelConfig(titleTextField.getText(), inputDevices, outputDevices, choosedInputWaveFiles, choosedOutputWaveFiles);
		}

		private GridPane getInputPane() {
			inputPane = new GridPane();

			// inputPane.setPadding(new Insets(5));
			inputPane.setHgap(5);
			inputPane.setVgap(5);

			Label inputLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, INPUT_LABEL) + ":");

			inputPane.add(inputLabel, 0, 0);

			ChoiceBox<String> firstBox = inputBoxes.get(0);
			GridPane.setHgrow(firstBox, Priority.ALWAYS);
			firstBox.setMaxWidth(Double.MAX_VALUE);
			inputPane.add(firstBox, 1, 0);

			ObservableList<String> list = FXCollections.observableArrayList(inputAdmin.getInputDevices());
			list.add("Wave " + lanHandler.getLocalizedText("file"));
			firstBox.setItems(list);

			firstBox.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					if (firstBox.getSelectionModel().getSelectedItem()
							.equals("Wave " + lanHandler.getLocalizedText("file"))) {
						FileChooser fileChooser = new FileChooser();
						fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
						File waveFile = fileChooser.showOpenDialog(USPGui.stage);
						if (waveFile != null) {
							list.add(waveFile.getName());
							firstBox.setItems(list);
							firstBox.setValue(waveFile.getName());
							if (!inputWaveFiles.containsKey(waveFile.getName())) {
								inputWaveFiles.put(waveFile.getName(), waveFile);
							}
						}
					}
				}
			});

			Button addButton = new Button(lanHandler.getLocalizedText("add"));
			addButton.setFocusTraversable(false);
			Button removeButton = new Button(lanHandler.getLocalizedText("remove"));
			removeButton.setFocusTraversable(false);
			GridPane.getHgrow(removeButton);
			removeButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					ChoiceBox<String> box = new ChoiceBox<>();
					ObservableList<String> list = FXCollections.observableArrayList(inputAdmin.getInputDevices());
					list.add("Wave " + lanHandler.getLocalizedText("file"));
					box.setItems(list);

					box.setOnAction(new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent event) {
							if (box.getSelectionModel().getSelectedItem()
									.equals("Wave " + lanHandler.getLocalizedText("file"))) {
								FileChooser fileChooser = new FileChooser();
								fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
								File waveFile = fileChooser.showOpenDialog(USPGui.stage);
								if (waveFile != null) {
									list.add(waveFile.getName());
									box.setItems(list);
									box.setValue(waveFile.getName());
									if (!inputWaveFiles.containsKey(waveFile.getName())) {
										inputWaveFiles.put(waveFile.getName(), waveFile);
									}
								}
							}
						}
					});
					inputBoxes.add(box);
					GridPane.setHgrow(box, Priority.ALWAYS);
					box.setMaxWidth(Double.MAX_VALUE);
					inputPane.add(box, 1, inputBoxes.size() - 1);

					if (inputBoxes.size() == 2) {
						inputPane.add(removeButton, 2, 1);
					}
					dialogPane.getScene().getWindow().sizeToScene();
				}

			});
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					ChoiceBox<String> remove = inputBoxes.remove(inputBoxes.size() - 1);
					inputPane.getChildren().remove(remove);
					if (inputBoxes.size() == 1) {
						inputPane.getChildren().remove(removeButton);
					}
					dialogPane.getScene().getWindow().sizeToScene();
				}

			});

			inputPane.add(addButton, 2, 0);

			return inputPane;
		}

		private GridPane getOutputPane() {
			outputPane = new GridPane();

			// outputPane.setPadding(new Insets(5));
			outputPane.setHgap(5);
			outputPane.setVgap(5);

			Label outputLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, OUTPUT_LABEL) + ":");

			outputPane.add(outputLabel, 0, 0);

			ChoiceBox<String> firstBox = outputBoxes.get(0);
			GridPane.setHgrow(firstBox, Priority.ALWAYS);
			firstBox.setMaxWidth(Double.MAX_VALUE);
			outputPane.add(firstBox, 1, 0);
			// TODO Get items from output handler
			ObservableList<String> list = FXCollections.observableArrayList(outputAdmin.getOutputDevices());
			list.add("Wave " + lanHandler.getLocalizedText("file"));
			firstBox.setItems(list);
			
			firstBox.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					if (firstBox.getSelectionModel().getSelectedItem()
							.equals("Wave " + lanHandler.getLocalizedText("file"))) {
						FileChooser fileChooser = new FileChooser();
						fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
						File waveFile = fileChooser.showSaveDialog(USPGui.stage);
						if (waveFile != null) {
							list.add(waveFile.getName());
							firstBox.setItems(list);
							firstBox.setValue(waveFile.getName());
							if (!outputWaveFiles.containsKey(waveFile.getName())) {
								outputWaveFiles.put(waveFile.getName(), waveFile);
							}
						}
					}
				}
			});

			Button addButton = new Button(lanHandler.getLocalizedText("add"));
			addButton.setFocusTraversable(false);
			Button removeButton = new Button(lanHandler.getLocalizedText("remove"));
			removeButton.setFocusTraversable(false);
			GridPane.getHgrow(removeButton);
			removeButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					ChoiceBox<String> box = new ChoiceBox<>();
					ObservableList<String> list = FXCollections.observableArrayList(outputAdmin.getOutputDevices());
					list.add("Wave " + lanHandler.getLocalizedText("file"));
					box.setItems(list);
					
					box.setOnAction(new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent event) {
							if (box.getSelectionModel().getSelectedItem()
									.equals("Wave " + lanHandler.getLocalizedText("file"))) {
								FileChooser fileChooser = new FileChooser();
								fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
								File waveFile = fileChooser.showSaveDialog(USPGui.stage);
								if (waveFile != null) {
									list.add(waveFile.getName());
									box.setItems(list);
									box.setValue(waveFile.getName());
									if (!outputWaveFiles.containsKey(waveFile.getName())) {
										outputWaveFiles.put(waveFile.getName(), waveFile);
									}
								}
							}
						}
					});
					outputBoxes.add(box);
					GridPane.setHgrow(box, Priority.ALWAYS);
					box.setMaxWidth(Double.MAX_VALUE);
					outputPane.add(box, 1, outputBoxes.size() - 1);

					if (outputBoxes.size() == 2) {
						outputPane.add(removeButton, 2, 1);
					}
					dialogPane.getScene().getWindow().sizeToScene();
				}

			});
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					ChoiceBox<String> remove = outputBoxes.remove(outputBoxes.size() - 1);
					outputPane.getChildren().remove(remove);
					if (outputBoxes.size() == 1) {
						outputPane.getChildren().remove(removeButton);
					}
					dialogPane.getScene().getWindow().sizeToScene();
				}

			});

			outputPane.add(addButton, 2, 0);

			return outputPane;
		}

	}
}
