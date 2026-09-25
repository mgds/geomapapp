package org.geomapapp.db.dsdp;

import haxby.util.BrowseURL;
import haxby.util.URLFactory;
import haxby.util.XBTable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.util.Icons;
import org.geomapapp.util.ScalableXYPoints;
import org.geomapapp.util.XYGraph;
import org.geomapapp.util.Zoomer;

public class AgeDepthModel implements ScalableXYPoints, AdjustmentListener, ActionListener {
	org.geomapapp.db.dsdp.DSDP dsdp;
	XBTable table;
	JFrame frame;
	JToggleButton toggle;
	XYGraph graph;
	JToolTip tester;
	JDialog graphDialog;
	JScrollPane graphScroll = null;
	JToggleButton plotRange;
	FossilGroup[] groups;
	DSDPHole hole;
	DSDPHole[] holes;
	double[] xRange = new double[] {0., 200.};
	double[] ageRange;
	double[] yRange = new double[] {-100, 0.};
	double[] xr0 = new double[] {0., 200.};
	double[] yr0 = new double[] {-100, 0.};
	double cursorAge = 0;
	int prevYPos = -1;
	int prevXPos = -1;
	Vector ties = new Vector();
	Vector holeSources = new Vector();
	Shape circle;

	JLabel label;
	JLabel sourceLabel;
	String depthAgeBTD;
	Vector fossilDatum;
	Vector groupNames;
	Vector ranges;
	int currentTie = -1;
	Vector undo, redo;
	Vector clipboard;
	static final double B_COEFF = 350.0;

	int[] colors = new int[] {
			0x80ff0000,
			0x8000ff00,
			0x800000ff,
			0x80ffff00,
			0x8000ffff,
			0x80ff00ff,
			0x80ff8000
		};
	JFileChooser chooser;
	File saveDir;
	File[] files;
	JComboBox versionCB;
	
	JButton saveB;
	JButton chronosAdpPortalB;
	JButton normalizeB;
	
	public AgeDepthModel(org.geomapapp.db.dsdp.DSDP dsdp) {
		this.dsdp = dsdp;
		init();
	}
	public JToggleButton getToggle() {
		return toggle;
	}
	void init() {
		toggle = new JToggleButton(Icons.getIcon(Icons.DIGITIZE, false));
		toggle.setSelectedIcon( Icons.getIcon(Icons.DIGITIZE, true));
		toggle.setBorder(null);
		toggle.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphDialog.setVisible(toggle.isSelected());
			}
		});
		toggle.setToolTipText("view age-depth model for selected hole");
		versionCB = new JComboBox();
		versionCB.addItem("Choose Age Model");
		versionCB.addItem("Default");
		versionCB.addItem("Load Age Model");
		versionCB.addItem("Save Current Model");
		versionCB.setToolTipText("ctrl-s to save your edits");
		versionCB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchVersion();
			}
		});
		groups = dsdp.groups;
	//	Vector gps = dsdp.getFossilGroups();
	//	groups = new FossilGroup[gps.size()];
	//	for( int i=0 ; i<gps.size() ; i++) {
	//		try {
	//			groups[i] = dsdp.loadGroup( (String)gps.get(i) );
	//		} catch(Exception e) {
	//			e.printStackTrace();
	//		}
	//	}
		try {
// System.out.println( org.geomapapp.db.dsdp.DSDP.ROOT + "dsdp.datum" );
			URL url = URLFactory.url(org.geomapapp.db.dsdp.DSDP.ROOT + "dsdp.datum");
			BufferedReader in = new BufferedReader(
					new InputStreamReader( url.openStream() ));
			//	new FileReader(
			//		new File("/home/bill/projects/DSDP2000/janus/dsdp.datum")));
			fossilDatum = new Vector();
			groupNames = new Vector();
			while( true ) {
				String s = in.readLine();
				if( s==null )break;
				StringTokenizer st = new StringTokenizer(s,"\t");
				String prefix = st.nextToken();
				groupNames.add( dsdp.getGroupName(prefix) );
//	System.out.println( prefix +"\t"+ dsdp.getGroupName(prefix) );
				FossilDatum[] data = new FossilDatum[Integer.parseInt(st.nextToken())];
				fossilDatum.add( data );
				for( int k=0 ; k<data.length ; k++) {
					s = in.readLine();
					st = new StringTokenizer(s,"\t");
					int code = Integer.parseInt(st.nextToken());
					int type = st.nextToken().equals("FO") ? 0 : 1;
					float age = Float.parseFloat(st.nextToken());
					int ref = FossilDatum.getTimeScale( st.nextToken() );
					data[k] = new FossilDatum(age, ref, code, type, groupNames.size()-1);
				}
			}
			in.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		table = dsdp.getTable();
	//	table.getSelectionModel().setSelectionMode( 
	//			ListSelectionModel.SINGLE_SELECTION);
	//	frame = new JFrame("holes");
	//	frame.getContentPane().add( new JScrollPane(table) );
	//	frame.pack();
	//	frame.setSize( 300, 900);
	//	frame.setVisible(true);
	//	frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//	frame.addWindowListener( new WindowAdapter() {
	//		public void windowClosing(WindowEvent e) {
	//			toggle.setSelected(false);
	//		}
	//	});
		table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					update();
					versionCB.setSelectedIndex(0);
				}
			}
		});
		graph = new XYGraph(this, 0);
		Zoomer z = new Zoomer(graph);
		graph.setScrollableTracksViewportWidth(true);
		graph.setAxesSides( 3 | 8 );

		graph.addMouseListener(z);
		graph.addKeyListener(z);
		graph.setScrollableTracksViewportHeight(false);
		tester = graph.createToolTip();
		graph.setToolTipText("drag the right edge of the graph to the left to zoom age axis");
		//graphDialog = new JDialog((JDialog)table.getTopLevelAncestor());
		graphDialog = new JDialog((JFrame)table.getTopLevelAncestor());
		plotRange = new JToggleButton("fossil ranges");
		plotRange.setSelected(true);
		plotRange.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graph.repaint();
				prevYPos = -1;
				prevXPos = -1;
			}
		});

		saveB = new JButton(Icons.getIcon(Icons.SAVE, false));
		saveB.setSelectedIcon(Icons.getIcon(Icons.SAVE, true));
		saveB.addActionListener(this);
		saveB.setToolTipText("save");
		saveB.setBorder(null);

		chronosAdpPortalB = new JButton("CHRONOS LOC");
		chronosAdpPortalB.addActionListener(this);
//		chronosAdpPortalB.setToolTipText("Link to CHRONOS LOC entry for" + hole.name);

		normalizeB = new JButton(Icons.getIcon(Icons.NORMALIZE, false));
		normalizeB.setSelectedIcon(Icons.getIcon(Icons.NORMALIZE, true));
		normalizeB.addActionListener(this);
		normalizeB.setToolTipText("return to original view");
		normalizeB.setBorder(null);

		JPanel gpanel = new JPanel(new GridLayout(0,1));
		JPanel firstBPanel = new JPanel();
		JPanel secondBPanel = new JPanel();
		firstBPanel.add(versionCB);
		firstBPanel.add(plotRange);
		firstBPanel.add( graph.getDigitizeButton() );
		secondBPanel.add(saveB);
		//secondBPanel.add(chronosAdpPortalB);
		secondBPanel.add(normalizeB);
		gpanel.add(firstBPanel);
		gpanel.add(secondBPanel);
		graphDialog.getContentPane().add( gpanel, "North" );
		graphScroll = new JScrollPane(graph);
		graphScroll.getVerticalScrollBar().addAdjustmentListener(this);
		graphDialog.getContentPane().add( graphScroll );
		label = new JLabel("   ");
		sourceLabel = new JLabel("   ");
		JPanel labelPanel = new JPanel(new GridLayout(0,1));
		labelPanel.add(label);
		labelPanel.add(sourceLabel);
		graphDialog.getContentPane().add( labelPanel, "South" );
		graphDialog.pack();
		graphDialog.setSize( new Dimension( 400,600) );
		Point mapLoc = (dsdp.map.getApp() instanceof haxby.map.MapApp) ? ((haxby.map.MapApp)dsdp.map.getApp()).getFrame().getLocation() : new Point(0,0);
		graphDialog.setLocation(mapLoc.x+300,mapLoc.y);
	//	graphDialog.setVisible(true);
		graphDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		graphDialog.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				toggle.setSelected(false);
			}
		});

		MouseInputAdapter mL = new MouseInputAdapter() {
			public void mouseClicked( MouseEvent e ) {

//				1.4.2: If graph is not reset when clicked the lines are not erased
//				prevYPos = -1;
				/*graph.initSollipsyResize();
				
				graph.dragSollipsyEdge( 4, 200 );
				graph.resizeSollipsy( 4, 200 );*/
				//ScalableXYPoints pts = (ScalableXYPoints)graph.xy;
				if( !graph.canDigitize() )return;
				if(!e.isControlDown() )addPoint(e);
			}
			public void mouseDragged( MouseEvent e ) {
				if( !graph.canDigitize() )return;
				if(e.isControlDown() )return;
				drag(e);
			}
			public void mousePressed( MouseEvent e ) {
				if( !graph.canDigitize() )return;
				if(e.isControlDown() )return;
				setTie(e);
			}
			public void mouseMoved( MouseEvent e ) {
				move(e);
			}
		};
		graph.addMouseListener( mL );
		graph.addMouseMotionListener( mL );
		graph.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent evt) {
				if( evt.getKeyCode()==KeyEvent.VK_N ) {
					if( evt.isShiftDown() ) {
						resetRanges(0);
					} else {
						setXRange( 0, ageRange);
					}
					updateGraph();
					return;
				}
			//	if( !evt.isControlDown() )return;
				if( evt.getKeyCode()==KeyEvent.VK_Z) {
					if( evt.isShiftDown() )redo();
					else undo();
				} else if( evt.getKeyCode()==KeyEvent.VK_X ) {
					delete();
				} else if( evt.getKeyCode()==KeyEvent.VK_S ) {
					save();
				} else if( evt.getKeyCode()==KeyEvent.VK_C ) {
					copy();
				} else if( evt.getKeyCode()==KeyEvent.VK_V ) {
					paste();
				}
			}
		});
	}
	void updateGraph() {
		graph.setPoints( this, 0);
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	void updateID() {
		int row = table.getSelectedRow();
		if( row<0 )return;
		saveDir = org.geomapapp.io.GMARoot.getRoot();
		if( saveDir==null ) {
			File prefs = new File(
				System.getProperty("user.home"),
				".geomapapp-home");
			String root = org.geomapapp.io.GMARoot.createPrefs(prefs);
			saveDir = org.geomapapp.io.GMARoot.getRoot();
			if( saveDir==null ) {
				label.setText("Unable to create root directory, see Haxby");
				System.exit(-1);
			}
		}
		saveDir = new File(saveDir, "DSDP");
		saveDir = new File(saveDir, "age_depth");
		String id = (String)table.getValueAt(row, 0);
		saveDir = new File(saveDir, id);
		files = saveDir.listFiles();
		if( files==null )files=new File[0];
		versionCB.removeAllItems();
		versionCB.addItem("Choose Age Model");
		versionCB.addItem("Default");
		for( int i=0 ; i<files.length ; i++) {
			versionCB.addItem( files[i].getName() );
		}
		versionCB.addItem("Load Age Model");
		versionCB.addItem("Save Current Model");
	}

	public void exportASCII() {
		JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
		File f=new File(graphDialog.getTitle()+"_age_depth.txt");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(haxby.map.MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("ID\tdepth\tage\tsource\n");
			float[] firstTie = (float[])ties.get(0);
			String sourceString = (String)holeSources.get(0);
			out.write(graphDialog.getTitle() + "\t" + firstTie[0] + "\t" + firstTie[1] + "\t" + sourceString + "\n");
			for( int i=0 ; i<ties.size() ; i++) {
				float[] tie = (float[])ties.get(i);
				out.write("\t" + tie[0] + "\t" + tie[1] + "\t\n");
			}
			out.close();
		} catch (IOException ex){

		}
	}

	void switchVersion() {
		if( versionCB.getSelectedItem()==null) return;
		if (versionCB.getSelectedIndex() == 0) return;
		try {
			String id = versionCB.getSelectedItem().toString();
			
			if (id.equals("Load Age Model")) {
				loadAgeModel();
				versionCB.setSelectedIndex(0);
				return;
			} else if( id.equals("Default") ) {
				update();
				versionCB.setSelectedIndex(0);
				return;
			} else if (id.equals("Save Current Model")) {
				save();
				return;
			}
			File file = new File(saveDir, id);
			BufferedReader in = new BufferedReader(
				new FileReader(file));
			String s;
			ties = new Vector();
			while( (s=in.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(s);
				ties.add( new float[] { Float.parseFloat(st.nextToken()),
							Float.parseFloat(st.nextToken())});
			}
			undo = new Vector();
			redo = new Vector();
			graph.repaint();
			prevYPos = -1;
			prevXPos = -1;
		} catch(Exception e) {
			label.setText("error reading file");
			e.printStackTrace();
		}
	}

	private void loadAgeModel() {
		try {
			if( !saveDir.exists() )saveDir.mkdirs();
			if( chooser==null )chooser = new JFileChooser(saveDir);
			// chooser.setSelectedDirectory(saveDir);
			int row = table.getSelectedRow();
			String id = (String)table.getValueAt(row, 0);
			File file = new File( saveDir, id+".da" );
			chooser.setSelectedFile( file );
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int ok = JOptionPane.NO_OPTION;
			while( true ) {
				ok = chooser.showOpenDialog(graphDialog);
				if( ok==JFileChooser.CANCEL_OPTION ) return;
				if( chooser.getSelectedFile().exists() ) {
					break;
				}
			}

			BufferedReader in = new BufferedReader(new FileReader(file));
			String s;
			ties = new Vector();
			while( (s=in.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(s);
				ties.add( new float[] { Float.parseFloat(st.nextToken()),
							Float.parseFloat(st.nextToken())});
			}
			undo = new Vector();
			redo = new Vector();
			graph.repaint();
			prevYPos = -1;
			prevXPos = -1;
		} catch(Exception e) {
			label.setText("error loading");
		}
	}

	void save() {
		try {
			if( !saveDir.exists() )saveDir.mkdirs();
			if( chooser==null )chooser = new JFileChooser(saveDir);
			// chooser.setSelectedDirectory(saveDir);
			int row = table.getSelectedRow();
			String id = (String)table.getValueAt(row, 0);
			File file = new File( saveDir, id+".da" );
			chooser.setSelectedFile( file );
			int ok = JOptionPane.NO_OPTION;
			while( true ) {
				ok = chooser.showSaveDialog(graphDialog);
				if( ok==JFileChooser.CANCEL_OPTION ) return;
				if( chooser.getSelectedFile().exists() ) {
					ok = JOptionPane.showConfirmDialog(graphDialog, "file exists, overwrite?");
					if( ok==JOptionPane.CANCEL_OPTION) return;
					if( ok==JOptionPane.NO_OPTION) continue;
				}
				break;
			}
			PrintStream out = new PrintStream(
				new FileOutputStream( chooser.getSelectedFile() ));
			for( int k=0 ; k<ties.size() ; k++) {
				float[] tie = (float[])ties.get(k);
				out.println( tie[0] +"\t"+ tie[1]);
			}
			out.close();
			label.setText("saved "+chooser.getSelectedFile().getName());
		} catch(Exception e) {
			label.setText("error saving");
		}
	}
	void drawCircle() {
		if( circle==null) return;
		synchronized (graph.getTreeLock()) {
			Graphics2D g = graph.getGraphics2D();
			g.setXORMode( Color.white );
			g.setStroke( new BasicStroke(3f) );
			g.draw(circle);
			g.setXORMode( Color.red );
			g.setStroke( new BasicStroke(1f) );
			g.draw(circle);
		}
	}

	void drawCursorLine( int xPos, int yPos ) {
		synchronized (graph.getTreeLock()) {
			Graphics2D g = graph.getGraphics2D();
			g.setXORMode( Color.cyan );
			g.drawLine(0, yPos, xPos, yPos);
		}
	}

	void printTies( ) {
		for( int k=0 ; k<ties.size() ; k++) {
			float[] tie = (float[])ties.get(k);
			System.out.println( k +"\t"+ tie[0] +"\t"+ tie[1]);
		}
	}
	void setTie( MouseEvent e ) {
		if( ties.size()<2 ) {
			circle=null;
			currentTie = -1;
			return;
		}
		double y = graph.getYAt( e.getPoint() );
		int i=0;
		double r = 100000.;
		float[] tie = new float[2];
		for( int k=0 ; k<ties.size() ; k++) {
			tie = (float[])ties.get(k);
			double test = Math.abs(tie[0]-y);
			if( test<r ) {
				r = test;
				i=k;
			}
		}
		currentTie = i;
		redo = new Vector();
		undo.add( 0, cloneTies() );
		drag(e);
	}
	void addPoint( MouseEvent e ) {
		undo();
		if(redo.size()>0)redo.remove(0);
		double y = graph.getYAt( e.getPoint() );
		double x = graph.getXAt( e.getPoint() );
		int i=0;
		double r = 100000.;
		float[] tie = new float[2];
		for( int k=0 ; k<ties.size() ; k++) {
			tie = (float[])ties.get(k);
			if( y>tie[0] ) i=k+1;
		}
		currentTie = i;
		redo = new Vector();
		undo.add( 0, cloneTies() );
		if( i==ties.size() ) ties.add( new float[] {(float)y, (float)x} );
		else ties.add( i, new float[] {(float)y, (float)x} );
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
		currentTie = -1;
	}
	Vector cloneTies() {
		Vector tmp = new Vector( ties.size() );
		for( int k=0 ; k<ties.size() ; k++) {
			float[] tie = (float[])ties.get(k);
			tmp.add( new float[] {tie[0], tie[1]} );
		}
		return tmp;
	}
	void delete() {
		if( currentTie<0 )return;
		undo.add( 0, cloneTies() );
		float[] tie = (float[])ties.remove(currentTie);
		currentTie = -1;
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	void copy() {
		clipboard = cloneTies();
	}
	void paste() {
		if( clipboard==null || clipboard.size()==0 )return;
		undo.add( cloneTies() );
		ties = clipboard;
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	void undo() {
		if( undo==null || undo.size()==0 )return;
		redo.add( 0, cloneTies() );
		ties = (Vector)undo.remove(0);
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	void redo() {
		if( redo==null || redo.size()==0 )return;
		undo.add( cloneTies() );
		ties = (Vector)redo.remove(0);
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	void drag( MouseEvent e ) {
		drawCircle();
		if( ties.size()<2 || currentTie<0 ) {
			circle=null;
			currentTie = -1;
			return;
		}

		double y = graph.getYAt( e.getPoint() );
		double x = graph.getXAt( e.getPoint() );

		float[] tie = (float[])ties.get(currentTie);
		if( currentTie>0 ) {
			tie = (float[])ties.get(currentTie-1);
			if( y<tie[0]+.001 ) y = tie[0]+.001;
		} else {
			tie = (float[])ties.get(currentTie);
			y=(float)tie[0];
		}
		if( currentTie<ties.size()-1 ) {
			tie = (float[])ties.get(currentTie+1);
			if( y>tie[0]-.001 ) y = tie[0]-.001;
		//} else {
		//	tie = (float[])ties.get(currentTie);
		//	y=(float)tie[0];
		}
		if( x<0. )x=0.;
		tie = (float[])ties.get(currentTie);
		tie[0] = (float)y;
		tie[1] = (float)x;
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
		double[] info = graph.getPlotInfo();
		double x0 = info[2];
		double y0 = info[3];
		double xs = info[0];
		double ys = info[1];
		y = (-tie[0]-y0)*ys;
		x = (tie[1]-x0)*xs;
		circle = new Arc2D.Double( x-4., y-4., 8., 8., 0., 360., Arc2D.CHORD);
	//	circle = new Line2D.Double( 0., y, 1000., y);
		drawCircle();
		label.setText( currentTie+":  "+tie[0]+" m, "+ tie[1] +" mA");
	}

	void move( double yPos ) {
		//	currentTie = -1;
			drawCircle();
			if ( prevYPos != -1  && prevXPos != -1 ) {
				drawCursorLine(prevXPos, prevYPos);
			}
			if( ties.size()<2 ) {
				circle=null;
				currentTie = -1;
				return;
			}
			double y = yPos;
			int i=0;
			int iAbove = 0;
			int iBelow = 0;
			double r = 100000.;

			for( int k=0 ; k<ties.size() ; k++) {
				float[] tie = (float[])ties.get(k);
				double test = Math.abs(tie[0]-y);
				if( test<r ) {
					r = test;
					i=k;
				}
			}

			r = 100000.;

			for( int k=0 ; k<ties.size() ; k++) {
				float[] tie = (float[])ties.get(k);
				if (tie[0] < y)	{
					double test = Math.abs(tie[0]-y);
					if (test < r)	{
						r = test;
						iAbove = k;
					}
				}
			}

			r = 100000.;

			for( int k=0 ; k<ties.size() ; k++) {
				float[] tie = (float[])ties.get(k);
				if ( tie[0] > y )	{
					double test = Math.abs(tie[0]-y);
					if (test < r)	{
						r = test;
						iBelow = k;
					}
				}
			}
			if ( iBelow == 0 ) {
				iBelow = ties.size() - 1;
			}
			else if ( iBelow != (iAbove + 1) ) {
				iBelow = iAbove + 1;
			}

			currentTie = i;
			double[] info = graph.getPlotInfo();
			double x0 = info[2];
			double y0 = info[3];
			double xs = info[0];
			double ys = info[1];
			float[] tie = (float[])ties.get(i);
			float[]	tieAbove = (float[])ties.get(iAbove);
			float[]	tieBelow = (float[])ties.get(iBelow);

			cursorAge = 0;
			cursorAge = ((tieBelow[1] - tieAbove[1]) / (tieBelow[0] - tieAbove[0])) * (y - tieAbove[0]) + tieAbove[1];

			y = (-tie[0]-y0)*ys;
			double x = (tie[1]-x0)*xs;
			prevYPos = (int)( (-yPos - y0) * ys);
			prevXPos = (int)((cursorAge-x0)*xs);
			circle = new Arc2D.Double( x-4., y-4., 8., 8., 0., 360., Arc2D.CHORD);
		//	circle = new Line2D.Double( 0., y, 1000., y);
			drawCircle();
			drawCursorLine( prevXPos, prevYPos );

	/*
			label.setText( i+":  "+tie[0]+" m, "+ tie[1] +" mA" 
							+ ";  Above-" + iAbove + ":  " + tieAbove[0] + "m, " + tieAbove[1] + "mA"
							+ ";  Below-" + iBelow + ":  " + tieBelow[0] + "m, " + tieBelow[1] + "mA");
			System.out.println(cursorAge);
	*/

			double BTDepth = -1;
			NumberFormat fmtBTDepth = NumberFormat.getInstance();
			fmtBTDepth.setMaximumFractionDigits(0);
			int temp = table.getSelectedRow();
			float crustalAge = -1;
			float bottomDepth = -1;
			float totalPenetration = -1;
			Vector currentRow = (Vector)dsdp.db.getData().get(temp);
			if ( currentRow.get(DSDPDemo.CRUSTAL_AGE_COLUMN_INDEX) != null ) {
				crustalAge = Float.parseFloat(currentRow.get(DSDPDemo.CRUSTAL_AGE_COLUMN_INDEX).toString());
			}
			if ( currentRow.get(DSDPDemo.BOTTOM_DEPTH_COLUMN_INDEX) != null ) {
				bottomDepth = Float.parseFloat(currentRow.get(DSDPDemo.BOTTOM_DEPTH_COLUMN_INDEX).toString());
			}
			if ( currentRow.get(DSDPDemo.PENETRATION_COLUMN_INDEX) != null ) {
				totalPenetration = Float.parseFloat(currentRow.get(DSDPDemo.PENETRATION_COLUMN_INDEX).toString());
			}
			double depthBelowSeaFloor = yPos;
			double depthBelowSeaSurface = bottomDepth + depthBelowSeaFloor;
			double birthDepth = depthBelowSeaSurface - B_COEFF * Math.sqrt(crustalAge);
			double duration = crustalAge - cursorAge;
			if ( duration < 0 )	{
				duration = 0;
			}
			BTDepth = birthDepth + B_COEFF * Math.sqrt(duration) - (depthBelowSeaFloor / 2.0);

			NumberFormat fmtAge = NumberFormat.getInstance();
			fmtAge.setMaximumFractionDigits(3);
			NumberFormat fmtDepth = NumberFormat.getInstance();
			fmtDepth.setMaximumFractionDigits(2);

			depthAgeBTD = "";

			if ( cursorAge >= 0 )	{
				label.setText( i+":  "+ fmtDepth.format(tie[0]) +"mbsf, "+ fmtAge.format(tie[1]) +"Ma" 
						+ ";   " + fmtDepth.format(depthBelowSeaFloor) + "mbsf"
						+ ", " + fmtAge.format(cursorAge) + "Ma");
				depthAgeBTD = fmtDepth.format(depthBelowSeaFloor) + "mbsf"
				+ ", " + fmtAge.format(cursorAge) + "Ma";
				if ( crustalAge >= 0 )	{
					label.setText( i+":  "+ fmtDepth.format(tie[0]) +"mbsf, "+ fmtAge.format(tie[1]) +"Ma" 
							+ ";   " + fmtDepth.format(depthBelowSeaFloor) + "mbsf"
							+ ", " + fmtAge.format(cursorAge) + "Ma"
							+ ";   BTD: " + fmtBTDepth.format(BTDepth) + "mbss");
					depthAgeBTD = depthAgeBTD + ";   BTD: " + fmtBTDepth.format(BTDepth) + "mbss";
				}
			}
			else {
				label.setText( i + ":  " + fmtDepth.format(tie[0]) + "mbsf, " + fmtAge.format(tie[1]) +"Ma");
				if ( depthBelowSeaFloor < totalPenetration )	{
					depthAgeBTD = fmtDepth.format(depthBelowSeaFloor) + "mbsf";
				}
			}
			//System.out.println(hole.ageIntervals[1].age);
		}

	void move( MouseEvent e ) {
	//	currentTie = -1;
		drawCircle();
		if ( prevYPos != -1  && prevXPos != -1 ) {
			drawCursorLine(prevXPos, prevYPos);
		}
		if( ties.size()<2 ) {
			circle=null;
			currentTie = -1;
			return;
		}
		double y = graph.getYAt( e.getPoint() );
		int i=0;
		int iAbove = 0;
		int iBelow = 0;
		double r = 100000.;

		for( int k=0 ; k<ties.size() ; k++) {
			float[] tie = (float[])ties.get(k);
			double test = Math.abs(tie[0]-y);
			if( test<r ) {
				r = test;
				i=k;
			}
		}

		r = 100000.;

		for( int k=0 ; k<ties.size() ; k++) {
			float[] tie = (float[])ties.get(k);
			if (tie[0] < y)	{
				double test = Math.abs(tie[0]-y);
				if (test < r)	{
					r = test;
					iAbove = k;
				}
			}
		}

		r = 100000.;

		for( int k=0 ; k<ties.size() ; k++) {
			float[] tie = (float[])ties.get(k);
			if ( tie[0] > y )	{
				double test = Math.abs(tie[0]-y);
				if (test < r)	{
					r = test;
					iBelow = k;
				}
			}
		}
		if ( iBelow == 0 )	{
			iBelow = ties.size() - 1;
		}
		else if ( iBelow != (iAbove + 1) ) {
			iBelow = iAbove + 1;
		}

		currentTie = i;
		double[] info = graph.getPlotInfo();
		double x0 = info[2];
		double y0 = info[3];
		double xs = info[0];
		double ys = info[1];
		float[] tie = (float[])ties.get(i);
		float[]	tieAbove = (float[])ties.get(iAbove);
		float[]	tieBelow = (float[])ties.get(iBelow);

		cursorAge = 0;
		cursorAge = ((tieBelow[1] - tieAbove[1]) / (tieBelow[0] - tieAbove[0])) * (y - tieAbove[0]) + tieAbove[1];

		y = (-tie[0]-y0)*ys;
		double x = (tie[1]-x0)*xs;
		prevYPos = (int)( (-graph.getYAt( e.getPoint() ) - y0) * ys);
		prevXPos = (int)((cursorAge-x0)*xs);
		circle = new Arc2D.Double( x-4., y-4., 8., 8., 0., 360., Arc2D.CHORD);
	//	circle = new Line2D.Double( 0., y, 1000., y);
		drawCircle();
		drawCursorLine( prevXPos, prevYPos );

/*
		label.setText( i+":  "+tie[0]+" m, "+ tie[1] +" mA" 
						+ ";  Above-" + iAbove + ":  " + tieAbove[0] + "m, " + tieAbove[1] + "mA"
						+ ";  Below-" + iBelow + ":  " + tieBelow[0] + "m, " + tieBelow[1] + "mA");
		System.out.println(cursorAge);
*/

		double BTDepth = -1;
		NumberFormat fmtBTDepth = NumberFormat.getInstance();
		fmtBTDepth.setMaximumFractionDigits(0);
		int temp = table.getSelectedRow();
		if (temp == -1)	 return;

		float crustalAge = -1;
		float bottomDepth = -1;
		float totalPenetration = -1;
		Vector currentRow = (Vector)dsdp.db.getData().get(temp);
		if ( currentRow.get(DSDPDemo.CRUSTAL_AGE_COLUMN_INDEX) != null ) {
			crustalAge = Float.parseFloat(currentRow.get(DSDPDemo.CRUSTAL_AGE_COLUMN_INDEX).toString());
		}
		if ( currentRow.get(DSDPDemo.BOTTOM_DEPTH_COLUMN_INDEX) != null ) {
			bottomDepth = Float.parseFloat(currentRow.get(DSDPDemo.BOTTOM_DEPTH_COLUMN_INDEX).toString());
		}
		if ( currentRow.get(DSDPDemo.PENETRATION_COLUMN_INDEX) != null ) {
			totalPenetration = Float.parseFloat(currentRow.get(DSDPDemo.PENETRATION_COLUMN_INDEX).toString());
		}
		double depthBelowSeaFloor = graph.getYAt(e.getPoint());
		double depthBelowSeaSurface = bottomDepth + depthBelowSeaFloor;
		double birthDepth = depthBelowSeaSurface - B_COEFF * Math.sqrt(crustalAge);
		double duration = crustalAge - cursorAge;
		if ( duration < 0 )	{
			duration = 0;
		}
		BTDepth = birthDepth + B_COEFF * Math.sqrt(duration) - (depthBelowSeaFloor / 2.0);

		NumberFormat fmtAge = NumberFormat.getInstance();
		fmtAge.setMaximumFractionDigits(3);
		NumberFormat fmtDepth = NumberFormat.getInstance();
		fmtDepth.setMaximumFractionDigits(2);

		depthAgeBTD = "";

		if ( cursorAge >= 0 ) {
			label.setText( i+":  "+ fmtDepth.format(tie[0]) +"mbsf, "+ fmtAge.format(tie[1]) +"Ma" 
					+ ";   " + fmtDepth.format(depthBelowSeaFloor) + "mbsf"
					+ ", " + fmtAge.format(cursorAge) + "Ma");
			depthAgeBTD = fmtDepth.format(depthBelowSeaFloor) + "mbsf"
			+ ", " + fmtAge.format(cursorAge) + "Ma";
			if ( crustalAge >= 0 )	{
				label.setText( i+":  "+ fmtDepth.format(tie[0]) +"mbsf, "+ fmtAge.format(tie[1]) +"Ma" 
						+ ";   " + fmtDepth.format(depthBelowSeaFloor) + "mbsf"
						+ ", " + fmtAge.format(cursorAge) + "Ma"
						+ ";   BTD: " + fmtBTDepth.format(BTDepth) + "mbss");
				depthAgeBTD = depthAgeBTD + ";   BTD: " + fmtBTDepth.format(BTDepth) + "mbss";
			}
		}
		else {
			label.setText( i + ":  " + fmtDepth.format(tie[0]) + "mbsf, " + fmtAge.format(tie[1]) +"Ma");
			if ( depthBelowSeaFloor < totalPenetration ) {
				depthAgeBTD = fmtDepth.format(depthBelowSeaFloor) + "mbsf";
			}
		}
		//System.out.println(hole.ageIntervals[1].age);
	}

	public double getPosFromAge( double inputAge ) {
		int iAbove = -1;
		int iBelow = -1;
		for ( int i = 0; i < ties.size(); i++ ) {
			float[] tie = (float[])ties.get(i);
			if ( tie[1] > inputAge ) {
				iAbove = i - 1;
				iBelow = i;
				break;
			}
		}
		if ( iAbove == -1 ) {
			return 0;
		}
		float[] tieAbove = (float[])ties.get(iAbove);
		float[] tieBelow = (float[])ties.get(iBelow);
		double pos = ( (inputAge - tieAbove[1]) / ( (tieBelow[1] - tieAbove[1]) / (tieBelow[0] - tieAbove[0]) ) ) + tieAbove[0];
		return pos;
	}

	void update() {
		updateID();
		undo = new Vector();
		redo = new Vector();
		int[] rows = table.getSelectedRows();
		if( rows==null || rows.length==0 )return;
		rows = new int[] {rows[0]};
		String id = (String)table.getValueAt(rows[0], 0);
		graphDialog.setTitle( id );
		holes = new DSDPHole[rows.length];
		ranges = new Vector();
		double maxAge = 1.;
		DSDPHole hole = null;
		for( int k=0 ; k<rows.length ; k++) {
			id = (String)table.getValueAt(rows[k], 0);
			hole = dsdp.holeForID(id);
			holes[k] = hole;
			AgeInterval[] ages = hole.ageIntervals;
			if( ages==null ) {
				ties = new Vector();
				ties.add( new float[] {0f, 0f});
				continue;
			}	
			if( k==0 ) yRange = new double[] { -hole.totalPen, 0.};
			else if( -hole.totalPen<yRange[0] ) yRange[0]=-hole.totalPen;
		//	System.out.println("***\t"+ hole.toString() );
			for( int i=0 ; i<groupNames.size() ; i++) {
				FossilAssembly fossils = hole.getFossilAssembly((String)groupNames.get(i));
				if( fossils==null )continue;
				FossilEntry[] entries = fossils.entries;
				short[] codes = fossils.getAllCodes();
				FossilDatum[] data = (FossilDatum[])fossilDatum.get(i);
				int n = 0;
				for( int j=0 ; j<data.length ; j++) {
					boolean ok = true;
					for( int jj=0 ; jj<codes.length ; jj++) {
						if( codes[jj]==(short)data[j].code ) {
							n++;
							ok = false;
							break;
						}
					}
					if( ok ) continue;
					FossilRange range = new FossilRange( data[j], 0f, hole.totalPen);
					if( data[j].type==1 ) {
						if( (double)entries[0].abundanceForCode(data[j].code)!=-2. )continue;
						for( int jj=0 ; jj<entries.length ; jj++) {
							if( (double)entries[jj].abundanceForCode(data[j].code) == -2.) {
								range.minZ = entries[jj].depth;
							} else {
								range.maxZ = entries[jj].depth;
								ranges.add(range);
								break;
							}
						}
					} else {
						if( (double)entries[entries.length-1].abundanceForCode(data[j].code)!=-2. )continue;
						for( int jj=entries.length-1 ; jj>=0 ; jj--) {
							if( (double)entries[jj].abundanceForCode(data[j].code) == -2.) {
								range.maxZ = entries[jj].depth;
							} else {
								range.minZ = entries[jj].depth;
								ranges.add(range);
								break;
							}
						}
					}
				}
			// if( n>0 ) System.out.println( groupNames.get(i) +"\t"+ n);
			}
		}
	//	System.out.println( ranges.size() );
		for( int k=0 ; k<ranges.size() ; k++) {
			FossilRange range = (FossilRange)ranges.get(k);
			FossilDatum datum = range.datum;
			if( datum.age>maxAge )maxAge = datum.age;
	//		System.out.println( datum.code +"\t"+ datum.age +"\t"+ range.minZ +"\t"+ range.maxZ);
		}
		AgeInterval[] ages = getAgeIntervals();
		for( int i=0 ; i<ages.length ; i++ ) {
			float[] range = ages[i].getAgeRange();
			if( range[1]>maxAge ) maxAge = range[1];
		}
		ageRange = new double[] {0., maxAge};
	//	doModel();
		ties = hole.getAgeModel();
		ties = cloneTies();
		float[] tie = (float[])ties.get(ties.size() - 1);
		xRange = new double[] {0., (double)tie[1]};
		xr0 = new double[] {0., (double)tie[1]};
		if( ties.size()==0 ) {
			ties.add( new float[] {0f, 0f});
		}
		holeSources = hole.getSources();
		if ( holeSources.size() == 0 ) {
			holeSources.add( new String("") );
		}
		label.setText( ties.size() +" tie points");
		sourceLabel.setText("Source: " + (String)holeSources.get(0));
		graph.setPoints(this,0);
		graph.repaint();
		prevYPos = -1;
		prevXPos = -1;
	}
	AgeInterval[] getAgeIntervals() {
		if( holes==null||holes.length==0 )return new AgeInterval[0];
		int na = 0;
		for ( int k=0 ; k<holes.length ; k++) {
			if ( holes[k] != null && 
					holes[k].ageIntervals != null ) {
				na += holes[k].ageIntervals.length;
			}
		}
		AgeInterval[] ages = new AgeInterval[na];
		na = 0;
		for( int k=0 ; k<holes.length ; k++) {
			if ( holes[k].ageIntervals != null ) {
				AgeInterval[] a = holes[k].ageIntervals;
				for(int i=0 ; i<a.length ; i++) {
					ages[na++]=a[i];
				}
			}
		}
		Arrays.sort( ages, new Comparator() {
			public int compare( Object o1, Object o2 ) {
				AgeInterval a1 = (AgeInterval)o1;
				AgeInterval a2 = (AgeInterval)o2;
				if( a1.top>=a2.bottom )return 1;
				if( a2.top>=a1.bottom )return -1;
				int i=0;
				if( a1.top>a2.top )i++;
				else i--;
				if( a1.bottom>a2.bottom )i++;
				else i--;
				return i;
			}
			public boolean equals(Object o) {
				return o==this;
			}
		});
		return ages;
	}
	void doModel() {
		System.out.println("in domodel");
		ties = new Vector();
		AgeInterval[] a = getAgeIntervals();
//	System.out.println( a.length +" intervals");
		if( a.length<2 )return;
		float[] top = new float[] {0f, 0f};
		if( a[0].top==0f ) {
			top[1]= a[0].getAgeRange()[0];
		}
		ties.add(top);
		for( int i=0 ; i<a.length-1 ; i++) {
			float[] r1 = a[i].getAgeRange();
			float[] r2 = a[i+1].getAgeRange();
			if( a[i].bottom==a[i+1].top ) {
				if( r1[1]==r2[0] ) {
					ties.add( new float[] {a[i].bottom, r1[1]} );
				} else if( r1[1]<r2[0] ) {
					a[i+1].top += .001f;
				//	ties.add( new float[] {
				//		.5f*(a[i].bottom+a[i+1].top), .5f*(r1[1]+r2[0])} );
					ties.add( new float[] {a[i].bottom, r1[1]} );
					ties.add( new float[] {a[i+1].top, r2[0]} );
				}
			} else if( a[i].bottom<a[i+1].top ) {
				if( r1[1]<=r2[0] ) ties.add( new float[] {
						.5f*(a[i].bottom+a[i+1].top), .5f*(r1[1]+r2[0])} );
			}
		}
		//	System.out.println(ties.size() +" ties");
		if( ties.size()<=1 ) {
			if( ties.size()==0 ) ties.add( new float[] {0f, 0f} );
			return;
		}
		boolean changed=true;
		int nChange=0;
		while(changed) {
			changed=false;
			for( int i=0 ; i<a.length ; i++) {
				float[] r = a[i].getAgeRange();
				float age = ageAtDepth(a[i].top);
				if( r[1]!=0f && age<r[0] ) {
					if( insertAge( new float[] {a[i].top, r[0]}) ) {
						changed=true;
						nChange++;
					}
				}
				age = ageAtDepth(a[i].bottom);
				if( r[1]!=0f && age>r[1] ) {
					if( insertAge( new float[] {a[i].bottom, r[1]}) ) {
						changed=true;
						nChange++;
					}
				}
			}
		//	if(nChange>5 ) break;
		}
		if( ties.size()>1) {
			float ymax = 0f;
			for( int k=0 ; k<holes.length ; k++) {
				DSDPHole hole = holes[k];
				AgeInterval[] ages = hole.ageIntervals;
				if( ages==null)continue;
				for( int i=0 ; i<ages.length ; i++) {
					if( ages[i].bottom>ymax ) ymax=ages[i].bottom;
				}
			}
			float[] tie = (float[])ties.get(ties.size()-1);
			if( ymax>tie[0] ) {
				float amax = ageAtDepth(ymax);
				ties.add( new float[] {ymax, amax});
			}
		}
	}
	boolean insertAge( float[] tie ) {
		for( int k=0 ; k<ties.size() ; k++) {
			float[] t = (float[])ties.get(k);
			if( t[0]==tie[0] && t[1]==tie[1] ) return false;
			if( tie[0]<t[0] ) {
				ties.add( k, tie );
				return true;
			}
		}
		ties.add(tie);
		return true;
	}
	public static float ageAtDepth(Vector ties, float depth) {
		if( ties.size()<2 )return Float.NaN;
		int k=0;
		for( k=0 ; k<ties.size()-2 ; k++) {
			float[] tie = (float[])ties.get(k+1);
			if( tie[0]>=depth )break;
		}
		float[] t1=(float[])ties.get(k);
		float[] t2=(float[])ties.get(k+1);
		if( t1[0]==t2[0] ) {
			if( k==0) return t1[1];
			else if(k==ties.size()-2) return t2[1];
			else return .5f*(t1[1]+t2[1]);
		}
		return t1[1] + (depth-t1[0])*(t2[1]-t1[1])/(t2[0]-t1[0]);
	}
	public float ageAtDepth(float depth) {
		if( ties.size()<2 )return Float.NaN;
		int k=0;
		for( k=0 ; k<ties.size()-2 ; k++) {
			float[] tie = (float[])ties.get(k+1);
			if( tie[0]>=depth )break;
		}
		float[] t1=(float[])ties.get(k);
		float[] t2=(float[])ties.get(k+1);
		if( t1[0]==t2[0] ) {
			if( k==0) return t1[1];
			else if(k==ties.size()-2) return t2[1];
			else return .5f*(t1[1]+t2[1]);
		}
		return t1[1] + (depth-t1[0])*(t2[1]-t1[1])/(t2[0]-t1[0]);
	}
	public String getXTitle(int dataIndex) {
		return "age";
	}
	public String getYTitle(int dataIndex) {
		return "depth";
	}
	public double[] getXRange(int dataIndex) {
		return xRange;
	}
	public double[] getYRange(int dataIndex) {
		return yRange;
	}
	public double getPreferredXScale(int dataIndex) {
		return 2.;
	}
	public double getPreferredYScale(int dataIndex) {
		return 2.;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex){
		circle=null;
		if( holes==null||holes.length==0 )return;
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float xs = (float)xScale;
		float ys = (float)yScale;
		dsdp.setZScale( -ys, this);
		float ymax = 0f;
		for( int k=0 ; k<holes.length ; k++) {
			DSDPHole hole = holes[k];
			AgeInterval[] ages = hole.ageIntervals;
			if( ages==null)continue;
			g.setColor( new Color(colors[k%colors.length], true) );
			for( int i=0 ; i<ages.length ; i++) {
				float[] range = ages[i].getAgeRange();
				float x1 = (range[0]-x0)*xs;
				float x2 = (range[1]-x0)*xs;
				float y1 = (-ages[i].top-y0)*ys;
				float y2 = (-ages[i].bottom-y0)*ys;
	//	System.out.println( x1 +"\t"+ y1 +"\t"+ (x2-x1) +"\t"+ (y2-y1) );
				g.fill( new Rectangle2D.Float(x1,y1,x2-x1, y2-y1));
				if( ages[i].bottom>ymax ) ymax=ages[i].bottom;
			}
		}
		g.setStroke( new BasicStroke( 3f ));
		if( plotRange.isSelected() ) {
			for( int k=0 ; k<ranges.size() ; k++) {
				FossilRange range = (FossilRange)ranges.get(k);
				FossilDatum datum = range.datum;
			//	System.out.println( datum.code +"\t"+ datum.age +"\t"+ range.minZ +"\t"+ range.maxZ);
				float x1 = (datum.age-x0)*xs;
				float y1 = (-range.minZ-y0)*ys;
				float y2 = (-range.maxZ-y0)*ys;
				if( datum.type==0 ) g.setColor( Color.blue );
				else g.setColor( new Color( 150, 150, 0) );
				g.draw( new Line2D.Float(x1,y1,x1, y2));
			}
		}
		if( ties.size()<2 )return;
		g.setColor( Color.black );
		g.setStroke( new BasicStroke( 2f ));
		GeneralPath p = new GeneralPath();
		for( int i=0 ; i<ties.size() ; i++) {
			float[] tie = (float[])ties.get(i);
			if( i==0 ) p.moveTo( (tie[1]-x0)*xs, (-tie[0]-y0)*ys);
			else p.lineTo( (tie[1]-x0)*xs, (-tie[0]-y0)*ys);
		}
	//	float amax = ageAtDepth(ymax);
	//	p.lineTo( (amax-x0)*xs, (-ymax-y0)*ys );
		g.draw(p);
	}
	public void setXRange(int dataIndex, double[] range){
		xRange = range;
	}
	public void setYRange(int dataIndex, double[] range){
	}
	public void resetRanges(int dataIndex){
		xRange = xr0;
	}
	public static void main(String[] args) {
		new AgeDepthModel(new org.geomapapp.db.dsdp.DSDP());
	}
	public void adjustmentValueChanged(AdjustmentEvent ae) {
		if ( ae.getSource() == graphScroll.getVerticalScrollBar() ) {
			graph.repaint();

			if ( !dsdp.getDemo().getAdjustment() ) {
				dsdp.setZScale( -1 * graph.getYScale(), this );
				Rectangle visibleRect = graph.getVisibleRect();
//				dsdp.demo.adjustGraphs( 2 * graph.getZoom(), visibleRect.getCenterY(), "AGE DEPTH MODEL" );
				dsdp.demo.adjustGraphs( graph.getZoom(), visibleRect.getCenterY(), "AGE DEPTH MODEL" );
			}
			prevYPos = -1;
		}

	}
	public void actionPerformed(ActionEvent aevt) {
		if ( aevt.getSource().equals(saveB) ) {
			saveB.setEnabled(false);
			save();
//			exportASCII();
			saveB.setEnabled(true);
		}
		else if ( aevt.getSource().equals(chronosAdpPortalB) ) {
			String inputURLString = "https://doi.org/10.1130/GES00009.1";
			BrowseURL.browseURL(inputURLString);
		}
		else if ( aevt.getSource().equals(normalizeB) ) {
			update();
		}
	}
}