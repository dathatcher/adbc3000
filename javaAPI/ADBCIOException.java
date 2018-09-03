package advnetsys.adbc;

class ADBCIOException extends ADBCException {

	ADBCIOException( Mpe3000 mpe3000 ) {
		super();
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}

	ADBCIOException( Mpe3000 mpe3000, String reason ) {
		super( reason );
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}

	ADBCIOException( Mpe3000 mpe3000, String reason, int errorCode ) {
		super( reason, errorCode );
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}

	ADBCIOException( Mpe3000 mpe3000, String reason, String objectName, int errorCode ) {
		super( reason, objectName, errorCode );
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}

	ADBCIOException( Mpe3000 mpe3000, String reason, String objectName, String columnName, int errorCode ) {
		super( reason, objectName, columnName, errorCode );
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}

	ADBCIOException( Mpe3000 mpe3000, Exception originalException, String columnName ) {
		super( originalException, columnName );
		mpe3000.checkInSocket();
		mpe3000.invalidateLogin();
	}
}
