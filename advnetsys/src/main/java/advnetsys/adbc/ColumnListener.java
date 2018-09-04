package advnetsys.adbc;

import java.util.EventListener;

/** The listener interface for receiving column events from a 
  * {@link TurboBuffer}.
  * @see ColumnEvent
  * @see ADBCEvent
  */

public interface ColumnListener extends EventListener {

	/** Sent prior to a TurboBuffer column value change.
	  */

	public void columnPreUpdate( ColumnEvent e );

	/** Sent after a TurboBuffer column value change.
	  */

	public void columnUpdated( ColumnEvent e );

}

