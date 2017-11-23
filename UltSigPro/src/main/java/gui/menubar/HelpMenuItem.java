package gui.menubar;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;

import i18n.LanguageResourceHandler;
import javafx.scene.control.MenuItem;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;

public class HelpMenuItem extends MenuItem {
	
	private static final String TITLE = "title";
	
	public HelpMenuItem() throws ResourceProviderException, IOException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(HelpMenuItem.class, TITLE));
		
	}
}
