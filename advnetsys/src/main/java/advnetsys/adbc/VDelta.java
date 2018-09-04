package advnetsys.adbc;
import java.math.*;

class VDelta {

	public static double parseDouble( String value ) throws NumberFormatException {

		return Double.valueOf( value ).doubleValue();

	}

	public static float parseFloat( String value ) throws NumberFormatException {

		return Float.valueOf( value ).floatValue();
	
	}

	public static BigInteger unscaledValue( BigDecimal value ) {

		if ( value.scale() == 0 ) return value.toBigInteger();
		return value.setScale( 0 ).toBigInteger();
	}

}



