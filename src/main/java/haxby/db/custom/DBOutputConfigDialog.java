package haxby.db.custom;

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

public class DBOutputConfigDialog  extends JDialog implements ActionListener {

	int xIndex,yIndex;
	UnknownDataSet ds;
	JList hidden,visible;
	Vector<String> hiddenV;
	Vector<String> visibleV;
	public int[] indices;

	public DBOutputConfigDialog (Dialog owner, UnknownDataSet ds,int xIndex,int yIndex){
		super(owner,"Configure Table", true);
		this.ds=ds;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		initVectors();
		initGUI();
	}

	public DBOutputConfigDialog (Frame owner, UnknownDataSet ds,int xIndex,int yIndex){
		super(owner,"Configure Table", true);
		this.ds=ds;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		initVectors();
		initGUI();
	}

	public void initVectors(){
		hiddenV = new Vector<String>();
		visibleV = new Vector<String>();
		Vector<String> v = (Vector<String>) ds.header.clone();
		visibleV.add(v.get(ds.latIndex));
		visibleV.add(v.get(ds.lonIndex));
		visibleV.add(v.get(xIndex));
		visibleV.add(v.get(yIndex));
		for (int i=0;i<visibleV.size();i++)
			for (int z=0;z<v.size();z++)
				if (visibleV.get(i).equals(v.get(z)))
					v.remove(z);
		v.trimToSize();
		for (int i =0; i < v.size(); i++)
			hiddenV.add(v.get(i));
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

	public void ok() {
		indices = new int[visibleV.size()];
		Vector<String> v = (Vector<String>) ds.header.clone();
		for (int i=0;i<visibleV.size();i++)
			for (int x=0;x<v.size();x++)
				if (v.get(x).equals(visibleV.get(i)))
					indices[i]=x;
		dispose();
	}

	public void cancel() {
		dispose();
	}

	public void allLeft() {
		visibleV=new Vector<String>();
		hiddenV=new Vector<String>(ds.header.size());
		for (int i = 0; i < ds.header.size(); i++)
			hiddenV.add(i, ds.header.get(i));
		hiddenV.remove(ds.header.get(xIndex));
		hiddenV.remove(ds.header.get(yIndex));
		visibleV.add(ds.header.get(xIndex));
		visibleV.add(ds.header.get(yIndex));
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void left() {
		Object o[] = visible.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			if (o[i].equals(ds.header.get(xIndex))) continue;
			if (o[i].equals(ds.header.get(yIndex))) continue;
			visibleV.remove(o[i]);
			hiddenV.add((String) o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void right() {
		Object o[] = hidden.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			visibleV.add((String) o[i]);
			hiddenV.remove(o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void allRight() {
		visibleV=new Vector<String>(ds.header.size());
		hiddenV=new Vector<String>();
		for (int i = 0; i < ds.header.size(); i++)
			visibleV.add(i, ds.header.get(i));

		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}
}
