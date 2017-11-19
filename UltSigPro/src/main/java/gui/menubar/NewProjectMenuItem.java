package gui.menubar;

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
import resourceframework.ResourceProviderException;

public class NewProjectMenuItem extends MenuItem {

	private static final String TITLE = "title";

	public NewProjectMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(NewProjectMenuItem.class, TITLE));
		super.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
		super.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				LanguageResourceHandler lanHandler;
				try {
					lanHandler = LanguageResourceHandler.getInstance();
					USPGui.getStage().setTitle(lanHandler.getLocalizedText(USPGui.class, "title"));
				} catch (ResourceProviderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				int channelNumber = USPGui.getChannelBox().getChildren().size();
				if (channelNumber != 0) {
					ObservableList<Node> pane = USPGui.getChannelBox().getChildren();
					for (int i = channelNumber - 1; i > -1; i--) {
						SoundLevelBar.getSoundLevelBar().removeChannelSoundDevices(((ChannelPane) pane.get(i)).getChannelConfig());
						USPGui.deleteChannel((ChannelPane) pane.get(i));
					}
				}
			}
		});
	}

}
