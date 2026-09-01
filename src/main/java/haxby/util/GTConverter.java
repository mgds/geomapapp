package haxby.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.swing.JOptionPane;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.MercatorProjection;
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
import org.geotools.styling.Style;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Projection;

import haxby.map.MapApp;

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
	
	private static class RowProcessor extends ForkJoinTask<Map.Entry<Double, Double>> {
		
		private final GridCoverage2D gtGrid;
		private final GridCoordinates2D low, high;
		private final int xDir, y, realY;
		private final Function<Double, Boolean> isData;
		private final Grid2D.Double gmaGrid;
		private final ImportGrid ig;
		private final int numTotalRows;
		private double lowest, highest;
		
		private static volatile int numCompleted = 0;
		
		public RowProcessor(GridCoverage2D gridIn, Grid2D.Double gmaGridIn, GridCoordinates2D lowIn, GridCoordinates2D highIn, int xDirIn, int yIn, int realYIn, Function<Double, Boolean> dataFn, ImportGrid igIn, int numRowsIn, double lowestIn, double highestIn) {
			gtGrid = gridIn;
			gmaGrid = gmaGridIn;
			low = lowIn;
			high = highIn;
			xDir = xDirIn;
			y = yIn;
			realY = realYIn;
			isData = dataFn;
			ig = igIn;
			numTotalRows = numRowsIn;
			lowest = lowestIn;
			highest = highestIn;
		}

		@Override
		public boolean exec() {
//			LocalDateTime start = LocalDateTime.now();
			for(int x = low.x; x < high.x; x++) {
				int realX = (xDir > 0)?(x):(high.x-x-1+low.x);
				GridCoordinates2D pt = new GridCoordinates2D(realX, realY);
				double[] vals = gtGrid.evaluate(pt, (double[])null);
				if(isData.apply(vals[0])) {
					if(vals[0] < lowest) {
						lowest = vals[0];
					}
					if(vals[0] > highest) {
						highest = vals[0];
					}
					gmaGrid.setValue(x, y, vals[0]);
				}
				else {
					gmaGrid.setValue(x, y, Double.NaN);
				}
			}
//			LocalDateTime end = LocalDateTime.now();
//			System.out.println(Duration.between(start, end).toNanos() / (high.x - low.x));
			numCompleted++;
			double percentCompleted = numCompleted * 100. / numTotalRows;
//			System.out.println(percentCompleted + "%");
			ig.showPercent((int)Math.round(percentCompleted));
//			if(0 == numCompleted % 1000) {
//				System.out.println(numCompleted + "/" + numTotalRows);
//			}
			//return true iff there are more tasks to complete
			if(numTotalRows == numCompleted) {
				numCompleted = 0;
				return false;
			}
			return true;
		}

		@Override
		public Entry<Double, Double> getRawResult() {
			return new AbstractMap.SimpleEntry<Double, Double>(lowest, highest);
		}

		@Override
		protected void setRawResult(Entry<Double, Double> value) {
			lowest = value.getKey();
			highest = value.getValue();
		}
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
	 * @param rounder the MathContext showing the required precision for comparison to the NoData value
	 * @param xDir the sign of the x scale value
	 * @param yDir the sign of the y scale value
	 * @return information to plot the grid in GeoMapApp
	 */
	public static Grid2DWrapper getGrid(GridCoverage2D geotoolsGrid, MapProjection proj, boolean hasNoData, double noDataVal, MathContext rounder, int xDir, int yDir, ImportGrid ig) {
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
		Function <Double, Boolean> isData = (hasNoData)?(x -> !new BigDecimal(x, rounder).equals(new BigDecimal(noDataVal, rounder))):(x -> !Double.isNaN(x));
		long cellsPerRow = high.x - low.x + 1;
		long numRows = high.y - low.y + 1;
		long numCells = numRows * cellsPerRow;
		long howManyHundred = numCells/100;
		//TODO consider multithreading for larger grids
		//Probably better to use a ForkJoinPool than to spawn a new thread for every row or cell
		ForkJoinPool fjp = new ForkJoinPool();
		RowProcessor[] rps = new RowProcessor[(int)numRows];
		for(int y = low.y; y <= high.y; y++) {
			int realY = (yDir < 0)?(y):(high.y-y-1+low.y);
			RowProcessor rp = new RowProcessor(geotoolsGrid, grid, low, high, xDir, y, realY, isData, ig, (int)numRows, lowest, highest);
			rps[y - low.y] = rp; 
			fjp.submit(rp);
		}
		try {
			fjp.shutdown();
			boolean finished = fjp.awaitTermination(9*numCells, TimeUnit.MILLISECONDS);
			for(int i = 0; i < rps.length; i++) {
				Map.Entry<Double, Double> zBounds = rps[i].getRawResult();
				if(zBounds.getKey() < lowest) {
					lowest = zBounds.getKey();
				}
				if(zBounds.getValue() > highest) {
					highest = zBounds.getValue();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Grid2DWrapper(grid, lowest, highest, xOffset, yOffset, dx, dy);
	}
	
	private static void displayPopup(String projName) {
		String htmlMsg = "<html><body>This GeoTIFF grid is in " + projName + " projection.";
		htmlMsg += "<br><br>Currently, GeoMapApp can import GeoTIFF grids in Geographic (degrees) and UTM projections.";
		htmlMsg += "<br><br>Please convert your grid to one of those projections to import it in GeoMapApp.";
		htmlMsg += "<br><br>GIS tools and packages such as GDAL can be used for that conversion.";
		htmlMsg += "</body></html>";
		JOptionPane.showMessageDialog(MapApp.anchor, htmlMsg, "Incompatible Projection", JOptionPane.WARNING_MESSAGE);
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
			else if(crs.getName().getCode().contains("UTM")) {
				String str = "UTM zone ";
				String whichZone = crs.getName().getCode().substring(crs.getName().getCode().indexOf("UTM")+str.length());
				int zoneNum = Integer.parseInt(whichZone.split("[NS]")[0]);
				int whichHemisphere = whichZone.endsWith("N") ? MapProjection.NORTH : MapProjection.SOUTH;
				UTM utm = new UTM(zoneNum, 2, whichHemisphere);
				return utm;
			}
			//world mercator (probably EPSG:3395)
			else if(crs.getName().getCode().toUpperCase().contains("WORLD MERCATOR")) {
				displayPopup("World Mercator");
				System.err.println("Incompatible projection: " + epsgPrjStr);
				return null;
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
		displayPopup(crs.getName().getCode());
		System.err.println("Unknown projection: " + epsgPrjStr);
		return null;
	}
}
