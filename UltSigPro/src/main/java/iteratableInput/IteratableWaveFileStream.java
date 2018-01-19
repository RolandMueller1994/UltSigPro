package iteratableInput;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class IteratableWaveFileStream extends IteratableInputStream {

	private AudioInputStream inputStream;
	private byte[] dataBuffer;
	private byte[] nullBuffer;
	
	public IteratableWaveFileStream(File waveFile) {
		try {
			// At first we capture the complete sound file.
			inputStream = AudioSystem.getAudioInputStream(waveFile);

			int avail = inputStream.available();
			dataBuffer = new byte[avail];

			inputStream.read(dataBuffer, 0, avail);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Array filled with zeros -> We can just copy this array afterwards
		// and don't have to write zeros manually.
		nullBuffer = new byte[1000];

		for (int i = 0; i < nullBuffer.length; i++) {
			nullBuffer[i] = 0;
		}
	}
	
	@Override
	public byte[] read(int packageSize) {
		
		byte[] outData = new byte[packageSize];
		int cursor = getCursor();
		
		// Check if there are enough data from file or if we have to write
		// zeros.
		if (cursor + packageSize < dataBuffer.length) {
			System.arraycopy(dataBuffer, cursor, outData, 0, packageSize);
			cursor += packageSize;
		} else if (cursor < dataBuffer.length) {
			// Still date to write but not enough for packageSize
			int remaining = dataBuffer.length - cursor;
			System.arraycopy(dataBuffer, cursor, outData, 0, remaining);
			System.arraycopy(nullBuffer, 0, dataBuffer, remaining, packageSize - remaining);
			cursor += packageSize;
		} else {
			// Only zeros will be written
			System.arraycopy(nullBuffer, 0, outData, 0, packageSize);
			cursor += packageSize;
		}
		
		setCursor(cursor);
		return outData;
	}

	@Override
	public int[] readInt(int packageSize) {
		return null;
	}

}
