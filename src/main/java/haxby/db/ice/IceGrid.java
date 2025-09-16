package haxby.db.ice;

import haxby.map.*;
import haxby.proj.*;
import haxby.grid.*;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class IceGrid implements Overlay {
	JComboBox savedGrids;
	Z lastGrid;
	MapOverlay overlay;
	XMap map;
	IceDB db;
	boolean plot;
	boolean[] land = null;
	public IceGrid(XMap map, IceDB db) {
		this.map = map;
		this.db = db;
		plot = false;
		overlay = new MapOverlay(map);
		savedGrids = new JComboBox();
		savedGrids.addItem( new Z() );
	}
	public void setPlot(boolean tf) {
		plot = tf;
	//	map.repaint();
	}
	public void draw(Graphics2D g) {
	//	if( !plot )return;
		overlay.draw(g);
	}
	public void grid() {
		int yr1 = 1900;
		int yr2 = 2005;
		try {
			long[] interval = timeInterval();
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						if( db.before.isSelected() ) {
								cal.setTimeInMillis(interval[0]);
								yr1 = cal.get(cal.YEAR);
						}
			if( db.after.isSelected() ) {
								cal.setTimeInMillis(interval[1]);
								yr2 = cal.get(cal.YEAR);
						}
/*
			if( db.before.isSelected() ) {
				yr2 = Integer.parseInt( db.startF.getText() )-1;
			} else {
				yr1 = Integer.parseInt( db.startF.getText() );
			}
*/
		} catch(Exception e) {
		}
		boolean water = db.includeWater.isSelected();
		boolean ice = db.includeIce.isSelected();
		grid( yr1, yr2, water, ice);
	}
		long[] timeInterval() {
				long[] interval = new long[2];
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				interval[0] = 0;
				interval[1] = cal.getTimeInMillis();
				cal.setTimeInMillis(0L);
				try {
						if( db.before.isSelected() ) {
								StringTokenizer st = new StringTokenizer(db.startF.getText(), "/");
								int month = st.countTokens()==2 ? Integer.parseInt(st.nextToken().trim())-1 : 0;
								cal.set( cal.MONTH, month);
								cal.set( cal.YEAR, Integer.parseInt(st.nextToken().trim()));
								cal.set( cal.DATE, 1);
								interval[0] = cal.getTimeInMillis();
						}
						if( db.after.isSelected() ) {
								StringTokenizer st = new StringTokenizer(db.endF.getText(), "/");
								int month = st.countTokens()==2 ? Integer.parseInt(st.nextToken().trim())-1 : 0;
								cal.set( cal.MONTH, month);
								cal.set( cal.YEAR, Integer.parseInt(st.nextToken().trim()));
								cal.set( cal.DATE, 1);
								interval[1] = cal.getTimeInMillis();
						}
				} catch(Exception e) {
				}
				return interval;
	}
	public void grid( int yr1, int yr2, boolean includeWater, boolean includeIce) {
		long[] times = timeInterval();
		if(land==null)land = IBCAO.getMask600();
		float[] z = new float[30*30];
		float[] w = new float[30*30];
		Vector exp = db.expeditions;
		Calendar cal = Calendar.getInstance();
		if( includeIce ) {
			for( int i=0 ; i<exp.size() ; i++) {
			IceExpedition e = (IceExpedition)exp.get(i);
			for( int k=0 ; k<e.cores.length ; k++ ) {
				int[] date = e.cores[k].date;
				for( int j=0 ; j<e.cores[k].d18o.length ; j++) {
					cal.set( date[2], date[0]-1, date[1] );
					cal.add( cal.DATE, -e.cores[k].index18o[j] );
					long time = cal.getTimeInMillis();
					if( time<times[0] || time>times[1])continue;
				//	int year = cal.get(cal.YEAR);
				//	if( year<yr1 )continue;
				//	if( year>yr2)continue;
					if( e.cores[k].depth[j]/e.cores[k].thickness <= .1f )continue;
					if( e.cores[k].index18o[j]>e.cores[k].trackEnd ) continue;
					int index = e.cores[k].index18o[j];
					float x = e.cores[k].trajX[index]/20f;
					float y = e.cores[k].trajY[index]/20f;
					float val = e.cores[k].d18o[j]-2f;
					int ix = (int)Math.floor(x);
					int iy = (int)Math.floor(y);
					int ix1 = ix-4;
					if(ix1<0) ix1=0;
					int ix2 = ix+5;
					if( ix2>29 )ix2=29;
					int iy1 = iy-4;
					if(iy1<0) iy1=0;
					int iy2 = iy+5;
					if( iy2>29 )iy2=29;
					for( iy=iy1 ; iy<=iy2 ; iy++ ) {
						double rr = Math.pow(iy-(double)y,2);
						for( ix=ix1 ; ix<=ix2 ; ix++ ) {
							double r2 = rr+ Math.pow(ix-(double)x,2);
							if(r2>25.)continue;
							float wt = (float)Math.exp(-r2);
							int kk = ix + 30*iy;
							z[kk] += wt*val;
							w[kk] += wt;
						}
					}
				}
			}
			}
		}
		float[] zz = new float[30*30];
		if( includeWater ) db.dgrid.grid( z, w, yr1, yr2, false);
		Z g = (Z)savedGrids.getSelectedItem();
		float[] diff = g.z;
		for( int i=0 ; i<z.length ; i++) {
			if( w[i]<.001f ) z[i]=Float.NaN;
			else z[i] /= w[i];
			zz[i]=z[i];
			if( diff==null )continue;
			if( Float.isNaN(diff[i]) || Float.isNaN(z[i]) )z[i]=Float.NaN;
			else z[i] = z[i]-diff[i]-2;
		}
		lastGrid = new Z(zz, yr1, yr2, includeWater, includeIce);
		int gray = Color.gray.getRGB();
		BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
		boolean discrete = db.discrete.isSelected();
		double interval = Double.parseDouble(db.interval.getText());
		for( int y=0 ; y<600 ; y++) {
			double yy = y/20.;
			for( int x=0 ; x<600 ; x++) {
				double xx = x/20.;
				double val = Interpolate.bicubic( z, 30, 30, xx, yy);
				if( discrete ) {
					val = interval*Math.floor(val/interval);
				}
				if( land[x+600*y] ) {
					image.setRGB(x,y,0);
				} else if(Double.isNaN(val)) {
					image.setRGB(x,y, gray);
				} else {
					image.setRGB(x,y, IceCore.getColor( 2.f+(float)val ).getRGB() );
				}
			}
		}
		overlay.setImage( image, 0., 0., 1.);
	}
	public void saveGrid() {
		if( lastGrid!=null )savedGrids.addItem(lastGrid);
	}
	class Z {
		public float[] z;
		String desc;
		public Z() {
			desc = "null";
		}
		public Z(float[] z, int yr1, int yr2, boolean water, boolean ice) {
			this.z = z;
			desc = yr1 +" to "+ yr2 +" (";
			if( water )desc+="w";
			if( ice )desc+="i";
			desc+=")";
		}
		public String toString() {
			return desc;
		}
	}
}
//	public void setImage( BufferedImage im, double x0, double y0, double scale) {
