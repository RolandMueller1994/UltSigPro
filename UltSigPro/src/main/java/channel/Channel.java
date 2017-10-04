package channel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import inputhandler.InputAdministrator;
import outputhandler.OutputAdministrator;
import plugins.sigproplugins.SigproPlugin;

public class Channel implements InputDataListener, OutputDataSpeaker {

	private InputAdministrator inputAdmin;
	private OutputAdministrator outputAdmin;
	private boolean play = false;
	private String name;
	private ChannelPane pane;

	private PluginInput pluginInput;
	private PluginOutput pluginOutput;

	private ScheduledThreadPoolExecutor executor;

	private boolean firstFetch = true;

	private HashMap<OutputInfoWrapper, LinkedList<InputInfoWrapper>> dataflowMap = new HashMap<>();

	// private LinkedBlockingQueue<LinkedList<Integer>> outputQueue = new
	// LinkedBlockingQueue<>();
	private LinkedList<int[]> inputQueue = new LinkedList<>();
	private LinkedList<int[]> outputQueue = new LinkedList<>();

	public Channel(ChannelPane pane, ChannelConfig config) {
		this.name = config.getName();
		this.pane = pane;
		inputAdmin = InputAdministrator.getInputAdminstrator();
		inputAdmin.registerInputDataListener(this, config.getInputDevices());
		outputAdmin = OutputAdministrator.getOutputAdministrator();
		outputAdmin.registerOutputDevices(this, config.getOutputDevices());

		pluginInput = new PluginInput();
		pluginOutput = new PluginOutput();

		LinkedList<InputInfoWrapper> testList = new LinkedList<>();
		testList.add(new InputInfoWrapper(pluginOutput, "Output"));

		dataflowMap.put(new OutputInfoWrapper(pluginInput, "Input"), testList);
	}

	@Override
	public int[] fetchData() {

		synchronized (outputQueue) {
			return outputQueue.poll();
		}
	}

	@Override
	public void putData(int[] data) {

		pane.insertWaveChartData(data);

		synchronized (inputQueue) {
			inputQueue.add(data);
		}

		// pane.insertWaveChartData(waveChartData);
	}

	public void addInputDevice(String device) {
		inputAdmin.addDeviceToInputDataListener(this, device);
	}

	public void removeInputDevice(String device) {
		inputAdmin.removeDeviceFromInputDataListener(this, device);
	}

	public void addOutputDevice(String device) {
		outputAdmin.addSoundOutputDeviceToSpeaker(this, device);
	}

	public void removeOutputDevice(String device) {
		outputAdmin.removeDeviceFromOutputDataSpeaker(this, device);
	}

	public void delete() {
		inputAdmin.removeInputDataListener(this);
	}

	public synchronized void setPlay(boolean play) {
		if (play) {
			inputQueue.clear();
			outputQueue.clear();
			firstFetch = true;
			executor = new ScheduledThreadPoolExecutor(1);

			executor.scheduleAtFixedRate(new DataflowRunnable(), 0, 1, TimeUnit.MILLISECONDS);
		} else {
			if (executor != null) {
				executor.shutdownNow();
			}
		}
		this.play = play;
	}
	
	public SigproPlugin getPluginInput() {
		return pluginInput;
	}
	
	public SigproPlugin getPluginOutput() {
		return pluginOutput;
	}
	
	public void addPluginConnection(SigproPlugin sourcePlugin, String output, SigproPlugin destPlugin, String input) {
		OutputInfoWrapper outputWrapper = new OutputInfoWrapper(sourcePlugin, output);
		
		if(!dataflowMap.containsKey(outputWrapper)) {
			dataflowMap.put(outputWrapper, new LinkedList<InputInfoWrapper>());
		}
		
		InputInfoWrapper inputWrapper = new InputInfoWrapper(destPlugin, input);
		
		dataflowMap.get(outputWrapper).add(inputWrapper);
	}

	public void removePluginConnection(SigproPlugin sourcePlugin, String output, SigproPlugin destPlugin, String input) {
		
		OutputInfoWrapper outputWrapper = new OutputInfoWrapper(sourcePlugin, output);
		InputInfoWrapper inputWrapper = new InputInfoWrapper(destPlugin, input);
		
		if(dataflowMap.containsKey(outputWrapper)) {
			dataflowMap.get(outputWrapper).remove(inputWrapper);
			if(dataflowMap.get(outputWrapper).isEmpty()) {
				dataflowMap.remove(outputWrapper);
			}
		}	
	}
	
	private class DataflowRunnable implements Runnable {

		@Override
		public void run() {

			try {
				LinkedList<int[]> curData;

				synchronized (inputQueue) {
					if (inputQueue.isEmpty()) {
						return;
					}

					curData = new LinkedList<>();

					while (inputQueue.size() > 0) {
						curData.add(inputQueue.poll());
					}
				}

				LinkedList<double[]> exeData = new LinkedList<>();

				for (int[] inputArray : curData) {
					double[] exeArray = new double[inputArray.length];

					for (int i = 0; i < inputArray.length; i++) {
						exeArray[i] = (double) inputArray[i];
					}

					exeData.add(exeArray);
				}

				for (double[] inputData : exeData) {
					double[] sigflowOutputData = executeSignalProcessing(inputData);

					if (sigflowOutputData != null) {
						int[] outputData = new int[inputData.length];

						for (int i = 0; i < sigflowOutputData.length; i++) {
							outputData[i] = (int) sigflowOutputData[i];
						}

						synchronized (outputQueue) {
							outputQueue.add(outputData);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}

		private double[] executeSignalProcessing(double[] inputData) {

			double[] outputData = null;

			LinkedList<OutputDataWrapper> outputDataWrappers = pluginInput.putData("Input", inputData);

			for (OutputDataWrapper outputWrapper : outputDataWrappers) {
				LinkedList<InputInfoWrapper> inputInfoList = dataflowMap.get(outputWrapper.getOutputInfo());

				for (InputInfoWrapper inputInfo : inputInfoList) {
					double[] newData = new double[inputData.length];
					System.arraycopy(inputData, 0, newData, 0, inputData.length);

					double[] recursivOutputData = recursiveSignalProcessing(inputInfo, newData);

					if (recursivOutputData != null) {
						outputData = recursivOutputData;
					}
				}
			}

			return outputData;
		}

		private double[] recursiveSignalProcessing(InputInfoWrapper inputInfo, double[] data) {

			double[] outputData = null;

			LinkedList<OutputDataWrapper> outputDataWrappers = inputInfo.getDestPlugin()
					.putData(inputInfo.getDestInput(), data);

			for (OutputDataWrapper outputWrapper : outputDataWrappers) {
				if (outputWrapper.getOutputInfo().getSourcePlugin().equals(pluginOutput)) {
					outputData = outputWrapper.getOutputData();
				} else {
					LinkedList<InputInfoWrapper> inputInfoWrapperList = dataflowMap.get(outputWrapper.getOutputInfo());

					for (InputInfoWrapper inputInfoWrapper : inputInfoWrapperList) {

						double[] newData = new double[outputWrapper.getOutputData().length];
						System.arraycopy(outputWrapper.getOutputData(), 0, newData, 0,
								outputWrapper.getOutputData().length);

						double[] recursiveOutputData = recursiveSignalProcessing(inputInfoWrapper, newData);

						if (recursiveOutputData != null) {
							outputData = recursiveOutputData;
						}
					}
				}
			}

			return outputData;
		}

	}
}
