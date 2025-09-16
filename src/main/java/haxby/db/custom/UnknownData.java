package haxby.db.custom;

import haxby.map.XMap;
import haxby.proj.Projection;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

public class UnknownData {
	public Vector<Object> data;
	public float x,y;
	public int[] rgb = null;
	public float polyX0, polyY0;
	public GeneralPath polyline;
	private boolean visible = true;
	
	public UnknownData(Vector<Object> data){
		this.data=data;
	}

	public void updateXY(XMap map,int xIndex, int yIndex){
		if (xIndex==-1||yIndex==-1) return;
		try {
			Point2D p = map.getProjection().getMapXY(
					new Point2D.Float(Float.parseFloat((String) data.get(xIndex)), 
							Float.parseFloat((String) data.get(yIndex))));
			x=(float)p.getX();
			y=(float)p.getY();
		} catch (Exception ex) {
			x=Float.NaN;
			y=Float.NaN;
		}
	}

	public float[] getPointLonLat(int xIndex, int yIndex) {
		try {
			return new float[] {Float.parseFloat((String) data.get(xIndex)),
					Float.parseFloat((String) data.get(yIndex))};
		} catch (Exception ex) {
			return null;
		}
	}

	public void updateRGB(int rgbIndex) {
		if (rgbIndex < 1) {
			rgb = null;
			return;
		}

		// Try to parse our RGB field
		String rgbS = (String) data.get(rgbIndex);

		// Comma seperated?
		try {
		if (rgbS.indexOf(",") != -1) {
			String[] split = rgbS.split(",");
			if (split.length == 3) {
				float[] rgbF = new float[3];
				for (int i = 0; i < 3; i++)
					rgbF[i] = Float.parseFloat(split[i]);

				// Floats from 0-1
				rgb = new int[3];
				if (rgbF[0] <= 1 && rgbF[1] <= 1 && rgbF[2] <=1) {
					for (int i = 0; i < 3; i++)
						rgb[i] = (int) (rgbF[i] * 255);
				}
				else {
					for (int i = 0; i < 3; i++)
						rgb[i] = (int) rgbF[i];
				}
				// Clamp to 0-255 range
				for (int i = 0; i < 3; i++) {
					rgb[i] = Math.max(rgb[i], 0);
					rgb[i] = Math.min(rgb[i], 255);
				}
			}
			return;
		}

		//TODO Hexadecimal
		} catch (NumberFormatException ex) {
			rgb = null;
		}

		rgb = null;
	}

	public float[] getPolylineWESN(int polylineIndex) {
		float[] wesn = new float[] {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
		
		if (polylineIndex < 1) {
			polyline = null;
			return wesn;
		}
		String[] split = ((String) data.get(polylineIndex)).split("[\t,]");
		int i = 0;
		while ( i < split.length - 1) {
			try {
				float x = Float.parseFloat(split[i]);
				float y = Float.parseFloat(split[i + 1]);

				wesn[0] = Math.min(x, wesn[0]);
				wesn[1] = Math.max(x, wesn[1]);
				wesn[2] = Math.min(y, wesn[2]);
				wesn[3] = Math.max(y, wesn[3]);
			} catch (NumberFormatException ex) {
			}
			i+=2;
		}
		return wesn;
	}

	public void updatePolyline(XMap map, int polylineIndex) {
		if (polylineIndex < 1) {
			polyline = null;
			return;
		}
		String[] split = ((String) data.get(polylineIndex)).split("[\t,]");

		polyline = new GeneralPath();
		polyline.moveTo(0, 0);

		Projection proj = map.getProjection();

		int i = 0;
		int count = 0;
		while ( i < split.length - 1) {
			try {
				Point2D mapXY = proj.getMapXY(Float.parseFloat(split[i]),
						Float.parseFloat(split[i + 1]));
				if (count == 0) {
					polyX0 = (float) mapXY.getX();
					polyY0 = (float) mapXY.getY();
				}
				else
					polyline.lineTo((float)(mapXY.getX() - polyX0), 
							(float)(mapXY.getY() - polyY0));
				count++;

			} catch (NumberFormatException ex) {
				System.err.println("Number format exception at line point " + (count+1) );
				ex.printStackTrace();
			}
			i+=2;
		}

		if (count < 2) {
			polyline = null;
			return;
		}
	}
	
	//used to set whether the data should be visible on the map and table, eg in Velocity Vector decimation
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean tf) {
		visible = tf;
	}
}
