package iteratableinput;

public abstract class IteratableInputStream {

	private long startupTime;
	private int cursor;
	private final int samplingFreq = 44100;
	private byte[] dataBuffer;
	private byte[] nullBuffer;
	private boolean signalSource = false;
	private int signalSourceCursor = 0;

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

	public byte[] read(int packageSize) {
		byte[] outData = new byte[packageSize];

		if (signalSource) {
			signalSourceCursor = cursor % dataBuffer.length;

			if (signalSourceCursor + packageSize < dataBuffer.length) {
				System.arraycopy(dataBuffer, signalSourceCursor, outData, 0, packageSize);
			} else {
				int remaining = dataBuffer.length - signalSourceCursor;
				System.arraycopy(dataBuffer, signalSourceCursor, outData, 0, remaining);
				System.arraycopy(dataBuffer, 0, outData, remaining, packageSize - remaining);
			}
			
		} else {
			if (cursor + packageSize < dataBuffer.length) {
				System.arraycopy(dataBuffer, cursor, outData, 0, packageSize);

			} else if (cursor < dataBuffer.length) {
				// Still date to write but not enough for packageSize
				int remaining = dataBuffer.length - cursor;
				System.arraycopy(dataBuffer, cursor, outData, 0, remaining);
				System.arraycopy(nullBuffer, 0, dataBuffer, remaining, packageSize - remaining);

			} else {
				// Only zeros will be written
				System.arraycopy(nullBuffer, 0, outData, 0, packageSize);
			}
		}
		cursor += packageSize;

		return outData;
	}

	public byte[] getDataBuffer() {
		return dataBuffer;
	}

	public void setDataBuffer(byte[] dataBuffer) {
		this.dataBuffer = dataBuffer;
	}

	public byte[] getNullBuffer() {
		return nullBuffer;
	}

	public void setNullBuffer(byte[] nullBuffer) {
		this.nullBuffer = nullBuffer;
	}

	public void setSignalSource(boolean bool) {
		signalSource = bool;
	}
}
