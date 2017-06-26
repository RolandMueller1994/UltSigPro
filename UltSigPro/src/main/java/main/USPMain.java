package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

import javax.management.AttributeNotFoundException;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import logging.CommonLogger;
import resourceframework.GlobalResourceProvider;
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
		String argParserMessage = null;
		try {
			GlobalResourceProvider resProv = GlobalResourceProvider.getInstance();

			// Register current working directory
			current = new java.io.File(".").getCanonicalPath();
			resProv.registerResource("workDir", current);

			// Parse arguments
			ArgParser argParser = new ArgParser();
			
			try {
				argParser.parse(args, true);				
			} catch (IllegalArgumentException | AttributeNotFoundException ex) {
				argParserMessage = ex.getMessage();
			}

			// Create and register logging directory
			current = current + File.separator + "logging";
			File logDir = new File(current);

			if (!logDir.exists()) {
				logDir.mkdir();
			}
			resProv.registerResource("loggingPath", current);
			
			//Setup the LanguageResourceHandler
			LanguageResourceHandler.setCurrentLanguage(Locale.GERMAN);
			LanguageResourceHandler.setDefaultLanguage(Locale.GERMAN);
			LanguageResourceHandler.getInstance();
			
			// Check if help is required
			if (resProv.checkRegistered("help") || argParserMessage != null) {
				File helpFile = new File(
						resProv.getResource("workDir") + File.separator + "help" + File.separator + "message.txt");
				FileReader reader = new FileReader(helpFile);
				BufferedReader bufReader = new BufferedReader(reader);
				
				if(argParserMessage != null) {
					System.out.println(argParserMessage + System.lineSeparator());
				}
				
				String message;
				while ((message = bufReader.readLine()) != null) {
					System.out.println(message);
				}
				bufReader.close();
			} else {

				if (resProv.checkRegistered("console")) {
					String os = System.getProperty("os.name");
					System.out.println(os);
					// Check os
					if (os.equals("Linux")) {
						// Linux os
						Runtime.getRuntime().exec("x-terminal-emulator --disable-factory -e ultsigpro");
					} else {
						// Windows os
						// TODO
					}
				} else {
					// Startup the GUI
					USPGui gui = new USPGui();
					gui.buildGUI();

					System.out.println(resProv.getResource("workDir"));
				}
			}

		} catch (Exception e) {
			CommonLogger.getInstance().logException(e);
		} finally {
			try {
				// Remove when GUI builds
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Exit");
		}
	}
}
