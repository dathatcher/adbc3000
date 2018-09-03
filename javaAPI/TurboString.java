package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

class TurboString extends AbstractTurboColumn {

	public TurboString ( String name_, short columnId_, String columnType_, int size_ ) {
		super( name_, columnId_, columnType_, size_ );
	}

	public short getShort( byte[] b, int offset ) throws ADBCException {
		return Short.parseShort( ByteArray.getString( b, offset, this.size ) );
	}

	public void setShort( byte[] b, int offset, short value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, String.valueOf( value ) );
	}

	public int getInt( byte[] b, int offset ) throws ADBCException {
		return Integer.parseInt( ByteArray.getString( b, offset, this.size ) );
	}

	public void setInt( byte[] b, int offset, int value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, String.valueOf( value ) );
	}

	public long getLong( byte[] b, int offset ) throws ADBCException {
		return Long.parseLong( ByteArray.getString( b, offset, this.size ) );
	}

	public void setLong( byte[] b, int offset, long value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, String.valueOf( value ) );
	}

	public float getFloat( byte[] b, int offset, int scale ) throws ADBCException {
		//return Float.parseFloat( ByteArray.getString( b, offset, this.size ) );
		return VDelta.parseFloat( ByteArray.getString( b, offset, this.size ) );
	}

	public void setFloat( byte[] b, int offset, int scale, float value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, String.valueOf( value ) );
	}

	public double getDouble( byte[] b, int offset, int scale ) throws ADBCException {
		//return Double.parseDouble( ByteArray.getString( b, offset, this.size ) );
		return VDelta.parseDouble( ByteArray.getString( b, offset, this.size ) );
	}

	public void setDouble( byte[] b, int offset, int scale, double value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, String.valueOf( value ) );
	}

	public String getString( byte[] b, int offset, int scale, Format format ) throws ADBCException {
		if ( format != null )
			try {
				return format.format( ByteArray.getString( b, offset, this.size ) );
			} catch (Exception e) { throw new ADBCException( e, this.name ); }
		return ByteArray.getString( b, offset, this.size );
	}

	public void setString( byte[] b, int offset, int scale, String value, Format format ) throws ADBCException {
		try {
			if ( format == null )
				ByteArray.setString( b, offset, this.size, value );
			else
				ByteArray.setString( b, offset, this.size, (String) format.parseObject( value ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setObject( byte[] b, int offset, int scale, Object value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, (String) value );
	}

	public Object getObject( byte[] b, int offset, int scale ) throws ADBCException {
		return (Object) ByteArray.getString( b, offset, this.size );
	}

	public void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name);
		try {
			//this.setLong( b, offset, format.getLong( value ) );
			ByteArray.setString( b, offset, this.size, format.getString( value ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name); 
		try {
			return format.getDate( ByteArray.getString( b, offset, this.size ) );
			//return format.getDate( this.getLong( b, offset ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException {
		ByteArray.setString( b, offset, this.size, value.toString() );
		//this.setLong( b, offset, value.unscaledValue().longValue() );
	}

	public BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException {
		//return BigDecimal.valueOf( this.getLong( b, offset ), scale );
		return new BigDecimal( ByteArray.getString( b, offset, this.size ) ); 
	}

}
