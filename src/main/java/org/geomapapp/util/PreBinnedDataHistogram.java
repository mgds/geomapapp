package org.geomapapp.util;

public class PreBinnedDataHistogram extends Histogram {
	private int[] data;
	public PreBinnedDataHistogram(int[] data) {
		this.data = data;
		init();
		rebin(data.length, null);
	}
	void init() {
		minVal=0;
		maxVal=data.length;
		numCounts = 0;

		mean = 0;
		stdev = 0;
		for( int k=0 ; k<data.length ; k++) {
			int count = data[k];
			numCounts += count;
			mean += k * count;
			stdev += k*k * count;
		}

		if( numCounts==0 ) throw new NullPointerException(
					"no data points");

		mean /= numCounts;
		stdev = Math.sqrt( ( stdev-mean*mean*numCounts )
				/(double)numCounts );
		maxData = maxVal;
		minData = minVal;
	}
	public void rebin(int nBin, double[] range) {
		// ignore nBin, we are already binned
		nBin = data.length;

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

		interval = (maxData-minData) / (nBin-1.);
		minVal = Math.rint( minData/interval )*interval;
		maxVal = Math.rint( maxData/interval )*interval;
		int size = (int) Math.rint( (maxVal-minVal) / interval ) +2;
		counts = new int[size];
		for( int k=0 ; k<data.length ; k++) {
			double z = k;
			int i = (int)Math.rint( (z-minVal) / interval );
			if( i<0 || i>=size )continue;
			counts[i]+= data[k];
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