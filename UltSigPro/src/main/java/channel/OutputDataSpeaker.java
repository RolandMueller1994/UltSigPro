package channel;

import java.util.LinkedList;

/**
 * Interface between {@link Channel} class and {@link OutputAdministrator}
 * class. Allows the OutputAdministrator to fetch signal processed data from
 * channels.
 * 
 * @author Kone
 *
 */
public interface OutputDataSpeaker {

	LinkedList<Integer> fetchData();
}
