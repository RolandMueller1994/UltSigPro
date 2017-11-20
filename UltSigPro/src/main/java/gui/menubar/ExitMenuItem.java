package gui.menubar;

import i18n.LanguageResourceHandler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import resourceframework.ResourceProviderException;

public class ExitMenuItem extends MenuItem {

	private static final String TITLE = "title";
	
	public ExitMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(ExitMenuItem.class, TITLE));
		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Platform.exit();
				System.exit(0);
			}
		});
	}
}
