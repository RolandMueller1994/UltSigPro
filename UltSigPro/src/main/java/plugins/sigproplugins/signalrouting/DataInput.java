package plugins.sigproplugins.signalrouting;

public class DataInput {

	DataDestinationInterface dest;
	
	public void setDataDestination(DataDestinationInterface dest) {
		this.dest = dest;
	}

	public void putData(int[] data) {
		dest.putData(data);
	}
	
}
