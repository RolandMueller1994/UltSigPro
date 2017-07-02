package gui.menubar;

import i18n.LanguageResourceHandler;
import javafx.scene.control.MenuItem;
import resourceframework.ResourceProviderException;

public class HelpMenuItem extends MenuItem {
	
	private static final String TITLE = "title";
	
	public HelpMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(HelpMenuItem.class, TITLE));
	}
	
}
