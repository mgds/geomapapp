package org.geomapapp.db.dsdp;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import org.geomapapp.util.Icons;
import org.geomapapp.util.ImageComponent;

public class StratigraphicRangeChart extends JComponent implements MouseListener, ActionListener, WindowListener {
	JDialog stratigraphicRangesDialog;
	JScrollPane stratigraphicRangesSP;
	JToggleButton stratTB;
	JButton zoomInB;
	JButton zoomOutB;
	JButton wikiB;
	String wikiURLString = "https://en.wikipedia.org/wiki/List_of_Global_Boundary_Stratotype_Sections_and_Points";
	ImageComponent stratImage;
	int prevAge = -1;
	double zScale = 1.0;

	public StratigraphicRangeChart( JFrame inputFrame ) {
		stratigraphicRangesDialog = new JDialog(inputFrame,"Stratigraphic Range Chart");
		stratigraphicRangesDialog.addWindowListener(this);
		try {
			String stratURL = PathUtil.getPath("DSDP/STRATIGRAPHIC_RANGES", 
					MapApp.BASE_URL+"/data/portals/dsdp/timescales/StratigraphicRanges.jpg");
			stratImage = new ImageComponent( 
					ImageIO.read(URLFactory.url(stratURL)));

			JPanel zoomPanel = new JPanel();
			zoomInB = new JButton(Icons.getIcon(Icons.ZOOM_IN, false));
			zoomInB.setSelectedIcon( Icons.getIcon( Icons.ZOOM_IN, true ) );
			zoomInB.setBorder( BorderFactory.createEmptyBorder() );
			zoomInB.addActionListener(this);
			zoomOutB = new JButton(Icons.getIcon(Icons.ZOOM_OUT, false));
			zoomOutB.setSelectedIcon( Icons.getIcon( Icons.ZOOM_OUT, true ) );
			zoomOutB.setBorder( BorderFactory.createEmptyBorder() );
			zoomOutB.addActionListener(this);
			wikiB = new JButton("List of Global Boundary Stratotype Sections and Points");
			wikiB.addActionListener(this);
			zoomPanel.add(zoomInB);
			zoomPanel.add(zoomOutB);
			zoomPanel.add(wikiB);
			stratigraphicRangesSP = new JScrollPane(stratImage);
			stratImage.zoomOut(new Point(0,0));
//			Zoomer z = new Zoomer(stratImage);
//			stratImage.addMouseListener(z);
//			stratImage.addKeyListener(z);
//			stratImage.addMouseListener(this);
			stratigraphicRangesDialog.getContentPane().add(zoomPanel, "North");
			stratigraphicRangesDialog.getContentPane().add(stratigraphicRangesSP);
			stratigraphicRangesDialog.pack();
			stratigraphicRangesDialog.setSize(stratImage.getWidth() + stratigraphicRangesSP.getVerticalScrollBar().getWidth() + 11,500);
			stratigraphicRangesDialog.setLocation(inputFrame.getLocation().x+400,inputFrame.getLocation().y);
			stratigraphicRangesDialog.setVisible(true);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		stratigraphicRangesDialog.dispose();
	}

	public void openInputURL(String inputURLString) {
		BrowseURL.browseURL(inputURLString);
	}

	public void drawLineAtAge( int currentAge ) {
		synchronized (stratImage.getTreeLock()) {
			Graphics2D g = (Graphics2D)stratImage.getGraphics();
			if (g == null) return;
			Rectangle r = stratImage.getVisibleRect();
			int x1 = r.x;
			int x2 = r.x+r.width;
			g.setXORMode( Color.cyan );
			if ( prevAge != -1)	{
				g.drawLine(x1, prevAge, x2, prevAge);
			}
			g.drawLine(x1, currentAge, x2, currentAge);
			prevAge = currentAge;
		}
	}

	public void setStratTB( JToggleButton inputStratTB ) {
		stratTB = inputStratTB;
	}

	public void mouseClicked(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void actionPerformed(ActionEvent aevt) {
		if ( aevt.getSource().equals(zoomInB) ) {
			if ( zScale < 2.0 ) {
				stratImage.zoomIn(new Point( (int)stratImage.getVisibleRect().getMinX(), (int)stratImage.getVisibleRect().getCenterY() ) );
				zScale *= 2.0;
				stratigraphicRangesDialog.pack();
				stratigraphicRangesDialog.setSize(stratImage.getWidth() + stratigraphicRangesSP.getVerticalScrollBar().getWidth() + 11,500);
				stratigraphicRangesDialog.setVisible(true);
			}
		}
		else if ( aevt.getSource().equals(zoomOutB) ) {
			if ( zScale > 1.0 ) {
				stratImage.zoomOut(new Point( (int)stratImage.getVisibleRect().getMinX(), (int)stratImage.getVisibleRect().getCenterY() ) );
				zScale /= 2.0;
				stratigraphicRangesDialog.pack();
				stratigraphicRangesDialog.setSize(stratImage.getWidth() + stratigraphicRangesSP.getVerticalScrollBar().getWidth() + 11,500);
				stratigraphicRangesDialog.setVisible(true);
			}
		}
		else if ( aevt.getSource().equals(wikiB) ) {
			openInputURL(wikiURLString);
		}
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		if ( stratigraphicRangesDialog != null ) {
			stratigraphicRangesDialog.dispose();
		}
		if ( stratTB != null ) {
			stratTB.setSelected(false);
		}
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowOpened(WindowEvent arg0) {
	}
}
