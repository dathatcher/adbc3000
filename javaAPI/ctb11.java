package advnetsys.adbc;

import sun.io.CharToByteConverter;
import java.io.CharConversionException;

class ctb11 {

	static int convertX( CharToByteConverter ctb, char[] value, int size, 
						 byte[] result, int estLength )
						 throws CharConversionException {
		return ctb.convert(value, 0, size, result, 0, estLength);
	}

	static int flushX( CharToByteConverter ctb, byte[] result, int estLength )
					   throws CharConversionException {
		return ctb.flush(result, ctb.nextByteIndex(), estLength);
	}
}
