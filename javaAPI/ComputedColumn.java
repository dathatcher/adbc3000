package advnetsys.adbc;

import java.text.*;
import java.util.Date;
import java.math.BigDecimal;
/** A ComputedColumn is a user defined computation that a TurboBuffer
  * encapsulates and allows to appear as just another column of a TurboBuffer.
  * A ComputedColumn extends the capability of a TurboBuffer.  A number of 
  * methods have been provided in this interface that the programmer must 
  * overload in order to provide the computation.  A typical use of a 
  * ComputedColumn may be to report a product margin that is a calculation of
  * subtracting a products cost from it's price.  Another use might be to 
  * create an HTML string that represents a row in a table with each column
  * in the TurboBuffer representing an HTML table cell.  The syntax for such
  * a scenario is as follows:<BR>
  * <P><BLOCKQUOTE><PRE>
  * public class HTMLTableRow implements ComputedColumn {
  *
  *     public String getString( TurboBuffer tb, Format format ) throws ADBCException { 
  *         StringBuffer buf = new StringBuffer();
  *     
  *         for (int i=1;i<=tb.numColumns();i++) {
  *             buf.append("&lt;TD&gt;");
  *             buf.append( tb.getString(i));
  *             buf.append("&lt;/TD&gt;");
  *         }
  *         return new String( buf );
  *     }
  * }
  * </PRE></BLOCKQUOTE>
  *
  * Note that this ComputedColumn is generic and can be applied to any 
  * TurboBuffer(i.e it is reusable).
  * <P>
  * It is also highly advised that when a computed column represents a 
  * non-string data type to also overload the getString() method to 
  * return a string representation of the compuation.
  *
  * @see advnetsys.adbc.TurboBuffer#addComputedColumn
  * @see advnetsys.adbc.AbstractComputedColumn
  */

public interface ComputedColumn {

	/** This method is called by TurboBuffers getShort(), getInt(), and
	  * getLong() methods.  The TurboBuffer will apply a
	  * cast for the appropriate return types.  If the computation is 
	  * to return one of the types, short, int, or long, overload this
	  * method.
	  */

	public long getLong( TurboBuffer tb ) throws ADBCException;

	/** This method is called by a TurboBuffers updateShort(), updateInt(),
	  * and updateLong() methods.  Overload this method if the value
	  * being set is one of types short, int, or long.
	  */

	public void setLong( TurboBuffer tb, long value ) throws ADBCException;

	/** This method is called by TurboBuffers getFloat() and getDouble()
	  * methods.  The TurboBuffer will apply a cast for the appropriate
	  * return types.  If the computation is to return one of the types
	  * float or double, overload this method.
	  */

	public double getDouble( TurboBuffer tb ) throws ADBCException;

	/** This method is called by TurboBuffers updateFloat(), and 
	  * updateDouble() methods.  Overload this method if the value
	  * being set is one of the types float or double.
	  */

	public void setDouble( TurboBuffer tb, double value ) throws ADBCException;

	/** This method is called by TurboBuffers getString() method.  This
	  * method should be overloaded to give a string representation of the
	  * computation.  The computation may be a string computation also.
	  */

	public String getString( TurboBuffer tb, Format format ) throws ADBCException;

	/** This method is called by TurboBuffers updateString() method.
	  * Overload this method to handle the computation value being passed
	  * as a string.
	  */

	public void setString( TurboBuffer tb, String value ) throws ADBCException;

	/** This method is called by TurboBuffers getDate() method.  This method
	  * should be overloaded to return date representation of a computation.
	  */

	public Date getDate( TurboBuffer tb ) throws ADBCException;

	/** This method is called by TurboBuffers updateDate() method.
	  * Overload this method to handle the computation value being passed
	  * as a date.
	  */

	public void setDate( TurboBuffer tb, Date d ) throws ADBCException;

	/** This method is called by Turbobuffers updateObject() method.
	  * Overload this method to handle the computation value being passed
	  * as an object.
	  */

	public void setObject( TurboBuffer tb, Object obj ) throws ADBCException;

	/** This method is called by TurboBuffers getObject() method.  This method
	  * should be overloaded to return the Object representation of a computation.
	  */

	public Object getObject( TurboBuffer tb ) throws ADBCException;

	/** This method is called by TurboBuffers getBytes() method.  This method
	  * should be overloaded to return a byte array representation of a 
	  * computation.
	  */

	public byte[] getBytes( TurboBuffer tb ) throws ADBCException;

	/** This method is called by TurboBuffers getBigDecimal() method.  This method
	  * should be overloaded to return the BigDecimal representation of a computation.
	  */

	public BigDecimal getBigDecimal( TurboBuffer tb ) throws ADBCException;

	/** This method is called by TurboBuffers updateBigDecimal method.
	  * Overload this method to handle the computation value being passed
	  * as a BigDecimal.
	  */

	public void setBigDecimal( TurboBuffer tb, BigDecimal value ) throws ADBCException;

}
