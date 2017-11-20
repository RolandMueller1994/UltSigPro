package gui.menubar;

import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import resourceframework.ResourceProviderException;

public class SaveAsMenuItem extends MenuItem {

	private static final String TITLE = "title";
	
	public SaveAsMenuItem() throws ResourceProviderException {		
		super(LanguageResourceHandler.getInstance().getLocalizedText(SaveAsMenuItem.class, TITLE));	
		super.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					File file = USPFileCreator.getFileCreator().createFile();
					if (file != null) {
						Document doc = USPFileCreator.collectProjectSettings();
						USPFileCreator.createUSPFile(doc);
					}
				} catch (ResourceProviderException | ParserConfigurationException | TransformerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
}
