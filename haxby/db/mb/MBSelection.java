package haxby.db.mb;

import haxby.nav.Nearest;
import haxby.util.BrowseURL;
import haxby.util.VersionUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;

public class MBSelection implements ActionListener, ItemListener, MouseListener {
	MBTracks tracks;
	JCheckBox plot;
	JComboBox cruises;
	JScrollPane cruisesSP;
	JList cruisesList;
	DefaultListModel cruisesListModel;
	JButton downloadB;
	JPanel dialogPane;
	JLabel updateDate;

	public MBSelection( MBTracks tracks ) {
		this.tracks = tracks;
		initDialog();
	}
	void initDialog() {
		JPanel panel = new JPanel(new GridLayout(0, 1));
		
		updateDate = new JLabel("<html>Portal content last updated: " + VersionUtil.getReleaseDate("GMRT") + "</html>", SwingConstants.CENTER);
		updateDate.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		panel.add(updateDate);

		plot = new JCheckBox("View All Expeditions", true);
		panel.add(plot);

		tracks.setPlot(plot.isSelected());

		panel.setCursor(Cursor.getDefaultCursor());
		plot.addActionListener(this);

		cruisesListModel = new DefaultListModel();

		cruisesList = new JList( cruisesListModel );
		cruisesList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		cruisesList.setSelectedIndex(0);
		cruisesList.setVisibleRowCount(10);
		cruisesList.setToolTipText("double-click a leg to view profile");

		cruisesSP = new JScrollPane( cruisesList );
		cruisesSP.getViewport().setView( cruisesList );
		cruisesSP.setColumnHeaderView( new JLabel("Select An Expedition") );
		cruisesSP.setToolTipText( "double-click a leg to view profile" );
		//panel.add( cruisesSP );
		cruisesList.addMouseListener( this );

		cruises = new JComboBox( tracks.cruises );
		panel.add( cruises );
		cruises.addActionListener( this );
		cruises.addItemListener( this );

		String buttonText = "<html><body><center>" + "Cruise Info</center></body></ntml>";
		downloadB = new JButton( buttonText );
	//	downloadB = new JButton("open ping URL");
		downloadB.setEnabled( false );
		panel.add( downloadB );
		downloadB.addActionListener( this );

		dialogPane = new JPanel(new BorderLayout() );
		dialogPane.add( panel, "North" );
	}
	public void itemStateChanged(ItemEvent e) {
		if( cruises.getSelectedIndex()==-1 ) {
			downloadB.setEnabled( false );
		} else {
			downloadB.setEnabled( true );
			//System.out.println("item state changed fired");
			tracks.setSelectedCruise( cruises.getSelectedIndex() );
			MBCruise currentCruise = (MBCruise)tracks.cruises.get(cruises.getSelectedIndex());
			MBTrack currentTrack;

			if((tracks.selectedTrack==-1) || (tracks.selectedTrack > currentCruise.tracks.size()))
				currentTrack = (MBTrack) currentCruise.tracks.get(currentCruise.tracks.size()-1);
			else
				currentTrack = (MBTrack) currentCruise.tracks.get(tracks.selectedTrack);

			double zoom = tracks.map.getZoom();
			Nearest nearest = new Nearest(null, 0, 0, Math.pow(2./zoom, 2) );

			if(tracks.p == null)
				currentTrack.nav.nearestPoint(currentTrack.getBounds().getCenterX(), currentTrack.getBounds().getCenterY(), nearest);
			else
				currentTrack.nav.nearestPoint(tracks.p.x, tracks.p.y, nearest);

			tracks.updateDisplay(currentCruise, currentTrack, nearest);
		}
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==cruises ) {
			tracks.setSelectedCruise( cruises.getSelectedIndex() );
		} else if( evt.getSource()==plot ) {
			tracks.setPlot(plot.isSelected());
		} else if( evt.getSource()==downloadB ) {
			String name = ((MBCruise)cruises.getSelectedItem()).toString();
		//	String name = ((MBCruise)cruises.getSelectedItem()).toString().toUpperCase();
		//	if( name.indexOf( "_" ) >=0 ) name = name.substring(0, name.indexOf( "_" ));
			System.out.println( name);
			BrowseURL.browseURL("https://www.marine-geo.org/tools/search/entry.php?id="+name);
		}
	}
	public JComponent getDialog() {
		return dialogPane;
	}

	public void mouseClicked(MouseEvent e) {
		tracks.setSelectedCruise( cruisesList.getSelectedIndex() );
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void setSelectedCruiseIndex( int i )
	{
		cruises.setSelectedIndex(i);
	}
}
