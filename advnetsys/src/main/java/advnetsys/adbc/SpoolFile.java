package advnetsys.adbc;

/** A SpoolFile is an ADBC representation of a spool file on an HP3000
  * server.  If constructed with the {@link SpoolId} constructor, the
  * name of the file is automatically created as "O&lt;xxx&gt;.out.hpspool".
  * A {@link RowSet} is also automatically created with one column of type
  * "V"(variable length column) and a size of 133 bytes.  This {@link RowSet}
  * can be reset with the ancestor class method {@link MpeFile#setRowSet}.
  * The column name inside of the defined RowSet is "data".
  */

public class SpoolFile extends MpeFile {

	public SpoolFile() {
		super();
		this.createRowSet();
	}

	public SpoolFile( Mpe3000 mpe3000 ) {
		super( mpe3000 );
		this.createRowSet();
	}

	public SpoolFile( SpoolId spoolId ) {
		this( spoolId.getMpe3000() );
		this.createRowSet();
		this.setName( spoolId.getSpoolId() + ".out.hpspool" );
	}

	private void createRowSet() {
		RowSet rowSet = new RowSet();
		rowSet.addColumn( "data", "V", 133 );
		this.setRowSet( rowSet );
	}
}
