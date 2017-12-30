package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class SecondInstanceGui extends Application {
	
	public void buildGui(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.show();
		GridPane pane = new GridPane();
		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.show();
		new SecondInstanceDialog(primaryStage).showAndWait();
	}

	public class SecondInstanceDialog extends Dialog<ButtonType> {

		private static final String TITLE = "title";
		private static final String HEADER = "header";

		public SecondInstanceDialog(Stage stage) {
			initOwner(stage);
			setHeaderText("hhuoiuoi");
			setTitle("ttiiittitel");
			getDialogPane().getButtonTypes().add(ButtonType.OK);
		}
	}

}
