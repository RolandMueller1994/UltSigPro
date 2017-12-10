package gui.menubar;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
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

/**
 * Saves the project settings (channel name, input devices, output devices, ...)
 * in an .usp file. If called for the first time, a path for the file needs to
 * be specified. Afterwards, the file gets automatically overwritten when this
 * {@linkplain MenuItem} is selected.
 * 
 * @author Kone
 *
 */
public class SaveMenuItem extends MenuItem {

	private static final String TITLE = "title";

	public SaveMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(SaveMenuItem.class, TITLE));
		super.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				if (USPFileCreator.getFile() != null) {
					try {
						Document doc = USPFileCreator.collectProjectSettings();
						USPFileCreator.setReferenceDocument(doc);
						USPFileCreator.createUSPFile(doc);
					} catch (ParserConfigurationException | TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						USPFileCreator fileCreator = USPFileCreator.getFileCreator();
						fileCreator.createFile();
						if (USPFileCreator.getFile() != null) {
							Document doc = USPFileCreator.collectProjectSettings();
							USPFileCreator.setReferenceDocument(doc);
							USPFileCreator.createUSPFile(doc);
						}
					} catch (ResourceProviderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TransformerConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}
}
