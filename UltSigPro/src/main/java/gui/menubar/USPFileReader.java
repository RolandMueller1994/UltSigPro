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
import gui.USPGui;
import i18n.LanguageResourceHandler;
import plugins.sigproplugins.SigproPlugin;
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

	public void readUSPFile(File file) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ResourceProviderException {
		
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
			HashMap<String, Class<SigproPlugin>> plugins = new HashMap<>();
			
			if (channel.getNodeType() == Node.ELEMENT_NODE) {
				Element entry = (Element) channel;
				NodeList entries = entry.getChildNodes();

				// entries for each channel (inputDevice, outputDevice, ...)
				for (int j = 0; j < entries.getLength(); j++) {
					Node channelEntryNode = entries.item(j);
					if (channelEntryNode.getNodeType() == Node.ELEMENT_NODE) {
						Element channelItemElement = (Element) channelEntryNode;
						String tagName = channelItemElement.getTagName();
						
						// search for channel name, inputDevices, outputDevices, plugins...
						if (tagName == "name") {
							channelName = channelItemElement.getTextContent();
						} else if (tagName == "inputDevice") {
							inputDevices.add(channelItemElement.getTextContent());
						} else if (tagName == "outputDevice") {
							outputDevices.add(channelItemElement.getTextContent());
						} else if (tagName == "plugin") {
							NodeList pluginEntryNodeList = channelItemElement.getChildNodes();
							for (int k = 0; k < pluginEntryNodeList.getLength(); k++) {
								Node pluginNode = pluginEntryNodeList.item(k);
								if (pluginNode.getNodeType() == Node.ELEMENT_NODE) {
									Element pluginElement = (Element) pluginNode;
									if (pluginElement.getTagName() == "name") {
										//TODO collect plugin names and classes
										//plugins.put(pluginElement.getTextContent(), (Class<SigproPlugin>) pluginEntryNodeList.item(k+1).getTextContent());
										k++;
									}
								}
							}
						}
					}
				}
			}
			// TODO load wave files
			// TODO bug: loading two different projects -> old project settings are not deleted
			USPGui.addChannel(new ChannelConfig(channelName, inputDevices, outputDevices, choosedInputWaveFiles, choosedOutputWaveFiles));
			// TODO create plugins
			/*
			for (Map.Entry<String, Class<SigproPlugin>> plugin : plugins.entrySet()) {
				PluginManager.getInstance().registerInternSigproPlugin(plugin.getKey(), plugin.getValue());
			}*/
		}
		
	}
}
