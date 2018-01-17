package channel;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gui.USPGui;
import gui.soundLevelDisplay.SoundLevelBar;
import guicomponents.AbstractNumberTextField.ValueChangedInterface;
import guicomponents.DoubleTextField;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;

/**
 * Root for channel gui and the channel itself.
 * 
 * @author roland
 *
 */
public class ChannelPane extends TitledPane {

	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	private ChannelWaveChart waveChart = new ChannelWaveChart();

	// private BorderPane centralPane = new BorderPane();
	private GridPane centralPane = new GridPane();
	private ChannelConfig config;

	private Channel channel;

	private InputPane inputPane;
	private OutputPane outputPane;
	private String name;

	private ContextMenu contextMenu;

	/**
	 * Creates a new ChannelPane with the given {@link ChannelConfig}. A
	 * {@link Channel} will be created. This channel will register as listener
	 * at the {@link InputAdministrator} for the given devices.
	 * 
	 * @param config
	 *            The current {@link ChannelConfig}. Must not be null.
	 * @throws ResourceProviderException
	 *             If a problem with the {@link LanguageResourceHandler} occurs.
	 */
	public ChannelPane(@Nonnull ChannelConfig config) throws ResourceProviderException {
		super.setText(config.getName());
		super.setGraphic(new ImageView(new Image("file:icons/channelIconSmall.png")));
		super.setContent(centralPane);
		this.config = config;
		name = config.getName();
		inputPane = new InputPane(config.getInputDevices(), config.getInputWaveFiles().keySet(),
				config.getSignalSources());
		outputPane = new OutputPane(config.getOutputDevices(), config.getOutputWaveFiles().keySet());
		centralPane.getStyleClass().add("channel-central-pane");

		centralPane.add(inputPane, 0, 0);
		centralPane.add(outputPane, 2, 0);
		centralPane.add(waveChart, 1, 0);

		GridPane.setHgrow(waveChart, Priority.ALWAYS);

		centralPane.setPrefWidth(Double.MAX_VALUE);

		MenuItem deleteMenuItem = new MenuItem(lanHandler.getLocalizedText("delete"));

		deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				deleteThisChannel();
			}

		});
		contextMenu = new ContextMenu();
		contextMenu.getItems().add(deleteMenuItem);

		super.setContextMenu(contextMenu);

		channel = new Channel(this, config);

		setPlay(false);
	}

	public ChannelConfig getChannelConfig() {
		return config;
	}

	/**
	 * Collects all settings of a channel for creating a USP project file.
	 * 
	 * @param doc
	 *            The document to be created.
	 * @param channel
	 *            Root element where all settings are appended.
	 */
	public void collectChannelConfig(Document doc, Element channel) {
		String name = config.getName();
		Collection<String> inputDevices = config.getInputDevices();
		Collection<String> outputDevices = config.getOutputDevices();
		Collection<File> inputWaveFiles = config.getInputWaveFiles().values();
		Collection<File> outputWaveFiles = config.getOutputWaveFiles().values();
		// channel name
		Element channelName = doc.createElement("name");
		channelName.appendChild(doc.createTextNode(name));
		channel.appendChild(channelName);

		// channel expanded or collapsed
		Element expanded = doc.createElement("expanded");
		expanded.appendChild(doc.createTextNode(String.valueOf(super.isExpanded())));
		channel.appendChild(expanded);

		// input device elements
		for (String inputDevice : inputDevices) {
			Element inputDeviceElement = doc.createElement("inputDevice");
			Element inputDeviceName = doc.createElement("name");
			Element inputDeviceGain = doc.createElement("gain");
			inputDeviceName.appendChild(doc.createTextNode(inputDevice));
			inputDeviceGain.appendChild(doc.createTextNode(lookForDeviceGain(inputDevice)));
			inputDeviceElement.appendChild(inputDeviceName);
			inputDeviceElement.appendChild(inputDeviceGain);
			channel.appendChild(inputDeviceElement);
		}

		// output device elements
		for (String outputDevice : outputDevices) {
			Element outputDeviceElement = doc.createElement("outputDevice");
			Element outputDeviceName = doc.createElement("name");
			Element outputDeviceGain = doc.createElement("gain");
			outputDeviceName.appendChild(doc.createTextNode(outputDevice));
			outputDeviceGain.appendChild(doc.createTextNode(lookForDeviceGain(outputDevice)));
			outputDeviceElement.appendChild(outputDeviceName);
			outputDeviceElement.appendChild(outputDeviceGain);
			channel.appendChild(outputDeviceElement);
		}

		// input wave files
		for (File inputWaveFile : inputWaveFiles) {
			Element inputWaveElement = doc.createElement("inputWave");
			Element inputWavePath = doc.createElement("filePath");
			Element inputWaveGain = doc.createElement("gain");
			inputWavePath.appendChild(doc.createTextNode(inputWaveFile.getAbsolutePath()));
			inputWaveGain.appendChild(doc.createTextNode(lookForDeviceGain(inputWaveFile.getName())));
			inputWaveElement.appendChild(inputWavePath);
			inputWaveElement.appendChild(inputWaveGain);
			channel.appendChild(inputWaveElement);
		}

		// output wave files
		for (File outputWaveFile : outputWaveFiles) {
			Element outputWaveElement = doc.createElement("outputWave");
			Element outputWavePath = doc.createElement("filePath");
			Element outputWaveGain = doc.createElement("gain");
			outputWavePath.appendChild(doc.createTextNode(outputWaveFile.getAbsolutePath()));
			outputWaveGain.appendChild(doc.createTextNode(lookForDeviceGain(outputWaveFile.getName())));
			outputWaveElement.appendChild(outputWavePath);
			outputWaveElement.appendChild(outputWaveGain);
			channel.appendChild(outputWaveElement);
		}

	}

	public String lookForDeviceGain(String deviceName) {
		ObservableList<DeviceGainTuple> inputDevices = inputPane.getDeviceGainTable().getItems();
		ObservableList<DeviceGainTuple> outputDevices = outputPane.getDeviceGainTable().getItems();
		for (DeviceGainTuple device : inputDevices) {
			if (deviceName.equals(device.getDevice())) {
				return device.getGain().getValue().toString();
			}
		}
		for (DeviceGainTuple device : outputDevices) {
			if (deviceName.equals(device.getDevice())) {
				return device.getGain().getValue().toString();
			}
		}
		return "1.0";
	}

	public void insertWaveChartData(int[] data) {
		waveChart.insertData(data);
	}

	/**
	 * The name of this channel
	 * 
	 * @return The name as {@link String}. Won't be null.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * Triggers the channel to play.
	 * 
	 * @param play
	 *            True if the channel should start play, false else.
	 */
	public void setPlay(boolean play) {
		contextMenu.getItems().get(0).setDisable(play);
		inputPane.setEditable(!play);
		outputPane.setEditable(!play);
		channel.setPlay(play);
		waveChart.setPlay(play);
	}

	private void deleteThisChannel() {
		USPGui.deleteChannel(this);
		SoundLevelBar.getSoundLevelBar().removeChannelSoundDevices(channel.getChannelConfig());
	}

	public Channel getChannel() {
		return channel;
	}

	public InputPane getInputPane() {
		return inputPane;
	}

	public TableView<DeviceGainTuple> getInputDeviceGainTable() {
		return inputPane.getDeviceGainTable();
	}

	public TableView<DeviceGainTuple> getOutputDeviceGainTable() {
		return outputPane.getDeviceGainTable();
	}

	public OutputPane getOutputPane() {
		return outputPane;
	}

	private class InputPane extends Pane {

		private TableView<DeviceGainTuple> deviceGainTable;
		private Button addButton;
		private Button removeButton;

		private ObservableList<DeviceGainTuple> tableRows = FXCollections.observableArrayList();

		public InputPane(Collection<String> inputDevices, Collection<String> waveFiles,
				Collection<String> signalSources) {
			addButton = new Button();
			addButton.getStyleClass().add("plusButton");
			addButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					addDevice();
				}

			});
			removeButton = new Button();
			removeButton.getStyleClass().add("minusButton");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					removeDevice();
				}

			});

			deviceGainTable = new TableView<>();
			deviceGainTable.setPrefSize(200, 100);
			deviceGainTable.setMinSize(200, 100);
			deviceGainTable.setEditable(true);

			TableColumn<DeviceGainTuple, String> deviceColumn = new TableColumn<>("Device");
			deviceColumn.setCellValueFactory(new PropertyValueFactory<DeviceGainTuple, String>("device"));
			deviceColumn.setPrefWidth(100);

			TableColumn<DeviceGainTuple, DoubleTextField> gainColumn = new TableColumn<>("Gain");
			gainColumn.setCellValueFactory(new PropertyValueFactory<DeviceGainTuple, DoubleTextField>("gain"));
			gainColumn.setPrefWidth(100);

			for (String device : inputDevices) {
				addDevice(device);
			}

			for (String device : waveFiles) {
				addDevice(device);
			}
			
			for (String signalSource : signalSources) {
				addDevice(signalSource);
			}

			deviceGainTable.setItems(tableRows);
			deviceGainTable.getColumns().addAll(deviceColumn, gainColumn);

			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);

			gridPane.add(addButton, 0, 0, 1, 1);
			gridPane.add(removeButton, 1, 0, 1, 1);
			gridPane.add(deviceGainTable, 0, 1, 2, 1);

			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(deviceGainTable, Priority.ALWAYS);

			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);

			gridPane.getColumnConstraints().addAll(cc, cc);

			super.getChildren().add(gridPane);
		}

		private void addDevice(String device) {
			DeviceGainTuple deviceGainTuple = new DeviceGainTuple(device, 1.0);
			tableRows.add(deviceGainTuple);

			deviceGainTuple.getGain().registerValueChangedListener(new ValueChangedInterface() {

				@Override
				public void valueChanged(Number value) {
					InputAdministrator.getInputAdminstrator().inputLevelMultiplierChanged(channel, device,
							(Double) value);
				}
			});
		}

		private void removeDevice(String device) {
			Iterator<DeviceGainTuple> iter = tableRows.iterator();
			DeviceGainTuple remove = null;

			while (iter.hasNext()) {
				DeviceGainTuple cur = iter.next();
				if (cur.getDevice().equals(device)) {
					remove = cur;
					break;
				}
			}

			if (remove != null) {
				tableRows.remove(remove);
			}
		}

		private void setEditable(boolean b) {
			addButton.setDisable(!b);
			removeButton.setDisable(!b);
		}

		private void addDevice() {
			AddRemoveDialog dialog = new AddRemoveDialog(true, true);
			dialog.showAndWait();
		}

		private void removeDevice() {
			// if(!table.getItems().isEmpty()) {
			AddRemoveDialog dialog = new AddRemoveDialog(true, false);
			dialog.showAndWait();
			// }
		}

		public TableView<DeviceGainTuple> getDeviceGainTable() {
			return deviceGainTable;
		}

		public void addInput(String device) {
			// table.getItems().add(device);
			config.addInputDevice(device);
			channel.addInputDevice(device);
		}

		public void removeInput(String device) {
			// table.getItems().remove(device);
			config.removeInputDevice(device);
			channel.removeInputDevice(device);
		}
	}

	private class OutputPane extends Pane {

		private TableView<DeviceGainTuple> deviceGainTable;
		private Button addButton;
		private Button removeButton;

		private ObservableList<DeviceGainTuple> tableRows = FXCollections.observableArrayList();

		public OutputPane(Collection<String> outputDevices, Collection<String> waveFiles) {
			addButton = new Button();
			addButton.getStyleClass().add("plusButton");
			addButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					addDevice();
				}

			});
			removeButton = new Button();
			removeButton.getStyleClass().add("minusButton");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					removeDevice();
				}

			});

			deviceGainTable = new TableView<>();
			deviceGainTable.setPrefSize(200, 100);
			deviceGainTable.setMinSize(200, 100);
			deviceGainTable.setEditable(true);

			TableColumn<DeviceGainTuple, String> deviceColumn = new TableColumn<>("Device");
			deviceColumn.setCellValueFactory(new PropertyValueFactory<DeviceGainTuple, String>("device"));
			deviceColumn.setPrefWidth(100);

			TableColumn<DeviceGainTuple, DoubleTextField> gainColumn = new TableColumn<>("Gain");
			gainColumn.setCellValueFactory(new PropertyValueFactory<DeviceGainTuple, DoubleTextField>("gain"));
			gainColumn.setPrefWidth(100);

			for (String device : outputDevices) {
				addDevice(device);
			}

			for (String device : waveFiles) {
				addDevice(device);
			}

			deviceGainTable.setItems(tableRows);
			deviceGainTable.getColumns().addAll(deviceColumn, gainColumn);

			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);

			gridPane.add(addButton, 0, 0);
			gridPane.add(removeButton, 1, 0);

			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			gridPane.add(deviceGainTable, 0, 1, 2, 1);

			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(deviceGainTable, Priority.ALWAYS);

			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);

			gridPane.getColumnConstraints().addAll(cc, cc);

			super.getChildren().add(gridPane);
		}

		private void setEditable(boolean b) {
			addButton.setDisable(!b);
			removeButton.setDisable(!b);
		}

		public TableView<DeviceGainTuple> getDeviceGainTable() {
			return deviceGainTable;
		}

		private void addDevice(String device) {
			DeviceGainTuple deviceGainTuple = new DeviceGainTuple(device, 1.0);
			tableRows.add(deviceGainTuple);

			deviceGainTuple.getGain().registerValueChangedListener(new ValueChangedInterface() {

				@Override
				public void valueChanged(Number value) {
					OutputAdministrator.getOutputAdministrator().outputLevelMultiplierChanged(channel, device,
							(Double) value);
				}
			});
		}

		private void removeDevice(String device) {
			Iterator<DeviceGainTuple> iter = tableRows.iterator();
			DeviceGainTuple remove = null;

			while (iter.hasNext()) {
				DeviceGainTuple cur = iter.next();
				if (cur.getDevice().equals(device)) {
					remove = cur;
					break;
				}
			}

			if (remove != null) {
				tableRows.remove(remove);
			}
		}

		private void addDevice() {
			AddRemoveDialog dialog = new AddRemoveDialog(false, true);
			dialog.showAndWait();
		}

		private void removeDevice() {
			// if (!table.getItems().isEmpty()) {
			AddRemoveDialog dialog = new AddRemoveDialog(false, false);
			dialog.showAndWait();
			// }
		}
	}

	public class DeviceGainTuple {

		private DoubleTextField gain;
		private SimpleStringProperty device;

		public DeviceGainTuple(String device, double gain) {
			this.device = new SimpleStringProperty(device);
			this.gain = new DoubleTextField(gain, 0.0, 20.0);
		}

		public DoubleTextField getGain() {
			return gain;
		}

		public void setGain(DoubleTextField gain) {
			this.gain = gain;
		}

		public String getDevice() {
			return device.get();
		}

		public void setDevice(String device) {
			this.device.set(device);
		}
	}

	private class AddRemoveDialog extends Dialog<ButtonType> {

		private static final String INPUT_ADD_TITLE = "inputAddTitle";
		private static final String INPUT_REMOVE_TITLE = "inputRemoveTitle";
		private static final String OUTPUT_ADD_TITLE = "outputAddTitle";
		private static final String OUTPUT_REMOVE_TITLE = "outputRemoveTitle";
		private static final String EMPTY_ALERT_TITLE = "emptyAlertTitle";
		private static final String EMPTY_ALERT_HEADER = "emptyAlertHeader";
		private static final String DUPLICATE_ALERT_TITLE = "duplicateAlertTitle";
		private static final String DUPLICATE_ALERT_HEADER = "duplicateAlertHeader";
		private static final String DEVICE_LABEL = "deviceLabel";
		private static final String DEVICE_LABEL_WAVE = "deviceLabelWave";

		private InputAdministrator inputAdmin = InputAdministrator.getInputAdminstrator();
		private OutputAdministrator outputAdmin = OutputAdministrator.getOutputAdministrator();
		private ChoiceBox<String> choiceBox;
		private ChoiceBox<String> choiceBoxWave;

		private String selected;
		private String selectedWaveName;
		private File selectedWave;

		private Alert duplicateAlert;

		public AddRemoveDialog(boolean input, boolean add) {
			DialogPane dialogPane = getDialogPane();

			dialogPane.getButtonTypes().add(ButtonType.APPLY);
			dialogPane.getButtonTypes().add(ButtonType.CANCEL);
			initOwner(USPGui.stage);
			initStyle(StageStyle.UTILITY);

			duplicateAlert = new Alert(AlertType.ERROR);
			duplicateAlert.setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, DUPLICATE_ALERT_TITLE));
			duplicateAlert.setHeaderText(lanHandler.getLocalizedText(AddRemoveDialog.class, DUPLICATE_ALERT_HEADER));

			dialogPane.setPrefWidth(350);

			choiceBox = new ChoiceBox<String>();
			choiceBoxWave = new ChoiceBox<String>();

			if (input && add) {
				inputAdmin.collectSoundInputDevices();
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, INPUT_ADD_TITLE));
				choiceBox.setItems(FXCollections.observableArrayList(inputAdmin.getInputDevices()));
				ObservableList<String> list = FXCollections.observableArrayList();
				list.add("Wave " + lanHandler.getLocalizedText("file"));
				choiceBoxWave.setItems(list);

				choiceBoxWave.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						if (choiceBoxWave.getSelectionModel().getSelectedItem()
								.equals("Wave " + lanHandler.getLocalizedText("file"))) {
							FileChooser fileChooser = new FileChooser();
							fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
									"Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
							File waveFile = fileChooser.showOpenDialog(USPGui.stage);
							if (waveFile != null) {
								list.add(waveFile.getName());
								choiceBoxWave.setItems(list);
								choiceBoxWave.setValue(waveFile.getName());
								selectedWave = waveFile;
							}
						}
					}
				});
			} else if (input) {
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, INPUT_REMOVE_TITLE));
				ObservableList<String> inputDevices = FXCollections.observableArrayList();
				for (String device : getChannelConfig().getInputDevices()) {
					inputDevices.add(device);
				}
				choiceBox.setItems(inputDevices);
				ObservableList<String> inputDevicesWave = FXCollections.observableArrayList();
				for (String wave : getChannelConfig().getInputWaveFiles().keySet()) {
					inputDevicesWave.add(wave);
				}
				choiceBoxWave.setItems(inputDevicesWave);
			} else if (add) {
				outputAdmin.collectSoundOutputDevices();
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, OUTPUT_ADD_TITLE));
				choiceBox.setItems(FXCollections.observableArrayList(outputAdmin.getOutputDevices()));
				ObservableList<String> list = FXCollections.observableArrayList();
				list.add("Wave " + lanHandler.getLocalizedText("file"));
				choiceBoxWave.setItems(list);

				choiceBoxWave.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						if (choiceBoxWave.getSelectionModel().getSelectedItem()
								.equals("Wave " + lanHandler.getLocalizedText("file"))) {
							FileChooser fileChooser = new FileChooser();
							fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
									"Wave " + lanHandler.getLocalizedText("file"), "*.wav"));
							File waveFile = fileChooser.showSaveDialog(USPGui.stage);
							if (waveFile != null) {
								list.add(waveFile.getName());
								choiceBoxWave.setItems(list);
								choiceBoxWave.setValue(waveFile.getName());
								selectedWave = waveFile;
							}
						}
					}
				});
			} else

			{
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, OUTPUT_REMOVE_TITLE));
				ObservableList<String> outputDevices = FXCollections.observableArrayList();
				for (String device : getChannelConfig().getOutputDevices()) {
					outputDevices.add(device);
				}
				choiceBox.setItems(outputDevices);
				ObservableList<String> outputDevicesWave = FXCollections.observableArrayList();
				for (String wave : getChannelConfig().getOutputWaveFiles().keySet()) {
					outputDevicesWave.add(wave);
				}
				choiceBoxWave.setItems(outputDevicesWave);
			}

			Label deviceLabel = new Label(lanHandler.getLocalizedText(AddRemoveDialog.class, DEVICE_LABEL));
			Label deviceLabelWave = new Label(lanHandler.getLocalizedText(AddRemoveDialog.class, DEVICE_LABEL_WAVE));

			choiceBox.setMaxWidth(Double.MAX_VALUE);
			choiceBoxWave.setMaxWidth(Double.MAX_VALUE);
			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);
			gridPane.add(deviceLabel, 0, 0);
			gridPane.add(choiceBox, 1, 0);
			gridPane.add(deviceLabelWave, 0, 1);
			gridPane.add(choiceBoxWave, 1, 1);
			GridPane.setHgrow(choiceBox, Priority.ALWAYS);
			GridPane.setHgrow(choiceBoxWave, Priority.ALWAYS);
			GridPane.setHalignment(choiceBox, HPos.LEFT);
			GridPane.setHalignment(deviceLabel, HPos.RIGHT);
			GridPane.setHalignment(choiceBoxWave, HPos.LEFT);
			GridPane.setHalignment(deviceLabelWave, HPos.RIGHT);
			dialogPane.setContent(gridPane);

			Button applyButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
			applyButton.addEventFilter(ActionEvent.ACTION, e -> {
				selected = choiceBox.getSelectionModel().getSelectedItem();
				selectedWaveName = choiceBoxWave.getSelectionModel().getSelectedItem();
				if (selected == null && (selectedWaveName == null || (selectedWave == null && add))) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, EMPTY_ALERT_TITLE));
					alert.setHeaderText(lanHandler.getLocalizedText(AddRemoveDialog.class, EMPTY_ALERT_HEADER));
					alert.showAndWait();
					e.consume();
				} else if (add) {

					if (input) {

						if (selected != null) {
							for (String device : channel.getChannelConfig().getInputDevices()) {
								if (device.equals(selected)) {
									duplicateAlert.showAndWait();
									e.consume();
									return;
								}
							}
						}

						if (selectedWave != null && selectedWaveName != null) {
							for (String file : channel.getChannelConfig().getInputWaveFiles().keySet()) {
								if (file.equals(selectedWaveName)) {
									duplicateAlert.showAndWait();
									e.consume();
									return;
								}
							}
						}

						if (selected != null) {
							inputAdmin.addDeviceToInputDataListener(channel, selected);
							inputPane.addDevice(selected);
							channel.addInputDevice(selected);
							getChannelConfig().addInputDevice(selected);
							SoundLevelBar.getSoundLevelBar().addDeviceToChannel(selected, channel, true);
						}

						if (selectedWave != null && selectedWaveName != null) {
							HashMap<String, File> waveFiles = new HashMap<>();
							waveFiles.put(selectedWaveName, selectedWave);
							inputAdmin.openWaveFiles(waveFiles, channel);

							inputPane.addDevice(selectedWaveName);
							channel.addInputDevice(selectedWaveName);
							getChannelConfig().addInputWaveFile(selectedWaveName, selectedWave);
							SoundLevelBar.getSoundLevelBar().addDeviceToChannel(selectedWaveName, channel, true);
						}
					} else {

						if (selected != null) {
							for (String device : channel.getChannelConfig().getOutputDevices()) {
								if (device.equals(selected)) {
									duplicateAlert.showAndWait();
									e.consume();
									return;
								}
							}
						}

						if (selectedWave != null && selectedWaveName != null) {
							for (String file : channel.getChannelConfig().getOutputWaveFiles().keySet()) {
								if (file.equals(selectedWaveName)) {
									duplicateAlert.showAndWait();
									e.consume();
									return;
								}
							}
						}

						if (selected != null) {
							outputAdmin.addSoundOutputDeviceToSpeaker(channel, selected);
							outputPane.addDevice(selected);
							channel.addOutputDevice(selected);
							getChannelConfig().addOutputDevice(selected);
							SoundLevelBar.getSoundLevelBar().addDeviceToChannel(selected, channel, false);
						}

						if (selectedWave != null && selectedWaveName != null) {
							HashMap<String, File> waveFiles = new HashMap<>();
							waveFiles.put(selectedWaveName, selectedWave);
							outputAdmin.setWaveFileEntries(waveFiles, channel);
							outputPane.addDevice(selectedWaveName);
							channel.addOutputDevice(selectedWaveName);
							getChannelConfig().addOutputWaveFile(selectedWaveName, selectedWave);
							SoundLevelBar.getSoundLevelBar().addDeviceToChannel(selectedWaveName, channel, false);
						}
					}
				} else {
					if (input) {
						if (selected != null) {
							inputAdmin.removeDeviceFromInputDataListener(channel, selected);
							inputPane.removeDevice(selected);
							channel.removeInputDevice(selected);
							getChannelConfig().removeInputDevice(selected);
							SoundLevelBar.getSoundLevelBar().removeDeviceFromChannel(selected, channel, true);
						}

						if (selectedWaveName != null) {
							HashMap<String, File> waveFiles = new HashMap<>();
							waveFiles.put(selectedWaveName, null);
							inputAdmin.removeWaveFiles(waveFiles, channel);
							inputPane.removeDevice(selectedWaveName);
							channel.removeInputDevice(selectedWaveName);
							getChannelConfig().removeInputWaveFile(selectedWaveName);
							SoundLevelBar.getSoundLevelBar().removeDeviceFromChannel(selectedWaveName, channel, true);
						}
					} else {
						if (selected != null) {
							outputAdmin.removeDeviceFromOutputDataSpeaker(channel, selected);
							outputPane.removeDevice(selected);
							channel.removeOutputDevice(selected);
							getChannelConfig().removeOutputDevice(selected);
							SoundLevelBar.getSoundLevelBar().removeDeviceFromChannel(selected, channel, false);
						}

						if (selectedWaveName != null) {
							HashMap<String, File> waveFiles = new HashMap<>();
							waveFiles.put(selectedWaveName, null);
							outputAdmin.removeWaveFileEntries(waveFiles, channel);
							outputPane.removeDevice(selectedWaveName);
							channel.removeOutputDevice(selectedWaveName);
							getChannelConfig().removeOutputWaveFile(selectedWaveName);
							SoundLevelBar.getSoundLevelBar().removeDeviceFromChannel(selectedWaveName, channel, false);
						}
					}
				}
			});

		}

	}

}
