package gui.menubar;

import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import resourceframework.ResourceProviderException;

public class AboutMenuItem extends MenuItem {

	private static final String TITLE = "title";
	private static final String INFO_ALERT_TITLE = "infoAlertTitle";
	private static final String INFO_ALERT_HEADER = "infoAlertHeader";
	private static final String INFO_ALERT_CONTENT = "infoAlertContent";
	
	public AboutMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(AboutMenuItem.class, TITLE));
		
		super.setOnAction(new EventHandler<ActionEvent> () {

			@Override
			public void handle(ActionEvent event) {
				
				LanguageResourceHandler lanHandler;
				try {
					lanHandler = LanguageResourceHandler.getInstance();
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle(lanHandler.getLocalizedText(AboutMenuItem.class, INFO_ALERT_TITLE));
					alert.setHeaderText(lanHandler.getLocalizedText(AboutMenuItem.class, INFO_ALERT_HEADER));
					
					TextArea contentText = new TextArea(lanHandler.getLocalizedText(AboutMenuItem.class, INFO_ALERT_CONTENT));
					contentText.setEditable(false);
					contentText.setWrapText(true);
					
					alert.getDialogPane().setContent(contentText);
					
					alert.setResizable(true);
					alert.showAndWait();
				} catch (ResourceProviderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		});
	}
		
}
