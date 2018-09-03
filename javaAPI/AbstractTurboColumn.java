package advnetsys.adbc;

import java.text.Format;
import java.util.Date;
import java.math.BigDecimal;

abstract class AbstractTurboColumn extends Object 
				implements TurboColumn {

	protected String name;
	protected short  columnId;
	protected String columnType;
	protected short    size=0;
	protected short    offset=0;
	protected boolean keyColumn=false;
	protected boolean variableLength=false;

	public AbstractTurboColumn() {}

	public AbstractTurboColumn( String name_, short columnId_, String columnType_, int size_ ) {

		name = name_.toLowerCase();
		columnId = columnId_;
		size = (short) size_;
		columnType = columnType_;

	}

	public void setKeyColumn( boolean isKeyColumn ) {
		keyColumn = isKeyColumn;
	}

	public boolean isKeyColumn() { return keyColumn; }

	public void setVariableLength( boolean enabled_ ) {
		variableLength = enabled_;
		if ( variableLength ) this.size = -1;
	}	
  	

	public boolean isVariableLength() { return variableLength; }

	public void setOffset( short offset_ ) { offset = offset_; }
	public short getSize() { return size; }
	public short getOffset() { return offset; }
	public String getName() { return name; }
	public short getColumnId() { return columnId; }
	public String getColumnType() { return columnType; }

	public abstract short getShort( byte[] b, int offset ) throws ADBCException;
	public abstract void setShort( byte[] b, int offset, short value ) throws ADBCException;
	public abstract int getInt( byte[] b, int offset ) throws ADBCException;
	public abstract void setInt( byte[] b, int offset, int value ) throws ADBCException;
	public abstract long getLong( byte[] b, int offset ) throws ADBCException;
	public abstract void setLong( byte[] b, int offset, long value ) throws ADBCException;
	public abstract float getFloat( byte[] b, int offset, int scale ) throws ADBCException;
	public abstract void setFloat( byte[] b, int offset, int scale, float value ) throws ADBCException;
	public abstract double getDouble( byte[] b, int offset, int scale ) throws ADBCException;
	public abstract void setDouble( byte[] b, int offset, int scale, double value ) throws ADBCException;
	public abstract String getString( byte[] b, int offset, int scale, Format format ) throws ADBCException;
	public abstract void setString( byte[] b, int offset, int scale, String value, Format format ) throws ADBCException;
	public abstract Object getObject( byte[] b, int offset, int scale ) throws ADBCException;
	public abstract void setObject( byte[] b, int offset, int scale, Object value ) throws ADBCException;
	//public abstract byte[] getByteArray( byte[] b, int offset ) throws ADBCException;
	public abstract void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException;
	public abstract Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException;
	public abstract void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException;
	public abstract BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException;

	public byte[] getByteArray( byte[] b, int offset ) throws ADBCException {
		byte[] btmp = new byte[this.size];
		System.arraycopy( b, offset, btmp, 0, this.size );
		return btmp;
	}

	public void setByteArray( byte[] b, int offset, byte[] value ) throws ADBCException {
		int bSize = value.length;
		if ( bSize > this.size ) bSize = this.size;
		if ( bSize == 0 ) return;
		System.arraycopy( value, 0, b, offset, bSize );
	}

	public int compare( byte[] b1, byte[] b2, int offset ){
		int i = offset + this.size - 1;
		while ( i <= offset && b1[i] == b2[i] )
			i++;
		if ( i == offset + 1 ) return 0;
		return ( b1[i] < b2[i] ? -1 : 1 );
	}

}
