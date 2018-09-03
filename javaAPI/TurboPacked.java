package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

class TurboPacked extends AbstractTurboColumn {

	public TurboPacked( String name_, short columnId_, String columnType_, int size_ ) {
		super( name_, columnId_, columnType_, size_ );
	}

	public short getShort( byte[] b, int offset ) throws ADBCException {
		return (short) ByteArray.getPacked( b, offset, this.size );
	}

	public void setShort( byte[] b, int offset, short value ) throws ADBCException {
		ByteArray.setPacked( b, offset, this.size, (long) value );
	}

	public int getInt( byte[] b, int offset ) throws ADBCException {
		return (int) ByteArray.getPacked( b, offset, this.size );
	}

	public void setInt( byte[] b, int offset, int value ) throws ADBCException {
		ByteArray.setPacked( b, offset, this.size, (long) value );
	}

	public long getLong( byte[] b, int offset ) throws ADBCException {
		return ByteArray.getPacked( b, offset, this.size );
	}

	public void setLong( byte[] b, int offset, long value ) throws ADBCException {
		ByteArray.setPacked( b, offset, this.size, value );
	}

	public float getFloat( byte[] b, int offset, int scale ) throws ADBCException {
		return (float) ( ByteArray.getPacked( b, offset, this.size ) / Math.pow( 10, scale ) );
	}

	public void setFloat( byte[] b, int offset, int scale, float value ) throws ADBCException {
		ByteArray.setPacked( b, offset, this.size, (long) ( (double)value * Math.pow( 10, scale )) );
	}

	public double getDouble( byte[] b, int offset, int scale ) throws ADBCException {
		return (double) ( ByteArray.getPacked( b, offset, this.size ) / Math.pow( 10, scale ) );
	}

	public void setDouble( byte[] b, int offset, int scale, double value ) throws ADBCException {
		ByteArray.setPacked( b, offset, this.size, (long) ( value * Math.pow( 10, scale ) ) );
	}

	public String getString( byte[] b, int offset, int scale, Format format ) throws ADBCException {
		if ( format != null )
			try {
				if ( scale == 0 ) 
					return ((NumberFormat)format).format( ByteArray.getPacked( b, offset, this.size ) );
				else
					return ((NumberFormat)format).format( (double) (ByteArray.getPacked(b,offset,this.size) / Math.pow(10,scale) ) );
			} catch (Exception e) { throw new ADBCException( e, this.name ); }
		return Long.toString( ByteArray.getPacked( b, offset, this.size ) );
	}

	public void setString( byte[] b, int offset, int scale, String value, Format format ) throws ADBCException {
		try {
			if ( format == null ) 
				if ( scale == 0 ) 
					ByteArray.setPacked( b, offset, this.size, Long.parseLong(value) );
				else
					//ByteArray.setPacked( b, offset, this.size, (long) ( Double.parseDouble(value) * Math.pow( 10, scale ) ) );
					ByteArray.setPacked( b, offset, this.size, (long) ( VDelta.parseDouble(value) * Math.pow( 10, scale ) ) );
			else
				if ( scale == 0 )
					ByteArray.setPacked( b, offset, this.size, ((NumberFormat)format).parse( value ).longValue() ); 
				else 
					ByteArray.setPacked( b, offset, this.size, (long)(((NumberFormat)format).parse( value ).doubleValue() * Math.pow(10,scale))); 
		} catch (Exception e) { throw new ADBCException( e, this.name ); }
	}

	public void setObject( byte[] b, int offset, int scale, Object value ) throws ADBCException {
		if ( String.class.isInstance( value ) ) setString ( b, offset, scale, (String)value, null );
		else {
			if ( scale == 0 )
				ByteArray.setPacked( b, offset, this.size, ((Number)value).longValue() );
			else
				ByteArray.setPacked( b, offset, this.size, ((Number)value).longValue() );
		}
	}

	public Object getObject( byte[] b, int offset, int scale ) throws ADBCException {
		return (Object) new Long( ByteArray.getPacked( b, offset, this.size ) );
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
	}

	public void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException {
		this.setLong( b, offset, VDelta.unscaledValue(value).longValue() );
	}

	public BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException {
		return BigDecimal.valueOf( this.getLong( b, offset ), scale );
	}

}
