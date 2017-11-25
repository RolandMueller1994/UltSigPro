package gui.menubar;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import channel.ChannelConfig;
import channel.ChannelPane;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import inputhandler.InputAdministrator;
import javafx.scene.control.Alert;
import outputhandler.OutputAdministrator;
import resourceframework.ResourceProviderException;

public class USPFileReader {

	private static USPFileReader fileReader;

	public static USPFileReader getUSPFileReader() {

		if (fileReader == null) {
			fileReader = new USPFileReader();
		}

		return fileReader;
	}

	private USPFileReader() {

	}

	public void readUSPFile(File file) throws ParserConfigurationException, SAXException, IOException,
			InstantiationException, IllegalAccessException, ResourceProviderException {

		// add file name in the header line
		LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
		USPGui.getStage().setTitle(lanHandler.getLocalizedText(USPGui.class, "title") + " - " + file.getName());

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(file);

		// read all channel entries
		NodeList nodeList = document.getElementsByTagName("channel");
		for (int i = 0; i < nodeList.getLength(); i++) {

			Node channel = nodeList.item(i);
			String channelName = new String();
			List<String> inputDevices = new LinkedList<>();
			List<String> outputDevices = new LinkedList<>();
			HashMap<String, File> choosedInputWaveFiles = new HashMap<>();
			HashMap<String, File> choosedOutputWaveFiles = new HashMap<>();
			List<String> plugins = new LinkedList<>();
			List<String> missingResources = new LinkedList<>();

			if (channel.getNodeType() == Node.ELEMENT_NODE) {
				Element entry = (Element) channel;
				NodeList entries = entry.getChildNodes();

				// entries for each channel (inputDevice, outputDevice, ...)
				for (int j = 0; j < entries.getLength(); j++) {
					Node channelEntryNode = entries.item(j);
					if (channelEntryNode.getNodeType() == Node.ELEMENT_NODE) {
						Element channelItemElement = (Element) channelEntryNode;
						String tagName = channelItemElement.getTagName();

						// search for channel name, inputDevices, outputDevices,
						// plugins...
						if (tagName == "name") {
							channelName = channelItemElement.getTextContent();
						} else if (tagName == "inputDevice") {
							if (InputAdministrator.getInputAdminstrator().deviceAvailable(channelItemElement.getTextContent())) {
								inputDevices.add(channelItemElement.getTextContent());
							} else {
								missingResources.add(channelItemElement.getTextContent());
							}
						} else if (tagName == "outputDevice") {
							if (OutputAdministrator.getOutputAdministrator().deviceAvailable(channelItemElement.getTextContent())) {
								outputDevices.add(channelItemElement.getTextContent());
							} else {
								missingResources.add(channelItemElement.getTextContent());
							}
						} else if (tagName == "inputWave") {
							File inputFile = new File(channelItemElement.getTextContent());
							if (inputFile.exists()) {
								choosedInputWaveFiles.put(inputFile.getName(), inputFile.getAbsoluteFile());
							} else {
								missingResources.add(inputFile.getAbsolutePath());
							}
						} else if (tagName == "outputWave") {
							File outputFile = new File(channelItemElement.getTextContent());
							if (outputFile.exists()) {
								choosedOutputWaveFiles.put(outputFile.getName(), outputFile.getAbsoluteFile());
							} else {
								missingResources.add(outputFile.getAbsolutePath());
							}
						} else if (tagName == "plugin") {
							NodeList pluginEntryNodeList = channelItemElement.getChildNodes();
							for (int k = 0; k < pluginEntryNodeList.getLength(); k++) {
								Node pluginNode = pluginEntryNodeList.item(k);
								if (pluginNode.getNodeType() == Node.ELEMENT_NODE) {
									Element pluginElement = (Element) pluginNode;
									if (pluginElement.getTagName() == "name") {
										plugins.add(pluginElement.getTextContent());
									} else if (pluginElement.getTagName() == "position") {
										// TODO collect further informations,
										// e.g. plugin position
									}
								}
							}
						}
					}
				}
			}
			USPGui.addChannel(new ChannelConfig(channelName, inputDevices, outputDevices, choosedInputWaveFiles,
					choosedOutputWaveFiles));
			ChannelPane pane = (ChannelPane) USPGui.getChannelBox().getChildren().get(i);
			for (String plugin : plugins) {
				if (!plugin.equals("Output") && !plugin.equals("Input")) {
					USPGui.getPluginConfigGroup(pane).createPluginFromProjectFile(plugin, 50, 50);
				}
			}
			
			if (missingResources.size() != 0) {
				new MissingResourcesDialog(missingResources);
			}

		}
	}
	
	/**
	 * Shows missing resources when a .usp project file is loaded and devices/waves could not be loaded. 
	 * @author Kone
	 *
	 */
	private class MissingResourcesDialog extends Alert {
		
		private static final String TITLE = "title";
		private static final String HEADER = "header";
		private static final String CONTENT = "content";
		
		private MissingResourcesDialog(List<String> missingResources) {
			super(AlertType.ERROR);
			
			try {
				setTitle(LanguageResourceHandler.getInstance().getLocalizedText(MissingResourcesDialog.class, TITLE));
				setHeaderText(LanguageResourceHandler.getInstance().getLocalizedText(MissingResourcesDialog.class, HEADER));
				String contentString = LanguageResourceHandler.getInstance().getLocalizedText(MissingResourcesDialog.class, CONTENT);
				for (String missingResource : missingResources) {
					contentString = contentString + ("\u2022 " + missingResource + "\n\n");
				}
				setContentText(contentString);
			} catch (ResourceProviderException e) {
				e.printStackTrace();
			}
			
			showAndWait();
		}
	}
}
