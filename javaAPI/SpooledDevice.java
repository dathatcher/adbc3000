package advnetsys.adbc;

import java.lang.*;

public class SpooledDevice extends MpeFile {

	public SpooledDevice() {
		super();
	}

	public void setSpoolerCopies( int spoolerCopies ) {
		this.spoolerCopies = spoolerCopies;
	}

	public void setOutputPriority( int outputPriority ) {
		this.outputPriority = outputPriority;
	}

}

