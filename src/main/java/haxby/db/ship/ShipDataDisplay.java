package haxby.db.ship;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import haxby.db.XYGraph;
import haxby.db.mgg.MGG;
import haxby.db.mgg.MGGData;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.PathUtil;

public class ShipDataDisplay implements ActionListener, MouseListener {

	protected Ship tracks;
	protected XMap map;
	protected JList cruiseL;
	protected MGGData data;
	protected XYGraph[] xy;


	protected JRadioButton[] selectRB;
	protected JPanel dialog;
	protected JPanel panel;
	protected JScrollPane scrollPane;
	protected int dataIndex;
	protected String loadedLeg;
	//TODO
	static String SHIP_PATH = PathUtil.getPath("PORTALS/SHIP_PATH",
			MapApp.BASE_URL+"/data/portals/ship/");

	protected String selectedLeg;

	private class myListRenderer extends DefaultListCellRenderer 
	{
		public Component getListCellRendererComponent(JList list, 
				Object value,
				int index, 
				boolean isSelected,
				boolean cellHasFocus){

			JSplitPane renderPane = null;

				if(value instanceof String)
					if(((String)value).equalsIgnoreCase("none"))
						return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

					if (value instanceof String) {
					String splitValue[] = ((String)value).split("\\s", 2);	
					renderPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JLabel(splitValue[0]), new JLabel("Info: "+ splitValue[1] ));	
					if (isSelected) {
						renderPane.setBackground(new Color(200, 230, 255));
					} else {
						renderPane.setBackground(list.getBackground());
						setForeground(list.getForeground());
					}

				}
				return renderPane;
			}
	}

	public ShipDataDisplay(Ship tracks, XMap map) {
		this.tracks = tracks;
		this.map = map;
		cruiseL = new JList(tracks.model);
		initPanel();
		xy = null;
		data = null;
		dataIndex = -1;
		selectedLeg = null;
	}

	void initPanel() {
		//cruiseL.setCellRenderer(new myListRenderer());
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout( panel1, BoxLayout.Y_AXIS ) );
		panel1.add( new JScrollPane(cruiseL) );
		cruiseL.addMouseListener(this);
		panel1.setBorder( BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder( Color.black),
			BorderFactory.createEmptyBorder(1,1,1,1) ));


		dialog = new JPanel(new GridLayout( 1, 0 ));
		dialog.add(panel1);

		/*scrollPane = new JScrollPane( new JComponent() {
			public Dimension getPreferredSize() {
				return new Dimension(600, 100);
			}
			public void paint(Graphics g) {
				g.drawString("no track loaded", 20, 30);
			}
		});*/

		// GMA 1.6.2: Tool tip text
		//scrollPane.setToolTipText("Right-click to digitize");

		panel = new JPanel(new BorderLayout());
		panel.add(dialog, "Center");
		//panel.add(scrollPane, "Center");
	}

	public void showInfo() {
		/*print out the info field from the xml file*/
		//TODO
		String leg = (String)cruiseL.getSelectedValue();
	}
	
	public void mouseClicked(MouseEvent e) {
		if ( e.getSource() == cruiseL ) {
			if ( e.getClickCount() >= 2 ) {

			}
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

}