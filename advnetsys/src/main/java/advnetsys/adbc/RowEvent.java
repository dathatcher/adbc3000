package advnetsys.adbc;

import java.util.EventObject;

/** A RowEvent is messaged to a TurboBuffer's registered RowListeners
  * anytime status is about to changed or has changed.  Review the
  * documentation for {@link ADBCEvent} for additional functionality 
  * for the Event.
  */

public class RowEvent extends ADBCEvent {

	/** RowEvent type that is messaged prior to a row insert. */

	public static int ROW_PREINSERT=0;

	/** RowEvent type that is messaged after a row insert. */

	public static int ROW_INSERTED=1;

	/** RowEvent type that is messaged prior to a row delete */

	public static int ROW_PREDELETE=2;

	/** RowEvent type that is messaged after a row delete. */

	public static int ROW_DELETED=3;

	/** RowEvent type that is messaged prior to a row update */

	public static int ROW_PREUPDATE=4;

	/** RowEvent type that is messaged after a row update */

	public static int ROW_UPDATED=5;

	/** RowEvent type that is messaged prior to a row refresh */

	public static int ROW_PREREFRESH=6;

	/** RowEvent type that is messaged after a row refresh */

	public static int ROW_REFRESHED=7;

	/** RowEvent type that is messaged when the current row has been changed */

	public static int ROW_CURRENTCHANGED=8;

	/** RowEvent type that is messaged when the row is validated */

	public static int ROW_VALIDATE=9;

	/** Constructor for RowEvent 
	  * @param source The TurboBuffer.
	  * @param type The RowEvent type.
	  * @param row The row number this event is based.
	  */

	public RowEvent( Object source, int type, int row ) {

		super( source, type, row );

	}

}
