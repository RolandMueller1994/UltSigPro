package plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import logging.CommonLogger;
import pluginframework.PluginLoader;
import plugins.commonplugins.CommonPlugin;
import plugins.sigproplugins.SigproPlugin;
import resourceframework.GlobalResourceProvider;
import resourceframework.ResourceProviderException;

/**
 * This class is a central access point to plugins.
 * 
 * @author roland
 *
 */
public class PluginManager {

	private PluginLoader<SigproPlugin> sigproLoader = new PluginLoader<>();
	private PluginLoader<CommonPlugin> commonLoader = new PluginLoader<>();

	private String sigproDir;
	private String commonDir;
	private File sigproDirFile;
	private File commonDirFile;

	private static PluginManager instance;

	/**
	 * Create a singleton instance of this class
	 * 
	 * @return the singleton instance
	 */
	public static PluginManager getInstance() {
		if (instance == null) {
			instance = new PluginManager();
		}
		return instance;
	}

	private PluginManager() {
		try {
			String workDir = (String) GlobalResourceProvider.getInstance().getResource("workDir");
			sigproDir = workDir + File.separator + "sigproplugins";
			commonDir = workDir + File.separator + "commonplugins";

			sigproDirFile = new File(sigproDir);
			if (!sigproDirFile.isDirectory()) {
				sigproDirFile.mkdir();
			}

			commonDirFile = new File(commonDir);
			if (!commonDirFile.isDirectory()) {
				commonDirFile.mkdir();
			}

			registerPlugins("common");
			registerPlugins("sigpro");
		} catch (ResourceProviderException e) {
			CommonLogger.getInstance().logMessageAndException("Working directory requested but not registered", e);
			throw new RuntimeException(e);
		}
	}

	private void registerPlugins(String categorie) {
		if (categorie.equals("common")) {
			executeRegisterPlugins(commonDirFile, "common");
		} else if (categorie.equals("sigpro")) {
			executeRegisterPlugins(sigproDirFile, "sigpro");
		}
	}

	private void executeRegisterPlugins(File dir, String loader) {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			try {
				if (loader.equals("sigpro")) {
					sigproLoader.registerPlugin(files[i].toPath());
				} else if (loader.equals("common")) {
					commonLoader.registerPlugin(files[i].toPath());
				}
			} catch (IllegalArgumentException e) {
				CommonLogger.getInstance().logMessageAndException("Non .jar file in " + dir.getPath(), e);
			} catch (ClassNotFoundException | ClassCastException | IOException e) {
				CommonLogger.getInstance().logException(e);
			}
		}
	}

	/**
	 * Method to get all available signal processing plugins
	 * 
	 * @return a {@link List} of plugin names.
	 */
	public List<String> getAvailableSigproPlugins() {
		return sigproLoader.getAvailablePlugins();
	}

	/**
	 * Method to get all available common plugins
	 * 
	 * @return a {@link List} of plugin names.
	 */
	public List<String> getAvailableCommonPlugins() {
		return commonLoader.getAvailablePlugins();
	}

	/**
	 * Adds a file as signal processing plugin and copies it into the current
	 * working direcotry.
	 * 
	 * @param pluginPath
	 *            the {@link Path} to the .jar file
	 * @throws ClassNotFoundException
	 *             if the class can't be found
	 * @throws ClassCastException
	 *             if the class in the .jar file doesn't implement
	 *             {@link SigproPlugin}
	 * @throws IllegalArgumentException
	 *             if the given path doesn't point to a .jar file
	 * @throws IOException
	 *             if an error occurs within the file handling
	 */
	public void registerSigproPlugin(Path pluginPath)
			throws ClassNotFoundException, ClassCastException, IllegalArgumentException, IOException {
		sigproLoader.registerPlugin(pluginPath);
	}

	/**
	 * Adds a file as common plugin and copies it into the current working
	 * direcotry.
	 * 
	 * @param pluginPath
	 *            the {@link Path} to the .jar file
	 * @throws ClassNotFoundException
	 *             if the class can't be found
	 * @throws ClassCastException
	 *             if the class in the .jar file doesn't implement
	 *             {@link SigproPlugin}
	 * @throws IllegalArgumentException
	 *             if the given path doesn't point to a .jar file
	 * @throws IOException
	 *             if an error occurs within the file handling
	 */
	public void registerCommonPlugin(Path pluginPath)
			throws ClassNotFoundException, ClassCastException, IllegalArgumentException, IOException {
		commonLoader.registerPlugin(pluginPath);
	}

	/**
	 * Creates a new instance of the requested signal processing plugin
	 * 
	 * @param name
	 *            the name of the requested plugin
	 * @return the new instance of the requested plugin
	 * @throws InstantiationException
	 *             if the new instance can't be created
	 * @throws IllegalAccessException
	 *             if the access isn't be granted
	 */
	public SigproPlugin getSigproPlugin(String name) throws InstantiationException, IllegalAccessException {
		return sigproLoader.getPlugin(name);
	}

	/**
	 * Creates a new instance of the requested common plugin
	 * 
	 * @param name
	 *            the name of the requested plugin
	 * @return the new instance of the requested plugin
	 * @throws InstantiationException
	 *             if the new instance can't be created
	 * @throws IllegalAccessException
	 *             if the access isn't be granted
	 */
	public CommonPlugin getCommonPlugin(String name) throws InstantiationException, IllegalAccessException {
		return commonLoader.getPlugin(name);
	}

	/**
	 * Copies a given file into the plugin directory and registers the plugin at
	 * the registers it at the plugin loader.
	 * 
	 * @param path
	 *            The path to the source file.
	 * @throws IOException
	 *             if an error occurs within the file handling.
	 * @throws ClassNotFoundException
	 *             if the class can't be found
	 * @throws ClassCastException
	 *             if the class in the .jar file doesn't implement
	 *             {@link SigproPlugin}
	 * @throws IllegalArgumentException
	 *             if the given path doesn't point to a char file
	 */
	public void importSigproPlugin(Path path)
			throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
		File sourceFile = path.toFile();

		String fileName = sourceFile.getName();

		if (!fileName.substring(fileName.lastIndexOf(".")).equals(".jar")) {
			throw new IllegalArgumentException("File to register a plugin isn't a jar file");
		}

		File destFile = new File(sigproDir + File.separator + fileName);

		Path destPath = destFile.toPath();

		Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);

		registerSigproPlugin(destPath);
	}

	/**
	 * Copies a given file into the plugin directory and registers the plugin at
	 * the registers it at the plugin loader.
	 * 
	 * @param path
	 *            The path to the source file.
	 * @throws IOException
	 *             if an error occurs within the file handling.
	 * @throws ClassNotFoundException
	 *             if the class can't be found
	 * @throws ClassCastException
	 *             if the class in the .jar file doesn't implement
	 *             {@link CommonPlugin}
	 * @throws IllegalArgumentException
	 *             if the given path doesn't point to a char file
	 */
	public void importCommonPlugin(Path path)
			throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
		File sourceFile = path.toFile();

		String fileName = sourceFile.getName();

		if (!fileName.substring(fileName.lastIndexOf(".")).equals(".jar")) {
			throw new IllegalArgumentException("File to register a plugin isn't a jar file");
		}

		File destFile = new File(commonDir + File.separator + fileName);

		Path destPath = destFile.toPath();

		Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);

		registerCommonPlugin(destPath);
	}
}
