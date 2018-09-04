package advnetsys.adbc;

import java.util.*;

class CircularBuffer extends Object {

	public static int FORWARD_LOAD=1;
	public static int BACKWARD_LOAD=2;

	int bufferSize;
	int frontPtr=1;
	int backPtr=0;
	int currPtr=1;
	int forwardDelta=0;
	int backDelta=0;
	//...boolean containsRecords=false; 

	int loadDirection=FORWARD_LOAD;
	Object[] buffer;

	public CircularBuffer( int bufferSizeParm ) {
		bufferSize = bufferSizeParm + 2;
		buffer = new Object[bufferSize];
	}

	//public void setBufferSize( int bufferSizeParm ) {
	//	bufferSize = bufferSizeParm;
	//	buffer = null;
	//	buffer = new Object[bufferSize];
	//	containsRecords = false;
	//}

	public void setLoadDirection( int loadDirectionParm ) {
		loadDirection = loadDirectionParm;
	}

	public int inc( int value, int delta ) {
		return ( value + delta + bufferSize ) % bufferSize ;
	}

	public synchronized void addElement( Object objParm ) {

		//System.out.println("add:frontPtr=" + frontPtr + " backPtr=" + backPtr + " currPtr=" + currPtr );
		if ( loadDirection == FORWARD_LOAD ) {
			buffer[frontPtr] = objParm;
			if ( frontPtr == currPtr ) forwardDelta = -1;
			frontPtr = inc( frontPtr, 1 );
			buffer[frontPtr] = null;/*+-+-+-*/
			if ( frontPtr == backPtr ) {
				backPtr = inc( backPtr, 1 );
				buffer[backPtr] = null;
			}
			//System.out.println("added frontPtr=" + frontPtr );
			//frontPtr = ( frontPtr + 1 ) % bufferSize;
			//if ( frontPtr == backPtr ) backPtr = ( backPtr + 1 ) % bufferSize;
			//buffer[frontPtr] = null;
			//buffer[frontPtr] = objParm;
			//if ( !containsRecords ) backPtr = frontPtr;
		} else {
			buffer[backPtr] = objParm;
			if ( backPtr == currPtr ) backDelta = 1;
			backPtr = inc( backPtr, -1 );
			buffer[backPtr] = null;
			if ( backPtr == frontPtr ) {
				frontPtr = inc( frontPtr, -1 );
				buffer[frontPtr] = null;
			}
			//backPtr = ( backPtr - 1 + bufferSize ) % bufferSize;
			//if ( frontPtr == backPtr ) frontPtr = ( frontPtr - 1 + bufferSize ) % bufferSize;
			//buffer[backPtr] = null;
			//buffer[backPtr] = objParm;
			//if ( !containsRecords ) frontPtr = backPtr;
		}
		//if ( ! containsRecords ) containsRecords = true;
	}

	private int recNo( int ptr ) {

		if ( buffer[ptr] != null ) return ByteArray.getInt( (byte[]) buffer[ptr], 2 );

		return -32767;
	}

	private int index( int ptr ) {
		//return ( ( backPtr + ptr ) % bufferSize );
		return ( ( backPtr + ptr + 1 ) % bufferSize );
	}

	private int revIndex( int ptr ) {
		//return ( ( ptr - backPtr + bufferSize ) % bufferSize );
		return ( ( ptr - backPtr + bufferSize - 1 ) % bufferSize );
	}

	int size() {
		//if ( !containsRecords ) return 0;
		//return ( ( frontPtr - backPtr + 1 ) + bufferSize ) % bufferSize ;
		return ( ( frontPtr - backPtr - 1 ) + bufferSize ) % bufferSize ;
	}

	public synchronized void clipCache( int recNo ) {

		//if ( this.isEmpty() ) return;
		if ( this.size() >= 1 ) return;

		int frontRec = this.recNo( inc( frontPtr, -1 ) );
		int backRec = this.recNo( inc( backPtr, 1 ) );
		int currRec = this.recNo( currPtr );

		if ( recNo < backRec || recNo > frontRec ) return;

		if ( recNo >= backRec && recNo < currRec ) {
			int i = 0;
			while ( recNo( index(i) ) <= recNo ) {
				buffer[index(i)] = null;
				i++;
			}
			//backPtr = index( i );
			backPtr = index( i - 1 );
		} else if ( recNo > currRec && recNo <= frontRec ) {
			//int i = revIndex( frontPtr );
			int i = this.size() - 1;
			while ( recNo( index(i) ) >= recNo ) {
				buffer[index(i)] = null;
				i--;
			}
			//frontPtr = index( i );
			frontPtr = index( i + 1 );
		}
	}

	public void setElement( Object objParm ) {

		buffer[currPtr] = null;
		buffer[currPtr] = objParm;

	}

	public void removeElement() {

		buffer[currPtr] = null;

		int currIndex = revIndex( currPtr );

		for (int i=currIndex;i<=this.size()-2;i++) 
			buffer[index(i)] = buffer[index(i + 1)];
		
		//...buffer[frontPtr] = null;

		frontPtr = inc( frontPtr, -1 );

		buffer[frontPtr] = null;

		forwardDelta = -1;

	}

	public boolean currentOnFront() {
		return ( currPtr == frontPtr || currPtr == inc( frontPtr, -1 - forwardDelta ) );
	}

	public boolean currentOnBack() {
		return ( currPtr == backPtr || currPtr == inc( backPtr, 1 ) );
	}

	public synchronized Object getObject() {
		return buffer[currPtr];
	}

	public synchronized boolean isNextOK() {
		backDelta = 0;
		if ( currPtr == frontPtr ) return false;
		currPtr = inc( currPtr, 1 + forwardDelta );
		forwardDelta = 0;
		return ! ( currPtr == frontPtr );
	}

	public synchronized boolean isPrevOK() {
		forwardDelta = 0;
		if ( currPtr == backPtr ) return false;
		currPtr = inc( currPtr, -1 + backDelta );
		backDelta = 0;
		return ! ( currPtr == backPtr );
	}

	synchronized int getPerimeterRecordNumber() {
		if ( currPtr == frontPtr ) 
			return this.recNo( inc( frontPtr, -1 ) );
		else if ( currPtr == backPtr ) 
			return this.recNo( inc( backPtr, 1 ) );
		else 
			throw new IllegalArgumentException("CircularBuffer::getPerimeterRecordNumber...Not on back or front record.");
	}

	public Enumeration elements() {
		return new BufferEnumerator();
	}

	class BufferEnumerator implements Enumeration {

		int count=0;
		boolean enumComplete=false;

		BufferEnumerator() {
			//count = backPtr;
			//count = backPtr - 1;
			count = backPtr;
		}

		public boolean hasMoreElements() {
			//if ( ! containsRecords ) return false;
			if ( size() == 0 ) return false;
			return ! enumComplete;  //2.0.1 Must negate enumComplete on return.
		}

		public Object nextElement() {
			Object tmp = null;
			if ( size() > 0 && ! enumComplete ) {
				count = ( count + 1 ) % bufferSize;
				tmp = buffer[count];
				if ( inc( count, 1 ) == frontPtr ) enumComplete = true;
				//else count = ( currPtr + 1 ) % bufferSize;
			}
			return tmp;
		}

		public void setElement( Object obj ) {
			buffer[count] = obj;
		}
	
		//public Object nextElement() {
		//	Object tmp = null;
		//	if ( containsRecords && ! enumComplete ) {
		//		tmp = buffer[count];
		//		if ( count == frontPtr ) enumComplete = true;
		//		else count = ( currPtr + 1 ) % bufferSize;
		//	}
		//	return tmp;
		//}
	}
}


