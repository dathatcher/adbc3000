package advnetsys.adbc;

import java.text.Format;
import java.util.Date;
import java.math.BigDecimal;

interface TurboColumn {

	public void setKeyColumn( boolean isKeyColumn ) ;

	public boolean isKeyColumn();

	public void setVariableLength( boolean enabled_ ) ;

	public boolean isVariableLength(); 

	public void setOffset( short offset_ );
	public short getSize();
	public short getOffset();
	public String getName();
	public short getColumnId();
	public String getColumnType();

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
	public abstract byte[] getByteArray( byte[] b, int offset ) throws ADBCException;
	public abstract void setByteArray( byte[] b, int offset, byte[] value ) throws ADBCException;
	public abstract void setDate( byte[] b, int offset, Date value, ADBCDateFormat format ) throws ADBCException;
	public abstract Date getDate( byte[] b, int offset, ADBCDateFormat format ) throws ADBCException;
	public abstract void setBigDecimal( byte[] b, int offset, int scale, BigDecimal value ) throws ADBCException;
	public abstract BigDecimal getBigDecimal( byte[] b, int offset, int scale ) throws ADBCException;

	public abstract int compare( byte[] b1, byte[] b2, int offset );	
}
