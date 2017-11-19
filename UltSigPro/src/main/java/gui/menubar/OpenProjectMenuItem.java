package gui.menubar;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import channel.ChannelPane;
import gui.USPGui;
import gui.soundLevelDisplay.SoundLevelBar;
import i18n.LanguageResourceHandler;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import resourceframework.ResourceProviderException;

public class OpenProjectMenuItem extends MenuItem {

	private static final String TITLE = "title";

	public OpenProjectMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(OpenProjectMenuItem.class, TITLE));
		super.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));

		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();
				LanguageResourceHandler lanHandler;
				try {
					lanHandler = LanguageResourceHandler.getInstance();
					fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
							"UltSigPro " + lanHandler.getLocalizedText("file"), "*.usp"));
				} catch (ResourceProviderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				File file = fileChooser.showOpenDialog(USPGui.stage);
				if (file != null) {
					int channelNumber = USPGui.getChannelBox().getChildren().size();
					if (channelNumber != 0) {
						ObservableList<Node> pane = USPGui.getChannelBox().getChildren();
						for (int i = channelNumber - 1; i > -1; i--) {
							SoundLevelBar.getSoundLevelBar().removeChannelSoundDevices(((ChannelPane) pane.get(i)).getChannelConfig());
							USPGui.deleteChannel((ChannelPane) pane.get(i));
						}
					}

					try {
						USPFileReader.getUSPFileReader().readUSPFile(file);
					} catch (ParserConfigurationException | SAXException | IOException | InstantiationException
							| IllegalAccessException | ResourceProviderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}
}
