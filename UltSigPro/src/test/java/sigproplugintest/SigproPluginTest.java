package sigproplugintest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.scene.layout.Pane;
import plugins.sigproplugins.SigproPlugin;

public class SigproPluginTest implements SigproPlugin {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pane getGUI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, LinkedBlockingQueue<int[]>> getInputConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOutputConfig(HashMap<String, LinkedBlockingQueue<int[]>> outputConfig) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HashSet<String> getOutputConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPlay(boolean play) {
		// TODO Auto-generated method stub
		
	}

}
