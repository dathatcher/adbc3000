package advnetsys.adbc;

public class ValidationException extends ADBCException {

	private Object errorObject;
	//private int errorCode=0;

	public ValidationException() {
		super();
	}

	public ValidationException( String reason ) {
		super( reason );
	}

	public ValidationException( String reason, int errorCode ) {
		this( reason );
		this.errorCode = errorCode;
	}

	public ValidationException( String reason, String objectName, int errorCode ) {
		this( reason, errorCode );
		this.setObjectName( objectName );
	}

	public ValidationException( String reason, String objectName, String columnName, int errorCode ) {
		this( reason, objectName, errorCode );
		this.setColumnName( columnName );
	}

	public ValidationException( String reason, int errorCode, Object errorObject ) {
		this( reason, errorCode );
		this.setErrorObject( errorObject);
	}

	public void setErrorObject( Object errorObject ) {
		this.errorObject = errorObject;
	}

	public Object getErrorObject() {
		return this.errorObject;
	}
}

