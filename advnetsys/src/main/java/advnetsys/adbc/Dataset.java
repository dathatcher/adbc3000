package advnetsys.adbc;

import java.lang.*;
import java.util.*;
import java.io.*;

/** The Dataset class is the ADBC representation of an MPE Turbo Image
  * dataset.  A Dataset is created from {@link Database#createDataset}
  * method.
  */

public class Dataset extends Object implements DataTable {

	public static int MASTER=-1;
	public static int DETAIL=-2;
	public static int AUTOMATIC=-3;

	public static int LOCK_UNCONDITIONAL=3;
	public static int LOCK_CONDITIONAL=4;

	private String name;
	private short datasetId;   // ================== Change back to private ============
	private Database db;
	private int datasetType;
	private RowSet rowSet=null;
	//.private boolean compatibilityMode;
	private int defaultLockMode=LOCK_UNCONDITIONAL;

	Dataset() {}

	Dataset( String name, short datasetId ) {
		this.name = name;
		this.datasetId = datasetId;
	}

	void setName( String name ) {
		this.name = name;
	}

	void setDatabase( Database db ) {
		this.db = db;
	}

	void setRowSet( RowSet rowSet ) {
		this.rowSet = rowSet;
	}

	public RowSet getRowSet() { //fix here.............
		return rowSet;
	}

	/*2.0.1*/
	public boolean isTBInitOK() {
		return true;
	}

	void setType( int datasetType ) {
		this.datasetType = datasetType;
	}

	public int getType() { return datasetType; }

	public String getName() { return name; }

	void setIdentifier( short datasetId ) {
		this.datasetId = datasetId;
	}

	public short getIdentifier() { return datasetId; } //fix here...........

	/** Returns the dataset Id for this dataset.
	  */

	public short getId() {
		return datasetId;
	}
	
	//public short getDatasetId() {return datasetId;}

	//void setCompatibilityMode( boolean enabled ) {
		//this.compatibilityMode = enabled;
	//}

	//public boolean isCompatibilityMode() { return compatibilityMode; }

	public void setDefaultLockMode( int lockMode ) {
		this.defaultLockMode = lockMode;
	}

	public int getDefaultLockMode() {
		return defaultLockMode;
	}

	public void lock() throws ADBCException {

		this.lock( defaultLockMode );
	
	}

	public void lock( int lockMode ) throws ADBCException {

		db.lock( lockMode, this );

	}

	public void unLock() throws ADBCException {

		db.unLock();

	}

	public short getDataSourceId() {   //fix here..............

		return db.getDbId();

	}

	public Mpe3000 getMpe3000() {  //fix here..............

		return db.getMpe3000();  

	}

	public boolean isCriticalItemUpdateEnabled() {

		return db.isCriticalItemUpdateEnabled();

	}

	synchronized void setLastDatasetUser( long registryId ) {
		db.setLastDatasetUser( this, registryId );
	}


    synchronized long getLastDatasetUser() {
    	return db.getLastDatasetUser( this );
    }

	//.synchronized void setLastPtr( int lastPtr ) {
	//.	db.setLastPtr( this, lastPtr );
	//.}

	//.synchronized int getLastPtr() {
	//.	return db.getLastPtr( this );
	//.}

}
