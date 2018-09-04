package advnetsys.adbc;

import java.lang.*;

class Row extends Object {

	private byte[] columnModified=null;
	private byte[] origb=null;
	private byte[] b=null;
	
	public Row( byte[] b ) {
		this.b = b;
	}

	public byte[] getBytes() { 
		return b; 
	}

	public byte[] getOriginalBytes() {
		if ( origb == null ) return b;
		return origb;
	}

	public void setModified( int columnNo, int numColumns ) {

		if ( origb == null ) {
			origb = b;
			b = (byte[]) origb.clone();
			columnModified = new byte[numColumns];
		}

		columnModified[columnNo - 1] = 1;
	}

	public boolean isModified() {
		return ( columnModified != null );
	}

	public boolean isColumnModified( int columnNo ) {

		if ( columnModified == null ) return false;

		if ( columnModified[columnNo - 1] != 1 ) return false;

		return true;
	}

	public int getNumModifiedColumns() {

		int numColumns=0;

		if ( columnModified == null ) return 0;

		for (int i=0;i<columnModified.length;i++)
			if ( columnModified[i] == 1 ) numColumns++;
		
		return numColumns;
	}

	public void finalizeUpdate() {

		origb = null;
		columnModified = null;

	}

	public void resetToOriginal() {

		if ( origb != null ) {
			b = null;
			b = origb;
			origb = null;
			columnModified = null;
		}
	}

	public int getRecordNumber() {
		return ByteArray.getInt( b, 0 );
	}
}

			

