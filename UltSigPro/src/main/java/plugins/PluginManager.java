package plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
	@Nonnull
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
					sigproLoader.registerExternPlugin(files[i].toPath());
				} else if (loader.equals("common")) {
					commonLoader.registerExternPlugin(files[i].toPath());
				}
			} catch (IllegalArgumentException e) {
				CommonLogger.getInstance().logMessageAndException("Non .jar file in " + dir.getPath(), e);
			} catch (ClassNotFoundException | ClassCastException | IOException e) {
				CommonLogger.getInstance().logException(e);
			}
		}
	}

	/**
	 * Method to get all available external signal processing plugins
	 * 
	 * @return a {@link List} of plugin names.
	 */
	@Nonnull
	public List<String> getAvailableExternSigproPlugins() {
		return sigproLoader.getAvailableExternPlugins();
	}

	/**
	 * Method to get all available signal processing plugins.
	 * 
	 * @return a {@link List} of plugin names.
	 */
	@Nonnull
	public List<String> getAllAvailableSigproPlugins() {
		return sigproLoader.getAllAvailablePlugins();
	}

	/**
	 * Method to get all available common plugins
	 * 
	 * @return a {@link List} of plugin names.
	 */
	@Nonnull
	public List<String> getAvailableExternCommonPlugins() {
		return commonLoader.getAvailableExternPlugins();
	}

	/**
	 * Method to get all available common plugins.
	 * 
	 * @return a {@link List} of plugin names
	 */
	@Nonnull
	public List<String> getAllAvailbaleCommonPlugins() {
		return commonLoader.getAllAvailablePlugins();
	}

	/**
	 * Adds a file as signal processing plugin and copies it into the current
	 * working direcotry.
	 * 
	 * @param pluginPath
	 *            the {@link Path} to the .jar file. Must not be null.
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
	public void registerExternSigproPlugin(@Nonnull Path pluginPath)
			throws ClassNotFoundException, ClassCastException, IllegalArgumentException, IOException {
		sigproLoader.registerExternPlugin(pluginPath);
	}

	/**
	 * Register a internal signal processing plugin which isn't located in a
	 * .jar file.
	 * 
	 * @param name
	 *            the name of the plugin. Must not be null.
	 * @param clazz
	 *            the {@link Class} to register. Must not be null.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("unchecked")
	public void registerInternSigproPlugin(@Nonnull String name, @Nonnull Class<?> clazz) throws InstantiationException, IllegalAccessException {
		if(clazz.newInstance() instanceof SigproPlugin) {
			sigproLoader.registerInternalPlugin(name, (Class<SigproPlugin>) clazz);			
		}
	}

	/**
	 * Adds a file as common plugin and copies it into the current working
	 * direcotry.
	 * 
	 * @param pluginPath
	 *            the {@link Path} to the .jar file. Must not be null.
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
	public void registerExternCommonPlugin(@Nonnull Path pluginPath)
			throws ClassNotFoundException, ClassCastException, IllegalArgumentException, IOException {
		commonLoader.registerExternPlugin(pluginPath);
	}

	/**
	 * Register a interal signal processing plugin which isn't located in a .jar
	 * file.
	 * 
	 * @param name
	 *            the name of the plugin. Must not be null.
	 * @param clazz
	 *            the {@link Class} to register. Must not be null.
	 */
	@SuppressWarnings("unchecked")
	public void registerInternCommonPlugin(@Nonnull String name, @Nonnull Class<?> clazz) {
		if(clazz.isInstance(CommonPlugin.class)) {
			commonLoader.registerInternalPlugin(name, (Class<CommonPlugin>) clazz);			
		}
	}

	/**
	 * Creates a new instance of the requested signal processing plugin
	 * 
	 * @param name
	 *            the name of the requested plugin. Must not be null.
	 * @return the new instance of the requested plugin. Can be null.
	 * @throws InstantiationException
	 *             if the new instance can't be created
	 * @throws IllegalAccessException
	 *             if the access isn't be granted
	 */
	@CheckForNull
	public SigproPlugin getSigproPlugin(String name) throws InstantiationException, IllegalAccessException {
		return sigproLoader.getPlugin(name);
	}

	/**
	 * Creates a new instance of the requested common plugin
	 * 
	 * @param name
	 *            the name of the requested plugin. Must not be null.
	 * @return the new instance of the requested plugin. Can be null.
	 * @throws InstantiationException
	 *             if the new instance can't be created
	 * @throws IllegalAccessException
	 *             if the access isn't be granted
	 */
	@CheckForNull
	public CommonPlugin getCommonPlugin(@Nonnull String name) throws InstantiationException, IllegalAccessException {
		return commonLoader.getPlugin(name);
	}

	/**
	 * Copies a given file into the plugin directory and registers the plugin at
	 * the registers it at the plugin loader.
	 * 
	 * @param path
	 *            The path to the source file. Must not be null.
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
	public void importSigproPlugin(@Nonnull Path path)
			throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
		File sourceFile = path.toFile();

		String fileName = sourceFile.getName();

		if (!fileName.substring(fileName.lastIndexOf(".")).equals(".jar")) {
			throw new IllegalArgumentException("File to register a plugin isn't a jar file");
		}

		File destFile = new File(sigproDir + File.separator + fileName);

		Path destPath = destFile.toPath();

		Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);

		registerExternSigproPlugin(destPath);
	}

	/**
	 * Copies a given file into the plugin directory and registers the plugin at
	 * the registers it at the plugin loader.
	 * 
	 * @param path
	 *            The path to the source file. Must not be null.
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
	public void importCommonPlugin(@Nonnull Path path)
			throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
		File sourceFile = path.toFile();

		String fileName = sourceFile.getName();

		if (!fileName.substring(fileName.lastIndexOf(".")).equals(".jar")) {
			throw new IllegalArgumentException("File to register a plugin isn't a jar file");
		}

		File destFile = new File(commonDir + File.separator + fileName);

		Path destPath = destFile.toPath();

		Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);

		registerExternCommonPlugin(destPath);
	}

	/**
	 * Removes the plugin with the given name from the {@link PluginLoader} and
	 * deletes the corresponding .jar file. <br/>
	 * Instances of the delete plugin can be used further on.
	 * 
	 * @param name
	 *            the name of the plugin to delete. Must not be null.
	 */
	public void removeCommonPlugin(@Nonnull String name) {
		commonLoader.removeExternalPlugin(name);
		File[] commonFiles = commonDirFile.listFiles();

		for (int i = 0; i < commonFiles.length; i++) {
			String fileName = commonFiles[i].getName().substring(0, commonFiles[i].getName().indexOf("."));
			if (fileName.equals(name)) {
				commonFiles[i].delete();
			}
		}
	}

	/**
	 * Removes the plugin with the given name from the {@link PluginLoader} and
	 * deletes the corresponding .jar file. <br/>
	 * Instances of the delete plugin can be used further on.
	 * 
	 * @param name
	 *            the name of the plugin to delete. Must not be null.
	 */
	public void removeSigproPlugin(@Nonnull String name) {
		sigproLoader.removeExternalPlugin(name);
		File[] sigproFiles = sigproDirFile.listFiles();

		for (int i = 0; i < sigproFiles.length; i++) {
			String fileName = sigproFiles[i].getName().substring(0, sigproFiles[i].getName().indexOf("."));
			if (fileName.equals(name)) {
				sigproFiles[i].delete();
			}
		}
	}
}
