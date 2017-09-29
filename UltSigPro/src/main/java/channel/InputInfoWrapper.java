package channel;

import plugins.sigproplugins.SigproPlugin;

public class InputInfoWrapper {

	private SigproPlugin destPlugin;
	private String destInput;
	
	public InputInfoWrapper(SigproPlugin destPlugin, String destInput) {
		
		this.destPlugin = destPlugin;
		this.destInput = destInput;
	}

	public SigproPlugin getDestPlugin() {
		return destPlugin;
	}

	public void setDestPlugin(SigproPlugin destPlugin) {
		this.destPlugin = destPlugin;
	}

	public String getDestInput() {
		return destInput;
	}

	public void setDestInput(String destInput) {
		this.destInput = destInput;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof InputInfoWrapper) {
			InputInfoWrapper check = (InputInfoWrapper) obj;
			
			return destPlugin.equals(check.getDestPlugin()) && destInput.equals(check.getDestInput());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		
		String name = destPlugin.getName() + destInput;
		
		return name.hashCode();
	}
}
