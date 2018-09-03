package advnetsys.adbc;

import java.util.EventObject;

/** A ColumnEvent is message to a TurboBuffer's registered ColumnListeners
  * anytime status is about to be changed or has changed with a column.  
  * Review the documentation for {@link ADBCEvent} for additional functionality
  * for this Event.
  */

public class ColumnEvent extends ADBCEvent {

	/** ColumnEvent that is messaged prior to a column change. */

	public static int COLUMN_PREUPDATE=0;

	/** ColumnEvent that is messaged after a column has been changed. */

	public static int COLUMN_UPDATED=1;

	private String columnName;
	private int columnNo;
	private Object value;

	/** The ColumnEvent constructor.
	  * @param source The TurboBuffer.
	  * @param type The ColumnEvent type.
	  * @param row The row number.
	  * @param columnName The name of the column.
	  * @param columnNo The column number.
	  * @param value The new value of the column.
	  */

	public ColumnEvent( Object source, int type, int row, String columnName,
	                    int columnNo, Object value ) {

		super( source, type, row );
		this.columnName = columnName;
		this.columnNo = columnNo;
		this.value = value;

	}

	/** Return the name of the column that this event is based.
	  */

	public String getColumnName() {
		return this.columnName;
	}

	/** Return the number of the column that this event is based.
	  */

	public int getColumnNo() {
		return this.columnNo;
	}

	/** Return the new value of the column.
	  */

	public Object getColumnValue() {
		return this.value;
	}

}
