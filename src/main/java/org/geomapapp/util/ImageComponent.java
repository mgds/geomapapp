package org.geomapapp.util;

import haxby.map.Overlay;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.geomapapp.image.BalancePanel;

public class ImageComponent extends ScalableComponent {
	protected BufferedImage image = null;
	JScrollPane scPane = null;
	JFileChooser chooser = null;
	BalancePanel balance=null;
	JDialog colorDialog=null;
	protected File file=null;
	static BufferedImage logo;
	boolean showLogo=false;
	Color color;
	Vector overlays;
	private PropertyChangeListener balancePropL;
	
	public ImageComponent() {
		this( 800, 600);
	}
	public ImageComponent(int width, int height) {
		setLayout(null);
		this.width = width;
		this.height = height;
	}
	public ImageComponent(JFileChooser chooser) {
		this();
		this.chooser = chooser;
	}
	public ImageComponent(File file) throws IOException {
		this();
		chooser = null;
		open(file);
	}
	public ImageComponent( BufferedImage image ) {
		this();
		this.image = image;
		width = image.getWidth();
		height = image.getHeight();
		file = null;
	}
	public ImageComponent( String url ) throws IOException {
		this();
		chooser = null;
		open(url);
	}
	public void addOverlay( Overlay ovl ) {
		if(overlays==null)overlays=new Vector();
		overlays.add(ovl);
		repaint();
	}
	public void removeOverlay( Overlay ovl ) {
		if(overlays==null) return;
		overlays.remove( ovl );
		repaint();
	}
	public void setColor(Color c) {
		color = c;
	}
	public void setChooser(JFileChooser chooser) {
		this.chooser = chooser;
	}
	public void addNotify() {
		super.addNotify();
	//	if( getTopLevelAncestor() instanceof JFrame ) {
			initBalance();
	//	}
		Container c = getParent();
		while( !( c instanceof JScrollPane) ) {
			c = c.getParent();
			if( c==null ) {
				scPane=null;
				return;
			}
		}
		scPane = (JScrollPane)c;
	}
	void initBalance() {
		if(balance!=null) return;
		balance = new BalancePanel();
		balancePropL = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if( evt.getPropertyName().equals("BRIGHTNESS") ) {
					repaint();
				} else if( evt.getPropertyName().equals("APPLY_MODS")) {
					apply();
				}
			}
		};
		balance.addPropertyChangeListener( balancePropL);
		if( getTopLevelAncestor() instanceof JFrame ) {
			colorDialog = new JDialog( (JFrame)getTopLevelAncestor(), "ColorBalance");
		} else if(getTopLevelAncestor() instanceof JDialog ) {
			colorDialog = new JDialog( (JDialog)getTopLevelAncestor(), "ColorBalance");
		}
		colorDialog.getContentPane().add( balance );
		colorDialog.pack();
	}
	public void showColorDialog() {
		if(  colorDialog==null )initBalance();
		colorDialog.show();
	}
	public boolean open() throws IOException {
		initChooser();
		Container c = getTopLevelAncestor();
		int ok = chooser.showOpenDialog(c);
		if( ok==chooser.CANCEL_OPTION ) return false;
		open( chooser.getSelectedFile() );
		return true;
	}
	void initChooser() {
		if( chooser==null ) chooser = new JFileChooser(
				System.getProperty("user.dir"));
	}
	public void open(String url) throws IOException {
		BufferedImage image = ImageIO.read( haxby.util.URLFactory.url(url) );
		if(image==null)return;
		this.image = image;
		width = image.getWidth();
		height = image.getHeight();
		this.file = file;
		if(isVisible())repaint();
	}
	public void open(File file) throws IOException {
		BufferedImage image = ImageIO.read( file );
		if(image==null)return;
		this.image = image;
		width = image.getWidth();
		height = image.getHeight();
		this.file = file;
		repaint();
	}
	public File getFile() {
		return file;
	}
	public BufferedImage getImage() {
		return image;
	}
	public void save() throws IOException {
		initChooser();
		int ok = JOptionPane.NO_OPTION;
		if( file!=null ) {
			ok = JOptionPane.showConfirmDialog(
				(JFrame)getTopLevelAncestor(),
				"overwrite "+file.getName()+"?",
				"overwrite?",
				JOptionPane.YES_NO_CANCEL_OPTION);
			if( ok==JOptionPane.CANCEL_OPTION) return;
			chooser.setSelectedFile( file);
		}
		if( ok==JOptionPane.NO_OPTION) {
			ok = chooser.showSaveDialog((JFrame)getTopLevelAncestor());
			if( ok==chooser.CANCEL_OPTION) return;
			file = chooser.getSelectedFile();
		}
		apply();
		String type = file.getName().substring(
			file.getName().indexOf(".")+1).toLowerCase();
		if( !type.equals("jpg") && !type.equals("png") && !type.equals("tif")) type="jpg";
		ImageIO.write( image, type, file);
	}
	public void paintComponent(Graphics g) {
		if( color!=null ) {
			g.setColor(color);
			Dimension dim = getSize();
			g.fillRect(0,0,dim.width,dim.height);
		}
		if(image==null) {
			return;
		}
		Insets ins = getInsets();
		Graphics2D g2 = (Graphics2D) g;
		AffineTransform at0 = g2.getTransform();
		AffineTransform at = getTransform();
		Rectangle rect = getVisibleRect();
		if( ins!=null) {
			g2.translate(ins.left, ins.top);
			rect.width -= ins.left + ins.right;
			rect.height -= ins.top + ins.bottom;
		}
		if( tracksWidth ) {
			double scale = rect.getWidth()/width;
			at.setToScale( scale, at.getScaleY());
		}
		if( tracksHeight ) {
			double scale = rect.getHeight()/height;
			at.setToScale( at.getScaleX(), scale);
		}
		ByteLookupTable lookup = balance==null ? null
				: balance.getLookup();
		g2.transform(at);
		if(lookup!=null) {
			g2.drawImage( image, new LookupOp( lookup, null ), 0, 0);
		} else {
			g2.drawRenderedImage(image, new AffineTransform() );
		}
		if( overlays!=null ) {
			for( int k=0 ; k<overlays.size() ; k++) {
				((Overlay)overlays.get(k)).draw(g2);
			}
		}
		g2.setTransform( at0 );
/*
		if( showLogo || logo!=null ) {
			Rectangle r = getVisibleRect();
			int x = r.x+r.width-logo.getWidth();
			int y = r.y+r.height-logo.getHeight();
			if( ins!=null) {
				x -= ins.bottom;
				y -= ins.right;
			}
			AffineTransform at = new AffineTransform();
			at.translate(x,y);
			g2.drawRenderedImage( logo, at);
		}
*/
	}
	public void rotate( Point2D center, double angle ) {
		BufferedImage im = new BufferedImage( width, height, image.TYPE_INT_RGB);
		Graphics2D g = im.createGraphics();
		AffineTransform at = new AffineTransform();
		at.rotate( -angle, center.getX(), center.getY() );
		g.drawRenderedImage( image, at);
		image = im;
		repaint();
	}
	public void apply() {
		if( balance==null ) return;
		ByteLookupTable lookup = balance.getLookup();
		if(lookup==null) return;
		
		int ok = (getTopLevelAncestor() instanceof JFrame )
			? JOptionPane.showConfirmDialog(
				(JFrame)getTopLevelAncestor(),
				"Apply Color Mods?",
				"Modify Image Color",
				JOptionPane.YES_NO_OPTION)
			: JOptionPane.showConfirmDialog(
				(JOptionPane)getTopLevelAncestor(),
				"Apply Color Mods?",
				"Modify Image Color",
				JOptionPane.YES_NO_OPTION);
		if( ok== JOptionPane.NO_OPTION) return;
		BufferedImage im = new BufferedImage( width, height, 
			image.TYPE_INT_RGB);
		Graphics2D g = im.createGraphics();
		g.drawImage( image, new LookupOp( lookup, null ), 0, 0);
		image = im;
		balance.reset();
	}
	public void setImage(BufferedImage image) {
		if( this.image == image) return;
		this.image = image;
		width = image.getWidth();
		height = image.getHeight();
		if( logo!=null && showLogo ) {
			Graphics2D g = image.createGraphics();
			g.drawImage( logo, 
				image.getWidth()-logo.getWidth(),
				image.getHeight()-logo.getHeight(),
				this);
		}
		if( isVisible() ) repaint();
		if( scPane!=null ) {
			invalidate();
			scPane.validate();
		}
	}
	public void addLogo() {
		if( logo==null ) {
			if(!getLogo()) return;
		}
		Graphics2D g = image.createGraphics();
		g.drawImage( logo, 
			image.getWidth()-logo.getWidth(),
			image.getHeight()-logo.getHeight(),
			this);
		showLogo=true;
	}
	boolean getLogo() {
		try {
			ClassLoader loader  = org.geomapapp.util.Icons.class.getClassLoader();
			String path = "org/geomapapp/resources/icons/gma.png";
			java.net.URL url = loader.getResource(path);
			logo = ImageIO.read(url);
			return true;
		} catch(Exception ex) {
			return false;
		}
	}
	public void dispose() {
		if (balance != null)
			balance.removePropertyChangeListener(balancePropL);
	}
}
