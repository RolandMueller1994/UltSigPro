package iteratableinput;

import inputhandler.InputAdministrator;

public class IteratableSignalSourceStream extends IteratableInputStream {

	private String name;
	private byte[] dataBuffer;
	private double frequency;
	private int amplitude;
	private final double samplingFrequency = 44100;
	private int samplesPerPeriod;
	private int periodNumber;
	private final int minPackageSize = InputAdministrator.getInputAdminstrator().getOutPackageSize();
	private int signalSourceCursor = 0;

	/**
	 * Constructor for a signal source.
	 */
	public IteratableSignalSourceStream(String name, double frequency, double amplitude) {

		this.name = name;
		this.frequency = (int) frequency;
		this.amplitude = (int) (amplitude * 10000);
		samplesPerPeriod = (int) (samplingFrequency / this.frequency);

		int sinValue = 0;
		double time = 0;

		if (samplesPerPeriod < minPackageSize) {

			// calculate how much periods of the signal are needed, to create
			// more bytes than minPackageSize is
			periodNumber = minPackageSize / samplesPerPeriod + 1;

		} else {
			periodNumber = 1;
		}

		dataBuffer = new byte[2 * periodNumber * samplesPerPeriod];

		// fill the dataBuffer with a integer number of periods
		for (int i = 0; i < (dataBuffer.length / 2); i++) {
			time = i % samplesPerPeriod;
			sinValue = (int) (this.amplitude * Math.sin(2 * Math.PI * (time / samplesPerPeriod)));

			dataBuffer[2 * i] = (byte) ((0xFF00 & sinValue) >> 8);
			dataBuffer[2 * i + 1] = (byte) (0xFF & sinValue);

		}
	}

	@Override
	public byte[] read(int packageSize) {
		byte[] outData = new byte[packageSize];

		signalSourceCursor = getCursor() % dataBuffer.length;

		if (signalSourceCursor + packageSize < dataBuffer.length) {
			System.arraycopy(dataBuffer, signalSourceCursor, outData, 0, packageSize);
		} else {
			int remaining = dataBuffer.length - signalSourceCursor;
			System.arraycopy(dataBuffer, signalSourceCursor, outData, 0, remaining);
			System.arraycopy(dataBuffer, 0, outData, remaining, packageSize - remaining);
		}

		addToCursor(packageSize);
		return outData;
	}

	public String getName() {
		return name;
	}
}
