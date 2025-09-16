package haxby.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.geomapapp.geom.XYZ;
import org.geomapapp.grid.Grid2DOverlay;

import haxby.grid.GridImager;

public class MapColorScale implements MapInset, 
				MouseListener,
				MouseMotionListener {
	BufferedImage image;
	int xOffset, yOffset;
	int width, height;
	boolean vertical;
	XMap map;

//	***** GMA 1.6.6: Add left and right bar values
	float leftBar = -7000;
	float rightBar = 3000;
	Grid2DOverlay colorScaleGrid = null;
	String units = "m";
	String dataType = "";
//	***** GMA 1.6.6	

	public MapColorScale( XMap map ) {
		this.map = map;
		image = new BufferedImage( 20, 100, BufferedImage.TYPE_INT_RGB );
		GridImager imager = new GridImager();
		imager.setGamma(1.5);

//		Then try to get a background grid
		if ( ((MapApp)(map.getApp())).tools.gridDialog != null && ((MapApp)(map.getApp())).tools.gridDialog.isDialogVisible() ) {
			colorScaleGrid = ((Grid2DOverlay)(((MapApp)(map.getApp())).tools.gridDialog.getGrid()));
			units = map.getUnits();
			if (units == "")
				units = "m";
			dataType = map.getDataType();
		}
//		Then whatever grid might be on the map
		else if ( map.getFocus().getGrid() != null ) {
			colorScaleGrid = map.getFocus();
			units = map.getUnits();
			dataType = map.getDataType();
		}
		if ( colorScaleGrid != null && colorScaleGrid.lut != null ) {
			leftBar = colorScaleGrid.lut.getPalette().getRange()[0];
			rightBar = colorScaleGrid.lut.getPalette().getRange()[1];
		}

		if (map.getFocus().getGrid() == null) {
			units = "m";
			dataType = "GMRT Base Map Elevation";		
		}
		for( int y = 0; y < 100; y++ ) {

//			***** GMA 1.6.6: 
			float z = 2950 - 100f*(float)y;
//			***** GMA 1.6.6

			for( int x=0 ; x<20 ; x++ ) {
				float shade = .215f + .03f*(float)x;
//				***** GMA 1.6.6: Set new RGB value from the palette of the currently loaded grid
//								 Set the image color at x,y to the color from the loaded grid, if no loaded grid set it 
//								 to color from background imagery
//				image.setRGB( x, y, imager.getRGB( z, shade ) );

				if ( colorScaleGrid != null && colorScaleGrid.lut != null ) {
					shade = (float)colorScaleGrid.lut.getSun().dot( new XYZ(0.,0.,1.) );
					int rgb = colorScaleGrid.lut.getPalette().getRGB( (float)( (colorScaleGrid.lut.getPalette().getRange()[0]) + ( y * ( ( (colorScaleGrid.lut.getPalette().getRange()[1]) - (colorScaleGrid.lut.getPalette().getRange()[0]) ) / 100 ) ) ), shade );
					Color gridColor = new Color(rgb);
//					y is inverted to correctly vertically orient the color scale
					image.setRGB( x, 99 - y, gridColor.getRGB() );
				}
				else {
					image.setRGB( x, y, imager.getRGB( z, shade ) );
				}
//				***** GMA 1.6.6
			}
		}
		width = 25;
		height = 150;
		vertical = true;
		xOffset = yOffset = 50;
	}

	public void reset() {
		width = 25;
		height = 150;
		vertical = true;
		xOffset = yOffset = 50;
	}
	public void draw( Graphics2D g, int w, int h ) {
		int x = xOffset;
		int y = yOffset;
		if( x+width > w ) x = w - width;
		if( y+height > h ) y = h - height;
		xOffset = x;
		yOffset = y;

		if ( colorScaleGrid != null && colorScaleGrid.toString().toLowerCase().indexOf("gravity") != -1 ) {
			units = "mgals";
		}
		
		//set up label formatting
		float range = Math.abs(leftBar - rightBar);
		DecimalFormat df = new DecimalFormat("0.000E0");
		String sciNot = df.format(range);
		int exponent=Integer.parseInt(sciNot.split("E")[1]);
		
		//determine where to drawer markers on the color bar
		double marker = Math.pow(10, exponent);
		while (range / marker < 5)
			marker /= 2;
		boolean markerHasDecimals = false;
		if (marker != (int) marker) {
			markerHasDecimals = true;
		}
		double firstMarker = Math.ceil(leftBar / marker) * marker;

		ArrayList<Double> markers = new ArrayList<Double>();
		double nextMarker = firstMarker;
		while (nextMarker <= rightBar) {
			markers.add(nextMarker);
			nextMarker += marker;
		}

		DecimalFormat fmt;
		if(range<10 || markerHasDecimals) {
			int numberOfAmpersands = (-1)*(exponent);
			if (markerHasDecimals && range > 10)
				numberOfAmpersands++;
			String ampersandStr = "#.";
			for(int i=0;i<numberOfAmpersands+1;i++) {
				ampersandStr = ampersandStr.concat("#");
			}
			fmt = new DecimalFormat(ampersandStr);
			fmt.setMinimumFractionDigits(numberOfAmpersands+1);

		}else {
			fmt = new DecimalFormat("#");
		}		
		
		// create color bar
		AffineTransform at0 = g.getTransform();
		g.translate( x, y);
		g.setFont( new Font( "Helvetica", Font.PLAIN, 12 ) );
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D markerRect = fm.getStringBounds(Double.toString(firstMarker), g);
		int markerWidth = Math.max(42, (int)markerRect.getWidth() + 18);
		
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g.setStroke( new BasicStroke(1f) );
		g.setColor( Color.black );
		g.setBackground(Color.WHITE);

		Color textC = Color.BLACK;
		Color bgColor = new Color(255, 255, 255, 100);
		
		Rectangle2D rect = fm.getStringBounds(units, g);
		g.setColor(bgColor);
		g.fillRect((int)rect.getMinX() -4, (int) rect.getY() -4, width + markerWidth, height + 18);
		g.setColor(textC);
		g.drawString( units, 0, -2 );

		AffineTransform at = g.getTransform();
		if( width!=20 || height !=100 ) {
			g.scale( (double)width/20., (double)height/100. );
			g.setStroke( new BasicStroke( 100f/(float)height ) );
		}

		g.drawRenderedImage( image, new AffineTransform() );
		g.setTransform( at );
		g.setStroke( new BasicStroke(1f) );
		g.drawRect( 0, 0, width, height );
		x = (int)(24.*(double)width/20.);

		//draw markers
		for (double m : markers) {
			int yy = (int) ((rightBar - m) * height/range);
			g.drawString(fmt.format(m),  x, yy+5);
			g.drawLine(0, yy, width, yy);
		}
		
		g.setTransform( at0 );
	}

	boolean selected = false;
	Point lastPoint = null;
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
		selected = false;
		if( evt.isControlDown() ) {
			return;
		}
		selected = false;
		Rectangle r = map.getVisibleRect();
		MapBorder border = map.getMapBorder();
		if( border!=null ) {
			Insets ins = border.getBorderInsets(map);
			r.x += ins.left;
			r.y += ins.top;
		}
		r.x += xOffset;
		r.y += yOffset;
		r.width = width;
		r.height = height;
		if( r.contains( evt.getPoint() ) ) {
			selected = true;
			lastPoint = evt.getPoint();
		}
	}
	public void mouseReleased( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			selected = false;
			map.repaint();
		}
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			Point p = evt.getPoint();
			xOffset += p.x - lastPoint.x;
			yOffset += p.y - lastPoint.y;
			if(xOffset<0) xOffset=0;
			if(yOffset<0) yOffset=0;
			lastPoint = p;
		}
	}
	public void saveColorScale() throws IOException {
				
		File file = new File("colorbar.png");

		boolean includeDataType = false;
		if (dataType != "") {
			int mapInsetConfirm = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), "Include data type title in color bar image?");
			if ( mapInsetConfirm == JOptionPane.CANCEL_OPTION)
				return;
			if ( mapInsetConfirm == JOptionPane.YES_OPTION ) 
				includeDataType = true;
		}
		
		JFileChooser chooser = MapApp.getFileChooser();
		chooser.setSelectedFile(file);
		chooser.setFileFilter( new FileFilter() {
			public String getDescription() {
				return "PNG Image (.png)";
			}
			public boolean accept(File f) {
				return  f.isDirectory() ||
					f.getName().toLowerCase().endsWith(".png");
			}
		});

		int confirm = JOptionPane.NO_OPTION;

		while (confirm == JOptionPane.NO_OPTION) {
			int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

			if (ok == chooser.CANCEL_OPTION)
				return;

			file = chooser.getSelectedFile();
			if ( !file.getName().toLowerCase().endsWith(".png")) {
				file = new File(file.getPath() + ".png");
			}

			if (file.exists()) {
				confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
				if (confirm == JOptionPane.CANCEL_OPTION) return;
			}
			else
				break;
		}


		//set up label formatting
		float range = Math.abs(leftBar - rightBar);
		DecimalFormat df = new DecimalFormat("0.000E0");
		String sciNot = df.format(range);
		int exponent=Integer.parseInt(sciNot.split("E")[1]);
		
		//determine where to drawer markers on the color bar
		double marker = Math.pow(10, exponent);
		while (range / marker < 5)
			marker /= 2;
		boolean markerHasDecimals = false;
		if (marker != (int) marker) {
			markerHasDecimals = true;
		}
		double firstMarker = Math.ceil(leftBar / marker) * marker;

		ArrayList<Double> markers = new ArrayList<Double>();
		double nextMarker = firstMarker;
		while (nextMarker <= rightBar) {
			markers.add(nextMarker);
			nextMarker += marker;
		}

		DecimalFormat fmt;
		if(range<10 || markerHasDecimals) {
			int numberOfAmpersands = (-1)*(exponent);
			if (markerHasDecimals && range > 10)
				numberOfAmpersands++;
			String ampersandStr = "#.";
			for(int i=0;i<numberOfAmpersands+1;i++) {
				ampersandStr = ampersandStr.concat("#");
			}
			fmt = new DecimalFormat(ampersandStr);
			fmt.setMinimumFractionDigits(numberOfAmpersands+1);

		}else {
			fmt = new DecimalFormat("#");
		}
		
		//create color bar
		int saveWidth = width * 4;
		int saveHeight = height * 4;
		BufferedImage tmpImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = tmpImg.createGraphics();
		g.setFont( new Font( "Helvetica", Font.PLAIN, 24 ) );
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D txtRect = fm.getStringBounds(dataType, g);
		Rectangle2D markerRect = fm.getStringBounds(Double.toString(firstMarker), g);
		int markerWidth = Math.max(42, (int)markerRect.getWidth() + 36);
		int imgWidth = saveWidth + markerWidth;
		int imgHeight = saveHeight + 64;
		int imgX = -16;
		int imgY = -52;
		int imgYOffset = -52;
		if (dataType != "" && includeDataType) {
			imgWidth = Math.max(imgWidth, (int)txtRect.getWidth()+20) ;
			imgHeight += 16;
			imgY = -60;
			imgYOffset = -60;
		}
		
		Rectangle2D imgRect = new Rectangle(imgX, imgY, imgWidth + 6, imgHeight);
		
		g.dispose();
		
		
	    BufferedImage bImg = new BufferedImage((int) imgRect.getWidth(), (int) imgRect.getHeight(), BufferedImage.TYPE_INT_RGB);
	    g = bImg.createGraphics();
	    
	 
	    
		int x = (int) imgRect.getX() * -1;
		int y = (int) imgRect.getY() * -1;;


		if ( colorScaleGrid != null && colorScaleGrid.toString().toLowerCase().indexOf("gravity") != -1 ) {
			units = "mgals";
		}

		AffineTransform at0 = g.getTransform();
		g.translate( x, y);
		g.setFont( new Font( "Helvetica", Font.PLAIN, 24 ) );
		fm = g.getFontMetrics();

		g.setStroke( new BasicStroke(1f) );
		g.setColor( Color.black );
		g.setBackground(Color.WHITE);

		Color textC = Color.BLACK;
		Color bgColor = new Color(255, 255, 255, 255);
		
		g.setColor(bgColor);
		g.fillRect(-16, imgYOffset, imgWidth + 6, imgHeight);
		g.setColor(textC);
		if (dataType != "" && includeDataType) g.drawString( dataType, 0, -8 - g.getFontMetrics().getHeight());
		g.drawString( units, 0, -8 );

		AffineTransform at = g.getTransform();
		if( saveWidth!=20 || saveWidth !=100 ) {
			g.scale( (double)saveWidth/20., (double)saveHeight/100. );
			g.setStroke( new BasicStroke( 100f/(float)saveHeight ) );
		}

		g.drawRenderedImage( image, new AffineTransform() );
		g.setTransform( at );
		g.setStroke( new BasicStroke(2f) );
		g.drawRect( 0, 0, saveWidth, saveHeight );
		x = (int)(24.*(double)saveWidth/20.);
		
		//draw markers
		for (double m : markers) {
			int yy = (int) ((rightBar - m) * saveHeight/range);
			g.drawString(fmt.format(m),  x, yy+10);
			g.drawLine(0, yy, saveWidth, yy);
		}
		g.setTransform( at0 );
		
		
		ImageIO.write(bImg, "png", file);
	}
}
