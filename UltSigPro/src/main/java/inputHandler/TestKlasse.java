package inputHandler;

public class TestKlasse {

	public static void startTest() {
		InputAdministrator.getInputAdminstrator();
		InputAdministrator.getInputAdminstrator().collectSoundInputDevices();
		System.out.println("All devices: " + InputAdministrator.getInputAdminstrator().getInputDevices());
		Channel channel1 = new Channel();
		channel1.setSubscription("Primärer Soundaufnahmetreiber");
		System.out.println("Subscribed Devices: " + InputAdministrator.getInputAdminstrator().getSubscribedDevicesName());
		System.out.println("Open Lines: " + InputAdministrator.getInputAdminstrator().getTargetDataLines());
		InputAdministrator.getInputAdminstrator().startListening();
		channel1.removeSubscription("Primärer Soundaufnahmetreiber");
		System.out.println("Subscription removed!");
		System.out.println("Subscribed Devices: " + InputAdministrator.getInputAdminstrator().getSubscribedDevicesName());
		System.out.println("Open Lines: " + InputAdministrator.getInputAdminstrator().getTargetDataLines());
		InputAdministrator.getInputAdminstrator().startListening();
	}
}
