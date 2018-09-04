package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;

/** An abstract definition of a {@link ComputedColumn} interface.
  * With this abstract definition, a developer can simpley extend
  * this class not be foreced to define all methods of the interface.
  */

public abstract class AbstractComputedColumn implements ComputedColumn {

	/** Returns zero */

	public long getLong( TurboBuffer tb ) throws ADBCException {
		return 0;
	}

	public void setLong( TurboBuffer tb, long value ) throws ADBCException {
	}

	/** Returns zero */

	public double getDouble( TurboBuffer tb ) throws ADBCException {
		return 0;
	}

	public void setDouble( TurboBuffer tb, double value ) throws ADBCException {
	}

	/** Returns "" */

	public String getString( TurboBuffer tb, Format format ) throws ADBCException {
		return "";
	}

	public void setString( TurboBuffer tb, String value ) throws ADBCException {
	}

	/** Returns today's date. */

	public Date getDate( TurboBuffer tb ) throws ADBCException {
		return new Date();
	}

	public void setDate( TurboBuffer tb, Date d ) throws ADBCException {
	}

	/** Returns null */

	public Object getObject( TurboBuffer tb ) throws ADBCException {
		return null;
	}

	public void setObject( TurboBuffer tb, Object obj ) throws ADBCException {
	}

	/** Returns byte array of lenth zero. */

	public byte[] getBytes( TurboBuffer tb ) throws ADBCException {
		return new byte[0];
	}

	/** Returns zero. */

	public BigDecimal getBigDecimal( TurboBuffer tb ) throws ADBCException {
		return new BigDecimal( 0 );
	}

	public void setBigDecimal( TurboBuffer tb, BigDecimal value ) throws ADBCException {
	}

}
