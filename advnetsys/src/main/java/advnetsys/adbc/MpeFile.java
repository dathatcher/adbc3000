package advnetsys.adbc;

import java.lang.*;
import java.io.*;

/** The ADBC representation of an MPE file.
  */

public class MpeFile extends Object implements DataTable {

	private static int DOMAIN_MASK = 0xfffc;
	private static int DOMAIN_NEW = 0x0;
	private static int DOMAIN_PERMANENT = 0x1;
	private static int DOMAIN_TEMPORARY = 0x2;

	private static int CODE_MASK = 0xfffb;
	private static int CODE_BINARY = 0x0;
	private static int CODE_ASCII = 0x4;

	private static int DESIGNATOR_MASK = 0xffc7;

	private static int FORMAT_MASK = 0xff3f;
	private static int FORMAT_FIXED = 0x0;
	private static int FORMAT_VARIABLE = 0x40;
	private static int FORMAT_UNDEFINED = 0x80;
	private static int FORMAT_SPOOLFILE = 0xc0;

	private static int CARRIAGE_MASK = 0xfeff;
	private static int CARRIAGE_NONE = 0x0;
	private static int CARRIAGE_EXPECTED = 0x100;

	private static int LABELED_MASK = 0xfdff;

	private static int FILEEQ_MASK = 0xfbff;
	private static int FILEEQ_ALLOW = 0x0;
	private static int FILEEQ_DISALLOW = 0x400;

	private static int FILE_MASK = 0xc7ff;
	private static int FILE_STD = 0x0;
	private static int FILE_KSAM = 0x800;
	private static int FILE_RIO = 0x1000;
	private static int FILE_KSAMXL=0x1800;
	private static int FILE_CIR = 0x2000;
	private static int FILE_MSG = 0x3000;
	private static int FILE_SPOOLFILE = 0x2800;

	private static int ACCESS_MASK = 0xfff0;
	private static int ACCESS_READ = 0x0;
	private static int ACCESS_WRITE = 0x1;
	private static int ACCESS_WRITESAVE = 0x2;
	private static int ACCESS_APPEND = 0x3;
	private static int ACCESS_READWRITE = 0x4;
	private static int ACCESS_UPDATE = 0x5;
	private static int ACCESS_EXECUTE = 0x6;

	private static int MULTIRECORD_MASK = 0xffef;
	private static int MULTIRECORD_NOMULTI = 0x0;
	private static int MULTIRECORD_MULTI = 0x8;

	private static int LOCKING_MASK = 0xffdf;
	private static int LOCKING_DISABLED = 0x0;
	private static int LOCKING_ENABLED = 0x20;

	private static int EXCLUSIVE_MASK = 0xff3f;
	private static int EXCLUSIVE_READ = 0x0;
	private static int EXCLUSIVE_EXCLUSIVE = 0x40;
	private static int EXCLUSIVE_READSHARE = 0x80;
	private static int EXCLUSIVE_SHARE = 0xc0;

	private static int BUFFERING_MASK = 0xfeff;
	private static int BUFFERING_NORMAL = 0x0;
	private static int BUFFERING_INHIBIT = 0x100;

	private static int MULTIACCESS_MASK = 0xf9ff;
	private static int MULTIACCESS_NOMULTI = 0x0;
	private static int MULTIACCESS_MULTI = 0x200;
	private static int MULTIACCESS_GMULTI = 0x400;

	private static int NOWAIT_MASK = 0xf7ff;

	private static int COPYMODE_MASK = 0xefff;
	private static int COPYMODE_DISABLED = 0x0;
	private static int COPYMODE_ENABLED = 0x1000;

	public  static int LOCKFLAG_RIN=0;
	public  static int LOCKFLAG_UNCONDITIONAL=1;

	private static int CLOSE_PERMANENT=1;
	private static int CLOSE_TEMPORARY=2;
	private static int CLOSE_RELEASE=4;

	private String fileName="";

	private String lockWord;

	private short fileNum;

	private Mpe3000 mpe3000;
	private MpeSocket mpeSocket;

	private boolean permanent = true;

	private int fOptions = 1;
	private int aOptions = 0;

	private int recSize=0;
	private String device="";
	private int blockFactor=1;
	private int numBuffers=2;
	protected int spoolerCopies=1;
	protected int outputPriority=8;
	private int fileSize=1023;
	private int numExtents=8;
	private int initialAllocatedExtents=1;
	private int fileCode=0;

	//private KsamOptions ksamOption=null;

	private int defaultLockFlag=LOCKFLAG_UNCONDITIONAL;

	private int domainDisposition=0;

	private byte[] buffer;

	private RowSet rowSet=null;

	private int fetchSize=1;

	private CircularBuffer circularBuffer=null;


	public MpeFile() {}

	public MpeFile( Mpe3000 mpe3000 ) {
		this.setMpe3000( mpe3000 );
	}

	public final void setName( String fileName ) {
		this.fileName = fileName;
	}

	public final String getName() {
		return fileName;
	}

	/** If the file is protected with a lock work, set it with this method.
	  */

	public final void setLockWord( String lockWord ) {
		this.lockWord = lockWord;
	}

	/** Set the file to be permanent when MpeFile.close() is called.
	  * @param permanent TRUE - the file is closed as permanent, FALSE -
	  * the file is closed as a temporary file.
	  */

	public final void setPermanent( boolean permanent ) {
		this.permanent = permanent;
	}

	/** Set the Mpe3000 connection object for this file if one was not
	  * specified in the constructor.
	  * @param mpe3000 The Mpe3000 connection object.
	  */

	public final void setMpe3000( Mpe3000 mpe3000 ) {
		this.mpe3000 = mpe3000;
	}

	/*2.0.1*/
	public boolean isTBInitOK() {
		if ( ! rowSet.isVariableLength() && rowSet.getLength() > recSize ) {
                        System.out.println("rec = " + recSize);
			throw new ADBCRuntimeException("TurboBuffer:Rowset size > MpeFile record size",this.fileName,44000);
		}
		return true;
	}

	/** Set the File Options for the MPE file.  Use the java OR (|) to 
	  * compound the File Option values.  The interface {@link Foption} provides
	  * static variables that contain the available File Options.
	  * @param fOptions The desired File Options.
	  */

	public final void setFoptions( int fOptions ) {

		this.fOptions = fOptions;

	}

	/** Set the Access Options for the MPE file.  Use the java OR (|)
	  * to compound the Access Option values.  The interface {@link Aoption}
	  * provides static variables that contain the available Access Options.
	  * @param aOptions The desired Access Options.
	  */

	public final void setAoptions( int aOptions ) {

		this.aOptions = aOptions;

	}

	/** Return the File Options that have been set for this MPE file.
	  */

	public final int getFoptions() {
		return this.fOptions;
	}

	/** Return the Access Options that have been set for this MPE file.
	  */

	public final int getAoptions() {
		return this.aOptions;
	}

	/** Determines if carriage control has been enabled for this file.
	  */

	boolean isCCTL() {

		return ( ( fOptions & CARRIAGE_MASK ) > 0 );

	}

	//public void setType( int type ) {  //fix here.........................

	//	int tmptype = ( type & ~FILE_MASK );
	//	fOptions = ( fOptions & FILE_MASK );
	//	fOptions = ( fOptions | tmptype );

	//}

	public final int getType() {
		return fOptions & ~FILE_MASK;
	}

	/** Set the record size in bytes for a new MPE file.
	  * @param recSize The size of the record.
	  */

	public final void setRecSize( int recSize ) {
		this.recSize = recSize;
	}

	/** Return the record size.
	  */

	public final int getRecSize() {
		return recSize;
	}

	/** If opening a device, set the name of the device.
	  * @param device The device name.
	  */

	public final void setDevice( String device ) {
		this.device = device;
	}

	/** Set the Block Factor for a new MPE File.
	  * @param blockFactor The Blocking Factor.
	  */

	public final void setBlockFactor( int blockFactor ) {
		this.blockFactor = blockFactor;
	}

	/** Set the number of buffers for a new MPE file.
	  * @param numBuffers The number of buffers.
	  */

	public final void setNumBuffers( int numBuffers ) {
		this.numBuffers = numBuffers;
	}

	/** Set the file size for a new MPE file.
	  * @param fileSize The file size.
	  */

	public final void setFileSize( int fileSize ) {
		this.fileSize = fileSize;
	}

	/** Set the number of extents to use for a new MPE file.
	  * @param numExtents The number of extents.
	  */

	public final void setNumExtents( int numExtents ) {
		this.numExtents = numExtents;
	}

	/** Set the number of initially allocated extents for a new MPE file.
	  * @param initialAllocatedExtents The number of initially allocated extents.
	  */

	public final void setInitialAllocatedExtents( int initialAllocatedExtents ) {
		this.initialAllocatedExtents = initialAllocatedExtents;
	}

	/** Set the file code for a new MPE file.
	  * @param fileCode The file code.
	  */

	public final void setFileCode( int fileCode ) {
		this.fileCode = fileCode;
	}

	/** Set the defaul lock mode for this MPE file.  This value is used 
	  * whenever the MpeFile.lock() method is called with no arguments.
	  * The two appropriate values are MpeFile.LOCKFLAG_RIN and 
	  * MpeFile.LOCKFLAG_UNCONDITIONAL.
	  * @param lockMode The desired default lock mode.
	  */

	public final void setDefaultLockMode( int lockMode ) {

		this.defaultLockFlag = lockMode;

	}

	/** Return the default lock mode.
	  */

	public final int getDefaultLockMode() {
		return defaultLockFlag;
	}

	public final short getIdentifier() {  //fix here.......................
		return fileNum;
	}

	/** Return the MPE file number for this file.
	  */

	public final short getId() {
		return fileNum;
	}

	/** Set a {@link RowSet} for this MpeFile.  The specified RowSet is used
	  * by a TurboBuffer for determining columns for an MPE file.
	  * @param rowSet The RowSet.
	  */

	public final void setRowSet( RowSet rowSet ) {
		rowSet.setFinalized();  //2.0.1 - Do not allow RowSet to be altered after set.
		this.rowSet = rowSet;
	}

	/** Return the MpeFile's RowSet.
	  */

	public final RowSet getRowSet() {
		return rowSet;
	}

	/** Lock this MpeFile.  The set default lock flag is used.
	  */

	public final void lock() throws ADBCException {

		this.lock( defaultLockFlag );

	}

	/** Lock this MpeFile using the specified lock flag.
	  * The two appropriate values are MpeFile.LOCKFLAG_RIN and 
	  * MpeFile.LOCKFLAG_UNCONDITIONAL.
	  * @param lockFlag The desired lock flag.
	  */

	public final void lock( int lockFlag ) throws ADBCException {

		/* Followins is the information send to the server.
		 * Word  Byte
		 * 01    01:02 "FL" lock the file.
		 * 02    03:04 file number.
		 * 03    05:06 lock flag.
		 */
		
		mpeSocket = mpe3000.checkOutSocket();

		short errno = 0;

		short sendPacketLength = 6;

		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FL" );
			mpeSocket.out.writeShort( fileNum );
			mpeSocket.out.writeShort( lockFlag );

			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();

		} catch (IOException e) {throw new ADBCException("MpeFile.lock:Communication Error",this.fileName,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 ) throw new ADBCException("MpeFile.lock:MPE File Error",this.fileName,errno );

	}

	/** Unlock this MpeFile.
	  */

	public final void unLock() throws ADBCException {

		/* The folling information is sent to the server 
		 * Byte  Word
		 * 01    01:02 "FU" unlock file.
		 * 02    03:04 file number.
		 */
		
		mpeSocket = mpe3000.checkOutSocket();

		short errno=0;

		short sendPacketLength = 4;

		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FU" );
			mpeSocket.out.writeShort( fileNum );

			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();

		} catch (IOException e) {throw new ADBCException("MpeFile.unLock:Communication Error",this.fileName,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 ) throw new ADBCException("MpeFile.unlock:MPE File Error",this.fileName,errno );

	}

	public final Mpe3000 getMpe3000() {
		return mpe3000;
	}

	public final short getDataSourceId() {
		return (short) -9999;
	}

	/** ksamOptionLength() is designed to be overloaded by the KsamXL class.
	  * It will return the length of the ksam options used in an fopen statement
	  * when creating a ksam file.
	  */

	int ksamOptionLength() {
		return 0;
	}

	void writeKsamOptions( MpeSocket mpeSocket_ ) throws IOException {}

	int getAop( int aop ) {
		return aop;
	}

	int getFop( int fop ) {
		return fop;
	}

	private void openHelper() throws ADBCException {

		/* Following is the information sent to the server.
		 * Word  Byte
		 * 01    01:02 "FO" tells the server to open this file.
		 * 02    03:04 foptions
		 * 03    05:06 aoptions
		 * 04    07:08 record size
		 * 05    09:10 blocking factor
		 * 06    11:12 numBuffers, spoolerCopies and outputPriority all combined
		 * 07-08 13:16 file size
		 * 09    17:18 number of extents.
		 * 10    19:20 initial number of allocated extents.
		 * 11    21:22 file code.
		 * 12    23:24 length of ksam options(zero if not a ksam file).
		 * 13    25:26 length of file name/lock word combination.
		 * 14    27:28 length of the device string.
		 *
		 *       Note: *12, *13 and *14 are contents of words 12, 13 and 14.
		 *
		 * 15    29:(29 + *12 - 1) The ksam options(if a ksam file).
		 *       (29 + *12):(29 + *12 + *13 - 1) The filename/lockword combo data.
		 *       (29 + *12 + *13):(29 + *12 + *13 + *14 - 1) the device string.
		 */


		short errno = 0;

		/* If a lock word exists, place it into the fileName. */

		String fileNameOption = new String( fileName );
		
		if ( lockWord != null ) {
			int period = fileName.indexOf( '.' );
			if ( period > 0 ) {
				fileNameOption = fileName.substring( 0, period ) + "/" + lockWord + 
				                 fileName.substring( period );
				//System.out.println("fileName: " + fileNameOption );
			}
		}
		
		/* Compute the length of the packet */

		short sendPacketLength = 28;
		sendPacketLength += fileNameOption.length();
		sendPacketLength += device.length();
		sendPacketLength += ksamOptionLength();

		//if ( ksamOption != null ) sendPacketLength += ksamOption.length();
		
		mpeSocket = mpe3000.checkOutSocket();

		/* Allow subclasses to set the default file type(ie KsamXL) */

		fOptions = getFop( fOptions );
		aOptions = getAop( aOptions );

		/* Clear the left words of fOptions and aOptions. */ 

		fOptions = fOptions & 0x0000ffff & DESIGNATOR_MASK & LABELED_MASK;
		aOptions = aOptions & 0x0000ffff & NOWAIT_MASK;

		//System.out.println("foptions: " + Integer.toBinaryString( fOptions ) + " " + 
		//"aoptions: " + Integer.toBinaryString( aOptions ) );

		/* Combine numBuffers, spoolerCopies, and outputPriority into 
		 * one short value.
		 * numBuffers(11:5), spoolerCopies(4:7), outputPriority(0:4)
		 */
		
		int numBuffersOption = numBuffers | ( spoolerCopies << 5 ) | ( outputPriority << 12 );
	
		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FO" );
			mpeSocket.out.writeShort( fOptions );
			mpeSocket.out.writeShort( aOptions );
			mpeSocket.out.writeShort( recSize );
			mpeSocket.out.writeShort( blockFactor );
			mpeSocket.out.writeShort( numBuffersOption );
			mpeSocket.out.writeInt( fileSize );
			mpeSocket.out.writeShort( numExtents );
			mpeSocket.out.writeShort( initialAllocatedExtents );
			mpeSocket.out.writeShort( fileCode );

			//int ksamOptionLength = 0;
			//if ( ksamOption != null ) ksamOptionLength = ksamOption.length();
			mpeSocket.out.writeShort( ksamOptionLength() );

			mpeSocket.out.writeShort( fileNameOption.length() );
			mpeSocket.out.writeShort( device.length() );

			//if ( ksamOptionLength > 0 ) 
			//	ksamOption.writeOptions( mpeSocket );
			if ( ksamOptionLength() > 0 ) 
				writeKsamOptions( mpeSocket );

			if ( fileNameOption.length() > 0 ) 
				mpeSocket.out.writeBytes( fileNameOption );
			if ( device.length() > 0 ) 
				mpeSocket.out.writeBytes( device );

			/* The server returns the following.
			 * 01    01-02 error code.
			 * 02    03-04 file number.
			 * 03    05-06 foptions.
			 * 04    07-08 aoptions.
			 * 05    09-10 recsize.
			 * 06-07 11-14 filesize.
			 */
			
			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			fileNum = mpeSocket.in.readShort();
			int tmpFileOptions = mpeSocket.in.readShort();
			int tmpAccessOptions = mpeSocket.in.readShort();
			int tmpRecSize = mpeSocket.in.readShort();
			int tmpFileSize = mpeSocket.in.readInt();

			//System.out.println("fileoptions: " + tmpFileOptions );
			//System.out.println("accessoptions: " + tmpAccessOptions );
			//System.out.println("recsize: " + tmpRecSize );
			//System.out.println("filesize: " + tmpFileSize );

			//System.out.println("errno: " + errno + " fileno: " + fileNum );

			//2.0.1 force the actual record size received from the server 
			//if ( recSize == 0 ) recSize = tmpRecSize;2.0.1
			recSize = tmpRecSize;//2.0.1

			mpe3000.checkInSocket();

			if ( errno != 0 ) throw new ADBCException("MpeFile.open/create:MPE File Error",this.fileName,errno);

		} catch ( IOException e ) {throw new ADBCException("MpeFile.open/create:Communication Error",this.fileName,40000);}
	}

	/** Open an existing permanent or temporary file on the server.
	  */

	public final void open() throws ADBCException {

		/* set the lookup DOMAIN for the file to be either the
		 * temporary or permanent name spaces.
		 */

		fOptions = fOptions & DOMAIN_MASK;
		
		if ( permanent ) fOptions = fOptions | DOMAIN_PERMANENT;
		else             fOptions = fOptions | DOMAIN_TEMPORARY;

		openHelper();

	}

	/** Create a new file on the server. 
	  */

	public final void create() throws ADBCException {

		/* set the lookup DOMAIN for the file to be new.
		 * For a new file, the file will be set as permanent or
		 * temporary on the close() method.
		 */

		fOptions = fOptions & DOMAIN_MASK;
		fOptions = fOptions | DOMAIN_NEW;

		openHelper();

	}

	/** Close the this file on the server.
	  */

	public final void close() throws ADBCException {

		//domainDisposition = 0x8;
		domainDisposition = 0x0;

		if ( permanent ) domainDisposition = domainDisposition | CLOSE_PERMANENT;
		else             domainDisposition = domainDisposition | CLOSE_TEMPORARY;

		this.closeHelper();

	}

	private void closeHelper() throws ADBCException {

		/* The following information is sent to the server.
		 * Word  Byte
		 * 01    01:02 "FC" tells the server to perform an fclose.
		 * 02    03:04 file number.
		 * 03    05:06 the domain disposition.
		 */
		
		mpeSocket = mpe3000.checkOutSocket();

		short sendPacketLength = 6;
		short errno = 0;

		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FC" );
			mpeSocket.out.writeShort( fileNum );
			mpeSocket.out.writeShort( domainDisposition );

			//short recvPacketLength = mpeSocket.in.readShort();
			//errno = mpeSocket.in.readShort();

			byte[] b = new byte[4];
			mpeSocket.in.readFully( b );
			errno = ByteArray.getShort( b, 2 );

		} catch (IOException e) {throw new ADBCException("MpeFile.close:Communication Error",this.fileName,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 ) throw new ADBCException("MpeFile.close:MPE File Error",this.fileName,errno);

	}

	/** Read this files current record.
	  * The record is stored in an internal byte array.  Use the
	  * method {@link #getBuffer} to retrieve the byte array.
	  */

	public final boolean read() throws ADBCException {

		return read( this.recSize );

	}

	//public boolean readBackward() {

	//	return readBackward( this.recSize );

	//}

	/** Read in the number of specified bytes from this files current record.
	  * The record is stored in an internal byte array.  Use the
	  * method {@link #getBuffer} to retrieve the byte array.
	  * @param recSize The number of bytes to read in.
	  */

	public final boolean read( int recSize ) throws ADBCException {

		return this.readHelper( -1, recSize );

	}

	//public boolean readBackward( int recSize_ ) {

	//	return this.readHelper( -2, recSize_ );

	//}

	/** Read in the record specified at the passed address.
	  * The record is stored in an internal byte array.  Use the
	  * method {@link #getBuffer} to retrieve the byte array.
	  * @param recNum The record address.
	  */

    public final boolean readDir( int recNum ) throws ADBCException {

    	return this.readDir( this.recSize, recNum );

    }

	/** Read in the number of specified bytes from the record at the
	  * specified record address.
	  * The record is stored in an internal byte array.  Use the
	  * method {@link #getBuffer} to retrieve the byte array.
	  * @param recSize The number of bytes to read from the record.
	  * @param recNum The record address.
	  */

	public final boolean readDir( int recSize, int recNum ) throws ADBCException {

		return this.readHelper( recNum, recSize );

	}

	private boolean readHelper(int recNum_, int recSize_ ) throws ADBCException {

		/* recNum values:
		 * -1   server performs fread.
		 * -2   server performs freadbackward.
		 * >=0  server performs freaddir with recnum being the record number.
		 */

		/* Following is information sent to the server.
		 * Word  Byte
		 * 01    01:02 "FR" tells the server to perform an fread.
		 * 02    03:04 The file number.
		 * 03    05:06 The record Size;
		 * 04-05 07:10 The record Number.
		 */

		short errno=0;
		boolean returnStatus = false;

		mpeSocket = mpe3000.checkOutSocket();

		short sendPacketLength = 10;

		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FR" );
			mpeSocket.out.writeShort( fileNum );
			mpeSocket.out.writeShort( recSize_ );
			mpeSocket.out.writeInt( recNum_ );

			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();

			buffer = null;
			
			if ( recvPacketLength > 2 ) {
				buffer = new byte[ recvPacketLength - 2 ];
				mpeSocket.in.readFully( buffer );
				returnStatus = true;
			}

			if ( errno == 0 ) returnStatus = true;

		} catch (IOException e) {throw new ADBCException("MpeFile.read:Communication Error",this.fileName,40000);}

		mpe3000.checkInSocket();

		if ( errno > 0 ) throw new ADBCException("MpeFile.read:MPE File Error",this.fileName,errno);

		return returnStatus;
	}

	/** Return the internal byte array that contains the data from the
	  * record of the most recent MpeFile.read() method.
	  */

	public final byte[] getBuffer() {

		return buffer;

	}

	/** Write the specified byte array to the current record in the file.
	  * @param b The byte array that contains the data.
	  */

	public final void write( byte[] b ) throws ADBCException {

		this.writeHelper( -1, b, 0 );

	}

	/** Write the specified byte array to the record at the specified 
	  * address.
	  * @param b The data.
	  * @param recNum The record address.
	  */

	public final void writeDir( byte[] b, int recNum ) throws ADBCException {

		this.writeHelper( recNum, b, 0 );

	}

	/** Update the files current record with the data contained in
	  * the passed byte array.
	  * @param b The data.
	  */

	public final void update( byte[] b ) throws ADBCException {

		this.writeHelper( -2, b, 0 );

	}
        public final void setId( short fileNum ) {
     
                this.fileNum = fileNum;

        }
        
	private void writeHelper( int recNum_, byte[] b, int controlCode_ ) throws ADBCException {

		/* The following is what is sent to the server. 
		 * Word   Byte
		 * 01     01:02 "FW" for fwrite.
		 * 02     03:04 file number.
		 * 03     05:06 record size.
		 * 04     07:08 control code.
		 * 05-06  09:12 the record number. -1=fwrite, -2=fupdate, >=0 fwritedir.
		 * 07     13:14 the byte array of data.
		 */
	
		short errno=0;

		short sendPacketLength = (short)(12 + b.length);

		mpeSocket = mpe3000.checkOutSocket();

		try {
		
			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FW" );
			mpeSocket.out.writeShort( fileNum );
			mpeSocket.out.writeShort( b.length );
			mpeSocket.out.writeShort( controlCode_ );
			mpeSocket.out.writeInt( recNum_ );
			mpeSocket.out.write( b );

			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			//System.out.println("errno=" + errno );

		} catch (IOException e) {throw new ADBCException("MpeFile.write/update:Communication Error",this.fileName,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 ) throw new ADBCException("MpeFile.write/update:MPE File Error",this.fileName,errno);
	}

	/** Set the record specified by the passed address the
	  * current record in the file.
	  * @param recNum The record address.
	  */

	public final void point( int recNum ) throws ADBCException {

		/* The following information is sent to the server.
		 * Word  Bytes
		 * 01    01:02 "FP" calls fpoint.
		 * 02    03:04 file number.
		 * 03-04 05:08 the record number.
		 */

		short errno=0;

		short sendPacketLength = 8;

		mpeSocket = mpe3000.checkOutSocket();

		try {

			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "FP" );
			mpeSocket.out.writeShort( fileNum );
			mpeSocket.out.writeInt( recNum );

			short recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();

		} catch (IOException e) {throw new ADBCException("MpeFile.proint:Communication Error",this.fileName,errno);}

		mpe3000.checkInSocket();

		if ( errno > 0 ) throw new ADBCException("MpeFile.point:MPE File Error",this.fileName,errno);
	}

	//public void setFetchSize( int fetchSize_ ) {
	//	fetchSize = fetchSize_;
	//}

	//boolean isMpeFile() { return true; }

	//public void setKsamOptions( KsamOptions ksamOption_ ) {
	//	ksamOption = ksamOption_;
	//}

}

