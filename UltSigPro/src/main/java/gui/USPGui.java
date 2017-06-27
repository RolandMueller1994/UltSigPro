package gui;

import i18n.LanguageResourceHandler;
import javafx.application.Application;
import javafx.stage.Stage;
import resourceframework.ResourceProviderException;

/**
 * This class is a handler for all issues depending on building and executing the graphical user interface.
 * 
 * @author roland
 *
 */
public class USPGui extends Application {
	
	private static final String TITLE = "title";
	
	private String[] args;
	
	/**
	 * This method must be called at startup. The GUI will be set up.
	 */
	public void buildGUI(String[] args) {
		this.args = args;
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		LanguageResourceHandler languageRes = LanguageResourceHandler.getInstance();
		
		primaryStage.setTitle(languageRes.getLocalizedText(USPGui.class, TITLE));
		
		primaryStage.setMaximized(true);
		
		primaryStage.show();
		
	}
	
}
