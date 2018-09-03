package advnetsys.adbc;

import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

/** SpoolMgr is a refreshable container of {@link SpoolId} objects.
  * Basically, SpoolMgr provides a list of available spool files that
  * are on the HP3000 server.  The list can be condensed via the 
  * {@link #setQuery} method which accepts as it's input an MPE
  * LISTSPF commands SELEQ option.
  */

public class SpoolMgr extends Object {

	private Mpe3000 mpe3000=null;
	private String name="";

	private Vector spoolId = new Vector();

	private String query="";

	public SpoolMgr( Mpe3000 mpe3000 ){
		this.mpe3000 = mpe3000;
	}

	/** Set the name of this SpoolMgr.
	  */

	public void setName( String name ) {
		this.name = name;
	}

	/** Return the name of this SpoolMgr.
	  */

	public String getname() {
		return this.name;
	}

	/** Secify as query that will condense the list of spoolfiles
	  * provided by the HP3000 server.  The format of the query string
	  * is the format of the MPE LISTSPF commands SELEQ option.
	  * @param query The query string in LISTSPF ;SELEQ= format.
	  */

	public void setQuery( String query ) {
		this.query = query;
	}

	/** Refresh this SpoolMgr object.  If a query has been previously
	  * set, it is also passed to the server to condense the list.
	  */

	public void refresh() throws ADBCException {

		spoolId.removeAllElements();

		MpeSocket mpeSocket=null;

		try {

			mpeSocket = mpe3000.checkOutSocket();

			int sendPacketLength = 4;
			int queryLength = 0;
			if ( query != null && query.length() > 0 ) {
				queryLength = query.length();
				sendPacketLength += queryLength;
			}

			byte[] bOut = new byte[sendPacketLength + 2];

			ByteArray.setShort( bOut, 0, (short) sendPacketLength );
			ByteArray.setString( bOut, 2, 2, "SP" );

			ByteArray.setShort( bOut, 4, (short) queryLength );

			if ( queryLength > 0 ) {    // query contains value.
				ByteArray.setString( bOut, 6, query.length(), query );
			}

			mpeSocket.out.write( bOut );

			boolean done=false;

			while ( ! done ) {

				int recvLength = mpeSocket.in.readShort();
				byte[] bIn= new byte[recvLength];

				mpeSocket.in.readFully( bIn );

				if ( recvLength > 4 ) {

					int offset = 2;
					while ( offset < recvLength ) {
						SpoolId spId = new SpoolId( this.mpe3000 );
						spId.parse( bIn, offset );
						offset += 117;
						spoolId.addElement( spId );
					}
				}
			
				if ( bIn[0] == 'E' ) done = true;
			}


		} catch (IOException e) {throw new ADBCException("SpoolMgr.refresh:Communication Error",this.name,40000);}

		mpe3000.checkInSocket();
	}

	/** Returns the number of {@link SpoolId} objects.
	  */

	public int size() {
		return spoolId.size();
	}

	/** Return the {@link SpoolId} object at the given index.
	  */

	public Object get( int index ) {
		return spoolId.elementAt( index );
	}

	/** Return an enumeration of {@link SpoolId} objects.
	  */

	public Enumeration elements() {
		return spoolId.elements();
	}
}
