package org.geomapapp.credit;

import haxby.util.BrowseURL;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Position;

import java.awt.event.*;
import java.awt.*;

public class CreditDialog implements WindowListener, KeyListener {
	JDialog dialog;
	JList grids;
	JList cruises;
	Credit credit;
	JToggleButton mask;
	public CreditDialog(Credit credit, JFrame owner,
				Vector c, Vector g) {
		this.credit = credit;
		cruises = new JList(c);
		grids = new JList(g);
		dialog = new JDialog(owner, "GMRT Elevation Data Sources");
		dialog.setPreferredSize(new Dimension(700,400));
		dialog.setSize(new Dimension(700,400));
		dialog.addWindowListener(this);
		init();
	}
	void setEnabled(boolean tf) {
		if( !tf ) {
			if( mask.isSelected() ) {
				mask.setSelected(false);
				mask();
			}
			dialog.setVisible(false);
		} else {
			dialog.setVisible(true);
		}
	}
	void init() {
		grids.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		cruises.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		grids.setFixedCellHeight(25);
		cruises.setFixedCellHeight(25);

		grids.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if( grids.getSelectedValue()==null ) return;
				cruises.clearSelection();
				if(mask.isSelected())credit.mask( getSelectedObject() );
			}
		});

		cruises.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if( cruises.getSelectedValue()==null ) return;
				grids.clearSelection();
				if(mask.isSelected())credit.mask( getSelectedObject() );
			}
		});
		MyCellRenderer renderer = new MyCellRenderer();
		cruises.setCellRenderer( renderer );
		grids.setCellRenderer( renderer );
		JPanel panel = new JPanel(new GridLayout(1,2));
		JScrollPane sp = new JScrollPane(cruises);
		cruises.addKeyListener(this);
		sp.addKeyListener(this);
		sp.setBorder( BorderFactory.createTitledBorder("Cruises"));
		panel.add( sp );

		sp = new JScrollPane(grids);
		grids.addKeyListener(this);
		sp.setBorder( BorderFactory.createTitledBorder("Grids"));
		panel.add( sp );

		JPanel panel0 = new JPanel(new BorderLayout());
		panel0.add(panel, "Center");

		JButton b = new JButton("Get More Info");
		try {
			b.setIcon( Flag.getInfoIcon());
		} catch(Exception e) {
		}
		panel = new JPanel(new GridLayout(1,2));
		panel.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openPage();
			}
		});
		panel.setBorder( BorderFactory.createTitledBorder("For Selection"));

		mask = new JToggleButton("Show Location");
	//	mask.setIcon( Icons.getIcon( Icons.MASK, false));
	//	mask.setSelectedIcon( Icons.getIcon( Icons.MASK, true));
		mask.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mask();
			}
		});
		panel.add(mask);

		b = new JButton("Zoom/Focus");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				credit.zoom(getSelectedObject());
			}
		});
		panel.add(b);

		panel0.add( panel, "North");
		dialog.getContentPane().add(panel0, "Center");

		OtherSources other = new OtherSources();
		dialog.getContentPane().add(other.getPanel(), "West");

		cruises.setVisibleRowCount(5);
		grids.setVisibleRowCount(5);
		dialog.pack();
	}
	void mask() {
		if( !mask.isSelected() ) credit.mask( null );
		else credit.mask( getSelectedObject() );
	}
	static String base_url = "https://www.marine-geo.org/tools/search/";
	GMAObject getSelectedObject() {
		Object o = grids.getSelectedValue();
		if( o==null ) o = cruises.getSelectedValue();
		return (GMAObject)o;
	}
	public void openPage() {
		GMAObject g = getSelectedObject();
		if( g==null ) return;
		if( g.url != null ) {
			BrowseURL.browseURL(g.url);
		} else {
			BrowseURL.browseURL(base_url+"entry.php?id="+g.name);
		}
	}
	public void setModel( Vector cruises, Vector grids) {
		GMAObject o = getSelectedObject();
		this.cruises.setListData( cruises );
		this.grids.setListData( grids );
		if( o==null )return;
		this.cruises.setSelectedValue(o, true);
		this.grids.setSelectedValue(o, true);
	}
	public void show() {
		dialog.setVisible(true);
	}
	class MyCellRenderer extends JLabel implements ListCellRenderer {
		ImageIcon[] flags;
		public MyCellRenderer() {
			super();
			flags = new ImageIcon[Flag.names.length];
			for(int i= 0 ; i <= Flag.names.length -1; i++) {
				try {
					flags[i] = Flag.getSmallFlag(i);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		public Component getListCellRendererComponent(
				JList list,
				Object value, 
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
			GMAObject o = (GMAObject)value;
			setText( o.name );
			setIcon( flags[o.nation]);
			setOpaque(true);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			return this;
		}
	}
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == dialog)	{
			if (mask.isSelected())	{
				mask.doClick();
			}
		}
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void keyPressed(KeyEvent e) {
	}
	public void keyReleased(KeyEvent e) {
	}
	public void keyTyped(KeyEvent e) {
		if ( e.getSource() instanceof JList ) {
			final JList temp = ((JList)e.getSource());
			String stringTyped = String.valueOf(e.getKeyChar());
			int s = temp.getSelectedIndex();
			int i = temp.getNextMatch(stringTyped.toLowerCase(), s, Position.Bias.Forward) ;
			System.out.println(s + "\t" + i);
			if ( i == -1 )
				i = temp.getNextMatch(stringTyped.toUpperCase(), s, Position.Bias.Forward);

			if (i == -1) return;
			
			final int select = i;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					temp.ensureIndexIsVisible(select);
					temp.setSelectedIndex(select);
				}
			});
		}
	}
}
