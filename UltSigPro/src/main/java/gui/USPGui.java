package gui;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import channel.Channel;
import channel.ChannelConfig;
import channel.ChannelPane;
import channel.gui.Input;
import channel.gui.Output;
import channel.gui.PluginConfigGroup;
import channel.gui.SignalFlowConfigException;
import channel.gui.SignalFlowConfigException.SignalFlowErrorCode;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import plugins.sigproplugins.SigproPlugin;
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

	private static final String SIGNAL_FLOW_ALERT_TITLE = "signalFlowAlertTitle";
	private static final String SIGNAL_FLOW_ALERT_HEADER = "signalFlowAlertHeader";
	private static final String SIGNAL_FLOW_ALERT_TEXT_INPUT = "signalFlowAlertTextInput";
	private static final String SIGNAL_FLOW_ALERT_TEXT_OUTPUT = "signalFlowAlertTextOutput";

	private static final String SIGNAL_FLOW_ALERT_TEXT_CONNECTION = "signalFlowAlertTextConnection";

	public static Stage stage;

	private static VBox channelBox;
	private static TabPane pluginPane;
	private static SoundLevelBar soundLevelBar;
	private static HashMap<String, Tab> tabMap = new HashMap<>();
	private static HashMap<ChannelPane, PluginConfigGroup> pluginMap = new HashMap<>();

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
		// PluginManager.getInstance().registerInternSigproPlugin("AddBlock",
		// SignalAdder.class);

		stage = primaryStage;

		primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ESCAPE)) {
					for (Tab tab : tabMap.values()) {
						((PluginConfigGroup) ((ScrollPane) tab.getContent()).getContent()).escapeLineDrawing();
					}
				} else if (event.getCode().equals(KeyCode.DELETE)) {
					for (Tab tab : tabMap.values()) {
						((PluginConfigGroup) ((ScrollPane) tab.getContent()).getContent()).deleteLine();
					}
				}
			}

		});

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

					Iterator<Node> iter = channelBox.getChildren().iterator();
					for (Tab tab : tabMap.values()) {
						try {
							((PluginConfigGroup) ((ScrollPane) tab.getContent()).getContent()).initializePlay();
						} catch (SignalFlowConfigException e) {

							Alert signalFlowAlert = new Alert(AlertType.ERROR);
							try {

								signalFlowAlert.setTitle(LanguageResourceHandler.getInstance()
										.getLocalizedText(USPGui.class, SIGNAL_FLOW_ALERT_TITLE));
								signalFlowAlert.setHeaderText(LanguageResourceHandler.getInstance()
										.getLocalizedText(USPGui.class, SIGNAL_FLOW_ALERT_HEADER));

								String text = "";

								System.out.println(e.getErrorCode());

								if ((e.getErrorCode() & SignalFlowErrorCode.INPUT_ERROR.getValue()) != 0) {
									text += "- " + LanguageResourceHandler.getInstance().getLocalizedText(USPGui.class,
											SIGNAL_FLOW_ALERT_TEXT_INPUT) + System.lineSeparator();
								}

								if ((e.getErrorCode() & SignalFlowErrorCode.OUTPUT_ERROR.getValue()) != 0) {
									text += "- " + LanguageResourceHandler.getInstance().getLocalizedText(USPGui.class,
											SIGNAL_FLOW_ALERT_TEXT_OUTPUT) + System.lineSeparator();
								}

								if ((e.getErrorCode() & SignalFlowErrorCode.CONNECTION_ERROR.getValue()) != 0) {
									text += "- " + LanguageResourceHandler.getInstance().getLocalizedText(USPGui.class,
											SIGNAL_FLOW_ALERT_TEXT_CONNECTION) + System.lineSeparator();
								}

								TextArea contentText = new TextArea(text);
								signalFlowAlert.getDialogPane().setContent(contentText);

								signalFlowAlert.showAndWait();
							} catch (ResourceProviderException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							return;
						}
					}

					play = true;
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
			pluginPane.getSelectionModel().select(curTab);
			soundLevelBar.addNewChannelSoundDevices(config);
			pluginMap.put(channelPane, configGroup);
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

	public static VBox getChannelBox() {
		return channelBox;
	}

	public static TabPane getPluginPane() {
		return pluginPane;
	}

	public static void collectPluginConfig(Document doc, Element element, ChannelPane pane) {
		pluginMap.get(pane).collectPluginInfos(doc, element);
	}

	public static PluginConfigGroup getPluginConfigGroup(ChannelPane pane) {
		return pluginMap.get(pane);
	}
}
