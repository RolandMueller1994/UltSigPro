package gui;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;

import channel.ChannelConfig;
import channel.ChannelPane;
import channel.gui.Input;
import channel.gui.Output;
import channel.gui.PluginConfigGroup;
import gui.menubar.MenuBarCreator;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logging.CommonLogger;
import outputhandler.OutputAdministrator;
import plugins.PluginManager;
import plugins.sigproplugins.internal.GainBlock;
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

		// Initialize plugin manager
		PluginManager.getInstance();
		
		// Register internal plugins
		PluginManager.getInstance().registerInternSigproPlugin("Gain", GainBlock.class);
		//PluginManager.getInstance().registerInternSigproPlugin("AddBlock", SignalAdder.class);
		
		stage = primaryStage;
		
		Image icon = new Image("file:icon.png");
		
		primaryStage.getIcons().add(icon);

		LanguageResourceHandler languageRes = LanguageResourceHandler.getInstance();

		MenuBarCreator menuBarCreator = new MenuBarCreator();
		MenuBar menuBar = menuBarCreator.getMenuBar();
		
		GridPane topGrid = new GridPane();
		
		VBox vBox = new VBox();
		ImageView iconView = new ImageView(icon);
		
		topGrid.add(vBox, 0, 0);

		MenuBar buttonMenu = new MenuBar();
		Menu startMenu = new Menu();
		Label startLabel = new Label(languageRes.getLocalizedText("play"));
		startLabel.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (!play) {
					System.gc();
					play = true;
					Iterator<Node> iter = channelBox.getChildren().iterator();
					while (iter.hasNext()) {
						((ChannelPane) iter.next()).setPlay(true);
					}
					OutputAdministrator.getOutputAdministrator().startOutput();
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

		pane.setTop(topGrid);
		GridPane.setHgrow(vBox, Priority.ALWAYS);

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
			ChannelPane channelPane = new ChannelPane(config);
			channelBox.getChildren().add(channelPane);
			Tab curTab = new Tab(config.getName());
			
			ScrollPane scroll = new ScrollPane();
			PluginConfigGroup configGroup = new PluginConfigGroup(channelPane.getChannel(), scroll);
			scroll.setContent(configGroup);
			
			curTab.setContent(scroll);
			
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
	
	public static VBox getChannelBox () {
		return channelBox;
	}
}
