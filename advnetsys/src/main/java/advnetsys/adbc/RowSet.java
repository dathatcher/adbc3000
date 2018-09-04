package advnetsys.adbc;

import java.lang.*;
import java.util.*;
import java.io.*;

/** A RowSet is a definition of columns that a DataTable provides to a TurboBuffer. 
  * The DataTable can be either an ADBC Dataset or an ADBC MpeFile.  When an ADBC Dataset
  * is created, the RowSet is automatically created by the Database object.  MpeFile's 
  * must create a RowSet and assign it to the MpeFile instance using the MpeFile.setRowSet()
  * method.
  */

public class RowSet extends Object {

	private Vector columns=new Vector();
	private String name;
	private short length=0;
	private int numColumns=0;
	//Hashtable columnHash=new Hashtable();
	private Hashtable columnOrder = new Hashtable();
	private int numKeyColumns=0;
	private int firstKeyColumn=0;
	private boolean variableSet=false;
	private boolean finalized=false;
	//boolean compatibilityMode;

	public RowSet() {}

	public RowSet( String name ) { 
		this.setName( name ); 
	}

	public final void setName( String name ) {
		this.name = name;
	}

	final Hashtable getColumnOrder() {
		return columnOrder;
	}

	/** Finalize the RowSet.  When this method is called, additional columns can
	  * not be added to this RowSet.
	  */

	public final void setFinalized() {
		finalized = true;
	}

	/*2.0.1*/
	final boolean isVariableLength() {
		return variableSet;
	}

	private void add( TurboColumn column_ ) {
		column_.setOffset( (short) (1 + length) );
		length += column_.getSize();
		columns.addElement( column_ );
		//columnHash.put( column_.getName(), column_ );
		numColumns++;
		columnOrder.put( column_.getName(), new Integer( numColumns ) );
	}

	/*2.0.1*/
	public final String getColumnName( int columnNo ) {
		if ( columnNo <= 0 || columnNo > numColumns ) 
			throw new ADBCArgumentException("RowSet.getColumnName:Invalid Column");
		return ((TurboColumn)columns.elementAt( columnNo - 1 )).getName();
	}

	/*2.0.1*/
	public final boolean isKeyColumn( int columnNo ) {
		if ( columnNo <= 0 || columnNo > numColumns )
			throw new ADBCArgumentException("RowSet.isKeyColumn:Invalid Column");
		return ((TurboColumn)columns.elementAt( columnNo - 1 )).isKeyColumn();
	}

	final TurboColumn getColumn( int columnNo_ ) throws ADBCException {
		if ( columnNo_ < 1 || columnNo_ > numColumns ) 
			throw new ADBCException("RowSet.getColumn:Invalid columnNo",this.name,String.valueOf(columnNo_),43010);

		return ( TurboColumn ) columns.elementAt( columnNo_ - 1 ); 
	}

	final TurboColumn getColumn( String columnName_ ) throws ADBCException {
		Integer colNo = (Integer) columnOrder.get( columnName_ );
		//TurboColumn tmp = ( TurboColumn ) columnHash.get( columnName_ );
		if ( colNo == null ) 
			throw new ADBCException("RowSet.getColumn:Invalid columnName",this.name,columnName_,43011);
		return (TurboColumn) columns.elementAt( colNo.intValue() - 1 );
		//return tmp;
	}

	final void setKeyColumnInternal( short columnId ) {

		boolean done=false;
		int i=0;
		
		while ( ! done && i < numColumns ) {
			i++;
			TurboColumn tmp = (TurboColumn) columns.elementAt( i - 1 );
			if ( tmp.getColumnId() == columnId ) {
				done = true;
				tmp.setKeyColumn( true );
				numKeyColumns++;
				if ( numKeyColumns == 1 ) firstKeyColumn = i;
			}
		}
	}

	/** Specify a column within the RowSet to be a key column.  Typically
	  * used when setting up a RowSet for a KSAM file.
	  * @param columnNo The column Number.
	  */

	public final void setKeyColumn( int columnNo ) {

		if ( finalized )
			throw new ADBCArgumentException("RowSet.setKeyColumn:RowSet has been finalized",this.name,43118);

		if ( columnNo < 1 || columnNo > numColumns ) 
			throw new ADBCArgumentException("RowSet.setKeyColumn:Invalid Column",this.name,String.valueOf(columnNo),43116);
		
		TurboColumn tmp = (TurboColumn) columns.elementAt( columnNo - 1 );
		tmp.setKeyColumn( true );
		numKeyColumns++;
		if ( numKeyColumns == 1 ) firstKeyColumn = columnNo;
	}

	/** Specify a column within the RowSet to be a key column.  Typically
	  * used when setting up a RowSet for a KSAM file.
	  * @param columnName The name of the key column.
	  */

	public final void setKeyColumn( String columnName ) {

		if ( finalized )
			throw new ADBCArgumentException("RowSet.setKeyColumn:RowSet has been finalized",this.name,43118);

		boolean done=false;
		int i=0;
		
		while ( ! done && i < numColumns ) {
			i++;
			TurboColumn tmp = (TurboColumn) columns.elementAt( i - 1 );
			if ( tmp.getName().equals(columnName.toLowerCase()) ) {
				done = true;
				tmp.setKeyColumn( true );
				numKeyColumns++;
				if ( numKeyColumns == 1 ) firstKeyColumn = i;
			}
		}

		if ( ! done ) throw new ADBCArgumentException("RowSet.setKeyColumn:Invalid column",this.name,columnName,43116);
	}

	final TurboColumn getFirstKeyColumn() throws ADBCException {
		if ( firstKeyColumn == 0 )  
			throw new ADBCException("RowSet.getFirstKeyColumn:Does Not Contain a key",this.name,43012);
		return ( TurboColumn ) columns.elementAt( firstKeyColumn - 1 ); 
	}
			
	/** Return the length, in bytes, of this RowSet. 
	  */

	public final int getLength() { return length; }

	/** Get the number of columns that have been assigned to this RowSet.
	  */

	public final int getNumColumns() { return numColumns; }
	public final String getName() { return name; }
	//public void setCompatibilityMode( boolean enabled ) {
	//	compatibilityMode = enabled;
	//}

	//public boolean isCompatibilityMode() { return compatibilityMode; }

	/** Add a column to this RowSet.  The allowable data types for the columns are 
	  * "X", "U", "R", "E", "I", "J", "P", "Z", "K", and "V".  All of these data
	  * types, with the exception of "V", are the Turbo Image data types.  "V" is
	  * a special data type for ADBC.  "V" stands for variable size byte array.  This
	  * data type is intended for a TurboBuffer to be able to read a variable sized MPE 
	  * file.  If the data type "V" is chosen, one and only one column may be defined
	  * for the RowSet.  Use the TurboBuffer's "getBytes()" method to retrieve the 
	  * byte array for each row.
	  * @param columnName The name to be assigned to the column.
	  * @param columnType The data type of the column.
	  * @param columnLength The length of the column.
	  */

	public final void addColumn( String columnName, String columnType, int columnLength ) {
		this.addColumn( columnName, columnType.toUpperCase(), (short) 0, columnLength );
	}

	final void addColumn( String columnName, String columnType,  short columnId,
	                       int columnLength )    {

		String columnTypeX = columnType.toUpperCase();
		TurboColumn col=null;
		boolean lengthError=false;

		/* Check if the RowSet has been finalized */

		if ( finalized )
			throw new ADBCArgumentException("RowSet.addColumn:RowSet has been finalized, " +
			                                "Additional columns may not be added. " +
			                                " Column Name: " + columnName + 
			                                " Column Type: " + columnType + 
			                                " Column Length: " + columnLength, 43117 );

		/* If a variable sized byte array column is specified, be sure that one 
		   and only one column is used for this RowSet. */

		if ( variableSet ) {
			throw new ADBCArgumentException("RowSet.addColumn:Only one column allowed if the Variable data type has been selected",this.name,43115);
		}
		
		if        ( "X".equals(columnTypeX) || 
		            "U".equals(columnTypeX) ) {
		
			col = new TurboString( columnName, columnId, columnTypeX, columnLength );
			
		} else if ( "R".equals(columnTypeX) ) {
	
			if ( columnLength == 2 ) {
			
				col = new TurboFloatCM( columnName, columnId, columnTypeX, 4 );
				
			} else if ( columnLength == 4 ) {
			
				col = new TurboDoubleCM( columnName, columnId, columnTypeX, 8 );

			} else { lengthError = true; }
				
		} else if ( "E".equals(columnTypeX) ) {

			if ( columnLength == 2 ) {
			
				col = new TurboFloat( columnName, columnId, columnTypeX, 4 );
				
			} else if ( columnLength == 4 ) {
			
				col = new TurboDouble( columnName, columnId, columnTypeX, 8 );

			} else { lengthError = true; };
		
		} else if ( "I".equals(columnTypeX) || 
		            "J".equals(columnTypeX) ) {
		
			if ( columnLength == 1 ) 

				col = new TurboShort( columnName, columnId, columnTypeX, 2 );

			else if ( columnLength == 2 ) 

				col = new TurboInt( columnName, columnId, columnTypeX, 4 );

			else if ( columnLength == 4 ) 

				col = new TurboLong( columnName, columnId, columnTypeX, 8 );

			else { lengthError = true; }

		} else if ( "P".equals(columnTypeX) ) {

			col = new TurboPacked( columnName, columnId, columnTypeX, columnLength / 2 );

		} else if ( "Z".equals(columnTypeX) ) {

			col = new TurboZ( columnName, columnId, columnTypeX, columnLength );

		} else if ( "K".equals(columnTypeX) ) {

			//if ( columnLength == 1 ) col = new TurboShort( columnName, columnId, columnTypeX, columnLength );
			//else if ( columnLength == 2 ) col = new TurboInt( columnName, columnId, columnTypeX, columnLength );
			//else if ( columnLength == 4 ) col = new TurboLong( columnName, columnId, columnTypeX, columnLength );
			if ( columnLength == 1 ) col = new TurboShort( columnName, columnId, columnTypeX, 2 );
			else if ( columnLength == 2 ) col = new TurboInt( columnName, columnId, columnTypeX, 4 );
			else if ( columnLength == 4 ) col = new TurboLong( columnName, columnId, columnTypeX, 8 );
			else {
				lengthError = true;
				//throw new ADBCArgumentException( "RowSet.addColumn:Invalid RowSet column value" + 
				//                                    " Column Name: " + columnName +
				//                                    " Column Type: " + columnType +
				//                                    " Column Length: " + columnLength, this.name, 43113 );
			}

		} else if ( "V".equals(columnTypeX) ) {

			if ( getNumColumns() > 0 ) 
				throw new ADBCArgumentException("RowSet.addColumn:Only one column allowed if the Variable data type has been selected",this.name,43115);

			col = new TurboVariableByteArray( columnName, columnId, columnTypeX, columnLength );
			variableSet = true;  //2.0.1

		} else {
			throw new ADBCArgumentException( "RowSet.addColumn:Invalid RowSet column value" + 
												" Column Name: " + columnName +
												" Column Type: " + columnType +
												" Column Length: " + columnLength, this.name, 43113 );
		}

		if ( lengthError ) {
			throw new ADBCArgumentException( "RowSet.addColumn:Invalid column length specified" + 
												" Column Name: " + columnName + 
												" Column Type: " + columnType + 
												" Column Length: " + columnLength, this.name, 43114 );
		}

		this.add( col );

		//return col;

	}
	
}
