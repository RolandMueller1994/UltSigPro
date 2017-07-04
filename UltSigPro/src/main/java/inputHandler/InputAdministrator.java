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

import channel.Channel;

/**
 * Administrates which input devices are available and requests their sampled
 * data, if any {@linkplain Channel} has subscribed them. Then opens a
 * {@linkplain TargetDataLine} to the {@linkplain Mixer}, converts the received
 * bytes to integer and distributes it to the different signal processing paths.
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

	/**
	 * Collects all sound devices and filters the results for input devices.
	 */
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

	/**
	 * 
	 * @return A set of strings with all sound input devices.
	 */
	public Set<String> getInputDevices() {
		return allSoundInputDevices.keySet();
	}

	/**
	 * Returns which devices are subscribed. Adding/Removing of values to this
	 * HashMap is handled by the {@linkplain Channel} class.
	 * 
	 * @return A key set of all subscribed devices.
	 */
	public Set<String> getSubscribedDevicesName() {
		return subscribedDevices.keySet();
	}

	/**
	 * 
	 * @return A HashMap of all open {@linkplain TargetDataLine}s.
	 */
	public HashMap<String, TargetDataLine> getTargetDataLines() {
		return targetDataLines;
	}

	/**
	 * Removes the subscription of the device and closes its
	 * {@linkplain TargetDataLine}.
	 * 
	 * @param name
	 *            The name of the device.
	 */
	public void removeSubscribedDevice(String name) {
		subscribedDevices.remove(name);
		targetDataLines.get(name).stop();
		targetDataLines.get(name).close();
		targetDataLines.remove(name);
	}

	/**
	 * Checks first if the device is already subscribed. In the case of a new
	 * device it opens a {@linkplain TargetDataLine} with predefined values for
	 * sampling rate, bits per sample, ... .
	 * 
	 * @param deviceName
	 *            Name of the new subscribed device.
	 */
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

	/**
	 * Collects the sampled bytes of all subscribed target data lines and
	 * converts it to integer values. 
	 */
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
						// TODO Optimize sleep time after reading
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
