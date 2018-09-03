package advnetsys.adbc;

/** An abstract definition of a {@link ColumnListener} interface.
  * With this abstract definition, a developer can simply extend 
  * this class and not be forced to define all classes of the 
  * interface.
  */

public abstract class AbstractColumnListener extends Object implements ColumnListener {

	public void columnPreUpdate( ColumnEvent e ) {}

	public void columnUpdated( ColumnEvent e ) {}

}

