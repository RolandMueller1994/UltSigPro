package gui;

import java.util.HashMap;
import java.util.Iterator;

import channel.ChannelConfig;
import channel.ChannelPane;
import gui.menubar.MenuBarCreator;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logging.CommonLogger;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;

/**
 * This class is a handler for all issues depending on building and executing
 * the graphical user interface.
 * 
 * @author roland
 *
 */
public class USPGui extends Application {

	private static final String TITLE = "title";

	public static Stage stage;

	private static VBox channelBox;
	private static TabPane pluginPane;
	private static SoundLevelBar soundLevelBar;
	private static HashMap<String, Tab> tabMap = new HashMap<>();

	private String[] args;
	private static boolean play = false;

	/**
	 * This method must be called at startup. The GUI will be set up.
	 */
	public void buildGUI(String[] args) {
		this.args = args;
		launch(args);
	}

	@Override
	public void stop() throws Exception {
		// TODO ask the user for confirmation to stop
		stopPlay();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		stage = primaryStage;

		LanguageResourceHandler languageRes = LanguageResourceHandler.getInstance();

		MenuBarCreator menuBarCreator = new MenuBarCreator();
		MenuBar menuBar = menuBarCreator.getMenuBar();

		VBox vBox = new VBox();

		MenuBar buttonMenu = new MenuBar();
		Menu startMenu = new Menu();
		Label startLabel = new Label(languageRes.getLocalizedText("play"));
		startLabel.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (!play) {
					play = true;
					Iterator<Node> iter = channelBox.getChildren().iterator();
					while (iter.hasNext()) {
						((ChannelPane) iter.next()).setPlay(true);
					}
					OutputAdministrator.getOutputAdministrator().startDistribution();
					InputAdministrator.getInputAdminstrator().startListening();
					soundLevelBar.setPlay(true);
				}
			}

		});
		startMenu.setGraphic(startLabel);
		Menu stopMenu = new Menu();
		Label stopLabel = new Label(languageRes.getLocalizedText("stop"));
		stopLabel.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				stopPlay();
			}
		});

		stopMenu.setGraphic(stopLabel);

		buttonMenu.getMenus().addAll(startMenu, stopMenu);

		vBox.getChildren().addAll(menuBar, buttonMenu);

		BorderPane pane = new BorderPane();

		pane.setTop(vBox);

		// Build Channels
		SplitPane centerSplit = new SplitPane();
		centerSplit.setOrientation(Orientation.VERTICAL);
		pluginPane = new TabPane();
		ScrollPane channelScroll = new ScrollPane();
		channelScroll.setFitToWidth(true);
		channelBox = new VBox();
		channelScroll.setContent(channelBox);
		soundLevelBar = SoundLevelBar.getSoundLevelBar();
		centerSplit.getItems().addAll(pluginPane, channelScroll);
		pane.setCenter(centerSplit);
		pane.setBottom(soundLevelBar);

		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.setTitle(languageRes.getLocalizedText(USPGui.class, TITLE));
		primaryStage.setMaximized(true);
		primaryStage.show();

	}

	public static void addChannel(ChannelConfig config) {
		try {
			channelBox.getChildren().add(new ChannelPane(config));
			Tab curTab = new Tab(config.getName());
			tabMap.put(config.getName(), curTab);
			pluginPane.getTabs().add(curTab);
			soundLevelBar.addNewChannelSoundDevices(config);
		} catch (ResourceProviderException e) {
			CommonLogger.getInstance().logException(e);
		}
	}

	public static void deleteChannel(ChannelPane pane) {
		channelBox.getChildren().remove(pane);
		Tab curTab = tabMap.remove(pane.getName());
		pluginPane.getTabs().remove(curTab);
	}

	public static boolean checkIfPresent(String name) {
		Iterator<Node> iter = channelBox.getChildren().iterator();

		while (iter.hasNext()) {
			if (((ChannelPane) iter.next()).getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static void stopExternally() {
		stopPlay();
	}

	private static void stopPlay() {
		if (play) {
			play = false;
			
			soundLevelBar.setPlay(false);
			
			Iterator<Node> iter = channelBox.getChildren().iterator();
			InputAdministrator.getInputAdminstrator().stopListening();
			OutputAdministrator.getOutputAdministrator().stopPlayback();
			while (iter.hasNext()) {
				((ChannelPane) iter.next()).setPlay(false);
			}
		}
	}
}
