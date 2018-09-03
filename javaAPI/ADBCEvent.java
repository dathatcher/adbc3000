package advnetsys.adbc;

import java.util.EventObject;

/** An ADBCEvent is the base class for row and column events that report the 
  * changes of a TurboBuffer to row and column listeners that
  * have registered with the particular TurboBuffer.  A listener can 
  * use the event for validating changes to the TurboBuffer.  It can 
  * set the status of the event as having failed - setFailed().  A generic
  * or customized ValidationException can be passed back to the TurboBuffer.
  * When all listeners have been processed, the TurboBuffer will throw the
  * ValidationException.  This technique of passing the ValidationException 
  * back to the TurboBuffer was used so that all listeners can be processed
  * prior to the exception being thrown.  Listeners can use the
  * ADBCEvent.hasFailed() method to determine if a failure has been encountered.
  */

public class ADBCEvent extends EventObject {

	private int row;	
	private int type;
	private boolean failed=false;
	//private String errorMessage;
	//private int errorCode=0;
	private ValidationException vException=null;

	/** The ADBCEvent constructor.  
	  * @param source The TurboBuffer instance.
	  * @param type The event type.
	  * @param row The row number that this event is based.
	  */

	public ADBCEvent( Object source, int type, int row ) {

		super(source);
		this.type = type;
		this.row = row;

	}

	/** Return the event type.
	  */

	public int getType() { return type; }

	/** Return the row number that this event is based on.
	  */

	public int getRow() { return row; }

	/** Mark this event as having failed.  A generic ValidationException
	  * is created and passed back to the Turbobuffer that created the
	  * event.  The TurboBuffer will then throw the ValidationException.
	  */

	public void setFailed() {
		this.failed = true;
		vException = new ValidationException("A validation error occured.");
	}

	//public void setFailed( String errorMessage, int errorCode ) {
	//	this.failed = true;
	//	this.errorMessage = errorMessage;
	//	this.errorCode = errorCode;
	//	vException = new ValidationException( 
	//}

	/** Mark this even as having failed and pass a user defined 
	  * ValidationException back to the TurboBuffer that created the
	  * event.  This method allows the ability for the developer to
	  * create customized events that subclass ValidationException.
	  * @param customException A customized exception.
	  */

	public void setFailed( ValidationException customException ) {
		this.vException = customException;
		this.failed = true;
	}

	/** Inquire whether the event has been set to fail.
	  */

	public boolean hasFailed() {
		return this.failed;
	}

	//public String getErrorMessage() {
	//	return this.errorMessage;
	//}

	//public int getErrorCode() {
	//	return this.errorCode;
	//}

	//public boolean hasCustomException() {
	//	return ( this.vException != null );
	//}

	/** get the ValidationException that a failed event contains.
	  */

	public ValidationException getException() {
		return this.vException;
	}
}
