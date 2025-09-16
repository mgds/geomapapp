package org.geomapapp.util;

import org.geomapapp.grid.Grid2D;

public class Histogram {
	int[] counts;
	Grid2D grid;
	Grid2D.Boolean mask;
	boolean test;
	double minVal, maxVal, interval;
	double maxData, minData;
	int numCounts, maxCounts;
	double mode, median;
	double mean, stdev;
	protected Histogram() {
	}
	public Histogram(Grid2D grid, int nBin) {
		this.grid = grid;
		mask = null;
		init();
		rebin(nBin, null);
	}
	public Histogram(Grid2D grid, Grid2D.Boolean mask, boolean test, int nBin) {
		this.grid = grid;
		this.mask = mask;
		this.test = test;
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
		java.awt.Rectangle bounds = grid.getBounds();
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( mask!=null && mask.booleanValue(x,y)!=test)continue;
				double z = grid.valueAt(x,y);
				if( Double.isNaN(z) )continue;
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
		}
		if( numCounts==0 ) throw new NullPointerException(
					"no data points in grid");
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
		java.awt.Rectangle bounds = grid.getBounds();
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( mask!=null && mask.booleanValue(x,y)!=test)continue;
				double z = grid.valueAt(x,y);
				if( Double.isNaN(z) )continue;
				int k = (int)Math.rint( (z-minVal) / interval );
				if( k<0 || k>=size )continue;
				counts[k]++;
			}
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
