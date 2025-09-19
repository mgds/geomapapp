package haxby.image;

import haxby.map.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.io.*;
import java.util.*;

//import com.sun.image.codec.jpeg.*;

public class Composer extends JComponent
			implements Zoomable,
				ActionListener,
				KeyListener,
				MouseListener,
				MouseMotionListener {

	File imageFile;
	BufferedImage image=null;
	double zoom;
	JFileChooser chooser;
	JScrollPane scrollPane;
	JFrame frame;
	JPanel mainPanel;
	Box tools;
	JToggleButton select;
	JToggleButton text;
	JToggleButton cursor;
	JButton save;
	Vector elements;
	Vector arrows;
	Vector textObjects;
	BufferedImage arrow;
	Point arrowOffset;
	TextDialog textDialog;
	Point from=null;
	Point to=null;
	ComposerElement selectedElement = null;
	Shape shape = null;
	public Composer() {
		chooser = new JFileChooser( System.getProperty( "user.dir" ) );
		zoom = 1.;
		Zoomer zoomer = new Zoomer( this );
		addMouseListener( this );
		addMouseMotionListener( this );
		addKeyListener( this );
		addMouseListener( zoomer );
		addMouseMotionListener( zoomer );
		addKeyListener( zoomer );
		scrollPane = new JScrollPane( this );
		frame = new JFrame( "Untitled" );
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE );
		mainPanel = new JPanel( new BorderLayout() );
		mainPanel.add( scrollPane, "Center");
		initTools();
		mainPanel.add( tools, "North");
		frame.getContentPane().add(mainPanel, "Center");
		JMenuBar bar = new JMenuBar();
		JMenu fileM = new JMenu("File");
		JMenuItem openMI = new JMenuItem("open");
		fileM.add(openMI);
		bar.add(fileM);
		JMenuItem quitMI = new JMenuItem("quit");
		openMI.addActionListener( this );
		quitMI.addActionListener( this );
		fileM.add( quitMI );
		frame.setJMenuBar( bar );
		frame.pack();
		frame.setVisible(true);
		textDialog = new TextDialog();
	}
	void loadImage() {
		int ok = chooser.showOpenDialog(frame);
		if( ok!=chooser.APPROVE_OPTION ) return;
		File file = chooser.getSelectedFile();
		String name = file.getName();
		while( !name.toLowerCase().endsWith("jpg") ) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Only JPEG images currently loadable - suffix \".jpg\"" );
			ok = chooser.showOpenDialog(frame);
			if( ok!=chooser.APPROVE_OPTION ) return;
			file = chooser.getSelectedFile();
			name = file.getName();
		}
		image = null;
		imageFile = file;
		try {
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream( file ));
			//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
			//BufferedImage im = decoder.decodeAsBufferedImage();
			//image = im;
			image = ImageIO.read(in);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error while loading:\n  "+ex.getMessage());
			System.exit(0);
		}
		zoom = 1.;
		elements = new Vector();
		frame.setTitle( file.getName() );
		frame.pack();
	}
	void initTools() {
		tools = Box.createHorizontalBox();
		ButtonGroup gp = new ButtonGroup();
		select = new JToggleButton( MapTools.SELECT( false ) );
		ImageIcon sel = MapTools.SELECT( true );
		BufferedImage tmp = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = tmp.createGraphics();
		haxby.util.SimpleBorder b = new haxby.util.SimpleBorder();
		b.setSelected(false);
		b.paintBorder( select, g2d, 0, 0, 22, 22);
		g2d.drawImage( sel.getImage(), 1, 1, this);
		int test = tmp.getRGB( 1, 1);
		for( int x=1 ; x<21 ; x++ ) {
			for( int y=0 ; y<21 ; y++ ) {
				if( tmp.getRGB(x, y)==test) tmp.setRGB(x, y, 0);
			}
		}
		select.setIcon( new ImageIcon( tmp ));
		b.setSelected(true);
		BufferedImage tmp1 = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		g2d = tmp1.createGraphics();
		g2d.drawImage( tmp, 0, 0, this);
		b.paintBorder( select, g2d, 0, 0, 22, 22);
		select.setSelectedIcon( new ImageIcon( tmp1 ) );
		select.setBorder( null );
		gp.add( select );
		tools.add( select );
		text = new JToggleButton( haxby.db.mcs.Buttons.POSITIVE( false ) );
		text.setSelectedIcon( haxby.db.mcs.Buttons.POSITIVE(true) );
		tools.add( text );
		text.setBorder( border );
		gp.add( text );
		ImageElement.initArrow();
		BufferedImage arrow = ImageElement.arrow;
		BufferedImage im = new BufferedImage( 20, 20, 
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		g.translate(2.5, 2.5);
		g.scale(.75, .75);
		g.drawImage(arrow, 0, 0, this);
		cursor = new JToggleButton( new ImageIcon( im ) );
		cursor.setBorder( border );
		tools.add( cursor );
		gp.add( cursor );
		tools.add( Box.createHorizontalStrut(4) );
		save = new JButton( haxby.db.mcs.Buttons.SAVE(false) );
		save.setPressedIcon( haxby.db.mcs.Buttons.SAVE(true) );
		save.setBorder( border );
		tools.add( save );
		save.addActionListener( this);
		arrows = new Vector();
		textObjects = new Vector();
		elements = new Vector();
	}
	public Dimension getPreferredSize() {
		if( image==null ) {
			return new Dimension( (int)(640.*zoom), (int)(zoom*480.) );
		}
		return new Dimension( (int)(zoom*image.getWidth()),
					(int)(zoom*image.getHeight()) );
	}
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.scale( zoom, zoom );
		if( image != null )g2.drawImage( image, 0, 0, this );
		RenderingHints hints = g2.getRenderingHints();
/*
		if( textObjects.size()!=0 ) {
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			for(int k=0 ; k<textObjects.size() ; k++) {
				TextElement ti = (TextElement)textObjects.get(k);
				ti.draw( g2 );
			}
		}
		if( arrows.size()!=0 ) {
			for(int k=0 ; k<arrows.size() ; k++) {
				ImageElement ti = (ImageElement)arrows.get(k);
				ti.draw( g2 );
			}
		}
*/
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		for( int k=0 ; k<elements.size() ; k++) {
			((ComposerElement) elements.get(k)).draw(g2);
		}
		if( selectedElement != null ) {
			shape = selectedElement.getShape();
			g2.setRenderingHints( hints );
			g2.setXORMode( Color.blue );
			g2.draw( shape );
		}
			
	//	if( shape != null ) g2.draw(shape);
	}
	public void keyPressed(KeyEvent evt) {
	}
	BufferedImage clipboard=null;
	public void keyReleased(KeyEvent evt) {
		if( evt.getKeyCode()==evt.VK_DELETE && selectedElement!=null ) {
			elements.remove( selectedElement );
		//	textObjects.remove(selectedElement);
		//	arrows.remove( selectedElement );
			shape = null;
			selectedElement=null;
			repaint();
		} else if( evt.getKeyCode()==evt.VK_C ) {
			clipboard = new BufferedImage( image.getWidth(), image.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			double z = zoom;
			zoom = 1.;
			Graphics2D g = clipboard.createGraphics();
			paint(g);
		} else if( evt.getKeyCode()==evt.VK_V ) {
			if( clipboard==null ) return;
			elements.add( new ImageElement( new Point( 10, 10), clipboard ) );
			repaint();
		}
	}
	public void keyTyped(KeyEvent evt) {
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getActionCommand().equals("open") ) {
			loadImage();
			return;
		} if( evt.getActionCommand().equals("quit") ) {
			System.exit(0);
		} else if( evt.getSource()==save ) {
			int ok = chooser.showSaveDialog( frame );
			if( ok == chooser.CANCEL_OPTION )return;
			while( chooser.getSelectedFile().exists() ) {
				ok = JOptionPane.showConfirmDialog( frame, "over-write "
						+chooser.getSelectedFile().getName()+"?",
						"over-write?", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE );
				if( ok==JOptionPane.CANCEL_OPTION) return;
				if( ok==JOptionPane.YES_OPTION) break;
				ok = chooser.showSaveDialog( frame );
				if( ok == chooser.CANCEL_OPTION )return;
			}
			double z = zoom;
			zoom = 1.;
			Dimension dim = getPreferredSize();
			BufferedImage im = new BufferedImage( dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = im.createGraphics();
			paintComponent(g);
			try {
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream( chooser.getSelectedFile() ));
				//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
				//encoder.encode( im );
				ImageIO.write(im, "JPEG", out);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(frame, "Error while saving:\n  "+ex.getMessage());
			}
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
		from = evt.getPoint();
		to = null;
	}
	public void mouseReleased( MouseEvent evt ) {
		if( evt.isControlDown() )return;
		if( selectedElement==null ) return;
		if( to==null )return;
		from = to = null;
		shape = selectedElement.getShape();
		repaint();
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.isControlDown() )return;
		if( select.isSelected() ) {
			drawShape();
			int x = (int) (evt.getX()/zoom);
			int y = (int) (evt.getY()/zoom);
			for( int i=elements.size()-1 ; i>=0 ; i--) {
				ComposerElement el = (ComposerElement)elements.get(i);
				if( el.select(new Point(x, y)) ) {
					if( (el instanceof TextElement) && evt.getClickCount()==2 ) {
						shape = el.getShape();
						TextElement ob = (TextElement)el;
						int ok = textDialog.showDialog( this, ob, evt.getPoint());
						repaint();
						return;
					}
					selectedElement = el;
					shape = el.getShape();
					drawShape();
					return;
				}
			}
			shape=null;
			selectedElement = null;
			repaint();
			return;
		} else if( text.isSelected() ) {
			int x = (int) (evt.getX()/zoom);
			int y = (int) (evt.getY()/zoom);
			Point point = new Point(x, y);
			TextElement ti = new TextElement(point);
			elements.add(ti);
			int ok = textDialog.showDialog( this, ti, evt.getPoint());
			if( ok == -1 ) elements.remove(ti);
			repaint();
		} else if( cursor.isSelected() ) {
			int x = (int) (evt.getX()/zoom);
			int y = (int) (evt.getY()/zoom);
			Point point = new Point(x, y);
			elements.add( new ImageElement( point ));
			repaint();
		}
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		if( evt.isControlDown() )return;
		if( selectedElement!=null ) {
			int x = (int) (evt.getX()/zoom);
			int y = (int) (evt.getY()/zoom);
			if( to==null ) {
				to = new Point(x, y);
				return;
			}
			from = to;
			to = new Point(x, y);
			drawShape();
			selectedElement.dragged( from, to );
			shape = selectedElement.getShape();
			drawShape();
		}
	}
	void drawShape() {
		if( shape==null )return;
		synchronized( getTreeLock() ) {
			Graphics2D g = (Graphics2D) getGraphics();
			g.scale( zoom, zoom);
			g.setXORMode( Color.blue );
			g.draw(shape);
		}
	}
	public void setXY(Point p) {
	}
	public void setRect(Rectangle rect) {
	}
	public void zoomTo(Rectangle rect) {
	}
	public void zoomIn( Point p ) {
		doZoom( p, 2d );
	}
	public void zoomOut( Point p ) {
		Dimension dim = getPreferredSize();
		if( getVisibleRect().contains( new Rectangle(0,0,dim.width, dim.height ))) return;
		doZoom( p, .5d );
	}
	public void doZoom( Point p, double factor ) {
		Rectangle rect = getVisibleRect();
		double x = p.getX() / zoom;
		double y = p.getY() / zoom;
		double w = rect.getWidth();
		double h = rect.getHeight();
		zoom *= factor;
		int newX = (int) (x*zoom - w*.5d);
		int newY = (int) (y*zoom - h*.5d);
		invalidate();
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(newX);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(newY);
		revalidate();
	//	repaint();
	}
	static javax.swing.border.Border border = BorderFactory.createLineBorder(Color.black);
	public static void main( String[] args ) {
		new Composer();
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}
