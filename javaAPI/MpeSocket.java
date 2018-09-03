package advnetsys.adbc;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;

class MpeSocket extends Object {

	public DataInputStream in;
	public DataOutputStream out;
	public InputStream inStream;
	public OutputStream outStream;
	Socket socket;

	public MpeSocket( String url, int port ) throws IOException{

			socket = new Socket( url, port );
			//in = new DataInputStream( new BufferedInputStream(socket.getInputStream(),30000) );
			in = new DataInputStream( socket.getInputStream() );
			out = new DataOutputStream( socket.getOutputStream() );
			inStream = socket.getInputStream();
			outStream = socket.getOutputStream();
			//socket.setReceiveBufferSize( 30000 );
	}

	public String getAddress() {

		InetAddress inetAddr = socket.getLocalAddress();
		byte[] b = inetAddr.getAddress();

		DecimalFormat decFormat = new DecimalFormat("000");

		String addr = "";

		for (int i=0;i<b.length;i++) {
			int x = b[i] & 0xff;
			addr += decFormat.format(x);
			if ( i < 3 ) addr += ".";
		}
		addr += " ";

		return addr;
	}
}
