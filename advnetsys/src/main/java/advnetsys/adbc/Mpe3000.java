package advnetsys.adbc;

import java.lang.*;
import java.io.IOException;

/** The connection object to an HP3000. 
  */

public class Mpe3000 extends Object {

	private MpeSocket mpeSocket;
	private boolean available=true;
	private String mpeUrl="";
	private int mpePort;
	private String name="";
	private long registryTracker=0;
	private boolean loginOk=false;
	private short pin;

	public Mpe3000(){}

	/** Creates a Mpe3000 connection object.  The specified URL parameter
	  * passes the address of the HP3000 that is running the ADBC server
	  * component.  The syntax of the URL is as follows: <BR>
	  * "host.domain:&lt;port&gt;" 
	  */

	/* Creates a Mpe3000 connection object.  The specified URL parameter
	  * passes the address of the HP3000 that is running the ADBC server
	  * component.  The syntax of the URL is as follows: <BR>
	  * "host.domain:&lt;port&gt;:hpuser/&lt;password&gt;.hpaccount/&lt;password&gt;" 
	  */

	public Mpe3000( String url ) throws ADBCException {

		//setURL( url );
		parseURL( url );

	}

	//public void setName( String name ) { this.name = name ; }

	//public String getName(){ return name; }

	void setURL( String url ) {

		int port=30803;

		mpeUrl = url;
		String tmpUrl = new String( url );

		int colon = tmpUrl.indexOf(":");

		if ( colon > 0 ) { 
			port = Integer.valueOf( tmpUrl.substring( colon + 1 )).intValue();
			tmpUrl = tmpUrl.substring(0, colon );
		}

		try {
			mpeSocket = new MpeSocket( tmpUrl, port );
		} catch (IOException e) {
			throw new ADBCRuntimeException("Mpe3000:Unable to obtain connection to HP3000 host",40103);
		}

		loginOk = true;

	}

	private void parseURL(String url) throws ADBCException {
		int port=30803;
		int pos=0;
		int urlColon=-1;
		int portColon=-1;

		urlColon = url.indexOf( ':', 0 );
		if ( urlColon == -1 ) throw new ADBCException("Mpe3000.setURL:Invalid URL Format",40100);
		portColon = url.indexOf( ':', urlColon + 1 );
		if ( portColon == -1 ) throw new ADBCException("Mpe3000.setURL:Invalid URL Format",40100);

		String tmpUrl = url.substring(0,urlColon);
		port = Integer.valueOf( url.substring(urlColon+1,portColon)).intValue();
		String user = url.substring(portColon+1,url.length());

		//System.out.println("url=" + url.substring(0,urlColon) );
		//System.out.println("port=" + url.substring(urlColon+1,portColon));
		//System.out.println("user=" + url.substring(portColon+1,url.length()));

		int dot = user.indexOf('.',0);
		if ( dot == -1 ) throw new ADBCException("Mpe3000.setURL:Invalid URL Username Format",40101);
		int sessComma = user.lastIndexOf(',',dot);
		int userSlash = user.lastIndexOf('/',dot);
		int acctSlash = user.indexOf('/',dot);

		String sessName="";
		String userName="";
		String userPw="";
		String acctName="";
		String acctPw="";

		int userStart=0;

		if ( sessComma != -1 ) {
			userStart = sessComma + 1;
			sessName = user.substring( 0, sessComma ).toUpperCase();
		}

		if ( userSlash != -1 ) {
			userName = user.substring( userStart, userSlash ).toUpperCase();
			userPw = user.substring( userSlash + 1, dot ).toUpperCase();
		} else {
			userName = user.substring( userStart, dot ).toUpperCase();
		}

		if ( acctSlash != -1 ) {
			acctName = user.substring( dot + 1, acctSlash ).toUpperCase();
			acctPw = user.substring( acctSlash + 1, user.length() ).toUpperCase();
		} else {
			acctName = user.substring( dot + 1, user.length() ).toUpperCase();
		}

		//System.out.println("sessName=" + sessName + " length=" + sessName.length());
		//System.out.println("userName=" + userName);
		//System.out.println("userPw=" + userPw);
		//System.out.println("acctName=" + acctName);
		//System.out.println("acctPw=" + acctPw);

		if ( sessName.length() > 8 || userName.length() > 8 ||
		     userPw.length() > 8 || acctName.length() > 8 ||
		     acctPw.length() > 8 ) {
			throw new ADBCException("Mpe3000.setURL:Invalid URL Username Format",40101);
		}
		
		//.byte[] bOut = new byte[44];
		byte[] bOut = new byte[60];

		//.ByteArray.setShort( bOut, 0, (short) 42 );
		ByteArray.setShort( bOut, 0, (short) 58 );
		ByteArray.setString( bOut, 2, 2, "LI" );
		ByteArray.setString( bOut, 4, 8, sessName );/*+-+-+-*/
		ByteArray.setString( bOut, 12, 8, userName );
		ByteArray.setString( bOut, 20, 8, userPw );
		ByteArray.setString( bOut, 28, 8, acctName );
		ByteArray.setString( bOut, 36, 8, acctPw );

		int errno=0;

		try {

			mpeSocket = new MpeSocket( tmpUrl, port );

			ByteArray.setString( bOut, 44, 16, mpeSocket.getAddress() );

			mpeSocket.out.write( bOut );

			int recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			pin = mpeSocket.in.readShort();
		} catch (IOException e) {throw new ADBCException("Mpe3000:Unable to obtain connection to HP3000 host",40103);}

		if ( errno != 0 ) throw new ADBCException("Mpe3000.setURL:Invalid Login",40102);

		loginOk = true;

	}

	//public String getURL() { return mpeUrl; }

	synchronized MpeSocket checkOutSocket() throws ADBCException {

		if ( ! loginOk ) throw new ADBCException("Mpe3000:No Connection to Server",40200);
		while ( ! available ) {
			try {
				wait();
			} catch ( InterruptedException ex ) {
				ex.printStackTrace();
			}
		}
		available = false;
		return mpeSocket;
	}

	synchronized void checkInSocket() {
		available = true;
		notifyAll();
	}

	/* Added 12/04/2002 per goodyear. */

	synchronized void invalidateLogin() {
		this.loginOk = false;
		notifyAll();
	}

	synchronized long getRegistryId() {
		registryTracker++;
		return registryTracker;
	}

	public Database createDatabase( String dbName, int mode ) {
		return new Database( dbName, mode, this );
	}

	public Database createDatabase( String dbName ) {
		return new Database( dbName, Database.MODE5, this );
	}

	public MpeFile createMpeFile( String fileName ) {
		MpeFile f = new MpeFile( this );
		f.setName( fileName );
		return f;
	}

	public KsamXL createKsamXL( String fileName ) {
		KsamXL k = new KsamXL();
		k.setMpe3000( this );
		k.setName( fileName );
		return k;
	}

	public SpooledDevice createdSpooledDevice( String device, String spoolName ) {
		SpooledDevice sp = new SpooledDevice();
		sp.setMpe3000( this );
		sp.setName( spoolName );
		sp.setDevice( device );
		return sp;
	}

	public SpoolMgr createSpoolMgr() {
		return new SpoolMgr( this );
	}

	public Intrinsic createIntrinsic() {
		return new Intrinsic( this );
	}

	public Intrinsic createIntrinsic( String procName, String xl ) {
		Intrinsic in = this.createIntrinsic();
		in.setXL( xl );
		in.setName( procName );
		return in;
	}

	public short getPin() {
		return pin;
	}

	/** Gracefully close an Mpe3000 connection.
	  */

	public void close() {
		byte[] bOut = new byte[4];

		this.loginOk = false;

		ByteArray.setShort( bOut, 0, (short) 2 );
		ByteArray.setString( bOut, 2, 2, "CL" );
		try {
			MpeSocket mpeSocket = this.checkOutSocket();
			mpeSocket.out.write( bOut );
			mpeSocket.out.close();
			mpeSocket.in.close();
			//mpeSocket.socket.close();
		} catch (Exception e) {}
	}

	/** Kill an Mpe3000 connection.  The communication socket is closed hard.
	  */

	public void kill() {

		this.loginOk = false;

		try {

			this.mpeSocket.out.close();
			this.mpeSocket.in.close();
			this.mpeSocket.socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
        /** Check if still connected.  
	  */
        public boolean isConnected() {

             return loginOk;
        }
       /** Reconnect if connection has been lost (See advnetsys.adbc.Mpe3000.isConnected()
	  */
        public void reConnect( String url ) throws ADBCException {

		//setURL( url );
		parseURL( url );

	}
}
