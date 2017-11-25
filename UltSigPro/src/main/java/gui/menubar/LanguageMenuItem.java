package gui.menubar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert.AlertType;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;

public class LanguageMenuItem extends MenuItem {

private static final String TITLE = "title";
private static final String INFO_ALERT_HEADER = "infoAlertHeader";
private static final String INFO_ALERT_CONTENT = "infoAlertContent";
	
	public LanguageMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(LanguageMenuItem.class, TITLE));
		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				ChangeLanguageDialog dialog = new ChangeLanguageDialog(); 
				Optional<ButtonType> result = dialog.showAndWait();
				
				if (result.isPresent() && result.get() == ButtonType.APPLY) {
					Locale languageSelection = dialog.languageListView.getSelectionModel().getSelectedItem().getLocale();
					
					if (languageSelection != null) {
						try {
							String currentLanguage = (String) GlobalResourceProvider.getInstance().getResource("language");
							if (!currentLanguage.equals(languageSelection.toString())) {
								GlobalResourceProvider.getInstance().changeResource("language", languageSelection.getLanguage());
								Alert alert = new Alert(AlertType.INFORMATION);
								alert.setTitle(LanguageResourceHandler.getInstance().getLocalizedText(ChangeLanguageDialog.class, TITLE));
								alert.setHeaderText(LanguageResourceHandler.getInstance().getLocalizedText(LanguageMenuItem.class, INFO_ALERT_HEADER));
								alert.setContentText(LanguageResourceHandler.getInstance().getLocalizedText(LanguageMenuItem.class, INFO_ALERT_CONTENT));
								alert.showAndWait();
							}
						} catch (ResourceProviderException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});
	}
	
	private class ChangeLanguageDialog extends Dialog<ButtonType> {
		
		private static final String TITLE = "title";
		private ListView<LocaleStringWrapper> languageListView = new ListView<LocaleStringWrapper>();
		
		private ChangeLanguageDialog() {
			
			try {
				setTitle(LanguageResourceHandler.getInstance().getLocalizedText(ChangeLanguageDialog.class, TITLE));
			} catch (ResourceProviderException e) {
				e.printStackTrace();
			}
			
			List<Locale> languageListLocale;
			try {
				languageListLocale = LanguageResourceHandler.getInstance().collectAvailableLanguages();

				for (Locale language : languageListLocale) {
					LocaleStringWrapper locStrWrap = new LocaleStringWrapper(language);
					languageListView.getItems().add(locStrWrap);
				}
				getDialogPane().setContent(languageListView);
				getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
				getDialogPane().getButtonTypes().add(ButtonType.APPLY);
			} catch (ResourceProviderException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	private class LocaleStringWrapper {
		private Locale locale;
		private String localeStr;
		
		LocaleStringWrapper(Locale locale) {
			this.locale = locale;
			this.localeStr = locale.getDisplayName(locale);
		}
		
		public Locale getLocale() {
			return locale;
		}
		
		@Override
		public String toString() {
			return localeStr;
		}
	}
}
