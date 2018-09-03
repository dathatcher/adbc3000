package advnetsys.adbc;

/** The TurboKey class allows a {@link TurboBuffer} to perform keyed
  * reads on KSAM files and also Third Party Indexing reads on a 
  * dataset.
  */

public class TurboKey extends Object {

	private String columnName=null;
	private int columnNo=0;
	private int keyMode=0;
	private int keyLength=0;
	private boolean userSpecified=false;
	private TurboColumn userSpecifiedKeyColumn;

	public TurboKey() {}

	/** Constructor.
	  * @param columnName The name of the key column.
	  * @param keyMode Either the TPI DBFIND mode for datasets or
	  *  the relational operator(relop) for a KSAM file.
	  */

	public TurboKey( String columnName, int keyMode ) {
		setColumnName( columnName );
		setKeyMode( keyMode );
	}

	/** Constructor.
	  * @param columnNo The column number..
	  * @param keyMode Either the TPI DBFIND mode for datasets or
	  *  the relational operator(relop) for a KSAM file.
	  */

	public TurboKey( int columnNo, int keyMode ) {
		setColumnNo( columnNo );
		setKeyMode( keyMode );
	}

	/** Constructor.
	  * @param columnName The name of the key column.
	  * @param keyMode Either the TPI DBFIND mode for datasets or
	  *  the relational operator(relop) for a KSAM file.
	  * @param keyLength The length of the search key.
	  */

	public TurboKey( String columnName, int keyMode, int keyLength ) {
		this( columnName, keyMode );
		setKeyLength( keyLength );
	}

	/** Constructor.
	  * @param columnNo The column number..
	  * @param keyMode Either the TPI DBFIND mode for datasets or
	  *  the relational operator(relop) for a KSAM file.
	  * @param keyLength The length of the search key.
	  */

	public TurboKey( int columnNo, int keyMode, int keyLength ) {
		this( columnNo, keyMode );
		setKeyLength( keyLength );
	}

	/** Constructor that is used to specify a custom key field used primarily 
	  * for TPI retrievals.  
	  * @param columnName The name of the key column.
	  * @param keyMode Either the TPI DBFIND mode for datasets or
	  *  the relational operator(relop) for a KSAM file.
	  * @param columnType The Image column type.
	  * @param keyLength The length of the search key.
	  */

	public TurboKey( String columnName, int keyMode, String columnType, int keyLength ) {
		this( columnName, keyMode );
		setKeyLength( keyLength );
		RowSet rs = new RowSet();
		rs.addColumn( columnName, columnType, (short) 0, keyLength );
		try {
			userSpecifiedKeyColumn = rs.getColumn( 1 );
		} catch (ADBCException e) {e.printStackTrace();}
		userSpecified = true;
		rs = null;
	}

	public final void setColumnName( String columnName ) {
		this.columnName = columnName;
	}

	public final void setColumnNo( int columnNo ) {
		this.columnNo = columnNo;
	}

	public final void setKeyMode( int keyMode ) {
		this.keyMode = keyMode;
	}

	public final void setKeyLength( int keyLength ) {
		this.keyLength = keyLength;
	}

	boolean isUserSpecified() {
		return userSpecified;
	}

	TurboColumn getUserSpecifiedKeyColumn() {
		return userSpecifiedKeyColumn;
	}

	public final String getColumnName() {
		return this.columnName;
	}

	public final int getColumnNo() {
		return this.columnNo;
	}

	public final int getKeyMode() {
		return this.keyMode;
	}

	public final int getKeyLength() {
		return this.keyLength;
	}
}
