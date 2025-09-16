package haxby.dig;

import haxby.map.*;
import haxby.proj.*;
import haxby.util.*;
import haxby.image.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

public class AnnotationObject extends LineSegmentsObject
				implements DigitizerObject,
					MouseListener,
					MouseMotionListener,
					KeyListener {
	private String annotation = "";

	static ImageIcon ICON = Icons.getIcon(Icons.ANNOTATION, false);
	static ImageIcon SELECTED_ICON = Icons.getIcon(Icons.ANNOTATION, true);
	static ImageIcon DISABLED_ICON = new ImageIcon(
			GrayFilter.createDisabledImage( ICON.getImage() ));

	private Font font;

	public AnnotationObject( ScaledComponent map, Digitizer dig ) {
		super (map, dig);
		font = new Font("Times New Roman", 0, 12);
	}

	public void mouseClicked( MouseEvent evt ) {
		super.mouseClicked( evt );

		if ( active && points.size() >= 2) {
			dig.selectB.doClick();

			annotation = JOptionPane.showInputDialog(MapApp.anchor, "Annotation:");
			if (annotation == null) {
				dig.delete();
				return;
			}
			map.repaint();
		}
	}

	public void draw( Graphics2D g, double[] scales, Rectangle bounds, Insets ins ) {
		super.draw( g, scales, bounds, ins );

		if( points.size()<2 || annotation == null) return;
	
		double[] xyz = (double[])points.get(1);
		double x1 = xyz[0] * scales[0] + ins.left;
		double y1 = xyz[1] * scales[1] + ins.top;

		Rectangle2D annoBounds = font.getStringBounds( annotation, g.getFontRenderContext() );

		x1 -= annoBounds.getWidth() / 2;

		if (( (double[])points.get(0) )[1] < ( (double[])points.get(1) )[1])
			y1 += annoBounds.getHeight();
		else
			y1 -= annoBounds.getHeight() * .5;
		g.setFont( font );
		g.drawString( annotation, (float) x1, (float) y1 );
	}

	public void mouseDragged( MouseEvent evt ) {
		super.mouseDragged( evt );
	}

	public void mouseReleased( MouseEvent evt ) {
		super.mouseReleased( evt );
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public ImageIcon getIcon() {
		return ICON;
	}

	public ImageIcon getDisabledIcon() {
		return DISABLED_ICON;
	}

	public void setFont(Font f) {
		font = f;
	}

	public Font getFont() {
		return font;
	}
}