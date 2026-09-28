package org.geomapapp.image;

import haxby.map.MapApp;
import haxby.proj.IdentityProjection;
import haxby.proj.PolarStereo;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.ProjectionDialog;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.gis.shape.ShapeSuite;
import org.geomapapp.grid.TileIO;

import ucar.nc2.dt.point.decode.MP;

public class ImportImage implements Runnable {

	public static FileFilter imageFileFilter = 
		new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) return true;
				for (String imageType : ImageIO.getReaderFormatNames() ) {
					if (pathname.getName().endsWith(imageType))
						return true;
				}
				return false;
			}
			
			public String getDescription() {
				return "Image Files";
			}
		};
	
	JFrame frame;
	JTextArea area;
	JButton imageB;
	double[] wesn;
	double dxMin, dyMin;

	ShapeSuite suite;
	
	int gridType;
	
	ProjectionDialog pd = new ProjectionDialog();
	
	String imageType = "png";
	
	protected int mapType;
	
	public ImportImage(JFrame frame, ShapeSuite suite) {
		this(frame, suite, MapApp.MERCATOR_MAP);
	}
	public ImportImage(JFrame frame, ShapeSuite suite, int mapType) {
		this.frame = frame;
		this.suite = suite;
		this.mapType = mapType;
		area = new JTextArea(6,60);
		init();
	}
	
	void init() {
		JPanel panel = new JPanel();
		imageB = new JButton("Import Image");
		panel.add(imageB);
		imageB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				begin();
			}
		});
		
		frame.getContentPane().add( panel, "North");
		frame.getContentPane().add( new JScrollPane(area) );
		frame.pack();
		frame.show();
	
//		GMA 1.4.8: Automatically bring up file chooser to select grid	
		imageB.doClick();
	
	}
	void begin() {
		(new Thread(this)).start();
	}
	public void run() {
		imageB.setEnabled(false);
		try {
			open();
		}catch(IOException e) {
			org.geomapapp.io.ShowStackTrace.showTrace(e, frame);
		}
		imageB.setEnabled(true);
	}
	void open() throws IOException {
		JFileChooser chooser = haxby.map.MapApp.getFileChooser();
		int mode = chooser.getFileSelectionMode();
		boolean multi = chooser.isMultiSelectionEnabled();
		chooser.setMultiSelectionEnabled( true );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.addChoosableFileFilter(imageFileFilter);
		
		int ok = chooser.showOpenDialog(frame);
		File[] choice = null;
		if( ok!=chooser.CANCEL_OPTION ) choice = chooser.getSelectedFiles();
		chooser.setMultiSelectionEnabled(multi);
		chooser.setFileSelectionMode( mode );
		chooser.removeChoosableFileFilter(imageFileFilter);

		if( ok==chooser.CANCEL_OPTION ) {
			return;
		}
		
		if (mapType == MapApp.MERCATOR_MAP)
			openImagesMercator(choice);
		else
			openImagesPolar(choice);
	}
	
	MapProjection getProjection(String name, MapProjection inputPrj, double[] wesn, int width, int height) {
		MapProjection prj = pd.getProjection( frame, wesn, 1, width, height, inputPrj, name);
		return prj;
	}
	
	MapProjection getPolarProjection(String name, double cell_size)
	{
		MapProjection prj = pd.getPolarProjection( frame, cell_size, 1, name, mapType == MapApp.SOUTH_POLAR_MAP);
		return prj;
	}
	
	void tileImages(String name, File[] files, ImageProjection[] projections, double dx0) throws IOException
	{
		int res = 2;
		while (dx0/res > 1.4 * dxMin)
			res *= 2;
		
		tileImages(name, files, projections, res);
	}
	
	void tileImages(String name, File[] files, ImageProjection[] projections, int res) throws IOException
	{
		double dx0;
		if (mapType == MapApp.MERCATOR_MAP)
			dx0 = 360./640.;
		else 
			dx0 = 25600;
		double mPerPixel = dx0 / res;
		
		MapProjection proj;
		switch (mapType)
		{
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
		
		tileImages(name, files, projections, proj, res);
	}
	
	void tileImages(String name, File[] files, ImageProjection[] images, MapProjection proj, int res) throws IOException
	{
		// Clamp Latitudes in the Mercator
//		if (mapType == MapApp.MERCATOR_MAP) {
//			if( wesn[2]<-79. )wesn[2]=-79.;
//			if( wesn[3]>81. )wesn[3]=81.;
//		}

		File dir = files[0].getParentFile();
		File top = new File( dir +"/i_"+res );
		if( !top.exists() ) top.mkdirs();
		
		for( int k=0 ; k<files.length ; k++) {
			area.setText("Processing "+files[k].getName()+", "+ (k+1) +" of "+ files.length + " at res " + res);
			tile( files[k], images[k], proj, res);
		}
	}
	
	void openImagesMercator(File[] files)  throws IOException {
		String name = files[0].getParentFile().getName();
		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);
		
		dxMin = Double.MAX_VALUE;
		double pixelsPerMAX = -Double.MAX_VALUE;

		ImageProjection[] projections = new ImageProjection[files.length];
		
		for( int k=0 ; k<files.length ; k++) {
			BufferedImage image = ImageIO.read(files[k]);
			
			int width = image.getWidth();
			int height = image.getHeight();
			
			JPanel p = new JPanel(new BorderLayout());
			JTextField n = new JTextField(10);
			p.add(n, BorderLayout.NORTH);
			JTextField w = new JTextField(10);
			p.add(w, BorderLayout.WEST);
			JTextField e = new JTextField(10);
			p.add(e, BorderLayout.EAST);
			JTextField s = new JTextField(10);
			p.add(s, BorderLayout.SOUTH);
			
			int c = JOptionPane.showConfirmDialog(frame, p);

			double wesn[] = new double[]
			{ Double.parseDouble(w.getText()),
				Double.parseDouble(e.getText()),
				Double.parseDouble(s.getText()),
				Double.parseDouble(n.getText())
			};
			
//			double wesn[] = new double[] {-98,-58,2,28};
			
			double pixelsPer360 = width / (wesn[1] - wesn[0]) * 360 ;
			
			double dx = 1;
			
			MapProjection proj = 
					new Mercator( wesn[0], 
							wesn[3], 
							pixelsPer360, 
							Mercator.SPHERE, 
							Mercator.RANGE_0_to_360);

			
			projections[k] = new ImageProjection();
			projections[k].x0 = 0;
			projections[k].y0 = 0;
			projections[k].cell_size = dx;
			projections[k].width = width;
			projections[k].height = height;
			projections[k].proj = proj;
			
			
			pixelsPerMAX = Math.max(pixelsPerMAX, pixelsPer360);
			dxMin = Math.min(dx, dxMin);
		}
		
		dyMin = dxMin;
		
		int res = 1;
		while (res * 640 < pixelsPerMAX)
			res *= 2;
		
		for (int ires = res; ires >= 1; ires /= 2)
			tileImages(name, files, projections, ires);

		File dir = files[0].getParentFile();
		File shp = (new XBItoShape()).open(dir, name, imageType, mapType);
		
		frame.dispose();
		if( shp!=null && suite!=null )suite.addShapeFile(shp);
	}
	
	void openImagesPolar(File[] files)  throws IOException {
		String name = files[0].getParentFile().getName();
		if( files.length==1 ) {
			name = files[0].getName();
			name = name.substring( 0, name.lastIndexOf(".") );
		}
		area.setText(name);
		
		dxMin = Double.MAX_VALUE;

		ImageProjection[] projections = new ImageProjection[files.length];
		
		for( int k=0 ; k<files.length ; k++) {
			String fileName = files[k].getName();
			int fileNameLength = fileName.length();
			String prefix = fileName.substring(0, fileName.lastIndexOf(".")); 
			
			File header = new File(files[k].getParentFile(),
					fileName + "w");
			
			if (!header.exists())
				header = new File(files[k].getParentFile(),
					prefix + 
					"." + 
					fileName.charAt(fileNameLength - 3) + 
					fileName.charAt(fileNameLength - 1) +
					"w");
			
			if (!header.exists())
				header = new File(files[k].getParentFile(),
					prefix + 
					"." + 
					fileName.charAt(fileNameLength - 3) + 
					fileName.charAt(fileNameLength - 2) +
					"w");
			
			if (!header.exists()) {
				System.out.println("No header found for " + fileName);
				continue;
			}
			
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(header)));
			
			double dx = Float.parseFloat(in.readLine().trim());
			in.readLine();
			in.readLine();
			double dy = Double.parseDouble(in.readLine().trim());
			double xUL = Double.parseDouble(in.readLine().trim());
			double yUL = Double.parseDouble(in.readLine().trim());
			in.close();
			
			MapProjection proj;
			switch (mapType)
			{
			default:
			case MapApp.SOUTH_POLAR_MAP:
				proj = new PolarStereo(new Point(0, 0), 180., Math.abs(dx), -71.,
						PolarStereo.SOUTH, PolarStereo.WGS84);
				break;
			case MapApp.NORTH_POLAR_MAP:
				proj = new PolarStereo(new Point(0, 0), 0., Math.abs(dx), 71.,
						PolarStereo.NORTH, PolarStereo.WGS84);
				break;
			}
			
			projections[k] = new ImageProjection();
			projections[k].x0 = xUL;
			projections[k].y0 = dy < 0 ? -yUL : yUL;
			projections[k].cell_size = Math.abs(dx);
			projections[k].width = -1;
			projections[k].height = -1;
			projections[k].proj = proj;
			
			
			dxMin = Math.min(dx, dxMin);
		}
		
		dyMin = dxMin;
		
		int res = 2;
		while (25600 / res > dxMin) res *= 2;
		for (int ires = res; ires >= 1; ires /= 2)
			tileImages(name, files, projections, ires);

		File dir = files[0].getParentFile();
		File shp = (new XBItoShape()).open(dir, name, imageType, mapType);
		
		frame.dispose();
		if( shp!=null && suite!=null )suite.addShapeFile(shp);
	}
	
	void tile(File file, ImageProjection imageProj,
			MapProjection mapProj, int res) throws IOException {
		
		BufferedImage inputImage = ImageIO.read(file);

		imageProj.width = inputImage.getWidth();
		imageProj.height = inputImage.getHeight(); 
		
		double[] wesn;
		if (imageProj.proj.isCylindrical() && mapType == MapApp.MERCATOR_MAP)
		{
			Point2D p = imageProj.proj.getRefXY(imageProj.x0, imageProj.y0); 
			Point2D p2 = imageProj.proj.getRefXY(imageProj.x0 + imageProj.width - 1, 
					imageProj.y0 + imageProj.height - 1);
			wesn = new double[] {p.getX(), p2.getX(), p.getY(), p2.getY()};  
		}
		else
			wesn = getMapWESN(imageProj.proj, new IdentityProjection(),
				imageProj.x0 / imageProj.cell_size,
				imageProj.y0 / imageProj.cell_size,
				imageProj.width, imageProj.height);
		
		if (wesn[1] > 360 && wesn[0] < 360) { 
			MapProjection grdProj = imageProj.proj;
			
			Point2D p = grdProj.getMapXY(360, 0);
			
			Rectangle subBounds = new Rectangle(0,0, (int) p.getX() - 1, imageProj.height);
			Point2D endPoint = grdProj.getRefXY(subBounds.width - 1, 0);
			double[] subWESN = new double[] {wesn[0], endPoint.getX(), wesn[2], wesn[3]};
			MapProjection subProjection = 
				new RectangularProjection(subWESN, subBounds.width, subBounds.height);

			ImageProjection subProj = new ImageProjection();
			subProj.height = imageProj.height;
			subProj.width = (int) p.getX() - 1;
			subProj.x0 = imageProj.x0;
			subProj.y0 = imageProj.y0;
			subProj.cell_size = imageProj.cell_size;
			subProj.proj = subProjection;
			
			tile(file, subProj, mapProj, res);
			
			subBounds = new Rectangle((int) p.getX(), 0, 
								imageProj.width - (int) p.getX(), 
								imageProj.height);
			subWESN = new double[] {360.00000001, wesn[1], wesn[2], wesn[3]};
			subProjection = 
				new RectangularProjection(subWESN, subBounds.width, subBounds.height);
			
			subProj = new ImageProjection();
			subProj.height = imageProj.height;
			subProj.width = imageProj.width - (int) p.getX();
			subProj.x0 = imageProj.x0 + ((int) p.getX() - 1) * subProj.cell_size;
			subProj.y0 = imageProj.y0;
			subProj.cell_size = imageProj.cell_size;
			subProj.proj = subProjection;
			
			tile(file, subProj, mapProj, res);
			return;
		}
		
		Rectangle2D.Double bounds = 
			new Rectangle2D.Double(imageProj.x0 / imageProj.cell_size, 
					imageProj.y0 / imageProj.cell_size,
					imageProj.width, 
					imageProj.height);
		
		int x1, y1, x2, y2;
		x1 = y1 = Integer.MAX_VALUE;
		x2 = y2 = -Integer.MAX_VALUE;
		
		MapProjection gridProj = imageProj.proj;
		Point2D.Double ul = (Point2D.Double) gridProj.getRefXY( 
				bounds.x, bounds.y);
		Point2D.Double lr = (Point2D.Double) gridProj.getRefXY(
				bounds.x + bounds.width-1,
				bounds.y + bounds.height-1);
		
		double wrap;
		wrap = 360.*(bounds.width-1.)/(lr.getX()-ul.getX());
		if (!gridProj.isCylindrical())
			wrap = -1;
		
		ul = (Point2D.Double) mapProj.getMapXY(ul);
		lr = (Point2D.Double) mapProj.getMapXY(lr);
		if( gridProj.isCylindrical() && mapType == MapApp.MERCATOR_MAP) 
		{
			x1 = (int)Math.floor(ul.getX());
			y1 = (int)Math.floor(ul.getY());
			x2 = (int)Math.ceil(lr.getX());
			y2 = (int)Math.ceil(lr.getY());
			
			if (x1 > x2)
			{
				int tmp = x1;
				x1 = x2;
				x2 = tmp;
			}
			
			if (y1 > y2)
			{
				int tmp = y1;
				y1 = y2;
				y2 = tmp;
			}
		}
		else
		{
			double [] mapWESN = getMapWESN(gridProj, mapProj, bounds.x, bounds.y, bounds.width, bounds.height);

			x1 = (int) Math.floor(mapWESN[0]);
			y1 = (int) Math.floor(mapWESN[2]);
			x2 = (int) Math.ceil(mapWESN[1]);
			y2 = (int) Math.ceil(mapWESN[3]);
		}
		
		// Clamp the bounds to within the Polar Map
		if (mapType != MapApp.MERCATOR_MAP)
		{
			x1 = Math.max(x1, -320 * res);
			y1 = Math.max(y1, -320 * res);
			x2 = Math.min(x2, 320 * res);
			y2 = Math.min(y2, 320 * res);
		}
			
		int ix1 = (int)Math.floor(x1/320.);
		int iy1 = (int)Math.floor(y1/320.);
		int ix2 = (int)Math.ceil(x2/320.);
		int iy2 = (int)Math.ceil(y2/320.);

		area.append("\nTiling from X: " + ix1 + " to " + ix2 + "\n\t and Y: " + iy1 + " to " + iy2);
		for( int ix=ix1 ; ix<=ix2 ; ix++) {
			int xA = Math.max(ix*320, x1);
			int xB = Math.min((ix+1)*320, x2);
			for( int iy=iy1 ; iy<=iy2 ; iy++) {
				int yA = Math.max(iy*320, y1);
				int yB = Math.min((iy+1)*320, y2);
				
				File outputFile = new File(
						file.getParentFile(),
						"i_" + res + "/" + TileIO.getName(xA, yA, 320) + "." + imageType);

				BufferedImage outputImage;
				if (outputFile.exists())
					outputImage = ImageIO.read( outputFile );
				else 
				{
					outputImage = new BufferedImage(336,336, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = outputImage.createGraphics();
					g.setComposite(
							AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
					g.fillRect(0, 0, 336, 336);
					g.dispose();
				}
				
				ul = (Point2D.Double) mapProj.getRefXY(ix * 320 - 8,
											iy * 320 - 8);
				lr = (Point2D.Double) mapProj.getRefXY((ix + 1) * 320 + 8,
									(iy + 1) * 320 + 8);
				
				ul = (Point2D.Double) gridProj.getMapXY(ul);
				lr = (Point2D.Double) gridProj.getMapXY(lr);
				
				double xOffset0, yOffset0;
				double xOffset1, yOffset1;
				xOffset0 = 
					yOffset0 =
					xOffset1 = 
					yOffset1 = 0;
				
				if (ul.x > bounds.x + bounds.width) continue;
				if (ul.y > bounds.y + bounds.height) continue;
				if (lr.x < bounds.x) continue;
				if (lr.y < bounds.y) continue;
				
				if (ul.x < bounds.x)
				{
					ul.x = bounds.x;
					xOffset0 = mapProj.getMapXY(gridProj.getRefXY(ul)).getX()
						- (ix * 320 - 8);
					
					if (xOffset0 > 336) continue; 
				}
				
				if (ul.y < bounds.y)
				{
					ul.y = bounds.y;
					yOffset0 = mapProj.getMapXY(gridProj.getRefXY(ul)).getY()
						- (iy * 320 - 8);
					
					if (yOffset0 > 336) continue; 
				}
				
				if (lr.x > bounds.x + bounds.width)
				{
					lr.x = bounds.x + bounds.width;
					
					xOffset1 = mapProj.getMapXY(gridProj.getRefXY(lr.x, lr.y)).getX()
						- ((ix + 1) * 320 + 8);
				}
				
				if (lr.y > bounds.y + bounds.height)
				{
					lr.y = bounds.y + bounds.height;
					
					yOffset1 = mapProj.getMapXY(gridProj.getRefXY(lr.x, lr.y)).getY()
						- ((iy + 1) * 320 + 8);
				}
				
				int dx1 = (int) Math.round(xOffset0);
				int dy1 = (int) Math.round(yOffset0);
				int dx2 = (int) Math.round(336 + xOffset1);
				int dy2 = (int) Math.round(336 + yOffset1);
				
				int sx1 = (int) Math.round(ul.x - bounds.x);
				int sy1 = (int) Math.round(ul.y - bounds.y);
				int sx2 = (int) Math.round(lr.x - bounds.x);
				int sy2 = (int) Math.round(lr.y - bounds.y);
				
				int sw = sx2 - sx1;
				int sh = sy2 - sy1;
				
				BufferedImage subImage = null;
				try {
					subImage = inputImage.getSubimage(sx1, sy1, sw, sh);
				} catch (Exception ex) {
					ex.printStackTrace();
					continue;
				}
//				BufferedImage subImage =
				
				int firstColor = subImage.getRGB(0, 0);
				boolean blank = true;
				outer: for (int subIMGX = 0; subIMGX < sw; subIMGX++)
					for (int subIMGY = 0; subIMGY < sh; subIMGY++)
						if (subImage.getRGB(subIMGX, subIMGY) != firstColor)
						{
							blank = false;
							break outer;
						}
				
				if (blank) 
				{
					area.append("\nTile "+ix+", "+iy + ": empty source tile");
					continue;
				}
				
				
				Graphics2D g = outputImage.createGraphics();
				g.drawImage(inputImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
				g.dispose();
				
				ImageIO.write(outputImage, imageType, outputFile);
				
				area.append("\nTile "+ix+", "+iy + ": " + outputFile.length() + " bytes");
			}
		}
	}
	
	public static class ImageProjection 
	{
		public MapProjection proj;
		public int width, height;
		public double cell_size;
		public double x0, y0;
	}
	
	public static void main(String[] args) {
		int mapType = MapApp.MERCATOR_MAP;
		if (args.length != 0)
			mapType = Integer.parseInt(args[0]);
		
		JFrame frame = new JFrame("Import Images");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		new ImportImage(frame, null, mapType);
	}
	
	public static double[] getMapWESN(MapProjection gridProj,
			MapProjection mapProj,
			double grid_x0,
			double grid_y0,
			double width,
			double height)
	{
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
}
