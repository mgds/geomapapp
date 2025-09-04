package haxby.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.geomapapp.geom.XYZ;

import haxby.map.XMap;
import haxby.proj.Projection;

/**
 * A collection of generally useful utilities
 * 
 * @author Neville Shane
 *
 */
public class GeneralUtils {
	
	public static final double KNOTS_2_KPH = 1.852;
	
	/**
	 * convert and ArrayList<Double> to an array of doubles
	 * @param doubles
	 * @return
	 */
	public static double[] arrayList2doubles(ArrayList<Double> doubles) {
		if (doubles == null) return null;
	    double[] ret = new double[doubles.size()];
	    for(int i = 0; i < ret.length; i++) ret[i] = doubles.get(i).doubleValue();
	    return ret;
	}
	
	/**
	 * convert an ArrayList<Float> to an array of floats
	 * @param floats
	 * @return
	 */
	public static float[] arrayList2floats(ArrayList<Float> floats) {
		if (floats == null) return null;
	    float[] ret = new float[floats.size()];
	    for(int i = 0; i < ret.length; i++) ret[i] = floats.get(i).floatValue();
	    return ret;
	}
	
	/**
	 * convert an ArrayList<Integer> to an array of ints
	 * @param floats
	 * @return
	 */
	public static int[] arrayList2ints(ArrayList<Integer> ints) {
		if (ints == null) return null;
	    int[] ret = new int[ints.size()];
	    for(int i = 0; i < ret.length; i++) ret[i] = ints.get(i).intValue();
	    return ret;
	}

	/**
	 * convert an array of floats to a CSV string
	 * @param array
	 * @return
	 */
	public static String array2String(float[] array) {
		String str = "";
		if (array == null) return str;
		for (int i=0; i<array.length; i++) {
			if (i==0) str += Float.toString(array[i]);
			else str += "," + Float.toString(array[i]);
		}
		return str;
	}
	
	/**
	 * convert an array on integers to a CSV string
	 * @param array
	 * @return
	 */
	public static String array2String(int[] array) {
		String str = "";
		if (array == null) return str;
		for (int i=0; i<array.length; i++) {
			if (i==0) str += Integer.toString(array[i]);
			else str += "," + Integer.toString(array[i]);
		}
		return str;
	}
	
	/**
	 * convert a CSV string to an array of floats
	 * @param str
	 * @return
	 */
	public static float[] string2FloatArray(String str) {
		if (str == null) return null;
		String[] strArray = str.split(",");
		float[] array = new float[strArray.length];
		for (int i=0; i<strArray.length; i++) {
			array[i] = Float.parseFloat(strArray[i]);
		}
		return array;
	}
	
	/**
	 * convert a CSV string to an array of integers
	 * @param str
	 * @return
	 */
	public static int[] string2IntArray(String str) {
		if (str == null) return null;
		String[] strArray = str.split(",");
		int[] array = new int[strArray.length];
		for (int i=0; i<strArray.length; i++) {
			array[i] = Integer.parseInt(strArray[i]);
		}
		return array;
	}
	
	/**
	 * removes basic html tags from a string
	 * @param html
	 * @return
	 */
	public static String html2text(String html) {
		return html.replaceAll("\\<[^>]*>","");
	}
	
	/**
	 * tests whether a string can be parsed as a double
	 * @param str
	 * @return
	 */
    public static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

	/**
	 * tests whether a string can be parsed as an integer
	 * @param str
	 * @return
	 */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Find the distance between the first and last in an array of points
     * @param pts
     * @return
     */
    public static double distance(Point2D[] pts) {
    	return distance(pts, false);
    }
    
	public static double distance(Point2D[] pts, boolean longWay) {
		if( pts==null ) return 0.;
	
		try {
			if (Double.isNaN(pts[0].getX()) || Double.isNaN(pts[0].getY()) 
				|| Double.isNaN(pts[pts.length-1].getY()) ||Double.isNaN(pts[pts.length-1].getY())) return 0;
			XYZ r1 = XYZ.LonLat_to_XYZ(pts[0]);
			XYZ r2 = XYZ.LonLat_to_XYZ(pts[pts.length-1]);
			double angle = Math.acos( r1.dot(r2) );
			if (Double.isNaN(angle)) return 0;
			if (longWay) angle = 2*Math.PI - angle; // going the long way round
			return Projection.major[0]*angle/1000.;
		} catch (Exception e) {
			return 0;
		}
	}
	
    /**
     * Find the cumulative distance between an array of points
     * @param pts
     * @return
     */
	public static double cumulativeDistance(Point2D[] pts) {
		if( pts==null ) return 0.;
		try{
			double d = 0.;
			for (int i=1; i<pts.length; i++) {
				Point2D[] points = {pts[i-1], pts[i]};
				d += distance(points);
			}
			return d;
		} catch (Exception e) {
			return 0;
		}
	}
	
	
	/**
	 * Find the initial bearing of a line defined by an array of points
	 * (see http://www.movable-type.co.uk/scripts/latlong.html)
	 * @param pts
	 * @return
	 */
	public static double bearing(Point2D[] pts) {
		double f1 = Math.toRadians(pts[0].getY());
		double f2 = Math.toRadians(pts[pts.length-1].getY());
		double l1 = Math.toRadians(pts[0].getX());
		double l2 = Math.toRadians(pts[pts.length-1].getX());
		
		double y = Math.sin(l2-l1) * Math.cos(f2);
		double x = Math.cos(f1)*Math.sin(f2) -
		        Math.sin(f1)*Math.cos(f2)*Math.cos(l2-l1);
		double brng = Math.atan2(y, x);
		return Math.toDegrees(brng);
	}
	
	/**
	 * Find the final bearing of a line defined by an array of points
	 * (see http://www.movable-type.co.uk/scripts/latlong.html)
	 * @param pts
	 * @return
	 */
	public static double finalBearing(Point2D[] pts) {
		double f2 = Math.toRadians(pts[0].getY());
		double f1 = Math.toRadians(pts[pts.length-1].getY());
		double l2 = Math.toRadians(pts[0].getX());
		double l1 = Math.toRadians(pts[pts.length-1].getX());
		
		double y = Math.sin(l2-l1) * Math.cos(f2);
		double x = Math.cos(f1)*Math.sin(f2) -
		        Math.sin(f1)*Math.cos(f2)*Math.cos(l2-l1);
		double brng = (Math.atan2(y, x) + Math.PI) % (Math.PI * 2.);
		return Math.toDegrees(brng);
	}
	
	/**
	 * Find the bearing of a line defined by two points
	 * on a flat Earth (ie not using great circles)
	 * @param pts
	 * @return
	 */
	public static double flatEarthBearing(Point2D[] pts) {
		double y = pts[1].getX() - pts[0].getX();
		double x = pts[0].getY() - pts[1].getY();
		double brng = Math.atan2(y, x);	
		return Math.toDegrees(brng);
	}
	
	/**
	 * Calculate a new point that is a distance d(km) and a bearing brng (rad) from point pt1
	 * (see http://www.movable-type.co.uk/scripts/latlong.html)
	 * @param pt1
	 * @param d
	 * @param brng
	 * @return
	 */
	public static Point2D pointFromDistAndBearing(Point2D pt1, double d, double brng) {
		double f1 = Math.toRadians(pt1.getY());
		double l1 = Math.toRadians(pt1.getX());
		double R = Projection.major[0]/1000.;

		double f2 = Math.asin( Math.sin(f1)*Math.cos(d/R) +
                Math.cos(f1)*Math.sin(d/R)*Math.cos(brng) );
		double l2 = l1 + Math.atan2(Math.sin(brng)*Math.sin(d/R)*Math.cos(f1),
                     Math.cos(d/R)-Math.sin(f1)*Math.sin(f2));
		
		Point2D pt2 = new Point2D.Double(Math.toDegrees(l2), Math.toDegrees(f2));
		return pt2;
	}
	
	/**
	 * Calculate the start and end points for a line parallel to that defined by the 
	 * input array of points pts.  The distance input (in km) gives the perpendicular distance
	 * of the new line from the original.  dir can be +1 or -1 and determines in which
	 * direction the new line is from the original.
	 * @param pts
	 * @param distance
	 * @param dir
	 * @return
	 */
	public static Point2D[] parallelLine(Point2D[] pts, double distance, byte dir) {
	
		if (dir != 1 && dir != -1) return null;
		double brng = Math.toRadians(finalBearing(pts) + 90 * dir);
		Point2D start = pointFromDistAndBearing(pts[0], distance, brng);
		Point2D end = pointFromDistAndBearing(pts[pts.length-1], distance, brng);
		Point2D[] newPts = {start, end};
		return newPts;
	}
	
	/**
	 * When calculating the distance between two points in map coords, need to 
	 * take into account the displayed map size, and whether one of the points crosses 
	 * the wrap boundary.
	 * @param map
	 * @param p0
	 * @param p1
	 * @return
	 */
	public static void wrapPoints(XMap map, Point2D.Double p0, Point2D.Double p1) {
		double wrap = map.getWrap();
		
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		double xmax = rect.getMaxX();
		
		//make sure points are within the displayed map by adding or subtracting the wrap value
		if( wrap>0f ) {
			while( p0.x <= xmin ){p0.x+=wrap;}
			while( p0.x >= xmax ){p0.x-=wrap;}
			while( p1.x <= xmin ){p1.x+=wrap;}
			//while( p1.x > wrap + xmin ){p1.x-=wrap;}
			while( p1.x >= xmax ){p1.x-=wrap;}
		}
	}
	
	/**
	 * Make sure point is within the displayed map by adding 
	 * or subtracting the wrap value
	 * @param map
	 * @param p
	 */
	public static void wrapPoint(XMap map, Point2D.Double p) {
		double wrap = map.getWrap();
		
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		double xmax = rect.getMaxX();
		
		//make sure point is within the displayed map by adding or subtracting the wrap value
		if( wrap>0f ) {
			while( p.x <= xmin ){p.x+=wrap;}
			while( p.x >= xmax ){p.x-=wrap;}
			
			//if the point isn't in range, work out which boundary it is closest to.
			if (p.x < xmin) {
				if (p.x + wrap - xmax < (xmin - p.x) ) p.x+=wrap;
			}
		}
	}
	
	/**
	 * If a point occurs multiple times on the map, subtract the wrap 
	 * value until we have the first occurrence
	 * @param map
	 * @param p
	 */
	public static void unwrapPoint(XMap map, Point2D.Double p) {
		double wrap = map.getWrap();
		
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		//get the first occurrence
		if (wrap > 0f) {
			while (p.x - wrap >= xmin) {p.x -= wrap;}
		}
	}
		
	/**
	 * Create a JEditorPane that will convert the text into html with working hyperlinks.
	 * This can be included in a messageDialog with, e.g.
	 * JOptionPane.showMessageDialog(MapApp.anchor, ep)
	 * @param text
	 * @return
	 */
	public static JEditorPane makeEditorPane(String text) {
	    // for copying style
	    JLabel label = new JLabel();
	    Font font = label.getFont();

	    // create some css from the label's font
	    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
	    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
	    style.append("font-size:" + font.getSize() + "pt;");
		
	    //need to use editor pane and hyperlink listener so that we can include hyperlinks in help text
	    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
	            + text //
	            + "</body></html>");

	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e)
	        {
	            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
	            	BrowseURL.browseURL(e.getURL().toString());
	        }
	    });
		ep.setEditable(false);
		ep.setBackground(label.getBackground());
		
		return ep;
	}
	
	/**
	 * Delete a folder, including all it's contents
	 * @param folder
	 */
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}
	
	/**
	 * Place a string in the center of a bounding rectangle (eg a plot)
	 * @param g
	 * @param text
	 * @param rect
	 * @param font
	 * @param xScale
	 * @param yScale
	 */
	public static void drawCenteredString(Graphics g, String text, Rectangle2D rect, Font font, double xScale, double yScale) {
		drawCenteredString (g, text, rect, font, xScale, yScale, false);
	}
	
	/**
	 * Place a string in the center of a bounding rectangle (eg a plot), with an optional white box 
	 * behind the text
	 * @param g
	 * @param text
	 * @param rect
	 * @param font
	 * @param xScale
	 * @param yScale
	 * @param whiteBox
	 */
	public static void drawCenteredString(Graphics g, String text, Rectangle2D rect, Font font, double xScale, 
			double yScale, boolean whiteBox) {
		// Get the original font
		Font oldFont = g.getFont();
	    // Get the FontMetrics
	    FontMetrics metrics = g.getFontMetrics(font);
	    // Determine the X coordinate for the text
	    int x = (int) ((rect.getWidth()* xScale  - metrics.stringWidth(text))  / 2);
	    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
	    int y = (int) ((rect.getHeight() * yScale - metrics.getHeight()) / 2) + metrics.getAscent();
	    // Set the font
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setFont(font);
	    
	    if (whiteBox) {
	    	//draw a white box behind the text
		    int textWidth = (int) font.getStringBounds(text, g2.getFontRenderContext()).getWidth() + 2;
		    int textHeight = metrics.getHeight() + 2;
		    Color oldColor = g2.getColor();
		    g2.setColor(Color.white);
		    g2.fillRect(x-1, y-textHeight+4, textWidth, textHeight);
		    g2.setColor(oldColor);
	    }
	    
	    // Draw the String
	    g.drawString(text, x , y);
	    // Reset font
	    g.setFont(oldFont);
	}
	
	/**
	 * Place a string in the lower left of a bounding rectangle (eg a plot)
	 * @param g
	 * @param text
	 * @param rect
	 * @param font
	 * @param xScale
	 * @param yScale
	 */
	public static void drawLowerLeftString(Graphics g, String text, Rectangle2D rect, Font font, double xScale, double yScale, boolean whiteBox) {
		drawLowerLeftString(g, text, rect, font, xScale, yScale, whiteBox, 0);
	}
	
	public static void drawLowerLeftString(Graphics g, String text, Rectangle2D rect, Font font, double xScale, double yScale, boolean whiteBox, int lineNum) {
		// Get the original font
		Font oldFont = g.getFont();
	    // Get the FontMetrics
	    FontMetrics metrics = g.getFontMetrics(font);
	    // Determine the X coordinate for the text
	    int x = 10;
	    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
	    int y = (int) ((rect.getHeight() * yScale - metrics.getHeight()) ) + metrics.getAscent() - 5;
	    // If this is not the first line, draw the string above the existing string
	    y -= (metrics.getHeight() + 5) * lineNum;
	    // Set the font
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setFont(font);
	    
	    if (whiteBox) {
	    	//draw a white box behind the text
		    int textWidth = (int) font.getStringBounds(text, g2.getFontRenderContext()).getWidth() + 2;
		    int textHeight = metrics.getHeight() + 2;
		    Color oldColor = g2.getColor();
		    g2.setColor(Color.white);
		    g2.fillRect(x-1, y-textHeight+4, textWidth, textHeight);
		    g2.setColor(oldColor);
	    }
	    // Draw the String
	    g2.drawString(text, x , y);
	    // Reset font
	    g2.setFont(oldFont);
	}
	
	/**
	 * Get the key for a given value from a hashmap with a one-to-one relationship
	 * @param map
	 * @param value
	 * @return
	 */
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	/**
	 * Get the keys for a given value from a hashmap with a many-to-one relationship
	 * @param map
	 * @param value
	 * @return
	 */
	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
	    Set<T> keys = new HashSet<T>();
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            keys.add(entry.getKey());
	        }
	    }
	    return keys;
	}
	
	/**
	 * determine number of decimal places to display latitude and longitude, 
	 * based on the zoom level.
	 * @param zoom
	 * @return
	 */
	public static NumberFormat getZoomNumberFormat(double zoom) {
		NumberFormat fmt = NumberFormat.getInstance();
		if ( zoom < 16 ) {
			fmt.setMaximumFractionDigits(2);
			fmt.setMinimumFractionDigits(2);
		}
		else if ( zoom >= 16 && zoom < 256 ) {
			fmt.setMaximumFractionDigits(3);
			fmt.setMinimumFractionDigits(3);
		}
		else if ( zoom >= 256 && zoom < 4096 ) {
			fmt.setMaximumFractionDigits(4);
			fmt.setMinimumFractionDigits(4);
		}
		else if ( zoom >= 4096 && zoom < 32768 ) {
			fmt.setMaximumFractionDigits(5);
			fmt.setMinimumFractionDigits(5);
		}
		else if ( zoom >= 32768 ) {
			fmt.setMaximumFractionDigits(6);
			fmt.setMinimumFractionDigits(6);
		}
		return fmt;
	}
	
	/**
	 * determine number of decimal places to display latitude and longitude minutes, 
	 * based on the zoom level.
	 * @param zoom
	 * @return
	 */
	public static NumberFormat getZoomNumberFormatMins(double zoom) {
		NumberFormat fmt = NumberFormat.getInstance();
		if ( zoom < 256 ) {
			fmt.setMaximumFractionDigits(1);
			fmt.setMinimumFractionDigits(1);
		}
		else if ( zoom >= 256 && zoom < 4096 ) {
			fmt.setMaximumFractionDigits(2);
			fmt.setMinimumFractionDigits(2);
		}
		else if ( zoom >= 4096 && zoom < 32768 ) {
			fmt.setMaximumFractionDigits(3);
			fmt.setMinimumFractionDigits(3);
		}
		else if ( zoom >= 32768 ) {
			fmt.setMaximumFractionDigits(4);
			fmt.setMinimumFractionDigits(4);
		}
		return fmt;
	}
	
	/**
	 * Format a double to a given number of significant figures.
	 * From: http://helpdesk.objects.com.au/java/how-to-format-a-number-to-a-certain-number-of-significant-figures-as-opposed-to-decimal-places
	 * @param value
	 * @param significant
	 * @return
	 */
	public static String formatToSignificant(double value,int significant) {
		MathContext mathContext = new MathContext(significant,RoundingMode.DOWN);
		BigDecimal bigDecimal = new BigDecimal(value,mathContext);
		return bigDecimal.toPlainString();
	}
	
	/**
	 * Generate a SHA256 hash code for an input string 
	 * @param stringIn
	 * @return
	 */
	public static String stringToSHA256(String stringIn) {
		if (stringIn != null) {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] hash = digest.digest(stringIn.getBytes(StandardCharsets.UTF_8));

			    StringBuilder sb = new StringBuilder();
			    for (int i = 0; i < hash.length; i++) {
			        sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
			    }
				
			    return sb.toString();

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Select a button from a button group using the buttons actionCommand. Usage:
	 * setButtonGroup(yourValue, yourButtonGroup.getElements());
	 * @param rdValue
	 * @param elements
	 */
	public static void setButtonGroup(String rdValue, Enumeration<AbstractButton> elements ){
	    while (elements.hasMoreElements()){
	        AbstractButton button = (AbstractButton)elements.nextElement();
	        if(button.getActionCommand().equals(rdValue)){
	            button.setSelected(true);
	        }
	    }
	}
	
	/**
	 * Create a JComboBox where the width of the popup list is wider than the width of the combo box
	 * see https://stackoverflow.com/questions/956003/how-can-i-change-the-width-of-a-jcombobox-dropdown-list
	 * This is needed for the Windows GUI
	 */
	public static class WideComboBox extends JComboBox{ 

		private static final long serialVersionUID = 1L;

		public WideComboBox() { 
        } 

        public WideComboBox(final Object items[]){ 
            super(items); 
        } 

        public WideComboBox(Vector items) { 
            super(items); 
        } 

            public WideComboBox(ComboBoxModel aModel) { 
            super(aModel); 
        } 

        private boolean layingOut = false; 

        public void doLayout(){ 
            try{ 
                layingOut = true; 
                    super.doLayout(); 
            }finally{ 
                layingOut = false; 
            } 
        } 

        public Dimension getSize(){ 
            Dimension dim = super.getSize(); 
            if(!layingOut) 
                dim.width = Math.max(dim.width, getPreferredSize().width)+100; 
            return dim; 
        } 
    }
}
