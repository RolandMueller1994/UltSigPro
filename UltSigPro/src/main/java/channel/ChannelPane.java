package channel;

import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nonnull;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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
	
	//private BorderPane centralPane = new BorderPane();
	private GridPane centralPane = new GridPane();
	private ChannelConfig config;
	private Channel channel;
	
	private boolean play;
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
		super.setContent(centralPane);
		this.config = config;
		name = config.getName();
		inputPane = new InputPane(config.getInputDevices(), config.getInputWaveFiles().keySet());
		outputPane = new OutputPane(config.getOutputDevices(), config.getOutputWaveFiles().keySet());
		
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
		this.play = play;
		inputPane.setEditable(!play);
		outputPane.setEditable(!play);
		channel.setPlay(play);
		waveChart.setPlay(play);
	}

	private void deleteThisChannel() {
		channel.delete();
		USPGui.deleteChannel(this);
	}
	
	public Channel getChannel() {
		return channel;
	}

	private class InputPane extends Pane {

		private ListView<String> table;
		private Button addButton;
		private Button removeButton;

		public InputPane(Collection<String> inputDevices, Collection<String> waveFiles) {
			addButton = new Button("+");
			addButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					addDevice();
				}

			});
			removeButton = new Button("-");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					removeDevice();
				}

			});
			table = new ListView<>();
			table.setPrefSize(200, 100);

			for (String cur : inputDevices) {
				table.getItems().add(cur);
			}
			
			for (String cur : waveFiles) {
				table.getItems().add(cur);
			}

			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);

			gridPane.add(addButton, 0, 0);
			gridPane.add(removeButton, 1, 0);
			gridPane.add(table, 0, 1, 2, 1);

			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(table, Priority.ALWAYS);

			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);

			gridPane.getColumnConstraints().addAll(cc, cc);

			super.getChildren().add(gridPane);
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
			if(!table.getItems().isEmpty()) {
				AddRemoveDialog dialog = new AddRemoveDialog(true, false);
				dialog.showAndWait();				
			}
		}

		public ListView<String> getTable() {
			return table;
		}

		public void addInput(String device) {
			table.getItems().add(device);
			config.addInputDevice(device);
			channel.addInputDevice(device);
		}

		public void removeInput(String device) {
			table.getItems().remove(device);
			config.removeInputDevice(device);
			channel.removeInputDevice(device);
		}
	}

	private class OutputPane extends Pane {

		private ListView<String> table;
		private Button addButton;
		private Button removeButton;

		public OutputPane(Collection<String> outputDevices, Collection<String> waveFiles) {
			addButton = new Button("+");
			addButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					addDevice();
				}

			});
			removeButton = new Button("-");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			removeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					removeDevice();
				}

			});
			table = new ListView<>();
			table.setPrefSize(200, 100);
			
			for (String cur : outputDevices) {
				table.getItems().add(cur);
			}
			
			for (String cur : waveFiles) {
				table.getItems().add(cur);
			}

			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);

			gridPane.add(addButton, 0, 0);
			gridPane.add(removeButton, 1, 0);
			gridPane.add(table, 0, 1, 2, 1);

			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(table, Priority.ALWAYS);

			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);

			gridPane.getColumnConstraints().addAll(cc, cc);

			super.getChildren().add(gridPane);
		}

		private void setEditable(boolean b) {
			addButton.setDisable(!b);
			removeButton.setDisable(!b);
		}

		public ListView<String> getTable() {
			return table;
		}

		public void addOutput(String device) {
			table.getItems().add(device);
			config.addOutputDevice(device);
		}

		public void removeOutput(String device) {
			table.getItems().remove(device);
			config.removeOutputDevice(device);
		}

		private void addDevice() {
			AddRemoveDialog dialog = new AddRemoveDialog(false, true);
			dialog.showAndWait();
		}

		private void removeDevice() {
			if(!table.getItems().isEmpty()) {
				AddRemoveDialog dialog = new AddRemoveDialog(false, false);
				dialog.showAndWait();				
			}
		}
	}

	private class WaveFormPane extends Pane {
		
		public WaveFormPane() {
			//setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.DARKGRAY, null, new Insets(5))));
			//setPadding(new Insets(5));
			
			GridPane pane = new GridPane();
			
			waveChart = new ChannelWaveChart();
			pane.add(waveChart, 0, 1);
			GridPane.setHgrow(waveChart, Priority.ALWAYS);
			getChildren().add(waveChart);
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

		private InputAdministrator inputAdmin = InputAdministrator.getInputAdminstrator();
		private OutputAdministrator outputAdmin = OutputAdministrator.getOutputAdministrator();
		private ChoiceBox<String> choiceBox;
		private String selected;

		public AddRemoveDialog(boolean input, boolean add) {
			DialogPane dialogPane = getDialogPane();

			dialogPane.getButtonTypes().add(ButtonType.APPLY);
			dialogPane.getButtonTypes().add(ButtonType.CANCEL);

			dialogPane.setPrefWidth(270);

			choiceBox = new ChoiceBox<String>();

			if (input && add) {
				inputAdmin.collectSoundInputDevices();
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, INPUT_ADD_TITLE));
				choiceBox.setItems(FXCollections.observableArrayList(inputAdmin.getInputDevices()));
			} else if (input) {
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, INPUT_REMOVE_TITLE));
				choiceBox.setItems(inputPane.getTable().getItems());
			} else if (add) {
				outputAdmin.collectSoundOutputDevices();
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, OUTPUT_ADD_TITLE));
				choiceBox.setItems(FXCollections.observableArrayList(outputAdmin.getOutputDevices()));
			} else {
				setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, OUTPUT_REMOVE_TITLE));
				choiceBox.setItems(outputPane.getTable().getItems());
			}

			Label deviceLabel = new Label(lanHandler.getLocalizedText(AddRemoveDialog.class, DEVICE_LABEL));

			choiceBox.setMaxWidth(Double.MAX_VALUE);
			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.add(deviceLabel, 0, 0);
			gridPane.add(choiceBox, 1, 0);
			GridPane.setHgrow(choiceBox, Priority.ALWAYS);
			GridPane.setHalignment(choiceBox, HPos.LEFT);
			GridPane.setHalignment(deviceLabel, HPos.RIGHT);
			dialogPane.setContent(gridPane);

			Button applyButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
			applyButton.addEventFilter(ActionEvent.ACTION, e -> {
				if ((selected = choiceBox.getSelectionModel().getSelectedItem()) == null) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, EMPTY_ALERT_TITLE));
					alert.setHeaderText(lanHandler.getLocalizedText(AddRemoveDialog.class, EMPTY_ALERT_HEADER));
					alert.showAndWait();
					e.consume();
				} else if (add) {
					ListView<String> table;
					if (input) {
						table = inputPane.getTable();
					} else {
						table = outputPane.getTable();
					}

					Iterator<String> iter = table.getItems().iterator();
					while (iter.hasNext()) {
						if (iter.next().equals(selected)) {
							Alert alert = new Alert(AlertType.WARNING);
							alert.setTitle(lanHandler.getLocalizedText(AddRemoveDialog.class, DUPLICATE_ALERT_TITLE));
							alert.setHeaderText(
									lanHandler.getLocalizedText(AddRemoveDialog.class, DUPLICATE_ALERT_HEADER));
							alert.showAndWait();
							e.consume();
							return;
						}
					}

					if (input) {
						inputPane.addInput(selected);
					} else {
						outputPane.addOutput(selected);
					}
				} else {
					if (input) {
						inputPane.removeInput(selected);
					} else {
						outputPane.removeOutput(selected);
					}
				}
			});

		}

	}

}
