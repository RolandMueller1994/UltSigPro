package gui;

import gui.menubar.MenuBarCreator;
import i18n.LanguageResourceHandler;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
		
		MenuBarCreator menuBarCreator = new MenuBarCreator();
		MenuBar menuBar = menuBarCreator.getMenuBar();
		
		VBox vBox = new VBox();
		vBox.getChildren().add(menuBar);
		
		Scene scene = new Scene(vBox);
		
		primaryStage.setScene(scene);
		
		primaryStage.setTitle(languageRes.getLocalizedText(USPGui.class, TITLE));
		
		primaryStage.setMaximized(true);
		
		primaryStage.show();
		
	}
	
}
