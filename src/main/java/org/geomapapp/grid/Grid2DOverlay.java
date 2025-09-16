package org.geomapapp.grid;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.image.Palette;
import org.geomapapp.image.RenderingTools;
import org.geomapapp.util.XML_Menu;

import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.map.MapTools;
import haxby.map.XMap;
import haxby.proj.Mercator;
import haxby.proj.ProjectionFactory;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager.LayerPanel;

public class Grid2DOverlay extends MapOverlay {
	private boolean imported = false;
	protected String name;
	protected Grid2D grid;
	protected JDialog modifyContour;
	protected Grid2D.Boolean landMask,
							dataMask;
	protected Point2D p0;
	protected double gridScale;
	protected boolean land, ocean;
	protected int background = 0xff646464;
	public ContourGrid contour;
	public RenderingTools lut;
	public double interval = -1;
	public double bolding_interval = -1;
	public int [] cb = new int[2];

	public Grid2DOverlay( XMap map ) {
//		1.3.5: Changed "DEM" to "Topography"
//		this( map, "DEM" );
		this( map,  org.geomapapp.grid.GridDialog.DEM );
	}

	public Grid2DOverlay( XMap map, String name ) {
		super(map);
		grid = null;
		lut = null;
		contour = new ContourGrid( this );
		this.name = name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String toString() {
		return name;
	}
	public void fillNaNs() {
		if( grid==null )return;
		grid.fillNaNs();
		lut.gridImage();
	}
	public void setBackground( int argb ) {
		background = argb;
		if(lut!=null) lut.setBackground( argb );
	}
	int[] reviseContours(int[] c, double interval, double bolding_interval) {
		NumberFormat fmt = NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(0);
		fmt.setGroupingUsed(false);
		JPanel panel = new JPanel(new GridLayout(0, 1));
		JTextField c1 = new JTextField(fmt.format(c[0]*interval));
		JTextField c2 = new JTextField(fmt.format(c[1]*interval));
		JTextField con = new JTextField(fmt.format(interval));
		JTextField bolding = new JTextField(fmt.format(bolding_interval));
		JLabel label = new JLabel("Modify contour interval and/or range?");
		panel.add(label);
		JPanel p1 = new JPanel( new GridLayout(1,0));
		label = new JLabel("Interval", label.CENTER);
		p1.add( label );
		p1.add( con );
		panel.add(p1);
		p1 = new JPanel( new GridLayout(1,0));
		label = new JLabel("Bolding interval", label.CENTER);
		p1.add( label );
		p1.add( bolding );
		panel.add(p1);
		p1 = new JPanel( new GridLayout(1,0));
		label = new JLabel("Minimum", label.CENTER);
		p1.add( label );
		p1.add( c1 );
		panel.add(p1);
		p1 = new JPanel( new GridLayout(1,0));
		label = new JLabel("Maximum", label.CENTER);
		p1.add( label );
		p1.add( c2 );
		panel.add(p1);
		String title = "Modify Contours?";
		while(true) {
			int ok = JOptionPane.showConfirmDialog(lut, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if( ok==JOptionPane.CANCEL_OPTION ) {
				this.interval=-1;
//				1.3.5: Deselect the contour button when the user cancels the contour window
				lut.setContourTBUnselected();
				return c;
			} else if(ok==JOptionPane.OK_OPTION){
				System.out.println("ok");
			}

/*			// 1.3.5: Code no longer applies with no NO button
			else if(ok==JOptionPane.NO_OPTION ) {
				this.interval = -1;
				lut.setContourTBUnselected();
				return c;
			}
*/

			title = "try again";
			try {
				double val = Double.parseDouble(con.getText());
				if( val <=0.) return c;
				double bolding_val = Double.parseDouble(bolding.getText());
				if( bolding_val < 0.) return c;
				double min = Double.parseDouble(c1.getText());
				double max = Double.parseDouble(c2.getText());
				c[0] = (int)Math.ceil(min/val);
				c[1] = (int)Math.ceil(max/val);
				this.interval = val;
				this.bolding_interval = bolding_val;
				return c;
			} catch(Exception ex) {
			}
		}
	}
	public void contourGrid() {
		if( grid==null ) {
			return;
		}
		double[] range = grid.getRange();
		double interval = this.interval;
		double bolding_interval = this.bolding_interval;
		if (interval <= 0) {
			interval = (range[1] - range[0]) / 10;
			int i = 0;
			while (interval > 1) {
				interval /= 10;
				i++;
			}
			if (i > 0) {
				if (interval > .75)
					interval = Math.pow(10, i);
				else if (interval > .25)
					interval = 5 * Math.pow(10, i - 1);
				else 
					interval = Math.pow(10, i - 1);
			}
			if (bolding_interval <= 0) {
				bolding_interval = 5 * interval;
			}
		} else {
			interval = this.interval;
			bolding_interval = this.bolding_interval;
		}
		int[] c = new int[] {
			(int)Math.floor(range[0]/interval),
			(int)Math.ceil(range[1]/interval)
		};
		c = reviseContours(c, interval, bolding_interval);
		cb = c;
		interval = this.interval;
		if( interval <= 0 ) {
			if( !contour.isVisible() ) {
				return;
			}
			contour.setVisible( false );
			return;
		}

		((MapApp)map.getApp()).addProcessingTask("Contouring Grid...", new Runnable() {
			public void run() {
				try {
		//				***** Changed by A.K.M. 06/28/06 *****
		//				Display busy icon while contour info is processed and displayed
						lut.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//							map.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//							map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//				***** Changed by A.K.M. 06/28/06 *****
						contour.contour( Grid2DOverlay.this.interval, Grid2DOverlay.this.bolding_interval, cb );
		//			}
					contour.setVisible( true );
					map.repaint();
				}
				finally {
					if (lut != null)
						lut.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
	}
	public double getInterval() {
		return contour.getInterval();
	}
	public double getBolding() {
		return contour.getBolding();
	}
	public boolean isVisible() {
		return contour.isVisible();
	}
	public void setGrid( Grid2D grid, Grid2D.Boolean landMask, boolean hasLand, boolean hasOcean, boolean reset ) {
		setGrid( grid, landMask, hasLand, hasOcean, reset, false);
	}
	public void setGrid( Grid2D grid, Grid2D.Boolean landMask, boolean hasLand, boolean hasOcean ) {
		setGrid( grid, landMask, hasLand, hasOcean, true);
	}
	public void setGrid( Grid2D grid, Grid2D.Boolean landMask, boolean hasLand, boolean hasOcean, boolean reset, boolean isGMRT ) {
		this.landMask = landMask;
		land = hasLand;
		ocean = hasOcean;
		if( this.grid==null && grid==null)return;
		this.grid = grid;
		if( grid==null ) return;
		if( !reset ) return;
		Rectangle r = grid.getBounds();
		p0 = new Point2D.Double(r.getX(), r.getY());
		p0 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p0));
		Point2D p1 = new Point2D.Double(r.getX()+1., r.getY());
		p1 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p1));
		if( p1.getX()<p0.getX() ) {
			gridScale = p1.getX()+map.getWrap()-p0.getX();
		} else {
			gridScale = p1.getX() - p0.getX();
		}
		contour.setGrid(grid);
		if( lut==null ) {
			try {
				initRenderer();
			} catch(Exception e) {
				return;
			}
		}
		lut.setNewGrid(isGMRT);
		//if (toString().equals(GridDialog.DEM))
	//	lut.showDialog();
	}
	public boolean hasLand() {
		return land;
	}
	public boolean hasOcean() {
		return ocean;
	}
	protected void initRenderer() {
		if( lut!=null ) return;
		lut = new RenderingTools(this);
		lut.setBackground( background );
	}
	public RenderingTools getRenderer() {
		initRenderer();
		return lut;
	}
	public double getScale() {
		return gridScale;
	}
	public double[] getOffsets() {
		return new double[] { p0.getX(), p0.getY() };
	}
	public Grid2D getGrid() {
		return grid;
	}
	public Grid2D.Boolean getLandMask() {
		return landMask;
	}
	public boolean isGridNull()	{
		return grid == null;
	}
	public void dispose() {
		if ( lut != null ) {
			lut.dispose();
			lut = null;
		}
		if ( contour != null ) {
			contour.dispose();
		}
		grid=null;
		image=null;

		if (getXMap() != null) {
			MapApp app = (MapApp) getXMap().getApp();
			if (app.getMapTools().gridToSave == this)
				app.setToolsGrid(null);
		}
	}
	public float getZ( Point2D lonlat ) {
		if( grid==null ) return Float.NaN;
		double wrap = map.getWrap();
		Point2D pt = grid.getProjection().getMapXY(lonlat);
		if( wrap>0. ) {
			wrap /= gridScale;
			double x = pt.getX();
			Rectangle r = grid.getBounds();
			while( x<r.x )x+=wrap;
			while( x>r.x+r.width ) x-=wrap;
			pt.setLocation( x, pt.getY() );
		}
		float z;
		if (name.equals(org.geomapapp.grid.GridDialog.DEM)) {
			z = (float)grid.valueAt( pt.getX(), pt.getY(), true);
		}
		else {
			z = (float)grid.valueAt( pt.getX(), pt.getY());
		}
//	System.out.println( pt.getX() +"\t"+ pt.getY() 
//			+"\t"+ grid.contains( (int)pt.getX(), (int)pt.getY())+"\t"+ z);
		return z;
	}	
	public double valueAt( Point2D lonlat ) {
		if( grid==null ) return Float.NaN;
		double wrap = map.getWrap();
		Point2D pt = grid.getProjection().getMapXY(lonlat);
		if( wrap>0. ) {
			wrap /= gridScale;
			double x = pt.getX();
			Rectangle r = grid.getBounds();
			while( x<r.x )x+=wrap;
			while( x>r.x+r.width ) x-=wrap;
			pt.setLocation( x, pt.getY() );
		}
		return grid.valueAt( pt.getX(), pt.getY());
	}
	public void draw(Graphics2D g) {
		g.setColor( Color.black );
		g.setStroke( new BasicStroke(1f/(float)map.getZoom()));
		AffineTransform at = g.getTransform();
		Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
		double wrap = map.getWrap();
		if( !mask || contour == null ||!contour.isVisible() ) {
			super.draw(g);
			if( contour != null && contour.isVisible() ) {
				if( wrap>0 ) {
					double x = p0.getX();
					while( x>rect.x ) {
						g.translate(-wrap, 0.);
						x -= wrap;
					}
					while( x<rect.x+rect.width ) {
						contour.draw(g);
						g.translate(wrap, 0.);
						x += wrap;
					}
				} else {
					contour.draw(g);
				}
				g.setTransform(at);
			}
			return;
		}
		super.drawImage( g );
		if( contour != null && contour.isVisible() ) {
			if( wrap>0 ) {
				double x = p0.getX();
				while( x>rect.x ) {
					g.translate(-wrap, 0.);
					x -= wrap;
				}
				while( x<rect.x+rect.width ) {
					contour.draw(g);
					g.translate(wrap, 0.);
					x += wrap;
				}
			} else {
				contour.draw(g);
			}
			g.setTransform(at);
		}
		super.drawMask( g );
	}

	public void savePS() {
		Mercator merc = ProjectionFactory.getMercator(640);
		double x = x0;
		while( x>640. )x-=640.;
		double y=y0-260.;
		System.err.println( x0 +"\t"+ (y0-260.) +"\t"+ scale);
		Point2D p1 = merc.getRefXY(new Point2D.Double(x,y));
		Point2D p2 = merc.getRefXY( new Point2D.Double(
				x+image.getWidth()*scale,
				y+image.getHeight()*scale));
		double[] wesn = new double[] {p1.getX(),
				p2.getX(),
				p2.getY(),
				p1.getY() };
		while( wesn[0]<-180. ) wesn[0] += 180.;
		while( wesn[0]>=180. ) wesn[0] -= 180.;
		wesn[1] = wesn[0] + image.getWidth()*360.*scale/640.;
		System.err.println( wesn[0] +"\t"+ 
				wesn[1] +"\t"+
				wesn[2] +"\t"+
				wesn[3]);
		int width = image.getWidth();
		int height = image.getWidth();
		System.out.println( "%" );
		System.out.println( "% PostScript produced by GeoMapApp, commad like:\n%" );
		double scl = 1.;
		System.out.println( "%% grdimage grdfile -Jm" + scl +" -R"+
				wesn[0]+"/"+wesn[1]+"/"+ wesn[2]+"/"+wesn[3] +" -K -O\n");
		System.out.println( "%\n% Activate Map clip path\n%\n");
		System.out.println( "% Start of clip path\nS V\n0 0 M\n");
	}
	/**
		Save the grid.
	*/
	public void saveGrid(File file) throws IOException {
		PrintStream out = new PrintStream(
				new FileOutputStream(file));

		Rectangle rect = grid.getBounds();
		Point p = new Point();

		for (int y=rect.y; y < rect.y+rect.height; y++) {
			p.y = y;
			for (int x=rect.x; x<rect.x+rect.width; x++) {
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
		try {
//			***** GMA 1.6.4: Default save for GRD files is now in GMT-4 format
//			NetCDFGrid2D.createStandardGrd( grid, file );
			Grd.writeGrd( grid, file.getPath() );
//			***** GMA 1.6.4

		} catch(IOException ex) {
			ex.printStackTrace();
			throw ex;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	***** GMA 1.6.4: Create new function to retain functionality to save as GMT-3 GRD file
	public void saveGrdGMT3( File file ) throws IOException {
		try {
			NetCDFGrid2D.createStandardGrd( grid, file );
		} catch(IOException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	// Save as ESRI will give choice for binary or ascii
	public void saveGridToESRI() {
		Rectangle rect = grid.getBounds();
		MapProjection proj = grid.getProjection();

		double minLon = proj.getRefXY(rect.x, rect.y).getX();
		double minLat = proj.getRefXY(rect.x, rect.getMaxY()).getY();

		double maxLon = proj.getRefXY(rect.getMaxX(), rect.y).getX();
		double maxLat = proj.getRefXY(rect.x, rect.y).getY();

		double dLon = maxLon - minLon;
		double dLat = maxLat - minLat;

		// if second x is wrapped
		if ((int) Math.rint(proj.getMapXY(maxLon, 0).getX()) !=
			(int) Math.rint(rect.getMaxX()))
			// if first x is NOT wrapped
			if ((int) Math.rint(proj.getMapXY(minLon, 0).getX()) ==
				(int) Math.rint(rect.x))
				dLon += 360;

		int nCols = rect.width;
		int nRows = (int) (nCols * dLat / dLon);

		double cell_size = dLon / nCols;

		File file = new File("untitled" + MapTools.saveCount++);

		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Save " + nCols + " by " + nRows + " grid in ESRI format as?");
		panel.add( label , BorderLayout.NORTH);

		JPanel p2 = new JPanel(new GridLayout(0,1));
		ButtonGroup bg = new ButtonGroup();
		JRadioButton b = new JRadioButton("ASCII", true);
		bg.add(b);
		p2.add(b);
		final JRadioButton b2 = new JRadioButton("Binary");
		bg.add(b2);
		p2.add(b2);
		panel.add(p2);

		int s = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save grid", JOptionPane.OK_CANCEL_OPTION);
		// Close on cancel
		if(s == JOptionPane.CANCEL_OPTION) {
			return;
		}

		JFileChooser chooser = MapApp.getFileChooser();
		chooser.setSelectedFile(file);
		chooser.setFileFilter(null);
		int confirm = JOptionPane.NO_OPTION;

		while (confirm == JOptionPane.NO_OPTION) {
			int ok = chooser.showSaveDialog(map.getTopLevelAncestor());
			if (ok == chooser.CANCEL_OPTION)
				return;
			// append .asc to file if save as ESRI ascii is choosen 
			file = chooser.getSelectedFile();
			if ( !file.getName().endsWith(".asc") && !b2.isSelected()) {
				file = new File(file.getPath() + ".asc");
			}

			if (file.exists()) {
				confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
				if (confirm == JOptionPane.CANCEL_OPTION) return;
			}
			else
				break;
		}

		final File saveTo = file;
		final int cols = nCols;
		final int rows = nRows;
		final double mLon = minLon;
		final double mLat = minLat;
		final double c_size = cell_size;

		((MapApp) map.getApp()).addProcessingTask("Saving Grid...", new Runnable() {
			public void run() {
				try {
					if (b2.isSelected())
						saveGridToESRI_Bin(saveTo, cols, rows, mLon, mLat, c_size);
					else
						saveGridToESRI_ASCII(saveTo, cols, rows, mLon, mLat, c_size);
				} catch(Exception ex) {
						JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
								"an error occurred during this operation:\t"
								+ " "+ ex.getMessage());
				}
			}
		});
	}

	private void saveGridToESRI_Bin(File file, int nCols, int nRows, double minLon, double minLat, double cell_size) throws IOException {
		File header = new File(file.getParent(), file.getName() + ".hdr");
		// If xcorner is greater than 180 we convert to number between 0-180 and add a negative sign for west longitude.
		if(minLon > 180) {
			minLon -= 360;
		}

		PrintStream out = new PrintStream(new FileOutputStream(header));
		out.print("ncols ");
		out.println(nCols);
		out.print("nrows ");
		out.println(nRows);
		out.print("xllcorner ");
		out.println(minLon);
		out.print("yllcorner ");
		out.println(minLat);
		out.print("cellsize ");
		out.println(cell_size);
		out.print("nodata_value ");
		out.println(-Integer.MIN_VALUE);
		out.print("byteorder ");
		out.println("msbfirst");
		out.flush();
		out.close();

		File data = new File(file.getParent(), file.getName() + ".flt");

		DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(data)));

		MapProjection proj = grid.getProjection();
		Rectangle rect = grid.getBounds();

		double lat = minLat + nRows * cell_size;
		Point2D p = proj.getMapXY(0, lat);;
		double gridX;

		for (int row = 0; row < nRows; row++) {
			lat -= cell_size;
			gridX = rect.x;
			for (int col = 0; col < nCols; col++) {
				p = proj.getMapXY(0, lat);
				double value = grid.valueAt(gridX, p.getY());

				if (Double.isNaN(value))
					dos.writeDouble(-Integer.MIN_VALUE);
				else
					dos.writeDouble(value);

				gridX++;
			}
			out.println();
		}
		dos.flush();
		dos.close();
	}

	private void saveGridToESRI_ASCII(File file, int nCols, int nRows, double minLon, double minLat, double cell_size) throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(file));

		out.print("ncols ");
		out.println(nCols);
		out.print("nrows ");
		out.println(nRows);
		out.print("xllcorner ");
		out.println(minLon);
		out.print("yllcorner ");
		out.println(minLat);
		out.print("cellsize ");
		out.println(cell_size);
		out.print("nodata_value ");
		out.println(-Integer.MIN_VALUE);

		MapProjection proj = grid.getProjection();
		Rectangle rect = grid.getBounds();

		double lat = minLat + nRows * cell_size;
		Point2D p = proj.getMapXY(0, lat);;
		double gridX;

		for (int row = 0; row < nRows; row++) {
			lat -= cell_size;
			gridX = rect.x;
			for (int col = 0; col < nCols; col++) {
				p = proj.getMapXY(0, lat);
				double value = grid.valueAt(gridX, p.getY());
				if (Double.isNaN(value))
					out.print(-Integer.MIN_VALUE);
				else
					out.print(value);
				out.print(" ");
				gridX++;
			}
			out.println();
		}
		out.flush();
		out.close();
	}

	public void saveMaskedGrd( File file ) throws IOException {
		throw new IOException("CDF IO not yet available");
/*
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
		URLMasker masker = URLFactory.urlMasker( 320, res, 1, nLevel, proj, 
				MGridServer.getBaseURL()+"merc_320_1024" );
		Mask mask = new Mask( bounds[0], bounds[1], bounds[2]-bounds[0], bounds[3]-bounds[1], masker);
		NetCDFGrid.createStandardGrd( grid, mask, file );
*/
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
	
	public void setIsImported(boolean b) {
		imported  = b;
	}
	public boolean isImported() {
		return imported;
	}
	public String getName() {
		return name;
	}
	
	/*
	 * load the saved grid parameters from the session XML file
	 */
	public void loadSessionParameters(XML_Menu inputXML_Menu) {
		//if no grid values are present, just return
		if (inputXML_Menu.grid == null || inputXML_Menu.grid.equals("false")) return;

		//set default palette
		Palette pal = makePalette(inputXML_Menu.grid_dpal_name, inputXML_Menu.grid_dpal_r, inputXML_Menu.grid_dpal_g, inputXML_Menu.grid_dpal_b, 
				inputXML_Menu.grid_dpal_ht, inputXML_Menu.grid_dpal_discrete, inputXML_Menu.grid_dpal_range);
		if (pal != null) lut.setDefaultPalette(pal);

		//set land palette
		pal = makePalette(inputXML_Menu.grid_lpal_name, inputXML_Menu.grid_lpal_r, inputXML_Menu.grid_lpal_g, inputXML_Menu.grid_lpal_b, 
				inputXML_Menu.grid_lpal_ht, inputXML_Menu.grid_lpal_discrete, inputXML_Menu.grid_lpal_range);
		if (pal != null) lut.setLandPalette(pal);
		
		//set ocean palette
		pal = makePalette(inputXML_Menu.grid_opal_name, inputXML_Menu.grid_opal_r, inputXML_Menu.grid_opal_g, inputXML_Menu.grid_opal_b, 
				inputXML_Menu.grid_opal_ht, inputXML_Menu.grid_opal_discrete, inputXML_Menu.grid_opal_range);
		if (pal != null) lut.setOceanPalette(pal);
		
		//set up buttons depending on which palette was displayed when the session was saved
		lut.setWhichPalette(inputXML_Menu.grid_which_pal);

		//set V.E.
		if (inputXML_Menu.grid_ve != null) {
			lut.setVE(Float.parseFloat(inputXML_Menu.grid_ve));
		}
		//set diamond tabs on histogram
		if (inputXML_Menu.grid_tabs != null && inputXML_Menu.grid_tabs != "") {
			int[] tabs = GeneralUtils.string2IntArray(inputXML_Menu.grid_tabs);
			lut.getScaler().setTabs(tabs);
		}
		//add sun illumination settings
		if (inputXML_Menu.grid_illum != null && inputXML_Menu.grid_az != null && inputXML_Menu.grid_alt != null) {
			boolean illum = inputXML_Menu.grid_illum.equals("true") ? true : false;
			lut.setSunOn(illum);
			lut.getSunTool().setDeclination(Double.parseDouble(inputXML_Menu.grid_az));
			lut.getSunTool().setInclination(Double.parseDouble(inputXML_Menu.grid_alt));
		}
		
		//add contour settings
		if (inputXML_Menu.grid_contours != null) {
			boolean contours = inputXML_Menu.grid_contours.equals("true") ? true : false;
			lut.setContourSelected(contours);
		}
		if (inputXML_Menu.grid_cont_int != null && inputXML_Menu.grid_cont_min != null && inputXML_Menu.grid_cont_max != null) {
			interval = Double.parseDouble(inputXML_Menu.grid_cont_int);
			cb[0] = Integer.parseInt(inputXML_Menu.grid_cont_min);
			cb[1] = Integer.parseInt(inputXML_Menu.grid_cont_max);
		}
		if (inputXML_Menu.grid_cont_bolding != null) {
			bolding_interval = Double.parseDouble(inputXML_Menu.grid_cont_bolding);
		}
		
		lut.setFitToStDev(false);
		lut.showDialog();

		MapApp app = (MapApp)map.getApp();
		// do a refresh to update the palettes, etc
		app.getMapTools().getGridDialog().refreshGrids();
		
		// Add the xml_menu to the layerpanel for this grid
		LayerPanel layerPanel = app.layerManager.getLayerPanel(this);
		if (layerPanel != null) layerPanel.setItem(inputXML_Menu);
		
		map.repaint();
	}
	
	/*
	 * Create a palette from parameters stored in a saved session XML file
	 */
	public Palette makePalette(String palName, String palR, String palG, String palB, String palHT, String disc_interval, String palRange) {
		Palette pal = null;
		if ( palR != null && palG != null && palB != null  && palHT != null) {
			
			float[] r = GeneralUtils.string2FloatArray(palR);
			float[] g = GeneralUtils.string2FloatArray(palG);
			float[] b = GeneralUtils.string2FloatArray(palB);
			float[] ht = GeneralUtils.string2FloatArray(palHT);
			pal = new Palette(r, g, b, ht);

			if (palRange != null) {
				float[] range = GeneralUtils.string2FloatArray(palRange);
				pal.setRange(range[0], range[1]);
			}
						
			if (disc_interval != null) {
				pal.setDiscrete(Float.parseFloat(disc_interval));
			}
			
		}
		if (palName != null && palName != "") {
			pal.setName(palName);
		} else pal.setName("unknown");
		
		return pal;
	}
	
	/*
	 * Get the units for the grid
	 */
	public String getUnits() {
		if (GridDialog.GRID_UNITS.containsKey(name)) {
			return GridDialog.GRID_UNITS.get(name);
		}else {
			//for contributed grid, need to find the units from the ESRIShapefile object
			for (LayerPanel layerPanel : ((MapApp)map.getApp()).layerManager.getLayerPanels()) {
				if (layerPanel.layer instanceof ESRIShapefile && ((ESRIShapefile)layerPanel.layer).equals(this)) {
					ESRIShapefile esf = (ESRIShapefile)(layerPanel.layer);
					return esf.getGridUnits();
				}
			}
		}
		return "";
	}
	
	/*
	 * Get the data type for the grid
	 */
	public String getDataType() {
				
		//for contributed grid, need to find the units from the ESRIShapefile object
		for (LayerPanel layerPanel : ((MapApp)map.getApp()).layerManager.getLayerPanels()) {
			if (layerPanel.layer instanceof ESRIShapefile && ((ESRIShapefile)layerPanel.layer).equals(this)) {
				ESRIShapefile esf = (ESRIShapefile)(layerPanel.layer);
				return esf.getGridDataType();
			}
		}
		
		String units = getUnits();
		if ( name.equals(GridDialog.GEOID) ) {
			return "Geoid Height";
		}
		else if ( units.equals("m") ){
			return "Elevation";
		}else if ( units.equals("mgal") ) {
			return "Gravity Anomaly";
		}else if ( units.equals("percent") ) {
			return "Percent";
		}else if ( units.equals("mY") ) {
			return "Age";
		}else if ( units.equals("%") ) {
			return "Percentage";
		}else if ( units.equals("mm/a") ) {
			return "Rate";
		}

		return "";
	}
}