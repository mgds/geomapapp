package haxby.db.custom;

import haxby.map.MapApp;
import haxby.util.DisplayUtil;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.geomapapp.db.dsdp.CustomBRGTable;
import org.geomapapp.util.CustomXYGraph;
import org.geomapapp.util.Zoomer;

public class CustomGraph implements WindowListener, ItemListener, ActionListener, MouseListener, MouseMotionListener {
	JDialog testDialog;
	JDialog selectColumnDialog;
	JComboBox columnCB;
	JComboBox selectAddColumnCB;
	Box testBox;
	JButton saveDataB;
	JCheckBox flipYAxisCB;
	JCheckBox ignoreZerosCB;
	CustomXYGraph testGraph;
	CustomDB customDB;
	String urlString;
	String name;
	int max_Height = 700;
	int min_Height = 400;
	int preferred_Height = 700;
	int max_Width = 300;
	int min_Width = 300;
	int preferred_Width = 300;

	public CustomGraph( String inputURLString, CustomDB inputCustomDB, String inputName ) {
		urlString = inputURLString;
		customDB = inputCustomDB;
		name = inputName;
	}

	public void dispose() {
		testDialog.dispose();
		testDialog = null;
		System.gc();
	}

	public void setMinSize( int inputWidth, int inputHeight ) {
		max_Width = inputWidth;
		max_Height = inputHeight;
	}

	public void setMaxSize( int inputWidth, int inputHeight ) {
		min_Width = inputWidth;
		min_Height = inputHeight;
	}

	public void setPreferredSize( int inputWidth, int inputHeight ) {
		preferred_Width = inputWidth;
		preferred_Height = inputHeight;
	}

	public void initDialog() {
		try {
			testBox = Box.createHorizontalBox();
			System.out.println(name);
			testDialog = new JDialog(((haxby.map.MapApp)customDB.map.getApp()).getFrame(),name);
			testDialog.addWindowListener(this);
			JPanel selectSedimentPanel = new JPanel( new BorderLayout() );
			selectSedimentPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
			JPanel sedimentDialogBottomPanel = new JPanel( new GridLayout(0,1) );
			JPanel sedimentDialogSouthPanel = new JPanel( new FlowLayout());
			columnCB = new JComboBox();
			JLabel sedimentLabel = new JLabel("");
			saveDataB = new JButton("Save Data");
			columnCB.addItemListener(this);
			columnCB.setAlignmentX(Component.LEFT_ALIGNMENT);
			saveDataB.addActionListener(this);
			testDialog.setLayout( new BorderLayout() );
			testDialog.getContentPane().add( selectSedimentPanel, "North" );
			sedimentDialogSouthPanel.add(saveDataB);
			sedimentDialogBottomPanel.add(sedimentDialogSouthPanel);
			sedimentDialogBottomPanel.add( sedimentLabel);
			testDialog.getContentPane().add( sedimentDialogBottomPanel, "South" );
			CustomBRGTable testPts;
			testPts = new CustomBRGTable(urlString);
			String[] columnHeadings = null;
			columnHeadings = testPts.getColumnHeadings();
			for ( int i = 1; i < columnHeadings.length; i++ ) {
				columnCB.addItem(columnHeadings[i]);
			}
			if ( columnCB.getItemCount() > 1 ) {
				selectSedimentPanel.add(columnCB, BorderLayout.WEST );
			}
			testGraph = new CustomXYGraph( testPts, 0 );
			selectAddColumnCB = new JComboBox();
			selectAddColumnCB.addItem("Add Graph");
			for ( int i = 1; i < ((CustomBRGTable)testGraph.getPoints()).getColumnHeadings().length; i++ ) {
				selectAddColumnCB.addItem(((CustomBRGTable)testGraph.getPoints()).getColumnHeadings()[i]);
			}
			selectAddColumnCB.addActionListener(this);
			sedimentDialogSouthPanel.add(selectAddColumnCB);
			flipYAxisCB = new JCheckBox("Flip Y-Axis", CustomBRGTable.REVERSE_Y_AXIS );
			ignoreZerosCB = new JCheckBox("Ignore 0's", CustomBRGTable.IGNORE_ZEROS );
			flipYAxisCB.addActionListener(this);
			ignoreZerosCB.addActionListener(this);
			sedimentDialogSouthPanel.add(flipYAxisCB, "East");
			sedimentDialogSouthPanel.add(ignoreZerosCB);
			Zoomer z = new Zoomer(testGraph);
			testGraph.setScrollableTracksViewportWidth(true);
			testGraph.setScrollableTracksViewportHeight(true);
			testGraph.setMinimumSize(new Dimension(300, min_Height));
			testGraph.setPreferredSize(new Dimension( 300, preferred_Height));
			testGraph.setMaximumSize(new Dimension( 300, max_Height));
			testGraph.setAlignmentX(Component.TOP_ALIGNMENT);
			testGraph.addMouseListener(z);
			testGraph.addKeyListener(z);
			testGraph.addMouseMotionListener(this);
			testBox.add(testGraph);
			testBox.setMinimumSize(new Dimension( 400, min_Height));
			testBox.setPreferredSize(new Dimension( 10000, preferred_Height));
			testBox.setMaximumSize(new Dimension( 10000, max_Height));
			JScrollPane sedimentSP = new JScrollPane( testBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
			testDialog.getContentPane().add( sedimentSP, "Center" );
			testDialog.pack();
			DisplayUtil.setRelativeLocation(testDialog, 500, 500, MapApp.anchor);
			testDialog.setSize( 600, 400 );
			testDialog.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		dispose();
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowOpened(WindowEvent arg0) {
	}

	public void itemStateChanged(ItemEvent ievt) {
		if ( ievt.getSource().equals(columnCB) && columnCB != null && testGraph != null ) {
			testGraph.setPoints( testGraph.getPoints(), columnCB.getSelectedIndex() );
			testGraph.repaint();
		}
	}

	public void actionPerformed(ActionEvent aevt) {
		if ( aevt.getSource().equals(selectAddColumnCB) && selectAddColumnCB.getSelectedIndex() != 0 ) {
			int selectedAddColumn = selectAddColumnCB.getSelectedIndex();
			selectAddColumnCB.removeItemListener(this);
			CustomXYGraph tempGraph = new CustomXYGraph( ((CustomBRGTable)testGraph.getPoints()), selectedAddColumn - 1 );
			Zoomer z = new Zoomer(tempGraph);
			tempGraph.setScrollableTracksViewportWidth(true);
			tempGraph.setScrollableTracksViewportHeight(true);
			testGraph.setMinimumSize(new Dimension(300, 700));
			testGraph.setPreferredSize(new Dimension( 300, 700));
			testGraph.setMaximumSize(new Dimension( 300, 700));
			tempGraph.setMinimumSize(new Dimension(300, 700));
			tempGraph.setPreferredSize(new Dimension( 300, 700));
			tempGraph.setMaximumSize(new Dimension( 300, 700));
			int tempHeight = testDialog.getHeight();
			int tempWidth = testDialog.getWidth();
			testBox.add(tempGraph);
			testDialog.setSize( tempWidth + max_Width, tempHeight );
			testDialog.setVisible(true);
			tempGraph.setCloseButton(true);
			tempGraph.addMouseListener(this);
			tempGraph.addMouseMotionListener(this);
			selectAddColumnCB.addItemListener(this);
		} else if ( aevt.getSource().equals(saveDataB) ) {
			JFileChooser sedimentSaveDialog = new JFileChooser(System.getProperty("user.home"));
			File sedimentSaveFile = new File( urlString.substring( urlString.lastIndexOf("/") + 1 ) );
			sedimentSaveDialog.setSelectedFile(sedimentSaveFile);

			int c = JOptionPane.NO_OPTION;
			DisplayUtil.setRelativeLocation(sedimentSaveDialog, 300, 300, MapApp.anchor);

			while ( c == JOptionPane.NO_OPTION ) {
				c = sedimentSaveDialog.showSaveDialog(testDialog);
				if ( c == JFileChooser.CANCEL_OPTION ) {
					return;
				}
				else if ( c == JFileChooser.APPROVE_OPTION ) {
					sedimentSaveFile = sedimentSaveDialog.getSelectedFile();
					if ( sedimentSaveFile.exists() ) {
						int c2 = JOptionPane.showConfirmDialog(MapApp.anchor, "File exists, Overwrite?");
						if (c2 == JOptionPane.OK_OPTION ) {
							break;
						}
						else {
							c = JOptionPane.NO_OPTION;
						}
					}
				}
			}
			try {
				BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url( urlString ) ).openStream() ) );
				FileWriter out = new FileWriter(sedimentSaveFile);
				String s;
				while ( ( s = in.readLine() ) != null ) {
					out.write( s + "\n" );
				}
				in.close();
				out.flush();
				out.close();
			} catch (MalformedURLException mue) {
				mue.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if ( aevt.getSource().equals(flipYAxisCB) ) {
			try {
				CustomBRGTable.setReverseYAxis(flipYAxisCB.isSelected());
				testGraph.setPoints( new CustomBRGTable(urlString), columnCB.getSelectedIndex());
				testGraph.repaint();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		else if ( aevt.getSource().equals(ignoreZerosCB) ) {
			try {
				CustomBRGTable.setIgnoreZeros(ignoreZerosCB.isSelected());
				testGraph.setPoints( new CustomBRGTable(urlString), columnCB.getSelectedIndex());
				testGraph.repaint();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public void mouseClicked(MouseEvent mevt) {
		if ( !mevt.getSource().equals(testGraph) && mevt.getSource() instanceof org.geomapapp.util.CustomXYGraph ) {
			CustomXYGraph tempGraph = (CustomXYGraph)mevt.getSource();
			Rectangle r = tempGraph.getVisibleRect();
			if ( mevt.getX() + tempGraph.getX() > tempGraph.getX() + r.width - 10 && mevt.getX() + tempGraph.getX() < tempGraph.getX() + r.width && mevt.getY() > r.getMinY() && mevt.getY() < r.getMinY() + 10 ) {
				int tempHeight = testDialog.getHeight();
				int tempWidth = testDialog.getWidth();
				testBox.remove(tempGraph);
				testDialog.setSize( tempWidth - max_Width, tempHeight );
				testDialog.setVisible(true);
				tempGraph.removeMouseListener(this);
				tempGraph = null;
			}
		}
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void mouseDragged(MouseEvent arg0) {
	}

	public void mouseMoved(MouseEvent mevt) {
		if ( mevt.getSource() instanceof CustomXYGraph ) {
			for ( int i = 0; i < testBox.getComponentCount(); i++ ) {
				((CustomXYGraph)testBox.getComponent(i)).drawLineAtPoint(mevt.getY());
			}
		}
	}
}
