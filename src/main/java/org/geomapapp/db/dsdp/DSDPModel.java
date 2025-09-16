package org.geomapapp.db.dsdp;

import org.geomapapp.util.*;
import org.geomapapp.db.util.GTable;
import org.geomapapp.image.Palette;
import haxby.util.XBTable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;

public class DSDPModel implements RangeListener, ColorServer, ActionListener {
	JTree dataTree;
	JComboBox dataSets;
	JDialog dialog;
	JToggleButton toggle;
	JToggleButton colorButton;
	JToggleButton drawButton;
	JButton closeB;
	DataEntry rootEntry;
	DSDP dsdp;
	DSDPDemo demo;
	JTextField fromT, toT;
	RangeDialog rangeDialog;
	double[] range;
	Palette palette;
	Hashtable colors;
	public DSDPModel(DSDP dsdp, DSDPDemo demo) {
		this.dsdp = dsdp;
		this.demo = demo;
		range = new double[] {0., 10.};
		float[] red = new float[] { 0f, 0f, 0f, 1f, 1f };
		float[] green = new float[] { 0f, 1f, 1f, 1f, 0f };
		float[] blue = new float[] { 1f, 1f, 0f, 0f, 0f };
		float[] ht = new float[] { 0f, 1f, 2f, 3f, 4f };
		palette = new Palette(red, green, blue, ht);
		palette.setRange(0f, 100f);
		init();
	}
	void init() {
		toggle = new JToggleButton(Icons.getIcon(Icons.FOLDER, false));
		toggle.setSelectedIcon( Icons.getIcon(Icons.FOLDER, true));
		toggle.setBorder(null);
		toggle.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				show();
			}
		});
		Vector rootChildren = new Vector();
		rootEntry = new DataEntry(null, rootChildren, "Data",
				DSDP.ROOT + "composition/");
		for( int k=0 ; k<categories.length ; k++ ) {
			Vector children = new Vector();
			DataEntry entry = new DataEntry( rootEntry, children, categories[k], categories[k]+".");
			rootChildren.add( entry );
			for( int i=0 ; i<items[k].length ; i++) {
				DataEntry dataE = new DataEntry( entry, null, items[k][i][1], items[k][i][0]+".tsf.gz" );
				children.add( dataE );
			}
		}
		dataTree = new JTree(rootEntry);

		dataSets = new JComboBox();
		dataSets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setData();
			}
		});
		
		JButton load = new JButton("load");
		load.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});

		dialog = new JDialog(demo.dsdpF);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add( new JScrollPane(dataTree) );
		panel.add( load, "South" );
		dialog.getContentPane().add( panel, "West");

		panel = new JPanel(new BorderLayout());
	//	panel.add( dataSets, "North" );
		JPanel p1 = new JPanel( new GridLayout(0,1) );
		p1.add(dataSets);
		JPanel p2 = new JPanel( new GridLayout(1,0) );
		JButton close = new JButton( Icons.getIcon(Icons.CLOSE, false));
		close.setToolTipText("dispose loaded type");
		close.setBorder(null);
		close.setPressedIcon( Icons.getIcon(Icons.CLOSE, true));
		p2.add(close);
		close.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeData();
			}
		});
		
		ActionListener paintMap = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dsdp.repaintMap();
			}
		};

		colorButton = new JToggleButton(Icons.getIcon(Icons.CONTINUOUS, false));
		colorButton.setSelectedIcon(Icons.getIcon(Icons.CONTINUOUS, true));
		colorButton.setToolTipText("color symbols by \"scaled\" value");
		drawButton = new JToggleButton(Icons.getIcon(Icons.POINTS, false));
		drawButton.setSelectedIcon(Icons.getIcon(Icons.POINTS, true));
		drawButton.setToolTipText("show only holes with measurements");
		drawButton.setSelected(true);
		colorButton.addActionListener(paintMap);
		drawButton.addActionListener(paintMap);
		colorButton.setBorder(null);
		drawButton.setBorder(null);
		p2.add(drawButton);
		p2.add(colorButton);
		p1.add(p2);
		panel.add( p1, "North" );

	//	JPanel p1 = new JPanel( new GridLayout(0,1) );
	//	JLabel label = new JLabel("From (mA)");
	//	p1.add(label);
	//	fromT = new JTextField("0");
	//	p1.add(fromT);
	//	label = new JLabel("To (mA)");
	//	p1.add(label);
	//	toT = new JTextField("200");
	//	p1.add(toT);
	//	panel.add( p1, "South");

		rangeDialog = new RangeDialog(this, 0, 200, 2, 2, 1.);
		panel.add( rangeDialog );
		
		dialog.getContentPane().add( panel);
		
		closeB = new JButton("Close");
		closeB.addActionListener(this);
		dialog.getContentPane().add(closeB, "South");
		
		dialog.pack();
		dialog.setLocation(demo.anchorLocation);
		dialog.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				toggle.setSelected(false);
				if ( colorButton.isSelected() ) {
					colorButton.doClick();
				}
				closeData();
				dsdp.demo.table.getSelectionModel().clearSelection();
//				dsdp.map.repaint();
				dialog.dispose();
			}
		});
	}
	public void setRange(double[] range) {
		if( this.range[0]==range[0] && this.range[1]==range[1] )return;
		this.range = range;
		setData();
	}
	void closeData() {
		if( dataSets.getSelectedItem()==null )return;
		DSDPDataSet dataset = (DSDPDataSet)dataSets.getSelectedItem();
		dataSets.removeItem(dataset);
		dataset.dispose();
	}
	void setData() {
		if( dataSets.getSelectedItem()==null )return;
		DSDPDataSet dataset = (DSDPDataSet)dataSets.getSelectedItem();
		XBTable table = dsdp.getTable();
		GTable db = dsdp.db;
		db.setDrawSelectionOnly( !drawButton.isSelected() );
		db.setColorServer(this);
		Vector data = dataset.data;
		table.getSelectionModel().setValueIsAdjusting(true);
		table.getSelectionModel().clearSelection();
		float[] age = new float[] {0f, 200f};
		age = new float[] { (float)range[0], (float)range[1] };
		colors = new Hashtable();
		Vector column = db.getCurrentColumn(0);
		for( int k=0 ; k<data.size() ; k++) {
			DSDPData d = (DSDPData)data.get(k);
			String hole = d.hole.toString();
			int hIndex = column.indexOf(hole);
			if( hIndex<0 )continue;
			boolean ok=false;
			float max = -1f;
			for( int i=0 ; i<d.age.length ; i++) {
				if( d.data[i]==0f )continue;
				if( d.age[i]>=age[0] && d.age[i]<age[1] ) {
					ok=true;
				//	if( colorButton.isSelected() ) {
						if( d.data[i]>max )max=d.data[i];
				//	} else {
				//		break;
				//	}
				}
			}
			if( !ok )continue;
			colors.put( hole, new Color(palette.getRGB(max)) );
			table.getSelectionModel().addSelectionInterval(hIndex, hIndex);
		//	for( int row=0 ; row<db.getRowCount() ; row++) {
		//		if( db.getValueAt(row,0).equals(d.hole.toString())) {
		//			table.getSelectionModel().addSelectionInterval(row, row);
		//			break;
		//		}
		//	}
		}
		table.getSelectionModel().setValueIsAdjusting(false);
		dsdp.repaintMap();
	//	db.redraw();
	}
	public Color getColor(Object o) {
		if( colors==null ) return new Color(0,0,0,0);
		Color color = (Color)colors.get(o);
		if( color==null ) {
			if( !drawButton.isSelected() )return null;
			else return new Color(0,0,0,0);
		}
		if( !colorButton.isSelected() )return new Color(0,0,0,0);
		return color;
	}
	void load() {
		DataEntry entry = (DataEntry)dataTree.getSelectionPath().getLastPathComponent();
		for( int k=0 ; k<dataSets.getItemCount() ; k++) {
			if( entry.toString().equals( dataSets.getItemAt(k).toString()) ) {
				dataSets.setSelectedIndex(k);
				return;
			}
		}
		try {
			DSDPDataSet dataset = new DSDPDataSet(dsdp, entry);
			dataSets.addItem( dataset );
			dataSets.setSelectedItem( dataset );
		} catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	void show() {
		dialog.setVisible(toggle.isSelected());
	}
	public JToggleButton getToggle() {
		return toggle;
	}
	public JTree getTree() {
		return dataTree;
	}
	static String[] categories = new String[] {
			"fauna", 
			"minerals", 
			"volcanic", 
			"carbonate", 
			"hard_ground", 
			"diagenic",
			"carbon", 
			"phys_props", 
		//	"lithology"
			};
	static String[][][] items = new String[][][] {
		{						// fauna
			{ "nan", "nannofossils" },
			{ "frm", "foraminifera" },
			{ "diat", "diatoms" },
			{ "rad", "radiolaria" },
			{ "ptr", "pteropods" },
			{ "shl", "shells" },
			{ "shl_dbr", "shell_debris" }
		},
		{						// fauna
			{ "qtz", "quartz" },
			{ "fld", "feldspar" },
			{ "zeol", "zeolite" },
			{ "pyrt", "pyrite" },
			{ "mica", "mica" },
			{ "hvy_min", "heavy_minerals" },
			{ "smect", "smectite" }
		},
		{						// fauna
			{ "volgls", "volcanic_glass" },
			{ "pumc", "pumice" },
			{ "baslt", "basalt" },
			{ "gbbro", "gabbro" },
			{ "umafic", "ultramafic" },
			{ "maf", "mafic" },
			{ "vol", "volcanic_fragments" },
			{ "palgnt", "palagonite" }
		},
		{						// fauna
			{ "calct", "calcite" },
			{ "biv", "bivalves" },
			{ "bryz", "bryozoa" },
			{ "c_alg", "calcareous_algae" },
			{ "ool", "oolite" }
		},
		{						// fauna
			{ "ferug", "ferrugenous" },
			{ "mn_nod", "manganese_nodules" },
			{ "glauc", "glauconite" },
			{ "mnoxd", "manganese_oxide" }
		},
		{						// fauna
			{ "dolmt", "dolomite" },
			{ "chrt", "chert" },
			{ "silca", "silica" },
			{ "gyps", "gypsum" },
			{ "sulf", "sulfur" },
			{ "barit", "barite" },
			{ "phspht", "phosphate" }
		},
				{											// carbon
						{ "Carbonate", "Carbonate" },
						{ "OrgCarbon", "Organic carbon" },
				},
				{											// phys_props
						{ "Porosity", "Porosity" },
//					  { "Grainsize", "Grainsize" },
//					  { "PWaveVel", "P Wave Velocity" },
/*
				},
				{											// lithology
						{ "SeabedCls", "classification" },
*/
		}};
	public void actionPerformed(ActionEvent aevt) {
		if ( aevt.getSource().equals(closeB) ) {
			toggle.setSelected(false);
			if ( colorButton.isSelected() ) {
				colorButton.doClick();
			}
			closeData();
			dsdp.demo.table.getSelectionModel().clearSelection();
//			dsdp.map.repaint();
			dialog.dispose();
		}
	}
}
