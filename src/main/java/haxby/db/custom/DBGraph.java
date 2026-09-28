package haxby.db.custom;

import haxby.db.Axes;
import haxby.db.XYGraph;
import haxby.db.XYSave;
import haxby.map.Zoomer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;

public class DBGraph {
	private UnknownDataSet ds;
	private String xTitle;
	private String yTitle;
	private DataSetGraph dsg;
	private XYGraph xyg;
	private XYSave xys;
	private JPopupMenu savePM;
	private JMenuItem closeMI;
	private JLabel helpLabel;
	private JToggleButton lassoTB;
	private JToggleButton scaleTB;
	private JDialog graphDialog;
	private JMenuBar graphMB;
	private final int xIndex;
	private final int yIndex;
	private final boolean linePlot;
	private Zoomer zoom;
	private ActionListener closeMIListener;
	private MouseAdapter popUpAdapter;
	private ActionListener autoScaleActionListener;
	private MouseAdapter autoScaleMouseListener;
	private ActionListener lassoTBListener;
	private MouseAdapter lassoMouseAdapter;
	private WindowAdapter onClosingAdapter;
	private TableModelListener tableChangeListener;
	private JScrollPane sp;

	public DBGraph(UnknownDataSet ds, int xIndex, int yIndex,
				boolean linePlot) {
		this.ds = ds;
		this.yIndex = yIndex;
		this.xIndex = xIndex;
		this.linePlot = linePlot;
		initDBGraph();
	}

	private void initDBGraph() {
		xTitle = ds.header.get(xIndex).toString();
		yTitle = ds.header.get(yIndex).toString();

		dsg = new DataSetGraph(!linePlot, xTitle,yTitle,xIndex,yIndex,ds);
		xyg = new XYGraph(dsg,0);
		xys = new XYSave(xyg);
		savePM = xys.getPopupSaveMenu();

		closeMI = new JMenuItem("Close");
		closeMIListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ds.db.close(DBGraph.this);
			}
		};
		closeMI.addActionListener( closeMIListener);
		savePM.add(closeMI);

		xyg.setScrollableTracksViewportHeight(true);
		xyg.setScrollableTracksViewportWidth(true);
		xyg.addMouseMotionListener(dsg);
		xyg.addMouseListener(dsg);

		if (linePlot) {
			zoom = new Zoomer(xyg);
			xyg.addMouseListener(zoom);
			xyg.addKeyListener(zoom);
		}

		popUpAdapter = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				tryPopUp(e);
			}
			public void mouseReleased(MouseEvent e) {
				tryPopUp(e);
			}

			public void tryPopUp(MouseEvent evt){
				if (evt.isPopupTrigger()){
					savePM.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		};

		xyg.addMouseListener( popUpAdapter );
		xyg.setAxesSides(Axes.LEFT | Axes.BOTTOM);
		sp = new JScrollPane();
		sp.setViewportView(xyg);
		xyg.setPreferredSize(new Dimension(600,200));

		if (linePlot) {
			ds.tp.add(yTitle + " vs. " + xTitle, sp);
			ds.tp.setSelectedComponent(sp);
			return;
		}

		Container c = ds.map.getParent();
		while (!(c instanceof JFrame)) c = c.getParent();
		helpLabel = new JLabel("Drag Blue lines to change graph scale.");
		helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));

		graphDialog = new JDialog((JFrame)c,yTitle + " vs. " + xTitle);
		graphDialog.getContentPane().setLayout(new BorderLayout());
		graphDialog.getContentPane().add(sp);
		graphMB = new JMenuBar();

		JLabel dividerLabel = new JLabel("  ");

		// Resize
		scaleTB = new JToggleButton(Icons.getIcon(Icons.SCALE, false));
		scaleTB.setSelectedIcon(Icons.getIcon(Icons.SCALE, true));
		scaleTB.setSelected(true);
		scaleTB.setToolTipText("Auto Resize");
		scaleTB.setBorder(null);
		autoScaleActionListener = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if (((AbstractButton)evt.getSource()).isSelected())
					{ dsg.updateRange(); xyg.setPoints(dsg, 0); xyg.repaint(); }
			}
		};

		scaleTB.addActionListener(autoScaleActionListener);
		autoScaleMouseListener = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				helpLabel.setText("Click to reset graph scales to include all data in map area.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(11.0f));
			}
			public void mouseExited(MouseEvent e) {
				helpLabel.setText("Drag Blue lines to change graph scale.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));
			}
		}; 
		scaleTB.addMouseListener( autoScaleMouseListener);
		dsg.autoResize=scaleTB;

		// Lasso
		lassoTB = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		lassoTB.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
		lassoTB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		lassoTBListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((AbstractButton)e.getSource()).isSelected()) {
					xyg.setCursor(Cursors.getCursor(Cursors.LASSO));
				} else 
					xyg.setCursor(Cursor.getDefaultCursor());
			}
		};
		lassoTB.addActionListener(lassoTBListener);

		lassoMouseAdapter = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				helpLabel.setText("Click to activate Lasso. Drag Lasso in window to select points.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(11.0f));
			}
			public void mouseExited(MouseEvent e) {
				helpLabel.setText("Drag Blue lines to change graph scale.");
				helpLabel.setFont(helpLabel.getFont().deriveFont(12.0f));
			}
		};
		lassoTB.addMouseListener( lassoMouseAdapter );

		dsg.lassoButton = lassoTB;

		graphMB.add(xys.getSaveMenu());	// Save
		graphMB.add(dividerLabel);		// Divider
		graphMB.add(scaleTB);			// Auto resize
		graphMB.add(lassoTB);			// Lasso
		graphMB.add(Box.createHorizontalStrut(8));
		graphMB.add(helpLabel);
		graphDialog.setJMenuBar(graphMB);
		graphDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		onClosingAdapter = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				ds.db.close(DBGraph.this);
			}
		};

		tableChangeListener = new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				dsg.updateRange(); 
				xyg.setPoints(dsg, 0);
				xyg.repaint();
			}
		};
		ds.tm.addTableModelListener(tableChangeListener);

		graphDialog.addWindowListener( onClosingAdapter );
		graphDialog.pack();

		ds.graphs.add(this);
	}

	public void dispose() {
		xTitle = yTitle = null;
		sp.setViewportView(null);

		savePM.remove(closeMI);
		closeMI.removeActionListener( closeMIListener );
		closeMI = null;
		savePM = null;

		xyg.removeMouseMotionListener(dsg);
		xyg.removeMouseListener(dsg);
		xyg.removeMouseListener( popUpAdapter );

		if (linePlot) {
			xyg.removeMouseListener(zoom);
			xyg.removeKeyListener(zoom);
			zoom = null;

			closeMIListener = null;
			popUpAdapter = null;
			sp = null;
			dsg = null;
			xyg = null;
			xys = null;
			return;
		}

		graphDialog.setVisible(false);
		graphDialog.getContentPane().removeAll();

		scaleTB.removeMouseListener(autoScaleMouseListener);
		scaleTB.removeActionListener(autoScaleActionListener);

		lassoTB.removeActionListener(lassoTBListener);
		lassoTB.removeMouseListener( lassoMouseAdapter );

		ds.tm.removeTableModelListener(tableChangeListener);
		tableChangeListener = null;

		graphMB.removeAll();
		graphDialog.setJMenuBar(null);
		graphDialog.removeWindowListener( onClosingAdapter );
		graphDialog.dispose();

		xyg.dispose();
		dsg.dispose();

		closeMIListener = null;
		popUpAdapter = null;
		scaleTB = null;
		graphDialog = null;
		lassoTB = null;
		helpLabel = null;
		graphMB = null;
		dsg = null;
		xyg = null;
		xys = null;
		sp = null;
	}

	public DataSetGraph getDataSetGraph() {
		return dsg;
	}

	public XYGraph getXYGraph() {
		return xyg;
	}

	public void setVisible(boolean b) {
		if (graphDialog != null)
			graphDialog.setVisible(true);
	}

	public boolean isLinePlot() {
		return linePlot;
	}
}
