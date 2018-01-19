package iteratableInput;

import inputhandler.InputAdministrator;

public class IteratableSignalSourceStream extends IteratableInputStream {

	private int[] dataBuffer;
	private int frequency = 600;
	private int amplitude = 4000;
	private final int samplingFrequency = 44100;
	private int samplesPerPeriod;
	private int periodNumber;
	private final int minPackageSize = InputAdministrator.getInputAdminstrator().getOutPackageSize();

	/**
	 * Constructor for a signal source.
	 */
	public IteratableSignalSourceStream() {

		samplesPerPeriod = samplingFrequency / frequency;
		int sinValue = 0;
		double time = 0;

		if (samplesPerPeriod < minPackageSize) {

			// calculate how much periods of the signal are needed, to create
			// more bytes than minPackageSize is
			periodNumber = minPackageSize / samplesPerPeriod + 1;
			
		} else {
			periodNumber = 1;
		}
		
		dataBuffer = new int[periodNumber * samplesPerPeriod];

		// fill the dataBuffer with a integer number of periods
		for (int i = 0; i < dataBuffer.length; i++) {
			time = i % samplesPerPeriod;
			sinValue = (int) (amplitude * Math.sin(2 * Math.PI * (time / samplesPerPeriod)));
			dataBuffer[i] = sinValue;
		}
	}

	@Override
	public int[] readInt(int packageSize) {

		int cursor = getCursor();
		int[] outData = new int[packageSize];
		int srcPos = cursor % (dataBuffer.length);

		if (srcPos + packageSize < dataBuffer.length) {
			System.arraycopy(dataBuffer, srcPos, outData, 0, packageSize);
		} else {
			System.arraycopy(dataBuffer, srcPos, outData, 0, (dataBuffer.length - srcPos));
			System.arraycopy(dataBuffer, 0, outData, (dataBuffer.length - srcPos),
					(packageSize - (dataBuffer.length - srcPos)));
		}
		setCursor(cursor + packageSize);

		return outData;
	}

	@Override
	public byte[] read(int packageSize) {
		return null;
	}

}
