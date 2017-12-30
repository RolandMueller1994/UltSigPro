package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import gui.USPGui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * This class prevents the user to start another instance of UltSigPro.
 * 
 * @author Kone
 *
 */
public class SingleInstanceCheck {

	SingleInstanceThread singleInstanceThread;

	public SingleInstanceCheck() {
		singleInstanceThread = new SingleInstanceThread();
	}

	public void stopSingleInstanceThread() {
		singleInstanceThread.stopThread();
	}

	/**
	 * Starts a new {@linkplain ServerSocket}. If there is already another
	 * {@linkplain ServerSocket} running, a dialog with a hint appears and the
	 * new instance of UltSigPro gets closed.
	 */
	public void checkForAnotherInstance(String[] args) {
		try {
			Socket socket = new Socket("localhost", SingleInstanceThread.PORT);
			// TODO show a message to the user
			// SecondInstanceGui gui = new SecondInstanceGui();
			// gui.buildGui(args);
			System.out.println("usp already running");
			System.exit(1);
		} catch (Exception e) {
			singleInstanceThread.start();
		}
	}

	/**
	 * Thread who runs the {@linkplain ServerSocket} and waits for a connection
	 * from a second {@linkplain ServerSocket}.
	 * 
	 * @author Kone
	 *
	 */
	class SingleInstanceThread extends Thread {

		public static final int PORT = 30080;
		public boolean runThread = true;

		ServerSocket serverSocket = null;
		Socket clientSocket = null;

		public void run() {

			try {
				serverSocket = new ServerSocket(PORT, 1);
				serverSocket.setSoTimeout(200);
				while (runThread) {
					// Wait for a connection (from the second instance)
					try {
						clientSocket = serverSocket.accept();
						clientSocket.close();
					} catch (SocketTimeoutException e) {
						// gets thrown, when there is no another ServerSocket
						// running
						// --> only one instance of USP is running
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Stops the {@linkplain SingleInstanceThread}.
		 */
		private void stopThread() {
			runThread = false;
		}
	}

	private class SecondInstanceGui extends Application {

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
	}

	private class SecondInstanceDialog extends Dialog<ButtonType> {

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
