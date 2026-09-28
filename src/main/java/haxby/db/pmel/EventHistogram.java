package haxby.db.pmel;

/**
 * @author William F. Haxby
 * @since 1.1.6
 */

public class EventHistogram {
	PMEL pmel;
	int minTime, maxTime;
	int maxCounts;
	double interval;
	double[] range;
	int[] counts;
	public EventHistogram( PMEL pmel, double interval ) {
		this.pmel = pmel;
		PMELEvent evt = (PMELEvent)pmel.current.get(0);
		minTime = evt.time;
		evt = (PMELEvent)pmel.current.get(pmel.current.size()-1);
		maxTime = evt.time;
		setRange( new double[] {(double)minTime, (double)maxTime }, interval);
	}
	public void setRange( double[] r, double interval) {
		range = new double[] {r[0], r[1]};
		double dr = (range[1]-range[0])/4.;
		range[0] -= dr;
		range[1] += dr;
		this.interval = interval;
		int n = (int)(2+(range[1]-range[0]) / interval);
		counts = new int[n];
		reBin();
	}
	public void reBin() {
		int n = counts.length;
		for( int i=0 ; i<n ; i++) counts[i]=0;
		maxCounts = 0;
		for( int i=0 ; i<pmel.current.size() ; i++) {
			PMELEvent e = (PMELEvent)pmel.current.get(i);
			int k = (int)Math.rint((e.time-range[0])/interval);
			if(k<0)continue;
			if(k>=n) break;
			counts[k]++;
		}
		for( int i=0 ; i<n ; i++) if(counts[i]>maxCounts) maxCounts=counts[i];
	}
	public double[] getRange() {
		return new double[] {(double)minTime, (double)maxTime };
	}
	public double[] getCurrentRange() {
		return new double[] {range[0], range[1]};
	}
	public int getMaxCounts() {
		return maxCounts;
	}
	public int getCounts(double z) {
		int k = (int)Math.rint((z-range[0])/interval);
		if(k<0 || k>=counts.length)return 0;
		return counts[k];
	}
}
