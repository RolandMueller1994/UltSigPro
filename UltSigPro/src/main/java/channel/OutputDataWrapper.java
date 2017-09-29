package channel;

public class OutputDataWrapper {

	private OutputInfoWrapper outputInfo;
	private double[] outputData;
	
	public OutputDataWrapper(OutputInfoWrapper outputInfo, double[] outputData) {

		this.outputInfo = outputInfo;
		this.outputData = outputData;
	}

	public OutputInfoWrapper getOutputInfo() {
		return outputInfo;
	}

	public double[] getOutputData() {
		return outputData;
	}
	
}
