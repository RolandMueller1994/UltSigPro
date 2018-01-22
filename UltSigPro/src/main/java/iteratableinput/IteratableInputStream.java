package iteratableinput;

public abstract class IteratableInputStream {

	private long startupTime;
	private int cursor;
	private final int samplingFreq = 44100;
	private byte[] nullBuffer;

	public IteratableInputStream() {

		// Data to be written to the output, when the stream has ended
		nullBuffer = new byte[1000];
		for (int i = 0; i < nullBuffer.length; i++) {
			nullBuffer[i] = 0;
		}
	}

	public void start() {
		// Set the cursor to start of the sound file
		startupTime = System.currentTimeMillis();
		cursor = 0;
	}

	public long available(int bytesPerSample) {

		// How long the track has been played.
		long timediff = System.currentTimeMillis() - startupTime;

		// Devide by 500 because of 4 bytes per sample -> samplingFreq * 4 /
		// 1000
		long curpos = (long) (timediff * ((double) samplingFreq) / (1000 / bytesPerSample));

		if (curpos > cursor) {
			return curpos - cursor;
		}

		return 0;
	}

	public abstract byte[] read(int packageSize);

	public byte[] getNullBuffer() {
		return nullBuffer;
	}

	public void setNullBuffer(byte[] nullBuffer) {
		this.nullBuffer = nullBuffer;
	}
	
	public int getCursor() {
		return cursor;
	}
	
	public void addToCursor(int add) {
		cursor += add;
	}
}
