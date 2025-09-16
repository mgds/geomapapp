package haxby.worldwind.tilers.spec;

import haxby.grid.GridImager;
import haxby.worldwind.tilers.GridToWorldWindTiler;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import org.geomapapp.grid.Grd;
import org.geomapapp.grid.GrdProperties;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.image.GridRenderer;
import org.geomapapp.image.Palette;

public class Asymmetry_Tiler {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Ussage: Asymmetry_Tiler aysmmetry_grid.grd outputDirectory [# of levels to Tile]");
			System.err.println(" aysmmetry_grid must be a GMT v3o or v4 grid with lon range from -180 to 180");
			System.exit(-1);
		}

		GrdProperties gridP = new GrdProperties(args[0]);
		Grid2D grid = Grd.readGrd(args[0], null, gridP, false, null);
		
		for (double d : grid.getWESN()) {
			System.out.println(d);
		}
		
		Rectangle bounds = grid.getBounds();
		for (int x = 0; x < bounds.width; x++)
			for (int y = 0; y < bounds.height; y++)
				if (grid.valueAt(x + bounds.x, y + bounds.y) == 999 ||
						grid.valueAt(x + bounds.x, y + bounds.y) == 300) {
					grid.setValue(x + bounds.x, y + bounds.y, Double.NaN);
				}
		
		GridToWorldWindTiler tiler = new GridToWorldWindTiler(grid, new File(args[1]), 36);
		tiler.setOutputFormat("png");
		
		GridRenderer renderer = tiler.getRenderer();
		Palette p = new Palette(5);

		p.setRange((float)grid.getRange()[0], 
				(float)grid.getRange()[1]);
		renderer.setPalette(p);
		
		System.out.println(p.getRange()[0] + " " + p.getRange()[1]);
		
		renderer.sunIllum = false;
		
		int levels;
		if (args.length == 3)
			levels = Integer.parseInt(args[2]);
		else
			levels = 3;
		
		renderer.setBackground( 0x00000000 );
		
		for (int i = 0; i < levels; i++) {
			double zoom = 8 * Math.pow(2, i);
	    	int mapRes = 512; 
			int res = mapRes;
			
			while(zoom*res/mapRes > 1.5 && res>1) {
				res /=2;
			}
			
			double ve = 2.5;
			int ires = res;
			while( ires>32 ) {
				ve *= 1.5;
				ires /=2;
			}

			renderer.setVE(ve);
			renderer.setUnitsPerNode( res*100. );
			
			tiler.gridToTilesAtLevel(i);
		}
	}
}
