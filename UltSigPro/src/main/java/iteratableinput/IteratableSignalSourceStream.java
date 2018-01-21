package iteratableinput;

import inputhandler.InputAdministrator;

public class IteratableSignalSourceStream extends IteratableInputStream {

	private String name;
	private byte[] byteBuffer;
	private double frequency;
	private int amplitude;
	private final double samplingFrequency = 44100;
	private int samplesPerPeriod;
	private int periodNumber;
	private final int minPackageSize = InputAdministrator.getInputAdminstrator().getOutPackageSize();

	/**
	 * Constructor for a signal source.
	 */
	public IteratableSignalSourceStream(String name, double frequency, double amplitude) {

		super.setSignalSource(true);
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
		
		byteBuffer = new byte[2 * periodNumber * samplesPerPeriod];

		// fill the dataBuffer with a integer number of periods
		for (int i = 0; i < (byteBuffer.length/2); i++) {
			time = i % samplesPerPeriod;
			sinValue = (int) (this.amplitude * Math.sin(2 * Math.PI * (time / samplesPerPeriod)));
			
			byteBuffer[2 * i] = (byte) ((0xFF00 & sinValue) >> 8);
			byteBuffer[2 * i + 1] = (byte) (0xFF & sinValue);
			
		}
		super.setDataBuffer(byteBuffer);
	}
	
	public String getName() {
		return name;
	}
}
