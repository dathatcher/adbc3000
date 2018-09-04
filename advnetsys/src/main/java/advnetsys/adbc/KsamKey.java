package advnetsys.adbc;

import java.io.IOException;

/** KsamKey is the key object that is created to specify a KSAM key
  * field when creating a KSAM file.  A seperate instance of this object
  * is needed for each key field.  An instance of this object is added 
  * to a {@link KsamXL} instance via the {@link KsamXL#addKey} method.
  */

public class KsamKey extends Object {

	public static int TYPE_BYTE=1;
	public static int TYPE_SHORT_INTEGER=2;
	public static int TYPE_INTEGER=3;
	public static int TYPE_REAL=4;
	public static int TYPE_LONG_REAL=5;
	public static int TYPE_NUMERIC_DISPLAY=6;
	public static int TYPE_PACKED_OLD=7;
	public static int TYPE_PACKED_EVEN=8;
	public static int TYPE_IEEE=9;

	private int keyType=TYPE_BYTE;
	private int keyLength;
	private int keyLocation;
	private int dupKeyFlag=1;
	private int randomInsertFlag=0;

	public KsamKey() {}

	public KsamKey( int keyType_, int keyLength, int keyLocation, 
	                boolean dupKeyEnabled_, boolean randomInsertEnabled_ ) {
	}

	public final void setKeyType( int keyType ) {
		this.keyType = keyType;
	}

	public final void setKeyLength( int keyLength ) {
		this.keyLength = keyLength;
	}

	public final void setKeyLocation( int keyLocation ) {
		this.keyLocation = keyLocation;
	}

	public final void setDupKeyEnabled( boolean enabled ) {
		this.dupKeyFlag = ( enabled ? 1 : 0 );
	}

	final void writeKey( MpeSocket mpeSocket ) throws IOException {

		int short1 = ( keyLength | ( keyType << 12 ) ) & 0xf0ff;
		int short3 = dupKeyFlag << 15;
		int short4 = randomInsertFlag << 7;

		mpeSocket.out.writeShort( short1 );
		mpeSocket.out.writeShort( keyLocation );
		mpeSocket.out.writeShort( short3 );
		mpeSocket.out.writeShort( short4 );

	}

}
