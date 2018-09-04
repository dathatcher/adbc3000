package advnetsys.adbc;

import java.text.*;
import java.util.*;

/** ADBCDateFormat converts Turbo Image string and numeric fields
  * into a java.util.Date object and vice versa.  Unlike the 
  * java.text.SimpleDateFormat class, this class actually contains
  * two formats.  An internal format and a display format.  The
  * internal format describes the date format of the data field
  * in the database.  The display format describes the format
  * of the date that is used throughout the developer's application.
  * <BR><BR>
  * If the "date" field within the database is of a Julian type where
  * the field represents the number of time units since a given
  * point in time(the epoch), then instead of specifying an internal
  * format, specify the epoch.  The time unit must be specified({@link #setDateUnit})
  * as one of {@link #DAY}, {@link #HOUR}, {@link #MINUTE}, {@link #SECOND},
  * {@link #TENTHSECOND}, {@link #HUNDRETHSECOND}, or {@link #MILLISECOND}.
  * <BR><BR>
  * When ADBCDateFormat is used in conjunction with a {@link TurboBuffer},
  * the developer need not be concerned about the converting, parsing and formatting
  * methods of this class.  The {@link TurboBuffer} takes care of this
  * functionality internally.  Once the format has been set in the
  * TurboBuffer({@link TurboBuffer#setFormat}), the developer uses
  * the normal {@link TurboBuffer#getDate} and {@link TurboBuffer#updateDate}
  * methods to retrieve and update the field.  Example:
  * <BR><BR>
  * DATE1 is the Turbo Image dataset field that is of type I2.
  * The date is stored in this field in the format YYYYMMDD.
  * <BLOCKQUOTE><PRE>
  * TurboBuffer tb = new TurboBuffer ... ;
  *
  * ADBCDateFormat dateFormat = new ADBCDateFormat( "yyyyMMdd", "MM.dd.yyyy" );
  *
  * tb.setColumns("@");
  * tb.setFormat( "DATE1", dateFormat );
  *
  * tb.initiateRead();
  *
  * if ( tb.next() ) {
  *
  *     Date d1 = tb.getDate( "DATE1" );       // Returns the date as a Java Date Object
  *
  *     tb.updateDate( "DATE1", new Date() );  // Sets the dataset date field to todays date.
  *
  *     tb.updateRow();                        // Apply the change to the dataset.
  *
  * }
  * </PRE></BLOCKQUOTE>
  * The following code is also allowed:
  * <BLOCKQUOTE><PRE>
  * if ( tb.next() ) {
  *
  *     String dateString = tb.getString( "DATE1" );  // Return the date as a string in
  *                                                   // the format "MM.dd.yyyy".
  *
  *     tb.updateString( "DATE1", "12.31.2000" );     // Sets the dataset date field to
  *                                                   // I2 value 20001231.
  *     tb.updateRow();
  *
  * }
  * </PRE></BLOCKQUOTE>
  * <BR>
  * The internal and display format must adhere to the formatting used by the 
  * java.text.SimpleDateFormat class.
  */

public class ADBCDateFormat extends NumberFormat {

	/** Date Unit representing the number of milliseconds in one day. */
	//public static long DAY = 86400000;
	public static int DAY = Calendar.DATE;

	/** Date Unit representing the number of milliseconds in one hour. */
	//public static long HOUR = 3600000;
	public static int HOUR = Calendar.HOUR;

	/** Date Unit representing the nubmer of milliseconds in one minute. */
	//public static long MINUTE = 60000;
	public static int MINUTE = Calendar.MINUTE;

	/** Date Unit representing the nubmer of milliseconds in one seond. */
	//public static long SECOND = 1000;
	public static int SECOND = Calendar.SECOND;

	/** Date Unit representing the number of milliseconds in a tenth of a second. */
	//public static long TENTHSECOND = 100;

	/** Date Unit representing the number of milliseconds in a hundreth of a second. */
	//public static long HUNDRETHSECOND = 10;

	/** Date Unit representing a millisecond. */
	//public static long MILLISECOND = 1;
	public static int MILLISECOND = Calendar.MILLISECOND;
	
	private SimpleDateFormat internalDateFormat;
	private SimpleDateFormat displayDateFormat;
	private Date epoch;
	private long epochDelta=0;
	private boolean isJulian=false;
	private DecimalFormat decFormat;
	//private long dateUnit = DAY;
	private int dateUnit = DAY;

	public ADBCDateFormat() {
	}

	/** ADBCDateFormat constructor.
	  * @param internalFormat The date format of the data field in the dataset.
	  * @param displayFormat The date format used in the application.
	  */

	public ADBCDateFormat( String internalFormat, String displayFormat ) {

		this.setInternalFormat( internalFormat );
		this.setDisplayFormat( displayFormat );
	}

	/** ADBCDateFormat constructor.
	  * @param epoch The epoch as a java.util.Date.
	  * @param displayFormat The date format used in the application.
	  */

	public ADBCDateFormat( Date epoch, String displayFormat ) {

		this.setEpoch( epoch );
		this.setDisplayFormat( displayFormat );
	}

	/** Set the date format of the data field in the dataset.
	  * @param internalFormat The date format of the data field in the dataset.
	  */

	public final void setInternalFormat( String internalFormat ) {

		internalDateFormat = new SimpleDateFormat( internalFormat );
		if ( displayDateFormat == null ) displayDateFormat = internalDateFormat;

		String zeroes = "";
		for (int i=1;i<=internalFormat.length();i++)
			zeroes += "0";
		decFormat = new DecimalFormat( zeroes );

	}

	/** Set the date format to be used withing the Java application.
	  * @param displayFormat the date format used in the application.
	  */

	public final void setDisplayFormat ( String displayFormat ) {

		displayDateFormat = new SimpleDateFormat( displayFormat );

	}

	/** For Julian type dates, set the epoch.
	  * @param epoch The epoch as a java.util.Date.
	  */

	public final void setEpoch( Date epoch ) {

		this.epoch = epoch;
		this.isJulian = true;
		this.epochDelta = epoch.getTime() - new Date( 0 ).getTime();

	}

	/** For Julian type date, set the epoch.
	  * @param epoch A String representation of the epoch date.
	  * @param epochFormat The String date format of the epoch.
	  */

	public final void setEpoch( String epoch, String epochFormat ) {

		SimpleDateFormat sdf = new SimpleDateFormat( epochFormat );
		try {
			this.setEpoch( sdf.parse( epoch ) );
		} catch (ParseException e) { e.printStackTrace();}
	}

	/** Return the epoch.
	  */

	public final Date getEpoch() {
		return epoch;
	}

	/** For Julian type date, set the date unit that represents 
	  * one incremental value of the Julian date.
	  * @param dateUnit A date unit static variable defined in this class.
	  */

	public final void setDateUnit( int dateUnit ) {

		//if ( ! ( dateUnit == DAY || dateUnit == HOUR || dateUnit == MINUTE ||
		//         dateUnit == SECOND || dateUnit == TENTHSECOND ||
		//         dateUnit == HUNDRETHSECOND || dateUnit == MILLISECOND ) 
		//   ) throw new ADBCException( "Illegal date unit set." );

		this.dateUnit = dateUnit;
	}

	private Date computeJulian( long number ) {

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(epoch);
		//calendar.add(Calendar.DATE,(int)number);
		calendar.add(dateUnit,(int)number);
		return calendar.getTime();
	
	}

	/** Converts a long into a date.  It is assumed that the long number's format
	  * is the set internal format of this object.  If Julian based, it is the 
	  * number of date intervals since the set epoch.  A TurboBuffer will internally call 
	  * this method when the dataset column is of type I, J, or K.
	  * <BR>Note: If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this
	  * method redefined to convert the passed date to a java.util.Date.
	  * @param number The passed date.
	  * @return The converted date.
	  */

	public Date getDate( long number ) throws ParseException {

		if ( isJulian ) return computeJulian(number);
			//System.out.println("number=" + number);
			//System.out.println("dateUnit=" + dateUnit);
			//System.out.println("epochDelta=" + epochDelta);
			//long l = number * this.dateUnit + epochDelta;
			//System.out.println("l=" + l);
			//return new Date( l );
		//}

		Date dt = null;
		//try {
			dt = internalDateFormat.parse( decFormat.format( number ) );
			//dt = internalDateFormat.parse( Long.toString( number ) );
		//} catch (ParseException e) {e.printStackTrace();}
		return dt;
	}

	/** Converts a double into a date.  It is assumed that the double number's format
	  * is the set internal format of this object.  If Julian based, it is the
	  * number of the date intervals since the set epoch.  A TurboBuffer will internally call
	  * this method when the dataset column is of type E or R.
	  * <BR>Note: If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this
	  * method redefined to convert the passed date to a java.util.Date.
	  * @param number The passed date.
	  * @return The converted date.
	  */

	public Date getDate( double number ) throws ParseException {

		//if ( isJulian ) return new Date( (long) number * this.dateUnit + epochDelta );
		if ( isJulian ) return computeJulian((long)number);

		Date dt = null;
		//try {
			dt = internalDateFormat.parse( decFormat.format( number ) );
			//return internalDateFormat.parse( Double.toString( number ) );
		//} catch (ParseException e) {e.printStackTrace();}
		return dt;
	}

	/** Converts a String into a date.  It is assumed that the String's format
	  * is the set interanal format of this object.  A TurboBuffer will internally call
	  * this method when the dataset column is of type U or X.
	  * <BR>Note: If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this
	  * method redefined to convert the passed date to a java.util.Date.
	  * @param st The passed date.
	  * @return The converted date.
	  */

	public Date getDate( String st ) throws ParseException {

		//if ( isJulian ) return new Date( Long.parseLong( st ) * this.dateUnit + epochDelta );
		if ( isJulian ) return computeJulian(Integer.parseInt(st));

		Date dt = null;
		//try {
			dt = internalDateFormat.parse( st );
		//} catch (ParseException e) {e.printStackTrace();}
		return dt;
	}

    public final StringBuffer format( long number, StringBuffer toAppendTo, FieldPosition pos ) {

		StringBuffer s = null;
		try {
			if ( displayDateFormat == null ) throw new ADBCRuntimeException("ADBCDateFormat:Display format is null");
			s = displayDateFormat.format( this.getDate( number ), toAppendTo, pos ); 
		} catch (ParseException e) { throw new IllegalArgumentException( e.getMessage() ); }
		return s;
	}
	
    public final StringBuffer format( double number, StringBuffer toAppendTo, FieldPosition pos ) { 
    
		StringBuffer s = null;
		try {
			if ( displayDateFormat == null ) throw new ADBCRuntimeException("ADBCDateFormat:Display format is null");
			s = displayDateFormat.format( this.getDate( number ), toAppendTo, pos  );
		} catch (ParseException e) { throw new IllegalArgumentException( e.getMessage() ); }
		return s;
    }

    public final String format( String st ) { 
    	//public StringBuffer format( String st, StringBuffer toAppendTo, FieldPosition pos ) { 
     
		String s = null;
		try {
			if ( displayDateFormat == null ) throw new ADBCRuntimeException("ADBCDateFormat:Display format is null");
			s = displayDateFormat.format( this.getDate( (String) st ) );
		} catch (ParseException e) { throw new IllegalArgumentException( e.getMessage() ); }
		return s;
    }

	/** Converts the passed java.util.Date object to the specified internal format.
	  * If the internal date is Julian based, this method returns the date interval
	  * based from the difference in the passed date and the set epoch.
	  * <BR>Note:  If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this method
	  * redefined to perform the appropriate calculations to convert the passed
	  * java.util.Date to the internal format's used by the particular organization.
	  * @param d The passed date.
	  * @return The internal date.
	  */

    public long getLong( Date d ) {

    	if ( isJulian ) return (long) ( ( d.getTime() - epochDelta ) / dateUnit );

    	String s = internalDateFormat.format( d );
    	return Long.parseLong( s );
    }

	/** Converts the passed java.util.Date object to the specified internal format.
	  * If the internal date is Julian based, this method returns the date interval
	  * based from the difference in the passed date and the set epoch.
	  * <BR>Note:  If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this method
	  * redefined to perform the appropriate calculations to convert the passed
	  * java.util.Date to the internal format's used by the particular organization.
	  * @param d The passed date.
	  * @return The internal date.
	  */

	public double getDouble( Date d ) {
	
    	if ( isJulian ) return (double) ( ( d.getTime() - epochDelta ) / dateUnit );

		String s = internalDateFormat.format( d );
		//return Double.parseDouble( s );
		return VDelta.parseDouble( s );
	}

	/** Converts the passed java.util.Date object to the specified internal format.
	  * If the internal date is Julian based, this method returns the date interval
	  * based from the difference in the passed date and the set epoch.
	  * <BR>Note:  If an organization uses a date format that is much more complicated
	  * than the standard date formats, ADBCDateFormat can be subclassed and this method
	  * redefined to perform the appropriate calculations to convert the passed
	  * java.util.Date to the internal format's used by the particular organization.
	  * @param d The passed date.
	  * @return The internal date.
	  */

	public String getString( Date d ) {
		return internalDateFormat.format( d );
	}

    public final Number parse(String text, ParsePosition parsePosition) {

    	//System.out.println("trying to parse: Number parse(...");

		parsePosition.setIndex( 1 );
		Date d=null;
		try {
			d = displayDateFormat.parse( text );
		} catch (ParseException e) {parsePosition.setIndex ( 0 );}

    	return new Long( this.getLong( d ) );
    }

    public final Number parse(String text) throws ParseException {

    	return new Long( this.getLong( displayDateFormat.parse( text ) ) );

    }

    public final Object parseObject(String source) throws ParseException {

    	//return (Number) this.parse( source );
    	return this.getString( this.displayDateFormat.parse( source ) );
    	
    }

}

