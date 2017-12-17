package gui.menubar;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;
import resourceframework.ResourceProviderException;

public class ExitMenuItem extends MenuItem {

	private static final String TITLE = "title";

	public ExitMenuItem() throws ResourceProviderException {
		super(LanguageResourceHandler.getInstance().getLocalizedText(ExitMenuItem.class, TITLE));
		super.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				USPGui.getStage().fireEvent(new WindowEvent(USPGui.getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
			}
		});
	}
}
