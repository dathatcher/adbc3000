package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

class TurboVariableByteArray extends AbstractTurboColumn {

	//int totalBytesRead = 0;
	//int numBytesRead = 0;
	//byte[] b;


	public TurboVariableByteArray ( String name_, short columnId_, String columnType_, int size_ ) {
		super( name_, columnId_, columnType_, size_ );
		setVariableLength( true );
	}

	public short getShort( byte[] b, int offset ) {
		return (short) 0;
	}

	public void setShort( byte[] b, int offset, short value ) {
	}

	public int getInt( byte[] b, int offset ) {
		return (int) 0;
	}

	public void setInt( byte[] b, int offset, int value ) {
	}

	public long getLong( byte[] b, int offset ) {
		return (long) 0;
	}

	public void setLong( byte[] b, int offset, long value ) {
	}

	public float getFloat( byte[] b, int offset, int scale ) {
		return (float) 0;
	}

	public void setFloat( byte[] b, int offset, int scale, float value ) {
	}

	public double getDouble( byte[] b, int offset, int scale ) {
		return (double) 0;
	}

	public void setDouble( byte[] b, int offset, int scale, double value ) {
	}

	public String getString( byte[] b, int offset, int scale, Format format ) {
		if ( b.length - 1 < offset ) return "";
		return ByteArray.getString( b, offset, b.length - offset );
	}

	public void setString( byte[] b, int offset, int scale, String value, Format format ) {
	}

	public byte[] getByteArray( byte[] b, int offset ) {
		if ( b.length - 1 < offset ) return new byte[0];
		byte[] btmp = new byte[ b.length - offset ];
		System.arraycopy( b, offset, btmp, 0, btmp.length );
		return btmp;
	}

	public void setByteArray( byte[] b, int offset, byte[] value ) throws ADBCException {
	}

	public void setObject( byte[] b, int offset, int scale, Object value ) {
	}

	public Object getObject( byte[] byte_, int offset_, int scale ) {

		if ( byte_.length - 1 < offset_ ) return null;

		byte[] b = new byte[ byte_.length - offset_ ];

		System.arraycopy( byte_, offset_, b, 0, b.length );

		return b;

	}
	public void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException {
	}

	public Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException {
		return new Date( 0 );
	}

	public void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException {
		//this.setLong( b, offset, value.unscaledValue().longValue() );
	}

	public BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException {
		return BigDecimal.valueOf( 0 );
	}

}
