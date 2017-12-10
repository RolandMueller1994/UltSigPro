package gui.menubar;

import java.io.File;
import java.util.Iterator;

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

import channel.Channel;
import channel.ChannelPane;
import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import junit.framework.Assert;
import resourceframework.ResourceProviderException;

public class USPFileCreator {

	private static USPFileCreator fileCreator;
	private static File file;
	private static boolean fileCreated;
	private static Document referenceDocument;

	public static USPFileCreator getFileCreator() {

		if (fileCreator == null) {
			fileCreator = new USPFileCreator();
			fileCreated = false;
		}
		return fileCreator;
	}

	private USPFileCreator() {

	}

	public File createFile() throws ResourceProviderException {
		LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("UltSigPro " + lanHandler.getLocalizedText("file"), "*.usp"));
		file = fileChooser.showSaveDialog(USPGui.stage);

		// add file name in the header line
		if (file != null) {
			USPGui.getStage().setTitle(lanHandler.getLocalizedText(USPGui.class, "title") + " - " + file.getName());
			fileCreated = true;
		}

		return file;
	}

	/**
	 * Is normally called, when a new project gets created. Creates a document
	 * for reference issues. The reference is used to check, if there are any
	 * changes made on this project after it has been created.
	 * 
	 * @throws ParserConfigurationException
	 */
	public void createReferenceProjectDocument() throws ParserConfigurationException {
		referenceDocument = collectProjectSettings();
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
	public static void createUSPFile(Document doc) throws TransformerConfigurationException, TransformerException {
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
	public static Document collectProjectSettings() throws ParserConfigurationException {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// root elements
		Element rootElement = doc.createElement("UspProjectFile");
		doc.appendChild(rootElement);

		Iterator<Node> iterChannelName = USPGui.getChannelBox().getChildren().iterator();
		while (iterChannelName.hasNext()) {
			ChannelPane currentChannelPane = (ChannelPane) iterChannelName.next();
			// channel elements
			Element channelXMLElement = doc.createElement("channel");
			rootElement.appendChild(channelXMLElement);

			// collects input/output devices and input/output wave files
			currentChannelPane.collectChannelConfig(doc, channelXMLElement);

			// plugin elements
			USPGui.collectPluginConfig(doc, channelXMLElement, currentChannelPane);
		}
		return doc;
	}

	/**
	 * 
	 * @return The file where the current project has been saved.
	 */
	public static File getFile() {
		return file;
	}

	/**
	 * 
	 * @param file
	 *            Contains the current project settings to be saved.
	 */
	public static void setFile(File file) {
		USPFileCreator.file = file;
	}

	/**
	 * Is normally called, when the current project gets saved. Updates the
	 * reference document, which has been created with a new project. The
	 * reference is used to check, if there are any changes made to this project
	 * after the last time "save" was executed.
	 * 
	 * @param doc
	 *            The current project settings
	 */
	public static void setReferenceDocument(Document doc) {
		referenceDocument = doc;
	}

	/**
	 * Checks, if the current project differs from the last saved project. This
	 * would mean, that the current project has unsaved changes.
	 * 
	 * @param doc
	 *            The current project
	 * @return True, if the both project differ from each other.
	 */
	public static boolean projectChangedSinceLastSaving(Document doc) {

		// TODO uncomment if "normalizeDocument()" is needed
		// works currently without that
		// referenceDocument.normalizeDocument();
		// doc.normalizeDocument();
		if (doc.isEqualNode(referenceDocument)) {
			return false;
		}
		return true;
	}
}
