package inputHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Administrates which {@linkplain SoundInputDevice} sends data and receives
 * their data. Then converts the received bytes to integer and distributes it to
 * the different signal processing paths as requested by them. The hashmap of
 * subscribed devices correspond the requested input devices by the
 * {@linkplain Channel}.
 * 
 * @author Kone
 *
 */
public class InputAdministrator {

	private static InputAdministrator inputAdministrator;
	private static HashMap<String, Mixer> allSoundInputDevices;
	private static HashMap<String, Mixer> subscribedDevices;
	private static HashMap<String, TargetDataLine> targetDataLines;
	private static boolean stopped = false;

	public static InputAdministrator getInputAdminstrator() {
		if (inputAdministrator == null) {
			inputAdministrator = new InputAdministrator();
			allSoundInputDevices = new HashMap<>();
			subscribedDevices = new HashMap<>();
			targetDataLines = new HashMap<>();
		}
		return inputAdministrator;
	}

	private InputAdministrator() {
	}

	public void collectSoundInputDevices() {

		Mixer mixer;
		Mixer.Info[] allSoundDevices = AudioSystem.getMixerInfo();
		DataLine.Info info;
		int deviceCounter = 0;
		info = new DataLine.Info(TargetDataLine.class, null);

		for (Mixer.Info i : allSoundDevices) {
			mixer = AudioSystem.getMixer(i);
			if (mixer.isLineSupported(info)) {
				allSoundInputDevices.put(allSoundDevices[deviceCounter].getName(), mixer);
			}
			deviceCounter++;
		}
	}

	public Set<String> getInputDevices() {
		return allSoundInputDevices.keySet();
	}

	public Mixer getSubscribedDevice(String name) {
		return subscribedDevices.get(name);
	}

	public Set<String> getSubscribedDevicesName() {
		return subscribedDevices.keySet();
	}

	public HashMap<String, TargetDataLine> getTargetDataLines() {
		return targetDataLines;
	}

	public void removeSubscribedDevice(String name) {
		subscribedDevices.remove(name);
		targetDataLines.get(name).stop();
		targetDataLines.get(name).close();
		targetDataLines.remove(name);
	}

	public void setSubscribedDevices(String deviceName) {

		if (!subscribedDevices.containsKey(deviceName)) {

			subscribedDevices.put(deviceName, allSoundInputDevices.get(deviceName));

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
			TargetDataLine line = null;

			// AudioFormat(sample rate, bits per sample, channel number
			// (stereo/mono), signed, bigEndian)
			AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, true);

			try {
				line = (TargetDataLine) allSoundInputDevices.get(deviceName).getLine(info);
				line.open(audioFormat);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}

			if (line != null) {
				line.start();
				targetDataLines.put(deviceName, line);
			}
		}
	}

	public void startListening() {

		for (Map.Entry<String, TargetDataLine> entry : targetDataLines.entrySet()) {

			TargetDataLine line = entry.getValue();

			Thread recordThread = new Thread(new Runnable() {

				@Override
				public void run() {

					line.start();
					line.flush();
					System.out.println("Started recording on: " + line);
					System.out.println(line.getFormat());
					List<Integer> intBuffer = new LinkedList<>();

					while (!stopped) {

						// We will only read the current available data. If a
						// fixed size is read, the read()-call will block until
						// the requested data are available. This would cause
						// latency.
						// TODO Optimize sleep time after reading (line 148)
						byte[] data = new byte[line.available()];
						int bytesRead = line.read(data, 0, data.length);
						int byteBuffer = 0, shiftCounter = 0;
						intBuffer = new LinkedList<>();
						if (bytesRead != 0) {
							synchronized (intBuffer) {
								for (shiftCounter = 0; shiftCounter < bytesRead; shiftCounter = shiftCounter + 2) {
									for (int i = 0; i < 2; i++) {
										byteBuffer = byteBuffer << 8;
										byteBuffer = byteBuffer | Byte.toUnsignedInt(data[shiftCounter + i]);
									}
									byteBuffer = byteBuffer << 16;
									byteBuffer = byteBuffer >> 16;
									intBuffer.add(byteBuffer);
									byteBuffer = 0;
								}
							}
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					line.stop();
					line.close();
					System.out.println("Recording stopped on " + line);
				}
			});
			recordThread.start();
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopped = true;
	}
}
