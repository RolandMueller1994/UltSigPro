package gui.menubar;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
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
				File file = fileChooser.showOpenDialog(USPGui.stage);
				if (file != null) {
					DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder documentBuilder;
					try {
						documentBuilder = documentBuilderFactory.newDocumentBuilder();
						Document document = documentBuilder.parse(file);
						NodeList nodeList = document.getElementsByTagName("*");
						System.out.println(nodeList);
					} catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		});
	}
}
