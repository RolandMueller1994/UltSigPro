package gui;

import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import channel.ChannelConfig;
import channel.ChannelPane;
import channel.gui.PluginConfigGroup;
import channel.gui.SignalFlowConfigException;
import channel.gui.SignalFlowConfigException.SignalFlowErrorCode;
import gui.menubar.AddChannelMenuItem;
import gui.menubar.MenuBarCreator;
import gui.menubar.SaveProjectDialog;
import gui.menubar.USPFileCreator;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import logging.CommonLogger;
import outputhandler.OutputAdministrator;
import plugins.PluginManager;
import plugins.sigproplugins.internal.GainBlock;
import plugins.sigproplugins.internal.GainBlock2;
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
	private static final String SIGNAL_FLOW_ALERT_CHANNEL = "signalFlowAlertChannel";

	private static final String SIGNAL_FLOW_ALERT_TEXT_CONNECTION = "signalFlowAlertTextConnection";

	public static Stage stage;

	private static VBox channelBox;
	private static TabPane pluginPane;
	private static SoundLevelBar soundLevelBar;
	private static HashMap<String, Tab> tabMap = new HashMap<>();
	private static HashMap<ChannelPane, PluginConfigGroup> pluginMap = new HashMap<>();

	private static boolean play = false;

	private static boolean ctrl = false;
	private static boolean shift = false;

	private final double playBackButtonSize = 30;

	private Label startLabel;
	private Label stopLabel;

	/**
	 * This method must be called at startup. The GUI will be set up.
	 */
	public void buildGUI(String[] args) {
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
		PluginManager.getInstance().registerInternSigproPlugin("Gain2", GainBlock2.class);
		// PluginManager.getInstance().registerInternSigproPlugin("AddBlock",
		// SignalAdder.class);

		stage = primaryStage;

		primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ESCAPE)) {
					for (Tab tab : tabMap.values()) {
						((PluginConfigGroup) ((Pane) tab.getContent()).getChildren().get(0)).escapeLineDrawing();
					}
				} else if (event.getCode().equals(KeyCode.DELETE)) {
					for (Tab tab : tabMap.values()) {
						((PluginConfigGroup) ((Pane) tab.getContent()).getChildren().get(0)).deleteLine();
					}
				} else if (event.getCode().equals(KeyCode.CONTROL)) {
					ctrl = false;
				} else if (event.getCode().equals(KeyCode.SHIFT)) {
					shift = false;
				} else if (event.getCode().equals(KeyCode.F1)) {
					Tab selectedTap = pluginPane.getSelectionModel().getSelectedItem();
					if (selectedTap != null) {
						((PluginConfigGroup) ((Pane) selectedTap.getContent()).getChildren().get(0)).fitToScreen();
					}
				}
			}

		});

		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.CONTROL)) {
					ctrl = true;
				} else if (event.getCode().equals(KeyCode.SHIFT)) {
					shift = true;
				}
			}

		});

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				SaveProjectDialog dialog = new SaveProjectDialog();
				dialog.saveProjectDialogAfterCloseRequest(event);
			}
		});

		Image icon = new Image("file:iconNewSmall.png");

		primaryStage.getIcons().add(icon);

		LanguageResourceHandler languageRes = LanguageResourceHandler.getInstance();

		MenuBarCreator menuBarCreator = new MenuBarCreator();
		MenuBar menuBar = menuBarCreator.getMenuBar();

		GridPane topGrid = new GridPane();

		VBox vBox = new VBox();

		topGrid.add(vBox, 0, 0);

		MenuBar buttonMenu = new MenuBar();
		Menu startMenu = new Menu();
		ImageView playButtonImageView = new ImageView(new Image("file:icons/playButtonNew.png"));
		playButtonImageView.setFitHeight(playBackButtonSize);
		playButtonImageView.setFitWidth(playBackButtonSize);
		startLabel = new Label();
		startLabel.setGraphic(playButtonImageView);
		startLabel.setDisable(true);
		startLabel.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (!play) {
					System.gc();

					boolean error = false;
					String errorText = "";

					Iterator<Node> iter = channelBox.getChildren().iterator();
					for (Tab tab : tabMap.values()) {
						PluginConfigGroup configGroup = (PluginConfigGroup) ((Pane) tab.getContent()).getChildren()
								.get(0);
						try {
							configGroup.initializePlay();
						} catch (SignalFlowConfigException e) {

							error = true;

							try {
								errorText += LanguageResourceHandler.getInstance().getLocalizedText(USPGui.class,
										SIGNAL_FLOW_ALERT_CHANNEL) + " "
										+ configGroup.getChannel().getChannelConfig().getName() + ":"
										+ System.lineSeparator();

								System.out.println(e.getErrorCode());

								if ((e.getErrorCode() & SignalFlowErrorCode.INPUT_ERROR.getValue()) != 0) {
									errorText += "- " + LanguageResourceHandler.getInstance().getLocalizedText(
											USPGui.class, SIGNAL_FLOW_ALERT_TEXT_INPUT) + System.lineSeparator();
								}

								if ((e.getErrorCode() & SignalFlowErrorCode.OUTPUT_ERROR.getValue()) != 0) {
									errorText += "- " + LanguageResourceHandler.getInstance().getLocalizedText(
											USPGui.class, SIGNAL_FLOW_ALERT_TEXT_OUTPUT) + System.lineSeparator();
								}

								if ((e.getErrorCode() & SignalFlowErrorCode.CONNECTION_ERROR.getValue()) != 0) {
									errorText += "- " + LanguageResourceHandler.getInstance().getLocalizedText(
											USPGui.class, SIGNAL_FLOW_ALERT_TEXT_CONNECTION) + System.lineSeparator();
								}

								errorText += System.lineSeparator();
							} catch (ResourceProviderException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}

					if (error) {
						Alert signalFlowAlert = new Alert(AlertType.ERROR);

						try {
							signalFlowAlert.setTitle(LanguageResourceHandler.getInstance()
									.getLocalizedText(USPGui.class, SIGNAL_FLOW_ALERT_TITLE));
							signalFlowAlert.setHeaderText(LanguageResourceHandler.getInstance()
									.getLocalizedText(USPGui.class, SIGNAL_FLOW_ALERT_HEADER));

							TextArea contentText = new TextArea(errorText);
							signalFlowAlert.getDialogPane().setContent(contentText);

							signalFlowAlert.showAndWait();
						} catch (ResourceProviderException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return;
					}

					play = true;
					while (iter.hasNext()) {
						((ChannelPane) iter.next()).setPlay(true);
					}
					OutputAdministrator.getOutputAdministrator().startOutput();
					InputAdministrator.getInputAdminstrator().startListening();
					soundLevelBar.setPlay(true);
					startLabel.setDisable(true);
					stopLabel.setDisable(false);
				}
			}

		});
		startMenu.setGraphic(startLabel);

		Menu stopMenu = new Menu();
		ImageView stopButtonImageView = new ImageView(new Image("file:icons/stopButtonNew.png"));
		stopButtonImageView.setFitHeight(playBackButtonSize);
		stopButtonImageView.setFitWidth(playBackButtonSize);
		stopLabel = new Label();
		stopLabel.setGraphic(stopButtonImageView);
		stopLabel.setDisable(true);
		stopLabel.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				stopPlay();
				stopLabel.setDisable(true);
				startLabel.setDisable(false);
			}
		});

		stopMenu.setGraphic(stopLabel);

		buttonMenu.getMenus().addAll(startMenu, stopMenu);

		vBox.getChildren().addAll(menuBar, buttonMenu);

		BorderPane pane = new BorderPane();

		pane.setTop(topGrid);
		GridPane.setHgrow(vBox, Priority.ALWAYS);

		Button addChannelButton = new Button(
				LanguageResourceHandler.getInstance().getLocalizedText(AddChannelMenuItem.class, TITLE),
				new ImageView(new Image("file:icons/channelIcon.png")));
		addChannelButton.setContentDisplay(ContentDisplay.TOP);
		addChannelButton.setOnMouseReleased(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				try {
					new AddChannelMenuItem().fire();
				} catch (ResourceProviderException e) {
					e.printStackTrace();
				}
			}
		});

		// Build Channels
		SplitPane centerSplit = new SplitPane();
		centerSplit.setOrientation(Orientation.VERTICAL);
		pluginPane = new TabPane();
		pluginPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		ScrollPane channelScroll = new ScrollPane();
		channelScroll.setFitToWidth(true);
		channelBox = new VBox();
		channelBox.getChildren().addListener(new ListChangeListener<Node>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Node> c) {
				if (channelBox.getChildren().size() > 0) {
					startLabel.setDisable(false);
					startLabel.getStyleClass().add("controlLabelEnabled");
					pane.setCenter(centerSplit);
					pane.setBottom(soundLevelBar);
				} else {
					startLabel.setDisable(true);
					pane.setCenter(addChannelButton);
					pane.setBottom(null);
				}
			}

		});
		soundLevelBar = SoundLevelBar.getSoundLevelBar();
		channelScroll.setContent(channelBox);
		centerSplit.getItems().addAll(pluginPane, channelScroll);
		pane.setBottom(soundLevelBar);
		pane.setCenter(addChannelButton);

		Scene scene = new Scene(pane);
		scene.getStylesheets().add("file:USPStyleSheet.css");
		primaryStage.setScene(scene);
		primaryStage.setTitle(languageRes.getLocalizedText(USPGui.class, TITLE));
		primaryStage.setMaximized(true);
		primaryStage.show();
		pane.setBottom(null);

		// set initial reference project (here a new blank project)
		// any changes to this, will be noticed and can be saved before closing
		USPFileCreator.setReferenceDocument(USPFileCreator.collectProjectSettings());
	}

	public static boolean isCtrlPressed() {
		return ctrl && !shift;
	}

	public static boolean isCtrlShiftPressed() {
		return ctrl && shift;
	}

	public static boolean isShiftPressed() {
		return shift && !ctrl;
	}

	public static void addChannel(ChannelConfig config) {
		try {
			ChannelPane channelPane = new ChannelPane(config);
			channelBox.getChildren().add(channelPane);
			Tab curTab = new Tab(config.getName());

			ScrollPane scroll = new ScrollPane();
			PluginConfigGroup configGroup = new PluginConfigGroup(channelPane.getChannel(), scroll);
			// scroll.setContent(configGroup);

			Pane parentPane = new Pane();
			parentPane.getChildren().add(configGroup);

			curTab.setContent(parentPane);
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
		pane.getChannel().delete();
	}

	public static void deleteAllChannels() {
		int channelNumber = channelBox.getChildren().size();
		if (channelNumber != 0) {
			ObservableList<Node> pane = channelBox.getChildren();
			for (int i = channelNumber - 1; i > -1; i--) {
				SoundLevelBar.getSoundLevelBar()
						.removeChannelSoundDevices(((ChannelPane) pane.get(i)).getChannelConfig());
				deleteChannel((ChannelPane) pane.get(i));
			}
		}
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

	public static Stage getStage() {
		return stage;
	}
}
