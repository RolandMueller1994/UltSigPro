package plugins.sigproplugins.signalrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gui.USPGui;
import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

/**
 * Class which is used as {@link SigproPlugin}. It has multiple inputs and one
 * output. The values of the inputs will be added and written to the output.
 * 
 * @author roland
 *
 */
public class SignalAdder implements SigproPlugin {

	private HashMap<String, LinkedBlockingQueue<int[]>> inputs = new HashMap<>();

	private boolean play = false;
	private DataDestinationInterface output;

	/**
	 * Empty default constructor. Needed for instantiation by reflection. 
	 */
	public SignalAdder() {
		
	}
	
	/**
	 * Creates a new add block.
	 * 
	 * @param size
	 *            The number of inputs.
	 */
	public SignalAdder(int size) {

		for (int i = 0; i < size; i++) {
			inputs.put(new Integer(i).toString(), new LinkedBlockingQueue<int[]>());
		}

	}

	@Override
	public void putData(String input, int[] data) {

		inputs.get(input).offer(data);

	}

	private void play() {
		play = true;

		Thread addThread = new Thread(new Runnable() {

			@Override
			public void run() {

				ArrayList<int[]> intBuffers = new ArrayList<>(inputs.size());
				int[] counter = new int[inputs.size()];
				int[] max = new int[inputs.size()];

				int i = 0;

				for (LinkedBlockingQueue<int[]> input : inputs.values()) {
					try {
						intBuffers.add(i, input.poll(1, TimeUnit.SECONDS));
						counter[i] = 0;
						max[i] = intBuffers.get(i).length;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					i++;
				}

				try {
					while (play) {

						i = 0;
						int value;

						int[] data = new int[100];

						for (int j = 0; i < 100; i++) {
							value = 0;
							i = 0;
							for (LinkedBlockingQueue<int[]> input : inputs.values()) {
								if (counter[i] == max[i] - 1) {
									intBuffers.remove(i);
									try {
										intBuffers.add(i, input.poll(100, TimeUnit.MILLISECONDS));
										counter[i] = 0;
										max[i] = intBuffers.get(i).length;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								value += intBuffers.get(i)[counter[i]];
								counter[i]++;
								i++;
							}
							data[j] = value;
						}

						output.putData(data);
					}
				} catch (NullPointerException ex) {
					if (play) {
						ex.printStackTrace();
						USPGui.stopExternally();
					}
				}
			}
		});
		addThread.start();

	}

	private void stop() {
		play = false;

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (LinkedBlockingQueue<int[]> queue : inputs.values()) {
			queue.clear();
		}
	}

	@Override
	public String getName() {

		return "Add";
	}

	@Override
	public String getVersion() {

		return "1.0.0";
	}

	@Override
	public Pane getGUI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOutputConfig(HashMap<String, DataDestinationInterface> outputConfig) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOutputConfig(String output, DataDestinationInterface dest) {
		if (output.equals("out")) {
			this.output = dest;
		}
	}

	@Override
	public HashSet<String> getOutputConfig() {

		HashSet<String> outputConfig = new HashSet<>();
		outputConfig.add("out");

		return outputConfig;
	}

	@Override
	public void setPlay(boolean play) {
		if (play) {
			play();
		} else {
			stop();
		}

	}

}
