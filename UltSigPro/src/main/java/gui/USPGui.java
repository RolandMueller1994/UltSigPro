package gui;

import channel.ChannelConfig;
import channel.ChannelPane;
import gui.menubar.MenuBarCreator;
import i18n.LanguageResourceHandler;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logging.CommonLogger;
import resourceframework.ResourceProviderException;

/**
 * This class is a handler for all issues depending on building and executing the graphical user interface.
 * 
 * @author roland
 *
 */
public class USPGui extends Application {
	
	private static final String TITLE = "title";
	
	private static VBox channelBox;
	
	private String[] args;
	
	/**
	 * This method must be called at startup. The GUI will be set up.
	 */
	public void buildGUI(String[] args) {
		this.args = args;
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		LanguageResourceHandler languageRes = LanguageResourceHandler.getInstance();
		
		MenuBarCreator menuBarCreator = new MenuBarCreator();
		MenuBar menuBar = menuBarCreator.getMenuBar();
		
		VBox vBox = new VBox();
		
		MenuBar buttonMenu = new MenuBar();
		Menu startMenu = new Menu();
		Label startLabel = new Label(languageRes.getLocalizedText("play"));
		startLabel.setOnMousePressed(new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				
			}
			
		});
		startMenu.setGraphic(startLabel);		
		Menu stopMenu = new Menu();
		Label stopLabel = new Label(languageRes.getLocalizedText("stop"));
		stopLabel.setOnMousePressed(new EventHandler<MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {
				
				
			}		
		});
		
		buttonMenu.getMenus().addAll(startMenu, stopMenu);
		
		vBox.getChildren().addAll(menuBar, buttonMenu);
		
		BorderPane pane = new BorderPane();
		
		pane.setTop(vBox);
		
		// Build Channels
		ScrollPane channelScroll = new ScrollPane();
		channelScroll.setFitToWidth(true);
		channelBox = new VBox();
		channelScroll.setContent(channelBox);
		pane.setCenter(channelScroll);
		
		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.setTitle(languageRes.getLocalizedText(USPGui.class, TITLE));
		primaryStage.setMaximized(true);
		primaryStage.show();
		
	}
	
	public static void addChannel(ChannelConfig config) {
		try {
			channelBox.getChildren().add(new ChannelPane(config));
		} catch (ResourceProviderException e) {
			CommonLogger.getInstance().logException(e);
		}
	}
	
	public static void deleteChannel(ChannelPane pane) {
		channelBox.getChildren().remove(pane);
	}
	
}
