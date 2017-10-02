package plugins.sigproplugins.signalrouting;

import java.io.DataOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import plugins.sigproplugins.SigproPlugin;

/**
 * This class represents the connection between to {@link SigproPlugin}s. Every
 * connection has exactly one input and multiple outputs. If there are more
 * outputs, the data will be divided by deep copy, so the next plugin can handle
 * the data in every way without influencing other plugins. The connection will
 * call the plugins with new data in cyclic manner.
 * 
 * @author roland
 *
 */
public class PluginConnection implements DataDestinationInterface {

	private HashMap<SigproPlugin, HashMap<String, LinkedBlockingQueue<int[]>>> outputMap = new HashMap<>();
	private DataOutput dataOutput;
	private LinkedBlockingQueue<int[]> dataOutputQueue;

	private int outputSize = 0;
	private boolean play = false;

	/**
	 * This method will start the signal distribution in this connection.
	 */
	public void play() {
		play = true;

		dataOutputQueue.clear();
		
		for (Map.Entry<SigproPlugin, HashMap<String, LinkedBlockingQueue<int[]>>> entry : outputMap.entrySet()) {

			for (Map.Entry<String, LinkedBlockingQueue<int[]>> inputEntry : entry.getValue().entrySet()) {

				Thread playThread = new Thread(new Runnable() {

					@Override
					public void run() {

						LinkedBlockingQueue<int[]> queue = inputEntry.getValue();
						queue.clear();
						SigproPlugin dest = entry.getKey();
						String pluginInput = inputEntry.getKey();

						int[] data;

						while (play) {
							try {
								data = queue.poll(100, TimeUnit.MILLISECONDS);
								if (data != null) {
									dest.putData(pluginInput, data);
								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				});

				playThread.start();
			}

		}
	}

	/**
	 * This method will stop the signal distribution. It cleans up the queues.
	 */
	public void stop() {
		play = false;
	}

	/**
	 * Adds a plugin as output for this connection. A new queue will be created.
	 * 
	 * @param plugin
	 *            the {@link SigproPlugin} as output. Must not be null.
	 * @param input
	 *            the input of the plugin to which data shall we written. Must
	 *            not be null.
	 */
	public synchronized void addOutput(@Nonnull SigproPlugin plugin, @Nonnull String input) {
		if (!outputMap.containsKey(plugin)) {
			outputMap.put(plugin, new HashMap<String, LinkedBlockingQueue<int[]>>());
		}
		outputMap.get(plugin).put(input, new LinkedBlockingQueue<int[]>());
		outputSize++;
	}
	
	public synchronized void addOutput(@Nonnull DataOutput dest) {
		this.dataOutput = dataOutput;
		dataOutputQueue = new LinkedBlockingQueue<int[]>();
	}

	/**
	 * Removes a plugin as output for this connection.
	 * 
	 * @param plugin
	 *            the {@link SigproPlugin} to remove. Must not be null.
	 * @param input
	 *            the input of the plugin to remove. Must not be null.
	 */
	public synchronized void removeOutput(@Nonnull SigproPlugin plugin, @Nonnull String input) {
		if(outputMap.containsKey(plugin)) {
			outputMap.get(plugin).remove(input);
			if(outputMap.get(plugin).isEmpty()) {
				outputMap.remove(plugin);
			}
		}
		outputSize--;
	}

	public synchronized void removeOutput() {
		dataOutput = null;
		dataOutputQueue = null;
	}
	
	@Override
	public void putData(int[] data) {

		int i = 0;

		for (Map.Entry<SigproPlugin, HashMap<String, LinkedBlockingQueue<int[]>>> entry : outputMap.entrySet()) {
			for(LinkedBlockingQueue<int[]> queue : entry.getValue().values()) {
				if (i == 0) {
					queue.offer(data);
				} else {
					int[] newData = new int[data.length];
					System.arraycopy(data, 0, newData, 0, data.length);
					queue.offer(newData);
				}
				i++;				
			}
		}
		
		if(dataOutput != null) {
			if (i == 0) {
				dataOutputQueue.offer(data);
			} else {
				int[] newData = new int[data.length];
				System.arraycopy(data, 0, newData, 0, data.length);
				dataOutputQueue.offer(newData);
			}
		}
	}

	public int[] fetch() {
		try {
			return dataOutputQueue.poll(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
}
