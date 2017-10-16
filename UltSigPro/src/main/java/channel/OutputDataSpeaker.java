package channel;

/**
 * Interface between {@link Channel} class and {@link OutputAdministrator}
 * class. Allows the OutputAdministrator to fetch signal processed data from
 * channels.
 * 
 * @author Kone
 *
 */
public interface OutputDataSpeaker {

	int[] fetchData();
}
