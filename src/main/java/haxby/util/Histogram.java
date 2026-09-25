package haxby.util;

public class Histogram {
	int[] counts;
	double minVal, maxVal, interval;
	int numCounts, maxCounts;
	double mode, median;
	double mean, stdev;
	public Histogram(float[] z, float interval) {
		this.interval = (double) interval;
		minVal=0;
		maxVal=0;
		numCounts = 0;
		boolean start = true;
		mean = 0;
		stdev = 0;
		for(int i=0 ; i<z.length ; i++) {
			if( Float.isNaN(z[i]) ) continue;
			numCounts++;
			mean += z[i];
			stdev += z[i]*z[i];
			if( start ) {
				minVal=(double)z[i];
				maxVal=(double)z[i];
				start=false;
			} else if( (double)z[i]>maxVal ) {
				maxVal = (double)z[i];
			} else if( (double)z[i]<minVal ) {
				minVal = (double)z[i];
			}
		}
		if( numCounts==0 ) {
			maxCounts=0;
			counts = new int[0];
			mode = 0;
			median = 0;
			return;
		}
		mean /= numCounts;
		stdev = Math.sqrt( ( stdev-mean*mean*numCounts )
				/(double)numCounts );
		minVal = Math.rint( minVal/this.interval )*this.interval;
		maxVal = Math.rint( maxVal/this.interval )*this.interval;
		int size = (int) Math.rint( (maxVal-minVal) / interval ) +2;
	System.out.println( "min:\t"+ minVal);
	System.out.println( "max:\t"+ maxVal);
		counts = new int[size];
		for(int i=0 ; i<z.length ; i++) {
			if( Float.isNaN(z[i]) ) continue;
			int k = (int)Math.rint( ((double)z[i]-minVal) / interval );
			if( k>=size || k<0 ) {
				System.out.println("out-of-bounds\t"
						+k+"\t"+size+"\t"+z[i]);
				continue;
			}
			counts[k]++;
		}
		maxCounts = 0;
		int nc = 0;
		median = minVal;
		mode = minVal;
		for( int k=0 ; k<size ; k++) {
			nc += counts[k];
			if( nc < numCounts/2 ) median = minVal+interval*k;
			if( counts[k]>maxCounts ) {
				mode = minVal+interval*k;
				maxCounts = counts[k];
			}
		}
	}
	public int getMaxCounts() {
		return maxCounts;
	}
	public int getTotalCounts() {
		return numCounts;
	}
	public int getCounts( float z ) {
		int k = (int) Math.rint( ((float)z-minVal) / interval );
		if(k<0 || k>=counts.length ) return 0;
		return counts[k];
	}
	public double[] getRange() {
		return new double[] { minVal, maxVal };
	}
	public double[] getRange(double fraction) {
		if(fraction>=1. || fraction<=0. ) return getRange();
		double test = fraction*numCounts;
		int k1 = 1;
		int n = counts[0];
		while( n<=test && k1<counts.length-1) n += counts[k1++];
		int k2 = counts.length-2;
		n = counts[counts.length-1];
		while( n<test && k2>k1 ) n += counts[k2--];
		return new double[] { k1*interval+minVal, k2*interval+minVal };
	}
	public double getMean() {
		return mean;
	}
	public double getMedian() {
		return median;
	}
	public double getMode() {
		return mode;
	}
	public double getStdDev() {
		return stdev;
	}
}
