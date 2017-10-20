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
				LanguageResourceHandler lanHandler;
				try {
					lanHandler = LanguageResourceHandler.getInstance();

					GlobalResourceProvider prov = GlobalResourceProvider.getInstance();
					if (prov.checkRegistered("projectFile")) {
						File file = (File) prov.getResource("projectFile");
						Document doc = collectProjectSettings();
						createXMLFile(doc, file);

					} else {
						FileChooser fileChooser = new FileChooser();
						fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
								"UltSigPro " + lanHandler.getLocalizedText("file"), "*.usp"));
						File file = fileChooser.showSaveDialog(USPGui.stage);

						if (file != null) {
							prov.registerResource("projectFile", file);
							Document doc = collectProjectSettings();
							createXMLFile(doc, file);
						}
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
		});
	}

	/**
	 * Creates an XML file at the specified path with the given text and
	 * structure.
	 * 
	 * @param doc
	 *            Contains the structure and elements which have to be written
	 *            into the file.
	 * @param file
	 *            Is the file with the file name and path.
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	private void createXMLFile(Document doc, File file) throws TransformerConfigurationException, TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
	}

	/**
	 * Collects the informations of every {@linkplain Channel} and builds the
	 * structure for an XML file.
	 * 
	 * @return The structure with the values for the XML file.
	 * @throws ParserConfigurationException
	 */
	private Document collectProjectSettings() throws ParserConfigurationException {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// root elements
		Element rootElement = doc.createElement("UspProjectFile");
		doc.appendChild(rootElement);

		Iterator<Node> iterChannelName = USPGui.getChannelBox().getChildren().iterator();
		while (iterChannelName.hasNext()) {
			ChannelPane curElement = (ChannelPane) iterChannelName.next();
			// channel elements
			Element channelName = doc.createElement(curElement.getName());
			rootElement.appendChild(channelName);

			// set attribute to channelName element
			// channelName.setAttribute("id", "1");

			// input device elements
			ObservableList<String> inputDevices = curElement.getInputPaneTableItems();
			for (String inputDevice : inputDevices) {
				Element inputDeviceName = doc.createElement("inputDevice");
				inputDeviceName.appendChild(doc.createTextNode(inputDevice));
				channelName.appendChild(inputDeviceName);
			}

			// output device elements
			ObservableList<String> outputDevices = curElement.getOutputPaneTableItems();
			for (String outputDevice : outputDevices) {
				Element outputDeviceName = doc.createElement("outputDevice");
				outputDeviceName.appendChild(doc.createTextNode(outputDevice));
				channelName.appendChild(outputDeviceName);
			}
			
			// plugin elements
			HashMap<String, Class<SigproPlugin>> plugins = PluginManager.getInstance().getSigproLoader().getInternalPluginMap();
			for (Map.Entry<String, Class<SigproPlugin>> plugin : plugins.entrySet()) {
				Element pluginElement = doc.createElement("plugin");
				Element pluginName = doc.createElement("name");
				Element pluginClass = doc.createElement("class");
				pluginName.appendChild(doc.createTextNode(plugin.getKey()));
				pluginClass.appendChild(doc.createTextNode(plugin.getValue().toString()));
				pluginElement.appendChild(pluginName);
				pluginElement.appendChild(pluginClass);
				channelName.appendChild(pluginElement);
			}
		}
		return doc;
	}

}
