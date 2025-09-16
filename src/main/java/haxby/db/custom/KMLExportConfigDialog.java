/**
 * 
 */
package haxby.db.custom;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class KMLExportConfigDialog extends JDialog 
					implements ActionListener, WindowListener, TreeSelectionListener {

	private static final String[] COMMON_UNITS = {
		"\u00B0", // degrees
		"dms", // degrees minutes seconds
		"meters",
		"\u00B0C", //degrees celcius 
		"\u00B0F" //degrees FAHRENHEIT
	};

	boolean export;
	String dbName;
	Vector<String> fields;
	Vector<JPanel> scalePanels = new Vector<JPanel>();
	JTextField[] names;
	JComboBox[] units;
	JRadioButton[] placeMarkName;
	JCheckBox[] image;
	JTree tree;
	JComponent fieldConfig;
	JComponent currentComponent;
	DefaultMutableTreeNode data;
	int imageIndex;

	public KMLExportConfigDialog(JFrame owner, String dbName, Vector<String> fields, int imageIndex) {
		super(owner, "Exporting to KML: Field Config", true);
		this.fields = fields;
		this.dbName = dbName;
		this.imageIndex = imageIndex;
		initGUI();
	}

	private void initGUI() {
		addWindowListener(this);

		names = new JTextField[fields.size()]; 
		units = new JComboBox[fields.size()];
		placeMarkName = new JRadioButton[fields.size()];
		image = new JCheckBox[fields.size()];

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

		JPanel headerP = new JPanel(new GridLayout(0,1));
		JPanel nameP = new JPanel(new GridLayout(0,1));
		JPanel unitsP = new JPanel(new GridLayout(0,1));
		JPanel placeP = new JPanel(new GridLayout(0,1));
		JPanel imageP = new JPanel(new GridLayout(0,1));

		headerP.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		nameP.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		unitsP.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		placeP.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		imageP.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		headerP.add(new JLabel("Header"));
		nameP.add(new JLabel("Full Name"));
		unitsP.add(new JLabel("Units"));
		placeP.add(new JLabel("<html>Select<br>placemark<br>name</html>"));
		imageP.add(new JLabel("<html>Image<br>link</html>"));

		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < fields.size(); i++) {
			headerP.add(new JLabel(fields.get(i).toString()));
			names[i] = new JTextField(fields.get(i).toString());
			names[i].setText(processName(names[i].getText()));
			nameP.add(names[i]);
			units[i] = new JComboBox(COMMON_UNITS);
			units[i].setEditable(true);
			units[i].setSelectedItem(processUnits(fields.get(i).toString()));
			unitsP.add(units[i]);
			placeMarkName[i] = new JRadioButton();
			placeP.add(placeMarkName[i]);
			bg.add(placeMarkName[i]);
			image[i] = new JCheckBox();
			imageP.add(image[i]);
		}

		int maxH = Math.max(headerP.getHeight(), nameP.getHeight());
		maxH = Math.max(unitsP.getHeight(), maxH);
		maxH = Math.max(placeP.getHeight(), maxH);
		if (imageIndex != -1)
			maxH = Math.max(imageP.getHeight(), maxH);

		headerP.setSize(headerP.getWidth(), maxH);
		nameP.setSize(nameP.getWidth(), maxH);
		unitsP.setSize(unitsP.getWidth(), maxH);
		placeP.setSize(placeP.getWidth(), maxH);
		imageP.setSize(imageP.getWidth(), maxH);

		p.add(headerP);
		p.add(nameP);
		p.add(unitsP);
		p.add(placeP);
		if (imageIndex != -1)
			p.add(imageP);

		int c = guessName(fields);
		if (c != -1)
			placeMarkName[c].setSelected(true);
//		if (imageIndex != -1)
//			image[imageIndex].setSelected(true);

		JScrollPane sp = new JScrollPane(p);
		fieldConfig = sp;
		currentComponent = fieldConfig;
		getContentPane().add(fieldConfig,BorderLayout.CENTER);

		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createRaisedBevelBorder());
		JButton b = new JButton("Click to Add Scaled Data");
		b.setActionCommand("addScale");
		b.addActionListener(this);
		p.add(b, BorderLayout.NORTH);
		DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("Unscaled Data");
		DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("Data");
		node2.add(node1);
		node1 = new DefaultMutableTreeNode(dbName);
		node1.add(node2);
		data = node2;
		tree = new JTree(node1){
			public Dimension getPreferredScrollableViewportSize() {
				return new Dimension(200,0);
			}
		};;
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.expandRow(1);
		tree.addTreeExpansionListener(new TreeExpansionListener() {
			public void treeExpanded(TreeExpansionEvent event) {
			}
			public void treeCollapsed(TreeExpansionEvent event) {
				tree.expandPath(event.getPath());
			}
		});
		sp = new JScrollPane(tree);
		p.add(sp);

		getContentPane().add(p,BorderLayout.EAST);

		p = new JPanel();
		b = new JButton("Back");
		b.addActionListener(this);
		b.setActionCommand("back");
		p.add(b);
		b = new JButton("Export");
		b.setActionCommand("export");
		b.addActionListener(this);
		p.add(b);
		p.setAlignmentX(JComponent.RIGHT_ALIGNMENT);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(p, BorderLayout.EAST);
		getContentPane().add(p2, BorderLayout.SOUTH);

		pack();
		setSize(Math.min(800, getWidth() + 20), Math.min(600, getHeight() + 20));
		setLocationRelativeTo(haxby.map.MapApp.anchor);
		setVisible(true);
	}

	private int guessName(Vector<String> fields) {
		for (int i = 0; i < fields.size(); i++) {
			String name = fields.get(i).toString();
			if (name.toLowerCase().indexOf("name") != -1) return i;
			else if (name.toLowerCase().indexOf("label") != -1) return i;
			else if (name.toLowerCase().indexOf("desc")  != -1) return i;
		}
		return -1;
	}

	private String processUnits(String name) {
		if (name.toLowerCase().indexOf("lat") != -1) return COMMON_UNITS[0];
		else if (name.toLowerCase().indexOf("lon") != -1) return COMMON_UNITS[0];
		else if (name.toLowerCase().indexOf("temp") != -1) return COMMON_UNITS[3];
		else if (name.toLowerCase().indexOf("depth") != -1) return COMMON_UNITS[2];
		else if (name.toLowerCase().endsWith("_m")) return COMMON_UNITS[2];
		return "";
	}

	/**
	 * As of now just returns name.  Should try to format name to
	 * be more readable
	 * @param name
	 * @return
	 */
	private String processName(String name) {
		if (name.toLowerCase().startsWith("lat"))
			return "Latitude";
		else if (name.toLowerCase().startsWith("lon"))
			return "Longitude";
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private void export() {
		export = true;
		dispose();
	}

	private void back() {
		export = false;
		dispose();
	}

	private void addScale() {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Scaled by");

		getContentPane().remove(currentComponent);

		JPanel scalePanel = new ScalePanel(node); 
		currentComponent = scalePanel;
		getContentPane().add(scalePanel);
		scalePanels.add(scalePanel);

		((DefaultTreeModel) tree.getModel()).insertNodeInto(node, data, data.getChildCount());
		tree.setSelectionPath(new TreePath(node.getPath()));

		getContentPane().validate();
	}

	public int getNameIndex() {
		for (int i = 0; i < placeMarkName.length; i++)
			if (placeMarkName[i].isSelected())
				return i;
		return -1;
	}

	public Vector<JPanel> getScales() {
		return scalePanels;
	}

	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
								tree.getLastSelectedPathComponent();

		// If nothing is selected, or it is not a data child, or it is the Unscaled Data
		// load the field config panel
		if (node == null || !data.isNodeChild(node) || data.getChildAt(0) == node) {
			if (currentComponent == fieldConfig)
				return;
			getContentPane().remove(currentComponent);
			getContentPane().add(fieldConfig);
			currentComponent = fieldConfig;
		} else {
			int i = data.getIndex(node) - 1;
			JPanel p = scalePanels.get(i);
			if (currentComponent == p) return;
			getContentPane().remove(currentComponent);
			getContentPane().add(p);
			currentComponent = p;
		}

		validate();
		currentComponent.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("export")) export();
		else if (e.getActionCommand().equals("back")) back();
		else if (e.getActionCommand().equals("addScale")) addScale();
	}

	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		back();
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

	public class ScalePanel extends JPanel {
		DefaultMutableTreeNode node;
		JCheckBox sizeCB;
		JCheckBox colorCB;
		JComboBox name;
		JComboBox size;
		JComboBox color;
		public ScalePanel(DefaultMutableTreeNode aNode) {
			super();
			JPanel p3 = new JPanel();
			p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
			add(p3);
			node = aNode;

			JPanel p = new JPanel();
			p.add(new JLabel("Scaled Placemark's Name: "));
			name = new JComboBox(fields);
			name.setSelectedIndex(0);
			p.add(name);
			JPanel p2 = new JPanel(new BorderLayout());
			p2.add(p, BorderLayout.WEST);
			p3.add(p2);

			p = new JPanel();
			colorCB = new JCheckBox("Scale Color by ");
			colorCB.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					color.setEnabled(colorCB.isSelected());
					String str = getScaleName();
					node.setUserObject(str);
					((DefaultTreeModel) tree.getModel()).nodeChanged(node);
					tree.scrollPathToVisible(new TreePath(node.getPath()));
				}
			});
			p.add(colorCB);
			color = new JComboBox(fields);
			color.setSelectedIndex(0);
			color.setEnabled(false);
			color.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = getScaleName();
					node.setUserObject(str);
					((DefaultTreeModel) tree.getModel()).nodeChanged(node);
					tree.scrollPathToVisible(new TreePath(node.getPath()));
				}
			});
			p.add(color);
			p2 = new JPanel(new BorderLayout());
			p2.add(p, BorderLayout.WEST);
			p3.add(p2);

			p = new JPanel();
			sizeCB = new JCheckBox("Scale Size by ");
			sizeCB.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					size.setEnabled(sizeCB.isSelected());

					String str = getScaleName();
					node.setUserObject(str);
					((DefaultTreeModel) tree.getModel()).nodeChanged(node);
					tree.scrollPathToVisible(new TreePath(node.getPath()));
				}
			});
			p.add(sizeCB);
			size = new JComboBox(fields);
			size.setSelectedIndex(0);
			size.setEnabled(false);
			size.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = getScaleName();
					node.setUserObject(str);
					((DefaultTreeModel) tree.getModel()).nodeChanged(node);
					tree.scrollPathToVisible(new TreePath(node.getPath()));
				}
			});
			p.add(size);
			p2 = new JPanel(new BorderLayout());
			p2.add(p, BorderLayout.WEST);
			p3.add(p2);

			p = new JPanel();
			JButton b = new JButton("Remove");
			b.setHorizontalAlignment(JButton.LEFT);
			b.addActionListener(new ActionListener () {
				public void actionPerformed(ActionEvent e) {
					scalePanels.remove(currentComponent);
					((DefaultTreeModel) tree.getModel()).removeNodeFromParent(node);
					tree.scrollPathToVisible(new TreePath(data.getPath()));
					
					getContentPane().remove(currentComponent);
					getContentPane().add(fieldConfig);
					currentComponent = fieldConfig;
				}
			});
			p.add(b);
			p2 = new JPanel(new BorderLayout());
			p2.add(p, BorderLayout.WEST);
			p3.add(p2);
		}

		public String getScaleName() {
			String str = "Scaled by ";
			if (colorCB.isSelected()) str += color.getSelectedItem();
			if (colorCB.isSelected() && sizeCB.isSelected()) str += " & ";
			if (sizeCB.isSelected()) str += size.getSelectedItem();
			return str;
		}
	}

	public static void main(String[] args) {
		Vector<String> v = new Vector<String>();
		v.add("Lat");
		v.add("Lon");
		v.add("Depth");
		v.add("Magnitude");
		v.add("Label");
		new KMLExportConfigDialog(null, "abcdefghijklmnopqrstuvwxyz", v,-1);
	}
}