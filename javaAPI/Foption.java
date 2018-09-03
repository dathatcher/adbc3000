package advnetsys.adbc;

/** This interface contains static variables that represent the
  * various File Options for opening or creating an MPE file.
  * The variables have been named the same as the parameters that
  * are used in the MPE Build and File commands.  These variables 
  * are used in setting the {@link MpeFile#setFoptions} method.
  * Use the Java OR (|) operator to combine the desired options.
  * Example:
  * <P><BLOCKQUOTE><PRE>
  * MpeFile fn = new MpeFile( mpe3000 );
  *		
  * fn.setName("myFile");
  * fn.setFoptions( Foption.STD | Foption.F | Foption.ASCII );
  *
  * </PRE></BLOCKQUOTE>
  * This example sets the File Options to be an ascii standard file with 
  * fixed length records.
  */

public interface Foption {

	//private static int DOMAIN_MASK = 0xfffc;
	//private static int DOMAIN_NEW = 0x0;
	//private static int DOMAIN_PERMANENT = 0x1;
	//private static int DOMAIN_TEMPORARY = 0x2;

	static int CODE_MASK = 0xfffb;

	/** A binary file */

	public  static int BINARY = 0x0;

	/** An ascii file */

	public  static int ASCII = 0x4;

	//private static int DESIGNATOR_MASK = 0xffc7;

	static int FORMAT_MASK = 0xff3f;

	/** A fixed length file. */

	public  static int F = 0x0;

	/** A variable length file. */

	public  static int V = 0x40;

	/** An undefined length file. */

	public  static int U = 0x80;
	//public  static int FORMAT_SPOOLFILE = 0xc0;

	static int CARRIAGE_MASK = 0xfeff;

	/** Carriage control characters are not specified. */

	public  static int NOCCTL = 0x0;

	/** Carriage control characters are specified. */

	public  static int CCTL = 0x100;

	//private static int LABELED_MASK = 0xfdff;

	static int FILEEQ_MASK = 0xfbff;
	public  static int FILEEQ = 0x0;
	public  static int NOFILEEQ = 0x400;

	static int FILE_MASK = 0xc7ff;
	public  static int STD = 0x0;
	public  static int KSAM = 0x800;
	public  static int RIO = 0x1000;
	public  static int KSAMXL=0x1800;
	public  static int CIR = 0x2000;
	public  static int MSG = 0x3000;
	public  static int SPOOL = 0x2800;

}

