package haxby.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.util.Date;
import java.util.function.Function;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.geom.UTM;
import org.geomapapp.geom.UTMProjection;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.ImportGrid;
import org.geomapapp.grid.ImportGrid.GridFile;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Projection;

/**
 * This class converts between GeoMapApp's internally defined Grid2D objects and GeoTools's GridCoverage2D objects.
 */
public class GTConverter {
	
	public static class GridInfo {
		float[][] values;
		Rectangle bounds;
		public GridInfo(float[][] valuesIn, Rectangle boundsIn) {
			values = valuesIn;
			bounds = boundsIn;
		}
	}

	public static class Grid2DWrapper {
		public final Grid2D data;
		private double lowest, highest;
		private double xOffset, yOffset, dx, dy;
		public Grid2DWrapper(Grid2D dataIn, double low, double high, double xOffsetIn, double yOffsetIn, double dxIn, double dyIn) {
			data = dataIn;
			lowest = low;
			highest = high;
			xOffset = xOffsetIn;
			yOffset = yOffsetIn;
			dx = dxIn;
			dy = dyIn;
		}
		public double getLowest() { return lowest; }
		public double getHighest() { return highest; }
		public double getXOffset() { return xOffset; }
		public double getYOffset() { return yOffset; }
		public double getdx() { return dx; }
		public double getdy() { return dy; }
	}
	
	/**
	 * Converts a GeoMapApp Grid2D into an array that GeoTools can use.
	 * @param grid the grid to convert
	 * @return information necessary for GeoTools to create its own type of grid
	 */
	public static GridInfo getArr(Grid2D grid) {
		if(grid instanceof Grid2D.Image) {
			System.out.println("This is an image! No conversion this time!");
			return null;
		}
		Rectangle bounds = grid.getBounds();
		float[][] ret = new float[bounds.width][bounds.height];
		for(int x = 0; x < bounds.width; x++) {
			for(int y = 0; y < bounds.height; y++) {
				ret[x][y] = (float) grid.valueAt(x+bounds.getX(), y+bounds.getY());
			}
		}
		return new GridInfo(ret, bounds);
	}
	
	/**
	 * Converts a GeoTools grid into a GeoMapApp Grid2D.Double
	 * @param geotoolsGrid the data
	 * @param proj the map projection of the data
	 * @param hasNoData true iff this grid has a "No Data" value defined
	 * @param noDataVal The "No Data" value; ignored if hasNoData is false
	 * @param xDir the sign of the x scale value
	 * @param yDir the sign of the y scale value
	 * @return information to plot the grid in GeoMapApp
	 */
	public static Grid2DWrapper getGrid(GridCoverage2D geotoolsGrid, MapProjection proj, boolean hasNoData, double noDataVal, int xDir, int yDir, ImportGrid ig) {
		GridGeometry2D geom = geotoolsGrid.getGridGeometry();
		GridEnvelope2D env = geom.getGridRange2D();
		Matrix m = ((AffineTransform2D)geom.getGridToCRS2D()).getMatrix();
		double xOffset = m.getElement(0, 2),
				yOffset = m.getElement(1, 2),
				dx = m.getElement(0, 0),
				dy = m.getElement(1, 1);
		Grid2D.Double grid = new Grid2D.Double(env, proj);
		GridCoordinates2D low = env.getLow(), high = env.getHigh();
		double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
		Function <Double, Boolean> isData = (hasNoData)?(x -> x != noDataVal):(x -> !Double.isNaN(x));
		long cellsPerRow = high.x - low.x + 1;
		long numRows = high.y - low.y + 1;
		long numCells = numRows * cellsPerRow;
		long howManyHundred = numCells/100;
		//TODO consider multithreading for larger grids
		for(int y = low.y; y < high.y; y++) {
			int realY = (yDir < 0)?(y):(high.y-y-1+low.y);
			for(int x = low.x; x < high.x; x++) {
				
				int realX = (xDir > 0)?(x):(high.x-x-1+low.x);
				long whichCell = (x - low.x) + (y - low.y)*cellsPerRow;
				if(whichCell % howManyHundred == 0) {
					ig.showPercent((int)(whichCell/howManyHundred));
				}
				else if(x+1 == high.x && y+1 == high.y) {
					ig.showPercent(100);
				}
				
				GridCoordinates2D pt = new GridCoordinates2D(realX, realY);
				//try {
					double[] vals = geotoolsGrid.evaluate(pt, (double[])null);
					if(isData.apply(vals[0])) {
						//System.out.println("("+x + ", "+y+"): "+vals[0]);
						if(vals[0] < lowest) lowest = vals[0];
						if(vals[0] > highest) highest = vals[0];
						grid.setValue(x, y, vals[0]);
					}
					else {
						grid.setValue(x, y, Double.NaN);
					}
				//}
				//catch(Exception e) {
				//}
			}
		}
		return new Grid2DWrapper(grid, lowest, highest, xOffset, yOffset, dx, dy);
	}
	
	public static MapProjection getGmaProj(GridGeometry2D geom) {
		CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
		String epsgPrjStr = String.valueOf(crs.getIdentifiers().toArray()[0]);
		if(epsgPrjStr.startsWith("EPSG:")) {
			String code = epsgPrjStr.substring(5);
			//UTM projections
			if(code.startsWith("326") || code.startsWith("327")) {
				int whichHemisphere = code.startsWith("326")? MapProjection.NORTH : MapProjection.SOUTH;
				int whichZone = Integer.parseInt(code.substring(3));
				UTM utm = new UTM(whichZone, 2, whichHemisphere);
				return utm;
			}
			//assume geographic projection
			else {
				Envelope2D coordRange = geom.getEnvelope2D();
				GridEnvelope2D gridRange = geom.getGridRange2D();
				DirectPosition low = coordRange.getLowerCorner(), high = coordRange.getUpperCorner();
				RectangularProjection rp = new RectangularProjection(new double[] {low.getOrdinate(0), high.getOrdinate(0), low.getOrdinate(1), high.getOrdinate(1)}, gridRange.width, gridRange.height);
				rp.setRange(1);
				return rp;
			}
		}
		System.err.println("Unknown projection: " + epsgPrjStr);
		return null;
	}
}
