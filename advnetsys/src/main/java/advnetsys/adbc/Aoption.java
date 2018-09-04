package advnetsys.adbc;

/** This interface contains static variables that represent the
  * various Access Options for opening or creating an MPE file.
  * The variables have been named the same as the parameters that
  * are used in the MPE File command.  These variables are used in
  * setting the {@link MpeFile#setAoptions} method.
  * Use the Java OR (|) operator to combine the desired options.
  * Example:
  * <BLOCKQUOTE><PRE>
  *     MpeFile fn = new MpeFile( mpe3000 );
  *
  *     fn.setName("myFile");
  *     fn.setAoptions( Aoption.INOUT | Aoption.LOCK | Aoption.EXC );
  * </PRE></BLOCKQUOTE>
  * This example sets the Access Options to be an read/write file with 
  * exclusive access and locking enabled.
  */

public interface Aoption {

	//private static int ACCESS_MASK = 0xfff0;
	public  static int IN = 0x0;
	public  static int OUT = 0x1;
	public  static int OUTKEEP = 0x2;
	public  static int APPEND = 0x3;
	public  static int INOUT = 0x4;
	public  static int UPDATE = 0x5;
	//public  static int ACCESS_EXECUTE = 0x6;

	//private static int MULTIRECORD_MASK = 0xffef;
	public  static int NOMR = 0x0;
	public  static int MR = 0x8;

	//private static int LOCKING_MASK = 0xffdf;
	public  static int NOLOCK = 0x0;
	public  static int LOCK = 0x20;

	//private static int EXCLUSIVE_MASK = 0xff3f;
	public  static int EAR = 0x0;   // was read.
	public  static int EXC = 0x40;
	public  static int SEMI = 0x80;  // was readshare
	public  static int SHR = 0xc0;

	//private static int BUFFERING_MASK = 0xfeff;
	public  static int BUF = 0x0;
	public  static int NOBUF = 0x100;

	//private static int MULTIACCESS_MASK = 0xf9ff;
	public  static int NOMULTI = 0x0;
	public  static int MULTI = 0x200;
	public  static int GMULTI = 0x400;

	//private static int NOWAIT_MASK = 0xf7ff;

	//private static int COPYMODE_MASK = 0xefff;
	public  static int NOCOPY = 0x0;
	public  static int COPY = 0x1000;

}

