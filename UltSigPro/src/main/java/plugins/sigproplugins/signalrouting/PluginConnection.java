package plugins.sigproplugins.signalrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import channel.DataDestinationInterface;
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

	private HashMap<SigproPlugin, LinkedBlockingQueue<int[]>> outputMap = new HashMap<>();

	private int outputSize = 0;
	private boolean play = false;

	/**
	 * This method will start the signal distribution in this connection.
	 */
	public void play() {
		play = true;

		for (Map.Entry<SigproPlugin, LinkedBlockingQueue<int[]>> entry : outputMap.entrySet()) {

			Thread playThread = new Thread(new Runnable() {

				@Override
				public void run() {

					LinkedBlockingQueue<int[]> queue = entry.getValue();
					SigproPlugin dest = entry.getKey();
					int[] data;

					while (play) {
						try {
							data = queue.poll(100, TimeUnit.MILLISECONDS);
							if (data != null) {
								dest.putData(data);
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

	/**
	 * This method will stop the signal distribution. It cleans up the queues.
	 */
	public void stop() {
		play = false;

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Map.Entry<SigproPlugin, LinkedBlockingQueue<int[]>> entry : outputMap.entrySet()) {
			entry.getValue().clear();
		}
	}

	/**
	 * Adds a plugin as output for this connection. A new queue will be created.
	 * 
	 * @param plugin
	 *            the {@link SigproPlugin} as output. Must not be null.
	 */
	public synchronized void addOutput(@Nonnull SigproPlugin plugin) {
		outputMap.put(plugin, new LinkedBlockingQueue<int[]>());
		outputSize++;
	}

	/**
	 * Removes a plugin as output for this connection.
	 * 
	 * @param plugin
	 *            the {@link SigproPlugin} to remove. Must not be null.
	 */
	public synchronized void removeOutput(@Nonnull SigproPlugin plugin) {
		outputMap.remove(plugin);
		outputSize--;
	}

	@Override
	public void putData(int[] data) {

		int i = 0;

		for (Map.Entry<SigproPlugin, LinkedBlockingQueue<int[]>> entry : outputMap.entrySet()) {
			if (i == 0) {
				entry.getValue().offer(data);
			} else {
				int[] newData = new int[data.length];
				System.arraycopy(data, 0, newData, 0, data.length);
				entry.getValue().offer(newData);
			}
			i++;
		}

	}

}
