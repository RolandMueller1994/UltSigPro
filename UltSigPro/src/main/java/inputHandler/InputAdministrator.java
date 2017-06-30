package inputHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Administrates which {@linkplain SoundInputDevice} sends data and receives
 * their data. Then converts the received bytes to integer and distributes it to
 * the different signal processing paths as requested by them. The list/hashmap
 * of registered devices of this class correspond the created
 * {@linkplain SoundInputDevices}, the list/hashmap of subscribed devices
 * correspond the requested input devices by the {@linkplain Listener}.
 * 
 * @author Kone
 *
 */
public class InputAdministrator {

	private static InputAdministrator inputAdministrator;
	private static HashMap<String, Mixer> registeredDevices;
	private static List<String> registeredDevicesList;
	private static HashMap<String, Mixer> subscribedDevices;
	private static List<String> subscribedDevicesList;
	private static HashMap<String, TargetDataLine> targetDataLines;
	private static boolean stopped = false;

	public static InputAdministrator getInputAdminstrator() {
		if (inputAdministrator == null) {
			inputAdministrator = new InputAdministrator();
			registeredDevices = new HashMap<>();
			subscribedDevices = new HashMap<>();
			registeredDevicesList = new LinkedList<>();
			subscribedDevicesList = new LinkedList<>();
			targetDataLines = new HashMap<>();
		}
		return inputAdministrator;
	}

	private InputAdministrator() {
	}

	public Mixer getRegisteredDevice(String name) {
		return registeredDevices.get(name);
	}

	public List<String> getRegisteredDevicesName() {
		return registeredDevicesList;
	}

	public Mixer getSubscribedDevice(String name) {
		return subscribedDevices.get(name);
	}

	public List<String> getSubscribedDevicesName() {
		return subscribedDevicesList;
	}

	public HashMap<String, TargetDataLine> getTargetDataLines() {
		return targetDataLines;
	}

	public void removeRegisteredDevice(String name) {
		registeredDevices.remove(name);
		registeredDevicesList.remove(name);
	}

	public void removeSubscribedDevice(String name) {
		subscribedDevices.remove(name);
		subscribedDevicesList.remove(name);
	}

	public void setRegisteredDevice(String name, Mixer newDevice) {
		registeredDevices.put(name, newDevice);
		registeredDevicesList.add(name);
	}

	public void setSubscribedDevices(String deviceName) {

		if (!subscribedDevices.containsKey(deviceName)) {

			subscribedDevices.put(deviceName, getRegisteredDevice(deviceName));
			subscribedDevicesList.add(deviceName);

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
			TargetDataLine line = null;
			AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
			try {
				line = (TargetDataLine) getRegisteredDevice(deviceName).getLine(info);
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
			List<Byte> buffer = new LinkedList<>();
			
			
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
						int shiftBuffer = 0, shiftCounter = 0;
						intBuffer = new LinkedList<> ();
						if (bytesRead != 0) {
							synchronized (intBuffer) {
								for (shiftCounter = 0; shiftCounter < bytesRead; shiftCounter = shiftCounter + 2) {
									for (int i = 0; i < 2; i++) {
										shiftBuffer = shiftBuffer << 8;
										shiftBuffer = shiftBuffer | data[shiftCounter+i];
									}
									System.out.println("***" + shiftBuffer);
									shiftBuffer = shiftBuffer << 16;
									System.out.println(shiftBuffer);
									shiftBuffer = shiftBuffer >> 16;
									intBuffer.add(shiftBuffer);
									System.out.println(shiftBuffer);
									System.out.println(intBuffer.get(shiftCounter/2));
									shiftBuffer = 0;
								}
							}
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
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
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stopped = true;
	}
}
