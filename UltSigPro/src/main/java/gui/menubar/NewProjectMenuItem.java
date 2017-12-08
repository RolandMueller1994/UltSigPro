package gui.menubar;

import javax.xml.parsers.ParserConfigurationException;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
					USPFileCreator.setFile(null);
				} catch (ResourceProviderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				USPGui.deleteAllChannels();
				try {
					USPFileCreator.setReferenceDocument(USPFileCreator.collectProjectSettings());
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

}
