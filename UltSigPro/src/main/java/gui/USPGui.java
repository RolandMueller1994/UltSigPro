package gui;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

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
import gui.menubar.SaveMenuItem;
import gui.menubar.USPFileCreator;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
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
	private static final String SIGNAL_FLOW_ALERT_CHANNEL = "signalFlowAlertChannel";

	private static final String SIGNAL_FLOW_ALERT_TEXT_CONNECTION = "signalFlowAlertTextConnection";

	public static Stage stage;

	private static VBox channelBox;
	private static TabPane pluginPane;
	private static SoundLevelBar soundLevelBar;
	private static HashMap<String, Tab> tabMap = new HashMap<>();
	private static HashMap<ChannelPane, PluginConfigGroup> pluginMap = new HashMap<>();

	private String[] args;
	private static boolean play = false;

	private static boolean ctrl = false;
	private static boolean shift = false;

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
					 if(selectedTap != null) {
						 ((PluginConfigGroup) ((Pane) selectedTap.getContent())
								 .getChildren().get(0)).fitToScreen();						 
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
			// TODO check if there are any changes made to this project
			// if not -> don't ask to save project
			// if yes -> ask to save the project

			@Override
			public void handle(WindowEvent event) {
				SaveProjectBeforeClosingDialog dialog = new SaveProjectBeforeClosingDialog();
				Optional<ButtonType> result = dialog.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.YES) {
					if (USPFileCreator.getFile() != null) {
						try {
							Document doc = USPFileCreator.collectProjectSettings();
							USPFileCreator.createUSPFile(doc);
						} catch (ParserConfigurationException | TransformerException e) {
							e.printStackTrace();
						}
					} else {
						try {
							USPFileCreator fileCreator = USPFileCreator.getFileCreator();
							fileCreator.createFile();
							if (USPFileCreator.getFile() != null) {
								Document doc = USPFileCreator.collectProjectSettings();
								USPFileCreator.createUSPFile(doc);
							} else {
								event.consume();
							}
						} catch (ResourceProviderException | TransformerException | ParserConfigurationException e) {
							e.printStackTrace();
						}
					}
				} else if (result.isPresent() && result.get() == ButtonType.NO) {

				} else if (result.isPresent() && result.get() == ButtonType.CANCEL) {
					event.consume();
				}
			}
		});

		Image icon = new Image("file:iconNew.png");

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

	private class SaveProjectBeforeClosingDialog extends Dialog<ButtonType> {

		private static final String TITLE = "title";
		private static final String HEADER = "header";

		private SaveProjectBeforeClosingDialog() {

			initOwner(USPGui.stage);
			initStyle(StageStyle.UTILITY);
			try {
				setTitle(LanguageResourceHandler.getInstance().getLocalizedText(SaveProjectBeforeClosingDialog.class,
						TITLE));
				setHeaderText(LanguageResourceHandler.getInstance()
						.getLocalizedText(SaveProjectBeforeClosingDialog.class, HEADER));
			} catch (ResourceProviderException e) {
				e.printStackTrace();
			}
			getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
			getDialogPane().getButtonTypes().add(ButtonType.YES);
			getDialogPane().getButtonTypes().add(ButtonType.NO);

		}
	}
}
