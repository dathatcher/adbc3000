package advnetsys.adbc;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/** SpoolId is an ADBC representation of a spool file's properties on an HP3000
  * server.  A SpoolId object can be passed into the constructor for a
  * {@link SpoolFile} object.  The SpoolFile object will be automatically
  * set with the file name being "O&lt;xxx&gt;.out.hpspool".
  */

public class SpoolId extends Object {

	private Mpe3000 mpe3000=null;

	private String spoolId;
	private String jobNum;
	private String fileDes;
	private String pri;
	private String copies;
	private String dev;
	private String state;
	private String rspfn;
	private String owner;
	private String formId;
	private String jobName;
	private String copSrm;
	private String sectors;
	private String recs;
	private String pages;
	private Date   date;

	SimpleDateFormat dtFormat = new SimpleDateFormat( "MM/dd/yy hh:mm" );


	/** SpoolId constructor.
	  */

	SpoolId() {}

	SpoolId( Mpe3000 mpe3000 ) {
		this.mpe3000 = mpe3000;
	}

	Mpe3000 getMpe3000() {
		return this.mpe3000;
	}

	/** Return the spool id 
	  */

	public String getSpoolId() {
		return this.spoolId;
	}

	/** Return the spool file owners job/session number.
	  */

	public String getJobNum() {
		return this.jobNum;
	}
	
	/** Return the spool files File Descriptor.
	  */

	public String getFileDes() {
		return this.fileDes;
	}

	/** Return the spool files output priority.
	  */

	public String getPri() {
		return this.pri;
	}

	/** Return the spool files device number. 
	  */

	public String getDev() {
		return this.dev;
	}

	public String getState() {
		return this.state;
	}

	public String getRSPFN() {
		return this.rspfn;
	}

	/** Return the spool file owner's user.account.
	  */

	public String getOwner() {
		return this.owner;
	}

	public String getFormId() {
		return this.formId;
	}

	/** Return the spoolfiles job name.
	  */

	public String getJobName() {
		return this.jobName;
	}

	public String getCopSrm() {
		return this.copSrm;
	}

	/** Return the size, in sectors, of this spool file.
	  */

	public String getSectors() {
		return this.sectors;
	}

	public String getRecs() {
		return this.recs;
	}

	public String getPages() {
		return this.pages;
	}

	/** Return the date this spool file was created.
	  */

	public Date getDate() {
		return this.date;
	}

	void parse( byte[] b, int offset ) {

		String dateSt=null;

		//System.out.println( new String( b, offset, 104 ) );

		spoolId = new String( b, offset, 6).trim() ;
		//offset += 6
		jobNum = new String( b, offset += 6, 6).trim(); 
		//offset += 6
		fileDes = new String( b, offset += 6, 8).trim(); 
		//offset += 8
		pri = new String( b, offset += 8, 2).trim(); 
		//offset += 2
		copies = new String( b, offset += 2, 5).trim(); 
		//offset += 5
		dev = new String( b, offset += 5, 8).trim(); 
		//offset += 8
		state = new String( b, offset += 8, 6).trim();   
		//offset += 1
		rspfn = new String( b, offset += 6, 5).trim(); 
		//offset += 5
		owner = new String( b, offset += 5, 17).trim();
		//offset += 17
		formId = new String( b, offset += 17, 8).trim(); 
		//offset += 8
		jobName = new String( b, offset += 8, 8).trim(); 
		//offset += 8
		copSrm = new String( b, offset += 8, 5).trim(); 
		//offset += 5
		sectors = new String( b, offset += 5, 8).trim(); 
		//offset += 8
		recs = new String( b, offset += 8, 6).trim(); 
		//offset += 6
		pages = new String( b, offset += 6, 5).trim(); 
		//offset += 5
		dateSt = new String( b, offset += 5, 14);
		//offset += 14			
		try {
			date = dtFormat.parse( dateSt );
		} catch (ParseException e) {date = new Date( 0L );}

		//System.out.println("spoolId="+spoolId+" jobNum=" + jobNum );
	}

	public String toString() {

		return (
			"SpoolId: " + spoolId +
			" JobNum: " + jobNum +
			" FileDes: " + fileDes +
			" Pri: " + pri +
			" Copies: " + copies + 
			" Dev: " + dev +
			" State: " + state + 
			" RSPFN: " + rspfn +
			" Owner: " + owner +
			" FormId: " + formId +
			" JobName: " + jobName +
			" CopSrm: " + copSrm +
			" Sect: " + sectors +
			" recs: " + recs + 
			" Pages: " + pages +
			" Date: " + dtFormat.format( date )
		);
	}

}
