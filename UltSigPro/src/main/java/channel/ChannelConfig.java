package channel;

import java.util.Collection;

public class ChannelConfig {
	
	private String name;
	private Collection<String> inputDevices;
	private Collection<String> outputDevices;
	
	public ChannelConfig(String name, Collection<String> inputDevices, Collection<String> outputDevices) {
		this.name = name;
		this.inputDevices = inputDevices;
		this.outputDevices = outputDevices;
	}

	public String getName() {
		return name;
	}

	public Collection<String> getInputDevices() {
		return inputDevices;
	}

	public Collection<String> getOutputDevices() {
		return outputDevices;
	}

}
