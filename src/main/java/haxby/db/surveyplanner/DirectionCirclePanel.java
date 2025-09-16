package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import javax.swing.JPanel;

/**
 * Draw a circle with a horizon at a given angle, and an arrow pointing out
 * in the direction in which we wish to add additional lines.
 * Selecting the circle will turn the border red.
 * @author Neville Shane 2017
 *
 */
public class DirectionCirclePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	int centerX, centerY, radius, angle;
	Color color = COLOR_DESELECTED;
	boolean selected = false;
	boolean enabled = true;
	private static Color COLOR_SELECTED = Color.red;
	private static Color COLOR_DESELECTED = Color.black;
	
	public void setArc(int centerX,int centerY, int radius, int angle) {
	    this.centerX = centerX;
	    this.centerY = centerY;
	    this.radius = radius;
	    this.angle = angle;
	    revalidate();
	    repaint();
	}
	
	public void setSelected(boolean selected) {
		if (selected) {
			this.color = COLOR_SELECTED;
		} else {
			this.color = COLOR_DESELECTED;
		}
		this.selected = selected;
		revalidate();
		repaint();
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setAngle(int angle) {
		this.angle = angle;
		revalidate();
		repaint();
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		revalidate();
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setStroke(new BasicStroke(2));
	    
	    //rotate the circle so that the horizon is at the given bearing
	    g2.rotate(Math.toRadians(90 + angle), radius/2., radius/2.);
	    
	    //draw the black half of the circle
	    g2.setColor(Color.lightGray);
	    g2.fillArc(centerX, centerY, radius, radius, 0, 180);
	    //draw the white half of the circle
	    g2.setColor(Color.WHITE); 
	    
	    g2.fillArc(centerX, centerY, radius, radius, 0, -180);
	    //draw the outline of the circle, either red for selected, or black for not selected
	    if (enabled) g2.setColor(color);
	    else g2.setColor(Color.lightGray);
	    g2.drawArc(centerX, centerY, radius, radius, 0, 360);
	    
	    //draw the arrow
	    Path2D arrow = new Path2D.Double();
	    double arr_size = radius/6.;
	    double x0 = radius/2. + centerX;
	    double y0 = radius/2. + centerY;
        double xpts[] = {x0-0.6*arr_size, x0-0.6*arr_size, x0-1.*arr_size, x0, x0+1.*arr_size, x0+0.6*arr_size, x0+0.6*arr_size};
        double ypts[] = {y0, y0+1*arr_size, y0+1.*arr_size, y0+2.*arr_size, y0+1.*arr_size, y0+1*arr_size, y0};
        arrow.moveTo(xpts[0], ypts[0]);
        arrow.lineTo(xpts[1], ypts[1]);
        arrow.lineTo(xpts[2], ypts[2]);
        arrow.lineTo(xpts[3], ypts[3]);
        arrow.lineTo(xpts[4], ypts[4]);
        arrow.lineTo(xpts[5], ypts[5]);
        arrow.lineTo(xpts[6], ypts[6]);
        if (enabled) g2.setColor( Color.black );
        else g2.setColor(Color.lightGray);
        g2.draw(arrow);
	}
}