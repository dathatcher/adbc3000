package advnetsys.adbc;

import java.util.Vector;
import java.util.Hashtable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;
import java.util.Enumeration;

class ColumnNameParser extends Object {

	Vector selectedColumns = new Vector();

	ColumnNameParser( String columnNames, Hashtable columnOrder, int numColumns, String tbName, String methodName, int errBase ) throws ADBCException {

		if ( columnNames.equals("@") ) {
			for (int i=1;i<=numColumns;i++)
				selectedColumns.addElement( new Integer(i) );
			return;
		}

		StreamTokenizer st = new StreamTokenizer( new StringReader( columnNames ) );
		st.whitespaceChars( ' ',' ' );
		st.whitespaceChars( ',',',' );
		st.ordinaryChar( ':' );
		st.wordChars( '_', '_' );  //2.1

		int lastIndex = -1;
		boolean isRange=false;

		try {
			while ( st.nextToken() != st.TT_EOF ) {
				String colName="";
				if ( st.ttype == ':' ) {
					colName = ":";
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD ) {
					colName = st.sval.toLowerCase();
				} else {
					throw new ADBCException("TurboBuffer." + methodName + ":Invalid column specification",tbName,columnNames,errBase);
				}

				if ( colName.equals(":") ) {
					if ( isRange || lastIndex == -1 ) {
						throw new ADBCException("TurboBuffer." + methodName + ":Invalid column range selection",tbName,columnNames,errBase + 1);
					}
					isRange = true;
				} else {

					Integer colIndex = (Integer) columnOrder.get( colName );

					if ( colIndex == null ) 
						throw new ADBCException("TurboBuffer." + methodName + ":Invalid Column",tbName,colName,errBase + 2);

					if ( isRange && lastIndex > 0 ) {
						if ( lastIndex < colIndex.intValue() ) {
							//for (int i=lastIndex+1;i<colIndex.intValue();i++) {
							for (int i=lastIndex+1;i<=colIndex.intValue();i++) {
								selectedColumns.addElement( new Integer( i ) );
							}
						} else if ( lastIndex > colIndex.intValue() ) {
							//for (int i=lastIndex-1;i>colIndex.intValue();i--) {
							for (int i=lastIndex-1;i>=colIndex.intValue();i--) {
								selectedColumns.addElement( new Integer( i ) );
							}
						}
						isRange = false; //2.0.1
						lastIndex = -1;  //2.0.1

					//}
					} else {

					//if ( lastIndex != colIndex.intValue() ) {
						selectedColumns.addElement( new Integer( colIndex.intValue() ) );
					}

					lastIndex = colIndex.intValue();
					//if ( lastIndex == -1 ) lastIndex = colIndex.intValue();
					//else lastIndex = -1;
				}
			}

		} catch (IOException e) {throw new ADBCException("TurboBuffer." + methodName + "Column Parsing Error",tbName,columnNames,errBase + 3);}
	}

	public Enumeration elements() {
		return selectedColumns.elements();
	}
}
