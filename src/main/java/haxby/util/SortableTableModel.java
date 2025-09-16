package haxby.util;

/*
 * Table that has rows that can be sorted by clicking on the corner cell
 * Used by the PetDB tables
 * Neville Shane April 2019
 */

public abstract class SortableTableModel extends XBTableModel {

	private static final long serialVersionUID = 1L;
	public SortableTableModel() {
		super();
	}
	public void sortRows() {}
}
