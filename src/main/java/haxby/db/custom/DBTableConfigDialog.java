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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class DBTableConfigDialog extends JDialog implements ActionListener, WindowListener {
	UnknownDataSet ds;
	JList hidden,visible;
	Vector<String> hiddenV, visibleV;

	public DBTableConfigDialog(JDialog owner, UnknownDataSet ds){
		super(owner,"Configure Table", true);
		this.ds=ds;
		initVectors();
		initGUI();
	}

	public void initVectors(){
		hiddenV = new Vector<String>();
		visibleV = new Vector<String>();
		Vector<String> v = (Vector<String>) ds.header.clone();
		for (int i = 0; i < ds.tm.indexH.size(); i++){
			visibleV.add(ds.header.get(ds.tm.indexH.get(i)));
			v.remove(visibleV.get(visibleV.size()-1));
		}
		v.trimToSize();
		for (int i =0; i < v.size(); i++)
			hiddenV.add(v.get(i));
	}

	public void initGUI(){
		this.addWindowListener(this);
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Hidden"),BorderLayout.NORTH);
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
		p.add(new JLabel("Visible"), BorderLayout.NORTH);
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
		if (visibleV.size()==0) return;

		ds.tm.indexH.removeAllElements();

		for (int i = 0; i < ds.header.size(); i++)
			for (int j = 0; j < visibleV.size(); j++)
				if (ds.header.get(i) == visibleV.get(j))
					ds.tm.indexH.add(i);

		ds.cst=null;

		ds.tm.fireTableStructureChanged();
		dispose();
	}

	public void cancel(){
		dispose();
	}

	public void allLeft(){
		visibleV=new Vector<String>();
		hiddenV=new Vector<String>(ds.header.size());
		for (int i = 0; i < ds.header.size(); i++)
			hiddenV.add(i, ds.header.get(i));
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void left(){
		Object o[] = visible.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			visibleV.remove(o[i]);
			hiddenV.add((String) o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void right(){
		Object o[] = hidden.getSelectedValues();
		for (int i = 0; i < o.length; i++) {
			visibleV.add((String) o[i]);
			hiddenV.remove(o[i]);
		}
		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void allRight(){
		visibleV=new Vector<String>(ds.header.size());
		hiddenV=new Vector<String>();
		for (int i = 0; i < ds.header.size(); i++)
			visibleV.add(i, ds.header.get(i));

		hidden.setListData(hiddenV);
		visible.setListData(visibleV);
	}

	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		cancel();
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
}
