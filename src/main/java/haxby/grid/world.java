package haxby.grid;

import haxby.grid.*;
import haxby.proj.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;

public class world {
	public static void main( String[] args ) {
		try {
			int nLevel1 = 0;
			int nGrid1 = 8;
			Mercator proj1 = ProjectionFactory.getMercator( 8*320 );
			double y1 = proj1.getY( 72. );
			System.out.println("y = "+ y1);
			int y0 = (int)Math.rint(Math.abs(y1));
			int height = 1+2*y0;
			TilerZ tiler = new TilerZ( 320, 128, 8, nLevel1, proj1, 
					"/data/ridgembs/scratch/bill/grid/final/merc_320_1024");
			tiler.setReadonly( true );
			XGrid_Z grid = new XGrid_Z( 0, y0, 2560, height, tiler );
			PrintStream out = new PrintStream(
				new FileOutputStream(new File("world.xyz")));

			Dimension dim = grid.getSize();
			Point p = new Point();

			for (int y = 0; y < dim.height; y++) {
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
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
