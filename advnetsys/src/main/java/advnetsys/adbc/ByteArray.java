package advnetsys.adbc;

import java.lang.*;
//import sun.io.ByteToCharConverter;
//import sun.io.CharToByteConverter;
import java.nio.*;
import java.io.CharConversionException;

public class ByteArray extends Object {

	protected static int JDK11=1;
	protected static int JDK12=2;

	protected static ByteToCharConverter btc=null;
	protected static CharToByteConverter ctb=null;

	protected static int jdkVersion=0;

	public ByteArray() {}

	public static int getInt( byte[] b, int offset ) {
		int l = (b[offset] & 0xFF);
		for (int i=offset+1;i<=offset+3;i++) 
			l = ( l << 8 ) | ( b[i] & 0xFF );
		return l;
	}

	public static int getInt( byte[] b ) {
		return getInt( b, 0 );
	}

	public static void setInt( byte[] b, int offset, int value ) {
		b[offset] = (byte)((value >>> 24) & 0xFF);
		b[offset+1] = (byte)((value >>> 16) & 0xFF);
		b[offset+2] = (byte)((value >>> 8) & 0xFF);
		b[offset+3] = (byte)( value & 0xFF );
	}

	public static void setInt( byte[] b, int value ) {
		setInt( b, 0, value );
	}

	public static long getLong( byte[] b, int offset ) {
		long l = (b[offset] & 0xFF);
		for (int i=offset+1;i<=offset+7;i++) l = ( l << 8 ) | (b[i] & 0xFF);
		return l;
	}

	public static long getLong( byte[] b ) {
		return getLong( b, 0 );
	}

	public static void setLong( byte[] b, int offset, long value ) {
		b[offset] = (byte)((value >>> 56) & 0xFF);
		b[offset+1] = (byte)((value >>> 48) & 0xFF);
		b[offset+2] = (byte)((value >>> 40) & 0xFF);
		b[offset+3] = (byte)((value >>> 32) & 0xFF);
		b[offset+4] = (byte)((value >>> 24) & 0xFF);
		b[offset+5] = (byte)((value >>> 16) & 0xFF);
		b[offset+6] = (byte)((value >>> 8) & 0xFF);
		b[offset+7] = (byte)( value & 0xFF );
	}

	public static void setLong( byte[] b, long value ) {
		setLong( b, 0, value );
	}

	public static short getShort( byte[] b, int offset ) {
		short l = (short) (b[offset] & 0xFF);
		l = (short) (( l << 8 ) | ( b[offset+1] & 0xFF ));
		return l;
	}

	public static short getShort( byte[] b ) {
		return getShort( b, 0 );
	}

	public static void setShort( byte[] b, int offset, short value ) {
		b[offset] = (byte)((value >>> 8) & 0xFF);
		b[offset+1] = (byte)( value & 0xFF );
	}

	public static void setShort( byte[] b, short value ) {
		setShort( b, 0, value );
	}

	public static double getDouble( byte[] b, int offset ) {
		long l = (b[offset] & 0xFF);
		for (int i=offset+1;i<=offset+7;i++) l = ( l << 8 ) | (b[i] & 0xFF);
		return Double.longBitsToDouble(l);
	}

	public static double getDouble( byte[] b ) {
		return getDouble( b, 0 );
	}

	public static void setDouble( byte[] b, int offset, double value ) {
		setLong( b, offset, Double.doubleToLongBits( value ) );
	}

	public static void setDouble( byte[] b, double value ) {
		setDouble( b, 0, value );
	}

	public static float getFloat( byte[] b, int offset ) {
		int l = (b[offset] & 0xFF);
		for (int i=offset+1;i<=offset+3;i++) 
			l = ( l << 8 ) | ( b[i] & 0xFF );
		return Float.intBitsToFloat( l );
	}

	public static float getFloat( byte[] b ) {
		return getFloat( b, 0 );
	}

	public static void setFloat( byte[] b, int offset, float value ) {
		setInt( b, offset, Float.floatToIntBits( value ) );
	}

	public static void setFloat( byte[] b, float value ) {
		setFloat( b, 0, value );
	}

	public static double getDoubleCM( byte[] b, int offset ) {
		long l = (b[offset] & 0xFF);
		
		for (int i=offset+1;i<=offset+7;i++) l = ( l << 8 ) | (b[i] & 0xFF);

		if ( l == 0 ) return 0;  // No conversion necessary.

		/* Shift two bits to right to increase the exponent field from 9(hp) to
		   11 IEEE 754. */
		   
		l = ( l >> 2 );

		/* Previous bit shift was a signed shift, so upper two bits of the exponent
		   which carried the sign with it. */
		   
		l = ( l & 0x9FFFFFFFFFFFFFFFL );

		/* the bias for hp floating point is 256, where IEEE754 is 1023. Add in the
		   difference, which happens to be 767.  We must add it to the exponent field
		   though so the result is the following hex number. */

		l += 0x2FF0000000000000L;

		return Double.longBitsToDouble( l );
	}

	public static double getDoubleCM( byte[] b ) {
		return getDoubleCM( b, 0 );
	}

	public static void setDoubleCM( byte[] b, int offset, double value ) {
	    
	    if ( value == -0.0 ) value = 0.0;
	    
		long l = Double.doubleToLongBits( value );

		boolean isNegative = ( l < 0 );

		if ( l != 0 ) { //2.0.1

			l -= 0x2FF0000000000000L;

			l = ( l << 2 );

			if ( isNegative ) l = l | 0x8000000000000000L;
		
		}

		setLong( b, offset, l );

	}

	public static void setDoubleCM( byte[] b, double value ) {
		setDoubleCM( b, 0, value );
	}

	public static double getFloatCM( byte[] b, int offset ) {
		long l = (b[offset] & 0xFF);
		for (int i=offset+1;i<=offset+3;i++) 
			l = ( l << 8 ) | ( b[i] & 0xFF );

		l = (l << 32);

		if ( l == 0 ) return 0;  // No conversion necessary.

		/* Shift two bits to right to increase the exponent field from 9(hp) to
		   11 IEEE 754. */
		   
		l = ( l >> 2 );

		/* Previous bit shift was a signed shift, so upper two bits of the exponent
		   which carried the sign with it. */
		   
		l = ( l & 0x9FFFFFFFFFFFFFFFL );

		/* the bias for hp floating point is 256, where IEEE754 is 1023. Add in the
		   difference, which happens to be 767.  We must add it to the exponent field
		   though so the result is the following hex number. */

		l += 0x2FF0000000000000L;

		return Double.longBitsToDouble( l );
	}

	public static double getFloatCM( byte[] b ) {
		return getFloatCM( b, 0 );
	}

	public static float getFloatCMF( byte[] b, int offset ) {
		return (float) getFloatCM( b, offset );
	}

	public static float getFloatCMF( byte[] b ) {
		return (float) getFloatCM( b );
	}

	public static void setFloatCM( byte[] b, int offset, double value ) {
	    
	    if ( value == -0.0 ) value = 0.0;
	    
		long l = Double.doubleToLongBits( value );
		
		boolean isNegative = ( l < 0 );

		if ( l != 0 ) {  //2.0.1

			l -= 0x2FF0000000000000L;

			l = ( l << 2 );

			if ( isNegative ) l = l | 0x8000000000000000L;

		}

		int i = (int) ( l >> 32 );

		setInt( b, offset, i );
	}

	public static void setFloatCM( byte[] b, double value ) {
		setFloatCM( b, 0, value );
	}

	public static void setFloatCMF( byte[] b, int offset, float value ) {
		setFloatCM( b, offset, value );
	}

	public static void setFloatCMF( byte[] b, float value ) {
		setFloatCM( b, (float) value );
	}

	public static long getPacked( byte[] b, int offset, int size ) {

		long packedValue=0;

		int ln = size + offset - 1;

		byte sign = (byte) ( b[ln] & 0xF );

		packedValue = (long) ((byte) ((b[ln] >>> 4) & 0xF) );

		long i=10;

		while ( ln > offset ) {
			ln--;
			packedValue += ((long) ((byte) (b[ln] & 0xF))) * i;
			i*=10;
			packedValue += ((long) ((byte) ((b[ln] >>> 4) & 0xF)) ) * i;
			i*=10;
		}

		if ( sign == 0xD ) packedValue *= -1;

		return packedValue;
	}

	public static long getPacked( byte[] b ) {
		return getPacked( b, 0, b.length );
	}

	public static void setPacked( byte[] b, int offset, int size, long value ) {
		byte tmpBuf;
		int ln = size + offset - 1;

		long packedValue = value;
		if ( packedValue < 0 ) {
			b[ln] = 0xD;
			packedValue *= -1;
		}
		else b[ln] = 0xC;

		/* Convert the base 10 number to hexadecimal for Cobol's comp-3 */

		tmpBuf = (byte) ( packedValue % 10);
		b[ln] = (byte) ( b[ln] | ( tmpBuf<<4 ) );
		
		packedValue /= 10;
		while ( packedValue > 0 && ln > 0 ) {
			ln--;
			b[ln] = (byte) ( packedValue % 10 );
			packedValue /= 10;
			tmpBuf = (byte) ( packedValue % 10 );
			tmpBuf = (byte) (tmpBuf<<4);
			b[ln] = (byte) ( b[ln] | tmpBuf );
			packedValue /= 10;
		}

		/* Zero out the rest of the byte array */

		while ( ln > offset ) {
			ln--;
			b[ln] = (byte) 0;
		}
	}

	public static void setPacked( byte[] b, long value ) {
		setPacked( b, 0, b.length, value );
	}

	/*
		ZONED LAST DIGIT

		digit	positive	negative 
		0		{			} 
		1		A			J 
		2		B			K 
		3		C			L 
		4		D			M 
		5		E			N 
		6		F			O 
		7		G			P 
		8		H			Q 
		9		I			R 

		Diff:	16			25
	*/

	public static long getZ( byte[] b, int offset, int size ) {

		long zValue=0;

  		int ln = size + offset;
		long i = 1;
		long sign = 1;

		while ( ln > offset ) {

			ln--;

			if ( ln == size + offset - 1 && b[ln] == '{' ) {

				zValue *= 10;
				i *= 10;

			} else if ( ln == size + offset - 1 && b[ln] == '}' ) {

				sign = -1;
				zValue *= 10;
				i *= 10;

			} else if ( ln == size + offset - 1 && b[ln] >= 'J' ) {

				/* Negative Signed */
				sign = -1;
				zValue += ((b[ln] & 0xFF) - '0' - 25) * i;
				i *= 10;

			} else if ( ln == size + offset - 1 && b[ln] >= 'A' ) {

				/* Positive Signed */
				zValue += ((b[ln] & 0xFF) - '0' - 16) * i;
				i *= 10;

			} else if ( b[ln] < '0' || b[ln] > '9' ) ln = 0;   /* Terminate the loop. */
			else {
				zValue += ((b[ln] & 0xFF) - '0') * i;
				i *= 10;
			}
		}

		return zValue * sign;
	}


	public static long getZ( byte[] b ) {
		return getZ( b, 0, b.length );
	}

/*	public static void setZ( byte[] b, int offset, int size, long value ) {
		long zValue = value;
		int ln = offset + size - 1;

		while ( zValue != 0 && ln >= offset ) {
			b[ln] = (byte)(( zValue % 10 ) + '0');
			zValue /= 10;
			ln--;
		}
		while ( ln >= offset ) {
			b[ln] = (byte) ' ';
			ln--;
		}
	}*/

// Fixed by DT via Evan Nordstrom bug placing incorrect data in field

        public static void setZ( byte[] b, int offset, int size, long value ) {
		long zValue = Math.abs(value);
		int ln = offset + size - 1;

		while ( zValue != 0 && ln >= offset ) {
                   b[ln] = (byte)(( zValue % 10 ) + '0');
			if ( value < 0 && ln == offset + size - 1 ) b[ln] += 25;
			zValue /= 10;
			ln--;
		}
		while ( ln >= offset ) {
			b[ln] = (byte) '0';
			ln--;
		}
	}



	public static void setZ( byte[] b, long value ) {
		setZ( b, 0, b.length, value );
	}

	private static String btcConv( byte[] b, int offset, int size ) {

		int numCharsEst = btc.getMaxCharsPerByte() * size;
		char[] c = new char[numCharsEst];

		int numChars=0;

		try {
			numChars = btc.convert(b, offset, offset+size, c, 0, numCharsEst);
			numChars += btc.flush(c, btc.nextCharIndex(), numCharsEst);
		} catch (CharConversionException x) {
			numChars = btc.nextCharIndex();
		}

		if (numChars < numCharsEst) {
			char[] cTrimmed = new char[numChars];
			System.arraycopy(c, 0, cTrimmed, 0, numChars);
			c = cTrimmed;
		}

		return new String( c );
	}

	public static String getString( byte[] b, int offset, int size ) {
		int newSize = size;

		while ( (newSize + offset - 1 >= offset) && (b[ newSize + offset - 1 ] <= ' ') ) 
			newSize--;

		if ( newSize <= 0 ) return new String( "" );

		if ( btc != null ) return ByteArray.btcConv( b, offset, newSize );

		return new String( b, offset, newSize );

		//.StringBuffer sb = new StringBuffer( newSize + 1 );

		//char[] c = new char[newSize];
	
		//int k=0;
		//.for (int i=offset;i<=offset + newSize - 1;i++) {
			//sb.setCharAt( k, (char) b[i] );
			//.sb.append( (char) b[i] );
			//c[k] = (char) b[i];
			//k++;
		//.}

		//return new String( c );
		//return sb.toString();
	}

    private static byte[] ctbConv( String st ) {
    	int size = st.length();
    	char[] value = st.toCharArray();
		ctb.reset();
		int estLength = ctb.getMaxBytesPerChar() * size;
		byte[] result = new byte[estLength];
		int length = 0;
		try { 
			//length += ctb.convertAny(value, offset, (offset + size), result, 0, estLength);
			//.length += ctb.convertAny(value, 0, size, result, 0, estLength);
			//.length += ctb.flushAny(result, ctb.nextByteIndex(), estLength);
			if ( jdkVersion == 0 ) {
				String version=System.getProperty("java.version");
				if ( version.startsWith("1.1") ) jdkVersion = JDK11;
				else if ( version.startsWith("1.2") || 
				          version.startsWith("1.3") ) jdkVersion = JDK12;
			}

			if ( jdkVersion == JDK11 ) {
				length += ctb11.convertX(ctb, value, size, result, estLength);
				length += ctb11.flushX(ctb, result, estLength);
			} else if ( jdkVersion == JDK12 ) {
				length += ctb12.convertX(ctb, value, size, result, estLength);
				length += ctb12.flushX(ctb, result, estLength);
			} else {
			}

			//length += ctb.convert(value, 0, size, result, 0, estLength);
			//length += ctb.flush(result, ctb.nextByteIndex(), estLength);
		} catch (CharConversionException e) {
			throw new InternalError("Converter malfunction: " +
						ctb.getClass().getName());
		}

		if (length < estLength) {
			// A short format was used:  Trim the byte array.
			byte[] trimResult = new byte[length];
			System.arraycopy(result, 0, trimResult, 0, length);
			return trimResult;
		}
		else {
			return result;
		}
    }

	public static void setString( byte[] b, int offset, int size, String value ) {
		byte[] btmp = null;
		if ( ctb != null ) btmp = ctbConv( value );
		else btmp = value.getBytes();

		//byte[] btmp = value.getBytes();

		int newSize = ( btmp.length > size ? size : btmp.length );

		//System.out.println("ByteArray::newSize="+newSize+" offset="+offset+" b.length="+b.length+" btmp.length="+btmp.length );

		System.arraycopy( btmp, 0, b, offset, newSize );

		// For jdk1.2 - use Array.fill()

		while ( newSize < size ) {
			newSize++;
			b[offset + newSize - 1] = (byte) ' ';
		}
	}
}
