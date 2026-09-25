package haxby.nav;

public class IndexControlPt extends ControlPt {
	public int index;
	public ControlPt cpt;
	public IndexControlPt(ControlPt cpt, int index) {
		this.cpt = cpt;
		this.index = index;
	}
	public ControlPt getControlPt() {
		return cpt;
	}
	public double getX() {
		return cpt.getX();
	}
	public double getY() {
		return cpt.getY();
	}
	public void setLocation(double x, double y) {
		cpt.setLocation(x, y);
	}
	public String toString() {
		return "IndexControlPt["+index+", "+cpt.toString()+"]";
	}
}
