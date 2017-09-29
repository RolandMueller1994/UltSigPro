package channel;

import plugins.sigproplugins.SigproPlugin;

public class OutputInfoWrapper {

	private SigproPlugin sourcePlugin;
	private String sourceOutput;
	
	public OutputInfoWrapper(SigproPlugin sourcePlugin, String sourceOutput) {
		
		this.sourcePlugin = sourcePlugin;
		this.sourceOutput = sourceOutput;
	}

	public SigproPlugin getSourcePlugin() {
		return sourcePlugin;
	}

	public void setSourcePlugin(SigproPlugin sourcePlugin) {
		this.sourcePlugin = sourcePlugin;
	}

	public String getSourceOutput() {
		return sourceOutput;
	}

	public void setSourceOutput(String sourceOutput) {
		this.sourceOutput = sourceOutput;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof OutputInfoWrapper) {
			OutputInfoWrapper check = (OutputInfoWrapper) obj;
			
			return sourcePlugin.equals(check.getSourcePlugin()) && sourceOutput.equals(check.getSourceOutput());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		
		String name = sourcePlugin.getName() + sourceOutput;
		
		return name.hashCode();
	}
}
