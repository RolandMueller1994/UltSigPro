package gui.menubar;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Optional;
import java.util.Set;

import channel.ChannelConfig;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import inputHandler.InputAdministrator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import resourceframework.ResourceProviderException;

public class AddChannelMenuItem extends MenuItem {

	private static final String TITLE = "title";	
	private static final String ADD_CHANNEL_TITLE = "addChannelTitle";
	
	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	private InputAdministrator inputAdmin = InputAdministrator.getInputAdminstrator();
	
	private DialogPane dialogPane;
	private AddChannelDialog dialog;
	private GridPane inputPane;
	private GridPane outputPane;
	
	public AddChannelMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(AddChannelMenuItem.class, TITLE));
		
		inputAdmin.collectSoundInputDevices();
		
		super.setOnAction(new EventHandler<ActionEvent> () {

			@Override
			public void handle(ActionEvent event) {
				
				dialog = new AddChannelDialog();
				
				Optional<ButtonType> result = dialog.showAndWait();
				
				if(result.isPresent() && result.get() == ButtonType.OK) {
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
		
		private static final double IN_OUT_WIDTH = 70;
		private static final String WARNING = "warning";
		private static final String PRESENT_WARNING = "presentWarning";
		
		private List<ChoiceBox<String>> inputBoxes = new LinkedList<> ();
		private List<ChoiceBox<String>> outputBoxes = new LinkedList<> ();
		private TextField titleTextField;
		
		private GridPane gridPane;
		
		public AddChannelDialog() {

			setTitle(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE));
			
			gridPane = new GridPane();
			
			Label titleLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE_LABEL) + ": *");
			titleTextField = new TextField();
			
			inputBoxes.add(new ChoiceBox<String>());
			outputBoxes.add(new ChoiceBox<String> ());
			
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
				if(titleTextField.getText().isEmpty()) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setHeaderText(lanHandler.getLocalizedText(AddChannelDialog.class, WARNING));
					alert.showAndWait();
					e.consume();
				} else if(USPGui.checkIfPresent(titleTextField.getText())) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setHeaderText(lanHandler.getLocalizedText(AddChannelDialog.class, PRESENT_WARNING));
					alert.showAndWait();
					e.consume();
				}
			});
		}
		
		public ChannelConfig getConfig() {
			List<String> inputDevices = new LinkedList<> ();
			
			for(ChoiceBox<String> choiceBox : inputBoxes) {
				String cur;
				if((cur = choiceBox.getSelectionModel().getSelectedItem()) != null) {
					inputDevices.add(cur);
				}
			}
			return new ChannelConfig(titleTextField.getText(), inputDevices, null);
		}
		
		private GridPane getInputPane() {
			inputPane = new GridPane();
			
			//inputPane.setPadding(new Insets(5));
			inputPane.setHgap(5);
			inputPane.setVgap(5);
			
			Label inputLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, INPUT_LABEL) + ":");
			
			inputPane.add(inputLabel, 0, 0);
			
			int i=0;
			for(ChoiceBox<String> choiceBox : inputBoxes) {
				GridPane.setHgrow(choiceBox, Priority.ALWAYS);
				choiceBox.setMaxWidth(Double.MAX_VALUE);
				inputPane.add(choiceBox, 1, i);
				choiceBox.setItems(FXCollections.observableArrayList(inputAdmin.getInputDevices()));
				i++;
			}
			
			Button addButton = new Button(lanHandler.getLocalizedText("add"));
			Button removeButton = new Button(lanHandler.getLocalizedText("remove"));
			GridPane.getHgrow(removeButton);
			removeButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					inputBoxes.add(new ChoiceBox<String>());
					gridPane.getChildren().remove(inputPane);
					inputPane = getInputPane();
					gridPane.add(inputPane, 0, 1, 2, 1);
					inputPane.getColumnConstraints().add(0, new ColumnConstraints(IN_OUT_WIDTH));
					dialogPane.getScene().getWindow().sizeToScene();
				}
				
			});
			removeButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					inputBoxes.remove(inputBoxes.size() - 1);
					gridPane.getChildren().remove(inputPane);
					inputPane = getInputPane();
					gridPane.add(inputPane, 0, 1, 2, 1);
					inputPane.getColumnConstraints().add(0, new ColumnConstraints(IN_OUT_WIDTH));
					dialogPane.getScene().getWindow().sizeToScene();
				}
				
			});
			if(inputBoxes.size() > 1) {
				inputPane.add(removeButton, 2, 1);
			} 
			inputPane.add(addButton, 2, 0);
			
			return inputPane;
		}
		
		private GridPane getOutputPane() {
			outputPane = new GridPane();			
			
			//outputPane.setPadding(new Insets(5));
			outputPane.setHgap(5);
			outputPane.setVgap(5);
			
			Label outputLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, OUTPUT_LABEL) + ":");
			
			outputPane.add(outputLabel, 0, 0);
			
			int i=0;
			for(ChoiceBox<String> choiceBox : outputBoxes) {
				GridPane.setHgrow(choiceBox, Priority.ALWAYS);
				choiceBox.setMaxWidth(Double.MAX_VALUE);
				outputPane.add(choiceBox, 1, i);
				choiceBox.setItems(FXCollections.observableArrayList(inputAdmin.getInputDevices()));
				i++;
			}
			
			Button addButton = new Button(lanHandler.getLocalizedText("add"));
			Button removeButton = new Button(lanHandler.getLocalizedText("remove"));
			GridPane.getHgrow(removeButton);
			removeButton.setMaxWidth(Double.MAX_VALUE);
			addButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					outputBoxes.add(new ChoiceBox<String>());
					gridPane.getChildren().remove(outputPane);
					outputPane = getOutputPane();
					gridPane.add(outputPane, 0, 2, 2, 1);
					outputPane.getColumnConstraints().add(0, new ColumnConstraints(IN_OUT_WIDTH));
					dialogPane.getScene().getWindow().sizeToScene();
				}
				
			});
			if(outputBoxes.size() > 1) {
				outputPane.add(removeButton, 2, 1);
			} 
			outputPane.add(addButton, 2, 0);
			
			return outputPane;
		}
		
	}
}
