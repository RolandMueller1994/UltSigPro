package plugins;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import gui.USPGui;
import gui.menubar.AddChannelMenuItem;
import i18n.LanguageResourceHandler;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import resourceframework.ResourceProviderException;

public class PluginManagerMenuItem extends MenuItem {

	private static final String TITLE = "title";
	
	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	
	private PluginManagerDialog dialog;
	
	public PluginManagerMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(PluginManagerMenuItem.class, TITLE));
		
		super.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				dialog = new PluginManagerDialog();
				dialog.showAndWait();
			}
		});
	}
	
	private class PluginManagerDialog extends Dialog<ButtonType> {
		
		private static final String TITLE = "title";
		private static final String COMMON = "common";
		private static final String SIGPRO = "sigpro";
		
		private static final double BUTTON_WIDTH = 35;
		private static final double LIST_HEIGHT = 200;
		
		
		private DialogPane dialogPane;
		private ListView<String> commonList = new ListView<> ();
		private ListView<String> sigproList = new ListView<> ();
		private Button addCommonButton = new Button("+");
		private Button removeCommonButton = new Button("-");
		private Button addSigproButton = new Button("+");
		private Button removeSigproButton = new Button("-");
		private Label commonTextField = new Label(lanHandler.getLocalizedText(PluginManagerDialog.class, COMMON));
		private Label sigproTextField = new Label(lanHandler.getLocalizedText(PluginManagerDialog.class, SIGPRO));
		private ScrollPane commonScrollPane = new ScrollPane();
		private ScrollPane sigproScrollPane = new ScrollPane();
		
		private GridPane gridPane;
		
		public PluginManagerDialog() {
			setTitle(lanHandler.getLocalizedText(PluginManagerDialog.class, TITLE));
			
			dialogPane = getDialogPane();
			dialogPane.getButtonTypes().add(ButtonType.CLOSE);
					
			addCommonButton.setMaxWidth(Double.MAX_VALUE);
			addCommonButton.setMinWidth(BUTTON_WIDTH);
			removeCommonButton.setMaxWidth(Double.MAX_VALUE);
			removeCommonButton.setMinWidth(BUTTON_WIDTH);
			addSigproButton.setMaxWidth(Double.MAX_VALUE);
			addSigproButton.setMinWidth(BUTTON_WIDTH);
			removeSigproButton.setMaxWidth(Double.MAX_VALUE);
			removeSigproButton.setMinWidth(BUTTON_WIDTH);
			
			commonList.setPrefHeight(LIST_HEIGHT);
			sigproList.setPrefHeight(LIST_HEIGHT);
			commonScrollPane.setContent(commonList);
			sigproScrollPane.setContent(sigproList);
			
			gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);
			
			gridPane.add(commonTextField, 0, 0);
			gridPane.add(addCommonButton, 1, 0);
			gridPane.add(removeCommonButton, 2, 0);
			gridPane.add(commonScrollPane, 0, 1, 3, 1);
			
			gridPane.add(sigproTextField, 0, 2);
			gridPane.add(addSigproButton, 1, 2);
			gridPane.add(removeSigproButton, 2, 2);
			gridPane.add(sigproScrollPane, 0, 3, 3, 1);
			
			GridPane.setHgrow(commonScrollPane, Priority.ALWAYS);
			GridPane.setHgrow(sigproScrollPane, Priority.ALWAYS);
			
			removeCommonButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					ObservableList<String> selectionList = commonList.getSelectionModel().getSelectedItems();
					Iterator<String> iter = selectionList.iterator();
					while(iter.hasNext()) {
						PluginManager.getInstance().removeCommonPlugin(iter.next());
					}
					initializeCommonList();
				}
				
			});
			
			removeSigproButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					ObservableList<String> selectionList = sigproList.getSelectionModel().getSelectedItems();
					Iterator<String> iter = selectionList.iterator();
					while(iter.hasNext()) {
						PluginManager.getInstance().removeSigproPlugin(iter.next());
					}
					initializeSigprolist();
				}
				
			});
			
			addCommonButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					FileChooser fileChooser = new FileChooser();
					
					File plugin = fileChooser.showOpenDialog(USPGui.stage);
					
					if(plugin != null) {
						try {
							PluginManager.getInstance().importCommonPlugin(plugin.toPath());
						} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						initializeCommonList();						
					}
				}
				
			});
			
			addSigproButton.setOnAction(new EventHandler<ActionEvent> () {

				@Override
				public void handle(ActionEvent event) {
					FileChooser fileChooser = new FileChooser();
					
					File plugin = fileChooser.showOpenDialog(USPGui.stage);
					
					if(plugin != null) {
						try {
							PluginManager.getInstance().importSigproPlugin(plugin.toPath());;
						} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						initializeSigprolist();						
					}
				}
				
			});
			
			initializeCommonList();
			initializeSigprolist();
			
			dialogPane.setContent(gridPane);
		}
		
		private void initializeCommonList() {
			commonList.getItems().clear();
			for(String plugin : PluginManager.getInstance().getAvailableCommonPlugins()) {
				commonList.getItems().add(plugin);
			}
		}
		
		private void initializeSigprolist() {
			sigproList.getItems().clear();
			for(String plugin : PluginManager.getInstance().getAvailableSigproPlugins()) {
				sigproList.getItems().add(plugin);
			}
		}
		
		
	}
	
}
