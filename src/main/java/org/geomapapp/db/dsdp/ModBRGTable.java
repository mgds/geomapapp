package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.geomapapp.util.ScalableXYPoints;

public class ModBRGTable implements ScalableXYPoints {
	String[] headings;
	Vector rows;
	boolean[][] plot;
	double[][] range0;
	double[][] range;
	int nCol;
	
	int dataIndex1;
	int dataIndex2;
	
	public ModBRGTable(String url) throws IOException {
		BufferedReader in = new BufferedReader(
			new InputStreamReader(
			(URLFactory.url(url)).openStream()));
		String s;
		StringTokenizer st;
		for( int k=0 ; k<4 ; k++) in.readLine();
		s = in.readLine();
		String[] sArr = s.split("\t");
//		st = new StringTokenizer(s);
//		nCol = st.countTokens();
		nCol = sArr.length;
		headings = new String[nCol];
		int k=0;
//		while( st.hasMoreTokens() ) headings[k++] = st.nextToken();
		int m = 0;
		while ( m < sArr.length ) {
			headings[m] = sArr[m];
			m++;
		}
		rows = new Vector();
		range = new double[nCol][2];
		while( (s=in.readLine())!=null ) {
			st = new StringTokenizer(s);
			Vector row = new Vector(st.countTokens());
			double[] entry = new double[nCol];
			k=0;
			while( st.hasMoreTokens() ) entry[k++] = Double.parseDouble( st.nextToken() );
			entry[0] = -entry[0];
			rows.add(entry);
//			System.out.println("entry[0]: " + entry[0]);
		}
		plot = new boolean[nCol-1][rows.size()];
		for( k=0 ; k<nCol-1 ; k++) {
			for( int i=0 ; i<rows.size() ; i++)plot[k][i]=true;
		}
		for( k=0 ; k<nCol ; k++) {
			boolean start = true;
			for( int i=0 ; i<rows.size() ; i++) {
				double[] entry = (double[])rows.get(i);
				if( k!=0 && entry[k]==-999.25 ) {
					plot[k-1][i]=false;
					continue;
				}
				if( start ) {
					range[k][0] = range[k][1] = entry[k];
					start = false;
				} else {
					if( entry[k]>range[k][1] ) range[k][1] = entry[k];
					else if( entry[k]<range[k][0] ) range[k][0] = entry[k];
				}
			}
			double dr = range[k][1]-range[k][0];
			range[k][1] += .02*dr;
			range[k][0] -= .02*dr;
			if( dr==0. ) {
				range[k][1] += 1.;
				range[k][0] -= 1.;
			}
		}
		range0 = (double[][])range.clone();
	}
	public int getDataCount() {
		return nCol-1;
	}
	public String getXTitle(int dataIndex) {
		return headings[dataIndex+1];
	}
	public String getYTitle(int dataIndex) {
		return "DEPTH";
	}
	public double[] getXRange(int dataIndex) {
		return range[dataIndex+1];
	}
	public double[] getYRange(int dataIndex) {
		return range[0];
	}
	public void setXRange(int dataIndex, double[] range) {
		this.range[dataIndex+1] = range;
	}
	public void setYRange(int dataIndex, double[] range) {
	}
	public void resetRanges(int dataIndex) {
		this.range[dataIndex+1] = range0[dataIndex+1];
	}
	public double getPreferredXScale(int k) {
		double dr = range[k][1]-range[k][0];
		return 400./dr;
	}
	public double getPreferredYScale(int dataIndex) {
		return 2.;
	}
	public void trim(int dataIndex) {
		int k = dataIndex;
		double min, max;
		min = max = 0.;
		boolean start = true;
		for( int i=0 ; i<rows.size() ; i++) {
			double[] row = (double[])rows.get(i);
			if( row[k+1]>range[k+1][1] ) plot[k][i] = false;
			else if( row[k+1]<range[k+1][0] ) plot[k][i] = false;
			else if( row[k+1]==-999.25 ) plot[k][i] = false;
			else if(start) {
				min = max = row[k+1];
				start = false;
			} else {
				if( row[k+1]>max )max = row[k+1];
				if( row[k+1]<min )min = row[k+1];
			}
		}
		range[k+1] = new double[] {min, max};
	}
	public void trim( boolean max, int dataIndex ) {
		double test = max ? 1. : -1.;
		int k = dataIndex;
		double extreme = 0.;
		int index = 0;
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			if( i==0 ) {
				extreme = test*row[k+1];
			} else {
				if( extreme<test*row[k+1] ) {
					extreme = test*row[k+1];
					index = i;
				}
			}
		}
		plot[k][index]=false;

		double minZ, maxZ;
		minZ = maxZ = 0.;
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			if( i==0 ) {
				minZ = maxZ = row[k+1];
			} else {
				if( row[k+1]>maxZ ) maxZ = row[k+1];
				else if( row[k+1]<minZ ) minZ = row[k+1];
			}
		}
		k++;
		range[k][0] = minZ;
		range[k][1] = maxZ;
		double dr = range[k][1]-range[k][0];
		range[k][1] += .02*dr;
		range[k][0] -= .02*dr;
		if( dr==0. ) {
			range[k][1] += 1.;
			range[k][0] -= 1.;
		}
		setXRange( dataIndex, range[k] );
	}
	
	public void setDataIndexes( int inputDataIndex1, int inputDataIndex2 ) {
		dataIndex1 = inputDataIndex1;
		dataIndex2 = inputDataIndex2;
	}
	
	public double getRatio() {
		double ratio = ( ( range[dataIndex1 + 1][1] - range[dataIndex1 + 1][0] ) / ( range[dataIndex2 + 1][1] - range[dataIndex2 + 1][0] ) );
		return ratio;
	}
	
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int k) {
		GeneralPath path = new GeneralPath();
		boolean start = true;
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			float x = 0;
			if ( k != dataIndex1 ) {
				double ratio = ( range[dataIndex1 + 1][1] - range[dataIndex1 + 1][0] ) / ( range[k + 1][1] - range[k + 1][0] );
				x = (float)( ( ( ( row[k + 1] - range[k + 1][0] ) * ratio ) + range[dataIndex1 + 1][0] - bounds.getX() ) * xScale );
			}
			else {
				x = (float)((row[k+1]-bounds.getX())*xScale);
			}
				
	
			float y = (float)((row[0]-bounds.getY())*yScale);
			if( start ) {
				path.moveTo(x,y);
				start = false;
				if ( rows.size() > 1 ) {
					double[] nextRow = (double[])rows.get( i + 1 );
//					System.out.println("previousRow[0]: " + nextRow[0] + " row[0]: " + row[0]);
					if ( Math.abs( nextRow[0] - row[0] ) > 10 ) {
						path.lineTo(x,y);
					}
				}
			} 
			else {
				double[] previousRow = (double[])rows.get(i-1);
//				System.out.println("previousRow[0]: " + previousRow[0] + " row[0]: " + row[0]);
				if ( Math.abs( previousRow[0] - row[0] ) < 10 ) {
					path.lineTo(x,y);
				}
				else {
					path.moveTo( x, y );
					path.lineTo( x, y );
				}
			}
		}
		g.setColor( Color.white );
		g.setStroke( new BasicStroke(5f) );
		g.draw(path);
		if ( k != dataIndex1 ) {
			g.setColor( Color.red );
		}
		else {
			g.setColor( Color.black );
		}
		g.setStroke( new BasicStroke(3f) );
		g.draw(path);
	}
}