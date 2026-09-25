package haxby.db.custom;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import haxby.util.GeneralUtils;

public class DBGraphDialog extends JDialog implements ActionListener {

	UnknownDataSet ds;
	JComboBox xPlot, yPlot;
	JRadioButton linePlot;
	Frame owner;
	ArrayList<String> v;

	public DBGraphDialog(Frame owner, UnknownDataSet db){
		super(owner,"Custom Graph", true);
		this.ds=db;
		this.owner = owner;
		initGUI();
	}

	public void initGUI(){
		JPanel p = new JPanel(new GridLayout(4,2));

		//get a list of column names that contain only numerical data
		v = ds.getNumericalColumns();

		p.add(new JLabel("Choose X-Axis: "));
		xPlot = new JComboBox(v.toArray());

//		***** GMA 1.6.4: Do not make the selected item the last item in the list
//		xPlot.setSelectedItem(ds.header.get(((Integer)ds.tm.indexH.get(ds.tm.indexH.size()-1)).intValue()));
//		***** GMA 1.6.4

		p.add(xPlot);

		p.add(new JLabel("Choose Y-Axis: "));
		yPlot = new JComboBox(v.toArray());
		p.add(yPlot);

		ButtonGroup bg = new ButtonGroup();
		linePlot = new JRadioButton("Line Plot",false);
		JRadioButton rb = new JRadioButton("Scatter Plot",true);
		bg.add(linePlot);
		bg.add(rb);
		p.add(linePlot);
		p.add(rb);

		JPanel p2 = new JPanel();
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
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("ok")) ok();
		else if (e.getActionCommand().equals("cancel")) cancel();
	}

	public void ok(){
		if (xPlot.getSelectedItem()== null) return;
		if (yPlot.getSelectedItem() == null) return;
	
		String xColName = GeneralUtils.html2text(xPlot.getSelectedItem().toString());
		int xIndex = 0;
		for (xIndex = 0; xIndex < ds.tm.indexH.size(); xIndex++) {
			if (ds.header.get(ds.tm.indexH.get(xIndex)).equals(xColName)) break;
		}
		
		String yColName = GeneralUtils.html2text(yPlot.getSelectedItem().toString());
		int yIndex = 0;
		for (yIndex = 0; yIndex < ds.tm.indexH.size(); yIndex++) {
			if (ds.header.get(ds.tm.indexH.get(yIndex)).equals(yColName)) break;
		}

		DBGraph graph = new DBGraph(ds, xIndex, yIndex, linePlot.isSelected());
		graph.setVisible(true);
		setVisible(false);
	}

	public void cancel(){
		setVisible(false);
	}

	public void showDialog(){
		setVisible(true);
	}

	public void disposeDialog(){
		ds = null;

		try {
			super.dispose();
		} finally {
			try {
				finalize();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
