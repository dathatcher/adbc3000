package advnetsys.adbc;

import java.util.EventListener;

/** An abstract definition of a {@link RowListener} interface.
  * With this abstract definition, a developer can simpley extend
  * this class and not be forced to define all classes of the
  * interface.
  */

public abstract class AbstractRowListener extends Object implements RowListener {

	public void rowPreInsert( RowEvent e ) {}

	public void rowInserted( RowEvent e ) {}

	public void rowPreDelete( RowEvent e ) {}

	public void rowDeleted( RowEvent e ) {}

	public void rowPreUpdate( RowEvent e ) {}

	public void rowUpdated( RowEvent e ) {}

	public void rowPreRefresh( RowEvent e ) {}

	public void rowRefreshed( RowEvent e ) {}

	public void rowCurrentChanged( RowEvent e ) {}

	public void rowValidate( RowEvent e ) {}

}

