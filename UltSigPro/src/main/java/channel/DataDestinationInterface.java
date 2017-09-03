package channel;

import javax.annotation.Nonnull;

/**
 * Common interface to recieve data from the signal processing system.
 * 
 * @author roland
 *
 */
public interface DataDestinationInterface {

	/**
	 * Insert data in the implementation of this interface.
	 * 
	 * @param data
	 *            a array of int's. Must not be null.
	 */
	void putData(@Nonnull int[] data);

}
