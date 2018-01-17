package iteratableInput;

import javax.sound.sampled.AudioInputStream;

public class IteratableSignalSourceStream extends IteratableInputStream {

	AudioInputStream inputStream;
	private byte[] dataBuffer;
	private final int samplingFreq = 44100;
	private int frequency = 1000;
	
	/**
	 * Constructor for a signal source.
	 */
	public IteratableSignalSourceStream() {
		int value = 0;
		inputStream = null;
		
		dataBuffer = new byte[samplingFreq];
		for (int i = 0; i < (samplingFreq /2); i++) {
			value = (int) (5000*Math.sin(frequency*i));
			dataBuffer[2 * i] = (byte) ((value & 0xFF00) >> 8);
			dataBuffer[2 * i + 1] = (byte) ((value & 0xFF));						
		}
	}
	
	
	@Override
	public byte[] read(int packageSize) {
		int cursor = getCursor();
		
		return null;
	}

}
