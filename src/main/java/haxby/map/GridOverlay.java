package haxby.map;

import haxby.grid.Contour;
import haxby.grid.GridImager;
import haxby.grid.MGridServer;
import haxby.grid.Mask;
import haxby.grid.NetCDFGrid;
import haxby.grid.URLMasker;
import haxby.grid.XGrid_Z;
import haxby.proj.Projection;
import haxby.proj.ScaledProjection;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class GridOverlay extends MapOverlay {
	XGrid_Z grid;
	Contour contour;
	GridImager imager;
	public GridOverlay( XMap map ) {
		super(map);
		grid = null;
		imager = null;
		contour = new Contour( map, grid );
	}
	public void contourGrid( int interval ) {
		if( grid==null )return;
		if( interval <= 0 ) {
			if( !contour.isVisible() ) return;
			contour.setVisible( false );
		} else {
			if( interval != getInterval() )contour.contour( interval );
			contour.setVisible( true );
		}
		map.repaint();
	}
	public int getInterval() {
		return contour.getInterval();
	}
	public boolean isVisible() {
		return contour.isVisible();
	}
	public void setGrid( XGrid_Z grid ) {
		this.grid = grid;
		contour.setGrid(grid);
	}
	public XGrid_Z getGrid() {
		return grid;
	}
	public float getZ( Point2D p ) {
		if( grid==null ) return Float.NaN;
		Point2D pt = grid.getProjection().getMapXY(p);
		if( grid.contains(pt.getX(), pt.getY()) ) {
			float z = grid.valueAt( pt.getX(), pt.getY());
			return z;
		} else {
			return Float.NaN;
		}
	}
	public void draw(Graphics2D g) {
		if( !mask || !contour.isVisible() ) {
			super.draw(g);
			if( contour.isVisible() ) contour.draw(g);
			return;
		}
		super.drawImage( g );
		if( contour.isVisible() ) contour.draw(g);
		super.drawMask( g );
	}
	
	/**
	 	Save the grid.
	 */
	 public void saveGrid(File file) throws IOException {
		 PrintStream out = new PrintStream(
		 		new FileOutputStream(file));
		 
		 Dimension dim = grid.getSize();
		 Point p = new Point();
		 
		 for (int y = 0; y < dim.height; y++)
		 {
		 	p.y = y;
		 	for (int x = 0; x< dim.width; x++)
		 	{
		 		p.x = x;
		 		Point2D p2 = grid.getProjection().getRefXY(p);
		 		out.println(p2.getX() + "\t" +
		 					p2.getY() + "\t" +
		 					grid.valueAt(x,y));
		 	}
		 }
		out.close();
	}
	public void saveGrd( File file ) throws IOException {
		NetCDFGrid.createStandardGrd( grid, file );
	}
	public void saveMaskedGrd( File file ) throws IOException {
		ScaledProjection proj0 = (ScaledProjection)grid.getProjection();
		Projection proj = proj0.getParent();
		Point2D pt = proj.getMapXY( new Point2D.Double(0.,0.) );
		double x = pt.getX();
		pt = proj.getMapXY( new Point2D.Double(1.,0.) );
		int res = (int)Math.rint( 1024.*320. / ((pt.getX()-x)*360.) );
		int nLevel = 0;
		int nGrid = 1024/res;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int[] bounds = grid.getBounds();
		URLMasker masker = new URLMasker( 320, res, 1, nLevel, proj, 
				MGridServer.getBaseURL()+"merc_320_1024" );
		Mask mask = new Mask( bounds[0], bounds[1], bounds[2]-bounds[0], bounds[3]-bounds[1], masker);
		NetCDFGrid.createStandardGrd( grid, mask, file );
	}
	public void saveGrid() throws IOException {
		if (grid == null) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "No grid loaded");
			return;
		}

		int confirm = JOptionPane.NO_OPTION;
		File file = null;
		while (confirm == JOptionPane.NO_OPTION) {
			JFileChooser chooser = MapApp.getFileChooser();
			int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

			if (ok == chooser.CANCEL_OPTION)
				return;

			file = chooser.getSelectedFile();

			if (file.exists()) {
				confirm = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), 
							"File exists, Overwrite?");
				if( confirm == JOptionPane.CANCEL_OPTION ) return;
			} else {
				break;
			}
		}
		 saveGrid( file );
	 }
}
