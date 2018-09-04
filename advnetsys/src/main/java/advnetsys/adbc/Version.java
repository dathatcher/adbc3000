package advnetsys.adbc;

public class Version extends Object {

	public static String get() {
		return new String("ADBC Version 2.1.9");
	}

	public static void main(String[] args) {

		System.out.println(get());

	}
}
