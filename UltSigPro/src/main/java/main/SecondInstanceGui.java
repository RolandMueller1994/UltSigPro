package main;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import resourceframework.ResourceProviderException;

public class SecondInstanceGui extends Application {
	
	public void buildGui(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		Image icon = new Image("file:iconNewSmall.png");
		primaryStage.getIcons().add(icon);
		GridPane pane = new GridPane();
		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.hide();
		new SecondInstanceAlert(AlertType.ERROR, primaryStage).showAndWait();
		System.exit(1);
	}
	
	public class SecondInstanceAlert extends Alert {

		private static final String TITLE = "title";
		private static final String HEADER = "header";

		public SecondInstanceAlert(AlertType alertType, Stage stage) throws ResourceProviderException {
			super(alertType);
			initOwner(stage);
			setTitle(LanguageResourceHandler.getInstance().getLocalizedText(USPGui.class, TITLE));
			setHeaderText(LanguageResourceHandler.getInstance().getLocalizedText(SecondInstanceAlert.class, HEADER));
		}
		
	}

}
