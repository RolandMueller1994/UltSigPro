package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import gui.USPGui;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;
import startup.ArgParser;

/**
 * Initializes all modules and handles input arguments.
 * 
 * @author roland
 *
 */
public class USPMain {

	/**
	 * Startup the application
	 * 
	 * @param args
	 *            Input arguments
	 */
	public static void main(String[] args) {

		String current;
		try {
			GlobalResourceProvider resProv = GlobalResourceProvider.getInstance();

			// Register current working directory
			current = new java.io.File(".").getCanonicalPath();
			resProv.registerResource("workDir", current);

			// Parse arguments
			ArgParser argParser = new ArgParser();
			argParser.parse(args);

			// Create and register logging directory
			current = current + File.separator + "logging";
			File logDir = new File(current);

			if (!logDir.exists()) {
				logDir.mkdir();
			}
			resProv.registerResource("loggingPath", current);

			// Check if help is required
			if (resProv.checkRegistered("help")) {
				File helpFile = new File(
						resProv.getResource("workDir") + File.separator + "help" + File.separator + "message.txt");
				FileReader reader = new FileReader(helpFile);
				BufferedReader bufReader = new BufferedReader(reader);
				String message;
				while ((message = bufReader.readLine()) != null) {
					System.out.println(message);
				}
				bufReader.close();
			} else {
				// Startup the GUI
				USPGui gui = new USPGui();
				gui.buildGUI();

				System.out.println(resProv.getResource("workDir"));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceProviderException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Exit");
		}
	}
}
