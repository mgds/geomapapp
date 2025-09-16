package haxby.dig;

import javax.swing.*;

public class DigListModel extends AbstractListModel {
	Digitizer dig;
	public DigListModel(Digitizer dig) {
		this.dig = dig;
	}
	public int getSize() {
		if( dig.objects.size()==0 )return 1;
		return dig.objects.size();
	}
	public Object getElementAt( int index ) {
		if( dig.objects.size()==0 || index >= dig.objects.size() ) return "no objects";
		return dig.objects.get(index);
	}
	public void objectRemoved() {
		int i = dig.objects.size()-1;
		fireContentsChanged( this, 0, i );
	}
	public void objectAdded() {
		int i = dig.objects.size()-1;
		fireIntervalAdded( this, i, i );
	}
	public void contentsChanged() {
		fireContentsChanged( this, 0, dig.objects.size()-1 );
	}
}