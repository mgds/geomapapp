package org.geomapapp.grid;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.geom.UTMProjection;
import org.geomapapp.util.ParseLink;

public class GRD98_Grid {
	float[] gridFloat;
	int[] gridInteger;
	Grid2D.Float grdFloat = null;
	Grid2D.Integer grdInteger = null;
	int width, height;
	double dx;
	double x0, y0;
	public double zMin = Double.MAX_VALUE, zMax = -Double.MAX_VALUE;
	MapProjection proj;
	int zone;
	double nodata;
	String filename;

	int dataType = 0;
	int precision = 0;
	int dataFormat = 0;

	private boolean headerRead = false;

	public GRD98_Grid(File file){
		this.filename = file.getPath();
	}

	public void readHeader() throws IOException {
		if ( grdInteger != null || grdFloat != null || headerRead ) {
			return;
		}

		Vector props = new Vector();
		DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream(filename) ) );
		int i = 0;
		int inputInt = 0;
		while( i < 32 ) {
			try {
				inputInt = in.readInt();
				if ( i == 2 ) {
					dataType = inputInt;
				} else if ( i == 3 ) {
					y0 = (double)inputInt;
				} else if ( i == 4 ) {
					y0 += ( (double)inputInt / 60. );
				} else if ( i == 5 ) {
					y0 += ( (double)inputInt / 3600. );
				} else if ( i == 7 ) {
					height = inputInt;
				} else if ( i == 8 ) {
					x0 = (double)inputInt;
				} else if ( i == 9 ) {
					x0 += ( (double)inputInt / 60. );
				} else if ( i == 10 ) {
					x0 += ( (double)inputInt / 3600. );
				} else if ( i == 11 ) {
					dx = ( (double)inputInt / 3600. );
				} else if ( i == 12 ) {
					width = inputInt;
				} else if ( i == 13 ) {
					zMin = inputInt;
				} else if ( i == 14 ) {
					zMax = inputInt;
				} else if ( i == 16 ) {
					precision = inputInt;
				} else if ( i == 17 ) {
					nodata = (double)inputInt;
				} else if ( i == 18 ) {
					dataFormat = inputInt;
				}
			}
			catch ( EOFException eofe ) {
				break;
			}
			i++;
		}

		in.close();
		headerRead = true;
		y0 = y0 - ( height - 1 ) * dx;

		double[] wesn = new double[] { x0, x0 + ( width - 1 ) * dx, y0, y0 + ( height - 1 ) * dx };
		proj = new RectangularProjection( wesn, width, height );
	}

	public Grid2D getGrid() throws IOException {
		if( grdFloat != null ) {
			return grdFloat;
		} else if ( grdInteger != null ) {
			return grdInteger;
		}

		readHeader();
		DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream(filename) ) );
		int i = 0;
		int j = 0;

		while ( i < 32 ) {
			in.readInt();
			i++;
		}

		if ( dataFormat > 0 ) {
			gridInteger = new int[width*height];
			int inputValue = 0;
			while( true ) {
				try {
					if ( dataFormat == 1 ) {
						inputValue = in.read();
					}
					else if ( dataFormat == 2 ) {
						inputValue = in.readShort();
					}
					else if ( dataFormat == 4 ) {
						inputValue = in.readInt();
					}
					if( nodata == (double)inputValue ) { 
						gridInteger[j++] = org.geomapapp.grid.Grid2D.Integer.NaN;
					} 
					else {
//						gridInteger[j++] = ( inputValue / ( precision ) );
						gridInteger[j++] = inputValue;
					}
//					System.out.println(gridInteger[(j-1)]);
				}
				catch ( EOFException eofe ) {
					break;
				}
			}
			grdInteger = new Grid2D.Integer( new java.awt.Rectangle( 0, 0, width, height ), proj );
			grdInteger.setBuffer(gridInteger);
		} else if ( dataFormat < 0 ) {
			gridFloat = new float[width*height];
			float inputValue = 0;
			while( true ) {
				try {
					inputValue = in.readFloat();
					precision = 1;
					if( nodata == (double)inputValue ) { 
						gridFloat[j++] = Float.NaN;
					}
					else {
//						gridFloat[j++] = (float)( inputValue / precision );
						gridFloat[j++] = (float)inputValue;
					}
				}
				catch ( EOFException eofe ) {
					break;
				}
			}
			grdFloat = new Grid2D.Float( new java.awt.Rectangle( 0, 0, width, height ), proj );
			grdFloat.setBuffer(gridFloat);
		}
		in.close();
		if ( grdFloat != null ) {
			return grdFloat;
		}
		else {
			return grdInteger;
		}
	}
}
