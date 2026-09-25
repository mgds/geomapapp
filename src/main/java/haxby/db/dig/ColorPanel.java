package haxby.db.dig;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ColorPanel extends JPanel
			implements MouseListener,
			MouseMotionListener,
			ActionListener {
	Color color;
	Color startColor;
	int transparency, startTrans;
	ColorPane[] pane;
	TransparencyPane tPane;
	ColorSwatch swatch;
	static Cursor cursor = Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR );
	public ColorPanel( Color color, int transparency, ColorSwatch swatch ) {
		super( new BorderLayout() );
		this.transparency = transparency;
		startTrans = transparency;
		JPanel panel = new JPanel(new GridLayout(1,0) );
		this.color = color;
		startColor = color;
		pane = new ColorPane[3];
		for( int k=0 ; k<3 ; k++ ) {
			pane[k]=new ColorPane(color, k);
			pane[k].addMouseListener(this);
			pane[k].addMouseMotionListener(this);
			panel.add( pane[k] );
		}
		tPane = new TransparencyPane( color, transparency );
		tPane.addMouseListener(this);
		tPane.addMouseMotionListener(this);
		panel.add( tPane );
		add(panel, "Center" );
		this.swatch = swatch;
		swatch.setColor(color);
		swatch.setTransparency( transparency);
		panel = new JPanel(new GridLayout(0,1) );
		JButton button = new JButton("reset");
		panel.add( button );
		add( panel, "South");
		button.addActionListener( this );
	}
	public void setSwatch( ColorSwatch swatch ) {
		this.swatch = swatch;
		setColor( swatch.getColor() );
		setTransparency( swatch.getTransparency() );
	}
	public Color getColor() {
		return color;
	}
	public int getTransparency() {
		return transparency;
	}
	public void setColor( Color color ) {
		this.color = color;
		startColor = color;
	}
	public void setTransparency(int transparency) {
		this.transparency = transparency;
		startTrans = transparency;
	}
	public void reset() {
		color = startColor;
		for(int k=0 ; k<3 ; k++) {
			pane[k].setColor( color );
			pane[k].repaint();
		}
		tPane.setColor( color );
		tPane.setTransparency( startTrans );
		tPane.repaint();
		swatch.setColor( color );
		swatch.setTransparency( startTrans );
		swatch.repaint();
	}
	public void actionPerformed( ActionEvent evt ) {
		String cmd = evt.getActionCommand();
		if( cmd.equals("reset")) {
			reset();
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
		mouseReleased( evt );
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
		Component c = evt.getComponent();
		c.setCursor( Cursor.getDefaultCursor() );
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.getSource() instanceof ColorPane) {
			ColorPane src = (ColorPane) evt.getSource();
			float scale = (float) (src.getHeight()-4);
			float val = 1f-(float)( (evt.getY()-2) )/scale;
			if(val<0f)val=0f;
			if(val>1f)val=1f;
			float[] hsb = Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), null);
			hsb[src.getHSorB()] = val;
			color = new Color( Color.HSBtoRGB( hsb[0], hsb[1], hsb[2] ) );
			for(int k=0 ; k<3 ; k++) {
				pane[k].setColor( color );
				pane[k].repaint();
			}
			tPane.setColor( color );
			tPane.repaint();
			swatch.setColor( color );
			swatch.repaint();
		} else {
			TransparencyPane src = (TransparencyPane)evt.getSource();
			float scale = (float) (src.getHeight()-4);
			int val = (int) ( 255f*(1f-(float)( (evt.getY()-2) )/scale));
			if(val<0)val=0;
			if(val>255)val=255;
			tPane.setTransparency( val );
			tPane.repaint();
			swatch.setTransparency( val );
			swatch.repaint();
		}
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		Component c = evt.getComponent();
		if( c.getCursor()!=cursor ) c.setCursor(cursor);
		mouseClicked(evt);
	}
	public static void main(String[] args) {
		JFrame f = new JFrame();
		ColorSwatch swatch = new ColorSwatch( Color.blue, 255);
		f.getContentPane().add( new ColorPanel( Color.blue, 255, swatch), "Center");
		f.getContentPane().add(swatch, "South");
		f.pack();
		f.show();
		f.setDefaultCloseOperation( f.EXIT_ON_CLOSE);
	}
}
