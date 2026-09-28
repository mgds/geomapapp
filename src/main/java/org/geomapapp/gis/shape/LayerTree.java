package org.geomapapp.gis.shape;

import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import org.geomapapp.util.Icons;
import org.geomapapp.util.ParseLink;

public class LayerTree implements TreeCellRenderer {
	JTree layerTree;
	LayerEntry rootEntry;
	LayerEntry currentEntry;
	int n_null = 0;
	JPopupMenu pop;
	JCheckBoxMenuItem on;
	JLabel popLabel;
	JLabel renderer;
	Icon onIcon = Icons.getIcon(Icons.LIGHTBULB, false);
	Icon offIcon = Icons.getDisabledIcon(Icons.LIGHTBULB, false);
	public LayerTree() throws IOException {
		this("layers.xml");
	}
	public LayerTree(String layersXMLName) throws IOException {
		init(layersXMLName);
	}
	void init(String layersXMLName) throws IOException {
	//	Vector layers = ParseLink.parse(URLFactory.url(org.geomapapp.io.GMARoot.ROOT_URL +"/Layers/layers.xml"));
		Vector layers = ParseLink.parse(URLFactory.url("file:///home/bill/geomapapp/GMA/Layers/" + layersXMLName) );
	//	ParseLink.printProperties( layers, 0);
		rootEntry = new LayerEntry(null, new Vector(), "layers", "", null);
		layers = ParseLink.getProperties( layers, "layer");
		for( int i=0 ; i<layers.size() ; i++) {
			addLayers( (Vector)layers.get(i), rootEntry);
		}
		layerTree = new JTree(rootEntry);
		layerTree.setRootVisible(false);
		layerTree.addMouseListener( new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger()) {
					popup(e);
				}
			}
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger()) {
					popup(e);
				}
			}
		});
		renderer = new JLabel("");
		renderer.setOpaque(true);
		layerTree.setCellRenderer(this);
		layerTree.setBackground( new Color(225,225,225) );
		renderer.setBackground( new Color(215,215,255) );
		pop = new JPopupMenu();
		popLabel = new JLabel("");
		popLabel.setHorizontalAlignment( JLabel.CENTER );
		on = new JCheckBoxMenuItem( "display");
		pop.add( popLabel );
		pop.addSeparator();
		pop.add( on );
		pop.add( "edit properties");
		on.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( currentEntry!=null) currentEntry.setVisible( on.isSelected() );
				layerTree.repaint();
			}
		});
	}
	
	void popup(MouseEvent e) {
		currentEntry = (LayerEntry)layerTree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent();
		on.setSelected( currentEntry.isVisible());
		popLabel.setText( currentEntry.toString() );
		pop.show( layerTree, e.getX(), e.getY() );
	//	entry.setVisible( on.isSelected() );
	}

	public Component getTreeCellRendererComponent(JTree tree,
					Object value,
					boolean selected,
					boolean expanded,
					boolean leaf,
					int row,
					boolean hasFocus) {
		LayerEntry entry = (LayerEntry)value;
		if( entry.isVisible() ) renderer.setIcon( onIcon );
		else renderer.setIcon( offIcon );
		renderer.setText( value.toString() );
		renderer.setOpaque( selected );
		if( selected ) {
			renderer.setBorder( BorderFactory.createLineBorder( Color.black ) );
		} else if(hasFocus) {
			renderer.setBorder( BorderFactory.createLineBorder( Color.pink ) );
		} else {
			renderer.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		}
		return renderer;
	}
	void addLayers(Vector layers, LayerEntry parent) {
		String name = (String)ParseLink.getProperty( layers, "name");
		if( name==null ) name = "untitled_"+(++n_null);
		String description = (String)ParseLink.getProperty( layers, "description");
		if( description==null )description="";
		String url = (String)ParseLink.getProperty( layers, "url");
		Vector children = (url==null)
				? new Vector()
				: null;
		LayerEntry entry = new LayerEntry( parent, children, name, description, url);
		parent.getChildren().add( entry );
		Vector props = ParseLink.getProperties( layers, "layer");
		for( int k=0 ; k<props.size() ; k++) {
			Vector prop = (Vector)props.get(k);
			addLayers( prop, entry);
		}
	}
	public LayerEntry getSelectedEntry() {
		if( layerTree.getSelectionPath()==null )return null;
		LayerEntry layer = (LayerEntry)layerTree.getSelectionPath().getLastPathComponent();
		return layer;
	}
	public JTree getLayerTree() {
		return layerTree;
	}
	public static void main(String[] args) {
		try {
			LayerTree model = new LayerTree();
			JFrame frame = new JFrame("layers");
			frame.getContentPane().add( new JScrollPane(model.getLayerTree()));
			frame.pack();
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
