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

public class CustomBRGTable implements ScalableXYPoints {
	String[] headings;
	Vector rows;
	boolean[][] plot;
	double[][] range0;
	double[][] range;
	int nCol;
	Vector rowsNotes;
	
	public static boolean REVERSE_Y_AXIS = false;
	public static boolean IGNORE_ZEROS = false;
	
	public CustomBRGTable(String url) throws IOException {
		BufferedReader in = new BufferedReader(
			new InputStreamReader(
			(URLFactory.url(url)).openStream()));
		String s;
		StringTokenizer st;
		s = in.readLine();
		
		// Disregard all comments from /* to */
		while ( s.startsWith("/*")) {
			s = in.readLine();
			while(!s.endsWith("*/")){
				s = in.readLine();
				}
			}
		// If the line ends with */ proceed to process data
		while(s.endsWith("*/")){
			s = in.readLine();
			break;
		}
		
		while ( s.indexOf(":") != -1 ) {
			s = in.readLine();
		}
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
		rowsNotes = new Vector();
		range = new double[nCol][2];
		while( (s=in.readLine())!=null ) {
//			st = new StringTokenizer(s);
			String[] sDataArr = s.split("\t");
//			Vector row = new Vector(st.countTokens());
			Vector row = new Vector(sDataArr.length);
			double[] entry = new double[nCol];
			String[] noteEntries = new String[nCol];
			k=0;
			int j = 0;
			while( j < sDataArr.length ) {
				String temp = sDataArr[j];
				temp = temp.trim();				
				String temp2 = temp.replaceAll("\\.", "");
				temp2 = temp2.replaceAll("-", "");
				if ( !temp2.matches("\\d*") ) {
					entry[j] = Double.NaN;
					noteEntries[j] = temp;
				}
				else {
					if ( temp.equals("") ) {
						entry[j] = Double.NaN;
					}
					else {
						if ( j == 0 ) {
							entry[j] = Double.parseDouble( temp );
						}
						else if ( Double.parseDouble(temp) != 0 || !IGNORE_ZEROS ) {
							entry[j] = Double.parseDouble(temp);
						}
						else {
							entry[j] = Double.NaN;
						}
					}
					noteEntries[j] = null;
				}
				j++;
			}
			if ( REVERSE_Y_AXIS ) {
				entry[0] = -1.0 * entry[0];
			}
			rows.add(entry);
			rowsNotes.add(noteEntries);
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
	
	public String [] getColumnHeadings() {
		return headings;
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
	
	public static void setReverseYAxis( boolean input ) {
		REVERSE_Y_AXIS = input;
	}
	
	public static void setIgnoreZeros( boolean input ) {
		IGNORE_ZEROS = input;
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
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int k) {
		GeneralPath path = new GeneralPath();
		boolean start = true;
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			float x = (float)((row[k+1]-bounds.getX())*xScale);
			float y = (float)((row[0]-bounds.getY())*yScale);
			if( start ) {
				path.moveTo(x,y);
				start = false;
			} else {
				if ( !Float.toString(x).equals("NaN") ) {
					path.lineTo(x,y);
				}
				else {
					path.moveTo(x,y);
				}
			}
		}
		g.setColor( Color.white );
		g.setStroke( new BasicStroke(3f) );
		g.draw(path);
		g.setColor( Color.black );
		g.setStroke( new BasicStroke(1f) );
		g.draw(path);
	}
}
