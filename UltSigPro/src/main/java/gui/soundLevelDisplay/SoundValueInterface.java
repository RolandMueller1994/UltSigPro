package gui.soundLevelDisplay;

import java.util.LinkedList;

public interface SoundValueInterface {
	
	public void updateSoundLevelItems(String deviceName, LinkedList<Integer> soundValues, boolean input);

}
