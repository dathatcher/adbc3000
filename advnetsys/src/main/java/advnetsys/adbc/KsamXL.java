package advnetsys.adbc;

import java.util.Vector;
import java.io.IOException;

/** The ADBC representation of a KSAM/XL file.
  */

public class KsamXL extends MpeFile {

	private static int RECNUM_MASK=0xfffb;
	private static int RECNUM_ZERO=0x0;
	private static int RECNUM_ONE=0x2;

	private static int PRIMARY_MASK=0xfffd;
	private static int PRIMARY_RANDOM_WRITE=0x0;
	private static int PRIMARY_SEQUENTIAL_WRITE=0x4;

	private static int REUSE_MASK=0xfff7;
	private static int REUSE_DISALLOW=0x0;
	private static int REUSE_ALLOW=0x8;

	private static int LANGUAGE_MASK=0xffef;
	private static int LANGUAGE_NOT_SPECIFIED=0x0;
	private static int LANGUAGE_SPECIFIED=0x10;

	private static int PRIMARY_FUPDATE_MASK=0xffdf;
	private static int PRIMARY_FUPDATE_ALLOW=0x0;
	private static int PRIMARY_FUPDATE_DISALLOW=0x20;

	private static int COBOL_MASK=0xffbf;
	private static int COBOL_NO=0x0;
	private static int COBOL_YES=0xff40;

	private static int BLOCKSIZE_MASK=0xff7f;
	private static int BLOCKSIZE_DEFAULT=0x0;
	private static int BLOCKSIZE_OPTIMAL=0x80;

	private int flagWord=0;
	private int languageId=0;

	private Vector keys = new Vector( 16 );

	public KsamXL() {}

	int getFop( int fop ) {
		return ( fop & Foption.FILE_MASK ) | Foption.KSAMXL ;
	}

	public final void setStartRecNumOne( boolean enabled ) {

		flagWord = flagWord & RECNUM_MASK;
		if ( enabled ) flagWord = flagWord | RECNUM_ONE ;

	}

	public final void setPrimarySequentialWrite( boolean enabled ) {

		flagWord = flagWord & PRIMARY_MASK;
		if ( enabled ) flagWord = flagWord | PRIMARY_SEQUENTIAL_WRITE;

	}

	public final void setReuse( boolean enabled ) {

		flagWord = flagWord & REUSE_MASK;
		if ( enabled ) flagWord = flagWord | REUSE_ALLOW;

	}

	public final void setLanguageTypeSpecified( boolean enabled ) {

		flagWord = flagWord & LANGUAGE_MASK;
		if ( enabled ) flagWord = flagWord | LANGUAGE_SPECIFIED;

	}

	public final void setPrimaryFupdate( boolean enabled ) {

		flagWord = flagWord & PRIMARY_FUPDATE_MASK;
		if ( ! enabled ) flagWord = flagWord | PRIMARY_FUPDATE_DISALLOW;

	}

	public final void setBlockSizeOptimal( boolean enabled ) {

		flagWord = flagWord & BLOCKSIZE_MASK;
		if ( enabled ) flagWord = flagWord | BLOCKSIZE_OPTIMAL;

	}

	public final void setLanguageId( int languageId ) {

		this.languageId = languageId;

	}

	public final void addKey( KsamKey key ) {

		keys.addElement( key );

	}

	int ksamOptionLength() {

		int ln=0;

		int numKeys = keys.size();

		if ( numKeys > 16 ) numKeys = 16;

		ln = (( numKeys * 4 ) + 3) * 2;

		return ln;

	}

	void writeKsamOptions( MpeSocket mpeSocket ) throws IOException {

		int numKeys = keys.size();
		if ( numKeys > 16 ) numKeys = 16;

		mpeSocket.out.writeShort( languageId );
		mpeSocket.out.writeShort( flagWord );
		mpeSocket.out.writeShort( numKeys );

		for (int i=1;i<=numKeys;i++) {

			((KsamKey) keys.elementAt( i-1 )).writeKey( mpeSocket );

		}
	}

}
