package channel.gui;

public class SignalFlowConfigException extends Exception {

	private boolean input;
	
	public SignalFlowConfigException(String message, boolean input) {
		super(message);
		this.input = input;
	}
	
	public boolean isInput() {
		return input;
	}
}
