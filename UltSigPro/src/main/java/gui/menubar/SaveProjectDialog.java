package gui.menubar;

import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import resourceframework.ResourceProviderException;

public class SaveProjectDialog extends Dialog<ButtonType> {

	private static final String TITLE = "title";
	private static final String HEADER = "header";

	public SaveProjectDialog() {

		initOwner(USPGui.stage);
		initStyle(StageStyle.UTILITY);
		try {
			setTitle(LanguageResourceHandler.getInstance().getLocalizedText(SaveProjectDialog.class, TITLE));
			setHeaderText(LanguageResourceHandler.getInstance().getLocalizedText(SaveProjectDialog.class, HEADER));
			getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
			getDialogPane().getButtonTypes().add(ButtonType.YES);
			getDialogPane().getButtonTypes().add(ButtonType.NO);
		} catch (ResourceProviderException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Checks if the user wants to save the current project settings before
	 * closing UltSigPro. Is normally called, when a {@linkplain WindowEvent}
	 * WINDOW_CLOSE_REQUEST is fired.
	 * 
	 * @param event
	 *            the event which can be consumed if cancel is pressed during
	 *            the dialog
	 */
	public void saveProjectDialogAfterCloseRequest(WindowEvent event) {
		Document currentProject;
		try {
			// check if project has changed since the last time saving
			// if so, ask the user to save his current project
			currentProject = USPFileCreator.collectProjectSettings();
			if (USPFileCreator.projectChangedSinceLastSaving(currentProject)) {
				SaveProjectDialog dialog = new SaveProjectDialog();
				Optional<ButtonType> result = dialog.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.YES) {
					if (USPFileCreator.getFile() != null) {
						try {
							USPFileCreator.createUSPFile(currentProject);
						} catch (TransformerException e) {
							e.printStackTrace();
						}
					} else {
						try {
							USPFileCreator fileCreator = USPFileCreator.getFileCreator();
							fileCreator.createFile();
							if (USPFileCreator.getFile() != null) {
								USPFileCreator.createUSPFile(currentProject);
							} else {
								event.consume();
							}
						} catch (ResourceProviderException | TransformerException e) {
							e.printStackTrace();
						}
					}
				} else if (result.isPresent() && result.get() == ButtonType.NO) {

				} else if (result.isPresent() && result.get() == ButtonType.CANCEL) {
					event.consume();
				}
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Checks if the user wants to save the current project settings before
	 * creating a new UltSigPro project. Is normally called, when the
	 * {@linkplain MenuItem} {@linkplain NewProjectMenuItem} is selected.
	 * 
	 */
	public void saveProjectDialogAfterNewProjectRequest() {
		Document currentProject;
		try {
			// check if project has changed since the last time saving
			// if so, ask the user to save his current project
			currentProject = USPFileCreator.collectProjectSettings();
			if (USPFileCreator.projectChangedSinceLastSaving(currentProject)) {
				SaveProjectDialog dialog = new SaveProjectDialog();
				Optional<ButtonType> result = dialog.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.YES) {
					if (USPFileCreator.getFile() != null) {
						try {
							USPFileCreator.createUSPFile(currentProject);
							deleteCurrentProject();
						} catch (TransformerException | ResourceProviderException e) {
							e.printStackTrace();
						}
					} else {
						try {
							USPFileCreator fileCreator = USPFileCreator.getFileCreator();
							fileCreator.createFile();
							if (USPFileCreator.getFile() != null) {
								USPFileCreator.createUSPFile(currentProject);
								deleteCurrentProject();
							}
						} catch (ResourceProviderException | TransformerException e) {
							e.printStackTrace();
						}
					}
				} else if (result.isPresent() && result.get() == ButtonType.NO) {
					deleteCurrentProject();
				} else if (result.isPresent() && result.get() == ButtonType.CANCEL) {

				}
			}
		} catch (ParserConfigurationException | ResourceProviderException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Deletes the current project with all settings.
	 * 
	 * @throws ResourceProviderException
	 * @throws ParserConfigurationException
	 */
	private void deleteCurrentProject() throws ResourceProviderException, ParserConfigurationException {
		LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
		USPGui.getStage().setTitle(lanHandler.getLocalizedText(USPGui.class, "title"));
		USPFileCreator.setFile(null);
		USPGui.deleteAllChannels();
		USPFileCreator.setReferenceDocument(USPFileCreator.collectProjectSettings());
	}

}
