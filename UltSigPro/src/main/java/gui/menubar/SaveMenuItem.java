package gui.menubar;

import java.io.File;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
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
import javafx.stage.FileChooser;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;

public class SaveMenuItem extends MenuItem {

	private static final String TITLE = "title";

	public SaveMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(SaveMenuItem.class, TITLE));

		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LanguageResourceHandler lanHandler;
				try {
					lanHandler = LanguageResourceHandler.getInstance();

					GlobalResourceProvider prov = GlobalResourceProvider.getInstance();
					if (prov.checkRegistered("projectFile")) {
						File file = (File) prov.getResource("projectFile");
						collectProjectSettings(file);

					} else {
						FileChooser fileChooser = new FileChooser();
						fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
								"UltSigPro " + lanHandler.getLocalizedText("file"), "*.usp"));
						File file = fileChooser.showSaveDialog(USPGui.stage);

						if (file != null) {
							// TODO create USP file with project settings
							prov.registerResource("projectFile", file);
							collectProjectSettings(file);

						}
					}

				} catch (ResourceProviderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private void collectProjectSettings(File file) {
		try {

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

				// output device element
				ObservableList<String> outputDevices = curElement.getOutputPaneTableItems();
				for (String outputDevice : outputDevices) {
					Element outputDeviceName = doc.createElement("outputDevice");
					outputDeviceName.appendChild(doc.createTextNode(outputDevice));
					channelName.appendChild(outputDeviceName);
				}
			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(file);

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}

}
