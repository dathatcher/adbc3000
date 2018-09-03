package advnetsys.adbc;

import java.lang.Exception;

public class ADBCException extends Exception {

	protected int errorCode;
	protected String columnName=null;
	protected String objectName=null;
	protected Exception originalException=null;

	public ADBCException() {
		super();
	}

	public ADBCException( String reason ) {
		super( reason );
	}

	public ADBCException( String reason, int errorCode ) {
		super( reason );
		this.errorCode = errorCode;
	}

	public ADBCException( String reason, String objectName, int errorCode ) {
		this( reason, errorCode );
		this.setObjectName( objectName );
	}

	public ADBCException( String reason, String objectName, String columnName, int errorCode ) {
		this( reason, objectName, errorCode );
		this.setColumnName( columnName );
	}

	public ADBCException( Exception originalException, String columnName ) {
		super( originalException.getMessage() );
		this.originalException = originalException;
		this.setColumnName( columnName );
	}

	public String getMessage() {
		String mess = super.getMessage() + ":errorCode=" + errorCode;
		if ( objectName != null ) mess = mess + ":objectName=" + objectName;
		if ( columnName != null ) mess = mess + ":columnName=" + columnName;
		return mess;
	}

	public int getErrorCode() {
		return errorCode;
	}

	//public String toString() {
	//	return super.toString() + " Error Code: " + errorCode;
	//}

	public void setObjectName( String objectName ) {
		this.objectName = objectName;
	}

	public String getObjectName() {
		return this.objectName;
	}

	public void setColumnName( String columnName ) {
		this.columnName = columnName;
	}

	public boolean hasColumnInfo() {
		return ( columnName != null );
	}

	public String getColumnName() {
		return this.columnName;
	}

    //public void printStackTrace() { 
	//	synchronized (System.err) {
	//		super.printStackTrace();
	//		if ( originalException != null ) {
	//			System.err.println("--Original Exception--");
	//			originalException.printStackTrace();
	//		}
	//	}
    //}

    //public void printStackTrace(java.io.PrintStream s) { 
	//	synchronized (s) {
	//		super.printStackTrace( s );
	//		if ( originalException != null ) {
	//			System.err.println("--Original Exception--");
	//			originalException.printStackTrace( s );
	//		}
	//	}
    //}

    //public void printStackTrace(java.io.PrintWriter s) { 
	//	synchronized (s) {
	//		super.printStackTrace( s );
	//		if ( originalException != null ) {
	//			System.err.println("--Original Exception--");
	//			originalException.printStackTrace( s );
	//		}
	//	}
    //}

}
