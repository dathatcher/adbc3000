package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

class TurboLong extends AbstractTurboColumn {

	public TurboLong( String name_, short columnId_, String columnType_, int size_ ) {
		super( name_, columnId_, columnType_, size_ );
	}

	public short getShort( byte[] b, int offset ) throws ADBCException {
		return (short) ByteArray.getLong( b, offset );
	}

	public void setShort( byte[] b, int offset, short value ) throws ADBCException {
		ByteArray.setLong( b, offset, (long) value );
	}

	public int getInt( byte[] b, int offset ) throws ADBCException {
		return (int) ByteArray.getLong( b, offset );
	}

	public void setInt( byte[] b, int offset, int value ) throws ADBCException {
		ByteArray.setLong( b, offset, (long) value );
	}

	public long getLong( byte[] b, int offset ) throws ADBCException {
		return ByteArray.getLong( b, offset );
	}

	public void setLong( byte[] b, int offset, long value ) throws ADBCException {
		ByteArray.setLong( b, offset, value );
	}

	public float getFloat( byte[] b, int offset, int scale ) throws ADBCException {
		return (float) ByteArray.getLong( b, offset );
	}

	public void setFloat( byte[] b, int offset, int scale, float value ) throws ADBCException {
		ByteArray.setLong( b, offset, (long) value );
	}

	public double getDouble( byte[] b, int offset, int scale ) throws ADBCException {
		return (double) ByteArray.getLong( b, offset );
	}

	public void setDouble( byte[] b, int offset, int scale, double value ) throws ADBCException {
		ByteArray.setLong( b, offset, (long) value );
	}

	public String getString( byte[] b, int offset, int scale, Format format ) throws ADBCException {
		if ( format != null )
			try {
				return ((NumberFormat)format).format( ByteArray.getLong( b, offset ) );
			} catch (Exception e) { throw new ADBCException( e, this.name ); }
		return Long.toString( ByteArray.getLong( b, offset ) );
	}

	public void setString( byte[] b, int offset, int scale, String value, Format format ) throws ADBCException {
		try {
			if ( format == null ) 
				ByteArray.setLong( b, offset, Long.parseLong(value) );
			else 
				ByteArray.setLong( b, offset, ((NumberFormat)format).parse( value ).longValue() );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setObject( byte[] b, int offset, int scale, Object value ) throws ADBCException {
		if ( String.class.isInstance( value ) ) setString ( b, offset, scale, (String)value, null );
		else ByteArray.setLong( b, offset, ((Number)value).longValue() );
	}

  	public Object getObject( byte[] b, int offset, int scale ) throws ADBCException {
		return (Object) new Long( ByteArray.getLong( b, offset ) );
	}

	public void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name);
		try {
			this.setLong( b, offset, format.getLong( value ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name); 
		try {
			return format.getDate( this.getLong( b, offset ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
		//return null;
	}

	public void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException {
		//this.setLong( b, offset, value.unscaledValue().longValue() );
		this.setLong( b, offset, VDelta.unscaledValue(value).longValue() );
	}

	public BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException {
		return BigDecimal.valueOf( this.getLong( b, offset ), scale );
	}

}
