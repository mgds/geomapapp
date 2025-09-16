package haxby.worldwind.db.pdb;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.WWIcon;
import haxby.db.Database;
import haxby.db.XYGraph;
import haxby.db.pdb.PDB;
import haxby.db.pdb.PDBDataType;
import haxby.db.pdb.PDBExpedition;
import haxby.db.pdb.PDBGraphDialog;
import haxby.db.pdb.PDBLocation;
import haxby.db.pdb.PDBSample;
import haxby.db.pdb.PDBSelectionDialog;
import haxby.db.pdb.PDBStation;
import haxby.db.pdb.SendToPetDB;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.IdentityProjection;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.awt.LassoSelectionHandler;
import haxby.worldwind.awt.LassoSelectionHandler.LassoSelectListener;
import haxby.worldwind.db.mgg.WWMGG;
import haxby.worldwind.layers.LayerSet;
import haxby.worldwind.layers.WWSceneGraph;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.layers.WWSceneGraph.SceneItemIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;

import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;

public class WWPDB extends PDB implements WWOverlay,
										SelectListener,
										LassoSelectListener {

	private static final String ICON_PATH = "org/geomapapp/resources/icons/wdot.png";
	private static final int ICON_SIZE = 12;
	
	public DetailedIcon[] icons;
	protected WWLayer layer;
	protected WWSceneGraph wwScenceGraph;
	protected DetailedIconRenderer iconRenderer = new DetailedIconRenderer();
	
	protected LassoSelectionHandler lassoSelectionHandler;
	
	public WWPDB(XMap map) {
		super(map);
	}
	
	@Override
	public void setEnabled(boolean tf) {
		super.setEnabled(tf);
		if (layer != null) 
			layer.setEnabled(tf);
	}
	
	public Layer getLayer() {
		if (wwScenceGraph == null) {
			wwScenceGraph = new WWSceneGraph();
			wwScenceGraph.setName(getDBName());
			loadLayer();
		}
		if (layer == null)
		{
			layer = new WWLayer(wwScenceGraph) {
				public void close() {
					((MapApp)map.getApp()).closeDB(WWPDB.this);
				}
				public Database getDB() {
					return WWPDB.this;
				}
			};
		}
		
		return layer;
	}

	public SelectListener getSelectListener() {
		return this;
	}

	public void setArea(final Rectangle2D bounds) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						model.setArea(bounds, 512);
						
						if (cst == null || !cst.isShowing())
							processVisibility();
				
					}
				});
	}

	public void selected(SelectEvent event) {
		if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
			Object topObject = event.getTopObject();
			int index = 0;
			for (DetailedIcon icon : icons) {
				if (icon == topObject)
					break;
				index++;
					
			}
			
			if (index == icons.length)
				return;
			
			final int i = index;
			new Thread () {
				public void run() {
					int j = 0;
					for (int index : model.current) {
						if (index == i) {
							table.getSelectionModel().setSelectionInterval(j, j);
							table.ensureIndexIsVisible(j);
							break;
						} else
							j++;
					}
				}
			}.start();
		}
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if (!loaded) return;
		
		processVisibility();
		layer.firePropertyChange(AVKey.LAYER, null, layer);
		
		// Let our Graphs know we've changed colors
		for (Object obj : graphs) {
			XYGraph graph = (XYGraph) obj;
			graph.repaint();
		}
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		
		super.valueChanged(e);
		
		for (WWIcon icon : icons) {
			if (icon == null) continue;
			icon.setHighlighted( false );
		}
		
		int[] selRows = table.getSelectedRows();
		
		for (int row : selRows) {
			int index = model.current[ row ];
			icons[index].setHighlighted(true);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	public void disposeDB() {
		super.disposeDB();
		for (DetailedIcon icon : icons) {
			if (icon == null) continue;
			
			wwScenceGraph.disposeSubLayer(0);
		}
		
		icons = null;
		wwScenceGraph = null;
		layer.dispose();
		layer = null;
	}
	
	public boolean loadDB() {
		if(loaded) return true;
		try {
	// load Expeditions
			PDBExpedition.load();
	// load Locations
			Dimension mapDim = map.getDefaultSize();
			Rectangle mapBounds = new Rectangle(0, 0,
					mapDim.width, mapDim.height);
			PDBLocation.load();
			Projection proj = map.getProjection();
			for(int i=0 ; i<PDBLocation.size() ; i++) {
				PDBLocation loc = PDBLocation.get(i);
				if( loc != null ) {
					loc.project(proj);
				}
			}
			PDBDataType dt = new PDBDataType();
	// load Stations
			PDBStation.load();
			PDBSample.load();
		} catch (IOException ex) {
			loaded = false;
			return false;
		}
		initTable();
		selectedIndices = new int[0];
	//	dialog = new PDBSelectionDialog( this );
		dialog = new JPanel( new BorderLayout() );
		JPanel p = new JPanel(new GridLayout(0,1));
		
		p.add(createLassoPanel());
		
		JPanel p2 = new JPanel(new GridLayout(1,0));
		JButton graph = new JButton("Graph Data");
		final PDB pdb = this; 
		graph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new PDBGraphDialog((JFrame)map.getTopLevelAncestor(),pdb);
			}
		});
		p2.add(graph);
		
		JButton color = new JButton("Color Data");
		color.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WWPDB.this.color();
			}
		});
		p2.add(color);
		p.add(p2);
		
		// Save Combo Box
		save = new JComboBox(saveOptions);
		save.setSelectedIndex(0);
		save.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (save.getSelectedIndex()) {
				case 1: // Copy
					copyToClipboard(getSelectionData()); 
					break;
				case 2: // Save ASCII
					saveAsASCII("all");
					break;
				case 3: // Save xls
					saveAsExcel("all");
					break;
				case 4: // Save Selected ASCII
					saveAsASCII("selection");
					break;
				case 5: // Save Selected Excel
					saveAsExcel("all");
					break;
				default:
					break;
				}
				save.setSelectedIndex(0);
			}
		});
		p.add(save);
		
		dataDisplay = new JTabbedPane(JTabbedPane.TOP);
		JScrollPane sp1 = new JScrollPane(getTable());
		dataDisplay.add("Stations", sp1);
		JScrollPane sp2 = new JScrollPane(getCompiledTable());
		dataDisplay.add("Compiled Chem", sp2);
		JScrollPane sp3 = new JScrollPane(getAnalysisTable());
		dataDisplay.add("Analyses", sp3);
		
		dialog.add( p, "North" );
		dialog.add( new PDBSelectionDialog( this ), "Center");
		dialog.add( new SendToPetDB(dataDisplay), "South");
		
		model.setArea(new Rectangle2D.Double(-180,-90,360,180), 1);
		loadLayer();
		
		loaded = true;
		return true;
	}
	
	protected JPanel createLassoPanel()
	{
		// Lasso Button
		JPanel p = new JPanel(new BorderLayout());
		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		tb.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
//		tb.setSelected(true);
		tb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if (((AbstractButton)e.getSource()).isSelected()) {
					lassoTB.setSelected(true);
					map.setBaseCursor(Cursors.getCursor(Cursors.LASSO));
				} else
					map.setBaseCursor(Cursor.getDefaultCursor());
					
			}
		});
		tb.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
		p.add(tb, BorderLayout.WEST);
		p.setBorder(null);
		JLabel l = new JLabel("Lasso Tool");
		l.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		p.add(l);
		
		lassoTB = tb;
		lassoTB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateLassoState();
			}
		});
		return p;
	}
	
	protected void updateLassoState()
	{
		if (lassoSelectionHandler != null)
			if (!isEnabled())
				lassoSelectionHandler.setLassoEnabled(false);
			else
				lassoSelectionHandler.setLassoEnabled(lassoTB.isSelected());
	}
	
	protected synchronized void processVisibility() {
		boolean[] visible = new boolean[icons.length]; 
		
		for (int index : model.toPlot) {
			Color c = computeStationColor(index);
			if (c == null)
				continue;
			else
				icons[index].setIconColor(c);
			
			visible[index] = true;
		}
		
		int index = 0;
		for (WWIcon icon : icons) {
			if (icon != null)
				icon.setVisible( visible[index] );
			index++;
		}
	}
	
	protected synchronized void loadLayer() {
		if (wwScenceGraph == null) return;
		if (!loaded) return;
		if (icons != null) return;
		
		icons = new DetailedIcon[PDBStation.stations.length];
		
		for (int index = 0; index < PDBStation.stations.length; index++) {
			PDBStation station = PDBStation.stations[ index ];
			if (station == null) continue;
			
			double stationX = station.getX();
			double stationY = station.getY();
			
			if (Double.isNaN(stationX)) continue;
			if (Double.isNaN(stationY)) continue;
			
			DetailedIcon icon = new DetailedIcon(ICON_PATH, Position.fromDegrees(stationY, stationX, 0));
			icon.setIconColor( Color.WHITE);
			icon.setSize(new Dimension(ICON_SIZE, ICON_SIZE));
			icon.setHighlightScale(2);
			icon.setVisible(false);
			
			icons[index] = icon;
			wwScenceGraph.addItem(
					new SceneItemIcon(icon, iconRenderer) );
		}
		
		processVisibility();
	}
	
	public void repaintMap() {
		setArea(map.getVisibleRect());
		
		if (cst == null || !cst.isShowing())
			processVisibility();
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	 public void tableChanged(TableModelEvent e) {
		if (e.getFirstRow() != -1) return;
		
		if (cst == null || !cst.isShowing())
			return;
		
		synchronized (this) {
			if (colorFocusTime == -1) {
				colorFocusTime = System.currentTimeMillis() + 1000;
				new Thread("PDBColor Focus Thread") {
					public void run() {
						while (System.currentTimeMillis() < colorFocusTime)
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
							}
						float[] grid = new float[colorTable.getRowCount()]; 
						
						int z = 0;
						for (int i = 0; i < grid.length; i++)
							try {
								grid[i] = Float.parseFloat(colorTable.getValueAt(i, colorColumnIndex).toString());
								z++;
							} catch (Exception ex) {
								grid[i] = Float.NaN;
							}
						if (z == 0) return;

						synchronized (this) {
							colorGrid = grid;
							cst.setGrid(colorGrid);
						}
						
						processVisibility();
						layer.firePropertyChange(AVKey.LAYER, null, layer);
						
						// Let our Graphs know we've changed colors
						for (Object obj : graphs) {
							XYGraph graph = (XYGraph) obj;
							graph.repaint();
						}
						
						colorFocusTime = -1;
					}
				}.start();
			} else
				colorFocusTime = System.currentTimeMillis() + 1000;
		}
	}
	 
	 public void selectLasso(List<Position> area) {
		 if (!isEnabled()) return;
		 
		 GeneralPath cylindricalPath = new GeneralPath();
		 GeneralPath polarPath = new GeneralPath();

		 Iterator<Position> iter = area.iterator();
		 Position pos = iter.next();

		 boolean northPole = pos.getLatitude().degrees > 0;
		 boolean crossedDateLine = false;
		 boolean positive = false;
		 Projection polarProj;
		 if (northPole)
			 polarProj = new PolarStereo( new Point(320, 320),
					 0., 25600., 71., PolarStereo.NORTH, PolarStereo.WGS84);
		 else
			 polarProj = new PolarStereo( new Point(320, 320),
					 180., 25600., -71., PolarStereo.SOUTH, PolarStereo.WGS84);
		 
		 cylindricalPath.moveTo((float) pos.getLongitude().degrees, 
				 (float) pos.getLatitude().degrees);

		 Point2D pnt = polarProj.getMapXY(pos.getLongitude().degrees, 
					pos.getLatitude().degrees);
		 polarPath.moveTo((float) pnt.getX(), 
				 (float) pnt.getY());
		 
		 double lastX = pos.getLongitude().degrees;
		 
		 while (iter.hasNext()) {
			 pos = iter.next();

			 double x = pos.getLongitude().degrees;
			 double dif = Math.abs(x - lastX);

			 // Crossed the date line, make it better
			 if (dif > 180)
			 {
				 crossedDateLine = true;

				 if (lastX > x){
					 positive = true;
					 x += 360;
				 }
				 else {
					 positive = false;
					 x -= 360;
				 }
			 }

			 cylindricalPath.lineTo((float) x, 
					 (float) pos.getLatitude().degrees);

			 lastX = x;
			 
			 pnt = polarProj.getMapXY(pos.getLongitude().degrees, 
						pos.getLatitude().degrees);
			 polarPath.lineTo((float) pnt.getX(),(float) pnt.getY());
		 }

		 cylindricalPath.closePath();
		 polarPath.closePath();
		 
		 if (northPole)
			 pnt = polarProj.getMapXY(0,90);
		 else
			 pnt = polarProj.getMapXY(0,-90);

		 GeneralPath lassoPath;
		 Projection lassoProj;

		 boolean isPolar = polarPath.contains(pnt) ;

		 // We've circled a pole deal with it
		 if (isPolar)
		 {
			 lassoPath = polarPath;
			 lassoProj = polarProj;
		 }
		 else
		 {
			 lassoPath = cylindricalPath;
			 lassoProj = new IdentityProjection();
		 }

		 Rectangle2D r = lassoPath.getBounds();
		 
		 table.getSelectionModel().setValueIsAdjusting(true);
		 table.clearSelection();

		 for( int k=0 ; k<model.current.length ; k++) {
			 PDBStation stat = PDBStation.get(model.current[k]); 

			 pnt = lassoProj.getMapXY(stat.getX(), stat.getY());
			 
			 double x = pnt.getX();
			 double y = pnt.getY();

			 if (!isPolar)
				 while (x > 180) x -= 360;
			 
			 if (r.contains(x, y) && lassoPath.contains(x, y))
				 table.addRowSelectionInterval(k, k);
			 else if (!isPolar && crossedDateLine)
			 {
				 if (positive)
					 x += 360;
				 else
					 x -= 360;

				 if (r.contains(x, y) && lassoPath.contains(x, y))
					 table.getSelectionModel().addSelectionInterval(k, k);
			 }
		 }
		 table.getSelectionModel().setValueIsAdjusting(false);

		 int selected = table.getSelectedRow();
		 if (selected != -1)
			 table.ensureIndexIsVisible(selected);

		 table.getRowHeader().setSelectedIndices(table.getSelectedRows());
	 }

	 public void setLassoSelectionHandler(
			 LassoSelectionHandler lassoSelectionHandler) {
		 this.lassoSelectionHandler = lassoSelectionHandler;
	 }
}