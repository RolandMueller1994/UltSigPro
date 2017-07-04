package inputHandler;

import OutputHandler.OutputAdministrator;
import channel.Channel;

public class TestKlasse {

	public static void startTest() {
		/*InputAdministrator.getInputAdminstrator();
		InputAdministrator.getInputAdminstrator().collectSoundInputDevices();
		System.out.println("All devices: " + InputAdministrator.getInputAdminstrator().getInputDevices());
		//Channel channel1 = new Channel();
		//channel1.setSubscription("Primï¿½rer Soundaufnahmetreiber");
		System.out.println("Subscribed Devices: " + InputAdministrator.getInputAdminstrator().getSubscribedDevicesName());
		System.out.println("Open Lines: " + InputAdministrator.getInputAdminstrator().getTargetDataLines());
		InputAdministrator.getInputAdminstrator().startListening();
		//channel1.removeSubscription("Primï¿½rer Soundaufnahmetreiber");
		System.out.println("Subscription removed!");
		System.out.println("Subscribed Devices: " + InputAdministrator.getInputAdminstrator().getSubscribedDevicesName());
		System.out.println("Open Lines: " + InputAdministrator.getInputAdminstrator().getTargetDataLines());
		InputAdministrator.getInputAdminstrator().startListening();*/
		OutputAdministrator.getOutputAdministrator().collectSoundOutputDevices();
		System.out.println("Output devices: " + OutputAdministrator.getOutputAdministrator().getOutputDevices());
		OutputAdministrator.getOutputAdministrator().setSelectedDevice("Primärer Soundtreiber");
		System.out.println("Selected devices: " + OutputAdministrator.getOutputAdministrator().getSourceDataLines());
	}
}
