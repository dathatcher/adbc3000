package advnetsys.adbc;

import java.io.*;

//....public class TurboBufferThread extends Thread {
class TurboBufferThread extends Object implements Runnable {

	private TurboBuffer tb=null;
	private MpeSocket mpeSocket=null;
	private Thread fetchThread=null;

	public TurboBufferThread() {
		//....System.out.println("TurboBufferThread initialized...");
	}

	void initiateRetrieve( TurboBuffer tb, MpeSocket mpeSocket ) {
		this.tb = tb;
		this.mpeSocket = mpeSocket;
		this.fetchThread = null;
		fetchThread = new Thread( this );
		fetchThread.start();
	}

	public void run() {

		//this.setPriority( Thread.MIN_PRIORITY );
		//Thread.yield();
		try {

			boolean done = false;
			short recordLength;
			byte[] b=null;
			int dbError = 0;

			while ( ! done ) {

				recordLength = mpeSocket.in.readShort();
				b = new byte[recordLength];
				mpeSocket.in.readFully( b, 0, recordLength );

				//System.out.print("*"+ new String(b,0,2)+"*");

				if ( b[0] != 'D' ) {

					//...System.out.println("=============================================Fetch is complete================================");
					ADBCException e = null;
					done = true;
					if ( b[0] == 'E' & b[1] == 'R' ) {
						//...dbError = mpeSocket.in.readShort();
						dbError = ByteArray.getShort( b, 2 );
						//if ( ! ( tb.isMpeFile() && ( dbError == 172 || dbError == 12 ) ) ) {
						String errorType = "Image";   //2.0.1
						if ( tb.isMpeFile() ) errorType = "MPE File";  //2.0.1

						//throw new ADBCRuntimeException("TurboBuffer.Thread:Image Error",tb.getName(),dbError);  //2.0.1
						//throw new ADBCRuntimeException(
							//"TurboBuffer.Thread:" + errorType + " Error",tb.getName(),dbError);  //2.0.1
						e = new ADBCException(
							"TurboBuffer.Thread:" + errorType + " Error",tb.getName(),dbError);  //2.0.1
					}
					tb.setFetchComplete( e );
				}
				else {

					tb.addBufferRecord( b );
					//System.out.println("thread element added...");
					//Thread.yield();

				}
			}

		//...} catch (IOException ex) {throw new ADBCRuntimeException("TurboBuffer.Thread:Communication Error",tb.getName(),40000);}      
		} catch (IOException ex) {
			tb.setFetchComplete( new ADBCException("TurboBuffer.Thread:Communication Error",tb.getName(),40000) );
		}      
	}
}
