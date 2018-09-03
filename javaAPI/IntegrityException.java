package advnetsys.adbc;

public class IntegrityException extends ADBCException {

	public IntegrityException() {
		super();
	}

	public IntegrityException( String reason ) {
		super( reason );
	}

	public IntegrityException( String reason, int errorCode ) {
		this( reason );
		this.errorCode = errorCode;
	}

	public IntegrityException( String reason, String objectName, int errorCode ) {
		this( reason, errorCode );
		this.setObjectName( objectName );
	}

	public IntegrityException( String reason, String objectName, String columnName, int errorCode ) {
		this( reason, objectName, errorCode );
		this.setColumnName( columnName );
	}
}

