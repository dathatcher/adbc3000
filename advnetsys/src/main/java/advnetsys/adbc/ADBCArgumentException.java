package advnetsys.adbc;

public class ADBCArgumentException extends ADBCRuntimeException {

	public ADBCArgumentException() {
		super();
	}

	public ADBCArgumentException( String reason ) {
		super( reason );
	}

	public ADBCArgumentException( String reason, int errorCode ) {
		this( reason );
		this.errorCode = errorCode;
	}

	public ADBCArgumentException( String reason, String objectName, int errorCode ) {
		this( reason, errorCode );
		this.setObjectName( objectName );
	}

	public ADBCArgumentException( String reason, String objectName, String columnName, int errorCode ) {
		this( reason, objectName, errorCode );
		this.setColumnName( columnName );
	}

	public ADBCArgumentException( Exception originalException, String columnName ) {
		super( originalException.getMessage() );
		this.originalException = originalException;
		this.columnName = columnName;
	}
}
