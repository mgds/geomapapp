package haxby.db.pdb;

import haxby.db.Axes;
import haxby.db.XYGraph;
import haxby.db.XYSave;
import haxby.map.Zoomer;
import haxby.util.XBTable;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.TableModel;

import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;

public class PDBGraphDialog extends JDialog implements ActionListener {
	Vector index;
	PDB db;
	JComboBox xPlot, yPlot;
	JRadioButton plotCompiledChem;
	Frame owner;

	public PDBGraphDialog(Frame owner, PDB db){
		super(owner,"Custom Graph", true);
		this.db=db;
		this.owner = owner;
		initGUI();
	}

	public void initGUI(){
		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		JPanel p = new JPanel(new GridLayout(2,2));
		String[] str = new String[db.sModel.getColumnCount(true)];
		for (int i = 0; i < str.length; i ++)
			str[i] = db.sModel.getColumnName(i, true);

		p.add(new JLabel("Choose X-Plot: "));
		xPlot = new JComboBox(str);
		p.add(xPlot);

		p.add(new JLabel("Choose Y-Plot: "));
		yPlot = new JComboBox(str);
		p.add(yPlot);
		p2.add(p, BorderLayout.NORTH);

		ButtonGroup bg = new ButtonGroup();
		p = new JPanel(new GridLayout(0,1)); 
		plotCompiledChem = new JRadioButton("Compiled Chemical");
		JRadioButton rb = new JRadioButton("Analysis");

		String selectedTab = db.dataDisplay.getTitleAt(db.dataDisplay.getSelectedIndex());
		if (selectedTab.equals("Analyses")) {
			rb.setSelected(true);
		} else {
			plotCompiledChem.setSelected(true);
		}

		bg.add(plotCompiledChem);
		bg.add(rb);
		p.add(new JLabel("Plot from: "));
		p.add(plotCompiledChem);
		p.add(rb);
		p2.add(p);

		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add(p2);

		p = new JPanel();
		JButton b = new JButton("OK");
		b.addActionListener(this);
		b.setActionCommand("ok");
		getRootPane().setDefaultButton(b);
		p.add(b);
		b = new JButton("Cancel");
		b.addActionListener(this);
		b.setActionCommand("cancel");
		p.add(b);
		getContentPane().add(p,BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("ok")) ok();
		else if (e.getActionCommand().equals("cancel")) cancel();
	}

	public void ok() {
		if (xPlot.getSelectedItem()== null) return;
		if (yPlot.getSelectedItem() == null) return;
		int yIndex = yPlot.getSelectedIndex();
		int xIndex = xPlot.getSelectedIndex();

		XBTable table = plotCompiledChem.isSelected() ? db.sTable :  db.aTable;
		TableModel model = table.getModel();

//		if (linePlot.isSelected())
//			for (int i = 0; i < ds.tm.getRowCount(); i++) {
//				float t;
//				try {
//					t = Float.parseFloat(((Vector)ds.rowData.get(((Integer)ds.tm.displayIndex.get(i)).intValue())).get(xIndex).toString());
//				} catch (Exception ex) {
//					t = Float.NaN;
//				}
//				if (Float.isNaN(t)) return;
//			}

		String xTitle = db.sModel.getColumnName(xIndex, true);
		String yTitle = db.sModel.getColumnName(yIndex, true);

		final PDBDataSetGraph dsg = new PDBDataSetGraph(db, xTitle,yTitle,xIndex,yIndex,table); 
		final XYGraph xyg = new XYGraph(dsg,0);

		dsg.setParent(xyg);
		table.getModel().addTableModelListener(dsg);
		table.getSelectionModel().addListSelectionListener(dsg);

		XYSave xys = new XYSave(xyg);
		final JPopupMenu pm = xys.getPopupSaveMenu();
		JMenuItem close = new JMenuItem("Close");
//		close.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				ds.db.close(xyg);
//			}
//		});
		pm.add(close);
		xyg.setScrollableTracksViewportHeight(true);
		xyg.setScrollableTracksViewportWidth(!plotCompiledChem.isSelected());
		Zoomer zoom = new Zoomer(xyg);
		xyg.addMouseMotionListener(dsg);
		xyg.addMouseListener(dsg);
		xyg.addMouseListener(zoom);
		xyg.addKeyListener(zoom);
		xyg.addMouseListener( new MouseListener() {
			public void mouseClicked(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }

			public void mousePressed(MouseEvent e) {
				tryPopUp(e);
			}
			public void mouseReleased(MouseEvent e) {
				tryPopUp(e);
			}
			public void tryPopUp(MouseEvent evt){
				if (evt.isPopupTrigger()){
					pm.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		} );

		xyg.setAxesSides(Axes.LEFT | Axes.BOTTOM);
		JScrollPane sp =new JScrollPane();
		sp.setViewportView(xyg);
		xyg.setPreferredSize(new Dimension(600,200));
		Container c = db.map;
		while (!(c instanceof JFrame)) c = c.getParent();
		final JLabel helpLabel = new JLabel("Drag Blue lines to change graph scale.");
		helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));
		final JDialog jf = new JDialog((JFrame)c,yPlot.getSelectedItem() + " vs. " + xPlot.getSelectedItem() + " in " + 
				(plotCompiledChem.isSelected() ? "Samples" : "Analyses"));
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(sp);
		JMenuBar mb = new JMenuBar();

		JLabel dividerLabel = new JLabel("  ");
		// Resize
		JToggleButton b = new JToggleButton(Icons.getIcon(Icons.SCALE, false));
		b.setSelectedIcon(Icons.getIcon(Icons.SCALE, true));
		b.setSelected(true);
		//b.setToolTipText("Auto Resize");
		b.setBorder(null);
		ActionListener ac = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if (((AbstractButton)evt.getSource()).isSelected())
					{ dsg.updateRange(); xyg.setPoints(dsg, 0); xyg.repaint(); }
			}
		};
		b.addActionListener(ac);
		b.addMouseListener( new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				helpLabel.setText("Click to reset graph scales to include all data in map area.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(11.0f));
			}
			public void mouseExited(MouseEvent e) {
				helpLabel.setText("Drag Blue lines to change graph scale.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));
			}
		});
		dsg.autoResize=b;

		// Lasso
		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		tb.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
//		tb.setSelected(true);
		tb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((AbstractButton)e.getSource()).isSelected()) {
					xyg.setCursor(Cursors.getCursor(Cursors.LASSO));
				} else {
					xyg.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
		tb.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		tb.addMouseListener( new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				helpLabel.setText("Click to activate Lasso. Drag Lasso on graph to select points.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(11.0f));
			}
			public void mouseExited(MouseEvent e) {
				helpLabel.setText("Drag Blue lines to change graph scale.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));
			}
		} );
		dsg.lassoButton=tb;

		mb.add(xys.getSaveMenu()); 	// Save
		mb.add(dividerLabel);		// Divider
		mb.add(b);					// Auto Resize
		mb.add(tb);					// Lasso
		mb.add(Box.createHorizontalStrut(8));
		mb.add(helpLabel);
		jf.setJMenuBar(mb);
		jf.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		jf.addWindowListener( new WindowListener() {
			public void windowActivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowClosing(WindowEvent e) {
				db.graphs.add(xyg); 
				dsg.table.getModel().removeTableModelListener(dsg);
				dsg.table.getSelectionModel().removeListSelectionListener(dsg);
				jf.dispose();
			}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}
			});
		jf.pack();
		jf.setVisible(true);
		db.graphs.add(xyg);
		dispose();
	}

	public void cancel() {
		dispose();
	}

	public void showDialog() {
		setVisible(true);
	}

	public void disposeDialog() {
		dispose();
	}
}
