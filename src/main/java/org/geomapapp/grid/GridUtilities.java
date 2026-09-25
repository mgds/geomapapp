package org.geomapapp.grid;

import java.awt.Rectangle;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2D.Boolean;
import org.geomapapp.grid.Grid2D.Byte;
import org.geomapapp.grid.Grid2D.Double;
import org.geomapapp.grid.Grid2D.Float;
import org.geomapapp.grid.Grid2D.FloatWT;
import org.geomapapp.grid.Grid2D.Image;
import org.geomapapp.grid.Grid2D.Integer;
import org.geomapapp.grid.Grid2D.Short;

public class GridUtilities {

	public static Grid2D newInstanceOfGrid(Rectangle bounds, Grid2D grid, MapProjection proj) {
		if (grid instanceof Grid2D.Float)
			return new Grid2D.Float(bounds, proj);
		else if (grid instanceof Grid2D.Boolean)
			return new Grid2D.Boolean(bounds, proj);
		else if (grid instanceof Grid2D.Byte)
			return new Grid2D.Byte(bounds, proj);
		else if (grid instanceof Grid2D.Double)
			return new Grid2D.Double(bounds, proj);
		else if (grid instanceof Grid2D.FloatWT)
			return new Grid2D.FloatWT(bounds, proj);
		else if (grid instanceof Grid2D.Image)
			return new Grid2D.Image(bounds, proj);
		else if (grid instanceof Grid2D.Integer)
			return new Grid2D.Image(bounds, proj);
		else if (grid instanceof Grid2D.Short)
			return new Grid2D.Short(bounds, proj);
		
		// Unknown Grid Type
		return new Grid2D.Double(bounds, proj);
	}

	public static Grid2D getSubGrid(Rectangle bounds, Grid2D grid) {
		return getSubGrid(bounds, grid, grid.getProjection());
	}
	
	public static Grid2D getSubGrid(Rectangle bounds, Grid2D grid, MapProjection proj) {
		Grid2D subGrid = newInstanceOfGrid(new Rectangle(bounds.width, bounds.height), grid, proj);
		for (int x = 0; x < bounds.width; x++)
			for (int y = 0; y < bounds.height; y++) {
				subGrid.setValue(x, y, grid.valueAt(x + bounds.x, y + bounds.y));
			}
		
		return subGrid;
	}

}
