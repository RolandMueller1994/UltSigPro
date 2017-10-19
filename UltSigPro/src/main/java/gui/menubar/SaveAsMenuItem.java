package gui.menubar;

import i18n.LanguageResourceHandler;
import javafx.scene.control.MenuItem;
import resourceframework.ResourceProviderException;

public class SaveAsMenuItem extends MenuItem {

	private static final String TITLE = "title";
	
	public SaveAsMenuItem() throws ResourceProviderException {		
		super(LanguageResourceHandler.getInstance().getLocalizedText(SaveAsMenuItem.class, TITLE));	
		
	}
	
}
