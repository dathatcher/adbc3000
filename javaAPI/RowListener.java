package advnetsys.adbc;

import java.util.EventListener;

/** The listener interface for receiving row events from a {@link TurboBuffer}.
  * @see RowEvent
  * @see ADBCEvent
  */

public interface RowListener extends EventListener {

	/** Sent prior to a TurboBuffer row insertion.
	  */

	public void rowPreInsert( RowEvent e );

	/** Sent after a TurboBuffer row insertion.
	  */

	public void rowInserted( RowEvent e );

	/** Sent prior to a TurboBuffer row deletion.
	  */

	public void rowPreDelete( RowEvent e );

	/** Sent after a TurboBuffer row deletion.
	  */

	public void rowDeleted( RowEvent e );

	/** Sent prior to a TurboBuffer row update.
	  */

	public void rowPreUpdate( RowEvent e );

	/** Sent after a TurboBuffer row update.
	  */

	public void rowUpdated( RowEvent e );

	/** Sent prior to a TurboBuffer row refresh.
	  */

	public void rowPreRefresh( RowEvent e );

	/** Sent after a TurboBuffer row refresh.
	  */

	public void rowRefreshed( RowEvent e );

	/** Sent after a TurboBuffer's current row has changed via
	  * the TurboBuffer.next() or TurboBuffer.previous() method
	  * calls.
	  */

	public void rowCurrentChanged( RowEvent e );

	/** Sent when a TurboBuffer's current row is being validated via
	  * the TurboBuffer.validateRow() method.
	  */

	public void rowValidate( RowEvent e );

}

