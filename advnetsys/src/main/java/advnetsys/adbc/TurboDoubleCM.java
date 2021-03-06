package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

	class TurboDoubleCM extends AbstractTurboColumn {

	public TurboDoubleCM( String name_, short columnId_, String columnType_, int size_ ) {
		super( name_, columnId_, columnType_, size_ );
	}

	public short getShort( byte[] b, int offset ) throws ADBCException {
		return (short) ByteArray.getDoubleCM( b, offset );
	}

	public void setShort( byte[] b, int offset, short value ) throws ADBCException {
		ByteArray.setDoubleCM( b, offset, (double) value );
	}

	public int getInt( byte[] b, int offset ) throws ADBCException {
		return (int) ByteArray.getDoubleCM( b, offset );
	}

	public void setInt( byte[] b, int offset, int value ) throws ADBCException {
		ByteArray.setDoubleCM( b, offset, (double) value );
	}

	public long getLong( byte[] b, int offset ) throws ADBCException {
		return (long) ByteArray.getDoubleCM( b, offset );
	}

	public void setLong( byte[] b, int offset, long value ) throws ADBCException {
		ByteArray.setDoubleCM( b, offset, (double) value );
	}

	public float getFloat( byte[] b, int offset, int scale ) throws ADBCException {
		return (float) ByteArray.getDoubleCM( b, offset );
	}

	public void setFloat( byte[] b, int offset, int scale, float value ) throws ADBCException {
		ByteArray.setDoubleCM( b, offset, (double) value );
	}

	public double getDouble( byte[] b, int offset, int scale ) throws ADBCException {
		return ByteArray.getDoubleCM( b, offset );
	}

	public void setDouble( byte[] b, int offset, int scale, double value ) throws ADBCException {
		ByteArray.setDoubleCM( b, offset, value );
	}

	public String getString( byte[] b, int offset, int scale, Format format ) throws ADBCException {
		if ( format != null )
			try {
				return ((NumberFormat)format).format( ByteArray.getDoubleCM( b, offset ) );
			} catch (Exception e) { throw new ADBCException( e, this.name ); }
		return Double.toString( ByteArray.getDoubleCM( b, offset ) );
	}

	public void setString( byte[] b, int offset, int scale, String value, Format format ) throws ADBCException {
		try {
			if ( format == null ) 
				//ByteArray.setDoubleCM( b, offset, Double.parseDouble(value) );
				ByteArray.setDoubleCM( b, offset, VDelta.parseDouble(value) );
			else
				ByteArray.setDoubleCM( b, offset, ((NumberFormat)format).parse( value ).doubleValue() ); 
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setObject( byte[] b, int offset, int scale, Object value ) throws ADBCException {
		if ( String.class.isInstance( value ) ) setString ( b, offset, scale, (String)value, null );
		else ByteArray.setDoubleCM( b, offset, ((Number)value).doubleValue() );
	}
 
	public Object getObject( byte[] b, int offset, int scale ) throws ADBCException {

		return (Object) new Double( ByteArray.getDoubleCM( b, offset ) );

	}

	public void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name);
		try {
			this.setDouble( b, offset, 0, format.getDouble( value ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException {
		if ( format == null ) throw new ADBCException("ADBCDateFormat must be set on column " + this.name); 
		try {
			return format.getDate( this.getDouble( b, offset, 0 ) );
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException {
		this.setDouble( b, offset, scale, value.doubleValue() );
	}

	public BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException {
		return new BigDecimal( this.getDouble( b, offset, scale ) );
	}

}
