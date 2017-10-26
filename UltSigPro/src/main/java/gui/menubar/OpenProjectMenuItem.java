package gui.menubar;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
					fileChooser.getExtensionFilters().add(
							new FileChooser.ExtensionFilter("UltSigPro " + lanHandler.getLocalizedText("file"), "*.usp"));
				} catch (ResourceProviderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				File file = fileChooser.showOpenDialog(USPGui.stage);
				if (file != null) {
					DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder documentBuilder;
					try {
						documentBuilder = documentBuilderFactory.newDocumentBuilder();
						Document document = documentBuilder.parse(file);
						
						// read all channel entries
						NodeList nodeList = document.getElementsByTagName("channel");
						for (int i = 0; i < nodeList.getLength(); i++) {
							Node channel = nodeList.item(i);
							if (channel.getNodeType() == Node.ELEMENT_NODE) {
								Element entry = (Element) channel;
								NodeList entries = entry.getChildNodes();

								// entries for each channel (inputDevice, outputDevice, ...)
								for (int j = 0; j < entries.getLength(); j++) {
									Node channelEntryNode = entries.item(j);
									if (channelEntryNode.getNodeType() == Node.ELEMENT_NODE) {
										Element channelItemElement = (Element) channelEntryNode;
										
										// check for plugin entries
										if (channelItemElement.getTagName() == "plugin") {
											NodeList pluginEntryNodeList = channelItemElement.getChildNodes();
											for (int k = 0; k < pluginEntryNodeList.getLength(); k++) {
												Node pluginNode = pluginEntryNodeList.item(k);
												if (pluginNode.getNodeType() == Node.ELEMENT_NODE) {
													Element pluginElement = (Element) pluginNode;
													System.out.println(pluginElement.getTagName() + " " + pluginElement.getTextContent());
												}
											}
										} else {
											// entry is no plugin entry
											System.out.println(channelItemElement.getTagName() + " " + channelItemElement.getTextContent());
										}
									}
								}
							}
						}
						
					} catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		});
	}
}
