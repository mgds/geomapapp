package haxby.db.pdb;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

public class PDBOutputConfigDialog extends JDialog implements ActionListener {

	int xIndex,yIndex;
	PDB db;
	JList hidden,visible;
	Vector hiddenV, visibleV;
	public int[] indices;
	TableModel model;

	public PDBOutputConfigDialog (Dialog owner, PDB db, TableModel model, int xIndex,int yIndex){
		super(owner,"Configure Table", true);
		this.db=db;
		this.model = model;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		initVectors();
		initGUI();
	}

	public PDBOutputConfigDialog (Frame owner, PDB db, TableModel model, int xIndex,int yIndex){
		super(owner,"Configure Table", true);
		this.db=db;
		this.model = model;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		initVectors();
		initGUI();
	}

	public void initVectors(){
		hiddenV = new Vector();
		visibleV = new Vector();

		for (int i = 0; i < model.getColumnCount(); i++)
			if (i == xIndex || i == yIndex)
				visibleV.add(model.getColumnName(i));
			else
				hiddenV.add(model.getColumnName(i));
	}

	public void initGUI(){
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Not Exported"),BorderLayout.NORTH);
		hidden = new JList(hiddenV);
		hidden.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		hidden.setLayoutOrientation(JList.VERTICAL);
		JScrollPane sp = new JScrollPane(hidden);
		//sp.setPreferredSize(hidden.getPreferredSize());
		p.add(sp);
		JPanel p2 = new JPanel();
		p2.add(p);

		p = new JPanel(new GridLayout(4,1));
		JButton b = new JButton("<<");
		b.setActionCommand("<<");
		b.addActionListener(this);
		p.add(b);
		b = new JButton("<");
		b.setActionCommand("<");
		b.addActionListener(this);
		p.add(b);
		b = new JButton(">");
		b.setActionCommand(">");
		b.addActionListener(this);
		p.add(b);
		b = new JButton(">>");
		b.setActionCommand(">>");
		b.addActionListener(this);
		p.add(b);
		p2.add(p);

		p = new JPanel(new BorderLayout());
		p.add(new JLabel("Exported"), BorderLayout.NORTH);
		visible = new JList(visibleV);
		visible.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		visible.setLayoutOrientation(JList.VERTICAL);
		JScrollPane sp2 = new JScrollPane(visible);
		p.add(sp2);
		p2.add(p);

		Dimension d;
		if (hidden.getPreferredSize().width>visible.getPreferredSize().width) d = sp.getPreferredSize();
		else d = sp2.getPreferredSize();
		sp.setPreferredSize(d);
		sp2.setPreferredSize(d);

		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		p.add(p2);

		p2 = new JPanel();
		b = new JButton("OK");
		b.setActionCommand("ok");
		b.addActionListener(this);
		p2.add(b);
		b = new JButton("Cancel");
		b.setActionCommand("cancel");
		b.addActionListener(this);
		p2.add(b);
		p.add(p2,BorderLayout.SOUTH);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		getContentPane().add(p);
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if (ac.equals("ok")) ok();
		else if (ac.equals("cancel")) cancel();
		else if (ac.equals("<<")) allLeft();
		else if (ac.equals("<")) left();
		else if (ac.equals(">")) right();
		else if (ac.equals(">>")) allRight();
	}

	public void ok(){
		indices = new int[visibleV.size()];
		for (int i=0;i<visibleV.size();i++)
			for (int x=0;x<model.getColumnCount();x++)
				if (model.getColumnName(x) == visibleV.get(i))
					indices[i]=x;
		dispose();
	}

	public void cancel(){
		dispose();
	}

	public void allLeft(){
		initVectors();
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void left(){
		Object o[] = visible.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			if (o[i] == model.getColumnName(xIndex)) continue;
			if (o[i] == model.getColumnName(yIndex)) continue;
			visibleV.remove(o[i]);
			hiddenV.add(o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void right(){
		Object o[] = hidden.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			visibleV.add(o[i]);
			hiddenV.remove(o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void allRight(){
		hiddenV = new Vector();
		visibleV = new Vector();

		for (int i = 0; i < model.getColumnCount(); i++)
			visibleV.add(model.getColumnName(i));

		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}
}
