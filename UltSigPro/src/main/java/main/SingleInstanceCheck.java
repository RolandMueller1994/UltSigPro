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
	public boolean checkForAnotherInstance() {
		try {
			Socket socket = new Socket("localhost", SingleInstanceThread.PORT);
			return true;
		} catch (Exception e) {
			singleInstanceThread.start();
		}
		return false;
	}
	
	public void buildSecondInstanceGui(String[] args) {
		SecondInstanceGui gui = new SecondInstanceGui();
		gui.buildGui(args);
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

}
