package gui.menubar;

import i18n.LanguageResourceHandler;
import javafx.scene.control.MenuItem;
import resourceframework.ResourceProviderException;

public class FileMenuItem extends MenuItem {

	private static final String TITLE = "title";
	
	public FileMenuItem() throws ResourceProviderException {		
		super(LanguageResourceHandler.getInstance().getLocalizedText(FileMenuItem.class, TITLE));	
	}
	
}
