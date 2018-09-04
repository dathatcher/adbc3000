package advnetsys.adbc;

import java.util.Vector;
import java.text.Format;
import java.math.*;
import java.io.IOException;

/** The Intrinsic class preforms the I/O between your Java program and 
  * an MPE XL subroutine or function that is located on your HP3000.
  * In essesence, this class is used to define a subroutine/function and
  * the parameters(types/size) of the routine.  Once the class is configured
  * with the routines parameters, the parameter values are set via 
  * updateXXX methods.  The routine is called - the parameter values are
  * passed via the network to the server.  Server performs that actual call and
  * passes returned values back to this class.  The return parameter values can be
  * retrieve via the getXXX methods.<br><br>
  * The following is a list of parameter types and sizes that are used in the
  * addParameter and addArrayParameter methods:<br><br>
  * <block><pre>
  *         Java     Fortran 77        Cobol                C
  * E   2 - float    real                                   float
  * E   4 - double   double precision                       double
  * R   2 - double   real                                   float
  * R   4 - double   double precision                       double
  * I   1 - short    integer*2         comp S9-S9(4)        short int
  * I   2 - int      integer*4         comp S9(5)-S9(9)     int
  * I   4 - long                       comp S9(10)-S9(18)
  * J   1 - short    integer*2         comp S9-S9(4)        short int
  * J   2 - int      integer*4         comp S9(5)-S9(9)     int
  * J   4 - long                       comp S9(10)-S9(18)
  * K   1 - short                                           unsigned short
  * K   2 - int
  * K   4 - long                       
  * P   4 - long                       comp3 S9(3)          char[2]
  * P   8 - long                       comp3 S9(7)          char[4]
  * P   n - long                       comp3 S9(n-1)
  * Z   n - long                       S9(n)
  * U     - String   character                              char
  * X   n - String   character*n       X(n)                 char[n]
  </pre></block>

  * Please note that if the routine to be called is a Fortran 77 routine, the 
  * <b>$FTN3000_16 CHARS ON</b> compiler directive must be applied to the
  * Fortran 77 routine if any of the parameters represents character data.
  */

public class Intrinsic extends Object {

	/** Will specify the parameter ioType be input only. */
	public static int IN_PARM=0;

	/** Will specify the parameter ioType be output only. */
	public static int OUT_PARM=1;

	/** Will specify the parameter ioType be both input and output. */
	public static int INOUT_PARM=2;

	/** Will specify that this class represents a subroutine. */
	public static int SUBROUTINE=0;

	/** Will specify that this class represents a function. */
	public static int FUNCTION=1;

	private Mpe3000 mpe3000;
	private MpeSocket mpeSocket;

	private RowSet rowSet;
	//.private Vector ioTypes;
	private Vector offsets;
	private Vector parms;
	private int p=0;
	private int outDataSize=0;
	private int ccode=0;

	private String name;
	private String xl;
	private int procId=-32767;
	private int intrinsicType=Intrinsic.SUBROUTINE;

	private int[] oarr1;
	private int[] oarr2;

	private byte[] buf;

	class Parm extends Object {
		int numItems;
		String type;
		int offset;
		int size;
		int ioType;
		Parm( int numItems, String type, int offset, int size, int ioType ) {
			this.numItems = numItems;
			this.type = type;
			this.offset = offset;
			this.size = size;
			this.ioType = ioType;
		}
	}

	public Intrinsic( Mpe3000 mpe3000 ) {
		this.mpe3000 = mpe3000;
		rowSet = new RowSet();
		//.ioTypes = new Vector();
		offsets = new Vector();
		parms = new Vector();
	}

	/** Set subroutine/function name.
	  */

	public void setName( String name ) {
		this.name = name;
	}

	/*+-+-+-*/

	/** Set the XL file name.
	  */

	public void setXL( String xl ) {
		this.xl = xl;
		if ( this.xl.length() % 2 != 0 ) this.xl += " ";
	}

	/** Set the type of the intrinsic - Subroutine or Function.  If the Intrinsic is
	  * to be set as a Function, the first parameter is automatically added.  This 
	  * first parameter represents the return value of the function.  It is always of
	  * type I2, which can return values of I1 or I2.  The name of the automatically 
	  * generated parameter is the name that is assigned this intrinsic.
	  * @param intrinsicType Either Intrinsic.SUBROUTINE or Intrinsic.FUNCTION.
	  */

	public void setIntrinsicType( int intrinsicType ) {
		if ( rowSet.getNumColumns() > 0 ) 
		     throw new ADBCRuntimeException("Intrinsic.setIntrinsicType:Cannot set intrinsicType after parameters have been added");
		this.intrinsicType = intrinsicType;
		
		/* If setting the Intrinsic to a function, add the function return value as
		   its first parameter. */

		if ( intrinsicType == Intrinsic.FUNCTION ) {
			if ( name == null || "".equals(name) ) {
				throw new ADBCRuntimeException("Intrinsic.setIntrinsicType:When setting to Intrinsic.FUNCTION, the Intrinsic name must be set");
			}
			addParameter( this.name, "I", 2, Intrinsic.OUT_PARM );
		}
	}

	private TurboColumn addParm( String parameterName, String parameterType, int parameterLength, int ioType ) {

		//.if ( rowSet.getNumColumns() == 30 )
		if ( parms.size() == 30 )
			throw new ADBCRuntimeException("Intrinsic.addParameter:Can only specify a maximum of 30 paramters");
		String parameterTypeUC = parameterType.toUpperCase();

		rowSet.addColumn( parameterName, parameterType, parameterLength );
		//ioTypes.addElement( new Integer( ioType ) );
		
		oarr1 = null;
		buf = null;

		TurboColumn tc=null;
		try { tc = rowSet.getColumn(rowSet.getNumColumns()); } catch (ADBCException e) {}
		return tc;

	}

	/** Add a parameter to this class.
	  * @param parameterName The desired name of the parameter.
	  * @param parameterType The type of the parameter.  See Intrinsic class introduction for a 
	  * list of types.
	  * @param parameterLength The size of the parameter.  Also see Intrinsic class introduction.
	  * @param ioType One of Intrinsic.IN_PARM, Intrinsic.OUT_PARM, or Intrinsic.INOUT_PARM.
	  */

	public final void addParameter( String parameterName, String parameterType, int parameterLength, int ioType ) {
		TurboColumn tc = addParm( parameterName, parameterType, parameterLength, ioType );
		parms.addElement( new Parm( 1, parameterType, tc.getOffset(), tc.getSize(), ioType ) );
	}

	/** Add a parameter to this class with default ioType = Intrinsic.INOUT_PARM.
	  * @param parameterName The desired name of the parameter.
	  * @param parameterType The type of the parameter.  See Intrinsic class introduction for a 
	  * list of types.
	  * @param parameterLength The size of the parameter.  Also see Intrinsic class introduction.
	  */

	public final void addParameter( String parameterName, String parameterType, int parameterLength ) {
		//.int ioType = Intrinsic.INOUT_PARM;
		//.if ( rowSet.getNumColumns() == 0 && intrinsicType == Intrinsic.FUNCTION ) 
			//.ioType = Intrinsic.IN_PARM;
		//.addParameter( parameterName, parameterType, parameterLength, ioType );
		addParameter( parameterName, parameterType, parameterLength, Intrinsic.INOUT_PARM );
	}

	/** Add an array parameter to this class.  This method will add "N" number of subparameters
	  * to this class.  Each subparameter represents an element of the array.  Each 
	  * subparameter is named "<parameterName>_1" through "<parameterName>_N".  Example:  The
	  * name of the array parameter is "ADDRESS" with 5 elements, this method would create
	  * 5 subparameters named "ADDRESS_1" through "ADDRESS_5".  getXXX and updateXXX methods
	  * use these subparameter names to get and set values.
	  * @param numElements The number of elements in the array.
	  * @param parameterName The desired name of the parameter.
	  * @param parameterType The type of the parameter.  See Intrinsic class introduction for a 
	  * list of types.
	  * @param parameterLength The size of the parameter.  Also see Intrinsic class introduction.
	  * @param ioType One of Intrinsic.IN_PARM, Intrinsic.OUT_PARM, or Intrinsic.INOUT_PARM.
	  */

	public final void addArrayParameter( int numElements, String parameterName, String parameterType, int parameterLength, int ioType ) {
		for (int i=1;i<=numElements;i++) {
			TurboColumn tc = addParm( parameterName + "_" + i, parameterType, parameterLength, ioType );
			if ( i == 1 ) {
				parms.addElement( new Parm( numElements, parameterType, tc.getOffset(), tc.getSize(), ioType ) );
			}
		}
	}

	/** Add an array parameter to this class with a default ioType = Intrinsic.INOUT_PARM.
	  * This method will add "N" number of subparameters
	  * to this class.  Each subparameter represents an element of the array.  Each 
	  * subparameter is named "<parameterName>_1" through "<parameterName>_N".  Example:  The
	  * name of the array parameter is "ADDRESS" with 5 elements, this method would create
	  * 5 subparameters named "ADDRESS_1" through "ADDRESS_5".  getXXX and updateXXX methods
	  * use these subparameter names to get and set values.
	  * @param numElements The number of elements in the array.
	  * @param parameterName The desired name of the parameter.
	  * @param parameterType The type of the parameter.  See Intrinsic class introduction for a 
	  * list of types.
	  */

	public final void addArrayParameter( int numElements, String parameterName, String parameterType, int parameterLength ) {
		addArrayParameter( numElements, parameterName, parameterType, parameterLength, Intrinsic.INOUT_PARM );
	}

	private void createOutputOffsets() throws ADBCException {

		if ( oarr1 != null ) return;

		int po=0;

		//.oarr1 = new int[rowSet.getNumColumns()];
		//.oarr2 = new int[rowSet.getNumColumns()];
		oarr1 = new int[parms.size()];
		oarr2 = new int[parms.size()];

		//.for (int i=1;i<=rowSet.getNumColumns();i++) {
		for (int i=1;i<=parms.size();i++) {

			Parm parm = (Parm) parms.elementAt( i-1 );
			//.int ioType = ((Integer)ioTypes.elementAt( i - 1 )).intValue();
			int ioType = parm.ioType;

			//if ( ioType != IN_PARM ) {
			//if ( ioType == OUT_PARM || ioType == INOUT_PARM ) {
			if ( ioType == IN_PARM || ioType == INOUT_PARM ) {

				//.TurboColumn tc = rowSet.getColumn( i );
				//.String columnType = tc.getColumnType();
				//.int size = tc.getSize();
				String columnType = parm.type;
				int size = parm.size;

				//System.out.println("parm " + i + " size = " + size);

				if ( "U".equals(columnType) || "X".equals(columnType) )
					po += ( po % 2 );
				else
					po += ( po % size );  // must make sure numerics are on appropriate boundaries.

				oarr1[i-1] = po;
				//.oarr2[i-1] = po + size - 1;
				oarr2[i-1] = po + (parm.numItems * size) - 1;

				//System.out.println("o1=" + oarr1[i-1] + " o2=" + oarr2[i-1]);

				po += (parm.numItems * size);

			}
		}
		outDataSize = po - 1;

		/* Now calculate offsets for OUT_PARM columns. */

		//.for (int i=1;i<=rowSet.getNumColumns();i++) {
		for (int i=1;i<=parms.size();i++) {

			Parm parm = (Parm) parms.elementAt( i-1 );
			//.int ioType = ((Integer)ioTypes.elementAt( i - 1 )).intValue();
			int ioType = parm.ioType;

			if ( ioType == OUT_PARM ) {

				//.TurboColumn tc = rowSet.getColumn( i );
				//.String columnType = tc.getColumnType();
				//.int size = tc.getSize();
				String columnType = parm.type;
				int size = parm.size;

				if ( !( "U".equals(columnType) || "X".equals(columnType) ) )
					po += ( po % 2 );
				else
					po += ( po % size );  // must make sure numerics are on appropriate boundaries.

				oarr1[i-1] = po;
				//.oarr2[i-1] = po + size - 1;
				oarr2[i-1] = po + (parm.numItems * size) - 1;

				po += (parm.numItems * size);
			}
		}
		//outDataSize = po - 1;
	}

	/** This method performs the "call" to the subroutine.  It initiates the
	  * transfer of all the IN_PARM and INOUT_PARM parameter values over the 
	  * network to the server.  The server then performs the XL routine call.
	  * The server then sends back all INOUT_PARM and OUT_PARM parameters
	  * back to this class.  These values are not store as "return" values
	  * inside this class.  Use the getXXX methods to retrieve their values.
	  */

	public void call() throws ADBCException {

		if ( buf == null ) buf = new byte[rowSet.getLength()];

		createOutputOffsets();

		//System.out.println("outDataSize=" + outDataSize);

		//.int baseOffset = 16 + (rowSet.getNumColumns() * 6);
		//.int nameOffset = 16 + (rowSet.getNumColumns() * 6);
		int baseOffset = 16 + (parms.size() * 6);
		int nameOffset = 16 + (parms.size() * 6);

		if ( procId == -32767 ) baseOffset += name.length() + xl.length();

		//System.out.println("orig1 baseOffset=" + baseOffset);
		baseOffset += 3;
		baseOffset += 8 - ( baseOffset % 8 );
		//baseOffset -= 2;

		//System.out.println("orig baseOffset=" + baseOffset);

		//.int outSize = baseOffset + outDataSize - 1;
		int outSize = baseOffset + outDataSize + 1;

		byte[] bOut = new byte[outSize + 2];

		ByteArray.setShort( bOut, 0, (short) outSize );
		ByteArray.setString( bOut, 2, 2, "PC" );
		ByteArray.setInt( bOut, 4, procId );

		ByteArray.setShort( bOut, 8, (short) 0 );
		ByteArray.setShort( bOut, 10, (short) 0 );

		if ( procId == -32767 ) {
			ByteArray.setShort( bOut, 8, (short) name.length() );
			ByteArray.setShort( bOut, 10, (short) xl.length() );
			ByteArray.setString( bOut, nameOffset, name.length(), name );
			ByteArray.setString( bOut, nameOffset + name.length(), xl.length(), xl );
		}

		ByteArray.setShort( bOut, 12, (short) intrinsicType );
		//.ByteArray.setShort( bOut, 14, (short) rowSet.getNumColumns() );
		ByteArray.setShort( bOut, 14, (short) parms.size() );

		//System.out.println("bOut.length=" + bOut.length );
		//System.out.println("numInputParms=" + numInputParms);
		//System.out.println("numColumns=" + rowSet.getNumColumns());

		int p = 16;
		//.for (int i=1;i<=rowSet.getNumColumns();i++) {
		for (int i=1;i<=parms.size();i++) {
		//for (int i=1;i<=numInputParms;i++) {

			//System.out.println("p=" + p);
			//.TurboColumn tc = rowSet.getColumn( i );
			//.int size = tc.getSize();
			//.int offset = tc.getOffset()-1;
			//.int ioType = ((Integer)ioTypes.elementAt( i - 1 )).intValue();
			Parm parm = (Parm) parms.elementAt( i-1 );
			int size = parm.numItems * parm.size;
			int offset = parm.offset - 1;
			int ioType = parm.ioType;

			//System.out.println("i="+i+ "o1="+(oarr1[i-1]+baseOffset) +
			//                   " o2="+(oarr2[i-1]+baseOffset));
			//.ByteArray.setShort( bOut, p, (short) (oarr1[i-1] + baseOffset - 1) );
			//.ByteArray.setShort( bOut, p+2, (short) (oarr2[i-1] + baseOffset - 1) );
			ByteArray.setShort( bOut, p, (short) (oarr1[i-1] + baseOffset + 1) );
			ByteArray.setShort( bOut, p+2, (short) (oarr2[i-1] + baseOffset + 1) );
			ByteArray.setShort( bOut, p+4, (short) ioType );
			p += 6;

			/* Copy column's portion of the buffer to bOut. */

			//System.out.println("baseOffset=" + baseOffset);
			//System.out.println("oarr1[i-1]=" + oarr1[i-1]);
			//System.out.println("size=" + size);
			//System.out.println("oarr1[i-1]+baseOffset=" + (oarr1[i-1] + baseOffset) );

			//if ( i <= numInputParms ) System.arraycopy( buf, offset, bOut, oarr1[i-1] + baseOffset, size );
			//.if ( ioType != OUT_PARM ) System.arraycopy( buf, offset, bOut, oarr1[i-1] + baseOffset, size );
			if ( ioType != OUT_PARM ) System.arraycopy( buf, offset, bOut, oarr1[i-1] + baseOffset + 2, size );

		}

		int recordLength;
		byte[] bIn;

		//System.out.println("Intrinsic.call::b4 writing to socket...");

		try {
			mpeSocket = mpe3000.checkOutSocket();
			mpeSocket.out.write( bOut );
			recordLength = mpeSocket.in.readShort();
			bIn = new byte[recordLength];
			mpeSocket.in.readFully( bIn, 0, recordLength );
			mpe3000.checkInSocket();
		} catch (IOException e) {throw new ADBCException("Intrinsic.call:Comm error");}

		int errno = (int) ByteArray.getShort( bIn, 0 );

		if ( errno == 0 && recordLength > 2 ) {

			procId = ByteArray.getInt( bIn, 2 ); /* Get the procId */

			ccode = (int) ByteArray.getShort( bIn, 6 ); /* Get ccode() */

			p = 8;

			//.for (int i=1;i<=rowSet.getNumColumns();i++) {
			for (int i=1;i<=parms.size();i++) {

				//.TurboColumn tc = rowSet.getColumn( i );
				//.int size = tc.getSize();
				//.int offset = tc.getOffset()-1;
				//.int ioType = ((Integer)ioTypes.elementAt( i - 1 )).intValue();
				Parm parm = (Parm) parms.elementAt( i-1 );
				int size = parm.numItems * parm.size;
				int offset = parm.offset - 1;
				int ioType = parm.ioType;

				//if ( ioType != OUT_PARM ) {
				if ( ioType != IN_PARM ) {

					System.arraycopy( bIn, p, buf, offset, size );
					p += size;

				}
			}
		} else { // errno != 0
			throw new ADBCException("Intrinsic.call:Illegal Intrinsic:" + this.name );
		}
	}

	public int getCcode() {
		return ccode;
	}

	private TurboColumn getColumn( int parmNo ) throws ADBCException {

		//System.out.println("rowset.getlength=" + rowSet.getLength() );
		if ( buf == null ) buf = new byte[rowSet.getLength()];

		if ( parmNo <= 0 || parmNo > rowSet.getNumColumns() ) 
			throw new ADBCException("Intrinsic.getXXX/updateXXX:Invalid parmNo",this.name,String.valueOf(parmNo),43003);
		return rowSet.getColumn( parmNo );
	
	}

	private TurboColumn getColumn( String parmName ) throws ADBCException {

		if ( buf == null ) buf = new byte[rowSet.getLength()];

		return rowSet.getColumn( parmName.toLowerCase() );
	}

	public final Object getObject( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getObject( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as an Object.
	  * @param parmName The parameter name.
	  */

	public final Object getObject( String parmName ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		return tc.getObject( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a String.
	  * @param parmNo The parameter number.
	  */
	
	public final String getString( int parmNo ) throws ADBCException{
		TurboColumn tc = getColumn( parmNo );
		return tc.getString( buf, tc.getOffset()-1, 0, null );
	}

	/** Return the parameter value as a String.
	  * @param parmName The parameter name.
	  */

	public final String getString( String parmName ) throws ADBCException{
		TurboColumn tc = getColumn( parmName );
		return tc.getString( buf, tc.getOffset()-1, 0, null );
	}

	/** Return the parameter value as a String with the specified format applied.
	  * @param parmNo The parameter number.
	  * @param format a format object.
	  */

	public final String getString( int parmNo, Format format ) throws ADBCException{
		TurboColumn tc = getColumn( parmNo );
		return tc.getString( buf, tc.getOffset()-1, 0, format );
	}

	/** Return the parameter value as a String with the specified format applied.
	  * @param parmName The parameter name.
	  * @param format A format object.
	  */

	public final String getString( String parmName, Format format ) throws ADBCException{
		TurboColumn tc = getColumn( parmName );
		return tc.getString( buf, tc.getOffset()-1, 0, format );

	}

	/** Return the parameter value as a Date object.  An ADBCDateFormat object
	  * must be applied to this column using the setFormat() method.
	  * @param parmNo The parameter number.
	  * @see ADBCDateFormat
	  * @see #setFormat
	  */

	//public final Date getDate( int parmNo ) throws ADBCException {
	//	TurboColumn tc = getColumn( parmNo );
	//	return tc.getDate( buf, tc.getOffset()-1, (ADBCDateFormat) columnFormat.get( obj ) );
	//}

	/** Return the parameter value as a Date object.  An ADBCDateFormat object
	  * must be applied to this column using the setFormat() method.
	  * @param parmName The parameter name.
	  * @see ADBCDateFormat
	  * @see #setFormat
	  */

	//public final Date getDate( String parmName ) throws ADBCException { 
	//	TurboColumn tc = getColumn( parmName );
	//	return tc.getDate( buf, tc.getOffset()-1, (ADBCDateFormat) columnFormat.get( obj ) );
	//}

	/** Return the parameter value as an int.
	  * @param parmNo The parameter number.
	  */

	public final int getInt( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getInt( buf, tc.getOffset()-1 );

	}

	/** Return the parameter value as an int.
	  * @param parmName The parameter name.
	  */

	public final int getInt( String parmName ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		return tc.getInt( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a long.
	  * @param parmNo The parameter number.
	  */

	public final long getLong( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getLong( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a long.
	  * @param parmName The parameter name.
	  */

	public final long getLong( String parmName ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		return tc.getLong( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a short.
	  * @param parmNo The parameter number.
	  */

	public final short getShort( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getShort( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a short.
	  * @param parmName The parameter name.
	  */

	public final short getShort( String parmName ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		return tc.getShort( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a double.
	  * @param parmNo The parameter number.
	  */

	public final double getDouble( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getDouble( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a double.
	  * @param parmName The parameter name.
	  */

	public final double getDouble( String parmName ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		return tc.getDouble( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a float.
	  * @param parmNo The parameter number.
	  */

	public final float getFloat( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getFloat( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a float.
	  * @param parmName The parameter name.
	  */

	public final float getFloat( String parmName ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		return tc.getFloat( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a BigDecimal.
	  * @param parmName The parameter name.
	  */

	public final BigDecimal getBigDecimal( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getBigDecimal( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a BigDecimal.
	  * @param parmName The parameter name.
	  */

	public final BigDecimal getBigDecimal( String parmName ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		return tc.getBigDecimal( buf, tc.getOffset()-1, 0 );
	}

	/** Return the parameter value as a BigDecimal applying the specified scale.
	  * @param parmName The parameter name.
	  * @param scale The scale of the BigDecimal.
	  */

	public final BigDecimal getBigDecimal( int parmNo, int scale ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getBigDecimal( buf, tc.getOffset()-1, scale );
	}

	/** Return the parameter value as a BigDecimal applying the specified scale.
	  * @param parmName The parameter name.
	  * @param scale The scale of the BigDecimal.
	  */

	public final BigDecimal getBigDecimal( String parmName, int scale ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		return tc.getBigDecimal( buf, tc.getOffset()-1, scale );
	}

	/** Return the parameter value as a byte array.  This byte array is in the format
	  * represented on the HP3000.  Meaning, if the column contains a packed type,
	  * this byte array will contain the packed representation of the number.
	  * @param parmNo The parameter number.
	  */

	public final byte[] getBytes( int parmNo ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		return tc.getByteArray( buf, tc.getOffset()-1 );
	}

	/** Return the parameter value as a byte array.  This byte array is in the format
	  * represented on the HP3000.  Meaning, if the column contains a packed type,
	  * this byte array will contain the packed representation of the number.
	  * @param parmName The parameter name.
	  */

	public final byte[] getBytes( String parmName ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		return tc.getByteArray( buf, tc.getOffset()-1 );
	}

	public final void updateObject( int parmNo, Object value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setObject( buf, tc.getOffset()-1, 0, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateObject( String parmName, Object value ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		tc.setObject( buf, tc.getOffset()-1, 0, value );
	}
	
	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateString( int parmNo, String value ) throws ADBCException{
		TurboColumn tc = getColumn( parmNo );
		tc.setString( buf, tc.getOffset()-1, 0, value, null ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateString( String parmName, String value ) throws ADBCException{
		TurboColumn tc = getColumn( parmName );
		tc.setString( buf, tc.getOffset()-1, 0, value, null );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	//public final void updateDate( int parmNo, Date value ) throws ADBCException{
	//	TurboColumn tc = getColumn( parmNo );
	//	tc.setDate( buf, tc.getOffset()-1, value, (ADBCDateFormat) columnFormat.get( obj ) ); 
	//}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	//public final void updateDate( String parmName, Date value ) throws ADBCException{
	//	TurboColumn tc = getColumn( parmName );
	//	tc.setDate( buf, tc.getOffset()-1, value, (ADBCDateFormat) columnFormat.get( obj ) );
	//}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateInt( int parmNo, int value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setInt( buf, tc.getOffset()-1, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateInt( String parmName, int value ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		tc.setInt( buf, tc.getOffset()-1, value );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateLong( int parmNo, long value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setLong( buf, tc.getOffset()-1, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateLong( String parmName, long value ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		tc.setLong( buf, tc.getOffset()-1, value );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateShort( int parmNo, short value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setShort( buf, tc.getOffset()-1, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateShort( String parmName, short value ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		tc.setShort( buf, tc.getOffset()-1, value );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateDouble( int parmNo, double value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setDouble( buf, tc.getOffset()-1, 0, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateDouble( String parmName, double value ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		tc.setDouble( buf, tc.getOffset()-1, 0, value );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateFloat( int parmNo, float value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setFloat( buf, tc.getOffset()-1, 0, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateFloat( String parmName, float value ) throws ADBCException { 
		TurboColumn tc = getColumn( parmName );
		tc.setFloat( buf, tc.getOffset()-1, 0, value );
	}

	/** Update the parameter with the data value.
	  * @param parmNo The parameter number.
	  * @param value The data value.
	  */

	public final void updateBigDecimal( int parmNo, BigDecimal value ) throws ADBCException {
		TurboColumn tc = getColumn( parmNo );
		tc.setBigDecimal( buf, tc.getOffset()-1, 0, value ); 
	}

	/** Update the parameter with the data value.
	  * @param parmName The parameter name.
	  * @param value The data value.
	  */

	public final void updateBigDecimal( String parmName, BigDecimal value ) throws ADBCException {
		TurboColumn tc = getColumn( parmName );
		tc.setBigDecimal( buf, tc.getOffset()-1, 0, value );
	}
}
