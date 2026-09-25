package org.geomapapp.grid;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.image.RenderingTools;

import haxby.map.MapApp;
import haxby.proj.PolarStereo;
import haxby.util.PathUtil;

public class MultiGrid {
	int minRes, maxRes;
	Rectangle2D bounds;
	double offset = Double.NaN;
	double scale = Double.NaN;
	String baseURL;
	Grid2DOverlay grid;
	int background;

	float hist_min = Float.NaN;
	float hist_max = Float.NaN;
	float ve = Float.NaN;
	String palette;
	String sun_illum;

//	GMA 1.4.8: public so can be read by zoomTo() in GridLayerDialog
//	ESRIShapefile shape;
	public ESRIShapefile shape;

	public MultiGrid( int minRes, 
			int maxRes, 
			Rectangle2D bounds, 
			double[] range,
			String baseURL,
			ESRIShapefile shape) {
		this(minRes, maxRes, bounds, range, baseURL, shape, 0);
	}

	public MultiGrid( int minRes, 
			int maxRes, 
			Rectangle2D bounds, 
			double[] range,
			String baseURL,
			ESRIShapefile shape,
			int background) {
		this.minRes = minRes;
		this.maxRes = maxRes;
		this.bounds = bounds;
		if( range!=null && range[1]-range[0]<32000. ) {
			double spread = range[1]-range[0];
			offset = (range[0]+range[1]) * .5;
			scale = 1.;
			while( spread*scale < 16000. ) scale*=2;
			if( scale>100. )scale=100.;
		}
		if( baseURL.startsWith("file://" )) baseURL = baseURL.substring(7);
		else if( baseURL.startsWith("file:")) baseURL = baseURL.substring(5);
		this.baseURL = baseURL;
		this.shape = shape;
		this.background = background;
	}

	public void setHist_max(float hist_max) {
		this.hist_max = hist_max;
	};
	
	public void setHist_min(float hist_min) {
		this.hist_min = hist_min;
	}
	
	public void setPalette(String palette) {
		this.palette = palette;
	}
	
	public void setSun_illum(String sun_illum) {
		this.sun_illum = sun_illum;
	}
	
	public void setMap() {
		grid = new Grid2DOverlay( shape.getMap(), shape.toString() );
		grid.setBackground( background );
	if(MapApp.AT_SEA) {
		grid.setIsImported( !baseURL.contains("www.geomapapp.org/") &&
				!baseURL.contains("www.marine-geo.org/geomapapp/") &&
				!baseURL.startsWith("https://www.gmrt.org/geomapapp/") &&
				!baseURL.startsWith(MapApp.BASE_URL));
	} else {
		grid.setIsImported( !baseURL.contains("www.geomapapp.org/") &&
				!baseURL.contains("www.marine-geo.org/geomapapp/") &&
				!baseURL.startsWith("https://www.gmrt.org/geomapapp/") &&
				!baseURL.startsWith(MapApp.BASE_URL) && 
				!baseURL.startsWith(PathUtil.getPath("ROOT_PATH")));
	}
		applyRenderSettings(grid.getRenderer());
		
		MapApp app = (MapApp)shape.getMap().getApp();
		GridDialog gridDialog = app.getMapTools().getGridDialog();
		gridDialog.gridCBElements.put(grid.name, grid);
		if (gridDialog.gridCB != null) {
			gridDialog.addGrid(grid, this);
		}
		gridDialog.showDialog(grid.name, this);
	}
	public void showDialog() {
		MapApp app = (MapApp)shape.getMap().getApp();
		GridDialog gridDialog = app.getMapTools().getGridDialog();
		if ( gridDialog != null ) {
			gridDialog.addGrid(grid, this);
		}
		gridDialog.showDialog(grid.name, this);
	}
	public void dispose() {

		MapApp app = (MapApp)shape.getMap().getApp();
		GridDialog gridDialog = app.getMapTools().getGridDialog();

		//new combined grid dialog version
		if ( shape != null && gridDialog != null ) {
			gridDialog.dispose(grid);
			if (gridDialog.mGrid == this)
				gridDialog.mGrid = null;
		}
		
		shape = null;
		grid = null;
	}
	public Grid2D getGrid2D() {
			if( grid==null )return null;
			else return grid.getGrid();
	}
	public Grid2DOverlay getGrid2DOverlay() {
		if( grid==null )return null;
		else return grid;
	}
	
	public Grid2D getGridPolar (int res, Rectangle2D area, boolean southPole)
	{
		if( res>maxRes )res=maxRes;
		else if( res<minRes )res=minRes;


		 double mPerPixel = 25600;
		 int i = 1;
		 while (i < res)
		 {
			 i *= 2;
			 mPerPixel /= 2;
		 }
		 
		MapProjection mapProj;
		MapProjection gridProj;
		if (southPole)
		{	
			mapProj = new PolarStereo(new Point(320,320), 180., 25600, -71., PolarStereo.SOUTH, PolarStereo.WGS84);
			gridProj = new PolarStereo(new Point(0,0), 180., mPerPixel, -71., PolarStereo.SOUTH, PolarStereo.WGS84);
		}
		else{ 
			mapProj = new PolarStereo(new Point(320,320), 0., 25600, 71., PolarStereo.NORTH, PolarStereo.WGS84);
			gridProj = new PolarStereo(new Point(0,0), 0., mPerPixel, 71., PolarStereo.NORTH, PolarStereo.WGS84);
		}
		
		Point2D p0 = mapProj.getRefXY(area.getX(), area.getY());
		Point2D p1 = mapProj.getRefXY(area.getMaxX(), area.getMaxY());
		
		p0 = gridProj.getMapXY(p0);
		p1 = gridProj.getMapXY(p1);
		
		Rectangle bnds0 = new Rectangle( (int)Math.floor( bounds.getX()*res ),
						(int)Math.floor( (bounds.getY())*res ),
						(int)Math.ceil( bounds.getWidth()*res ),
						(int)Math.ceil( bounds.getHeight()*res ) );

		Rectangle bnds = new Rectangle( (int)Math.floor( p0.getX()),
						(int)Math.floor( p0.getY()),
						(int)Math.ceil( p1.getX() - p0.getX()),
						(int)Math.ceil( p1.getY() - p0.getY()));

//		 System.out.println( bnds0 );
//		 System.out.println( bnds);
		 
		if( bnds.y+bnds.height<bnds0.y || bnds.y>bnds0.y+bnds0.height )return null;
		if( bnds.x>bnds0.x+bnds0.width )return null;
		if( bnds.x<bnds0.x ) {
			bnds.width -= bnds0.x-bnds.x;
			bnds.x = bnds0.x;
		}
		if( bnds.x+bnds.width>bnds0.x+bnds0.width ) {
			bnds.width = bnds0.x+bnds0.width-bnds.x;
		}
		if( bnds.y<bnds0.y ) {
			bnds.height -= bnds0.y-bnds.y;
			bnds.y = bnds0.y;
		}
		if( bnds.y+bnds.height>bnds0.y+bnds0.height ) {
			bnds.height = bnds0.y+bnds0.height-bnds.y;
		}
		
//System.out.println();
// System.out.println( res );
// System.out.println( bnds0.toString() );
// System.out.println( bnds.toString() );

		TileIO.Short tileIO = new TileIO.Short(  gridProj, baseURL+"z_"+res, 320);
// System.out.println( baseURL+"z_"+res );
		TiledGrid tg = new TiledGrid( gridProj, bnds, tileIO, 320, 1, null);
		Grid2D grd = null;
		if( Double.isNaN(offset) ) grd = new Grid2D.Float( bnds, gridProj );
		else {
			grd = new Grid2D.Short( bnds, gridProj );
			((Grid2D.Short)grd).scale( offset, scale);
		}
		tg.composeGrid( grd );
		int count = 0;
		for( int x=bnds.x ; x<bnds.x+bnds.width ; x++) {
			for( int y=bnds.y ; y<bnds.y+bnds.height ; y++) {
				if( !Double.isNaN( grd.valueAt(x,y) )) count++;
			}
		}
	// 	System.out.println(count);
		return grd;
	}
	
	public Grid2D getGrid( int res, Rectangle2D area ) {
		if( res>maxRes )res=maxRes;
		else if( res<minRes )res=minRes;
		int wrap = 640*res;
		Rectangle bnds0 = new Rectangle( (int)Math.floor( bounds.getX()*res ),
						(int)Math.floor( (bounds.getY())*res ),
						(int)Math.ceil( bounds.getWidth()*res ),
						(int)Math.ceil( bounds.getHeight()*res ) );

		Rectangle bnds = new Rectangle( (int)Math.floor( area.getX()*res ),
						(int)Math.floor( (area.getY()-260)*res ),
						(int)Math.ceil( area.getWidth()*res ),
						(int)Math.ceil( area.getHeight()*res ) );

// System.out.println( res );
// System.out.println( bnds0.toString() );
// System.out.println( bnds.toString() );

		if( bnds.y+bnds.height<bnds0.y || bnds.y>bnds0.y+bnds0.height )return null;
		while( bnds.x>bnds0.x+bnds0.width ) bnds.x-=wrap;
		while( bnds.x+bnds.width<bnds0.x ) bnds.x+=wrap;
		if( bnds.x>bnds0.x+bnds0.width )return null;
		if( bnds.x<bnds0.x ) {
			bnds.width -= bnds0.x-bnds.x;
			bnds.x = bnds0.x;
		}
		if( bnds.x+bnds.width > bnds0.x+bnds0.width ) {
			if (wrap > 0)
			{
				int x = bnds.x + bnds.width;
				while (x > wrap) x -= wrap;
				if (x > bnds0.x + bnds0.width)
					bnds.width = bnds0.x + bnds0.width - (x - bnds.width);
			}
			else
				bnds.width = bnds0.x + bnds0.width - bnds.x;
		}
		if( bnds.y<bnds0.y ) {
			bnds.height -= bnds0.y-bnds.y;
			bnds.y = bnds0.y;
		}
		if( bnds.y+bnds.height>bnds0.y+bnds0.height ) {
			bnds.height = bnds0.y+bnds0.height-bnds.y;
		}
// System.out.println( res );
// System.out.println( bnds0.toString() );
// System.out.println( bnds.toString() );

		Mercator merc = new Mercator( 0., 0., res*640, 0, 0);
		TileIO.Short tileIO = new TileIO.Short(  merc, baseURL+"z_"+res, 320);
// System.out.println( baseURL+"z_"+res );
		TiledGrid tg = new TiledGrid( merc, bnds, tileIO, 320, 1, null);
		tg.setWrap( wrap );
		Grid2D grd = null;
		if( Double.isNaN(offset) ) grd = new Grid2D.Float( bnds, merc );
		else {
			grd = new Grid2D.Short( bnds, merc );
			((Grid2D.Short)grd).scale( offset, scale);
		}
		tg.composeGrid( grd );
//		int count = 0;
//		for( int x=bnds.x ; x<bnds.x+bnds.width ; x++) {
//			for( int y=bnds.y ; y<bnds.y+bnds.height ; y++) {
//				if( !Double.isNaN( grd.valueAt(x,y) )) count++;
//			}
//		}
	// 	System.out.println(count);
		return grd;
	}
	public void draw(Graphics2D g) {
		grid.draw(g);
	}

	public void applyRenderSettings(RenderingTools renderer) {
		if ("off".equals(sun_illum))
			renderer.setSunOn(false);
		else if ("on".equals(sun_illum))
			renderer.setSunOn(true);
		
		if (palette != null)
			renderer.setPalette(palette);
		
		if (!Float.isNaN(ve)) {
			renderer.setVE(ve);
		}

		if (!Float.isNaN(hist_min) && !Float.isNaN(hist_max) &&
				hist_min < hist_max) {
			renderer.setRange(new float[] {hist_min, hist_max});
		}
	}

	public void setVE(String ve) {
		if (ve == null) return;
		try {
			this.ve = Float.parseFloat(ve);
		} catch (NumberFormatException e) {}
	}
}
