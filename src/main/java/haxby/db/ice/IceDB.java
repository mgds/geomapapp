package haxby.db.ice;

import haxby.db.Database;
import haxby.db.XYGraph;
import haxby.db.XYPoints;
import haxby.map.XMap;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class IceDB extends MouseAdapter 
			implements Database,
				XYPoints,
				ActionListener,
				ListSelectionListener {
	XMap map;
	Vector expeditions;
	boolean loaded;
	boolean enabled;
	int selCore;
	int selExpedition;
	JLabel label;
	JList expList;
	Box dialog;
	JPanel panel, bottomPanel;;
	JRadioButton before, after;
	JTextField startF, endF;
	JCheckBox includeWater, includeIce;
	JCheckBox discrete;
	JTextField interval;
	JTextField snow, heat, melt;
	JTree tree;
	JRadioButton obsB, gridB;
	D18oObservations obs = null;
	float[] range;
	XYGraph graph;
	XYGraph profile;
	JFrame xyFrame;
	JFrame profFrame;
	IceGrid grid;
	D18OGrid dgrid = null;
	public IceDB(XMap map) {
		this.map = map;
		grid = new IceGrid(map, this);
		dgrid = new D18OGrid(map, this);
		expeditions = new Vector();
		loaded = false;
		enabled = false;
		selCore = -1;
		selExpedition = 0;
		dialog = Box.createVerticalBox();

		JPanel pan = new JPanel( new GridLayout(0,1) );
		JLabel lab = new JLabel("K-snow");
		lab.setForeground( Color.black );
		pan.add(lab);
		snow = new JTextField(".25");
		pan.add(snow);

	//	lab = new JLabel("heat flux");
	//	lab.setForeground( Color.black );
	//	pan.add(lab);
	//	heat = new JTextField("2.");
	//	pan.add(heat);

		lab = new JLabel("melt rate-m/day");
		lab.setForeground( Color.black );
		pan.add(lab);
		melt = new JTextField(".005");
		pan.add(melt);

		JButton button = new JButton("compute");
		pan.add(button);
		button.addActionListener(this);

		button = new JButton("grid");
		pan.add(button);
		button.addActionListener(this);

		bottomPanel = new JPanel(new FlowLayout());
	//	bottomPanel.add(new JLabel("start"));
	//	ButtonGroup when = new ButtonGroup();
		before = new JRadioButton("from", false);
		bottomPanel.add( before );
	//	when.add( before );
		startF = new JTextField("01/1980");
		bottomPanel.add( startF );
		after = new JRadioButton("to");
		bottomPanel.add( after );
		endF = new JTextField("01/2005");
		bottomPanel.add( endF );
	//	when.add( after );
	//	bottomPanel.add( new JLabel(" 1/1/"));

		includeWater = new JCheckBox("water", true);
		bottomPanel.add( includeWater );

		includeIce = new JCheckBox("ice", true);
		bottomPanel.add( includeIce );

		discrete = new JCheckBox("discrete", false);
		bottomPanel.add( discrete );
		interval = new JTextField(".5", 3);
		bottomPanel.add( interval );

		JButton saveGrid = new JButton("save");
		saveGrid.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				grid.saveGrid();
			}
		});
		bottomPanel.add(saveGrid);
		bottomPanel.add( grid.savedGrids );

		ActionListener doGrid = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				grid();
			}
		};
		discrete.addActionListener(doGrid);
		interval.addActionListener(doGrid);
		startF.addActionListener(doGrid);
		before.addActionListener(doGrid);
		after.addActionListener(doGrid);
		includeWater.addActionListener(doGrid);
		includeIce.addActionListener(doGrid);

		label = new JLabel("no points");
		label.setForeground( Color.black );
		dialog.add(new IceColor());
		panel = new JPanel( new BorderLayout() );
		panel.add( dialog, "North");
		panel.add(pan,"South");
		obsB = new JRadioButton("control");
		obsB.addActionListener(this);
		gridB = new JRadioButton("plot grid");
		gridB.addActionListener(this);
		profile = null;
	}
	void grid() {
		grid.grid();
		map.repaint();
	}
	public void draw(Graphics2D g) {
	//	for(int i=0 ; i<expeditions.size() ; i++) {
	//		((IceExpedition)expeditions.get(i)).draw( g );
	//	}

		if( gridB.isSelected() ) {
			grid.draw(g);
		} else { 
			dgrid.draw(g);
		}
		if( obsB.isSelected() )obs.draw( g );
		int[] indices = expList.getSelectedIndices();
		for( int k=0 ; k<indices.length ; k++) {
			int i=indices[k];
			((IceExpedition)expeditions.get(i)).drawTrack( g );
		}
		if( selCore != -1 ) {
			IceExpedition ex = (IceExpedition)expeditions.get(selExpedition);
		//	ex.cores[selCore].drawTrack(g);
			ex.cores[selCore].drawTrack(g);
		}
		for( int k=0 ; k<indices.length ; k++) {
			int i=indices[k];
			((IceExpedition)expeditions.get(i)).draw( g );
		}
		if( selCore != -1 ) {
			IceExpedition ex = (IceExpedition)expeditions.get(selExpedition);
		//	ex.cores[selCore].drawTrack(g);
			ex.cores[selCore].draw(g);
		}
		AffineTransform at = g.getTransform();
				String year = startF.getText();
		String dateString = "";
		if( before.isSelected() ) {
			if( after.isSelected() ) dateString = "from "+startF.getText() +" to "+ endF.getText();
			else dateString = "after "+ startF.getText();
		} else if( after.isSelected() ) dateString = "before "+endF.getText();

		float size1 = 2.0f + 7f/(float)map.getZoom();
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont( (new Font("Serif", Font.BOLD, 1)).deriveFont( size1*1.5f));
		double s = (double) size1;
		Rectangle2D rect = map.getClipRect2D();
		double x = rect.getX() + s;
		double y = rect.getY() + 2.*s;
		g.translate( x, y );
		g.translate( s, s/2.);
		g.setColor( Color.black );
		g.drawString( dateString, 0, 0);
		g.setTransform(at);
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getActionCommand().equals("compute") ) {
			try {
				double ksnow = Double.parseDouble(snow.getText());
			//	double flux = Double.parseDouble(heat.getText());
				double flux = -1.;
				double rate = Double.parseDouble(melt.getText());
				for( int k=0 ; k<expeditions.size() ; k++) {
					IceExpedition e = (IceExpedition)expeditions.get(k);
					for( int i=0 ; i<e.cores.length ; i++) {
						e.cores[i].computeGrowth( flux, ksnow, rate );
					}
				}
			} catch( NumberFormatException ex) {
			}
		} else if( evt.getActionCommand().equals("grid") ) {
			grid.grid();
		}
		map.repaint();
		graph.repaint();
		xyFrame.toFront();
		if( selCore != -1 ) {
			profile.repaint();
			profFrame.toFront();
		}
	}
	public String toString() {
		return "Ice DB";
	}
	public void valueChanged( ListSelectionEvent e) {
		if( selCore!=-1 ) {
			int[] indices = expList.getSelectedIndices();
			boolean showing = false;
			for( int k=0 ; k<indices.length ; k++) {
				if( indices[k]==selExpedition ) {
					showing = true;
					break;
				}
			}
			if( !showing ) {
				IceCore core = ((IceExpedition)expeditions.get( selExpedition )).cores[selCore];
				core.setHighlight(false);
				selCore=-1;
			}
		}
		map.repaint();
		graph.repaint();
		xyFrame.toFront();
	}
	public String getDBName() {
		return "d18o analysis";
	}

	public String getCommand() {
		return "d18o analysis";
	}

	public String getDescription() {
		return "d18o analysis";
	}
	public boolean loadDB() {
		if( loaded ) return true;
		if(obs==null)obs = new D18oObservations(this, map);
		dgrid.grid();
		range = new float[] {10f, -10f};
		try {
			URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/icedb");
			DataInputStream in = new DataInputStream(
				new BufferedInputStream(url.openStream()));
			String name = "";
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(this);
			while( true ) {
				try {
					name = in.readUTF();
				} catch(EOFException ex) {
					loaded = true;
					dialog.add( obsB );
					dialog.add( gridB );
					expList = new JList( expeditions );
					expList.addListSelectionListener( this);
					expList.setVisibleRowCount( expeditions.size() );
					expList.setFont( new Font("SansSerif", Font.PLAIN, 10));
					dialog.add( new JScrollPane(expList) );
					tree = new JTree( root );
					tree.setCellRenderer( new IceCellRenderer() );
					tree.setVisibleRowCount( 8 );
					tree.setFont( new Font("SansSerif", Font.PLAIN, 10));
					tree.setSelectionModel( null );
					tree.addMouseListener(this);
					dialog.add( new JScrollPane(tree) );
					profFrame = new JFrame("Ice Profile");
					profFrame.pack();
					profFrame.setSize(400, 600);
					profFrame.setVisible(true);
					profFrame.toBack();
					profFrame.setDefaultCloseOperation(xyFrame.DO_NOTHING_ON_CLOSE);
					xyFrame = new JFrame("XY");
					graph = new XYGraph(this, 0);
					xyFrame.getContentPane().add( graph, "Center" );
					xyFrame.pack();
				//	xyFrame.setSize(400, 400);
					xyFrame.show();
					xyFrame.setDefaultCloseOperation(xyFrame.DO_NOTHING_ON_CLOSE);
					return true;
				}
				int nCore = in.readInt();
				IceCore[] cores = new IceCore[nCore];
				Vector cv = new Vector();
				for( int k=0 ; k<nCore ; k++) {
					String coreName = in.readUTF();
					int[] date = new int[3];
					for(int i=0 ; i<3 ; i++) date[i] = in.readInt();
					int nVal = in.readInt();
					float[][] d18o = new float[nVal][4];
					for(int i=0 ; i<nVal ; i++) {
						for(int j=0 ; j<4 ; j++) {
							d18o[i][j] = in.readFloat();
							if( d18o[i][j]>range[1] )range[1] = d18o[i][j];
							if( d18o[i][j]<range[0] )range[0] = d18o[i][j];
						}
					}
					int nPoint = in.readInt();
					float[][] track = new float[nPoint][3];
					for(int i=0 ; i<nPoint ; i++) {
						for(int j=0 ; j<3 ; j++) {
							track[i][j] = in.readFloat();
						}
					}
					cv.add( new IceCore(this, map, coreName, date, d18o, track) );
				//	cores[k] = new IceCore(map, coreName, date, d18o, track);
				//	cores[k].computeGrowth( 2, .33, .005 );
				}
				Collections.sort( cv );
				for( int k=0 ; k<nCore ; k++) {
					cores[k] = (IceCore)cv.get(k);
				}
				IceExpedition expedition = new IceExpedition(name, cores);
				expeditions.add( expedition );
				DefaultMutableTreeNode node = new DefaultMutableTreeNode( expedition );
				root.add(node);
				for( int k=0 ; k<nCore ; k++) {
					node.add( new DefaultMutableTreeNode( expedition.cores[k], false) );
				}
			}
		} catch (IOException ex) {
			return false;
		}
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
	}
	public void disposeDB() {
		loaded = false;
		xyFrame.dispose();
		graph = null;
		expList.removeListSelectionListener( this );
		Container c = expList.getParent();
		while( c!=null && !(c instanceof JScrollPane) ) c = c.getParent();
		if(c!=null) dialog.remove( c );
		c = tree.getParent();
		while( c!=null && !(c instanceof JScrollPane) ) c = c.getParent();
		if(c!=null) dialog.remove( c );
		dialog.remove( obsB );
		expeditions = new Vector();
	}
	public void setEnabled( boolean tf ) {
		enabled = tf;
		map.removeMouseListener(this);
		if( tf ) {
			map.addMouseListener( this );
		}
	}
	public void treeSelect( MouseEvent evt ) {
		try {
			TreePath tp = tree.getPathForLocation( evt.getX(), evt.getY() );
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tp.getLastPathComponent();
			IceCore core=null;
			core = (IceCore) node.getUserObject();
			node = (DefaultMutableTreeNode)tp.getParentPath().getLastPathComponent();
			IceExpedition e = (IceExpedition)node.getUserObject();
			Object[] selection = expList.getSelectedValues();
			boolean ok = false;
			for( int i=0 ; i<selection.length ; i++) {
				if( e==selection[i] ) {
					ok = true;
					break;
				}
			}
			if( !ok ) return;
			if(selCore!=-1) {
				IceCore cor = ((IceExpedition)expeditions.get( selExpedition)).cores[selCore];
				cor.setHighlight(false);
				synchronized (map.getTreeLock()) {
					Graphics2D g = map.getGraphics2D();
					cor.drawTrack(g);
					cor.draw( g );
				}
			}
			int nExped = expeditions.size();
			for( int i=0 ; i<nExped ; i++) {
				IceExpedition ex = (IceExpedition)expeditions.get( i );
				if(ex!=e) continue;
				selExpedition = i;
				for( int k=0 ; k<e.cores.length ; k++) {
					if( core!= e.cores[k] ) continue;
					selCore = k;
					core.setHighlight( true );
					synchronized (map.getTreeLock()) {
						Graphics2D g = map.getGraphics2D();
						core.drawTrack(g);
						core.draw( g );
					}
					graph.repaint();
					xyFrame.toFront();
					if(profile!=null)profFrame.remove(profile);
					profile = new XYGraph( core, 0);
					profFrame.setTitle( e.toString()+"-"+core.toString() );
					profFrame.getContentPane().add( profile, "Center");
					profFrame.pack();
					profFrame.toFront();
					profile.repaint();
					tree.repaint();
					return;
				}
			}
		} catch (Exception ex) {
			return;
		}
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.getSource()==tree ) {
			treeSelect( evt );
			return;
		}
//	source is map
		if(evt.isControlDown()) return;
		Point2D p = map.getScaledPoint( evt.getPoint() );
		float zoom = (float)map.getZoom();
		float dx = 1.5f+1.5f/zoom;
		float x = (float)p.getX();
		float y = (float)p.getY();
		int exped = selExpedition;
		int nExped = expeditions.size();
		Object[] selection = expList.getSelectedValues();
		if(selCore!=-1) {
			IceCore core = ((IceExpedition)expeditions.get( (exped)%nExped)).cores[selCore];
			core.setHighlight(false);
			synchronized (map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				core.drawTrack(g);
				core.draw( g );
			}
		}
		for( int i=0 ; i<nExped ; i++) {
			IceExpedition e = (IceExpedition)expeditions.get( (exped+i)%nExped);
			boolean ok=false;
			for( int k=0 ; k<selection.length ; k++) {
				if( e==selection[k] ) {
					ok = true;
					break;
				}
			}
			if( !ok )continue;
			for( int k=0 ; k<e.cores.length ; k++) {
				if( e.cores[(1+k+selCore)%e.cores.length].select(x, y, dx) ) {
					selExpedition = (exped+i)%nExped;
					selCore = (1+k+selCore)%e.cores.length;
					IceCore core = e.cores[selCore];
				//	label.setText( e.name + "-"+ core.name);
					core.setHighlight( true );
					synchronized (map.getTreeLock()) {
						Graphics2D g = map.getGraphics2D();
						core.drawTrack(g);
						core.draw( g );
					}
					graph.repaint();
					xyFrame.toFront();
					if(profile!=null)profFrame.remove(profile);
					profile = new XYGraph( core, 0);
					profFrame.setTitle( e.toString()+"-"+core.toString() );
					profFrame.getContentPane().add( profile, "Center");
					profFrame.pack();
					profFrame.toFront();
					profile.repaint();
					tree.repaint();
					return;
				}
			}
		}
	//	label.setText("None Selected");
		selCore = -1;
		selExpedition = 0;
		tree.repaint();
		profFrame.toBack();
		graph.repaint();
	}
	public boolean isEnabled() {
		return enabled;
	}
	public JComponent getSelectionDialog() {
		return panel;
	}
	public JComponent getDataDisplay() {
		return bottomPanel;
	}
	public String getXTitle(int dataIndex) {
		return "d18O Climatology";
	}
	public String getYTitle(int dataIndex) {
		return "d18O Observations";
	}
	public double[] getXRange(int dataIndex) {
	//	return new double[] { (double)(range[0]-.5f), (double)(range[1]+.5f)};
		return new double[] { -6., 2. };
	}
	public double[] getYRange(int dataIndex) {
	//	return new double[] { (double)(range[0]-.5f), (double)(range[1]+1.5f)};
		return new double[] { -8., 4. };
	}
	public double getPreferredXScale(int dataIndex) {
		return 40.;
	}
	public double getPreferredYScale(int dataIndex) {
		return 40.;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {

		if( obsB.isSelected() )obs.plotXY( g, bounds, xScale, yScale );
		int[] indices = expList.getSelectedIndices();
		Vector xy = new Vector();
		for( int k=0 ; k<indices.length ; k++) {
			int i=indices[k];
			((IceExpedition)expeditions.get(i)).plotXY( g, bounds, xScale, yScale, xy );
		}
		Vector scratch = new Vector();
		if( selCore != -1 ) {
			IceExpedition ex = (IceExpedition)expeditions.get(selExpedition);
			ex.cores[selCore].plotXY(g, bounds, xScale, yScale, scratch);
		}
		if( xy.size()==0 ) {
			label.setText("no points");
			return;
		}
		float sumX=0f;
		float sumY=0f;
		float sumXY=0f;
		float sumX2=0f;
		float diff = 0f;
		for(int i=0 ; i<xy.size() ; i++) {
			float[] p = (float[])xy.get(i);
			sumX += p[0];
			sumY += p[1];
			sumXY += p[1]*p[0];
			sumX2 += p[0]*p[0];
			diff += (p[1]-p[0]);
		}
		float a = (sumXY*sumX-sumY*sumX2) / (sumX*sumX-xy.size()*sumX2);
		float b = (sumY*sumX-xy.size()*sumXY) / (sumX*sumX-xy.size()*sumX2);
		a = 2f;
		b = 1f;
		float x1 = (-6f-(float)bounds.getX()) *(float)xScale;
		float x2 = (2f-(float)bounds.getX()) *(float)xScale;
		float y1 = ( (a-b*6f) - (float)bounds.getY()) *(float)yScale;
		float y2 = ( (a+b*2f) - (float)bounds.getY()) *(float)yScale;
		g.setColor( Color.lightGray );
		g.draw( new Line2D.Float( x1, y1, x2, y2));
		label.setText("mean difference = "+ (diff/(float)xy.size()) +" ("+ xy.size() +" points)");
}
	public static void main(String[] args) {
		XMap map = null;
		IceDB db = new IceDB( map);
		if( db.loadDB() ) {
			System.out.println( "ice database loaded" );
		} else {
			System.out.println( "ice database failed to load" );
		}
	}
}
