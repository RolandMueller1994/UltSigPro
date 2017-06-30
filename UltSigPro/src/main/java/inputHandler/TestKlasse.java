package inputHandler;

public class TestKlasse {

	public static void startTest() {
		InputAdministrator.getInputAdminstrator();
		SoundInputDevice soundDevice = new SoundInputDevice();
		soundDevice.setSelectedInputDevice(2);
		//soundDevice.setSelectedInputDevice(3);
		Listener listener = new Listener();
		//Listener listener2 = new Listener();
		System.out.println("Registered Devices: " + listener.getRegisteredDevices());
		listener.setSubscription("Primärer Soundaufnahmetreiber");
		//listener2.setSubscription("Mikrofon (Realtek High Definiti");
		System.out.println("Open Lines: " + InputAdministrator.getInputAdminstrator().getTargetDataLines());
		InputAdministrator.getInputAdminstrator().startListening();
	}
}
