package gui.menubar;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import channel.ChannelConfig;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import resourceframework.ResourceProviderException;

public class AddChannelMenuItem extends MenuItem {

	private static final String TITLE = "title";	
	private static final String ADD_CHANNEL_TITLE = "addChannelTitle";
	
	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	
	private DialogPane dialogPane;
	private AddChannelDialog dialog;
	private GridPane inputPane;
	private GridPane outputPane;
	
	public AddChannelMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(AddChannelMenuItem.class, TITLE));
		
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
		
		private List<ChoiceBox<String>> inputBoxes = new LinkedList<> ();
		private List<ChoiceBox<String>> outputBoxes = new LinkedList<> ();
		private TextField titleTextField;
		
		private GridPane gridPane;
		
		public AddChannelDialog() {

			setTitle(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE));
			
			gridPane = new GridPane();
			
			Label titleLabel = new Label(lanHandler.getLocalizedText(AddChannelDialog.class, TITLE_LABEL) + ":");
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
		}
		
		public ChannelConfig getConfig() {
			return new ChannelConfig(titleTextField.getText(), null, null);
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
				i++;
			}
			
			Button addButton = new Button(lanHandler.getLocalizedText("add"));
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
				i++;
			}
			
			Button addButton = new Button(lanHandler.getLocalizedText("add"));
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
			
			outputPane.add(addButton, 2, 0);
			
			return outputPane;
		}
		
	}
}
