package iteratableInput;

public abstract class IteratableInputStream {

	private long startupTime;
	private int cursor;
	private final int samplingFreq = 44100;
	
	public void start() {
		// Set the cursor to start of the sound file
		startupTime = System.currentTimeMillis();
		cursor = 0;
	}

	public long available() {

		// How long the track has been played.
		long timediff = System.currentTimeMillis() - startupTime;

		// Devide by 500 because of 4 bytes per sample -> samplingFreq * 4 /
		// 1000
		long curpos = (long) (timediff * ((double) samplingFreq) / 250);

		if (curpos > cursor) {
			return curpos - cursor;
		}

		return 0;
	}

	public abstract byte[] read(int packageSize);
	
	public int getCursor() {
		return cursor;
	}
	
	public void setCursor(int cursor) {
		this.cursor = cursor;
	}
}
