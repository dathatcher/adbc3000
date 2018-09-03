package advnetsys.adbc;

	/*FIX
	  FIX0001 08/05/1999 EVAALA Method getColumnName throw class cast exception for ComputedColumn.

	 */

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.math.*;

/** A TurboBuffer is a buffer object that contains a record read from an
  * HP3000 MPE data source.  The primary data sources used by a TurboBuffer
  * are a Turbo Image dataset - {@link Dataset advnetsys.adbc.Dataset}, an MPE File
  * - {@link MpeFile advnetsys.adbc.MpeFile}, and a KSAM XL file - {@link KsamXL advnetsys.adbc.KsamXL}.
  * Each of these data sources implement the advnetsys.adbc.DataTable interface, 
  * and will from here on out be referred to as data tables.
  * <BR><BR>
  * A TurboBuffer is actually a combination of a row buffer and a row cursor.
  * It is a navigator inside a data table.  Set the course via the {@link #initiateRead}
  * methods, and then navigate with the {@link #next} and {@link #previous} methods.  On each
  * row, extract the needed information from the buffer using the {@link #getObject getXXX()} methods.
  * Modify the buffer using the {@link #updateObject updateXXX()} methods.  Transfer the changes to 
  * the data table via the {@link #insertRow()}, {@link #updateRow()} and {@link #deleteRow()} methods.
  * To insure data integrity, create a transaction locking scheme using the 
  * {@link #lock()}, {@link #unLock()}, {@link #setUniqueKey setUniqueKey()}, {@link #setVerifyOn setVerifyOn()}, 
  * and {@link #setAutoLock setAutoLock()} methods.
  * <BR><BR>
  * Performance can be tuned by caching rows within a TurboBuffer.  Apply the 
  * {@link #setFetchSize setFetchSize()} method to adjust the number of records to be held in cache.
  * Fetch size also implies the number of rows retrieved with a single TurboBuffer
  * read request to the data table.
  * <BR><BR>
  * If various calculations need to be performed on the buffer's data to produce a result,
  * or a data column needs to be divied into subsets, add a computed column to the
  * TurboBuffer using the {@link #addComputedColumn addComputedColumn()} method.  A buffer can produce the 
  * result of a computed column using the same getXXX() methods that are called on
  * a standard data column.  In addition, a computed column can be set up to 
  * accept the updateXXX() methods to modify data based off these same calculations.
  * A programmer must create their own computed column by either implementing the
  * {@link ComputedColumn advnetsys.adbc.ComputedColumn} interface, or extending the 
  * {@link AbstractComputedColumn advnetsys.adbc.AbstractComputedColumn} class.
  * <BR><BR>
  * A TurboBuffer's status can be monitored via row/column listeners and events.  In addition 
  * to status, data validation can also be accomplished using these same listeners and events.
  * Use the {@link #addRowListener addRowListener()} and {@link #addColumnListener addColumnListener()} methods to add these listeners.  For
  * data validation, the row and column events can be set to fail with a default validation
  * exception, or a programmer defined exception.  These listeners can also be used
  * to notify other objects, either visual or non-visual, of data changes that are taking
  * place within the TurboBuffer.
  * <BR><BR>
  * A TurboBuffer, using {@link ADBCDateFormat advnetsys.adbc.ADBCDateFormat}, has the ability to interpret dates from
  * the raw data types of Turbo Image.  On creating an instance of ADBCDateFormat, an internal
  * date format must be specified in the constructor.  The internal date format is either
  * represented as a string containing the MMddyyyy type designators, or it is a date containing
  * the epoch for a julian date system.  If a date routine at a specific site is more
  * complicated than the standard feartures mentioned in ADBCDateFormat, ADBCDateFormat can 
  * be extended and certain methods overridden to process the date.
  * The ADBCDateFormat instance is placed in the TurboBuffer
  * using the {@link #setFormat setFormat()} method.
  * <BR><BR>
  * The {@link #setFormat setFormat()} method also is used to apply number formats to to TurboBuffer columns
  * that contain numeric information.  Custom formatters can also be created for handling
  * numeric, string, or date values.
  * <BR><BR>
  * Essentially, the TurboBuffer class ties together data residing on an HP3000
  * with an organization's business rules encapsulated in ADBC computed columns
  * and row/column listeners and events.
  */

public class TurboBuffer extends Object {

	private static int SERIAL=2;
	private static int BACKWARD_SERIAL=3;
	private static int DIRECTED=4;
	private static int CHAINED=5;
	private static int BACKWARD_CHAINED=6;
	private static int CALCULATED=7;

	private DataTable dataTable;
	private RowSet rowSet;
	private Mpe3000 mpe3000;
	private Vector rowListeners = null;
	private Vector columnListeners = null;

	private TurboBufferThread turboBufferThread=new TurboBufferThread();
	private boolean autoLockEnabled=false;
	private Vector columns = new Vector();
	private Vector columnAlias = new Vector();
	private Hashtable columnHash = new Hashtable();
	private Hashtable columnOrder = new Hashtable();
	private Hashtable columnFormat = new Hashtable();
	private byte[] verifyOriginalValue;
	private int numUniqueColumns=0;
	private short numColumns=0;
	private int numComputedColumns=0;
	private short length=0;
	private int readMode = SERIAL;   // default
	private int inverseReadMode = BACKWARD_SERIAL;
	private int fetchReadMode = SERIAL;
	private TurboColumn keyColumn;
	private TurboColumn recordIdColumn = new TurboInt("rec#",(short) 0, "I", 4);
	private Object keyValue;
	private Row currRow;
	private Row insertRow;
	private Row currRowBackup;
	private int onInsertRow=0;
	private MpeSocket mpeSocket;
	private int fetchSize=1;
	private int dataTableReset=1;
	private CircularBuffer cBuffer;
	private boolean fetching=false;
	private int lastRecordNumber=-1;
	private boolean isMpeFile=false;
	private boolean isKsamFile=false;
	private int[] offsetArray=null;
	private int[] scaleArray=null;
	private byte[] copyBuffer=null;
	private long registryId;
	private String toStringColumn=null;
	private String name="";
	private TurboKey turboKey=null; //2.0.1
	private ADBCException threadException=null;
        /// DAVE PERCobol
        private boolean readstatus=false;
     
	//private Vector tracker = new Vector();
        private boolean get_record_length=false;
        private TableInfo ti = new TableInfo();
        private Intrinsic in=null;
        private int db;
	private int prevRecNo=0;    // Used for keeping track of chained read items after a row delete.
	private int nextRecNo=0;    // ""   ""  "" ...

	/** Create a TurboBuffer using the specified dataTable as a data source.  
	  * A TurboBuffer can also be created using the Database.createTurboBuffer()
	  * method.
	  * @param dataTable The data table to extract, update, insert and delete data.
	  */

	public TurboBuffer( DataTable dataTable ) {
		this.setDataTable( dataTable );
		//rowSet = dataTable.getRowSet();
		//mpe3000 = dataTable.getMpe3000();
	}

	public TurboBuffer( DataTable dataTable, RowSet rowSet ) {
		this.setDataTable( dataTable );
		this.rowSet = rowSet;
	}

	/** Create a TurboBuffer.  Use the TurboBuffer.setDataTable method to
	  * assign the data table.
	  */

	public TurboBuffer() {}

	/** Set an instance of a Dataset/MpeFile/KsamXL object as the dataTable for
	  * this TurboBuffer.
	  */

	public final void setDataTable( DataTable dataTable ) {
		if ( this.dataTable != null ) { //2.0.1
			resetInternal();
		}
		this.dataTable = dataTable;
		this.rowSet = dataTable.getRowSet();
		this.mpe3000 = dataTable.getMpe3000();
		this.isMpeFile = ( dataTable.getType() >= 0 );
		this.registryId = mpe3000.getRegistryId();
		if ( this.isMpeFile ) {
			//this.isKsamFile = ( ( dataTable.getType() & Foption.KSAMXL ) > 0 );
			this.isKsamFile = ( ( dataTable.getType() & Foption.KSAMXL ) == Foption.KSAMXL ); //2.0.1
		}
		if ( "".equals(this.name) ) setName( dataTable.getName() );
		if ( ! dataTable.isTBInitOK() ) return;
	}

	public final void setName( String name ) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}
	
	/*2.0.1*/
	/** Used by TurboBufferThread to identify what dataTable type this TurboBuffer 
	  * is associated.
	  */

	final boolean isMpeFile() {
		return isMpeFile;
	}

	/** Locks the TurboBuffer's dataTable.
	  * @param lockMode The mode of the lock - LOCK_UNCONDITIONAL or LOCK_CONDITIONAL.
	  * @exception ADBCException if the lock fails or a network error occurs.
	  */

	public final synchronized void lock( int lockMode ) throws ADBCException {
	
		fetchWait();
		dataTable.lock( lockMode );
	
	}

	/** Locks the TurboBuffer's DataTable with the DataTable's default lock mode.
	  * @exception ADBCException if the lock fails.
	  */

	public final synchronized void lock() throws ADBCException {

		fetchWait();
		dataTable.lock();
	
	}

	/** unlocks the TurboBuffer's dataTable.
	  * @exception ADBCException if the unlock fails.
	  */

	public final synchronized void unLock() throws ADBCException {

		fetchWait();
		dataTable.unLock();

	}

	private synchronized void fetchWait()  throws ADBCException {

                if ( !mpe3000.isConnected() ){ 
                    System.out.println("Lost Connection to Host");
                    throw new ADBCException("Mpe3000:No Connection to Server",40200);
                   // return;
                }
               	while ( fetching )
                       try { wait();} catch (InterruptedException e) { e.printStackTrace();}
                      // System.out.println("Back from fetch waiting...");
	}

	/*2.0.1*/
	private synchronized void resetInternal() {

		try {
                fetchWait();
                }catch (ADBCException e) { e.printStackTrace();}
		columns.removeAllElements();
		columnAlias.removeAllElements(); //2.0.1
		columnHash.clear();
		columnOrder.clear();
		columnFormat.clear();
		numColumns = 0;
		numComputedColumns = 0;
		numUniqueColumns = 0;
		rowListeners = null;
		columnListeners = null;
	}


	/** Specifies the desired dataTable columns for the TurboBuffer.
	  * @param columnNames A comma seperated list of the desired column names. 
	  * The list can also contain ranges of columns in the form "columnName1:columnName4".
	  * @exception ADBCException if a column does not exist, or database
	  * and networking errors.
	  */

	public final synchronized void setColumns( String columnNames ) throws ADBCException {

		//fetchWait(); 2.0.1

		//columns.removeAllElements(); 2.0.1
		//columnHash.clear();  2.0.1
		//columnOrder.clear();  2.0.1
		//columnFormat.clear(); 2.0.1
		//numColumns = 0; 2.0.1
		//numComputedColumns = 0; 2.0.1
		//numUniqueColumns = 0; 2.0.1

		resetInternal();  //2.0.1

		ColumnNameParser columnParser = new ColumnNameParser( columnNames, rowSet.getColumnOrder(), rowSet.getNumColumns(), this.name, "setColumns", 43100 );

		Enumeration cols = columnParser.elements();

		while ( cols.hasMoreElements() ) {
			Integer colIndex = (Integer) cols.nextElement();
			this.add( rowSet.getColumn( colIndex.intValue() ) );
		}

		columnParser = null;

		verifyOriginalValue = new byte[this.numColumns];
		for (int i=0;i<verifyOriginalValue.length;i++) verifyOriginalValue[i] = 0;
		offsetArray = new int[this.numColumns];
		scaleArray = new int[this.numColumns];

		int offset = 6;
		offsetArray[0] = offset;

		for (int i=0;i<this.numColumns;i++) {
			offsetArray[i] = offset;
			offset += ((TurboColumn) columns.elementAt(i)).getSize();
			scaleArray[i] = 0;
		}

	}

	/** Set the specified columns, when modified, to verify their original values against 
	  * the values that are currently in the Database or file.
	  * When the TurboBuffer.updateRow() method is called, if any of the columns specified
	  * in this method have been modified, the original TurboBuffer values of these columns are compared
	  * against the current values in the associated record of the Turbo Image dataset or MPE file.
	  * Basically, this method checks to see if another user has modified the record in question after
	  * it was read by this TurboBuffer in a non locking mode.  If a record has been changed,
	  * the current values for this record can be read using the TurboBuffer.refreshRow() method.
	  * <B>This method is not a required method.</B>  It is an aid to determine 
	  * if a locked read needs to be performed prior to an update.  It is recomended by the authors
	  * of this software that when performing transactions, use either Database.lock() or TurboBuffer.lock()
	  * to lock the data that is to be read and then updated, refreshed, or deleted.  This method 
	  * allows the ability to program a looser locking scheme, but still maintain data integrity.
	  * @param columnNames A comma seperated list of the columns to be verified.
	  */

	public final synchronized void setVerifyOn( String columnNames ) throws ADBCException {

		fetchWait();

		ColumnNameParser columnParser = new ColumnNameParser( columnNames, this.columnOrder, this.numColumns, this.name, "setVerifyOn",43110 );

		Enumeration cols = columnParser.elements();

		while ( cols.hasMoreElements() ) {
			Integer colIndex = (Integer) cols.nextElement();
			int colI = colIndex.intValue();

			if ( verifyOriginalValue[colI - 1] != 2 ) verifyOriginalValue[colI - 1] = 1;
		}
	}

	/** Set the specified columns to be a unique key for a record in a DataTable.  These columns
	  * will allow the TurboBuffer to determine if another user has replaced the current record 
	  * by a record representing different key elements.  These columns are used by the 
	  * updateRow(), deleteRow() and refreshRow() methods.
	  * <B>This method is not a required method.</B>  It is an aid to determine 
	  * if a locked read needs to be performed prior to an update, refresh, or delete.  
	  * It is recomended by the authors
	  * of this software that when performing transactions, use either Database.lock() or TurboBuffer.lock()
	  * to lock the data that is to be read and then updated, refreshed, or deleted.  This method 
	  * allows the ability to program a looser locking scheme, but still maintain data integrity.
	  * @param columnNames A comma seperated list of the columns to represent a unique key.
	  */

	public final synchronized void setUniqueKey( String columnNames ) throws ADBCException {

		fetchWait();

		numUniqueColumns = 0;

		ColumnNameParser columnParser = new ColumnNameParser( columnNames, this.columnOrder, this.numColumns, this.name, "setUniqueKey",43120 );

		Enumeration cols = columnParser.elements();

		while ( cols.hasMoreElements() ) {
			Integer colIndex = (Integer) cols.nextElement();
			int colI = colIndex.intValue();

			verifyOriginalValue[ colI - 1 ] = 2;
			numUniqueColumns++;
		}
	}

	/** Set the scale that is to be used for Cobol Packed column types.  This scale
	  * is used in the getFloat(), getDouble(), getBigDecimal(), setFloat(), setDouble(),
	  * and setBigDecimal methods.
	  * @param columnNames A list of the column names that this scale value is to apply.
	  */

	public final synchronized void setScale( String columnNames, int scale ) throws ADBCException {

		fetchWait();

		ColumnNameParser columnParser = new ColumnNameParser( columnNames, this.columnOrder, this.numColumns, this.name, "setScale",43130 );

		Enumeration cols = columnParser.elements();

		while ( cols.hasMoreElements() ) {
			Integer colIndex = (Integer) cols.nextElement();
			int colI = colIndex.intValue();

			//................verifyOriginalValue[ colI - 1 ] = 2;
			scaleArray[ colI - 1 ] = scale;
		}
	}

	/** Set the string format of a column.  If the particular column represents a date
	  * in the database or file, use ADBCDateFormat as the format.  ADBCDateFormat will
	  * convert dates represented as numbers or strings, in a Turbo Image database or 
	  * MPE File, to java dates, using the TurboBuffer.getDate() method.  ADBCDateFormat
	  * just needs to know the internal date format.  An external date format must
	  * also be provided for the getString() method.
	  * @param column The name of the column.
	  * @param format The format to apply to the column.
	  */

	public final synchronized void setFormat( String columnNames, Format format ) throws ADBCException {

		fetchWait();

		ColumnNameParser columnParser = new ColumnNameParser( columnNames, this.columnOrder, this.numColumns, this.name, "setFormat",43140 );

		Enumeration cols = columnParser.elements();

		while ( cols.hasMoreElements() ) {
			Integer colIndex = (Integer) cols.nextElement();
			int colI = colIndex.intValue();

			//....................verifyOriginalValue[ colI - 1 ] = 2;
			columnFormat.put( columns.elementAt( colI - 1 ), format );
		}
	}

	/** Set all columns that have the specified data type to use the
	  * specified format.
	  * @param columnType A TurboImage data type U,X,R,E,I,J,K,P.
	  * @param format The desired format.
	  */

	public final synchronized void setFormatByType( String columnType, Format format ) throws ADBCException {

		for (int i=0;i<numColumns;i++) {
			TurboColumn col = (TurboColumn) columns.elementAt(i);
			if ( col.getColumnType().equals( columnType.toUpperCase() ) ) {
				columnFormat.put( col, format );
			}
		}
	}

	/** Specifies an alias name for a column.  Multiple alias's can be
	  * given to a column, however, only the last assigned alias for a given
	  * column will be returned in the TurboBuffer.getColumnAlias() command.  
	  * The column alias can be used to reference the
	  * column in the TurboBuffer getXXX/updateXXX methods.
	  * @param columnName The column name.
	  * @param aliasName The alias.
	  */

	public final synchronized void setColumnAlias( String columnName, String aliasName ) throws ADBCException {

		TurboColumn tmpCol = (TurboColumn) columnHash.get( aliasName.toLowerCase() );
		if ( tmpCol != null ) throw new ADBCException("TurboBuffer.setColumnAlias:Column Exists",this.name,columnName,43005);
		tmpCol = (TurboColumn) columnHash.get( columnName.toLowerCase() );
		if ( tmpCol == null ) throw new ADBCException("TurboBuffer.setColumnAlias:Column Alias Exists",this.name,columnName,43006);
		columnHash.put( aliasName.toLowerCase(), tmpCol );
		Integer i = (Integer) columnOrder.get( tmpCol.getName() );
		columnOrder.put( aliasName.toLowerCase(), i );
		columnAlias.setElementAt( aliasName, i.intValue() - 1 );

	}

	private void add( TurboColumn column ) {

		this.length += column.getSize();
		this.numColumns++;

		columns.addElement( column );
		columnHash.put( column.getName(), column );
		columnOrder.put( column.getName(), new Integer( this.numColumns ) );
		columnAlias.addElement( column.getName() );

	}

	/** Add a computed column to this TurboBuffer.  Computed columns can be used to 
	  * retrieve additional calculated data from a TurboBuffer as if these columns
	  * where actual database columns.  Create a computed column object by either
	  * implementing the advnetsys.adbc.ComputedColumn interface, or by extending the
	  * advnetsys.adbc.AbstractComputedColumn class.
	  * @param columnName The name of the computed column.
	  * @param computedColumn An instance of a computed Column.
	  * @see advnetsys.adbc.ComputedColumn
	  * @see advnetsys.adbc.AbstractComputedColumn
	  */

	public final synchronized void addComputedColumn( String columnName, ComputedColumn computedColumn ) throws ADBCException {

		fetchWait();

		if ( columnHash.get( columnName.toLowerCase() ) != null ) 
		     throw new ADBCException("TurboBuffer.addComputedColumn:Column Exists",this.name,columnName,43004);
		//computedColumns.addElement( computedColumn_ );
		columns.addElement( computedColumn );
		//computedHash.put( columnName_, computedColumn_ );
		columnHash.put( columnName.toLowerCase(), computedColumn );
		numComputedColumns++;
		columnOrder.put( columnName.toLowerCase(), new Integer( numComputedColumns + numColumns ) );
		columnAlias.addElement( columnName );
	}

	/** Manually set the mode of the dbget that is to be performed against the dataTable.
	  * Review the documentation on the initiateRead() methods.  The initiateRead() methods
	  * will do this for you automatically based on the parameters that are passed to it.
	  * @param readMode_ The desired dbget mode: SERIAL, BACKWARD_SERIAL, DIRECTED, CHAINED,
	  * BACKWARD_CHAINED, CALCULATED.
	  */

	private void setReadMode( int readMode_ ) {
		readMode = readMode_;
		if( readMode == CHAINED ) inverseReadMode = BACKWARD_CHAINED;
		else if( readMode == BACKWARD_CHAINED ) inverseReadMode = CHAINED;
		else if( readMode == SERIAL ) inverseReadMode = BACKWARD_SERIAL;
		else if( readMode == BACKWARD_SERIAL ) inverseReadMode = SERIAL;
		else inverseReadMode = readMode;
		fetchReadMode = readMode;
	}

	/** Initiate a read to the database in SERIAL dbget mode. 
	  * @exception ADBCException if a database or network error occurs.
	  */
	
	public final synchronized void initiateRead() throws ADBCException {
		turboKey = null;
		//if ( columns.size() == 0 ) this.setColumns( "@" );
		if ( columns.size() == 0 ) throw new ADBCException("TurboBuffer.initiateRead:No Columns have been set",this.name,43010);
		setReadMode( SERIAL );
		//System.out.println("Before startread()");
                startRead();
	}

	private synchronized void initiateReadHelper( Object keyValue_ ) throws ADBCException {

		fetchWait();

		keyValue = keyValue_;

		if ( keyColumn == null ) {
			keyColumn = rowSet.getFirstKeyColumn();
			if ( dataTable.getType() == Dataset.MASTER )
				setReadMode( CALCULATED );
			else
				setReadMode( CHAINED );

		} else if ( keyColumn == recordIdColumn ) {
			setReadMode( DIRECTED );
		} else if ( ! isMpeFile && turboKey != null ) {
			setReadMode( CHAINED );
		} else if ( ! isMpeFile && 
		              dataTable.getType() == Dataset.MASTER ) { // At this point, a CHAINED read is expected.
		                                                           // If a chained column was specified for a 
		                                                           // master dataTable, throw exception.
			if ( keyColumn != null && keyColumn == rowSet.getFirstKeyColumn() ) {
				setReadMode( CALCULATED );
			} else {
				throw new ADBCException("TurboBuffer.initiateRead:Cannot Perform CHAINED Read on a master Dataset",this.name,keyColumn.getName(),70001);
			}
		} else {
				if ( ! keyColumn.isKeyColumn() ) {
					throw new ADBCException("TurboBuffer.initiateRead:Invalid Key Column",this.name,keyColumn.getName(),43011);
				}
				setReadMode( CHAINED );
		}
		startRead();
	}

	/** Initiate a read to the database in DIRECTED mode. 
	  * @param recordId The Turbo Image dataTable record identifier.
	  * @exception ADBCException if a database or nework error occurs.
	  */

	public final synchronized void initiateRead( int recordId ) throws ADBCException {

		turboKey = null;
		if ( columns.size() == 0 ) throw new ADBCException("TurboBuffer.initiateRead:No Columns have been set",this.name,43010);
		keyColumn = recordIdColumn;
		initiateReadHelper( new Integer( recordId ) );

	}

	/** Initiate a read to the database in CHAINED dbget mode.
	  * @param keyColumn The column index number.
	  * @param keyValue An object that contains the key value used in dbfind.
	  * @exception ADBCException if a database or network error occurs.
	  */

	public final synchronized void initiateRead( int keyColumn, Object keyValue ) throws ADBCException {
		turboKey = null;
		iRead( keyColumn, keyValue );
	}

      public final void setTableInfo( boolean get_record_length ){
               // System.out.println("Im in the chain...");
                this.get_record_length = get_record_length;
                db = dataTable.getDataSourceId();
               // System.out.println("datasource id = "+db);
                in = new Intrinsic( mpe3000 );
	        in.setXL( "ADBCXL" );
	        in.setName( "speedracer" );
              	in.addParameter( "database",             "I", 1);
                in.addParameter( "setname",              "X", 16); 
                in.addParameter( "itemname",             "X", 16);
	        in.addParameter( "item_value",           "X", 18);
                in.addParameter( "number_of_records",    "I", 2);
                         
            
        }
	private final synchronized void iRead( int keyColumn, Object keyValue ) throws ADBCException {

		if ( columns.size() == 0 ) throw new ADBCException("TurboBuffer.initiateRead:No Columns have been set",this.name,43010);
		this.keyColumn = rowSet.getColumn( keyColumn );
		initiateReadHelper( keyValue );
	
	}

	/** Initiate a read to the database in CHAINED dbget mode.  If a value of
	  * "rec#' is passed as the column name, TurboBuffer assumes DIRECTED mode
	  * and also assumes that keyValue_ is an Integer object that contains
	  * the dataTable record identified.
	  * @param keyColumn The name of the key column.
	  * @param keyValue An object that contains the key value used in dbfind.
	  * @exception ADBCException if a database or network error occurs.
	  */

	/*public final synchronized void ( String keyColumn, Object keyValue ) throws ADBCException {
		turboKey = null;
		iRead( keyColumn, keyValue );
	}*/
      public final synchronized Object initiateRead( String keyColumn, Object keyValue ) throws ADBCException {
	       
        	turboKey = null;
		iRead( keyColumn, keyValue );
              
              //   Added by DT for programmers to get record length.  Not too efficiant but will work
               if ( get_record_length ) {
                       //System.out.println("Record length set..."+this.getName());
                        
                        try {
                        in.updateInt("database",db);
                        in.updateString( "setname", this.getName());
                        in.updateString( "itemname", keyColumn.toUpperCase());
                        in.updateString( "item_value", (String) keyValue);
                        in.updateInt ( "number_of_records", -1);
                                                 
                        in.call();
	                } catch (ADBCException e) {e.printStackTrace();}                       
                      //  System.out.println("number od records = " + in.getInt("number_of_records"));
                        ti.setChainCount(in.getInt("number_of_records"));
                       return (TableInfo) ti;
                }
                return null;
                
                
	}

	private final synchronized void iRead( String keyColumn, Object keyValue ) throws ADBCException {

		if ( columns.size() == 0 ) throw new ADBCException("TurboBuffer.initiateRead:No Columns have been set",this.name,43010);
		if ( "rec#".equals(keyColumn.toLowerCase()) ) 
			this.keyColumn = recordIdColumn;
		else 
			this.keyColumn = rowSet.getColumn( keyColumn.toLowerCase() );

		initiateReadHelper( keyValue );
	}

	public final synchronized void initiateRead( TurboKey key, Object keyValue ) throws ADBCException {

		this.turboKey = key;
		if ( key.isUserSpecified() ) {   // 2.1.1
			this.keyColumn = key.getUserSpecifiedKeyColumn();
			initiateReadHelper( keyValue );
		} else if ( key.getColumnName() != null ) {
			iRead( key.getColumnName(), keyValue );
		} else {
			iRead( key.getColumnNo(), keyValue );
		}
	}

	/** Initiate a read to the database in CALCULATED or CHAINED dbget mode.
	  * CALCULATED dbgets can only be performed on MASTER dataTables and therefore
	  * needs no key column specified.  If the dataTable happens to be a DETAIL
	  * dataTable, the TurboBuffer will perform a CHAINED read.  CHAINED reads do
	  * require a key column to be specified, therefore the TurboBuffer will assume
	  * that the user intended to use the first key column found in the schema.
	  * @param keyValue An object that contains the key value used in dbget or dbfind.
	  * @exception ADBCException if a database or network error occurs.
	  */

	public final synchronized void initiateRead( Object keyValue ) throws ADBCException {
		if ( columns.size() == 0 ) throw new ADBCException("TurboBuffer.initiateRead:No Columns have been set",this.name,43010);
		keyColumn = null;
		initiateReadHelper( keyValue );
	}

	/** This method must be used if the program manually sets the dbget modes, or the
	  * key columns and values that are to be used for the dbfind/dbget.  
	  * Review the documentation for the initiateRead() methods.  The 
	  * initiateRead() methods will do this for you automatically based on the parameters
	  * that are passed to it.
	  */

	private synchronized void startRead() {

                try{
                fetchWait();
                }catch (ADBCException e) { e.printStackTrace();}
		dataTableReset = 1;
		fetchReadMode = readMode;
		currRow = null;
		onInsertRow = 0;

		this.resetBuffer();
		lastRecordNumber = -1;
	}

	private synchronized void resetBuffer() {
		cBuffer = null;
		cBuffer = new CircularBuffer( fetchSize );
	}

   	private synchronized void initiateFetch( int fetchReadMode_ ) throws ADBCException {

 		fetching = true;
 		fetchReadMode = fetchReadMode_;

		//....System.out.println("Starting the fetch..................");

		mpeSocket = mpe3000.checkOutSocket();

		short sendPacketLength = 2 + 2 + 2 + 2 + 2 + 2 + 2 + 8 + 2 + 2;

		sendPacketLength += ( 2 * 2 * this.numColumns );

		if ( readMode == CHAINED || readMode == CALCULATED ||
		     readMode == DIRECTED ) {
			//.sendPacketLength += 2 + 2 + keyColumn.getSize();  //2.1.1

			/* 2.1.1 now sending column name in chained read.  Must add 16 to packetlength */

			sendPacketLength += 2 + 2 + keyColumn.getSize() + 16;
		}

		try {

			byte[] bOut = new byte[sendPacketLength + 2];

			ByteArray.setShort ( bOut, 0, sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "RE" );
			ByteArray.setShort ( bOut, 4, (short) fetchReadMode_ );
			ByteArray.setShort ( bOut, 6, dataTable.getDataSourceId() );
			ByteArray.setShort ( bOut, 8, dataTable.getIdentifier() );
	
			int lastRec = -1;
			if ( dataTableReset == 2 ) {

				if ( isMpeFile ) {  //2.0.1
					if ( ! isKsamFile && ! rowSet.isVariableLength() ) lastRec = lastRecordNumber;   //2.0.1
				} else if ( readMode == SERIAL ) {
					//...if ( ! ( isMpeFile && ! isKsamFile ) ) dataTableReset = 0; //C2.0.1
					lastRec = lastRecordNumber;
                              dataTableReset = 0;
				} else if ( readMode == CHAINED ) {
					if ( this.nextRecNo == 0 && this.prevRecNo == 0 ) dataTableReset = 1;
					else if ( fetchReadMode == BACKWARD_CHAINED ) {
						if ( this.prevRecNo == 0 ) {
							dataTableReset = 0;
							lastRec = this.nextRecNo;
						} else {
							dataTableReset = 2;
							lastRec = this.prevRecNo;
						}
					} else {  // implied CHAINED.
						if ( this.nextRecNo == 0 ) {
							dataTableReset = 0;
							lastRec = this.prevRecNo;
						} else {
							dataTableReset = 2;
							lastRec = this.nextRecNo;
						}
					}
				}

			} else if ( dataTableReset == 0 ) {

				if ( currRow != null ) {
					int currNo = this.getRecordNumber();
					//if ( currNo != lastRecordNumber ) lastRec = currNo; //2.0.1
					//2.0.1 MpeFile must always send lastRec <> -1.
					if ( currNo != lastRecordNumber || 
					     ( isMpeFile && ! isKsamFile && ! rowSet.isVariableLength() ) ) {
					     	lastRec = currNo; //2.0.1
					}
					else 
						if ( ! isMpeFile && this.registryId != ((Dataset) dataTable).getLastDatasetUser() )
							lastRec = currNo;

				} else 

					/* If the first record(backPtr) or last record(frontPtr) of the CircularBuffer
					   was deleted, currRow will be null so this will get the record number
					   of the first or last record in the CircularBuffer.  This will only happen
					   when datasetReset == 0.  If there are no records in the buffer, 
					   due to record deletions, datasetReset would be equal to 2.
					   Also, this initiateFetch with dataTableReset == 0 would only have ocurred 
					   if the current pointer in the CircularBuffer was on a perimieter record.
					 */

					lastRec = cBuffer.getPerimeterRecordNumber();
			}

			//...if ( currRow != null && getRecordNumber() != lastRecordNumber ) lastRec = getRecordNumber();

			//System.out.print(" dataRes=" + dataTableReset + " lastRec=" + lastRec + " " );
			ByteArray.setInt   ( bOut, 10, lastRec );

			//..ByteArray.setShort ( bOut, 14, (short) fetchSize );
			ByteArray.setInt ( bOut, 14, fetchSize );
			ByteArray.setShort ( bOut, 18, (short) dataTableReset ); //Reset Dataset=1, NoReset=0

			//if ( dataTableReset == 1 ) dataTableReset = 0; //2.0.1
			dataTableReset = 0;  //2.0.1

			ByteArray.setShort ( bOut, 20, (short) dataTable.getType() );

			int recSize = 0; //2.0.1
			if ( isMpeFile ) recSize = ((MpeFile)dataTable).getRecSize(); //2.0.1

			ByteArray.setShort ( bOut, 22, (short) recSize );  //optiona 2.0.1
			//.ByteArray.setShort ( bOut, 22, (short) rowSet.getLength() );  //optiona 2.0.1
			int keyMode = 0;
			if ( ! isMpeFile ) keyMode = 1;  //2.1 this sets the dbfind mode - 1 is default.
			if ( turboKey != null ) keyMode = turboKey.getKeyMode();
			//ByteArray.setShort ( bOut, 24, (short) 0 );  //optionb //2.0.1
			ByteArray.setShort ( bOut, 24, (short) keyMode );  //optionb //2.0.1

			ByteArray.setShort ( bOut, 26, this.numColumns );

			int offset = 28;
			
			for (int i=0; i<numColumns; i++) {
				TurboColumn colTemp = (TurboColumn) columns.elementAt( i );
				ByteArray.setShort ( bOut, offset, colTemp.getOffset() );
				ByteArray.setShort ( bOut, offset + 2, colTemp.getSize() );
				offset += 4;
			}
			if ( readMode == CHAINED || readMode == CALCULATED || 
			     readMode == DIRECTED ) {
				
				/* If the dataTable represents a ksam file, pass the byte offset
				 * of the key column, else pass the column id to be used for dbfind.
				 */

				short columnId = 0;
				int keyColumnLength = keyColumn.getSize();      //2.0.1
				//if ( isMpeFile ) columnId = keyColumn.getOffset();  //2.0.1
				if ( isMpeFile ) {
					columnId = keyColumn.getOffset();
					if ( turboKey != null ) keyColumnLength = turboKey.getKeyLength(); //2.0.1
					if ( keyColumnLength == 0 ) keyColumnLength = keyColumn.getSize(); //2.0.1
				}
				else columnId = keyColumn.getColumnId();

				ByteArray.setShort ( bOut, offset, columnId );
				//ByteArray.setShort ( bOut, offset + 2, keyColumn.getSize() ); //2.0.1
				ByteArray.setShort ( bOut, offset + 2, (short) keyColumnLength );  //2.0.1

				/* 2.1.1 Pass keycolumn name */

				ByteArray.setString( bOut, offset + 4, 16, keyColumn.getName().toUpperCase() );

				//..turboKey = null;  //2.0.1 //2.1B3

				//================================================================================================
				//=============================== Revisit Later ================================================== 
				//================================================================================================
				//Also check for scale.  The key column need not be in the turboBuffers set of columns.
				//Currently passing zero as scale.
				try {
					//.keyColumn.setObject( bOut, offset + 4, 0, keyValue );  // 2.1.1
					//keyColumn.setObject( bOut, offset + 20, 0, keyValue );  // 2.1.1  &  2.1.2
					if ( keyValue instanceof byte[] ) {  // 2.1.2
						keyColumn.setByteArray( bOut, offset + 20, (byte[]) keyValue );
					} else {  //2.1.2
						keyColumn.setObject( bOut, offset + 20, 0, keyValue );  // 2.1.1  &  2.1.2
					}
				} catch (ADBCException e) { throw new ADBCRuntimeException("TurboBuffer.initiateRead:Key Operation Failed",this.name,keyColumn.getName(),70000);}
			}

			mpeSocket.out.write( bOut );

		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"TurboBuffer.initiateRead:Communication Error",this.name,40000);}

		//....turboBufferThread = new TurboBufferThread();
		turboBufferThread.initiateRetrieve( this, mpeSocket );
		//this.fetchThread = null;
		//fetchThread = new Thread( turboBufferThread);
		//fetchThread.setPriority( Thread.MIN_PRIORITY );
		//fetchThread.start();
		//....turboBufferThread.start();

		//=========================================================================================
		//discontinue next section.  to replace code, retrieve from ZTurboBuffer.java.old15

	}

	synchronized void addBufferRecord( byte[] b ) {

		lastRecordNumber = ByteArray.getInt( b, 2 );

		cBuffer.addElement( b );
		//tracker.add("buffer record added rec=" + lastRecordNumber);
		this.notify();
		//notifyAll();
		//System.out.println("Element Added = " + lastRecordNumber );
	}

	synchronized void setFetchComplete( ADBCException threadException ) {
		this.threadException = threadException;
		fetching = false;
		if ( ! isMpeFile ) {
			//.((Dataset)dataTable).setLastPtr( lastRecordNumber );
			((Dataset)dataTable).setLastDatasetUser( this.registryId );
		}
		mpe3000.checkInSocket();
		this.notify();
	}

	public void close() {
		//for (int i=0;i<tracker.size();i++)
		//	System.out.println(tracker.get(i));
	}

	/** Advances the cursor to the next record in the TurboBuffer.
	  * @return true if the next record exists, false if the end of the TurboBuffer is encountered.
	  * @exception ADBCException if there is a database or network error.
	  */

	public final synchronized boolean next() throws ADBCException {

                //System.out.println("TRACE DEBUG - " + this.mpe3000.isConnected());

                if ( !this.mpe3000.isConnected() ) {
                      throw new ADBCException("Mpe3000:No Connection to Server",40200);
                }

		if ( onInsertRow > 0 ) {
		
			if ( onInsertRow == 1 ) {  // on the insert row.  Mode 1 means not yet updated.
				throw new ADBCException("TurboBuffer.next:Cursor is on insertRow",this.name,60000);
			} else { // onInsertRow == 2
				while( fetching ) 
					try {wait();} catch (InterruptedException e) {e.printStackTrace();}
				this.resetBuffer();
				cBuffer.addElement( currRow.getOriginalBytes() );

				/* Advance the current pointer so that the inserted record will not be
				   read as the next record.  The record following the inserted record
				   will be read in as the next record. */

				cBuffer.isNextOK();  
				onInsertRow = 0;  // reset the insertRow.
				//.initiateFetch( readMode );
			}
		}

		//while ( fetching && 
		//	  ( fetchReadMode == inverseReadMode || cBuffer.currentOnFront() ) ) {
		while ( fetching && fetchReadMode == inverseReadMode ) {
			try {wait();} catch (InterruptedException e) {e.printStackTrace();}
		}

		//....byte[] b = (byte[]) cBuffer.nextObject();

		//....while ( fetching && b == null ) {
		boolean recordAvailable = cBuffer.isNextOK();

		while ( fetching && ! recordAvailable ) {
			try {wait();} catch (InterruptedException e) {e.printStackTrace();}
			recordAvailable = cBuffer.isNextOK();
			//....b = (byte[]) cBuffer.nextObject();
		}


		//if ( !fetching && ( cBuffer.currentOnFront() ) ) {
		//....if ( !fetching && b == null ) {
		if ( !fetching && ! recordAvailable ) {
			cBuffer.setLoadDirection( CircularBuffer.FORWARD_LOAD );
			initiateFetch( readMode );
			//tracker.add("fetch init");
			//System.out.println("TurboBuffer fetch was initiated...");
		}

		//while ( fetching && ( cBuffer.currentOnFront() ) ){
		//System.out.println("waiting to enter wait loop...");
		//....while ( fetching && b == null ){
		while ( fetching && ! recordAvailable ){
			try {wait();} catch(InterruptedException e) {e.printStackTrace();}
			//System.out.println("turbobuffer.next()...");
			//tracker.add("fetch loop");
			//....b = (byte[]) cBuffer.nextObject();
			recordAvailable = cBuffer.isNextOK();
			//tracker.add("fetch loop b recordAvailable=" + recordAvailable );
			//if ( b == null ) System.out.println("turbobuffer.next() b is null...");
		}

		//if ( ! ( cBuffer.currentOnFront() ) ) {
		//....if ( b != null ) {
		//tracker.add("recordAvailable=" + recordAvailable);

		checkThreadException();
    
     /// DT PERCobol
                readstatus = recordAvailable; 
                
		if ( recordAvailable ) {
			//currRow = new Row( (byte[]) cBuffer.nextObject() );
			currRow = new Row( (byte[]) cBuffer.getObject() );
			//tracker.add("next complete true rec="+this.getRecordNumber());
			if ( rowListeners != null ) fireRowCurrentChanged( this.getRecordNumber() );
		} else {
			currRow = null;
                        // D A V E  this might be the place to pass back c-word 17
			//tracker.add("next complete false");
			return false;
		}
		
		if ( readMode == CALCULATED || readMode == DIRECTED ) {
			setReadMode( SERIAL );
		}

		return true;
	}

	/** Backs the cursor up to the previous record in the TurboBuffer.
	  * @return true if a previous record exists, false if at the beginning of the TurboBuffer.
	  * @exception ADBCException if there is a database or network error.
	  */

	public final synchronized boolean previous() throws ADBCException {

		if ( onInsertRow > 0 ) {
		
			if ( onInsertRow == 1 ) {
				throw new ADBCException("TurboBuffer.previous:Cursor is on insertRow",this.name,60001);
			} else { // onInsertRow == 2
				while( fetching ) 
					try {wait();} catch (InterruptedException e) {e.printStackTrace();}
				this.resetBuffer();
				cBuffer.addElement( currRow.getOriginalBytes() );
				cBuffer.isPrevOK();
				onInsertRow = 0;
				//.initiateFetch( inverseReadMode );
			}
		}

		//while ( fetching && ( fetchReadMode == readMode || 
		//					 cBuffer.currentOnBack() ) ) {
		while ( fetching && fetchReadMode == readMode ) {
			try {wait();} catch (InterruptedException e) {e.printStackTrace();}
		}

		//....byte[] b = (byte[]) cBuffer.previousObject();
		boolean recordAvailable = cBuffer.isPrevOK();

		while ( fetching && ! recordAvailable ) {
			try {wait();} catch (InterruptedException e) {e.printStackTrace();}
			recordAvailable = cBuffer.isPrevOK();
			//....b = (byte[]) cBuffer.nextObject();
		}

		//if ( !fetching && ( cBuffer.currentOnBack() ) ) {
		//....if ( !fetching && b == null ) {
		if ( !fetching && !recordAvailable ) {
			cBuffer.setLoadDirection( CircularBuffer.BACKWARD_LOAD );
			initiateFetch( inverseReadMode );
		}

		//while ( fetching && ( cBuffer.currentOnBack() ) ){
		//while ( fetching && b == null ){
		while ( fetching && !recordAvailable ){
			try {wait();} catch (InterruptedException e) {e.printStackTrace();}
			recordAvailable = cBuffer.isPrevOK();
			//....b = (byte[]) cBuffer.previousObject();
		}

		//if ( ! ( cBuffer.currentOnBack() ) ) {
		//if ( b != null ) {

		checkThreadException();

		if ( recordAvailable ) {
			//currRow = new Row( (byte[]) cBuffer.previousObject() );
			//....currRow = new Row( b );
			currRow = new Row( (byte[]) cBuffer.getObject() );
			if ( rowListeners != null ) fireRowCurrentChanged( this.getRecordNumber() );
		} else {
			currRow = null;
			return false;		
		}
		
		if ( readMode == CALCULATED || readMode == DIRECTED ) {
			setReadMode( SERIAL );
		}

		return true;
	}

	/** Copy the values of the current row into an internal buffer.
	  */
	
	public final void copyRow() {

		if ( currRow != null ) {
			copyBuffer = (byte[]) currRow.getBytes().clone();
		}
	}

	/** Paste values in the copy buffer to the current row.
	  */

	public final void pasteRow() throws ADBCException {

		if ( currRow != null && copyBuffer != null ) {
			byte[] b = currRow.getBytes();
			for (int i=1;i<=this.numColumns;i++) {
				TurboColumn col = (TurboColumn) columns.elementAt( i-1 );
				if ( col.compare( b, copyBuffer, offsetArray[i-1] ) == 0 ) {
					currRow.setModified( i , this.numColumns );
					if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), col.getName(), i, col.getObject( copyBuffer, offsetArray[i-1], scaleArray[i-1] ) );
				}
			}
			System.arraycopy( copyBuffer, 6, b, 6, b.length - 6 );
		}
	}

	/** Creates an empty row and points the cursor to this row.  The previous
	  * cursor position is remembered and can be returned to by using the
	  * moveToCurrentRow method.
	  */

	public final void moveToInsertRow() {

		if ( onInsertRow == 0 ) {
			currRowBackup = currRow;
			//if ( insertRow == null ) 
			     //...insertRow = new Object[this.numColumns + 1];
			     //insertRow = new Row( this.numColumns );
		}
		insertRow = new Row( new byte[this.length + 6] );
		currRow = insertRow;
		onInsertRow = 1;
	}

	/** Copy the values of the current row to the insert row and then sets 
	  * the cursor to the insert row.  Note that
	  * the cursor may already on the insert row.  In this case the
	  * values of the insert row will be copied to a new insert row.
	  */
	
	//public final void copyToInsertRow() {

	//	if ( onInsertRow == 0 ) {
	//		currRowBackup = currRow;
	//	}
	//	byte[] b = (byte[]) currRow.getBytes().clone();
	//	ByteArray.setInt( b, 2, 0 );     // Set the record number to 0.
	//	insertRow = new Row( b );
	//	currRow = insertRow;
	//	onInsertRow = 1;
	//}

	/** Use this method to move from the insert row back to the previous
	  * current row.
	  */

	public final void moveToCurrentRow() {

		if ( onInsertRow > 0 ) {
			currRow = currRowBackup;
			onInsertRow = 0;
			insertRow = null;
	
			if ( this.isKsamFile ) {

				this.resetBuffer();
				cBuffer.addElement( currRow.getOriginalBytes() );

				/* Advance the current pointer so that the inserted record will not be
				   read as the next record.  The record following the inserted record
				   will be read in as the next record. */

				cBuffer.isNextOK();  
			}
		}
	}
/// DT PERCobol

        public boolean getReadStatus() {
                
                return this.readstatus;
                
                }
                
        public int getReadMode() {
                
                return readMode;
               }
               
// DT PERCobol
	private Object getColumn( int columnNo ) throws ADBCException {

		if ( columnNo <= 0 || columnNo > this.numColumns  + this.numComputedColumns ) 
			throw new ADBCException("TurboBuffer.getXXX/updateXXX:Invalid columnNo",this.name,String.valueOf(columnNo),43003);
		return columns.elementAt( columnNo - 1 );
	
	}
      public int getType() {

           return dataTable.getType();

      }

	private Object getColumn( String columnName ) throws ADBCException {

		//Object tmp = (TurboColumn) columnHash.get( columnName.toLowerCase() );  //2.0.1 Bug
		Object tmp = columnHash.get( columnName.toLowerCase() );
		if ( tmp == null ) throw new ADBCException ("TurboBuffer.getXXX/updateXXX:Invalid columnName",this.name,columnName,43002);
		return tmp;
	}

	/** Finds the TurboBuffer column number for a given column name.  
	  * @param columnName The name of the column.
	  * @return The TurboBuffer column number.
	  */

	public final int findColumn( String columnName ) {
		int i=-1;
		Integer obj = (Integer) columnOrder.get( columnName.toLowerCase() );
		if ( obj != null ) i = obj.intValue();
		return i;
	}

	/** Finds the TurboBuffer column number for a given column name and throws
	  * an advnetsys.adbc.ADBCException if not found.
	  * @param columnName The name of the column.
	  */

	public final int findColumnX( String columnName ) throws ADBCException {
		int i=-1;
		Integer obj = (Integer) columnOrder.get( columnName.toLowerCase() );
		if ( obj != null ) i = obj.intValue();
		if ( i == -1 ) throw new ADBCException("TurboBuffer.getXXX/updateXXX:Invalid columnName",this.name,columnName,43002);
		return i;
	}

	/** Determines if the column is a computed column.
	  * @param The column name.
	  * @return true if the specified column is a computed column.
	  */

	public final boolean isComputedColumn( String columnName ) {
		return ( isComputedColumn( findColumn( columnName ) ) );
	}

	/** Determines if the column is a computed column.
	  * @param The column number.
	  * @return true if the specified column is a computed column.
	  */

	public final boolean isComputedColumn( int columnNo ) {
		return ( columnNo > numColumns && columnNo <= numColumns + numComputedColumns );
	}
		
	/** Return the column value as an Object.
	  * @param columnNo The column number.
	  */

	public final Object getObject( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getObject( this );
		return ((TurboColumn)obj).getObject( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getObject( currRow.getBytes(), offsetArray[columnNo_ -1] );
	}

	/** Return the column value as an Object.
	  * @param columnName The column name.
	  */

	public final Object getObject( String columnName ) throws ADBCException {
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getObject( this );
		return ((TurboColumn)obj).getObject( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getObject( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a String.
	  * @param columnNo The column number.
	  */
	
	public final String getString( int columnNo ) throws ADBCException{
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getString( this, (Format) columnFormat.get( obj ) );
		//if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getString( this, (Format) columnFormat.get( getColumn( columnNo ) ) );
		return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], (Format) columnFormat.get( obj ) );
		//return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[columnNo - 1], (Format) columnFormat.get( getColumn( columnNo ) ) );
		//return getColumn( columnNo_ ).getString( currRow.getBytes(), offsetArray[columnNo_ - 1], (Format) columnFormat.get( getColumn( columnNo_) ) );
	}

	/** Return the column value as a String.
	  * @param columnName The column name.
	  */

	public final String getString( String columnName ) throws ADBCException{
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getString( this, (Format) columnFormat.get( obj ) );
		//if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getString( this, (Format) columnFormat.get( getColumn( columnName ) ) );
		return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1], (Format) columnFormat.get( obj ) );
		//return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], (Format) columnFormat.get( getColumn( columnName ) ) );
		//return getColumn( columnName_ ).getString( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1], (Format) columnFormat.get( getColumn( columnName_ ) ) );
	}

	/** Return the column value as a String with the specified format applied.
	  * @param columnNo The column number.
	  * @param format a format object.
	  */

	public final String getString( int columnNo, Format format ) throws ADBCException{
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getString( this, format );
		return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], format );
		//return getColumn( columnNo ).getString( currRow.getBytes(), offsetArray[columnNo - 1], format );
	}

	/** Return the column value as a String with the specified format applied.
	  * @param columnName The column name.
	  * @param format A format object.
	  */

	public final String getString( String columnName, Format format ) throws ADBCException{
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getString( this, format );
		return ((TurboColumn)obj).getString( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1], format );
		//return getColumn( columnName_ ).getString( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1], format );

	}

	/** Return the column value as a Date object.  An ADBCDateFormat object
	  * must be applied to this column using the setFormat() method.
	  * @param columnNo The column number.
	  * @see ADBCDateFormat
	  * @see #setFormat
	  */

	public final Date getDate( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		//if ( isComputedColumn( columnNo ) ) return (float)((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getDate( currRow.getBytes(), offsetArray[columnNo - 1], (ADBCDateFormat) columnFormat.get( obj ) );
		//return getColumn( columnNo_ ).getFloat( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a Date object.  An ADBCDateFormat object
	  * must be applied to this column using the setFormat() method.
	  * @param columnName The column name.
	  * @see ADBCDateFormat
	  * @see #setFormat
	  */

	public final Date getDate( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		//if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getDate( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], (ADBCDateFormat) columnFormat.get( obj ) );
		//return getColumn( columnName_ ).getFloat( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as an int.
	  * @param columnNo The column number.
	  */

	public final int getInt( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return (int) ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getInt( currRow.getBytes(), offsetArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getInt( currRow.getBytes(), offsetArray[columnNo_ - 1] );

	}

	/** Return the column value as an int.
	  * @param columnName The column name.
	  */

	public final int getInt( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return (int) ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getInt( currRow.getBytes(), offsetArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getInt( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a long.
	  * @param columnNo The column number.
	  */

	public final long getLong( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getLong( currRow.getBytes(), offsetArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getInt( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a long.
	  * @param columnName The column name.
	  */

	public final long getLong( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getLong( currRow.getBytes(), offsetArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getLong( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a short.
	  * @param columnNo The column number.
	  */

	public final short getShort( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return (short) ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getShort( currRow.getBytes(), offsetArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getShort( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a short.
	  * @param columnName The column name.
	  */

	public final short getShort( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return (short) ((ComputedColumn)obj).getLong( this );
		return ((TurboColumn)obj).getShort( currRow.getBytes(), offsetArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getShort( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a double.
	  * @param columnNo The column number.
	  */

	public final double getDouble( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getDouble( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getDouble( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a double.
	  * @param The column name.
	  */

	public final double getDouble( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getDouble( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getDouble( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a float.
	  * @param columnNo The column number.
	  */

	public final float getFloat( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return (float)((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getFloat( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getFloat( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a float.
	  * @param columnName The column name.
	  */

	public final float getFloat( String columnName ) throws ADBCException { 
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return (float)((ComputedColumn)obj).getDouble( this );
		return ((TurboColumn)obj).getFloat( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getFloat( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the column value as a BigDecimal.
	  * @param columnName The column name.
	  */

	public final BigDecimal getBigDecimal( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getBigDecimal( this );
		return ((TurboColumn)obj).getBigDecimal( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1] );
	}

	/** Return the column value as a BigDecimal.
	  * @param columnName The column name.
	  */

	public final BigDecimal getBigDecimal( String columnName ) throws ADBCException {
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getBigDecimal( this );
		return ((TurboColumn)obj).getBigDecimal( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scaleArray[findColumn(columnName) - 1] );
	}

	/** Return the column value as a BigDecimal applying the specified scale.
	  * @param columnName The column name.
	  * @param scale The scale of the BigDecimal.
	  */

	public final BigDecimal getBigDecimal( int columnNo, int scale ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getBigDecimal( this );
		return ((TurboColumn)obj).getBigDecimal( currRow.getBytes(), offsetArray[columnNo - 1], scale );
	}

	/** Return the column value as a BigDecimal applying the specified scale.
	  * @param columnName The column name.
	  * @param scale The scale of the BigDecimal.
	  */

	public final BigDecimal getBigDecimal( String columnName, int scale ) throws ADBCException {
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getBigDecimal( this );
		return ((TurboColumn)obj).getBigDecimal( currRow.getBytes(), offsetArray[findColumn(columnName) - 1], scale );
	}

	/** Return the column value as a byte array.  This byte array is in the format
	  * represented on the HP3000.  Meaning, if the column contains a packed type,
	  * this byte array will contain the packed representation of the number.
	  * @param columnNo The column number.
	  */

	public final byte[] getBytes( int columnNo ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( isComputedColumn( columnNo ) ) return ((ComputedColumn)obj).getBytes( this );
		return ((TurboColumn)obj).getByteArray( currRow.getBytes(), offsetArray[columnNo - 1] );
		//return getColumn( columnNo_ ).getByteArray( currRow.getBytes(), offsetArray[columnNo_ - 1] );
	}

	/** Return the column value as a byte array.  This byte array is in the format
	  * represented on the HP3000.  Meaning, if the column contains a packed type,
	  * this byte array will contain the packed representation of the number.
	  * @param columnName The column name.
	  */

	public final byte[] getBytes( String columnName ) throws ADBCException {
		Object obj = getColumn( columnName );
		if ( isComputedColumn( columnName ) ) return ((ComputedColumn)obj).getBytes( this );
		return ((TurboColumn)obj).getByteArray( currRow.getBytes(), offsetArray[findColumn(columnName) - 1] );
		//return getColumn( columnName_ ).getByteArray( currRow.getBytes(), offsetArray[findColumn(columnName_) - 1] );
	}

	/** Return the entire row as a byte array.
	  */

	public final byte[] getBytes() throws ADBCException {
		byte[] b = currRow.getBytes();
		byte[] b2 = new byte[b.length - 6];
		System.arraycopy( b, 6, b2, 0, b.length - 6 );
		return b2;
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateObject( int columnNo, Object value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setObject( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setObject( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateObject( String columnName, Object value ) throws ADBCException {
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setObject( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setObject( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, value );
	}
	
	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateString( int columnNo, String value ) throws ADBCException{
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setString( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setString( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value, (Format) columnFormat.get( obj ) ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateString( String columnName, String value ) throws ADBCException{
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setString( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setString( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value, (Format) columnFormat.get( obj ) );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateDate( int columnNo, Date value ) throws ADBCException{
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDate( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setDate( currRow.getBytes(), offsetArray[columnNo - 1], value, (ADBCDateFormat) columnFormat.get( obj ) ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateDate( String columnName, Date value ) throws ADBCException{
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDate( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setDate( currRow.getBytes(), offsetArray[columnNo - 1], value, (ADBCDateFormat) columnFormat.get( obj ) );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateInt( int columnNo, int value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Integer( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, (long) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setInt( currRow.getBytes(), offsetArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Integer( value ) );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateInt( String columnName, int value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, new Integer( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, (long) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setInt( currRow.getBytes(), offsetArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, new Integer( value ) );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateLong( int columnNo, long value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Long( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setLong( currRow.getBytes(), offsetArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Long( value ) );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateLong( String columnName, long value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, new Long( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setLong( currRow.getBytes(), offsetArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, new Long( value ) );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateShort( int columnNo, short value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Short( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, (short) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setShort( currRow.getBytes(), offsetArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Short( value ) );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateShort( String columnName, short value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, new Short( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setLong( this, (short) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setShort( currRow.getBytes(), offsetArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, new Short ( value ) );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateDouble( int columnNo, double value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Double( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDouble( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setDouble( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Double( value ) );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateDouble( String columnName, double value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, new Double( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDouble( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setDouble( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, new Double( value ) );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateFloat( int columnNo, float value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Float( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDouble( this, (double) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setFloat( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, new Float( value ) );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateFloat( String columnName, float value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, new Float( value ) );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setDouble( this, (double) value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setFloat( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, new Float( value ) );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateBytes( int columnNo, byte[] value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setObject( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setByteArray( currRow.getBytes(), offsetArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateBytes( String columnName, byte[] value ) throws ADBCException { 
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setObject( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setByteArray( currRow.getBytes(), offsetArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnNo The column number.
	  * @param value The data value.
	  */

	public final void updateBigDecimal( int columnNo, BigDecimal value ) throws ADBCException {
		Object obj = getColumn( columnNo );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setBigDecimal( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setBigDecimal( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value ); 
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), this.getColumnName( columnNo ), columnNo, value );
	}

	/** Update the column with the data value.
	  * @param columnName The column name.
	  * @param value The data value.
	  */

	public final void updateBigDecimal( String columnName, BigDecimal value ) throws ADBCException {
		int columnNo = findColumnX( columnName );
		Object obj = columns.elementAt( columnNo - 1 );
		if ( columnListeners != null ) fireColumnPreUpdate( this.getRecordNumber(), columnName, columnNo, value );
		if ( isComputedColumn( columnNo ) ) ((ComputedColumn)obj).setBigDecimal( this, value );
		else {
			currRow.setModified( columnNo, this.numColumns );
			((TurboColumn)obj).setBigDecimal( currRow.getBytes(), offsetArray[columnNo - 1], scaleArray[columnNo - 1], value );
		}
		if ( columnListeners != null ) fireColumnUpdated( this.getRecordNumber(), columnName, columnNo, value );
	}

	/** Cancel the updates that have been made to the current row. 
	  */

	public final synchronized void cancelRowUpdates() {

		currRow.resetToOriginal();

	}

	/** Update the current row of the TurboBuffer.  If row listeners have been added to
	  * this TurboBuffer, the RowListener.rowPreUpdate() method is called, for each row listener, prior to the 
	  * update being performed.  This gives the programmer the option to validate 
	  * the columns of the TurboBuffer.  The row event can be set to fail if the row
	  * does not pass validation.  If the preupdate and update phases are successful, 
	  * updateRow() will call all the row listener rowUpdated() methods.
	  * @throws ADBCException if a database/file error occurs.
	  * @see advnetsys.adbc.TurboBuffer#setVerifyOn
	  * @see advnetsys.adbc.TurboBuffer#setUniqueKey
	  * @see advnetsys.adbc.TurboBuffer#setAutoLock
	  * @see advnetsys.adbc.RowListener
	  */

	public final synchronized void updateRow() throws ADBCException, 
	                                           ValidationException, IntegrityException {

		/* Items passed to the server:
		 * 01    01-02 "UP" - Tells server that we are updating the current record.
		 * 02    03-04 The database number.
		 * 03    05-06 The dataTable number.
		 * 04    07-08 0=autolock disabled, >0 lockmode for autolock.
		 * 05    09-10 Length of the entire dataTable.
		 * 06    11-12 1 = do dbupdate, 0 = do dbdelete/dbput.  
		 * 07-08 13-16 Record Number of column to be updated.
		 * 09    17-18 Number of columns to update = N.
		 *             Following is the record offsets and length
		 *             i=1..N
		 * (i-1)*2+9   Record offset for column.
		 * (i-1)*2+10  Length of the column.
		 * (i-1)*2+11  Verify the column with columns data prior to change(setObject()).
		 *
		 * N*3+9       Start of the column data.
		 *             Note: If the verify flag is set for this column, then the
		 *             new value for the column will be followed by the original 
		 *             value for the column.
		 */

		short numModifiedColumns=0;
		short numUniqueColumns=0;
		short doDbUpdateFlag=1;    /* this flag will determine if a dbupdate or dbdelete/dbput will be performed. */

		if ( currRow.getNumModifiedColumns() <= 0 ) return;

		this.fireRowPreUpdate( this.getRecordNumber() );

		int[] modifiedColumns = new int[ currRow.getNumModifiedColumns() ];

		short sendPacketLength = 18;   // Base length.

		for (int i=1;i<=numColumns;i++) 

			/* Determine if this column is set as unique(verifyOriginalValue[i-1] == 2).  If
			   so, add in the column's length.  Then check if the column has been modified.
			   If it has been modified, add the length in again. */

			if ( verifyOriginalValue[i-1] == 2 ) {

				numUniqueColumns++;
				short columnSize = ((TurboColumn) columns.elementAt( i - 1 )).getSize();   /* get the column length from column Vector. */
				sendPacketLength += columnSize + 6;
				if ( currRow.isColumnModified( i ) ) sendPacketLength += columnSize;

			} else if ( currRow.isColumnModified( i ) ){
				//k++;
				numModifiedColumns++;
				modifiedColumns[ numModifiedColumns - 1 ] = i;   
				short columnSize = ((TurboColumn) columns.elementAt( i - 1 )).getSize();   /* get the column length from column Vector. */
				sendPacketLength += columnSize + 6;

				/* If the previous value must be verified, include the size of the column again,
				 * to compensate for the original Value.
				 */

				if ( verifyOriginalValue[i - 1] > 0 )
					sendPacketLength += columnSize;
				
				/* Check if this is a key field, determine if a dbupdate can be performed, 
				 * or if a dbdelete/dbput is in order.
				 */
				
				if ( ! isMpeFile && (! ((Dataset)dataTable).isCriticalItemUpdateEnabled() & doDbUpdateFlag == 1 )) /* if doDbUpdate is already zero, don't bother.*/ 
					if ( ((TurboColumn) columns.elementAt( i - 1 )).isKeyColumn() ) doDbUpdateFlag = 0;
			}

		fetchWait();
		checkThreadException();

		mpeSocket = mpe3000.checkOutSocket();

		try {

			byte[] bOut = new byte[sendPacketLength + 2];

			ByteArray.setShort ( bOut, 0, sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "UP" );
			ByteArray.setShort ( bOut, 4, dataTable.getDataSourceId() );
			ByteArray.setShort ( bOut, 6, dataTable.getIdentifier() );
			ByteArray.setShort ( bOut, 8, ( autoLockEnabled ? (short) dataTable.getDefaultLockMode() : (short) 0 ) );
			ByteArray.setShort ( bOut, 10, (short) rowSet.getLength() );
			ByteArray.setShort ( bOut, 12, doDbUpdateFlag );
			ByteArray.setInt   ( bOut, 14, this.getRecordNumber() );
			ByteArray.setShort ( bOut, 18, (short)(numUniqueColumns + numModifiedColumns) );

			/* Write out the offset and length of each column. */
			/* First write out the values for the unique columns. The unique
			   columns are to be evaluated first, therefore they are first in the list. */
			
			int offset = 20;

			for (int i=1;i<=numColumns;i++) {

				if ( verifyOriginalValue[i-1] == 2 ) {
					TurboColumn tmp = (TurboColumn) columns.elementAt( i - 1 );
					ByteArray.setShort( bOut, offset, tmp.getOffset() );
					ByteArray.setShort( bOut, offset + 2, tmp.getSize() );

					
					/* If row is not modifed, pass 2, else pass 3. 2 tells the server
					   that this is a unique column only, only one data value will be
					   passed.  3 tells the server that the new data value and the old
					   data value will be passed. */

					ByteArray.setShort( bOut, offset + 4, (short)( currRow.isColumnModified(i) ? 3 : 2 ) );

					offset += 6;
				}
			}

			/* Now write out the offset and length values for the modifed columns */

			for (int i=1;i<=numModifiedColumns;i++) {
				
				/* In next statement, TurboColumns are stored in Vector columns 0..numColumns-1.
				   where data is store in Row class 1..numColumns.  Element 0 is recordId. 
				   therefore must subtract 1 in next statement to get the TurboColumn. */

				TurboColumn tmp = (TurboColumn) columns.elementAt( modifiedColumns[ i-1 ] - 1 );
				ByteArray.setShort( bOut, offset, tmp.getOffset() );
				ByteArray.setShort( bOut, offset + 2, tmp.getSize() );
				ByteArray.setShort( bOut, offset + 4, verifyOriginalValue[ modifiedColumns[i-1] - 1 ] );

				offset += 6;

				/* Determine if this column needs to compare the original value. */

			}

			/* Write out the data. */

			for (int i=1;i<=numColumns;i++) {

				if ( verifyOriginalValue[i-1] == 2 ) {
					TurboColumn tmp = (TurboColumn) columns.elementAt( i - 1 );
					System.arraycopy( currRow.getBytes(), offsetArray[i - 1], bOut, offset, tmp.getSize() );
					offset += tmp.getSize();
					if ( currRow.isColumnModified( i ) ) {
						System.arraycopy( currRow.getOriginalBytes(), offsetArray[i - 1], bOut, offset, tmp.getSize() );
						offset += tmp.getSize();
					}
				}
			}

			for (int i=1;i<=numModifiedColumns;i++) {

				TurboColumn tmp = (TurboColumn) columns.elementAt( modifiedColumns[ i-1 ] - 1 );
				System.arraycopy( currRow.getBytes(), offsetArray[modifiedColumns[i - 1] - 1], bOut, offset, tmp.getSize() );
				offset += tmp.getSize();
				if ( verifyOriginalValue[ modifiedColumns[ i-1 ] - 1 ] > 0 ) {
					System.arraycopy( currRow.getOriginalBytes(), offsetArray[modifiedColumns[i - 1] - 1], bOut, offset, tmp.getSize() );
					offset += tmp.getSize();
				}
			}

			mpeSocket.out.write( bOut );

			/* Get error code and new record id from the server. */

			short recvPacketLength=0;

			recvPacketLength = mpeSocket.in.readShort();
			short errno = mpeSocket.in.readShort();
			int newRecordId = mpeSocket.in.readInt();

			/* In the event that a dbdelete/dbput had to be performed, assign the 
			 * new record id number to the current row. 
			 */

			ByteArray.setInt( currRow.getBytes(), 2, newRecordId );

			lastRecordNumber = newRecordId;

			//.if ( ! isMpeFile ) ((Dataset)dataTable).setLastPtr( lastRecordNumber );
			if ( ! isMpeFile ) ((Dataset)dataTable).setLastDatasetUser( this.registryId );
			
			currRow.finalizeUpdate();

			mpe3000.checkInSocket();

			if ( errno == -32761 ) throw new IntegrityException("TurboBuffer.updateRow:Data Integrity has been compromised",this.name,41001);
			if ( errno == -32760 ) throw new IntegrityException("TurboBuffer.updateRow:Record Key Integrity has been compromised",this.name,41000);

			if ( errno != 0 ) throw new ADBCException("TurboBuffer.updateRow:Image Error",this.name,errno);
			
			if ( errno == 0 && rowListeners != null ) fireRowUpdated( this.getRecordNumber() );

		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"TurboBuffer.updateRow:Communication Error",this.name,40000);}

	}

	/** Insert a row into the data table.  If row listeners have been added to the
	  * TurboBuffer, insertRow() calls the RowListener.rowPreInsert() and RowListener.rowInserted()
	  * methods.
	  * @see RowListener
	  */

	public final synchronized void insertRow() throws ADBCException, ValidationException {
	
		/* Items passed to the server:
		 * 01    01-02 "PU" - Tells server that we are inserting a record.
		 * 02    03-04 The database number.
		 * 03    05-06 The dataTable number.
		 * 04    07-08 0=autolock disabled, >0 lockmode for autolock.
		 * 05    09-10 Length of the entire dataTable.
		 * 06    11-12 File Type.
		 * 07    13-14 optionA.
		 * 08    15-16 optionB.
		 * 09    17-18 Number of columns to update = N.
		 *             Following is the record offsets and length
		 *             i=1..N
		 * (i-1)*2+10  Record offset for column.
		 * (i-1)*2+11  Length of the column.
		 *
		 * N*2+7       Start of the column data.
		 */

		if ( onInsertRow == 0 ) throw new ADBCException("TurboBuffer.insertRow:Current row is not the insertRow",this.name, 42000 );

		fetchWait();
		checkThreadException();

		this.fireRowPreInsert( 0 );

		short numModifiedColumns=0;

		numModifiedColumns = (short) insertRow.getNumModifiedColumns();

		if ( numModifiedColumns <= 0 ) return;

		int[] modifiedColumns = new int[ numModifiedColumns ];

		short sendPacketLength = 18;   // Base length.

		sendPacketLength += ( numModifiedColumns * 4 );    // 4 bytes for each column, 2 for column offset and 2 for length.

		int k=-1;

		for (int i=1;i<=numColumns;i++) 
			if ( insertRow.isColumnModified( i ) ){
				k++;
				modifiedColumns[ k ] = i;   
				sendPacketLength += ((TurboColumn) columns.elementAt( i - 1 )).getSize();   /* get the column length from column Vector. */
			}

		mpeSocket = mpe3000.checkOutSocket();

		try {

			byte[] bOut = new byte[sendPacketLength + 2];

			ByteArray.setShort ( bOut, 0, sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "PU" );
			ByteArray.setShort ( bOut, 4, dataTable.getDataSourceId() );
			ByteArray.setShort ( bOut, 6, dataTable.getIdentifier() );
			ByteArray.setShort ( bOut, 8, ( autoLockEnabled ? (short) dataTable.getDefaultLockMode() : (short) 0 ) );
			ByteArray.setShort ( bOut, 10, (short) rowSet.getLength() );
			ByteArray.setShort ( bOut, 12, (short) dataTable.getType() );

			int optionA = 0;
			int optionB = 0;

			if ( isMpeFile ) 
				optionA = (((MpeFile) dataTable).isCCTL()? 1 : 0 );
			
			ByteArray.setShort ( bOut, 14, (short) optionA );
			ByteArray.setShort ( bOut, 16, (short) optionB );

			ByteArray.setShort ( bOut, 18, numModifiedColumns );
	
			int offset = 20;

			/* Write out the offset and length of each column. */

			for (int i=1;i<=numModifiedColumns;i++) {
				
				/* In next statement, TurboColumns are stored in Vector columns 0..numColumns-1.
				   where data is store in Row class 1..numColumns.  Element 0 is recordId. 
				   therefore must subtract 1 in next statement to get the TurboColumn. */

				TurboColumn tmp = (TurboColumn) columns.elementAt( modifiedColumns[ i-1 ] - 1 );
				ByteArray.setShort( bOut, offset, tmp.getOffset() );
				ByteArray.setShort( bOut, offset + 2, tmp.getSize() );
				offset += 4;
			}

			/* Write out the data. */

			for (int i=1;i<=numModifiedColumns;i++) {
				TurboColumn tmp = (TurboColumn) columns.elementAt( modifiedColumns[ i-1 ] - 1 );
				System.arraycopy( insertRow.getBytes(), offsetArray[modifiedColumns[i - 1] - 1], bOut, offset, tmp.getSize() );
				offset += tmp.getSize();
			}

			mpeSocket.out.write( bOut );

			/* Get error code and new record id from the server. */

			short recvPacketLength=0;

			recvPacketLength = mpeSocket.in.readShort();
			short errno = mpeSocket.in.readShort();

			//.int newRecordId = mpeSocket.in.readInt();
			//System.out.println("lastRecordNumber prior to insert:" + lastRecordNumber);
			lastRecordNumber = mpeSocket.in.readInt();
			//System.out.println("insert row recNo:" + lastRecordNumber);
			
			/* Set the record number in the insert row. */

			//.ByteArray.setInt( insertRow.getBytes(), 2, newRecordId );
			ByteArray.setInt( insertRow.getBytes(), 2, lastRecordNumber );


			//.if ( ! isMpeFile ) ((Dataset)dataTable).setLastPtr( newRecordId );
			if ( ! isMpeFile ) ((Dataset)dataTable).setLastDatasetUser( this.registryId );

			insertRow.finalizeUpdate();

			if ( cBuffer != null && ! isKsamFile ) cBuffer.clipCache( lastRecordNumber );

			mpe3000.checkInSocket();

			if ( errno != 0 ) throw new ADBCException("TurboBuffer.insertRow:Image Error",this.name, errno);
			
			if ( errno == 0 && rowListeners != null ) fireRowInserted( lastRecordNumber ); 

			onInsertRow = 2;  // Signify that still on the insert row, but it has been updated.

		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"TurboBuffer.insertRow:Communication Error",this.name,40000);}

	}

	/** Delete the current row in the TurboBuffer's data table.  If row listeners
	  * have been added to the TurboBuffer, deleteRow() calls the 
	  * RowLister.rowPreDelete() and RowListener.rowDeleted() methods.
	  * @see RowListener
	  * @see #setUniqueKey
	  */

	public final synchronized void deleteRow() throws ADBCException,
	                                           ValidationException, IntegrityException {

		/* Items passed to the server:
		 * 01    01:02 "DE" : Tells server that we are updating the current record.
		 * 02    03:04 The database number.
		 * 03    05:06 The dataTable number.
		 * 04    07:08 0=autolock disabled, >0 lockmode for autolock.
		 * 05    09:10 Length of the entire dataTable.
		 * 06    11:12 file type.
		 * 07:08 13:16 Record Number of column to be deleted.
		 * 09    17:18 Number of columns marked as unique.
		 *
		 *       i..*9
		 *
		 * (10+(i-1)*2) Unique column offset.
		 * (10+(i-1)*2+1) Unique column length.
		 *
		 *       (10+(*9*2)) The beginning of the unique column data.
		 */

		fetchWait();
		checkThreadException();

		this.fireRowPreDelete( this.getRecordNumber() );

		mpeSocket = mpe3000.checkOutSocket();

		short sendPacketLength = 18;

		if ( numUniqueColumns > 0 ) 
			for (int i=1;i<=numColumns;i++) 
				if ( verifyOriginalValue[i-1] == 2 )
					sendPacketLength += 4 + ((TurboColumn) columns.elementAt(i-1)).getSize();

		try {

			byte[] bOut = new byte[sendPacketLength + 2];

			ByteArray.setShort ( bOut, 0, sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "DE" );
			ByteArray.setShort ( bOut, 4, dataTable.getDataSourceId() );
			ByteArray.setShort ( bOut, 6, dataTable.getIdentifier() );
			ByteArray.setShort ( bOut, 8, ( autoLockEnabled ? (short) dataTable.getDefaultLockMode() : (short) 0 ) );
			/*2.0.1*/
			ByteArray.setShort ( bOut, 10,( isMpeFile ? (short)((MpeFile)dataTable).getRecSize() : (short) rowSet.getLength() ) );
			ByteArray.setShort ( bOut, 12, (short) dataTable.getType() );
			ByteArray.setInt   ( bOut, 14, this.getRecordNumber() );
			ByteArray.setShort ( bOut, 18, (short) this.numUniqueColumns );

			int offset = 20;

			if ( numUniqueColumns > 0 ) {

				for (int i=1;i<=numColumns;i++) {
					if ( verifyOriginalValue[i-1] == 2 ) {
						TurboColumn tmp = (TurboColumn) columns.elementAt(i-1);
						ByteArray.setShort( bOut, offset, tmp.getOffset() );
						ByteArray.setShort( bOut, offset + 2, tmp.getSize() );
						offset += 4;
					}
				}

				for (int i=1;i<=numColumns;i++) {
					if ( verifyOriginalValue[i-1] == 2 ) {
						TurboColumn tmp = (TurboColumn) columns.elementAt(i-1);
						System.arraycopy( currRow.getOriginalBytes(), offsetArray[i - 1], bOut, offset, tmp.getSize() );
						offset += tmp.getSize();
					}
				}
			}

			//System.out.println("Sending delete packet...");

			mpeSocket.out.write( bOut );

			short recvPacketLength=0;

			recvPacketLength = mpeSocket.in.readShort();
			//System.out.println("Delete packet return, length=" + recvPacketLength);
			short errno = mpeSocket.in.readShort();
			//System.out.println("Delete errno=" + errno );
			this.prevRecNo = mpeSocket.in.readInt();
			//System.out.println("Delete prevrec=" + prevRecNo );
			this.nextRecNo = mpeSocket.in.readInt();
			//System.out.println("Delete nextrec=" + nextRecNo );

			int deletedRecNum = ByteArray.getInt( currRow.getBytes(), 2 );

			lastRecordNumber = deletedRecNum;
			//.if ( ! isMpeFile ) ((Dataset)dataTable).setLastPtr( lastRecordNumber );
			if ( ! isMpeFile ) ((Dataset)dataTable).setLastDatasetUser( this.registryId );

			cBuffer.removeElement();

			if ( cBuffer.size() == 0 ) dataTableReset = 2;

			/* If the dataTable is an MPE file, subtract out one from each record
			 * number that is stored in the cBuffer.
			 */

			if ( isMpeFile && dataTable.getType() == Foption.STD ) {
				Enumeration en = cBuffer.elements();
				while ( en.hasMoreElements() ) {
					byte[] b = (byte[]) en.nextElement();
					int recNum = ByteArray.getInt( b, 2 );
					if ( recNum > deletedRecNum ) {
						ByteArray.setInt( b, 2, recNum - 1 );
					}
				}
			}

			currRow = null;

			mpe3000.checkInSocket();

			if ( errno != 0 ) {
				if ( errno == -32760 ) 
					throw new IntegrityException("TurboBuffer.deleteRow:Record Key Integrity has been compromised",this.name,41003);
				throw new ADBCException("TurboBuffer.deleteRow:Image Error",this.name,errno);
			}

			if ( errno == 0 && rowListeners != null ) fireRowDeleted( deletedRecNum );

		} catch (IOException e) {throw new ADBCIOException(mpe3000,"TurboBuffer.deleteRow:Communication Error",this.name,40000);}

	}

	/** Refresh the current row in the TurboBuffer.  If row listeners have been added
	  * to the TurboBuffer, refreshRow() calls the RowListener.rowPreRefresh() and
	  * RowListener.rowRefreshed() methods.
	  * @see RowListener
	  * @see #setUniqueKey
	  */

	public final synchronized void refreshRow() throws ADBCException,
	                                            ValidationException, IntegrityException {

		/* Items passed to the server:
		 * 01    01:02 "TR" : Tells server that we are updating the current record.
		 * 02    03:04 The database number.
		 * 03    05:06 The dataTable number.
		 * 04    07:08 0=autolock disabled, >0 lockmode for autolock.
		 * 05    09:10 Length of the entire dataTable.
		 * 06    11:12 file type.
		 * 07:08 13:16 Record Number of column to be refreshed.
		 * 09    17:18 Number of columns marked as unique.
		 * 10    19:20 Number of columns in TurboBuffer.
		 *
		 *       i..*9
		 *
		 * (11+(i-1)*2) Unique column offset.
		 * (11+(i-1)*2+1) Unique column length.
		 *
		 *       i..*10
		 *
		 * (11+(*9*2)+(i-1)*2) column offset.
		 * (11+(*9*2)+(i-1)*2+1) column length.
		 *
		 *       (11+(*9*2)) The beginning of the unique column data.
		 */

		fetchWait();
		checkThreadException();

		this.fireRowPreRefresh( this.getRecordNumber() );

		try {
			while ( fetching ) 
				wait();
		} catch (InterruptedException ex) { ex.printStackTrace();}

		mpeSocket = mpe3000.checkOutSocket();

		int sendPacketLength = 20 + ( numColumns * 4 );

		if ( numUniqueColumns > 0 ) 
			for (int i=1;i<=numColumns;i++) 
				if ( verifyOriginalValue[i-1] == 2 )
					sendPacketLength += 4 + ((TurboColumn) columns.elementAt(i-1)).getSize();

		try {

			byte[] bOut = new byte[sendPacketLength + 2];

			lastRecordNumber = this.getRecordNumber();

			ByteArray.setShort ( bOut, 0, (short) sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "TR" );
			ByteArray.setShort ( bOut, 4, dataTable.getDataSourceId() );
			ByteArray.setShort ( bOut, 6, dataTable.getIdentifier() );
			ByteArray.setShort ( bOut, 8, ( autoLockEnabled ? (short) dataTable.getDefaultLockMode() : (short) 0 ) );
			ByteArray.setShort ( bOut, 10, (short) rowSet.getLength() );
			ByteArray.setShort ( bOut, 12, (short) dataTable.getType() );
			ByteArray.setInt   ( bOut, 14, this.getRecordNumber() );/*+-+-+-*/
			ByteArray.setShort ( bOut, 18, (short) this.numUniqueColumns );
			ByteArray.setShort ( bOut, 20, this.numColumns );

			int offset = 22;

			if ( numUniqueColumns > 0 ) {

				for (int i=1;i<=numColumns;i++) {
					if ( verifyOriginalValue[i-1] == 2 ) {
						TurboColumn tmp = (TurboColumn) columns.elementAt(i-1);
						ByteArray.setShort( bOut, offset, tmp.getOffset() );
						ByteArray.setShort( bOut, offset + 2, tmp.getSize() );
						offset += 4;
					}
				}
			}

			for (int i=1;i<=numColumns;i++) {
				TurboColumn tmp = (TurboColumn) columns.elementAt(i-1);
				ByteArray.setShort( bOut, offset, tmp.getOffset() );
				ByteArray.setShort( bOut, offset + 2, tmp.getSize() );
				offset += 4;
			}

			if ( numUniqueColumns > 0 ) {

				for (int i=1;i<=numColumns;i++) {
					if ( verifyOriginalValue[i-1] == 2 ) {
						TurboColumn tmp = (TurboColumn) columns.elementAt(i-1);
						System.arraycopy( currRow.getOriginalBytes(), offsetArray[i - 1], bOut, offset, tmp.getSize() );
						offset += tmp.getSize();
					}
				}
			}

			mpeSocket.out.write( bOut );

			short recvPacketLength=0;

			recvPacketLength = mpeSocket.in.readShort();
			short errno = mpeSocket.in.readShort();

			if ( errno == 0 ) {

				byte[] b = new byte[recvPacketLength - 2 + 2];
				mpeSocket.in.readFully( b, 2, recvPacketLength - 2 );


				/* Read in HP Image record number */

				cBuffer.setElement( b );

			} else {  // errno != 0

				if ( errno == -32760 ) throw new 
					IntegrityException("TurboBuffer.refreshRow:Record Key Integrity has been compromised",this.name,41002);
				throw new ADBCException("TurboBuffer.refreshRow:Image Error",this.name,errno);

			}

		} catch (IOException e) {throw new ADBCIOException(mpe3000,"TurboBuffer.refreshRow:Communication Error",this.name,40000);}

		//.if ( ! isMpeFile ) ((Dataset)dataTable).setLastPtr( lastRecordNumber );
		if ( ! isMpeFile ) ((Dataset)dataTable).setLastDatasetUser( this.registryId );

		lastRecordNumber = this.getRecordNumber();

		mpe3000.checkInSocket();

		this.fireRowRefreshed( lastRecordNumber );
	}

	/** Sends a {@link RowEvent} to all {@link RowListener RowListener's} registered 
	  * with this TurboBuffer. The RowEvent will be passed through the 
	  * {@link RowListener#rowValidate RowListener.rowValidate()} method.
	  * This method is useful to guarantee that data in the current row adheres
	  * to the business rules of the organization.  It allows the developer to check
	  * the business rules prior to calling the TurboBuffer's row update methods.
	  * Business rules can also be verified inside each of the row update methods,
	  * prior to, and after a row update takes place.
	  */

	public final synchronized void validateRow() throws ADBCException, ValidationException {

		this.fireRowValidate( this.getRecordNumber() );

	}

	/** Get the record number of the TurboBuffers current record.  If the data table is
	  * a dataset, the Turbo Image dataset record number is returned.  If the data table is
	  * an MPE file, the current logical record pointer value is returned.
	  * @return the record number.
	  */

	public final int getRecordNumber() {
		return ByteArray.getInt( currRow.getBytes(), 2 );
	}

	//public int getNumRows() {
	//	return numRows;
	//}

	/** Return the number of columns that the TurboBuffer represents.  Note that
	  * this does not include the number of computed columns.
	  * @return the number of columns.
	  */

	public final int getNumColumns() {
		return (int) numColumns;
	}

	/** Return the number of computed columns that have been added to this TurboBuffer.
	  * @return the number of computed columns.
	  */

	public final int getNumComputedColumns() {
		return (int) numComputedColumns;
	}

	public final void setToStringColumn( String columnName ) {
		this.toStringColumn = columnName;
	}

	/** Return a tab seperated data list of all the columns in the current row
	  */
	
	public String toString() {
		String st=null;
		try {
			if ( this.toStringColumn != null ) {
				st = this.getString( this.toStringColumn );
			} else {
				StringBuffer stBuf = new StringBuffer( (int) (this.length * 1.5) );
				int numCols = this.numColumns + this.numComputedColumns;
				for (int i=1;i<=numCols;i++) {
					stBuf.append( this.getString( i ) );
					if ( i != numCols ) stBuf.append( '\t' );
				}
				st = new String( stBuf );
			}
		} catch (ADBCException e) { throw new ADBCRuntimeException("TurboBuffer.toString:Operation Failed",this.name,50000);}
		return st;
	}

	/** Get the name of a column from it's column number. 
	  * @param columnNo the number of the column.
	  * @return the column name.
	  */

	public final String getColumnName( int columnNo ) {
		if ( columnNo < 0 && columnNo > this.numColumns + this.numComputedColumns )
			throw new ADBCArgumentException("TurboBuffer.getColumnName:Invalid columnNo",this.name,String.valueOf(columnNo),43001);
		if ( columnNo <= this.numColumns )  /*FIX0001*/
			return ((TurboColumn)columns.elementAt( columnNo - 1 )).getName();
		/* Computed Column */
		return (String) columnAlias.elementAt( columnNo - 1 );
	}

	/** Return the column alias for the specified column.
	  * @param columnNo The desired column.
	  */
 public final String getColumnType(int colNum) {
                                        
                    TurboColumn col = (TurboColumn) columns.elementAt(colNum);

                    return col.getColumnType();
       }
       public final int getColumnLength(int colNum) {
               short columnSize = ((TurboColumn) columns.elementAt( colNum - 1 )).getSize();
            //   System.out.println("size = "+columnSize);
               return columnSize;
      }
      public final void initializeBuffer() {
                 
                    String type_check="";
                    int colcount = this.getNumColumns();
                  //  System.out.println("Number of cols = "+colcount);
                    int cntr = 1;
                    while ( cntr <= colcount ) {
                            type_check = this.getColumnType(cntr - 1);
                           // System.out.println("FClumn name = "+this.getColumnName(cntr) + " type =" +type_check+ "["+cntr+"]");
                            type_check = type_check.trim();
                          try {
                            if ( type_check.equals("X") ) {
                                 this.updateString(cntr, " ");
                            }
                            if ( type_check.equals("Z") ) {
                                 int csize = this.getColumnLength(cntr);
                                 byte[] b = new byte[csize];
                                 b=this.initializeZ(csize);
                                 this.updateBytes(cntr, b);
                            }
                            cntr++;
                           } catch (ADBCException ex) { ex.printStackTrace();}
                    }
                     

        }
        private byte[] initializeZ(int len) {
             // Temp fix for Z fields.     
                byte[] bwork = new byte[len];
                int cntr = 0;
                while ( cntr < len ) {
                        bwork[cntr] = '0';
                        cntr++;
                }
                
                return bwork;
        }

	public final String getColumnAlias( int columnNo ) {
		if ( columnNo < 0 && columnNo > this.numColumns + this.numComputedColumns )
			throw new ADBCArgumentException("TurboBuffer.getColumnAlias:Invalid columnNo",this.name,String.valueOf(columnNo),43001);
		return (String) columnAlias.elementAt( columnNo - 1 );
	}

	/** Set the number of records that are returned to the TurboBuffer in one
	  * one read request.  The fetchSize is synonymous with the number of
	  * records that are cached at the client.  This allows faster retrieval of
	  * data.  If the TurboBuffer's data table has a high volume of transaction
	  * processing, it would be advisable to keep this number low.  One is the safest.
	  * However, with a large fetch size, data integrity can be maintained, via the 
	  * TurboBuffer.setVerifyOn() and TurboBuffer.setUniqueKey() methods.
	  * @param fetchSize The number records to fetch in a single read request.
	  */

	public final synchronized void setFetchSize( int fetchSize ) {
		
            try{
            fetchWait();
            }catch (ADBCException e) { e.printStackTrace();}


		if ( fetchSize < 1 ) throw new ADBCArgumentException("TurboBuffer.setFetchSize:Invalid:fetchSize=" + fetchSize,this.name,43100 );
		this.fetchSize = fetchSize;
		if ( fetchSize < 1 ) fetchSize = 1;
	}

	/** When AutoLock is enabled, locking of the data table is handled automatically
	  * with each updateRow(), insertRow() and deleteRow().  Each request of these
	  * methods will automatically lock the data table, perform the desiginated
	  * function and then unlock the data table.  By default, AutoLock is disabled.
	  * @param enabled Set true to enable AutoLock, false to disable AutoLock.
	  */

	public final void setAutoLock( boolean enabled ) {
		autoLockEnabled = enabled;
	}

	/** Add a row listener to the TurboBuffer.  A row listener can be used to 
	  * validate data prior row status changes( update, delete, insert, refresh ).
	  * It can also be used to notify other objects, visual or non-visual, that
	  * row status is changing, or has changed.  The listening objects can reflect
	  * these changes appropriately.  If the row listener is being used for
	  * data validation, the programmer is given the ability to fail the RowEvent
	  * that is passed to each listener.  The failed event will generate a
	  * ValidationException.  A programmer can fail the event with their own
	  * custom ValidationException if desired.  The only requirement is that
	  * the customized exception must extend the advnetsys.adbc.ValidationException
	  * class.
	  * @param rowListener The row listener.
	  * @see RowListener
	  * @see AbstractRowListener
	  * @see RowEvent
	  * @see ValidationException
	  */

	public final void addRowListener( RowListener rowListener ) {
		if ( rowListeners == null ) rowListeners = new Vector(4);
		rowListeners.addElement( rowListener );
	}

	/** Remove a row listener from a TurboBuffer.
	  * @param rowListener The row listener to remove.
	  * @see RowListener
	  */

	public final void removeRowListener( RowListener rowListener ) {
		int i = rowListeners.indexOf( rowListener );
		rowListeners.removeElementAt( i );
		if ( rowListeners.size() == 0 ) rowListeners = null;
	}

	/** Add a column listener to the TurboBuffer.  The column listener can monitor
	  * changes to a TurboBuffer column prior to and after a change to the column
	  * occurs.  It can be used to validate data, or to notify other objects, 
	  * visual or non-visual, that column status is changing, or has changed.  The
	  * listening objects can reflect these changes appropriately.  Much like the 
	  * row listener, if the column listener is used for data validation, the programmer
	  * is given the ability to fail the ColumnEvent that is passed to each listener.
	  * The failed event will generate a ValidationException.  A programmer can fail the event
	  * with their own custom ValidationException if desired.  The only requirement is that
	  * the customized exception must extend the advnetsys.adbc.ValidationException
	  * class.
	  * @param columnListener The column listener.
	  * @see ColumnListener
	  * @see AbstractColumnListener
	  * @see ColumnEvent
	  * @see ValidationException
	  */

	public final void addColumnListener( ColumnListener columnListener ) {
		if ( columnListeners == null ) columnListeners = new Vector(4);
		columnListeners.addElement( columnListener );
	}

	/** Remove a column listener from the TurboBuffer.
	  * @param The column listener to remove.
	  * @see ColumnListener
	  */

	public final void removeColumnListener( ColumnListener columnListener ) {
		int i = columnListeners.indexOf( columnListener );
		columnListeners.removeElementAt( i );
		if ( columnListeners.size() == 0 ) columnListeners = null;
	}

	private final void fireRowPreInsert( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_PREINSERT, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowPreInsert( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowInserted( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_INSERTED, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowInserted( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowPreDelete( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_PREDELETE, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowPreDelete( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowDeleted( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_DELETED, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowDeleted( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowPreUpdate( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_PREUPDATE, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowPreUpdate( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowUpdated( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_UPDATED, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowUpdated( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowPreRefresh( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_PREREFRESH, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowPreRefresh( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowRefreshed( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_REFRESHED, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowRefreshed( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowCurrentChanged( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_CURRENTCHANGED, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowCurrentChanged( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireRowValidate( int rowNum_ ) throws ADBCException {
		if ( rowListeners == null ) return;
		RowEvent e = new RowEvent( this, RowEvent.ROW_VALIDATE, rowNum_ );
		for ( int i=0;i<=rowListeners.size() - 1;i++) 
			((RowListener)rowListeners.elementAt( i )).rowValidate( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireColumnPreUpdate( int rowNum_, String columnName, int columnNo, Object value ) throws ADBCException {
		if ( columnListeners == null ) return;
		ColumnEvent e = new ColumnEvent( this, ColumnEvent.COLUMN_PREUPDATE, rowNum_, columnName, columnNo, value );
		for ( int i=0;i<=columnListeners.size() - 1;i++) 
			((ColumnListener)columnListeners.elementAt( i )).columnPreUpdate( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private final void fireColumnUpdated( int rowNum_, String columnName, int columnNo, Object value ) throws ADBCException {
		if ( columnListeners == null ) return;
		ColumnEvent e = new ColumnEvent( this, ColumnEvent.COLUMN_UPDATED, rowNum_, columnName, columnNo, value );
		for ( int i=0;i<=columnListeners.size() - 1;i++) 
			((ColumnListener)columnListeners.elementAt( i )).columnUpdated( e );
		if ( e.hasFailed() ) throw e.getException();
	}

	private void checkThreadException() throws ADBCException {
		if ( threadException != null ) throw threadException;
	}

}
