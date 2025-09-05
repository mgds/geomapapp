package haxby.image;

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import java.text.DecimalFormat;

public class PS {
	double dpiX, dpiY, scaleX, scaleY;
	PrintStream psout=null;
	Dimension size=null;
	Dimension pageSize=null;
	Rectangle bounds=null;
	boolean active = false;
	float lineWidth = 1f;
	int fontNo = 0;
	int fontSize = 24;
	float gray = 0f;
	float[] rgb = {0f, 0f, 0f};
	DecimalFormat fmt=null;
	int nDec;
	public PS(double dpiX, double dpiY,  Dimension size, 
			Rectangle bounds, String file ) throws IOException {
		try {
			openPrintStream(file);
			active = true;
		} catch (IOException ex) {
			active = false;
			throw new IOException( ex.getMessage()+"\n\t"
					+notOpen.getMessage());
		}
		this.dpiX = dpiX;
		this.dpiY = dpiY;
		this.size = size;
		this.bounds = bounds;
		scaleX = 72. / dpiX;
		scaleY = 72. / dpiY;
		pageSize = new Dimension(
			(int) Math.ceil(size.getWidth()*scaleX),
			(int) Math.ceil(size.getHeight()*scaleY) );
		writeHeader();
		fmt = new DecimalFormat("#0");
		nDec=0;
	}
	public PS(double dpi, Dimension size, 
			Rectangle bounds, String file ) throws IOException {
		this( dpi, dpi, size, bounds, file );
	}
	public void close() throws IOException {
		if( !active ) throw notOpen;
		psout.println("S");
		psout.println("showpage");
		psout.println("");
		psout.println("%%Trailer");
		psout.println("");
		psout.println("end");
		psout.println("%%EOF");
		psout.close();
		active = false;
	}
	void writeHeader() throws IOException {
		if( !active ) throw notOpen;
		psout.println(PSHeader.comments[0]);
		String line = PSHeader.comments[1]
				+" 0 0 "
				+pageSize.width
				+" "+pageSize.height;
		psout.println(line);
		for( int i=2 ; i< PSHeader.comments.length ; i++) {
			psout.println(PSHeader.comments[i]);
		}
		for( int i=0 ; i<PSHeader.prolog.length ; i++) {
			psout.println(PSHeader.prolog[i]);
		}
		psout.println("%%BeginSetup");
		psout.println("");
		line = "<< /PageSize ["
			+pageSize.width
			+" "+pageSize.height
			+"  /ImagingBBox null >> setpagedevice";
	//	psout.println(line);
		psout.println("");
		psout.println("%%EndSetup");
		psout.println("");
		psout.println("%%Page: 1 1");
		psout.println("");

		psout.println( scaleX +" "+ scaleY +" scale");
		
		psout.println("");
		psout.println(bounds.x +" "+bounds.y +" T");
		psout.println("% Start of clip path");
		psout.println("S V");
		psout.println("0 0 M");
		psout.println(bounds.width +" 0 D");
		psout.println("0 "+ bounds.height +" D");
		psout.println("-"+bounds.width +" 0 D");
		psout.println("P");
		psout.println("eoclip N");
	}
	String format( double z, int nDec ) {
		if(nDec != this.nDec) {
			this.nDec = nDec;
			StringBuffer sb = new StringBuffer("#0");
			if(nDec>0) sb.append(".");
			for(int i=0 ; i<nDec ; i++) sb.append("#");
			fmt.applyPattern(sb.toString());
		}
		return fmt.format(z);
	}
	void psShape( Shape shape, double scaleX, double scaleY ) {
		if(!active) return;
		FlatteningPathIterator path=null;
		double[] seg = new double[6];
		double x, y;
		AffineTransform at = new AffineTransform();
		try {
			path = (FlatteningPathIterator)shape.getPathIterator(at);
		} catch( ClassCastException ex ) {
			path = new FlatteningPathIterator(
					shape.getPathIterator(at), .5d);
		}
		while( !path.isDone() ) {
			int type = path.currentSegment( seg );
			if( type == path.SEG_CLOSE ) {
				psout.println("P");
			} else if( type == path.SEG_MOVETO ) {
				x = seg[0]*scaleX;
				y = bounds.getHeight() - seg[1]*scaleY;
				psout.println( format(x,1)+"\t"+format(y,1)+"\tm" );
			} else if( type == path.SEG_LINETO ) {
				x = seg[0]*scaleX;
				y = bounds.getHeight() - seg[1]*scaleY;
				psout.println( format(x,1)+"\t"+format(y,1)+"\tL" );
			}
			path.next();
		}
	}
	public void setLineWidth( float w ) {
		lineWidth = w;
	}
	public void setColor( int rgb ) {
		setColor( 0xff&(rgb<<16), 0xff&(rgb<<8), 0xff&rgb );
	}
	public void setColor( int red, int green, int blue ) {
		rgb[0] = (float)red /255f;
		rgb[1] = (float)green /255f;
		rgb[2] = (float)blue /255f;
	}
	public void setColor ( float red, float green, float blue) {
		rgb[0] = red;
		rgb[1] = green;
		rgb[2] = blue;
	}
	public void setFont( int code ) {
		fontNo = code;
	}
	public void setFontSize( int size ) {
		fontSize = size;
	}
	public void drawString( String txt, double x, double y, double angle ) {
		psout.println("S V");
		psout.println(rgb[0] +" "+ rgb[1] +" "+ rgb[2] +" C");
		y = bounds.getHeight() - y;
		psout.println( format(x,0) +" "+ format(y,0) +" m");
		psout.println( format(angle,0) + " R");
		psout.println(fontSize +" F"+ fontNo +" ("+txt+") Z U");
	}
	public void drawString( String txt, double x, double y ) {
		psout.println("S");
		psout.println(rgb[0] +" "+ rgb[1] +" "+ rgb[2] +" C");
		y = bounds.getHeight() - y;
		psout.println( format(x,0) +" "+ format(y,0) +" m");
		psout.println(fontSize +" F"+ fontNo +" ("+txt+") Z U");
	}
	public void draw( Shape shape ) {
		psout.println("S V N");
		psShape( shape, 1., 1. );
		psout.println(rgb[0] +" "+ rgb[1] +" "+ rgb[2] +" C");
		psout.println(lineWidth +" W");
		psout.println("S");
	}
	public void fill( Shape shape ) {
		psout.println("S V N");
		psShape( shape, 1., 1. );
		psout.println(rgb[0] +" "+ rgb[1] +" "+ rgb[2] +" C");
		psout.println("F");
	}
	public void drawGrayImage( double scale, 
				double xOffset, double yOffset, 
				BufferedImage image) throws IOException {
		if( !active ) throw notOpen;
		yOffset = bounds.getHeight()-yOffset-(double)image.getHeight();
		psout.println(xOffset +" "+ yOffset +" T");
		psout.println(scale +" "+ scale +" scale");
		psout.println(image.getWidth() +" "+ image.getHeight() +" scale");
		int w = image.getWidth();
		psout.println("/pstr "+ (2*image.getWidth()) +" string def");
		psout.println(image.getWidth() +" "+ image.getHeight() +" 8 ["+
			image.getWidth() +" 0 0 "+ (-image.getHeight()) +" 0 "+
			image.getHeight() +"] {currentfile pstr readhexstring pop} image");
		StringBuffer sb;
		for(int y=0 ; y<image.getHeight() ; y++) {
			sb = new StringBuffer();
			for( int x=0 ; x<image.getWidth() ; x++ ) {
				sb.append( ascii[ image.getRGB(x, y)&0xff ] );
				if(sb.length()>=72) {
					psout.println( sb.toString() );
					sb = new StringBuffer();
				}
			}
			if(sb.length()>0)psout.println( sb.toString() );
		}
		psout.println("S U V");
	}
	public void drawGrayImage4( double scale, 
				double xOffset, double yOffset, 
				BufferedImage image) throws IOException {
		if( !active ) throw notOpen;
		yOffset = bounds.getHeight()-yOffset-(double)image.getHeight();
		psout.println(xOffset +" "+ yOffset +" T");
		psout.println(scale +" "+ scale +" scale");
		psout.println(image.getWidth() +" "+ image.getHeight() +" scale");
		int w = image.getWidth();
		psout.println("/pstr "+ (image.getWidth()) +" string def");
		psout.println(image.getWidth() +" "+ image.getHeight() +" 4 ["+
			image.getWidth() +" 0 0 "+ (-image.getHeight()) +" 0 "+
			image.getHeight() +"] {currentfile pstr readhexstring pop} image");
		StringBuffer sb;
		for(int y=0 ; y<image.getHeight() ; y++) {
			sb = new StringBuffer();
			for( int x=0 ; x<image.getWidth() ; x++ ) {
				sb.append( ascii[ image.getRGB(x, y)&0xff ].substring(0, 1 ));
				if(sb.length()>=72) {
					psout.println( sb.toString() );
					sb = new StringBuffer();
				}
			}
			if(sb.length()>0)psout.println( sb.toString() );
		}
		psout.println("S U V");
	}
	public void drawColorImage( double scale, 
				double xOffset, double yOffset, 
				BufferedImage image) throws IOException {
		if( !active ) throw notOpen;
		yOffset = bounds.getHeight()-yOffset-(double)image.getHeight();
		psout.println(xOffset +" "+ yOffset +" T");
		psout.println(scale +" "+ scale +" scale");
		psout.println(image.getWidth() +" "+ image.getHeight() +" scale");
		int w = image.getWidth();
		psout.println("/pstr "+ (3*image.getWidth()) +" string def");
		psout.println(image.getWidth() +" "+ image.getHeight() +" 8 ["+
			image.getWidth() +" 0 0 "+ (-image.getHeight()) +" 0 "+
			image.getHeight() +"] {currentfile pstr readhexstring pop}" );
		psout.println("false 3 colorimage");
		StringBuffer sb;
		for(int y=0 ; y<image.getHeight() ; y++) {
			sb = new StringBuffer();
			for( int x=0 ; x<image.getWidth() ; x++ ) {
				int rgb = image.getRGB(x, y);
				sb.append( ascii[ (rgb>>16) & 0xff ] );
				sb.append( ascii[ (rgb>>8) & 0xff ] );
				sb.append( ascii[ rgb & 0xff ] );
				if(sb.length()>=72) {
					psout.println( sb.toString() );
					sb = new StringBuffer();
				}
			}
			if(sb.length()>0)psout.println( sb.toString() );
		}
		psout.println("S U V");
	}
	void openPrintStream(String filename) throws IOException {
		File file=null;
		if( filename==null) {
			JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
			int ok = c.showOpenDialog(null);
			if(ok==c.CANCEL_OPTION ) {
				throw new IOException("\nPS-init: cancelled by user");
			}
			file = c.getSelectedFile();
		} else {
			file = new File( filename );
		}
		if( file.exists() ) {
			int yn = JOptionPane.showConfirmDialog(haxby.map.MapApp.anchor, 
				"Overwrite "+file.getPath()+" ?",
				"PS Message",
				JOptionPane.YES_NO_OPTION);
			if( yn==JOptionPane.NO_OPTION ) {
				openPrintStream(null);
				return;
			}
		}
		psout = new PrintStream( 
			new FileOutputStream( file ));
	}
	static String[] ascii = asciiCode();
	static String[] asciiCode() {
		String[] code = new String[256];
		for( int i=0 ; i<256 ; i++ ) {
			if( i<16 ) code[i] = "0"+ Integer.toHexString(i).toUpperCase();
			else code[i] = Integer.toHexString(i).toUpperCase();
		}
		return code;
	}
	static final IOException notOpen = new IOException("output stream not open");
}
