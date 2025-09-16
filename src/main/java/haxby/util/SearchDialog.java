package haxby.util;

import javax.swing.*;
import java.awt.*;

public class SearchDialog extends JPanel {
	String[] dataTypes;
	JTextField west, east, south, north;
	JToggleButton[] selectedData;;
	public SearchDialog() {
		super(new GridLayout(0,2,1,1));
	//	setBackground(Color.black);
		dataTypes = new String[] { "topography",
					"gravity",
					"magnetics" };
		JLabel label = new JLabel("West: ");
		label.setOpaque(true);
		label.setHorizontalAlignment(label.RIGHT);
		add(label);
		west = new JTextField("0.0");
		add(west);
		label = new JLabel("East: ");
		label.setHorizontalAlignment(label.RIGHT);
		label.setOpaque(true);
		add(label);
		east = new JTextField("360.0");
		add(east);
		label = new JLabel("South: ");
		label.setOpaque(true);
		label.setHorizontalAlignment(label.RIGHT);
		add(label);
		south = new JTextField("-90.0");
		add(south);
		label = new JLabel("North: ");
		label.setOpaque(true);
		label.setHorizontalAlignment(label.RIGHT);
		add(label);
		north = new JTextField("90.0");
		add(north);
		label = new JLabel("Data Types:");
		label.setOpaque(true);
		label.setHorizontalAlignment(label.CENTER);
		add(label);
	
		selectedData = new JToggleButton[dataTypes.length];;
		for( int i=0 ; i<dataTypes.length ; i++) {
			selectedData[i] = new JToggleButton(dataTypes[i]);
			selectedData[i].setSelected(true);
			add(selectedData[i]);
		}
	}
	public double[] getWESN() {
		double[] wesn = new double[4];
		wesn[0] = Double.parseDouble(west.getText());
		wesn[1] = Double.parseDouble(east.getText());
		wesn[2] = Double.parseDouble(south.getText());
		wesn[3] = Double.parseDouble(north.getText());
		return wesn;
	}
	public String[] getDataTypes() {
		int n=0;
		for( int i=0 ; i<selectedData.length ; i++) {
			if(selectedData[i].isSelected())n++;
		}
		String[] types = new String[n];
		n=0;
		for( int i=0 ; i<selectedData.length ; i++) {
			if(selectedData[i].isSelected()) {
				types[n]=dataTypes[i];
				n++;
			}
		}
		return types;
	}
	public int showDialog(Component comp) {
		return JOptionPane.showConfirmDialog( comp, this, "Search",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
	}
	public static void main(String[] args) {
		SearchDialog search = new SearchDialog();
		int ok = search.showDialog( null );
	//	JOptionPane.showConfirmDialog(MapApp.anchor, search, "Search", 
	//			JOptionPane.OK_CANCEL_OPTION,
	//			JOptionPane.PLAIN_MESSAGE);
		double[] wesn = search.getWESN();
		System.out.println("West:\t" + wesn[0]);
		System.out.println("East:\t" + wesn[1]);
		System.out.println("South:\t" + wesn[2]);
		System.out.println("North:\t" + wesn[3]);
		String[] types = search.getDataTypes();
		System.out.println("Data Types Selected");
		for( int i=0 ; i<types.length ; i++) {
			System.out.println("\t"+types[i]);
		}
		System.exit(0);
	}
}
