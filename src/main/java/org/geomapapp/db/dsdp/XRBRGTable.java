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

import org.geomapapp.image.Palette;
import org.geomapapp.util.ScalableXYPoints;

public class XRBRGTable implements ScalableXYPoints {
	String[] headings;
	Vector rows;
	Vector rowsNotes;
	boolean[][] plot;
	double[][] range0;
	double[][] range;
	int nCol;
	int dataIndex = 0;
	
	public XRBRGTable(String url) throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(
				(URLFactory.url(url)).openStream()));
			String s;
			StringTokenizer st;
			for( int k=0 ; k<4 ; k++) in.readLine();
			s = in.readLine();
			String[] sArr = s.split("\t");
//			st = new StringTokenizer(s);
//			nCol = st.countTokens();
			nCol = sArr.length;
			headings = new String[nCol];
			int k=0;
//			while( st.hasMoreTokens() ) headings[k++] = st.nextToken();
			int m = 0;
			while ( m < sArr.length ) {
				headings[m] = sArr[m];
				m++;
			}
			rows = new Vector();
			rowsNotes = new Vector();
			range = new double[nCol][2];
			while( (s=in.readLine())!=null ) {
//				st = new StringTokenizer(s);
				String[] sDataArr = s.split("\t");
//				Vector row = new Vector(st.countTokens());
				Vector row = new Vector(sDataArr.length);
				double[] entry = new double[nCol];
				String[] noteEntries = new String[nCol];
				k=0;
				int j = 0;
				while( j < sDataArr.length ) {
					String temp = sDataArr[j];
					String temp2 = temp.replaceAll("\\.", "");
//					System.out.println(temp2);
					if ( temp2.matches("\\D*") ) {
						entry[j] = Double.NaN;
						noteEntries[j] = temp;
					}
					else {
						entry[j] = Double.parseDouble( temp );
						noteEntries[j] = null;
					}
					j++;
				}
				entry[0] = -entry[0];
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
	
	public void setPlotColumn( int input ) {
		dataIndex = input;
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
	
	public double[] getClosestPoint( int inputXPos, double inputCurrentAge ) {
		int index = inputXPos / 10 - 4;
		double difference = 700;
		double closestValue = Double.NaN;
		int closestRow = 0;
		int indexOver = 0;
		double closestAge = 0.0;
		double closestXPos = 0.0;
		if ( index < ( nCol - 1 ) && index >= 0 ) {
			for ( int i = 0; i < rows.size(); i++ ) {
				if ( indexOver == 0 ) {
					double[] row = (double[])rows.get(i);
					if ( ( inputCurrentAge + row[0] ) > 0 && ( inputCurrentAge + row[0] ) < difference ) {
						closestRow = i;
						closestValue = row[index+1];
						closestAge = row[0];
						difference = inputCurrentAge + row[0];
					}
					else if ( ( inputCurrentAge + row[0] ) < 0 ) {
						indexOver++;
						if ( Math.abs( inputCurrentAge + row[0] ) < difference ) {
							closestRow = i;
							closestValue = row[index+1];
							closestAge = row[0];
							indexOver++;
						}
					}
				}
				else {
					break;
				}
			}
		}
		closestXPos = (double)( ( index + 1 ) * 10 );
		double[] returnValues = { closestValue, closestXPos, -1.0 * closestAge };
		return returnValues;
	}
	
	public String getHeading( int inputXPos ) {
		int index = inputXPos / 10 - 4;
		if ( index < ( nCol - 1 ) && index >= 0 ) {
			return headings[index+1];
		}
		else {
			return "";
		}
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
		
		if ( k == dataIndex ) {
			System.out.println("dataIndex: " + dataIndex);
			for( int i=0 ; i<rows.size() ; i++) {
				if( !plot[k][i] )continue;
				double[] row = (double[])rows.get(i);
				String[] notesRow = (String[])rowsNotes.get(i);
				float x = Float.NaN;
				if ( !Double.toString(row[k+1]).equals("NaN") ) {
					x = (float)((row[k+1]-bounds.getX())*xScale);
				}
				float y = (float)((row[0]-bounds.getY())*yScale);
				if( start ) {
					path.moveTo(x,y);
					start = false;
					if ( rows.size() > 1 ) {
						double[] nextRow = (double[])rows.get( i + 1 );
//						System.out.println("previousRow[0]: " + nextRow[0] + " row[0]: " + row[0]);
						if ( Math.abs( nextRow[0] - row[0] ) > 10 ) {
							if ( !Float.toString(x).equals("NaN") ) {
								path.lineTo(x,y);
							}
							else {
								System.out.println(row[k+1]);
								g.drawString(notesRow[k+1], 0, y);
							}
						}
					}
				} 
				else {
					double[] previousRow = (double[])rows.get(i-1);
//					System.out.println("previousRow[0]: " + previousRow[0] + " row[0]: " + row[0]);
					if ( Math.abs( previousRow[0] - row[0] ) < 10 ) {
						if ( !Float.toString(x).equals("NaN") ) {
							path.lineTo(x,y);
						}
						else {
							System.out.println(row[k+1]);
							g.drawString(notesRow[k+1], 0, y);
						}
					}
					else {
						if ( !Float.toString(x).equals("NaN") ) {
							path.moveTo( x, y );
							path.lineTo( x, y );
						}
						else {
							System.out.println(row[k+1]);
							g.drawString(notesRow[k+1], 0, y);
						}
					}
				}
			}
			g.setColor( Color.white );
			g.setStroke( new BasicStroke(4f) );
			g.draw(path);
			g.setColor( Color.black );
			g.setStroke( new BasicStroke(2f) );
			g.draw(path);
		}
		
		path = new GeneralPath();
		start = true;
		
		double min = 0.0;
		double max = 0.0;
		double[] tempRow = (double[])rows.get(0);
		if ( !( Double.toString(tempRow[k+1]).indexOf("NaN") != -1 ) ) {
			min = tempRow[k+1];
			max = tempRow[k+1];
		}
		for ( int i = 0; i < rows.size(); i++ ) {
			tempRow = (double[])rows.get(i);
			if ( !( Double.toString(tempRow[k+1]).indexOf("NaN") != -1 ) )  {
				if ( tempRow[k+1] < min ) {
					min = tempRow[k+1];
				}
				if ( tempRow[k+1] > max ) {
					max = tempRow[k+1];
				}
			}
		}
		
		System.out.println("min: " + min + "\tmax: " + max);
		
		Palette defaultPalette = new Palette(Palette.HAXBY);
//		g.setColor( new Color( (float)0.2, (float)0.7, (float)0.7, (float)0.5 ) );
		
		g.setStroke( new BasicStroke(3f) );
		for( int i=0 ; i<rows.size() ; i++) {
			if( !plot[k][i] )continue;
			double[] row = (double[])rows.get(i);
			float x = (float)((row[k+1]-bounds.getX())*xScale);
			float y = (float)((row[0]-bounds.getY())*yScale);
//			if( start ) {
//				path.moveTo(x,y);
//				start = false;
//			} else path.lineTo(x,y);
			
			float percOfRange = (float) ( ( row[k+1] - min ) / ( max - min ) );
			percOfRange = ( defaultPalette.getRange()[1] - defaultPalette.getRange()[0] ) * percOfRange + defaultPalette.getRange()[0];
			g.setColor( new Color( defaultPalette.getRGB( percOfRange ) ) );
			
			
//			g.setColor( new Color( (float) ( ( row[k+1] - min ) / ( max - min ) ), (float)0.7, (float)0.5 ) );
			if ( row[k+1] != 0.0 ) {
				g.drawLine( ( k + 1 ) * 10, (int)y, ( k + 1 ) * 10, (int)y );
			}
		}
//		g.setColor( Color.white );
//		g.setStroke( new BasicStroke(3f) );
//		g.draw(path);
//		g.setColor( Color.black );
//		g.setStroke( new BasicStroke(1f) );
//		g.draw(path);
	}
}
