package org.geomapapp.image;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.XYZ;
import org.geomapapp.grid.Grid2D;

public class GridRenderer {
	Palette palette;
	Palette landPalette;
	Palette oceanPalette;
	int background = 0xff646464;
	double ve, unitsPerNode;
	XYZ sun;
	double sunDotZ;
	double gamma;
	double veFactor = 1.;
	public boolean sunIllum = true;

	public GridRenderer() {
		this(1d, 1000d, new XYZ(-1,1,1));
	}
	public GridRenderer( double ve, double unitsPerNode, 
			XYZ toSun) {
		this(new Palette(0), ve, unitsPerNode, toSun);
	}
	public GridRenderer(Palette pal, double ve, double unitsPerNode, 
			XYZ toSun) {
		this.palette = pal;
		sun = toSun;
		sun.normalize();
		sunDotZ = sun.dot(new XYZ(0.,0.,1.));
		this.ve = ve;
		this.unitsPerNode = unitsPerNode;
		landPalette = new Palette(Palette.LAND);
		oceanPalette = new Palette(Palette.OCEAN);
	}
	public void setVEFactor( double factor ) {
		veFactor = factor;
	}
	public Palette getPalette() {
		return palette;
	}
	public void setPalette( Palette pal ) {
		this.palette = pal;
	}
	public void setLandPalette( Palette pal ) {
		landPalette = pal;
	}
	public void setOceanPalette( Palette pal ) {
		oceanPalette = pal;
	}
	public void setBackground( int rgb ) {
		background = rgb;
	}
	public int getBackground() {
		return background;
	}
	public XYZ getSun() {
		return sun;
	}
	public void setSun( XYZ sun ) {
		sun.normalize();
		this.sun = sun;
		sunDotZ = sun.dot(new XYZ(0.,0.,1.));
	}
	public void setUnitsPerNode(double scale) {
		unitsPerNode = scale;
	}
	public void setVE(double ve) {
		this.ve = ve;
	}
	public double getVE() {
		return ve;
	}
	public RenderResult gridImage(Grid2D grid) {
		return gridImage(grid, (BufferedImage)null);
	}
	public RenderResult gridImage(Grid2D grid, BufferedImage image) {
		return gridImage(grid, image, null);
	}
	public RenderResult gridImage(Grid2D grid, Grid2D.Boolean landMask) {
		return gridImage(grid, null, landMask);
	}
	public RenderResult gridImage(Grid2D grid, 
				BufferedImage image,
				Grid2D.Boolean landMask) {
		palette.sunIllum = sunIllum;
		oceanPalette.sunIllum = sunIllum;
		landPalette.sunIllum = sunIllum;
		
		Rectangle bounds = grid.getBounds();
		int width = bounds.width;
		int height = bounds.height;
		MapProjection proj = grid.getProjection();
		Point2D p1 = new Point2D.Double(
				bounds.getX()+.5*bounds.getWidth(),
				bounds.getY()+.5*bounds.getHeight());
		Point2D p2 = new Point2D.Double(
				bounds.getX()+.5*bounds.getWidth()+1.,
				bounds.getY()+.5*bounds.getHeight());
		p1 = proj.getRefXY(p1);
		p2 = proj.getRefXY(p2);
		XYZ r1 = XYZ.LonLat_to_XYZ(p1);
		XYZ r2 = XYZ.LonLat_to_XYZ(p2);
		double scale = proj.major[proj.SPHERE]*Math.acos(r1.dot(r2));
		setUnitsPerNode(scale);
//	System.out.println( scale +" m per node");
		if( image==null ) {
			image = new BufferedImage(width - 1, height - 1,
				((background & 0xff000000) == 0xff000000) ?
					  BufferedImage.TYPE_INT_RGB
					: BufferedImage.TYPE_INT_ARGB);
			for(int y=0 ; y<height-1 ; y++) {
				for(int x=0 ; x<width-1 ; x++) {
					image.setRGB( x, y, background );
				}
			}
		} else {
			width = image.getWidth()+1;
			height = image.getHeight()+1;
		}
		int iy;
		double z, nz, dx, dy, ndx, ndy;
		XYZ grad = new XYZ();
		
		int[] oceanSlopeDist = new int[90];
		int[] landSlopeDist = new int[90];
		
		for(int y=0 ; y<height - 1; y++) {
			iy = y*width;
			for(int x=0 ; x<width - 1; x++) {
				z=0;
				nz=0;
				double h1 = grid.valueAt(bounds.x+x, bounds.y+y);
				if(!Double.isNaN(h1)) {
					z += h1;
					nz++;
				}
				double h2 = grid.valueAt(bounds.x+x+1, bounds.y+y);
				if(!Double.isNaN(h2)) {
					z += h2;
					nz++;
				}
				double h3 = grid.valueAt(bounds.x+x, bounds.y+y+1);
				if(!Double.isNaN(h3)) {
					z += h3;
					nz++;
				}
				double h4 = grid.valueAt(bounds.x+x+1, bounds.y+y+1);
				if(!Double.isNaN(h4)) {
					z += h4;
					nz++;
				}
				if(nz<3) {
					continue;
				}
				z /= nz;
				dx = 0;
				ndx = 0;
				dy = 0;
				ndy = 0;
				if( !Double.isNaN(h1) ) {
					if( !Double.isNaN(h2) ) {
						dx += h2-h1;
						ndx++;
					}
					if( !Double.isNaN(h3)) {
						dy += h1-h3;
						ndy++;
					}
				}
				if( !Double.isNaN(h4) ) {
					if( !Double.isNaN(h3) ) {
						dx += h4-h3;
						ndx++;
					}
					if( !Double.isNaN(h2)) {
						dy += h2-h4;
						ndy++;
					}
				}
				dx /= ndx;
				dy /= ndy;
				boolean tf = false;
				if (landMask != null)
					tf = landMask.booleanValue(x+bounds.x,
								y+bounds.y);
				else 
					tf = z > 0;
				// Set V.E. according to is landMask is null or not.
				if(landMask != null){
				ve = tf ? landPalette.getVE() :
					oceanPalette.getVE();
				}else if(landMask == null){
					ve = palette.getVE();
				}
				grad.x = -dx*ve*veFactor/unitsPerNode;
				grad.y = -dy*ve*veFactor/unitsPerNode;
				grad.z = 1;
				
				if (landMask != null)
					image.setRGB(x, y, getRGB((float)z, grad, tf));
				else
					image.setRGB(x, y, getRGB((float)z, grad));
				
				double theta1 = Math.toDegrees(Math.atan2(dx, unitsPerNode));
				double theta2 = Math.toDegrees(Math.atan2(dy, unitsPerNode));
				
				int[] addTo = tf ? landSlopeDist : oceanSlopeDist;
				
				addTo[(int) Math.abs(theta1)]++;
				addTo[(int) Math.abs(theta2)]++;
			}
		}

		RenderResult renderResult = new RenderResult();
		renderResult.image = image;
		renderResult.landSlopesDist = landSlopeDist;
		renderResult.oceanSlopesDist = oceanSlopeDist;
		
		return renderResult;
	}
	public int getRGB(float z, XYZ grad) {
		grad.normalize();
		float shade = (float)sun.dot(grad);
		return palette.getRGB( z, shade, sunDotZ );
	}
	public int getRGB(float z, XYZ grad, boolean land) {
		grad.normalize();
		float shade = (float)sun.dot(grad);
		Palette pal = land ? landPalette
				: oceanPalette;
		return pal.getRGB( z, shade );
	}
	
	public static class RenderResult 
	{
		public BufferedImage image;
		public int[] landSlopesDist, oceanSlopesDist;
	}
}
