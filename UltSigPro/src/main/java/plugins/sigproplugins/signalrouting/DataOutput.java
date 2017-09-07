package plugins.sigproplugins.signalrouting;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DataOutput {
	
	private PluginConnection con;
	
	public int[] fetch() {
		return con.fetch();
	}
	
	public void setInputConnection(PluginConnection con) {
		this.con = con;
	}
	
}
