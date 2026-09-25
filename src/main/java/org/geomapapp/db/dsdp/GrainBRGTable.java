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

public class GrainBRGTable implements ScalableXYPoints {
	String[] headings;
	Vector rows;
	boolean[][] plot;
	double[][] range0;
	double[][] range;
	int nCol;
	
	int dataIndex1;
	int dataIndex2;
	
	public GrainBRGTable(String url) throws IOException {
		
		System.out.println(url);
		
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
	
	public void setDataIndex( int inputDataIndex1 ) {
		dataIndex1 = inputDataIndex1;
	}
	
	public double getRatio() {
		double ratio = ( ( range[dataIndex1 + 1][1] - range[dataIndex1 + 1][0] ) / ( range[dataIndex2 + 1][1] - range[dataIndex2 + 1][0] ) );
		return ratio;
	}
	
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int k) {
		
		int[] xFillPoints;
		int[] yFillPoints;
		
		if ( k == 0 ) {
			xFillPoints = new int[rows.size() + 2];
			yFillPoints = new int[rows.size() + 2];
		}
		else {
			xFillPoints = new int[2*rows.size()];
			yFillPoints = new int[2*rows.size()];
		}
		if ( k == 0 ) {
			xFillPoints[0] = 0;
			xFillPoints[xFillPoints.length - 1] = 0;
		}
		GeneralPath path = new GeneralPath();
		boolean start = true;
		if ( k == 1 ) {
			g.setColor( new Color( (float)0.2, (float)0.7, (float)0.7, (float)0.5 ) );
		}
		else if ( k == 2 ) {
			g.setColor( new Color( (float)0.7, (float)0.2, (float)0.2, (float)0.5 ) );
		}
		else {
			g.setColor( new Color( (float)0.2, (float)0.2, (float)0.7, (float)0.5 ) );
		}
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			float x = 0;
//			if ( k != dataIndex1 ) {
//				double ratio = ( range[dataIndex1 + 1][1] - range[dataIndex1 + 1][0] ) / ( range[k + 1][1] - range[k + 1][0] );
//				x = (float)( ( ( ( row[k + 1] - range[k + 1][0] ) * ratio ) + range[dataIndex1 + 1][0] - bounds.getX() ) * xScale );
//			}
//			else {
			if ( k == 1 ) {
				x = (float)(( ( row[k+1] + row[k] ) -bounds.getX())*xScale);
			}
			else if ( k == 2) {
				x = (float)(( ( row[k+1] + row[k] + row[k-1] ) -bounds.getX())*xScale);
			}
			else {
				x = (float)((row[k+1]-bounds.getX())*xScale);
			}
//			}
				
	
			float y = (float)((row[0]-bounds.getY())*yScale);
			if( start ) {
				
				if ( k == 1 ) {
					xFillPoints[i] = (int)((float)((row[k]-bounds.getX())*xScale));
					xFillPoints[xFillPoints.length - i - 1] = (int)x;
					yFillPoints[i] = (int)y;
					yFillPoints[yFillPoints.length - i - 1] = (int)y;
				}
				else if ( k == 2 ) {
					xFillPoints[i] = (int)((float)(( ( row[k] + row[k-1] ) -bounds.getX())*xScale));
					xFillPoints[xFillPoints.length - i - 1] = (int)x;
					yFillPoints[i] = (int)y;
					yFillPoints[yFillPoints.length - i - 1] = (int)y;
				}
				else {
					yFillPoints[0] = (int)y;
					xFillPoints[1] = (int)x;
					yFillPoints[1] = (int)y;
				}
				
				path.moveTo(x,y);
				start = false;
			} 
			else {
				
				if ( k == 1 ) {
					xFillPoints[i] = (int)((float)((row[k]-bounds.getX())*xScale));
					yFillPoints[i] = (int)y;
					xFillPoints[xFillPoints.length - i - 1] = (int)x;
					yFillPoints[yFillPoints.length - i - 1] = (int)y;
				}
				else if ( k == 2 ) {
					xFillPoints[i] = (int)((float)(( ( row[k] + row[k-1] ) -bounds.getX())*xScale));
					yFillPoints[i] = (int)y;
					xFillPoints[xFillPoints.length - i - 1] = (int)x;
					yFillPoints[yFillPoints.length - i - 1] = (int)y;
				}
				else {
					xFillPoints[i+1] = (int)x;
					yFillPoints[i+1] = (int)y;
					yFillPoints[yFillPoints.length - 1] = (int)y;
				}
				
				double[] previousRow = (double[])rows.get(i-1);
//				System.out.println("previousRow[0]: " + previousRow[0] + " row[0]: " + row[0]);
//				if ( Math.abs( previousRow[0] - row[0] ) < 10 ) {

					path.lineTo(x,y);
//				}
//				else {
//					path.moveTo( x, y );
//					path.lineTo( x, y );
//				}
			}
		}
		
		g.fillPolygon( xFillPoints, yFillPoints, xFillPoints.length);
		
		g.setColor( Color.white );
		g.setStroke( new BasicStroke(3f) );
//		g.draw(path);
		g.setColor( Color.black );
		g.setStroke( new BasicStroke(1f) );
		g.draw(path);
	}
}