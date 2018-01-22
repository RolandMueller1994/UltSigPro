package iteratableinput;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class IteratableWaveFileStream extends IteratableInputStream {

	private AudioInputStream inputStream;
	private byte[] dataBuffer;

	public IteratableWaveFileStream(File waveFile) {
		try {
			// At first we capture the complete sound file.
			inputStream = AudioSystem.getAudioInputStream(waveFile);

			int avail = inputStream.available();
			dataBuffer = new byte[avail];
			inputStream.read(dataBuffer, 0, avail);
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte[] read(int packageSize) {
		int cursor = getCursor();
		int avail = dataBuffer.length;
		byte[] outData = new byte[packageSize];

		if (cursor + packageSize < avail) {
			System.arraycopy(dataBuffer, cursor, outData, 0, packageSize);

		} else if (cursor < avail) {
			// Still date to write but not enough for packageSize
			int remaining = avail - cursor;
			System.arraycopy(dataBuffer, cursor, outData, 0, remaining);
			System.arraycopy(getNullBuffer(), 0, dataBuffer, remaining, packageSize - remaining);

		} else {
			// Only zeros will be written
			System.arraycopy(getNullBuffer(), 0, outData, 0, packageSize);
		}

		addToCursor(packageSize);
		return outData;
	}
}
