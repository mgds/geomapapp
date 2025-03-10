package org.geomapapp.grid;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.geomapapp.geom.CylindricalProjection;
import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.MercatorProjection;
import org.geomapapp.geom.ProjectionDialog;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.geom.UTM;
import org.geomapapp.geom.UTMProjection;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.gis.shape.ShapeSuite;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Projection;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;

import haxby.map.MapApp;
import haxby.proj.PolarStereo;
import haxby.util.FilesUtil;
import haxby.util.GTConverter;
import haxby.util.GeneralUtils;
import haxby.util.PathUtil;

public class ImportGrid implements Runnable {
	private static String[] supportedGrids = new String[] {
		"NetCDF grid (example: .grd  or .nc)", // .grd or .nc
		"ESRI ASCII grid files ( .asc )",
		"ESRI Binary grid files ( .hdr / .flt )",
		"GRD98 grid files ( .G98, big-endian )", // GMA 1.6.6
		"ASCII Polar Grid file ( .asc )",
		"GeoTIFF (.tif or .tiff)", //GMA 3.7.5
	};

	private static Map<Integer, FileFilter> gridFilter = new HashMap<Integer, FileFilter>();
	static {
		gridFilter.put(new Integer(0), new FileFilter() {
			// Add different file extensions to choose from
			String[] extensions = new String[] { ".grd", ".nc", ".GRD", ".NC" };
			String description = "NetCDF grid files ( *.grd, *.nc )";
			public boolean accept(File f) {
				if( f.isDirectory() ) {
					return true;
				}
				String name = f.getName().toLowerCase();
				for (int i = extensions.length - 1; i >= 0; i--) {
					if (name.endsWith(extensions[i])) {
						return true;
					}
				}
					return false;
			}
			public String getDescription() {
				return description;
			}
		});

		gridFilter.put(new Integer(1), new FileFilter() {
			public boolean accept(File f) {
				if( f.isDirectory() ) return true;
				if( !f.getName().toLowerCase().endsWith(".asc") )return false;
				String name = f.getName();
				name = name.substring(0, name.lastIndexOf(".") );
				return true;
			}
			public String getDescription() {
				return "ESRI ASCII grid files ( *.asc )";
			}
		});

		gridFilter.put(new Integer(2), new FileFilter() {
			public boolean accept(File f) {
				if( f.isDirectory() ) return true;
				if( !f.getName().toLowerCase().endsWith(".hdr") )return false;
				String name = f.getName();
				name = name.substring(0, name.lastIndexOf(".") );
				return true;
			}
			public String getDescription() {
				return "ESRI Binary grid header files ( *.hdr )";
			}
		});

//		***** GMA 1.6.6: Add new filter for GEODAS grids
		gridFilter.put(new Integer(3), new FileFilter() {
			public boolean accept(File f) {
				if( f.isDirectory() ) return true;
				if( !f.getName().toLowerCase().endsWith(".g98") )return false;
				String name = f.getName();
				name = name.substring(0, name.lastIndexOf(".") );
				return true;
			}
			public String getDescription() {
				return "GRD98 grid files ( *.G98 )";
			}
		});

		gridFilter.put(new Integer(4), new FileFilter() {
			public boolean accept(File f) {
				if( f.isDirectory() ) return true;
				if( !f.getName().toLowerCase().endsWith(".asc") )return false;
				String name = f.getName();
				name = name.substring(0, name.lastIndexOf(".") );
				return true;
			}
			public String getDescription() {
				return "Polar Projection ASCII Grids ( *.asc)";
			}
		});
		
		gridFilter.put(5, new FileFilter() {
			public boolean accept(File f) {
				if(f.isDirectory()) return true;
				String extension = f.getName().toLowerCase().substring(f.getName().lastIndexOf(".")+1);
				return extension.equals("tif") || extension.equals("tiff");
			}
			public String getDescription() {
				return "GeoTIFF grids (*.tif, *.tiff)";
			}
		});
	}

	JFrame frame;
	JTextArea area;
	JButton gridB;
	double dxMin,
			dyMin,
			zMin, 
			zMax,
			zMinFloor,
			zMaxCeiling,
			mostWest,
			mostEast,
			mostNorth,
			mostSouth;
	Double[] zMinTemp,
			 zMaxTemp;
	double[] wesn;
	double[] zScale,
			 add_offset;
	double noData = Float.NaN;
	String zUnits,
		   dataType;
	String areaText;
	boolean applyForAll;
	boolean[] flipGrid;
	DecimateXBG dec;
	ShapeSuite suite;
	int currentIndex,
		gridType;
	protected int mapType;
	boolean waiting = false;
	protected String logFileName;
	protected File logFile;
	protected boolean log = false;
	private BufferedWriter writer = null;
	
	ProjectionDialog pd = new ProjectionDialog();

	public ImportGrid(JFrame frame, ShapeSuite suite) {
		this(frame, suite, MapApp.MERCATOR_MAP);
	}

	public ImportGrid(JFrame frame, ShapeSuite suite, int mapType) {
		this.frame = frame;
		this.suite = suite;
		this.mapType = mapType;
		area = new JTextArea(12,50);
		dec = new DecimateXBG(area, this);
		init();
	}

	void init() {
		JPanel panel = new JPanel();
		gridB = new JButton("Import Grid");
		panel.add(gridB);
		gridB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				begin();
			}
		});

		frame.getContentPane().add( panel, "North");
	    JScrollPane scroll = new JScrollPane (area);

		frame.getContentPane().add( scroll );
		frame.pack();
		frame.setVisible(true);

		// GMA 1.4.8: Automatically bring up file chooser to select grid
		gridB.doClick();
		
		//determine whether logging is selected in Preferences
		MapApp app = (MapApp) suite.map.getApp();
		log = app.logGridImports;
	}
	
	class GeotiffGridFile implements GridFile {
		private GridCoverage2D gridCoverage;
		private Grid2D grid;
		private GridEnvelope2D env;
		private double[] _wesn;
		private MapProjection proj;
		GeoTiffIIOMetadataDecoder mdd;
		
		public GeotiffGridFile(GridCoverage2D gcIn, GridEnvelope2D envIn, MapProjection projIn, GeoTiffIIOMetadataDecoder mddIn) {
			grid = null;
			_wesn = null;
			gridCoverage = gcIn;
			env = envIn;
			proj = projIn;
			mdd = mddIn;
		}
		
		public Grid2D getGrid() {
			if(null == grid) {
				GridGeometry2D geom = gridCoverage.getGridGeometry();
				Matrix m = ((AffineTransform2D)geom.getGridToCRS2D()).getMatrix();
				double xOffset = m.getElement(0, 2),
						yOffset = m.getElement(1, 2),
						dx = m.getElement(0, 0),
						dy = m.getElement(1, 1);
				double posDx = Math.abs(dx), posDy = Math.abs(dy);
				int signDx = (int)(posDx/dx), signDy = (int)(posDy/dy);
				double tileOffsetX = dx/2, tileOffsetY = dy/2;
				if(proj instanceof UTM) {
					UTMProjection proj2 = new UTMProjection(xOffset-tileOffsetX, yOffset-tileOffsetY, posDx, posDy, (UTM)proj);
					double[] gridWesn = getGridWESN(proj2, new Rectangle(0, 0, env.width, env.height));
					double dx2 = (gridWesn[1] - gridWesn[0])/env.width,
							dy2 = (gridWesn[3] - gridWesn[2])/env.height;
					if(dxMin > dx2) dxMin = dx2;
					if(dyMin > dy2) dyMin = dy2;
					Date start = new Date();
					GTConverter.Grid2DWrapper tmp = GTConverter.getGrid(gridCoverage, proj2, mdd.hasNoData(), mdd.hasNoData()?mdd.getNoData():Double.NaN, signDx, signDy);
					Date end = new Date();
					long durMillis = end.getTime() - start.getTime();
					if(durMillis > 1000) {
						Duration elapsed = Duration.ofMillis(durMillis);
						System.out.println("Took " + elapsed.toString().substring(2).replaceAll("([HMS])", ("$1 ")).trim().toLowerCase());
					}
					else {
						System.out.println("Took " + durMillis + "ms");
					}
					if(zMin > tmp.getLowest()) zMin = tmp.getLowest();
					if(zMax < tmp.getHighest()) zMax = tmp.getHighest();
					grid = tmp.data;
					_wesn = gridWesn;
				}
				else {
					RectangularProjection rproj = (RectangularProjection)proj;
					double[] projWesn = rproj.getWESN();
					projWesn[0] -= tileOffsetX;
					projWesn[2] += tileOffsetY;
					RectangularProjection rproj2 = new RectangularProjection(projWesn, rproj.getWidth(), rproj.getHeight());
					double[] gridWesn = getGridWESN(rproj2, env);
					double dx2 = (gridWesn[1] - gridWesn[0])/env.width,
							dy2 = (gridWesn[3] - gridWesn[2])/env.height;
					if(dxMin > dx2) dxMin = dx2;
					if(dyMin > dy2) dyMin = dy2;
					Date start = new Date();
					GTConverter.Grid2DWrapper tmp = GTConverter.getGrid(gridCoverage, rproj2, mdd.hasNoData(), mdd.hasNoData()?mdd.getNoData():Double.NaN, signDx, signDy);
					Date end = new Date();
					long durMillis = end.getTime() - start.getTime();
					if(durMillis > 1000) {
						Duration elapsed = Duration.ofMillis(durMillis);
						System.out.println("Took " + elapsed.toString().substring(2).replaceAll("([HMS])", ("$1 ")).trim().toLowerCase());
					}
					else {
						System.out.println("Took " + durMillis + "ms");
					}
					if(zMin > tmp.getLowest()) zMin = tmp.getLowest();
					if(zMax < tmp.getHighest()) zMax = tmp.getHighest();
					grid = tmp.data;
					_wesn = gridWesn;
				}
			}
			return grid;
		}
		public double[] getWesn() {
			return _wesn;
		}
	}

	void begin() {
		(new Thread(this)).start();
	}

	public void run() {
		gridB.setEnabled(false);
		try {
			open();
		}catch(IOException e) {
			org.geomapapp.io.ShowStackTrace.showTrace(e, frame);
		}
		gridB.setEnabled(true);
	}

	void open() throws IOException {
		// show choice of which grid to import
		Object c = JOptionPane.showInputDialog(null,
								"<html><b>Select a grid type to import.</b></html>\n" + " ",
								"Import 2-D Grid File", 
								JOptionPane.QUESTION_MESSAGE,
								null, 
								supportedGrids,
								supportedGrids[0]);
		// check for null
		if (c == null) {
			return;
		}

		// search which the user selected and assign it as k num as gridtype
		int k = 0;
		for (Object o : supportedGrids){
			if (o == c) {
				gridType = k;
				break;
			}
			else { 
				k++;
			}
		}

		// add Event Dispatch thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				JFileChooser chooser = haxby.map.MapApp.getFileChooser();
				int mode = chooser.getFileSelectionMode();
				boolean multi = chooser.isMultiSelectionEnabled();
				chooser.setMultiSelectionEnabled( true );
				chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

				FileFilter fileFilter = (FileFilter) gridFilter.get( new Integer(gridType));

				chooser.addChoosableFileFilter(fileFilter);
				int ok = chooser.showOpenDialog(frame);
				File[] choice = null;
				if( ok!=JFileChooser.CANCEL_OPTION ) {
					choice = chooser.getSelectedFiles();
				}
				chooser.setMultiSelectionEnabled(multi);
				chooser.setFileSelectionMode( mode );
				chooser.removeChoosableFileFilter(fileFilter);
				if( ok==JFileChooser.CANCEL_OPTION ) {
					return;
				}

				switch (gridType) {
				case 0:
					try {
						openNETCDF(choice);
					} catch (IOException e) {
						showFormatError(choice[0].getName());
					}
					break;
				case 1:
					try {
						openESRI_ASCII(choice);
					} catch (IOException e) {
						showFormatError(choice[0].getName());
					}
					break;
				case 2:
					try {
						openESRI_Binary(choice);
					} catch (IOException e) {
						showFormatError(choice[0].getName());
					}
					break;
				case 3:
					try {
						// GMA 1.6.6: Added GRD98 grid format option
						openGRD98(choice);
					} catch (IOException e) {
						showFormatError(choice[0].getName());
					}
					break;
				case 4:
					try {
						openPolarASC(choice);
					} catch (IOException e) {
						showFormatError(choice[0].getName());
					}
				case 5:
					try {
						//GMA 3.7.5: added Geotiff grid import
						openGeotiff(choice);
					}
					catch(IOException e) {
						showFormatError(choice[0].getName());
					}
				default:
					break;
				}
			}
		});
	}

	void getGridsBounds(Grid2D[] grids) {
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;

		for( int k=0 ; k<grids.length ; k++) {
			double[] wesnGrid = grids[k].getWESN();
			double[] range = grids[k].getRange();

			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);

			zMin = Math.min(range[0] * zScale[k], zMin);
			zMax = Math.max(range[1] * zScale[k], zMax);
//			minDx = Math.min(, minDx);
		}
	}

	MapProjection getProjection(String name, MapProjection inputPrj, double[] wesn, int width, int height) {
		if (applyForAll && inputPrj != null) {
			zScale[currentIndex] = pd.getZScale();
			add_offset[currentIndex] = pd.getOffset();
			if (flipGrid != null) {
				flipGrid[currentIndex] = pd.getFlipGrid();
			}
			return  pd.getProjection( wesn, width, height );
		}

		MapProjection prj = pd.getProjection( frame,
							wesn,
							currentIndex == 0 ? 1 : zScale[currentIndex - 1],
							width, height,
							inputPrj,
							name);
		zScale[currentIndex] = pd.getZScale();
		dataType = pd.getDataType();
		zUnits = pd.getZUnits();
		noData = pd.getNoData();
		add_offset[currentIndex] = pd.getOffset();
		applyForAll = pd.getApplyForAll();
		if (flipGrid != null) {
			flipGrid[currentIndex] = pd.getFlipGrid();
		}
		return prj;
	}

	MapProjection getPolarProjection(String name, double cell_size, int width, int height) {
		MapProjection prj = pd.getPolarProjection( frame,
							cell_size,
							currentIndex == 0 ? 1 : zScale[currentIndex - 1],
							name,
							mapType == MapApp.SOUTH_POLAR_MAP, width, height);
		zScale[currentIndex] = pd.getZScale();
		dataType = pd.getDataType();
		zUnits = pd.getZUnits();
		add_offset[currentIndex] = pd.getOffset();
		return prj;
	}

	void tileGrids(String name, File[] files, GridFile[] grids, double dx0) throws IOException {
		int res = 2;
		while (dx0/res > 1.4 * dxMin)
			res *= 2;
		tileGrids(name, files, grids, res);
	}

	void tileGrids(String name, File[] files, GridFile[] grids, int res) throws IOException {
		double dx0;
		if (mapType == MapApp.MERCATOR_MAP)
			dx0 = 360./640.;
		else 
			dx0 = 25600;
		double mPerPixel = dx0 / res;

		MapProjection proj;
		switch (mapType) {
		default:
		case MapApp.MERCATOR_MAP:
			proj = new Mercator( 0., 0., res*640, Mercator.SPHERE, Mercator.RANGE_0_to_360);
			break;
		case MapApp.SOUTH_POLAR_MAP:
			proj = new PolarStereo(new Point(0,0), 180., mPerPixel, -71., PolarStereo.SOUTH, PolarStereo.WGS84);
			break;
		case MapApp.NORTH_POLAR_MAP:
			proj = new PolarStereo(new Point(0,0), 0., mPerPixel, 71., PolarStereo.NORTH, PolarStereo.WGS84);
			break;
		}
		tileGrids(name, files, grids, proj, res);
	}

	void tileGrids(String name, File[] files, GridFile[] grids, MapProjection proj, int res) throws IOException {	
		// Clamp Latitudes in the Mercator
		if (mapType == MapApp.MERCATOR_MAP) {
			if( wesn[2]<-79. )wesn[2]=-79.;
			if( wesn[3]>81. )wesn[3]=81.;
		}

	
		if (zMin > zMax) {
			double t = zMin;
			zMin = zMax;
			zMax = t;
		}

		appendNewText(" - "+files.length+" grids");
		appendNewText("\n  bounds:");
		appendNewText("\n           west = "+wesn[0]);
		appendNewText("\n           east = "+wesn[1]);
		appendNewText("\n           south = "+wesn[2]);
		appendNewText("\n           north = "+wesn[3]);
		appendNewText("\n  z_range:");
		appendNewText("\n           min = "+zMin);
		appendNewText("\n           max = "+zMax);
		
		double offset = (zMax+zMin) * .5;
		double spread = zMax - zMin;
		double scale = 1.;
		while( spread*scale < 16000. ) scale*=2;
		if( scale>100. )scale=100.;
		File dir = files[0].getParentFile();
		File top = new File( dir +"/z_"+res );
		if( !top.exists() ) top.mkdirs();

		TileIO.Short tileIO = new TileIO.Short(proj, dir +"/z_"+res, 320, 0);
		tileIO.setReadonly(false);

		boolean nullGrid = false;
		if (log) {
			logFileName = "GridImports_" + name +
					"_" + FilesUtil.fileTimeEST().replace( ':', '-' ) + ".log";
			System.out.println(logFileName);
			MapApp app = (MapApp) suite.map.getApp();
			logFile = new File(app.gridImportsLogDir, logFileName);
			writer = new BufferedWriter(new FileWriter(logFile));
		}
		for( int k=0 ; k<files.length ; k++) {
			area.setText("Processing "+files[k].getName()+", "+ (k+1) +" of "+ files.length);
			area.update(area.getGraphics());
			if (grids[k].getGrid() != null) {			
				tile( grids[k].getGrid(), tileIO, proj, scale, offset, zScale[k], add_offset[k], res);
				//if logging, write filename to log
				if (log) {
					writer.write(FilesUtil.fileTimeEST() + ": " +files[k].getName());
					writer.newLine();
				}
			} else {
				nullGrid = true;
			}	
		}
		if (log) writer.close();
		
		dec.decimate( top , mapType == MapApp.MERCATOR_MAP );
//		1.4.4: Pass name so that new files are named according
//		to the name of the original file and not the directory 
//		the original file is located in.
		File shp = (new XBGtoShape()).open(dir, name, mapType);
		if (shp == null) {
			if (!nullGrid) showFormatError(files[0].getName());
			suite.map.getMapTools().shapeTB.doClick();
		}
		
		frame.dispose();
		if( shp!=null && suite!=null )suite.addShapeFile(shp);
		//add the z-units to the shape file
		Vector<ESRIShapefile> shapes = suite.getShapes();
		for (ESRIShapefile shape : shapes) {
			if (shape.getName().equals(name)) {
				shape.setDataType(dataType);
				shape.setUnits(zUnits);
			}
		}
		
	}
	
	void openGeotiff(File[] files) throws IOException {
		if(files.length == 0) return;
		String name = files[0].getParentFile().getName();
		if(1 == files.length) {
			name = files[0].getName();
			name = name.substring(0, name.lastIndexOf("."));
		}
		zScale = new double[files.length];
		zScale[0] = 1;
		add_offset = new double[files.length]; 
		area.setText(name);
		area.update(area.getGraphics());
		double furthestWest = Double.MAX_VALUE, furthestEast = -Double.MAX_VALUE, furthestNorth = -Double.MAX_VALUE, furthestSouth = Double.MAX_VALUE;
		displayWaitingDots();
		dxMin = dyMin = Double.MAX_VALUE;
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		GridFile[] gridFiles = new GridFile[files.length];
		for(currentIndex = 0; currentIndex < files.length; currentIndex++) {
			File file = files[currentIndex];
			//read the file with GeoTools
			GeoTiffReader reader = new GeoTiffReader(file);
			GeoTiffIIOMetadataDecoder mdd = reader.getMetadata();
			GridCoverage2D gridCoverage = reader.read(null);
			Envelope2D coordRange = gridCoverage.getGridGeometry().getEnvelope2D();
			GridEnvelope2D env = gridCoverage.getGridGeometry().getGridRange2D();
			GridCoordinates2D low = env.getLow(), high = env.getHigh();
			int size = (high.x - low.x + 1) * (high.y - low.y + 1);
			//TODO determine what the max size is before it gets to take too long to process
			System.out.println("Converting " + size + " cells to GMA's internal format");
			//assume for now it's not too big
			DirectPosition lowerCorner = coordRange.getLowerCorner(), upperCorner = coordRange.getUpperCorner();
			//convert the file to Grid2D
			//get the projection first
			final MapProjection proj = GTConverter.getGmaProj(gridCoverage.getGridGeometry());
			gridFiles[currentIndex] = new GeotiffGridFile(gridCoverage, env, proj, mdd);
			gridFiles[currentIndex].getGrid();
			double[] curWesn = ((GeotiffGridFile)gridFiles[currentIndex]).getWesn();
			if(furthestWest > curWesn[0]) furthestWest = curWesn[0];
			if(furthestEast < curWesn[1]) furthestEast = curWesn[1];
			if(furthestSouth > curWesn[2]) furthestSouth = curWesn[2];
			if(furthestNorth < curWesn[3]) furthestNorth = curWesn[3];
		}
		mostWest = furthestWest;
		mostEast = furthestEast;
		mostSouth = furthestSouth;
		mostNorth = furthestNorth;
		pd.setWESNRange(furthestWest, furthestEast, furthestSouth, furthestNorth);
		wesn = new double[] {furthestWest, furthestEast, furthestSouth, furthestNorth};
		pd.setMinMaxZ(zMin, zMax);
		waiting = false;
		tileGrids(name, files, gridFiles, 360./640);
		MapApp.sendLogMessage("Imported_GeoTIFF_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	void openPolarASC(File[] files)  throws IOException {
		String name = files[0].getParentFile().getName();
		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);
		area.update(area.getGraphics());

		GridFile[] grids = new GridFile[files.length];
		add_offset = new double[files.length];
		zScale = new double[files.length];
		zScale[0] = 1;
		applyForAll = false;
		currentIndex = 0;

		dxMin = Double.MAX_VALUE;
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		pd.showFlipGridCheckBox(false);

		for( int k=0 ; k<files.length ; k++) {
			ASC_PolarGrid gridFile = new ASC_PolarGrid(files[k]);
			gridFile.readHeader();

			final MapProjection proj = getPolarProjection(files[k].getName(), gridFile.cell_size, gridFile.width, gridFile.height);
			if (proj == null)
				return;

			gridFile.setProjection(proj);

			Grid2D grid = gridFile.getGrid();

			final File file = files[k];
			grids[k] = new GridFile() {
				public Grid2D getGrid() throws IOException{
					ASC_PolarGrid gridFile = new ASC_PolarGrid(file);
					gridFile.readHeader();
					gridFile.setProjection(proj);
					return gridFile.getGrid();
				}
			};

			dxMin = Math.min(gridFile.cell_size, dxMin);
			double[] wesnGrid = grid.getWESN();

			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);

			zMin = Math.min(gridFile.zMin, zMin);
			zMax = Math.max(gridFile.zMax, zMax);

			currentIndex++;
		}

		dyMin = dxMin;

		int res = 2;
		while (25600 / res > dxMin) res *= 2;
		tileGrids(name, files, grids, res);
		MapApp.sendLogMessage("Imported_PolarASC_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	void openNETCDF(File[] files ) throws IOException {		
		String name = files[0].getParentFile().getName();
		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);
		area.update(area.getGraphics());

		GridFile[] grids = new GridFile[files.length];
		zScale = new double[files.length];
		zScale[0] = 1;
		flipGrid = new boolean[files.length];
		add_offset = new double[files.length];
		applyForAll = false;
		currentIndex = 0;
		dxMin = dyMin = Double.MAX_VALUE;
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		//allow user to flip grid in case it comes out upside down
		pd.showFlipGridCheckBox(true);

		/* If more then one grid file is selected then compute the ceiling
		 * and floor Z value for all the selected files.
		 */
		if(files.length > 1){
			Double[] lowestMin = new Double[files.length];
			Double[] highestMax = new Double[files.length];
			Double[] westNegative = new Double[files.length];
			Double[] eastPositive = new Double[files.length];
			Double[] southNegative = new Double[files.length];
			Double[] northPositive = new Double[files.length];

			waiting = true;
			displayWaitingDots();
			for( int k=0 ; k<files.length ; k++) {
				GrdProperties gridP1 = new GrdProperties(files[k].getPath());
				// z range
				lowestMin[k] = gridP1.z_range[0];
				highestMax[k] = gridP1.z_range[1];
				// wesn
				westNegative[k] = gridP1.x_range[0];
				eastPositive[k] = gridP1.x_range[1];
				southNegative[k] = gridP1.y_range[0];
				northPositive[k] = gridP1.y_range[1];
				//System.out.println("w " + westNegative[k] + " e " + eastPositive[k] + " s " +  southNegative[k] + " n " + northPositive[k]);
				currentIndex++;
			}
			Arrays.sort(lowestMin);
			Arrays.sort(highestMax);
			zMinFloor = lowestMin[0];
			zMaxCeiling = highestMax[highestMax.length-1];

			// Set values in ProjectionsDialog
			pd.setFloorCeilingZ(zMinFloor, zMaxCeiling);

			Arrays.sort(westNegative);
			Arrays.sort(eastPositive);
			Arrays.sort(southNegative);
			Arrays.sort(northPositive);
			mostWest = westNegative[0];
			mostEast = eastPositive[eastPositive.length-1];
			mostSouth = southNegative[0];
			mostNorth = northPositive[northPositive.length-1];
			//System.out.println("w " + mostWest + " e " +  mostEast + " s " +  mostSouth + " n " +  mostNorth);
			pd.setWESNRange(mostWest, mostEast, mostSouth, mostNorth);
		}

		currentIndex = 0;
		zMinTemp = new Double[files.length];
		zMaxTemp = new Double[files.length];
//		Handling multiple input files and finding the min/max bounds of the set
		for( int k=0; k<files.length; k++) {
			appendNewText("\nReading " + files[k].getName() + " dimensions");
			GrdProperties gridP = new GrdProperties(files[k].getPath());
			// Get the WESN
			double[] emptyRange = new double[2];
			if (gridP.x_range == null || gridP.y_range == null || gridP.z_range == null ||
					Arrays.equals(gridP.x_range, emptyRange) || Arrays.equals(gridP.y_range, emptyRange) || Arrays.equals(gridP.z_range, emptyRange)){
				String msg = "Unable to open " + files[k].getName() + 
						".<br>The netCDF grid file cannot be read properly." +
						"<br><br>Use, for example, \"grdinfo\" to check that:" +
						"<br>(a) It is a standard 2-D netCDF file and not a multi-data set netCDF file." +
						"<br>(b) The header min/max values are valid numbers and not \"NaN\"." +
						"<br><br>Use, for example, \"grdinfo\" and \"grdreformat\" to rewrite the header.";
				//create an EditorPane to handle any html
			    JEditorPane ep = GeneralUtils.makeEditorPane(msg);
				JOptionPane.showMessageDialog(frame,ep , "Import Error", JOptionPane.ERROR_MESSAGE);
				return;
			}			
			double gridWESN[] = new double[] { gridP.x_range[0], gridP.x_range[1], gridP.y_range[0], gridP.y_range[1] };
			pd.setInitialZScale(Double.toString(gridP.scaleFactor));
			pd.setOffset(Double.toString(gridP.add_offset));
			pd.setMinMaxZ(gridP.z_range[0],gridP.z_range[1]);
			pd.setNoData(gridP.fillValue);

			// Set Attributes
			String [] gAttributes = GrdProperties.getHeader(files[0].toString());
			pd.setfileAtt(gAttributes);

			/*  If importing more then one grid at a time on all other windows but
			 *  the first remove edit and reset features
			 */
			if(files.length > 1){
				if(k > 0){
					pd.removeEditFeature();
					pd.removeResetFeature();
				}
			}
			waiting = false;
			final MapProjection proj = getProjection(files[k].getName(), gridP.getProjection(), gridWESN, gridP.dimension[0], gridP.dimension[1]);
			if ( proj == null ) {
				return;
			}
			waiting = true;
			displayWaitingDots();
			double[] wesnGrid = getGridWESN(proj, new Rectangle(0, 0, gridP.dimension[0], gridP.dimension[1]));

			if ( proj instanceof UTMProjection) {
				double dx = (wesnGrid[1] - wesnGrid[0]) / gridP.dimension[0];
				dxMin = Math.min(dx, dxMin);
			}
			else {
				dxMin = Math.min(gridP.spacing[0], dxMin);
			}
			
			dyMin = Math.min(gridP.spacing[1], dyMin);

			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);

			// If zMin changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridP.z_range[0],5)) != pd.getMinEdit()) {
				zMin = pd.getMinEdit();
			}else {
				zMin = Math.min(zScale[k] * (gridP.z_range[0] + add_offset[k]), zMin);
			}

			// If zMax changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridP.z_range[1],5))  != pd.getMaxEdit()){
				zMax = pd.getMaxEdit();
			} else{
				zMax =  Math.max(zScale[k] * (gridP.z_range[1] + add_offset[k]), zMax);
			}
			
			final File file = files[k];
			boolean thisFlipGrid = flipGrid[k];
			grids[k] = new GridFile() {
				public Grid2D getGrid() throws IOException {
					GrdProperties gridP = new GrdProperties(file.getPath(), thisFlipGrid);
					Grid2D grid = Grd.readGrd(file.getPath(), null, gridP, thisFlipGrid, noData);
					if (grid == null) {
						return null;
					}
					else {
						grid.projection = proj;
						return grid;
					}
				}
			};
			currentIndex++;
		}
		waiting = false;
		tileGrids(name, files, grids, 360. / 640);
		MapApp.sendLogMessage("Imported_NetCDF_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	void openESRI_ASCII( File[] files ) throws IOException {
		String name = files[0].getParentFile().getName();
		Double[] lowestMin = new Double[files.length];
		Double[] highestMax = new Double[files.length];

		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);
		area.update(area.getGraphics());
		
		GridFile[] grids = new GridFile[files.length];
		zScale = new double[files.length];
		zScale[0] = 1;
		add_offset = new double[files.length];
		applyForAll = false;
		currentIndex = 0;
		dxMin = dyMin = Double.MAX_VALUE;
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		pd.showFlipGridCheckBox(false);


		/* If more then one grid file is selected then compute the ceiling
		 * and floor Z value for all the selected files.
		 */
		if(files.length > 1){
			Double[] westNegative = new Double[files.length];
			Double[] eastPositive = new Double[files.length];
			Double[] southNegative = new Double[files.length];
			Double[] northPositive = new Double[files.length];
			waiting = true;
			displayWaitingDots();
			for( int k=0 ; k<files.length ; k++) {				
				ASC_Grid gridFile = new ASC_Grid(files[k]);
				gridFile.readHeader();
				gridFile.readGrid();

				lowestMin[k] = gridFile.zMin;
				highestMax[k] = gridFile.zMax;
		
				// wesn
				westNegative[k] = gridFile.x0;
				eastPositive[k] = gridFile.x0 + (gridFile.width - 1) * gridFile.dx;
				southNegative[k] = gridFile.y0;
				northPositive[k] = gridFile.y0 + (gridFile.height - 1) * gridFile.dx;
				//System.out.println("w " + westNegative[k] + " e " + eastPositive[k] + " s " +  southNegative[k] + " n " + northPositive[k]);
				currentIndex++;
			}
			Arrays.sort(lowestMin);
			Arrays.sort(highestMax);
			zMinFloor = lowestMin[0];
			zMaxCeiling = highestMax[highestMax.length-1];

			// Set values in ProjectionsDialog
			pd.setFloorCeilingZ(zMinFloor, zMaxCeiling);

			Arrays.sort(westNegative);
			Arrays.sort(eastPositive);
			Arrays.sort(southNegative);
			Arrays.sort(northPositive);
			mostWest = westNegative[0];
			mostEast = eastPositive[eastPositive.length-1];
			mostSouth = southNegative[0];
			mostNorth = northPositive[northPositive.length-1];
			//System.out.println("w " + mostWest + " e " +  mostEast + " s " +  mostSouth + " n " +  mostNorth);
			pd.setWESNRange(mostWest, mostEast, mostSouth, mostNorth);
		}

		currentIndex = 0;
		zMinTemp = new Double[files.length];
		zMaxTemp = new Double[files.length];

		for( int k=0 ; k<files.length ; k++) {
			ASC_Grid gridFile = new ASC_Grid(files[k]);

			gridFile.readHeader();
			gridFile.readGrid();
			double gridWESN[] = new double[] { gridFile.x0, gridFile.x0 + (gridFile.width - 1) * gridFile.dx,
					gridFile.y0, gridFile.y0 + (gridFile.height - 1) * gridFile.dx};

			pd.setMinMaxZ(gridFile.zMin, gridFile.zMax);
			pd.removeEditFeature(); // remove edit button for all
			pd.removeResetFeature(); // remove reset button for all

			waiting = false;
			final MapProjection proj = getProjection(files[k].getName(), gridFile.proj, gridWESN, gridFile.width, gridFile.height);
			if (proj == null)
				return;
			waiting = true;
			displayWaitingDots();

			gridFile.proj = proj;
			Grid2D grid = gridFile.getGrid();
				
			if (gridFile.proj instanceof UTMProjection) {
				double dx = (grid.getWESN()[1] - grid.getWESN()[0]) / gridFile.width;
				dxMin = Math.min(dx, dxMin);
			} else {
//				Not UTM projection
				dxMin = Math.min(gridFile.dx, dxMin);
			}

			double[] wesnGrid = grid.getWESN();
			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);

			// If zMin changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridFile.zMin,5)) != pd.getMinEdit()) {
				zMin = pd.getMinEdit();
			}else {
				zMin = Math.min(zScale[k] * (gridFile.zMin + add_offset[k]), zMin);
			}

			// If zMax changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridFile.zMax,5)) != pd.getMaxEdit()){
				zMax = pd.getMaxEdit();
			} else{
				zMax =  Math.max(zScale[k] * (gridFile.zMax + add_offset[k]), zMax);
			}
					
			final File file = files[k];

			grids[k] = new GridFile() {
				public Grid2D getGrid() throws IOException {
					ASC_Grid gridFile = new ASC_Grid(file);
					gridFile.readHeader();
					gridFile.proj = proj;
					return gridFile.getGrid();
				}
			};
			currentIndex++;
		}
		waiting = false;
		tileGrids(name, files, grids, 360. / 640);
		MapApp.sendLogMessage("Imported_ESRI_ASCII_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	void openESRI_Binary( File[] files ) throws IOException {
		String name = files[0].getParentFile().getName();
		Double[] lowestMin = new Double[files.length];
		Double[] highestMax = new Double[files.length];

		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);

		GridFile[] grids = new GridFile[files.length];
		zScale = new double[files.length];
		zScale[0] = 1;
		add_offset = new double[files.length];
		applyForAll = false;

		dxMin = dyMin = Double.MAX_VALUE;
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		pd.showFlipGridCheckBox(false);

		currentIndex = 0;
		zMinTemp = new Double[files.length];
		zMaxTemp = new Double[files.length];
		

		for( int k=0 ; k<files.length ; k++) {
			ESRI_Binary_Grid gridFile = new ESRI_Binary_Grid(files[k]);

			gridFile.readHeader();
			Grid2D grid = gridFile.getGrid();

			pd.setMinMaxZ(gridFile.zMin * zScale[k], gridFile.zMax * zScale[k]);
			pd.removeEditFeature(); // remove edit button for all
			pd.removeResetFeature(); // remove reset button for all

			double gridWESN[] = new double[] { gridFile.x0, gridFile.x0 + (gridFile.width - 1) * gridFile.dx,
					gridFile.y0, gridFile.y0 + (gridFile.height - 1) * gridFile.dx};

			final MapProjection proj = getProjection(files[k].getName(), gridFile.proj, gridWESN, gridFile.width, gridFile.height);
			if (proj == null)
				return;

			gridFile.proj = proj;

			if (gridFile.proj instanceof UTMProjection) {
				double dx = (grid.getWESN()[1] - grid.getWESN()[0]) / gridFile.width;
				dxMin = Math.min(dx, dxMin);
			}
			else
				dxMin = Math.min(gridFile.dx, dxMin);

			double[] wesnGrid = grid.getWESN();

			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);
		
			// If zMin changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridFile.zMin,5)) != pd.getMinEdit()) {
				zMin = pd.getMinEdit();
			}else {
				zMin = Math.min(zScale[k] * (gridFile.zMin + add_offset[k]), zMin);
			}

			// If zMax changed use edited value otherwise use original
			if(Double.parseDouble(GeneralUtils.formatToSignificant(gridFile.zMax,5)) != pd.getMaxEdit()){
				zMax = pd.getMaxEdit();
			} else{
				zMax =  Math.max(zScale[k] * (gridFile.zMax + add_offset[k]), zMax);
			}
			
			
			final File file = files[k];

			grids[k] = new GridFile() {
				public Grid2D getGrid() throws IOException {
					ESRI_Binary_Grid gridFile = new ESRI_Binary_Grid(file);
					gridFile.readHeader();
					gridFile.proj = proj;
					return gridFile.getGrid();
				}
			};
			currentIndex++;
		}
		dyMin = dxMin;
//		getGridsBounds(grids);
		tileGrids(name, files, grids, 360. / 640);
		MapApp.sendLogMessage("Imported_ESRI_Binary_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	// Process the .GRD98 grid file type
	void openGRD98( File[] files ) throws IOException {
		String name = files[0].getParentFile().getName();

		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}

		area.setText(name);
		area.update(area.getGraphics());

		GridFile[] grids = new GridFile[files.length];
		zScale = new double[files.length];
		zScale[0] = 1;
		add_offset = new double[files.length];
		applyForAll = false;
		currentIndex = 0;
		dxMin = dyMin = Double.MAX_VALUE;
		wesn = new double[] { Double.MAX_VALUE, - Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		pd.showFlipGridCheckBox(false);

		/* If more then one grid98 file is selected then compute the ceiling
		 * and floor Z value for all the selected files.
		 */
		if(files.length > 1){
			Double[] lowestMin = new Double[files.length];
			Double[] highestMax = new Double[files.length];
			for( int k=0 ; k<files.length ; k++) {
				GRD98_Grid gridFile1 = new GRD98_Grid(files[k]);
				gridFile1.readHeader();
				lowestMin[k] = gridFile1.zMin/gridFile1.precision;
				highestMax[k] = gridFile1.zMax/gridFile1.precision;
				currentIndex++;
			}
			Arrays.sort(lowestMin);
			Arrays.sort(highestMax);
			zMinFloor = lowestMin[0];
			zMaxCeiling = highestMax[highestMax.length-1];

			// Set values in ProjectionsDialog
			pd.setFloorCeilingZ(zMinFloor, zMaxCeiling);
		}

		currentIndex = 0;
		zMinTemp = new Double[files.length];
		zMaxTemp = new Double[files.length];
		for( int k=0 ; k<files.length ; k++) {
			GRD98_Grid gridFile = new GRD98_Grid(files[k]);
			gridFile.readHeader();
			double gridWESN[] = new double[] { gridFile.x0, gridFile.x0 + (gridFile.width - 1) * gridFile.dx, gridFile.y0, gridFile.y0 + (gridFile.height - 1) * gridFile.dx};
			if ( gridFile.dataFormat > 0 ) {
				pd.setInitialZScale(Double.toString((1.0/gridFile.precision)));
			}
			pd.setMinMaxZ(gridFile.zMin/gridFile.precision,gridFile.zMax/gridFile.precision);
//
			/*  If importing more then one grid at a time on all other windows but
			 *  the first remove edit and reset features
			 */
			if(files.length > 1){
				if(k > 0){
					pd.removeEditFeature();
					pd.removeResetFeature();
				}
			}

			final MapProjection proj = getProjection(files[k].getName(), gridFile.proj, gridWESN, gridFile.width, gridFile.height);
			if (proj == null) {
				return;
			}
			gridFile.proj = proj;

			// Get Grid
			Grid2D grid = gridFile.getGrid();
			if ( gridFile.proj instanceof UTMProjection ) {
				double dx = ( grid.getWESN()[1] - grid.getWESN()[0] ) / gridFile.width;
				dxMin = Math.min(dx, dxMin);
			}
			else {
				dxMin = Math.min(gridFile.dx, dxMin);
			}

			double[] wesnGrid = grid.getWESN();

			wesn[0] = Math.min(wesnGrid[0], wesn[0]);
			wesn[1] = Math.max(wesnGrid[1], wesn[1]);
			wesn[2] = Math.min(wesnGrid[2], wesn[2]);
			wesn[3] = Math.max(wesnGrid[3], wesn[3]);

			// If zMin changed use edited value otherwise use original
			if(gridFile.zMin != pd.getMinEdit() *gridFile.precision) {
				zMin = pd.getMinEdit();
				zMinTemp[k] = zMin;
			}else {
				zMin = Math.min(zScale[k] * (gridFile.zMin + add_offset[k]), zMin);
				zMinTemp[k] = zMin;
			}

			// If zMax changed use edited value otherwise use original
			if(gridFile.zMax != pd.getMaxEdit() * gridFile.precision){
				zMax = pd.getMaxEdit();
				zMaxTemp[k] = zMax;
			} else{
				zMax =  Math.max(zScale[k] * (gridFile.zMax + add_offset[k]), zMax);
				zMaxTemp[k] = zMax;
			}

			final File file = files[k];
			final Double zMinT = zMinTemp[k];
			final Double zMaxT = zMaxTemp[k];
			grids[k] = new GridFile() {
				public Grid2D getGrid() throws IOException {
					GRD98_Grid gridFile = new GRD98_Grid(file);
					gridFile.readHeader();
					gridFile.zMax = zMaxT *gridFile.precision;
					gridFile.zMin = zMinT *gridFile.precision;
					gridFile.proj = proj;
					return gridFile.getGrid();
				}
			};
			currentIndex++;
		}
		dyMin = dxMin;
		tileGrids(name, files, grids, 360. / 640);
	//	getGridsBounds(grids);
	/*	if(files.length > 1){
		tileGrids(name, files, grids, 360. / 640, zMinTemp, zMaxTemp);
		} else {
			tileGrids(name, files, grids, 360. / 640);
		} */
		
		MapApp.sendLogMessage("Imported_GRD98_Grid&name="+name+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
	}

	void tile(Grid2D grd, TileIO.Short tileIO, MapProjection mapProj, double scale, double offset, double zScale, double add_offset, int res) throws IOException {
		if (grd == null) return;

		Rectangle bounds = grd.getBounds();
		int x1, y1, x2, y2;
		x1 = y1 = Integer.MAX_VALUE;
		x2 = y2 = -Integer.MAX_VALUE;

		MapProjection gridProj = grd.getProjection();
		Point2D ul = gridProj.getRefXY( new Point(grd.bounds.x, grd.bounds.y) );
		Point2D lr = gridProj.getRefXY( 
				new Point(grd.bounds.x + bounds.width-1, grd.bounds.y + bounds.height-1) );

		boolean crossesMeridian = (ul.getX() < 0 && lr.getX() > 0) || (ul.getX() < 360 && lr.getX() > 360);
		
		if (mapType == MapApp.MERCATOR_MAP) {
			if( ul.getY()<-79. )ul.setLocation(ul.getX(), -79);
			if( lr.getY()<-79. )lr.setLocation(lr.getX(), -79);
			if( ul.getY()>81 )ul.setLocation(ul.getX(), 81);
			if( lr.getY()>81. )lr.setLocation(lr.getX(), 81);
		}

		double wrap;
		wrap = 360.*(bounds.width-1.)/(lr.getX()-ul.getX());
		if (!gridProj.isCylindrical())
			wrap = -1;

		// I think we only need to use mapWrap in Mercator
		double mapWrap = Double.MAX_VALUE;

		ul = mapProj.getMapXY(ul);
		lr = mapProj.getMapXY(lr);
		if( gridProj.isCylindrical() && mapType == MapApp.MERCATOR_MAP) {
			mapWrap = 360 * ((CylindricalProjection) mapProj).getScale();
			
			x1 = (int)Math.floor(ul.getX());
			y1 = (int)Math.floor(ul.getY());
			x2 = (int)Math.ceil(lr.getX());
			y2 = (int)Math.ceil(lr.getY());

			if (x1 > x2) {
				if (crossesMeridian) {
					x2 += mapWrap;
				} else {
					int tmp = x1;
					x1 = x2;
					x2 = tmp;				
				}
			}

			if (y1 > y2) {
				int tmp = y1;
				y1 = y2;
				y2 = tmp;
			}

//			 Fix for when -180 and 180 are reprojected to the equivalent x coordinate.
//			 Assume we want to tile the entire width of the map
			if (x1 == x2) {
				x1 = 0;
				x2 = 2 * res * 320;
			}
		} else {
//			mapWrap = 360 * ((PolarStereo) mapProj).getScale();
			double [] mapWESN = getMapWESN(gridProj, mapProj, bounds.x, bounds.y, bounds.width, bounds.height);
			x1 = (int) Math.floor(mapWESN[0]);
			y1 = (int) Math.floor(mapWESN[2]);
			x2 = (int) Math.ceil(mapWESN[1]);
			y2 = (int) Math.ceil(mapWESN[3]);
		}

		// Clamp the bounds to within the Polar Map
		if (mapType != MapApp.MERCATOR_MAP) {
			x1 = Math.max(x1, -320 * res);
			y1 = Math.max(y1, -320 * res);
			x2 = Math.min(x2, 320 * res);
			y2 = Math.min(y2, 320 * res);
		}

		int ix1 = (int)Math.floor(x1/320.);
		int iy1 = (int)Math.floor(y1/320.);
		int ix2 = (int)Math.ceil(x2/320.);
		int iy2 = (int)Math.ceil(y2/320.);

		double totalCount = 0;
		appendNewText("\nTiling from X: " + ix1 + " to " + ix2 + "\n\t and Y: " + iy1 + " to " + iy2);
		waiting = true;
		displayWaitingDots();

		for( int ix=ix1 ; ix<=ix2 ; ix++) {
			int xA = (int)Math.max(ix*320, x1);
			int xB = (int)Math.min((ix+1)*320, x2);
			if (xA >= mapWrap) {
				xA -= mapWrap;
				xB -= mapWrap;
			}
			for( int iy=iy1 ; iy<=iy2 ; iy++) {

				int yA = (int)Math.max(iy*320, y1);
				int yB = (int)Math.min((iy+1)*320, y2);
				Grid2D.Short tile=null;
				try {
					tile = (Grid2D.Short)tileIO.readGridTile(xA, yA);
				} catch(Exception ex) {
					tile = (Grid2D.Short)tileIO.createGridTile(xA, yA);
					tile.scale(offset, scale);
				}
				//area.append("\nTile "+ix+", "+iy);
				//area.update(area.getGraphics());
				
				boolean hasData = false;
				int count = 0;
				for( int x=xA ; x<xB ; x++) {
					for( int y=yA ; y<yB ; y++) {
						Point2D.Double p0 = (Point2D.Double)mapProj.getRefXY(new Point(x, y));
						Point2D.Double p = (Point2D.Double)gridProj.getMapXY(p0);
						p.setLocation(p.getX() + 1e-5, p.getY());
						if( wrap>0. ) {
							while(p.x>=bounds.x+bounds.width)p.x-=wrap;
							while(p.x<bounds.x)p.x+=wrap;
						}
						double val = grd.valueAt(p.x, p.y);
						if( Double.isNaN(val))continue;

						hasData = true;
	
						if ( zScale < 0 ) {
							tile.setValue(x, y, (val + add_offset) * zScale, true);
						}
						else {
							tile.setValue(x, y, (val + add_offset) * zScale);
						}
						count++;
					}
				}
				//area.append(" - "+count+" points");
				//area.update(area.getGraphics());
				double[] scales = tile.getScales();
				if( hasData ) {
					tileIO.writeGridTile(tile);
				}
				totalCount += count;
			}
		}
		waiting = false;
	}

	public static double[] getGridWESN(MapProjection projection, Rectangle bounds) {
		double[] wesn = new double[] {0.,0.,0.,0.};
		if( projection.isCylindrical() ) {
		//	CylindricalProjection p = (CylindricalProjection)projection;
			Point2D ul = projection.getRefXY( bounds.getX(), bounds.getY() );
			Point2D lr = projection.getRefXY( bounds.getX()+bounds.getWidth(), bounds.getY()+bounds.getHeight() );
			wesn[0] = ul.getX();
			wesn[1] = lr.getX();
			if( wesn[1]<wesn[0] ) wesn[1]+=360;
			wesn[2] = lr.getY();
			wesn[3] = ul.getY();
			return wesn;
		}
		boolean start=true;
		Point2D.Double p;
		for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			p = (Point2D.Double)projection.getRefXY( new Point(x,bounds.y) );
			if( start ) {
				start=false;
				wesn[0] = wesn[1] = p.x;
				wesn[2] = wesn[3] = p.y;
				continue;
			}
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
			p = (Point2D.Double)projection.getRefXY( new Point(x,bounds.y+bounds.height) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
		}
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			p = (Point2D.Double)projection.getRefXY( new Point(bounds.x,y) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
			p = (Point2D.Double)projection.getRefXY( new Point(bounds.x+bounds.width,y) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
		}
		try {
			p = (Point2D.Double)projection.getMapXY( new Point(0, 90 ) );
			if (bounds.contains(p))
				wesn[3]=90.;
		} catch(Exception e) {
		}
		try {
			p = (Point2D.Double)projection.getMapXY( new Point(0, -90 ) );
			if( bounds.contains(p))
				wesn[2]=-90.;
		} catch(Exception e) {
		}
		return wesn;
	}

	public static double[] getMapWESN(MapProjection gridProj,
									MapProjection mapProj,
									double grid_x0,
									double grid_y0,
									double width,
									double height) {
		double x0, x1, y0, y1;
		x0 = y0 = Double.MAX_VALUE;
		x1 = y1 = - Double.MAX_VALUE;

		Point2D p;
		Point2D.Double pt = new Point2D.Double();
		for( int x=0 ; x<width ; x++) {
			pt.x = x + grid_x0;
			pt.y = grid_y0;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));

			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());

			pt.y = height + grid_y0 - 1;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));

			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
		}
		for( int y=0 ; y<height ; y++) {
			pt.x = grid_x0;
			pt.y = y + grid_y0;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));

			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());

			pt.x = width + grid_x0 - 1;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));

			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
		}
		return new double[] {x0, x1, y0, y1};
	}

	public interface GridFile {
		public Grid2D getGrid() throws IOException;
	}

	/*
	 * append new text to the Import Grid log area
	 */
	protected void appendNewText(final String txt) {
		Runnable runnable = new UpdateArea();
		Thread thread = new Thread(runnable);
		areaText = txt;
		thread.start();
	}	

	class UpdateArea implements Runnable {
		public void run() {
			area.append(areaText);
			area.setCaretPosition(area.getText().length() -1);
			area.update(area.getGraphics());
		}
	}

	/*
	 * Add a Please Wait... message top the Import Grid log area.
	 * Add a new . every 2s until the global variable 'waiting' is set to false. 
	 */
	protected void displayWaitingDots() {
		Runnable runnable = new WaitingDots();
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	class WaitingDots implements Runnable {
		public void run() {
			area.append("\nPlease wait.");
			area.update(area.getGraphics());
			while (waiting) {
				try {
				    Thread.sleep(2000);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
				//check area hasn't closed whilst we were waiting
				try {
					area.append(".");
					area.update(area.getGraphics());
				}
				catch(Exception e) {}
			}
		}
	}
	
	private void showFormatError (String filename) {
		String msg = "Unable to open " + filename + ". <br>Incompatible file format."
				+ "<html><br>See <a href=\""+PathUtil.getPath("PUBLIC_HOME_PATH")+"FAQ.html#ImportingData\">"
				+ "GeoMapApp FAQ</a> for supported formats.</html> ";
		//create an EditorPane to handle the html and hyperlink
	    JEditorPane ep = GeneralUtils.makeEditorPane(msg);
		JOptionPane.showMessageDialog(frame,ep , "Import Error", JOptionPane.ERROR_MESSAGE);
	}
}