package haxby.db.dig;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Vector;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import haxby.db.Axes;
import haxby.db.Database;
import haxby.db.XYGraph;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.map.Zoomer;
import haxby.util.BrowseURL;
import haxby.util.GeneralUtils;
import haxby.util.PathUtil;

public class Digitizer implements Database,
				MouseListener,
				MouseMotionListener,
				KeyListener,
				ListSelectionListener,
				ActionListener, ComponentListener {
	public Vector<Object> objects;
	XMap map;
	JPanel tools;
	boolean enabled;
	DigitizerObject currentObject;
	private DigitizerObject inserterObject;
	private int lastPointSelected;
	int lastSelectedIndex;
	public JToggleButton startStopBtn;
	public JButton deleteBtn, helpBtn, saveBtn, deletePtsBtn, addBtn, insertBtn;
	JCheckBox autoscaleCB;
	public JRadioButton greatCircleRB, straightLineRB;
	public JTextField speedTF;
	public Class[] objectClasses;
	public JList list;
	public DigListModel model;
	DigitizerOptionsDialog options;
	boolean listening;
	public JTable table;
	public JScrollPane tableSP;
	public JPanel buttons, speedP;
	XYGraph graph;
	DigProfile profile;
	public int obj_ind = 1;
	boolean editing;
	private boolean loadedGMRTForDig = false;
	private boolean isSurveyPlanner = false; //is Digitizer being used by Survey Planner
	private double speed = 0; // for use in Survey Planner
	public boolean didLoadGMRT = false;
	
	public Digitizer( XMap map ) {
		this.map = map;
		objects = new Vector<Object>();
		currentObject = null;
		inserterObject = null;
		model = new DigListModel( this );
		list = new JList(model);
		list.setCellRenderer( new DigCellRenderer() );
		list.addListSelectionListener( this);
		list.addMouseListener( this);
		initTools();
		enabled = false;
		listening = false;
		lastSelectedIndex = -1;
		options = new DigitizerOptionsDialog( map );
		initDialog();
		profile = new DigProfile( map );
		graph = new XYGraph( profile, 0);
		graph.setScrollableTracksViewportHeight( true );
		graph.setScrollableTracksViewportWidth( false );
		graph.setAxesSides( Axes.LEFT | Axes.BOTTOM );
		Zoomer zoomer = new Zoomer( graph );
		graph.addMouseListener( zoomer );
		graph.addMouseMotionListener( zoomer );
		graph.addKeyListener( zoomer );
		map.addComponentListener(this);
	}
	JPanel dialogPanel;
	JRadioButton[] tabs;
	void initDialog() {
		table = new JTable(new LineSegmentsObject(map, this));
		table.addKeyListener( this );
		table.addMouseListener( this);
		// make rows drag-able
		table.setDragEnabled(true);
		table.setDropMode(DropMode.INSERT_ROWS);
		table.setTransferHandler(new TableRowTransferHandler(table)); 
		tableSP = new JScrollPane(table);
		
		dialogPanel = new JPanel( new BorderLayout() ); // bottom panel
		dialogPanel.add( tableSP, "Center" );
		buttons = new JPanel( new GridLayout(0, 1) );

		ButtonGroup gp = new ButtonGroup();
		tabs = new JRadioButton[3];
		tabs[0] = new JRadioButton("Digitized Points");
		tabs[1] = new JRadioButton("Interpolated Points");
		tabs[2] = new JRadioButton("Draw Profile");
		tabs[2].setToolTipText("View grid values along digitized segment.");
		for(int k=0 ; k<3 ; k++) {
			buttons.add( tabs[k] );
			tabs[k].addActionListener( this );
			gp.add( tabs[k] );
		}
		tabs[0].setSelected(true);
		
		autoscaleCB = new JCheckBox("Autoscale");
		autoscaleCB.addActionListener(this);
		//set autoscale to true by default
		autoscaleCB.setSelected(true);
		autoscaleCB.setEnabled(tabs[2].isSelected());
		buttons.add(autoscaleCB);
		
		deletePtsBtn = new JButton("Delete point(s)");
		deletePtsBtn.addActionListener(this);
		deletePtsBtn.setEnabled(false);
		buttons.add(deletePtsBtn);
		
		saveBtn = new JButton("Save");
		saveBtn.addActionListener(this);
		saveBtn.setEnabled(false);
		buttons.add(saveBtn);
		
		dialogPanel.add( buttons, "East" );
	}

	void initTools() {
		tools = new JPanel(new BorderLayout());
		tools.setMinimumSize(new Dimension(200, tools.getMinimumSize().height));
		tools.setPreferredSize(new Dimension(200, tools.getPreferredSize().height));
		JPanel panel = new JPanel(new GridLayout(0,1));

		helpBtn = new JButton("Help");
		helpBtn.addActionListener(this);
		panel.add(helpBtn);
		
		startStopBtn = new JToggleButton("Start digitizing");
		startStopBtn.addActionListener(this);
		startStopBtn.setForeground(new Color(0,128,0));
		panel.add(startStopBtn);
		
		deleteBtn = new JButton("Delete segment(s)");
		deleteBtn.addActionListener(this);
		panel.add(deleteBtn);
		
		insertBtn = new JButton("Insert before selected point");
		insertBtn.setToolTipText("Insert new points before the selected point.");
		insertBtn.addActionListener(this);
		insertBtn.setEnabled(false);
		panel.add(insertBtn);
		
		addBtn = new JButton("Append to segment");
		addBtn.addActionListener(this);
		addBtn.setEnabled(false);
		panel.add(addBtn);
				
		greatCircleRB = new JRadioButton("Great Circle");
		greatCircleRB.setToolTipText("Always draws shortest path.");
		greatCircleRB.addActionListener(this);
		straightLineRB = new JRadioButton("Straight Line");
		straightLineRB.setToolTipText("Always draws between points.");
		straightLineRB.addActionListener(this);
		ButtonGroup linesBG = new ButtonGroup();
		linesBG.add(greatCircleRB);
		linesBG.add(straightLineRB);
		greatCircleRB.setSelected(true);
		panel.add(greatCircleRB);
		panel.add(straightLineRB);
		
		// for survey planner
		speedP = new JPanel();
		speedP.setLayout(new BoxLayout(speedP, BoxLayout.LINE_AXIS));		
		JLabel speedL = new JLabel("Ship speed (knots)");
		speedTF = new JTextField();
		speedTF.setPreferredSize(new Dimension(50,23));
		speedTF.setMaximumSize(new Dimension(50,23));
		speedTF.setText(Double.toString(getSpeed()));
		speedTF.addKeyListener(this);
		speedP.add(speedL);
		speedP.add(speedTF);

		objectClasses = new Class[2];
		objectClasses[0] = null;
		tools.add( panel, "North" );
		tools.add( new JScrollPane( list ), "Center");
		try {
			objectClasses[1] = Class.forName( "haxby.db.dig.LineSegmentsObject" );
		} catch( Exception ex) {
			ex.printStackTrace();
			objectClasses[1] = null;
		}
	}
	public void showObjectDialog( DigitizerObject obj ) {
		JOptionPane.showMessageDialog( map.getTopLevelAncestor(), obj.toString() );
	}
	public void valueChanged(ListSelectionEvent evt) {
		if(objects.size() == 0)return;
		saveBtn.setEnabled(true);
		int[] indices = list.getSelectedIndices();
		for( int i=0 ; i<objects.size(); i++ ) {
			try {
				DigitizerObject obj = (DigitizerObject) objects.get(i);
				obj.setSelected( false );
			} catch( Exception ex) {
			}
		}
		for( int i=0 ; i<indices.length ; i++ ) {
			try {
				DigitizerObject obj = (DigitizerObject) objects.get(indices[i]);
				obj.setSelected( true );
			} catch( Exception ex) {
			}
		}
		// re-click on the selected tab
		for (JRadioButton tab : tabs) {
			if (tab.isSelected()) {
				tab.doClick();
				break;
			}
		}
		map.repaint();
	//	redraw();
	}
	public void redraw() {
		synchronized (map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			draw(g);
		}
	}
	
	public void makeProfile() {
		try {
			LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
			obj.updatePoints();
			obj.getProfile();
			try {
				speed = Double.parseDouble(speedTF.getText());
			} catch(Exception e) {}
			// update the viewport based on the selected radio button
			if(tabs[0].isSelected()) {
				try {
					obj.setTable(0);
					tableSP.setViewportView( table );
					tableSP.revalidate();
				} catch( ClassCastException ex) {
				}
			} else if(tabs[1].isSelected()) {
				try {
					obj.setTable(1);
					tableSP.setViewportView( table );
					tableSP.revalidate();
				} catch( ClassCastException ex) {
				}
			} else if(tabs[2].isSelected()) {
				try {
					profile.setLine( obj );
					graph.setPoints( profile, 0 );
					graph.setScrollableTracksViewportWidth(autoscaleCB.isSelected());		
					tableSP.setViewportView( graph );
					tableSP.revalidate();
				} catch( ClassCastException ex) {
				}
			}
		} catch( ClassCastException ex) {System.out.println(ex.getMessage());}
		return;
	}
	
	public void actionPerformed( ActionEvent evt ) {
		//make sure Digitizer is at the top of the Layer Manager so that segments can be displayed
		moveDigitizerLayerToTop();
		//enable the add button if a segment is selected
		if (list.getSelectedIndices().length > 0) addBtn.setEnabled(true);
		if (evt.getSource() == helpBtn) {
			BrowseURL.browseURL(PathUtil.getPath("HTML/DIGITIZER_HELP"));
		}
		autoscaleCB.setEnabled(tabs[2].isSelected());
		if(evt.getSource()==tabs[0]) {
			try {
				LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
				obj.setTable(0);
				tableSP.setViewportView( table );
				tableSP.revalidate();
			} catch( ClassCastException ex) {
			}
			return;
		} else if(evt.getSource()==tabs[1]) {
			deletePtsBtn.setEnabled(false);
			try {
				LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
				obj.setTable(1);
				tableSP.setViewportView( table );
				tableSP.revalidate();
			} catch( ClassCastException ex) {
			}
			return;
		} else if(evt.getSource()==tabs[2]) {
			deletePtsBtn.setEnabled(false);
			try {
				LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
				profile.setLine( obj );
				if (isSurveyPlanner && !((MapApp)map.getApp()).getMapTools().getGridDialog().isDialogVisible()) {
					((MapApp)map.getApp()).getMapTools().getGridDialog().getToggle().setSelected(true);
					didLoadGMRT = true;
				}
				graph.setPoints( profile, 0 );
				graph.setScrollableTracksViewportWidth(autoscaleCB.isSelected());
				tableSP.setViewportView( graph );
				tableSP.revalidate();
			} catch( ClassCastException ex) {
			}		
			return;
		}
		if(evt.getSource() == startStopBtn) { 
			if (startStopBtn.isSelected()) {
				//make sure zoom and pan buttons are de-selected
				map.getMapTools().selectB.doClick();
				//always default to Digitized Points
				tabs[0].doClick();
				saveBtn.setEnabled(false);
				map.removeMouseListener( this );
				map.removeMouseMotionListener( this );
				map.removeKeyListener( this );
				listening = false;
				try {
					Class[] classes = new Class[] { map.getClass(), getClass() };
					Object[] objects = new Object[] { map, this };
					currentObject = (DigitizerObject) 
						objectClasses[1].getConstructor(classes).newInstance(objects);
					this.objects.add( currentObject );
					currentObject.start();
				} catch(Exception ex ) {
					ex.printStackTrace();
				}
			} else {
				startStopBtn.setText("Start digitizing");
				startStopBtn.setForeground(new Color(0,128,0));
				if( currentObject!=null ) {
					if( !currentObject.finish()) {
						objects.remove(currentObject);
						model.objectRemoved();
					}
				}
				if( !listening ) {
					map.addMouseListener( this );
					map.addMouseMotionListener( this );
					map.addKeyListener( this );
					listening = true;
	
					if (!editing) {
						if (objects.size() > 0) model.objectAdded();
						currentObject.setName(currentObject.toString() + " " + Integer.toString(obj_ind));
						obj_ind++;
					} else {
						editing = false;
						if(null != inserterObject) {
							inserterObject.finish();
							LineSegmentsObject temp = (LineSegmentsObject) inserterObject;
							((LineSegmentsObject)currentObject).insertPoints(lastPointSelected, temp);
							((LineSegmentsObject)currentObject).getProfile();
							map.repaint();
							currentObject.redraw();
							inserterObject = null;
						}
					}
					
					for( int k=0 ; k<objects.size() ; k++ ) {
						((DigitizerObject)objects.get(k)).setSelected(false);
					}
					if( currentObject != null ) {
						list.setSelectedValue(currentObject, true);
						currentObject.setSelected(true);
						currentObject.redraw();
					} else {
						list.setSelectedIndices( new int[] {} );
					}
					saveBtn.setEnabled(true);
				}
				currentObject = null;
			}
		}
		if(evt.getSource() == addBtn) { 
				// similar to Start Digitizing
				startStopBtn.setSelected(true);
				//make sure zoom and pan buttons are de-selected
				map.getMapTools().selectB.doClick();
				//always default to Digitized Points
				tabs[0].doClick();
				saveBtn.setEnabled(false);
				map.removeMouseListener( this );
				map.removeMouseMotionListener( this );
				map.removeKeyListener( this );
				listening = false;
				try {
					// get the current object - need to de-select it to enable the draw functions
					// to work as expected.
					currentObject = (LineSegmentsObject) table.getModel();
					currentObject.setSelected(false);
					currentObject.start();
					editing = true;
				} catch(Exception ex ) {
					ex.printStackTrace();
				}
		}
		if(evt.getSource() == insertBtn) {
			if(table.getSelectedRows().length != 1) {
				JOptionPane.showMessageDialog(MapApp.anchor, "Please select a point from the table or map.", "", JOptionPane.PLAIN_MESSAGE);
			}
			else {
				JOptionPane.showMessageDialog(MapApp.anchor, "The previously digitized points will temporarily disappear from the table while new points are being chosen.", "", JOptionPane.PLAIN_MESSAGE);
				lastPointSelected = table.getSelectedRow();
				// similar to Start Digitizing
				startStopBtn.setSelected(true);
				//make sure zoom and pan buttons are de-selected
				map.getMapTools().selectB.doClick();
				//always default to Digitized Points
				tabs[0].doClick();
				saveBtn.setEnabled(false);
				map.removeMouseListener( this );
				map.removeMouseMotionListener( this );
				map.removeKeyListener( this );
				listening = false;
				try {
					Class[] classes = new Class[] { map.getClass(), getClass() };
					Object[] objects = new Object[] { map, this };
					inserterObject = (DigitizerObject) 
							objectClasses[1].getConstructor(classes).newInstance(objects);
					currentObject = (LineSegmentsObject) table.getModel();
					inserterObject.start();
					editing = true;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		if(evt.getSource() == deleteBtn) {
			//get selected indices from list
			int[] selectedIndices = list.getSelectedIndices();
			//get corresponding objects
			ArrayList<Object> toDelete = new ArrayList<Object>();
			for (int ind : selectedIndices) {
				if (ind != -1 && ind < objects.size())
					toDelete.add(objects.get(ind));
			}
			
			String msg = "Are you sure you wish to delete: ";
			for (Object delObj : toDelete) {
				msg += "\n"+((LineSegmentsObject)delObj).toString();
			}
			msg += "?";
			int n = JOptionPane.showConfirmDialog(map, msg, "Confirm Digitized Segments Deletion", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION) return;

			//delete objects
			for (Object obj : toDelete) {
				objects.remove(obj);
				model.objectRemoved();
			}
			//reset list, table and graph
			list.setModel(model);
			table.setModel(new LineSegmentsObject(map, this));
			LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
			profile.setLine( obj );
			graph.setPoints(profile, 0);
			table.revalidate();
			tableSP.revalidate();
			graph.revalidate();
			graph.repaint();
			map.repaint();
			saveBtn.setEnabled(false);
		}
		else if (evt.getSource() == greatCircleRB || evt.getSource() == straightLineRB) {
			map.repaint();
			makeProfile();
		}
		else if (evt.getSource() == autoscaleCB) {
			graph.setScrollableTracksViewportWidth( autoscaleCB.isSelected() );
			graph.invalidate();
			tableSP.validate();
			tableSP.getViewport().getView().repaint();
		}
		else if (evt.getSource() == saveBtn) {
			save();
		} 
		else if (evt.getSource() == deletePtsBtn) {
			LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
			if (obj == null) return;					
			// get a list of the selected points to be deleted.
			Vector<double[]> itemsToRemove = new Vector<double[]>();
			for (int row : table.getSelectedRows()) {
					itemsToRemove.add((double[])obj.points.get(row));
			}
			// update the displayToDataIndex
			obj.displayToDataIndex.subList(table.getSelectedRows()[0], table.getSelectedRows()[table.getSelectedRows().length-1]+1).clear();
			
			//if deleting the last point, delete line
			if (itemsToRemove.size() == obj.points.size()) {
				deleteBtn.doClick();
				return;
			}
			
			obj.points.removeAll(itemsToRemove);
			// update profile, graph and tables
			obj.getProfile();
			profile.setLine( obj );
			graph.setPoints(profile, 0);
			table.revalidate();
			tableSP.revalidate();
			graph.revalidate();
			graph.repaint();
			map.repaint();
			// make sure the row selection is cleared
			table.clearSelection();
			deletePtsBtn.setEnabled(false);
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
		if (table.getSelectedRows().length == 0) deletePtsBtn.setEnabled(false);
		insertBtn.setEnabled(table.getSelectedRows().length == 1);
		//make sure Digitizer is at the top of the Layer Manager so that segments can be displayed
		moveDigitizerLayerToTop();
		if( evt.getSource()==list ) {
			if( objects.size()==0) return;
			// If clicking on the segments icon, toggle the line visibility,
			if (evt.getX() > 16) return;
			try {
				int i = list.locationToIndex( evt.getPoint() );
				if(i>=0) {
					DigitizerObject obj = (DigitizerObject) objects.get(i);
					obj.setVisible( !obj.isVisible() );
					if( obj.isVisible() ) redraw();
					else map.repaint();
					list.repaint();
				}
			} catch (Exception ex) {
			}
		} else if( evt.getSource()==map ) {
			if( evt.isControlDown() ) return;
			DigitizerObject obj = null;
			Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
			int j = lastSelectedIndex+1;
			boolean selection = false;
			for( int i=0 ; i<objects.size() ; i++,j++ ) {
				j %= objects.size();
				try {
					obj = (DigitizerObject) objects.get(j);
				} catch (Exception ex) {
					continue;
				}
				if( obj.select( p.x, p.y ) ) {
					selection = true;
					break;
				}
			}
			if( selection ) {
				lastSelectedIndex = j;
				try {
					obj = (DigitizerObject) objects.get(j);
				} catch( Exception ex) {
					return;
				}
				if( evt.isShiftDown() ) {
					if( obj.isSelected() ) {
						obj.setSelected( false );
						list.removeSelectionInterval(j, j);
					} else {
						obj.setSelected( true );
						list.addSelectionInterval(j, j);
					}
				} else {
					if( obj.isSelected() && list.getSelectedIndices().length==1 )return;
					list.setSelectedIndices( new int[] {j} );
					obj.setSelected( true );
				}
				makeProfile();
			} else if( evt.isShiftDown() ) {
				return;
			}
		} else if( evt.getSource() == table ) {
			// will mark the location of the selected point on the path
			map.repaint();
			redraw();
			if (tabs[0].isSelected() && table.getSelectedRows().length > 0) {
				deletePtsBtn.setEnabled(true);
			} 
		}
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		insertBtn.setEnabled(table.getSelectedRows().length == 1);
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		//make sure Digitizer is at the top of the Layer Manager so that segments can be displayed
		moveDigitizerLayerToTop();
		if( evt.getSource()==speedTF) {
			makeProfile();
		}
		if( evt.getSource()==map && evt.getKeyCode() == KeyEvent.VK_ENTER ) {
			// Not sure if we want the Select Colors dialog anymore.
			// Comment out for now, maybe bring back later.  NSS 06/12/17
//			int[] indices = list.getSelectedIndices();
//			if(objects.size()==0 || indices.length == 0) return;
//			DigitizerObject[] obj = new DigitizerObject[indices.length];
//			for( int i=0 ; i<obj.length ; i++) obj[i] = (DigitizerObject)objects.get(indices[i]);
//			options.showDialog( obj );
//			map.repaint();
		} else if( evt.getSource()==table && evt.isControlDown() ) {
			if( evt.getKeyCode() == KeyEvent.VK_C ) {
				LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
				String dataType = obj.grid.getDataType();
				String units = obj.grid.getUnits();
				StringBuffer sb = new StringBuffer();
				sb.append("Longitude\tLatitude\t"+ dataType + " (" + units + ")\n");
				int[] rows = table.getSelectedRows();
				for( int k=0 ; k<rows.length ; k++ ) {
					sb.append( table.getValueAt(rows[k],0) +"\t");
					sb.append( table.getValueAt(rows[k],1) +"\t");
					sb.append( table.getValueAt(rows[k],2) +"\n");
				}
				JTextArea text = new JTextArea(sb.toString());
				text.selectAll();
				text.cut();

			} else if( evt.getKeyCode() == evt.VK_A ) {
				table.selectAll();

			}
		}
		if(evt.getSource() == table) {
			map.repaint();
			redraw();
			insertBtn.setEnabled(table.getSelectedRows().length == 1);
			if (tabs[0].isSelected() && table.getSelectedRows().length > 0) {
				deletePtsBtn.setEnabled(true);
			}
		}
		if(evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S ) {
				save();
		}
	
	}
	
	@Override
	public void componentResized(ComponentEvent e) {
		// refresh the table if the zoom is changed (will affect the precision shown)
		synchronized( map.getTreeLock() )  {
			if (e.getSource() == map) {
				for (JRadioButton tab : tabs) {
					if (tab.isSelected()) tab.doClick();
				}
			}
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	void save(){
		JPanel savePanel = new JPanel( new BorderLayout() );
		savePanel.setBorder(BorderFactory.createEmptyBorder( 0, 5, 0, 5));

		JPanel savePrompt = new JPanel(new GridLayout(0, 1));
		JRadioButton saveTableRB = new JRadioButton("Save as ASCII Table");
		JRadioButton saveJPGProfileRB = new JRadioButton("Save Profile as JPG");
		JRadioButton savePNGProfileRB = new JRadioButton("Save Profile as PNG");
		saveJPGProfileRB.setEnabled(tabs[2].isSelected());
		savePNGProfileRB.setEnabled(tabs[2].isSelected());
		ButtonGroup saveGroup = new ButtonGroup();
		saveGroup.add(saveTableRB);
		saveGroup.add(saveJPGProfileRB);
		saveGroup.add(savePNGProfileRB);
		saveTableRB.setSelected(true);
		savePrompt.add(saveTableRB);
		savePrompt.add(saveJPGProfileRB);
		savePrompt.add(savePNGProfileRB);
		savePanel.add(savePrompt, BorderLayout.CENTER);
		
		int s = JOptionPane.showConfirmDialog(map, savePanel, "Save Options", JOptionPane.OK_CANCEL_OPTION);
		if(s == 2) {
			return;
		}
		if (saveTableRB.isSelected()) {
			saveTable();
			return;
		}
		String fmt = "jpg";
		if (savePNGProfileRB.isSelected()) {
			fmt = "png";
		}
		saveProfile(fmt);
	}
	void saveProfile(String fmt) {
		
		JFileChooser chooser = MapApp.getFileChooser();
		String defaultFileName = list.getSelectedValue().toString().replace(" ", "_") + "." + fmt;
		chooser.setSelectedFile(new File(defaultFileName));
		int ok = chooser.showSaveDialog(map.getTopLevelAncestor());
		if( ok==JFileChooser.CANCEL_OPTION ) return;
		File file = chooser.getSelectedFile();
		if( file.exists() ) {
			ok = askOverWrite();
			if( ok==JOptionPane.CANCEL_OPTION ) return;
		}

		//get the image from the graph
		BufferedImage image = graph.getFullImage();
		Graphics g = image.getGraphics();
		graph.paint(g);

		try {
			String name = file.getName();
			int sIndex = name.lastIndexOf(".");
			String suffix = sIndex<0
				? fmt
				: name.substring( sIndex+1 );
			if( !ImageIO.getImageWritersBySuffix(suffix).hasNext())suffix = fmt;

			if ( chooser.getSelectedFile().getPath().endsWith(fmt) ) {
				ImageIO.write(image, suffix, file);
			}
			else {
				ImageIO.write(image, suffix, new File(file.getPath() + fmt) );
			}

		} catch(IOException e) {
			JOptionPane.showMessageDialog(MapApp.anchor,
					" Save failed: "+e.getMessage(),
					" Save failed",
					 JOptionPane.ERROR_MESSAGE);
		}
	}
	
	int askOverWrite() {
		JFileChooser chooser = MapApp.getFileChooser();
		int ok = JOptionPane.NO_OPTION;
		while( true ) {
			ok = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(),
				"File exists. Overwrite?",
				"Overwrite?",
				JOptionPane.YES_NO_CANCEL_OPTION);
			if( ok!=JOptionPane.NO_OPTION) return ok;
			ok = chooser.showSaveDialog(map.getTopLevelAncestor());
			if( ok==JFileChooser.CANCEL_OPTION ) return JOptionPane.CANCEL_OPTION;
			if( !chooser.getSelectedFile().exists() ) return JOptionPane.YES_OPTION;
		}
	}
	
	void saveTable() {
		try {
			LineSegmentsObject obj = (LineSegmentsObject) table.getModel();
			JFileChooser chooser = MapApp.getFileChooser();
			String defaultFileName = list.getSelectedValue().toString().replace(" ", "_") + ".xyz";
			chooser.setSelectedFile(new File(defaultFileName));
			int ok = chooser.showSaveDialog(map.getTopLevelAncestor());
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			File file = chooser.getSelectedFile();
			if( file.exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			
			PrintStream out = new PrintStream(
					new FileOutputStream( file ));

			//add disclaimer at the top of the output file
			out.println("#NOT TO BE USED FOR NAVIGATION PURPOSES");
			
			String header = "";
			for (int col = 0; col < obj.getColumnCount() - 1; col++) {
				header += obj.getColumnName(col) + "\t";
			}
			header += obj.getColumnName(obj.getColumnCount()-1);
			out.println(header);
			
			//first print the digitized points 
			out.println("Digitized points");
			for (int row = 0; row < obj.points.size(); row ++) {
				String line = "";
				for (int col = 0; col < obj.getColumnCount() - 1; col++) {
					line += obj.getValueAt(row, col, 0, true).toString().replace(",", "") + "\t";
				}
				line += obj.getValueAt(row, obj.getColumnCount()-1, 0, true).toString().replace(",", "");
				out.println(line);
			}
			
			//then print the interpolated points
			out.println("Interpolated points");
			for (int row = 0; row < obj.profile.size(); row ++) {
				String line = "";
				for (int col = 0; col < obj.getColumnCount() - 1; col++) {
					line += obj.getValueAt(row, col, 1, true).toString().replace(",", "") + "\t";
				}
				line += obj.getValueAt(row, obj.getColumnCount()-1, 1, true).toString().replace(",", "");
				out.println(line);
			}

			out.close();
		} catch(IOException ex) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(),
					"\"save\" failed\n   "+ ex.getMessage() );
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
// methods implementing Database
	public void draw( Graphics2D g ) {
		for( int i=0 ; i<objects.size() ; i++ ) {
			((DigitizerObject)objects.get(i)).draw(g);
		}
	}
	public String getDBName() {
		return "Digitizer";
	}
	
	public String getCommand() {
		return "Digitizer";
	}

	public String getDescription() {
		return getDBName();
	}
	public boolean loadDB() {
		return true;
	}
	public boolean isLoaded() {
		return true;
	}
	public void unloadDB() {
	}
	public void disposeDB() {
		setEnabled( false );
	}
	public void setEnabled( boolean tf ) {
		if( tf==enabled ) return;
		if( enabled ) {
			if( currentObject!=null && !currentObject.finish() ) {
				objects.remove(currentObject);
				model.objectRemoved();
			}
		}
		enabled=tf;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public JComponent getSelectionDialog() {
		return tools;
	}
	public JComponent getDataDisplay() {
		return dialogPanel;
	}
	public void finish() {
		startStopBtn.doClick();
	}

	private void moveDigitizerLayerToTop() {
		if (((MapApp)map.getApp()).getCurrentDB() == this) {
			((MapApp)map.getApp()).layerManager.moveToTop(this);
		}
	}
	
	public void setLoadedGMRTForDig (boolean tf) {
		loadedGMRTForDig = tf;
	}
	
	public boolean getLoadedGMRTForDig() {
		return loadedGMRTForDig;	
	}
	
	public boolean isStraightLine() {
		return straightLineRB.isSelected();
	}
	
	public void setSurveyPlanner (boolean tf) {
		isSurveyPlanner = tf;
	}
	
	public boolean isSurveyPlanner() {
		return isSurveyPlanner;
	}
	
	public void setSpeed(double s) {
		speed = s;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public double calculateDuration(int cumulativeDistance) {
		if (speed == 0) return 0;
		double duration = cumulativeDistance / (speed * GeneralUtils.KNOTS_2_KPH);
		return (double)Math.round(duration * 100d) / 100d;
	}
	
	static ImageIcon SEGMENTS(boolean selected) {
		int[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,0,0,0,0,1,1,0,0,0},
			{0,0,0,0,0,0,1,0,0,1,1,0,1,1,0,0},
			{0,0,0,0,0,0,0,1,1,0,0,0,1,1,1,0},
			{0,0,0,0,0,0,1,1,0,0,0,0,1,1,1,1},
			{0,0,0,0,1,1,0,0,1,0,0,0,1,1,1,0},
			{1,1,1,1,0,0,0,0,1,0,0,0,1,0,1,0},
			{1,0,1,0,0,0,0,0,0,1,0,0,0,0,1,0},
			{1,1,1,1,0,0,0,0,0,1,0,0,0,0,0,1},
			{0,0,0,0,1,1,0,0,0,0,1,0,0,0,0,1},
			{0,0,0,0,0,0,1,1,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,1,1,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0}
		};
		return doIcon(map, selected);
	}
	static int black = Color.black.getRGB();
	static int white = (new Color(240,240,240)).getRGB();
	static ImageIcon doIcon(int[][] map, boolean selected) {
		BufferedImage im;
		im = new BufferedImage(map[0].length+4, map.length+4,
				BufferedImage.TYPE_INT_RGB);
		int color = white;
		if(selected) color = 0xffc0c0c0;
		for(int y=0 ; y<map[0].length+4 ; y++) {
			for( int x=0 ; x<map.length+4 ; x++) im.setRGB(x, y, color);
		}
		for(int y=2 ; y<map[0].length+2 ; y++) {
			for( int x=2 ; x<map.length+2 ; x++) {
				if(map[y-2][x-2] == 1) im.setRGB(x, y, black);
				else im.setRGB(x, y, color);
			}
		}
		return new ImageIcon(im);
	}
	static javax.swing.border.Border border = BorderFactory.createLineBorder(Color.black);
//	static javax.swing.border.Border borderSel = BorderFactory.createLoweredBevelBorder();
	
	/**
	 * Handles drag & drop row reordering
	 * based on code from https://stackoverflow.com/questions/638807/how-do-i-drag-and-drop-a-row-in-a-jtable
	 */
	public class TableRowTransferHandler extends TransferHandler {

		private static final long serialVersionUID = -3738670301072037773L;
		private final DataFlavor localObjectFlavor = new ActivationDataFlavor(Integer.class, "application/x-java-Integer;class=java.lang.Integer", "Integer Row Index");
		   private JTable           table             = null;

		   public TableRowTransferHandler(JTable table) {
		      this.table = table;
		   }

		   @Override
		   protected Transferable createTransferable(JComponent c) {
		      assert (c == table);
		      table.addColumnSelectionInterval(0, table.getColumnCount()-1);
		      return new DataHandler(new Integer(table.getSelectedRow()), localObjectFlavor.getMimeType());
		   }

		   @Override
		   public boolean canImport(TransferHandler.TransferSupport info) {
			  if (!tabs[0].isSelected()) return false;
		      boolean b = info.getComponent() == table && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
		      table.setCursor(b ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
		      return b;
		   }

		   @Override
		   public int getSourceActions(JComponent c) {
		      return TransferHandler.COPY_OR_MOVE;
		   }

		   @Override
		   public boolean importData(TransferHandler.TransferSupport info) {
		      JTable target = (JTable) info.getComponent();
		      JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
		      int index = dl.getRow();
		      int numSelected = table.getSelectedRowCount();
		      int max = table.getModel().getRowCount();
		      if (index + target.getSelectedRowCount() > max) return false;
		      if (index < 0 || index > max)
		         index = max;
		      target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		      try {		    	  
		         Integer rowFrom = (Integer) info.getTransferable().getTransferData(localObjectFlavor);
		         if (rowFrom != -1 && rowFrom != index) {
		            ((LineSegmentsObject)table.getModel()).reorder(rowFrom, index, numSelected);
		         }
		         //update the selected rows
		         target.clearSelection();
		         int index1 = index + numSelected - 1;
		         target.addRowSelectionInterval(index, index1);

		         return true; 
		      } catch (Exception e) {
		         e.printStackTrace();
		      }
		      return false;
		   }

		   @Override
		   protected void exportDone(JComponent c, Transferable t, int act) {
		      if ((act == TransferHandler.MOVE) || (act == TransferHandler.NONE)) {
		         table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		      }
		   }

		}
}
