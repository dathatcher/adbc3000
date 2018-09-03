
package advnetsys.adbc;


import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

/** The Database class is the ADBC representation of an MPE Turbo Image
  * database.  In order to access information from a Turbo Image database, 
  * an instance of this class must be created.  This class requires an 
  * Mpe3000 connection object, the name of the database, the database password,
  * and the mode that the database is to be opened.
  */

public class Database extends Object {

	public static int MODE1 = 1;
	public static int MODE2 = 2;
	public static int MODE3 = 3;
	public static int MODE4 = 4;
	public static int MODE5 = 5;
	public static int MODE6 = 6;
	public static int MODE7 = 7;
	public static int MODE8 = 8;
       
	private String dbName="";
	private String name="";
	private short dbMode;
	private short dbError;
	private short dbId;       // The mpe assigned database id number;
	private Mpe3000 mpe3000;  // For access to the socket for hp3000 communications.
	//Hashtable lastPtrTable = new Hashtable();
	private Hashtable lastDatasetUser = new Hashtable();
	//.boolean compatibilityMode = true;
	private short sendPacketLength;
	private short recvPacketLength;
	private String password;
	private boolean criticalItemUpdateEnabled=false;
	private boolean connected=false;
      private boolean TPI = false;
      private String URL = " ";
      private String driver = " ";
      private boolean target_base_selected = false;
      private String dbuser = " ";
      private String dbpassword = " ";
      private Connection con;
      private Statement stmt;
	private Hashtable dsCache;
	private boolean dsCacheEnabled=true;

	public Database() {}
	
	public Database( String dbName, int dbMode, Mpe3000 mpe3000 ) {
	
		this.mpe3000 = mpe3000;
		//this.dbName = dbName;
		setDatabaseName( dbName );
		this.dbMode = (short) dbMode;
		dsCache = new Hashtable();
	}

	public void setDatabaseName( String dbName ) {
		this.dbName = dbName;
		if ( "".equals(name) ) setName( dbName );
	}

	public String getDatabaseName() { return dbName; }

	public void setDatabaseMode( int dbMode ) {
		this.dbMode = (short) dbMode;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	//public void setMpeConnection( String st_ ){
	//	String st = st_;
	//	return;
	//}

	//public String getMpeConnection() {return new String("hello there");}
	//public void setMpeConnection( Mpe3000 mpe3000_ ) {
	//	mpe3000 = mpe3000_;
	//}

	//public Mpe3000 getMpeConnection() {
	//	return mpe3000;
	//}

	public void setPassword( String password ) {
		this.password = password;
	}
       
	public void connect() throws ADBCException {
		connect( this.password );
	}

	private void checkConnected(String label) throws ADBCException {
		if ( ! connected ) throw new ADBCException("Database." + label + ":Database not connected",this.name,40001);
	}

	/** Notify the ADBC server component that this database is to
	  * be opened using the specified password.
	  * @param password The database password.
	  */

	public void connect( String password ) throws ADBCException {
	
                if ( TPI ) {
                       // System.out.println("TPI Enabled");
                        Intrinsic  dbiopen = new Intrinsic( mpe3000 );
	                  dbiopen.setXL( "TPIXL" );
			      dbiopen.setName( "tpiopen" );
                      	dbiopen.addParameter( "database",   "X", 26);
                        dbiopen.addParameter( "password",    "X", 16); 
                        dbiopen.addParameter( "mode",       "I", 1);
			      dbiopen.addParameter( "status",     "I", 1);
                        dbiopen.addParameter( "id", "I", 1);
                       
                        dbiopen.updateString( "database",  dbName.toUpperCase());
                        dbiopen.updateString("password", password);
                        dbiopen.updateInt("mode", dbMode);
                        dbiopen.updateInt("status", 0 );
                        dbiopen.updateInt("id", 0);
                        dbiopen.call();
                        
                       // System.out.println("Status out = " + dbiopen.getInt("status"));
                        dbError = dbiopen.getShort("status");
                        if ( dbError != 0 ) {
                             throw new ADBCException("Database.connect:TPI Error",this.name, dbError );
		        }else {
                                dbId = dbiopen.getShort("id");
                                connected = true;
                              }
                }else{
		    MpeSocket mpeSocket = mpe3000.checkOutSocket();

		    sendPacketLength = 38;

		    try {
        			mpeSocket.out.writeShort(sendPacketLength);
	         		mpeSocket.out.writeBytes("OP");         // For "Open"
	        		mpeSocket.out.writeBytes(dbName + new Spaces().createSpaces( 26 - dbName.length()));
	        		mpeSocket.out.writeBytes(password + new Spaces().createSpaces( 8 - password.length()));
		        	mpeSocket.out.writeShort(dbMode);/*+-+-+-*/
        			recvPacketLength = mpeSocket.in.readShort();
	        		dbError = mpeSocket.in.readShort();
		        	dbId = mpeSocket.in.readShort();
			//System.out.println("Database: " + recvPacketLength + " " + dbError + " " + dbId );
	        	} catch (IOException ex) {throw new ADBCIOException(mpe3000,"Database.connect:Communication Error",this.name,40000);}

		     mpe3000.checkInSocket();  // Mark it available for other threads to access.
		    }  // end else for TPI
                    
             //  Logic for database replicator  DBR-DT
             /*try {
                if ( target_base_selected ) {
                        System.out.println("Your target base has been identified all ok...");
                        Class.forName(this.driver).newInstance();

                }*/
                
             	/*} catch (Exception ex) { //throw new ADBCException("Database.connect:Communication Error SQL",this.driver,40000)
                                          ex.printStackTrace();}  */ 
            
            // DBR-DT
             
		if ( dbError != 0 ) {
			throw new ADBCException("Database.connect:Image Error",this.name, dbError );
		} else { 
			connected = true;
		}
	}

	public void close() throws ADBCException {

		byte[] bOut = new byte[10];

		ByteArray.setShort( bOut, 0, (short) 8 );  // packet length
		ByteArray.setString( bOut, 2, 2, "DC" );
		ByteArray.setShort( bOut, 4, dbId );       // database Id
		ByteArray.setShort( bOut, 6, (short) 0 );  // dataset
		ByteArray.setShort( bOut, 8, (short) 1 );  // Mode

		MpeSocket mpeSocket = mpe3000.checkOutSocket();
		byte[] bIn = new byte[4];
		try {
			mpeSocket.out.write( bOut );
			mpeSocket.in.readFully( bIn );
		} catch (IOException e) {throw new ADBCIOException(mpe3000,"Database.close:Communication Error",this.name,40000);}
		mpe3000.checkInSocket();
		short dbError = ByteArray.getShort( bIn, 2 );
		if ( dbError != 0 ) {
			throw new ADBCException("Database.close:Image Error",this.name,dbError );
		}
	}

	private void transHelper( int tranType, String logText, int logMode ) throws ADBCException {

		String tranLabel="";
		switch( tranType ) {
			case 1 : tranLabel = "beginTransaction"; break;
			case 2 : tranLabel = "rollback"; break;
			case 3 : tranLabel = "commit"; break;
		}

		if ( logText == null ) logText = "";

		//byte[] bOut = new byte[10 + logText.length()];
		byte[] bOut = new byte[12 + logText.length()];
		ByteArray.setShort ( bOut, 0, (short)(bOut.length - 2) );
		ByteArray.setString( bOut, 2, 2, "TX" );
		ByteArray.setShort ( bOut, 4, dbId );
		ByteArray.setShort ( bOut, 6, (short) tranType );
		ByteArray.setShort ( bOut, 8, (short) logMode );
		ByteArray.setShort ( bOut, 10, (short) logText.length() );
		if ( logText.length() > 0 )
			ByteArray.setString( bOut, 12, logText.length(), logText );
		
		byte[] bIn = new byte[4];

		MpeSocket mpeSocket = mpe3000.checkOutSocket();
		try {
			mpeSocket.out.write( bOut );
			mpeSocket.in.readFully( bIn );
		} catch (IOException e) {throw new ADBCIOException(mpe3000,"Database."+tranLabel+":Communication Error",this.name,40000);}

		mpe3000.checkInSocket();
		short dbError = ByteArray.getShort( bIn, 2 );
		if ( dbError != 0 ) {
			throw new ADBCException("Database."+tranLabel+":Image Error",this.name,dbError);
		}
	}

	public void beginTransaction() throws ADBCException {
		transHelper( 1, "", 1 );
	}

	public void beginTransaction( String logText ) throws ADBCException {
		transHelper( 1, logText, 1 );
	}

	public void rollback() throws ADBCException {
		transHelper( 2, "", 1 );
	}

	public void rollback( String logText ) throws ADBCException {
		transHelper( 2, logText, 1 );
	}

	public void commit() throws ADBCException {
		transHelper( 3, "", 1 );
	}

	public void commit( boolean writeXMLog ) throws ADBCException {
		if ( ! writeXMLog ) transHelper( 3, "", 1 );
		else transHelper( 3, "", 2 );
	}

	public void commit( String logText ) throws ADBCException {
		transHelper( 3, logText, 1 );
	}

	public void commit( String logText, boolean writeXMLog ) throws ADBCException {
		if ( ! writeXMLog ) transHelper( 3, logText, 1 );
		else transHelper( 3, logText, 2 );
	}

	public short getDbId() {  //2.0.1 remove public later, just testing.
		return dbId;
	}

	/** Gets the Base ID for this database Object.
	  */

	public short getId() {
		return dbId;
	}

	/** Lock the database in the specified mode.
	  * @param lockMode The desired database lock mode.
	  */

	public void lock( int lockMode ) throws ADBCException {

		this.lockHelper( lockMode, (short) 0 );

	}

	void lock( int lockMode, Dataset dataset ) throws ADBCException {

		this.lockHelper( lockMode, dataset.getIdentifier() );

	}

	private void lockHelper( int lockMode, short datasetId ) throws ADBCException {

		checkConnected("lock");
	
		MpeSocket mpeSocket = mpe3000.checkOutSocket();

		sendPacketLength = 8;

		short errno;

		try {
			mpeSocket.out.writeShort(sendPacketLength);
			mpeSocket.out.writeBytes("LO");
			mpeSocket.out.writeShort( dbId );
			mpeSocket.out.writeShort( datasetId );
			mpeSocket.out.writeShort( lockMode );
			recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			//if ( errno != 0 ) throw new ADBCException("Database.lock:Image Error",this.name,errno);
		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"Database.lock:Communication Error",this.name,40000);}

		mpe3000.checkInSocket();
		if ( errno != 0 ) throw new ADBCException("Database.lock:Image Error",this.name,errno);
	}

	/** Unlock the database. 
	  */

	public void unLock() throws ADBCException {

		checkConnected("unLock");

		MpeSocket mpeSocket = mpe3000.checkOutSocket();

		sendPacketLength = 4;

		short errno;

		try {
			mpeSocket.out.writeShort(sendPacketLength);
			mpeSocket.out.writeBytes("UL");
			mpeSocket.out.writeShort( dbId );
			recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			//if ( errno != 0 ) throw new ADBCException("Database.unLock:Image Error",this.name,errno);
		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"Database.unLock:Communication Error",this.name,40000);}

		mpe3000.checkInSocket();
		if ( errno != 0 ) throw new ADBCException("Database.unLock:Image Error",this.name,errno);
	}

	/** Produce an Enumeration of String objects that contain the
	  * names of the datasets that are in the database.
	  */

	public Enumeration getDatasetList() throws ADBCException {

		checkConnected("getDatasetList");

		MpeSocket mpeSocket = mpe3000.checkOutSocket();

		sendPacketLength = 4;

		Vector datasetEnumeration = new Vector();

		short errno=0;

		try {
			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "GD" );
			mpeSocket.out.writeShort( dbId );
			recvPacketLength = mpeSocket.in.readShort();
			errno = mpeSocket.in.readShort();
			//if ( errno != 0 ) {
			//	throw new ADBCException("Database.getDatasetList:Could not get Datasets",this.name,errno);
			//}
			int numDatasets = mpeSocket.in.readShort();
			for (int i=1;i<=numDatasets;i++) {
				byte[] ds = new byte[16];
				mpeSocket.in.readFully( ds );
				datasetEnumeration.addElement( new String( ds ).trim() );
			}
		} catch ( IOException ex ) {throw new ADBCIOException(mpe3000,"Database.getDatasetList:Communication Error",this.name,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 )
			throw new ADBCException("Database.getDatasetList:Could not get Datasets",this.name,errno);

		return datasetEnumeration.elements();
	}

	/** Set to either allow or disallow dataset caching within this Database Object.
	  * Dataset caching enhances network performance by caching datasets for reuse.
	  * If Dataset caching is not allowed, a Dataset object is created from scratch 
	  * with information that is retrieved across the network from the server.
	  * @param dsCacheEnabled Enables or disables caching. Default is enabled.
	  */

	public final void setDatasetCacheEnabled( boolean dsCacheEnabled ) {
		this.dsCacheEnabled = dsCacheEnabled;
		if ( ! dsCacheEnabled ) dsCache = null;
	}

	/** Creates an ADBC representation of one of this databases datasets.
	  * @param datasetName The name of the dataset.
	  */

	public Dataset createDataset( String datasetName ) throws ADBCException {

		checkConnected("createDataset");

		Dataset dataset=null;
		RowSet  rowSet=null;
		String dsName = datasetName.toUpperCase();

		if ( dsCacheEnabled && dsCache != null ) {
			dataset = (Dataset) dsCache.get(dsName);
			if ( dataset != null ) {
				return dataset;
			}
		}

		MpeSocket mpeSocket = mpe3000.checkOutSocket();

		sendPacketLength = 20;

		short errno=0;

		try {
			mpeSocket.out.writeShort( sendPacketLength );
			mpeSocket.out.writeBytes( "DS" );
			mpeSocket.out.writeShort( dbId );
			mpeSocket.out.writeBytes( dsName + new Spaces().createSpaces( 16 - dsName.length()));
			recvPacketLength = mpeSocket.in.readShort();
			//System.out.println("Database:createdataset:recvpacketlength=" + recvPacketLength);
			errno =  mpeSocket.in.readShort();
			if ( errno == 0 ) {

				short datasetId = mpeSocket.in.readShort(); 
				byte[] datasetType = new byte[2];
				mpeSocket.in.readFully( datasetType );  
				//System.out.println("Dataset Type: " + datasetType[0] );
				int numItems = (int) mpeSocket.in.readShort();
				dataset = new Dataset( dsName, datasetId );
				rowSet = new RowSet( dsName );

				//.rowSet.setCompatibilityMode( compatibilityMode );

				if ( datasetType[0] == 'M' ) dataset.setType(Dataset.MASTER);
				else if ( datasetType[0] == 'D' ) dataset.setType(Dataset.DETAIL);
				else if ( datasetType[0] == 'A' ) dataset.setType(Dataset.AUTOMATIC);
				else throw new ADBCException("Database.createDataset:Illegal Dataset Type:type=" + datasetType,this.name,80000 );

				//dataset.setType( datasetType[0] );
				for (int i=1;i<=numItems;i++) {
					short columnId = mpeSocket.in.readShort();
					byte[] bColumnName = new byte[16];
					mpeSocket.in.readFully( bColumnName, 0, 16 ); 
					String columnName = new String( bColumnName ).trim();
					byte[] bColumnType = new byte[2];
					mpeSocket.in.readFully( bColumnType, 0, 2 );
					String columnType = new String( bColumnType ).trim();
					int columnLength = (int) mpeSocket.in.readShort();
					int numElements = (int) mpeSocket.in.readShort();
					if ( numElements == 1 ) {
						//TurboColumn col = createColumn(dsName,columnId,columnName,
						//                          columnType,columnLength);
						//rowSet.add( col );
						rowSet.addColumn( columnName, columnType, columnId, columnLength );
					} else {
						for (int k=1;k<=numElements;k++) {
							//TurboColumn col = createColumn(dsName,columnId,
							//                          columnName + "_" + k,
							//                          columnType,columnLength);
							//rowSet.add( col );
							rowSet.addColumn( columnName + "_" + k, columnType, columnId, columnLength );
						}
					}
				}

				int numKeyColumns = (int) mpeSocket.in.readShort();
				//System.out.println("Num Key Columns: " + numKeyColumns );
				for (int i=1;i<=numKeyColumns;i++) { 
					short tmpKey = mpeSocket.in.readShort();
					rowSet.setKeyColumnInternal( tmpKey );
					//System.out.println("Key Column: " + tmpKey );
				}

				rowSet.setFinalized();

				dataset.setRowSet( rowSet );
			}
			
		} catch (IOException ex) {throw new ADBCIOException(mpe3000,"Database.createDataset:Communication Error",this.name,40000);}

		mpe3000.checkInSocket();

		if ( errno != 0 ) {
			throw new ADBCException("Database.createDataset:Invalid Dataset:Dataset=" + dsName,this.name, errno );
		}

		dataset.setDatabase( this );

		if ( dsCacheEnabled ) {
			if ( dsCache == null ) dsCache = new Hashtable();
			dsCache.put( dsName, dataset );
		}

		return dataset;
	}

	/** Creates a TurboBuffer that navigates the specified dataset.
	  * @param datasetName The name of the dataset.
	  */

	public TurboBuffer createTurboBuffer( String datasetName ) throws ADBCException {

		Dataset ds = null;

		checkConnected("createTurboBuffer");

		ds = this.createDataset( datasetName );

		return new TurboBuffer( ds );
	}

	/** Creates a TurboBuffer that navigates the specified dataset and uses the userdefined
	  * {@link RowSet RowSet}.  The user specified {@link RowSet RowSet} will override the
	  * {@link Dataset Dataset} RowSet.  This is usefull in cases where a programmer has
	  * created a number of subcolumns within an Image type X field.
	  * @param datasetName The name of the dataset.
	  * @param rowSet A user specified RowSet.
	  */

	public TurboBuffer createTurboBuffer( String datasetName, RowSet rowSet ) throws ADBCException {

		Dataset ds = null;

		checkConnected("createTurboBuffer");

		ds = this.createDataset( datasetName );

		return new TurboBuffer( ds, rowSet );
	}

	Mpe3000 getMpe3000() { 
		return mpe3000; 
	}

	public void setCriticalItemUpdateEnabled( boolean enabled ) {
		this.criticalItemUpdateEnabled = enabled;
	}

	public boolean isCriticalItemUpdateEnabled() {
		return criticalItemUpdateEnabled;
	}
        public void setTPIOpen( boolean tpi ) {
                this.TPI = tpi;
        }
        public void setTargetBase( String URL, String dbuser, String dbpassword, String driver ) throws ADBCException {
                
                this.URL = URL;
                this.driver = driver;
                this.dbuser = dbuser;
                this.dbpassword = dbpassword;
               // this.target_base_selected = true;
           try {
                System.out.println("Your target base has been identified all ok...");
                Class.forName(this.driver).newInstance();
                con = DriverManager.getConnection(URL, dbuser, dbpassword);

                stmt = con.createStatement();
                              
             	} catch (Exception ex) { throw new ADBCException("Database.connect:Communication Error SQL",this.driver,40099);}
                                          //ex.printStackTrace();}   
            

             // Open validate ect.  
        
        }       
        public void replicateImageTables() throws ADBCException {
                
                try {
                	Enumeration en = this.getDatasetList();
			while( en.hasMoreElements() ) {
                                String sql_command;
                                String rep_set = (String) en.nextElement();
                                rep_set = rep_set.trim();
			//	System.out.println("Dataset: "+ rep_set);
                                TurboBuffer tb = null;
                                tb = this.createTurboBuffer(rep_set);
                                tb.setColumns("@");
                                int col_index = tb.getNumColumns();
                                int col_counter = 1;
                                int col_counter2 =0;
                                rep_set = rep_set.replace('-','_');
                                sql_command = "create table "+rep_set + "(";
                                while ( col_counter <= col_index ) {
                                    String col_final = tb.getColumnName(col_counter);
                                    col_final = col_final.replace('-','_');
                                    col_counter2 = col_counter - 1;
                                    String col_type = tb.getColumnType(col_counter2);
                                    int col_length = tb.getColumnLength(col_counter);
                                    //String col_type = "X";
                                    if (col_type.equals("X")) {
                                            col_type = "    CHAR(" + col_length + ")";
                                    }else{ col_type = "     DECIMAL(" + col_length + ")"; }
                                    sql_command = sql_command + " ";
                                    sql_command = sql_command + col_final + col_type + ",";
                                    
                                   // System.out.println("Number of Columns = "+ col_final + tb.getNumColumns());
                                    col_counter++;
                                  }
                                  int find_last_comma = sql_command.lastIndexOf(',');
                                  if ( find_last_comma > 0 ) {
                                          sql_command = sql_command.substring(0,find_last_comma) + " ) ";
                                     }
                                  System.out.println("sql = "+sql_command); 
                                  
                                  stmt.executeUpdate(sql_command);                       
                      }
                        

                      

                } catch (ADBCException ex) {throw new ADBCException("Database.replcateImageTable:Communication Error",this.name,80034);}
                  catch (SQLException ex) {//throw new SQLException("Database.replcateImageTable:Communication Error",this.name,80034);
                                            ex.printStackTrace();}
    
        }
       public void replicateImageTable(String tablename) {
                
        }
	synchronized void setLastDatasetUser( Dataset dataset, long registryId ) {
		lastDatasetUser.put( dataset.getName(), new Long( registryId ) );
	}

	synchronized long getLastDatasetUser( Dataset dataset ) {
		Long l = (Long) lastDatasetUser.get( dataset.getName() );
		if ( l == null ) return 0;
		return l.longValue();
	}

}

