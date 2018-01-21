package iteratableinput;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class IteratableWaveFileStream extends IteratableInputStream {

	private AudioInputStream inputStream;
	
	public IteratableWaveFileStream(File waveFile) {
		try {
			// At first we capture the complete sound file.
			inputStream = AudioSystem.getAudioInputStream(waveFile);

			int avail = inputStream.available();
			super.setDataBuffer(new byte[avail]);

			inputStream.read(super.getDataBuffer(), 0, avail);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
