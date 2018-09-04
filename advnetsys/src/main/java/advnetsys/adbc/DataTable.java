package advnetsys.adbc;

interface DataTable {

	public String getName();

	public int getType();

	public short getIdentifier();

	/*2.0.1*/
	public boolean isTBInitOK();
	
	public abstract RowSet getRowSet();

	public void setDefaultLockMode( int defaultLockMode );

	public int getDefaultLockMode();

	public void lock() throws ADBCException;

	public void lock( int lockMode ) throws ADBCException;

	public void unLock() throws ADBCException;

	public short getDataSourceId();

	public Mpe3000 getMpe3000();

}
