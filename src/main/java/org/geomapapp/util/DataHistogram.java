package org.geomapapp.util;

public class DataHistogram extends Histogram {
	float[] data;
	public DataHistogram(float[] data, int nBin) {
		this.data = data;
		init();
		rebin(nBin, null);
	}
	void init() {
		minVal=0;
		maxVal=0;
		numCounts = 0;
		boolean start = true;
		mean = 0;
		stdev = 0;
		if (data == null) return;
		for( int k=0 ; k<data.length ; k++) {
			if( Float.isNaN(data[k]) ) continue;
			double z = (double)data[k];
			numCounts++;
			mean += z;
			stdev += z*z;
			if( start ) {
				minVal=z;
				maxVal=z;
				start=false;
			} else if( z>maxVal ) {
				maxVal = z;
			} else if( z<minVal ) {
				minVal = z;
			}
		}
		if( numCounts==0 ) throw new NullPointerException("no data points");
		mean /= numCounts;
		stdev = Math.sqrt( ( stdev-mean*mean*numCounts )
				/(double)numCounts );
		maxData = maxVal;
		minData = minVal;
	}
	public void rebin(int nBin, double[] range) {
		if(range==null) range = new double[] {minData,maxData};
		double minData = range[0];
		double maxData = range[1];
		if( maxData<=minData ) {
			counts = new int[] {numCounts};
			median = mode = minData;
			maxCounts = numCounts;
			interval = 1.;
			return;
		}
		if( nBin<2 )nBin=2;
		interval = (maxData-minData) / (nBin-1.);
		minVal = Math.rint( minData/interval )*interval;
		maxVal = Math.rint( maxData/interval )*interval;
		int size = (int) Math.rint( (maxVal-minVal) / interval ) +2;
		counts = new int[size];
		for( int k=0 ; k<data.length ; k++) {
			if( Float.isNaN(data[k]) ) continue;
			double z = (double)data[k];
			int i = (int)Math.rint( (z-minVal) / interval );
			if( i<0 || i>=size )continue;
			counts[i]++;
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
	public int getCounts( double z ) {
		int k = (int) Math.rint( (z-minVal) / interval );
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