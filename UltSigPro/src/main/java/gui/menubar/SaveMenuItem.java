package gui.menubar;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import channel.ChannelPane;
import gui.USPGui;
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
import pluginframework.PluginInterface;
import pluginframework.PluginLoader;
import plugins.PluginManager;
import plugins.sigproplugins.SigproPlugin;
import resourceframework.GlobalResourceProvider;
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

				if (XMLFileCreator.getFile() != null) {
					try {
						Document doc = XMLFileCreator.collectProjectSettings();
						XMLFileCreator.createXMLFile(doc, XMLFileCreator.getFile());
					} catch (ParserConfigurationException | TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						XMLFileCreator fileCreator = XMLFileCreator.getFileCreator();
						fileCreator.createFile();
						if (XMLFileCreator.getFile() != null) {
							Document doc = XMLFileCreator.collectProjectSettings();
							XMLFileCreator.createXMLFile(doc, XMLFileCreator.getFile());
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
