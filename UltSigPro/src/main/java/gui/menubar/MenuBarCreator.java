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

import i18n.LanguageResourceHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;

public class MenuBarCreator {

	public MenuBar getMenuBar() throws ResourceProviderException, SAXException, IOException, ParserConfigurationException {
		MenuBar menuBar = new MenuBar();
		
		File menubarXML = new File((String) GlobalResourceProvider.getInstance().getResource("workDir") + File.separator + "menubar.xml");
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
		        .newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(menubarXML);
		
		// Get everything within tag menubar
		NodeList nodeList = document.getElementsByTagName("menubar");
		
		for(int i=0; i<nodeList.getLength(); i++) {
			Node menuNode = nodeList.item(i);
			
			// Read all menu tags in tag menubar
			if(menuNode.getNodeType() == Node.ELEMENT_NODE) {
				Element menuElement = (Element) menuNode;
				
				// Get menu title
				NodeList titleNodeList = menuElement.getElementsByTagName("title");
				Element titleElement = (Element) titleNodeList.item(0);
				menuBar.getMenus().add(new Menu(LanguageResourceHandler.getInstance().getLocalizedText(titleElement.getTextContent())));
				
				// Get list of class entrys
				NodeList classNodeList = menuElement.getElementsByTagName("class");
				
				for(int j=0; j<classNodeList.getLength(); j++) {
					Element classElement = (Element) classNodeList.item(j);
					System.out.println(classElement.getTextContent());
				}
			}
		}
		
		return menuBar;
	}
	
}
