package haxby.db.custom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.geomapapp.image.ColorModPanel;

public class DBConfigDialog extends JDialog implements ActionListener, ItemListener {

	JPanel panel;
	UnknownDataSet ds;
	JComboBox<String> lat,lon, rgb, polyline, shape, lineStyle;
	JRadioButton station, track;
	ButtonGroup bg;
	JCheckBox editable, drawOutline;
	JLabel stationL, shapeL, lineThickL, lineStyleL, outlineL, editableL;
	JSlider symbolSize;
	JTextField name;
	JCheckBox annotation;
	JComboBox anotCB;
	JButton color;
	JFormattedTextField lineThick;
	Vector<String> latlonOptions, columnOptions;

	public DBConfigDialog(Frame owner, UnknownDataSet ds){
		super((Frame) null,"Config " + ds.desc.name, true);
		this.ds=ds;
		initGUI(owner);
	}

	public void annotate() {
	if( !annotation.isSelected() )return;
	}
	public void initGUI(Frame owner){

		panel = new JPanel(new GridLayout(0,2));
		panel.setPreferredSize(new Dimension(500, 600));
		
		panel.add(new JLabel("Database Name: "));
		name = new JTextField(20);
		name.setText(ds.desc.name);
		panel.add(name);
		panel.add(new JLabel("Latitude Column: "));

		latlonOptions = new Vector<String>(ds.header.size()-1);
		latlonOptions.addAll(ds.header.subList(1, ds.header.size()));
		lat = new JComboBox<String>(latlonOptions);
		if (ds.latIndex!=-1)lat.setSelectedIndex(ds.latIndex-1);
		else {
			String choice = latlonOptions.get(0);
			for (int i = 1; i < latlonOptions.size(); i++)
				if (latlonOptions.get(i).toString().toLowerCase().startsWith("lat")) {
					choice = latlonOptions.get(i);
					break;
				}
			lat.setSelectedItem(choice);
		}
		panel.add(lat);

		panel.add(new JLabel("Longitude Column: "));
		lon = new JComboBox<String>(latlonOptions);
		if (ds.lonIndex!=-1)lon.setSelectedIndex(ds.lonIndex-1);
		else {
			String choice = latlonOptions.get(0);
			for (int i = 1; i < latlonOptions.size(); i++) 
				if (latlonOptions.get(i).toString().toLowerCase().startsWith("lon")) {
					choice = latlonOptions.get(i);
					break;
				}
			lon.setSelectedItem(choice);
		}
		panel.add(lon);

		panel.add(new JLabel("RGB Column: "));
		columnOptions = new Vector<String>(ds.header.size());
		columnOptions.addAll(ds.header.subList(1, ds.header.size()));
		columnOptions.add(0, "None");
		rgb = new JComboBox<String>(columnOptions);
		if (ds.rgbIndex!=-1)rgb.setSelectedIndex(ds.rgbIndex);
		else {
			int rgbIndex = 0;
			for (int i = 1; i < columnOptions.size(); i++) 
				if (columnOptions.get(i).toString().toLowerCase().startsWith("rgb")) {
					rgbIndex = i;
					break;
				}
			rgb.setSelectedIndex(rgbIndex);
		}
		panel.add(rgb);

		panel.add(new JLabel("Polyline Column: "));
		polyline = new JComboBox<String>(columnOptions);
		if (ds.polylineIndex!=-1)polyline.setSelectedIndex(ds.polylineIndex);
		else {
			int polylineIndex = 0;
			for (int i = 1; i < columnOptions.size(); i++) 
				if (columnOptions.get(i).toString().toLowerCase().startsWith("polyline")) {
					polylineIndex = i;
					break;
				}
			polyline.setSelectedIndex(polylineIndex);
		}
		panel.add(polyline);

	annotation = new JCheckBox("annotate");
	annotation.setSelected(false);
//	p.add(annotation);
	anotCB = new JComboBox(ds.header);
//	p.add(anotCB);
//	annotation.addActionListener( new ActionListener() {
//		public void actionPerformed(ActionEvent e) {
//			annotate();
//		}
//	});

		panel.add(new JLabel("Data Display type:"));
		JPanel p2 = new JPanel();
		bg = new ButtonGroup();
		p2 = new JPanel();
		station = new JRadioButton("Station",ds.station);
		station.addItemListener(this);
		p2.add(station);
		bg.add(station);
		track = new JRadioButton("Track",!ds.station);
		p2.add(track);
		bg.add(track);
		panel.add(p2);

		panel.add(new JLabel("Symbol/Line Color:"));
//		***** GMA 1.6.4: Changed name of button to "Color All" to more clearly indicate functionality
		color = new JButton("Color All");
		color.setPreferredSize(new Dimension(10, 10));

		color.setBackground(ds.getColor());
		color.setActionCommand("color");
		color.addActionListener(this);
		panel.add(color);

		shapeL = new JLabel("Shape");
		panel.add(shapeL);
		shapeL.setEnabled(ds.station);
		Vector<String> shapeOptions = new Vector<String>();
		shapeOptions.add("Circle");
		shapeOptions.add("Square");
		shapeOptions.add("Triangle");
		shapeOptions.add("Star");

		shape = new JComboBox<String>(shapeOptions);
		
		if(ds.shapeString.equalsIgnoreCase("Circle"))
			shape.setSelectedIndex(0);
		if(ds.shapeString.equalsIgnoreCase("Square"))
			shape.setSelectedIndex(1);
		if(ds.shapeString.equalsIgnoreCase("Triangle"))
			shape.setSelectedIndex(2);
		if(ds.shapeString.equalsIgnoreCase("Star"))
			shape.setSelectedIndex(3);
		shape.setEnabled(ds.station);
		panel.add(shape);

		outlineL = new JLabel("Symbol Outline: ");
		outlineL.setEnabled(ds.station);
		panel.add(outlineL);
		drawOutline = new JCheckBox("Draw", ds.drawOutlines);
		drawOutline.setEnabled(ds.station);
		panel.add(drawOutline);

		stationL = new JLabel("Symbol Size Percent:");
		stationL.setEnabled(ds.station);
		panel.add(stationL);
		symbolSize = new JSlider(0,200,ds.symbolSize);
		symbolSize.setMajorTickSpacing(50);
		symbolSize.setMinorTickSpacing(25);
		symbolSize.setPaintLabels(true);
		symbolSize.setPaintTicks(true);
		symbolSize.setEnabled(ds.station);
		panel.add(symbolSize);
		
				
		lineThickL = new JLabel("Line Thickness:");
		lineThickL.setEnabled(!ds.station);
		panel.add(lineThickL);
		lineThick = new JFormattedTextField(java.text.DecimalFormat.getInstance());
		lineThick.setText(String.valueOf(ds.lineThick));
		lineThick.setEnabled(!ds.station);
		panel.add(lineThick);
		
		lineStyleL = new JLabel("Line Style:");
		lineStyleL.setEnabled(!ds.station);
		panel.add(lineStyleL);
		Vector<String> lineOptions = new Vector<String>();
		lineOptions.add("Solid");
		lineOptions.add("Dashed");
		lineOptions.add("Dotted");
		lineOptions.add("Dash-dotted");
		lineStyle = new JComboBox<String>(lineOptions);		
		if(ds.lineStyleString.equalsIgnoreCase("Solid"))
			lineStyle.setSelectedIndex(0);
		if(ds.lineStyleString.equalsIgnoreCase("Dashed"))
			lineStyle.setSelectedIndex(1);
		if(ds.lineStyleString.equalsIgnoreCase("Dotted"))
			lineStyle.setSelectedIndex(2);
		if(ds.lineStyleString.equalsIgnoreCase("Dash-dotted"))
			lineStyle.setSelectedIndex(3);
		lineStyle.setEnabled(!ds.station);
		panel.add(lineStyle);
				
		panel.add(new JLabel("Data Table:"));
		JButton b = new JButton("Config");
		b.addActionListener(this);
		b.setActionCommand("table");
		panel.add(b);
		
		editableL = new JLabel("<html><font color='blue'>Data Cells/Interactive Points:</font></html>");
		editableL.setEnabled(ds.isEditable());
		panel.add(editableL);
		editable=new JCheckBox("<html><font color='blue'>Editable (Imported Tables Only)</font></html>");
		editable.setEnabled(ds.isEditable());
		if (ds.isEditable()) {
			editable.setSelected(ds.tm.editable);
		} else {
			editable.setSelected(false);
		}
		panel.add(editable);
		p2 = new JPanel();
		p2.add(panel);
		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add(p2);
		panel = new JPanel();
		b = new JButton("OK");
		b.addActionListener(this);
		b.setActionCommand("ok");
		getRootPane().setDefaultButton(b);
		panel.add(b);
		b = new JButton("Defaults");
		b.addActionListener(this);
		b.setActionCommand("defaults");
		panel.add(b);
		b = new JButton("Reset");
		b.addActionListener(this);
		b.setActionCommand("reset");
		panel.add(b);
		b = new JButton("Cancel");
		b.addActionListener(this);
		b.setActionCommand("cancel");
		panel.add(b);

		getContentPane().add(panel,BorderLayout.SOUTH);
		pack();
		if (owner != null) setLocation(owner.getX()+owner.getWidth()/2-getWidth()/2, owner.getY()+owner.getHeight()/2-getWidth()/2);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent evt) {
		String ac = evt.getActionCommand();
		if (ac.equals("ok")) ok();
		else if (ac.equals("defaults")) defaults();
		else if (ac.equals("reset")) reset();
		else if (ac.equals("cancel")) cancel();
		else if (ac.equals("symbol")) configSymbol();
		else if (ac.equals("table")) configTable();
		else if (ac.equals("color")) configColor();
	}

	public void itemStateChanged(ItemEvent e) {
		toggleStation();
	}

	public void toggleStation(){
		if (symbolSize.isEnabled()){
			symbolSize.setEnabled(false);
			stationL.setEnabled(false);
			shape.setEnabled(false);
			shapeL.setEnabled(false);
			drawOutline.setEnabled(false);
			outlineL.setEnabled(false);
			lineThickL.setEnabled(true);
			lineThick.setEnabled(true);
			lineStyleL.setEnabled(true);
			lineStyle.setEnabled(true);
		} else {
			symbolSize.setEnabled(true);
			stationL.setEnabled(true);
			shape.setEnabled(true);
			shapeL.setEnabled(true);
			drawOutline.setEnabled(true);
			outlineL.setEnabled(true);
			lineThickL.setEnabled(false);
			lineThick.setEnabled(false);
			lineStyleL.setEnabled(false);
			lineStyle.setEnabled(false);
		}
	}

	public void ok(){	
		if(track.isSelected()) {
			Vector<Integer> dangerIndices = new Vector<>();
			Vector<Integer> lineNumAdjuster = new Vector<>();
			int numTracksSoFar = 0;
			for(int pt = 1; pt < ds.getNewTrack().size(); pt++) {
				if(ds.getNewTrack().get(pt)) {
					numTracksSoFar++;
					if(ds.getNewTrack().get(pt-1)) {
						dangerIndices.add(pt-1);
						lineNumAdjuster.add(numTracksSoFar + 1 + ds.commentLinesMap.get(pt-1));
					}
					if(pt+1 == ds.getNewTrack().size()) {
						dangerIndices.add(pt);
						lineNumAdjuster.add(numTracksSoFar + 2 + ds.commentLinesMap.get(pt));
					}
				}
			}
			if(!dangerIndices.isEmpty()) {
				station.setSelected(true);
				StringBuilder sb = new StringBuilder("Cannot show tracks because there ");
				if(dangerIndices.size() == 1) {
					sb.append("is 1 isolated point:");
				}
				else {
					sb.append("are ").append(dangerIndices.size()).append(" isolated points:");
				}
				for(int i = 0; i < dangerIndices.size(); i++) {
					int dangerIndex = dangerIndices.get(i);
					int tracks = lineNumAdjuster.get(i);
					sb.append("\nInput file line ").append(dangerIndex + tracks).append(": ");
					String coordsAsString = ds.data.get(dangerIndex).data.subList(1, 3).toString();
					String presentableCoords = coordsAsString.replace('[', '(').replace(']', ')');
					sb.append(presentableCoords);
				}
				sb.append("\nShowing stations instead.");
				JOptionPane.showMessageDialog(this, sb.toString(), "Error Displaying Tracks", JOptionPane.WARNING_MESSAGE);
			}
		}
		
		ds.desc.name=name.getText();
		ds.tm.editable=editable.isSelected();
		ds.latIndex = lat.getSelectedIndex() + 1;
		ds.lonIndex = lon.getSelectedIndex() + 1;
		ds.rgbIndex = rgb.getSelectedIndex();
		ds.polylineIndex = polyline.getSelectedIndex() ;
		ds.setColor(color.getBackground());
		//ds. do something with unkown data set
		//TODO
		ds.symbolSize = symbolSize.getValue();
		ds.scene.clearScene();

		float[] wesn = new float[] {Float.MAX_VALUE, -Float.MAX_VALUE, 
				Float.MAX_VALUE, -Float.MAX_VALUE}; 
		ds.polylines.clear();

		int index = 0;
		for (UnknownData d : ds.data) {
			float[] lonLat = d.getPointLonLat(ds.lonIndex,ds.latIndex);
			if(lonLat != null) {
				wesn[0] = Math.min(lonLat[0], wesn[0]);
				wesn[1] = Math.max(lonLat[0], wesn[1]);
				wesn[2] = Math.min(lonLat[1], wesn[2]);
				wesn[3] = Math.max(lonLat[1], wesn[3]);
			}
			d.updateXY(ds.map,ds.lonIndex,ds.latIndex);
			d.updatePolyline(ds.map, ds.polylineIndex);
			if (d.polyline != null) {
				float[] polylineWESN = d.getPolylineWESN(ds.polylineIndex);
				wesn[0] = Math.min(wesn[0], polylineWESN[0]);
				wesn[1] = Math.max(wesn[1], polylineWESN[1]);
				wesn[2] = Math.min(wesn[2], polylineWESN[2]);
				wesn[3] = Math.max(wesn[3], polylineWESN[3]);
				
				ds.polylines.add(index);
			}

			d.updateRGB(ds.rgbIndex);
			if (station.isSelected()) {
				if (!Float.isNaN(d.x) &&
						!Float.isNaN(d.y))
					ds.scene.addEntry(ds.new UnknownDataSceneEntry(index));
			}
			index++;
		}

		ds.drawOutlines = drawOutline.isSelected();
		ds.wesn = wesn;
		
		if (ds.station != station.isSelected()){
			ds.station = station.isSelected();

			if (!ds.station){
				ds.tm.addCD();
			} else {
				ds.tm.removeCD();
			}
		}
		
		switch(lineStyle.getSelectedIndex()) {
			case 0:
				ds.lineStyleString = "solid";
				break;
			case 1:
				ds.lineStyleString = "dashed";
				break;
			case 2:
				ds.lineStyleString = "dotted";
				break;
			case 3:
				ds.lineStyleString = "dash-dotted";
				break;
		}
		
		switch(shape.getSelectedIndex()) {
			case 0:
				ds.setSymbolShape("circle");
				break;
			case 1:
				ds.setSymbolShape("square");
				break;
			case 2:
				ds.setSymbolShape("triangle");
				break;
			case 3:
				ds.setSymbolShape("star");
				break;
		}
			
		try {
			ds.lineThick = Float.parseFloat(lineThick.getText());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
				    "Line thickness must be a number",
				    "Input error",
				    JOptionPane.ERROR_MESSAGE);
			return;
		}

		setVisible(false);
	}

	public void defaults(){
		symbolSize.setValue(100);
			
		String choice = latlonOptions.get(0);
		for (int i = 1; i < latlonOptions.size(); i++)
			if (latlonOptions.get(i).toString().toLowerCase().startsWith("lat")) {
				choice = latlonOptions.get(i);
				break;
			}
		lat.setSelectedItem(choice);
		
		choice = latlonOptions.get(0);
		for (int i = 1; i < latlonOptions.size(); i++) 
			if (latlonOptions.get(i).toString().toLowerCase().startsWith("lon")) {
				choice = latlonOptions.get(i);
				break;
			}
		lon.setSelectedItem(choice);

		int rgbIndex = 0;
		for (int i = 1; i < columnOptions.size(); i++) 
			if (columnOptions.get(i).toString().toLowerCase().startsWith("rgb")) {
				rgbIndex = i;
				break;
			}
		rgb.setSelectedIndex(rgbIndex);
				
		int polylineIndex = 0;
		for (int i = 1; i < columnOptions.size(); i++) 
			if (columnOptions.get(i).toString().toLowerCase().startsWith("polyline")) {
				polylineIndex = i;
				break;
			}
		polyline.setSelectedIndex(polylineIndex);
		
		drawOutline.setSelected(true);
		station.setSelected(true);
		track.setSelected(false);
		editable.setSelected(false);
		color.setBackground(Color.GRAY);
		lineThick.setText("1");
		lineStyle.setSelectedIndex(0);
		shape.setSelectedIndex(0);
	}

	public void reset(){
		color.setBackground(ds.getColor());
		symbolSize.setValue(ds.symbolSize);
		
		if (ds.latIndex!=-1)lat.setSelectedIndex(ds.latIndex-1);
		else {
			String choice = latlonOptions.get(0);
			for (int i = 1; i < latlonOptions.size(); i++)
				if (latlonOptions.get(i).toString().toLowerCase().startsWith("lat")) {
					choice = latlonOptions.get(i);
					break;
				}
			lat.setSelectedItem(choice);
		}
		
		if (ds.lonIndex!=-1)lon.setSelectedIndex(ds.lonIndex-1);
		else {
			String choice = latlonOptions.get(0);
			for (int i = 1; i < latlonOptions.size(); i++) 
				if (latlonOptions.get(i).toString().toLowerCase().startsWith("lon")) {
					choice = latlonOptions.get(i);
					break;
				}
			lon.setSelectedItem(choice);
		}
		
		if (ds.rgbIndex==-1){
			rgb.setSelectedIndex(0);
			for (int i = 1; i < columnOptions.size(); i++) 
				if (columnOptions.get(i).toString().toLowerCase().startsWith("rgb")) {
					rgb.setSelectedIndex(i);
					break;
				}
		} else rgb.setSelectedIndex(ds.rgbIndex);
		
		if (ds.polylineIndex==-1){
			polyline.setSelectedIndex(0);
			for (int i = 1; i < columnOptions.size(); i++) 
				if (columnOptions.get(i).toString().toLowerCase().startsWith("polyline")) {
					polyline.setSelectedIndex(i);
					break;
				} 		
		} else polyline.setSelectedIndex(ds.polylineIndex);

		drawOutline.setSelected(ds.drawOutlines);
		station.setSelected(ds.station);
		track.setSelected(!ds.station);
		editable.setSelected(ds.tm.editable);
		lineThick.setText(String.valueOf(ds.lineThick));
		if(ds.lineStyleString.equalsIgnoreCase("Solid"))
			lineStyle.setSelectedIndex(0);
		if(ds.lineStyleString.equalsIgnoreCase("Dashed"))
			lineStyle.setSelectedIndex(1);
		if(ds.lineStyleString.equalsIgnoreCase("Dotted"))
			lineStyle.setSelectedIndex(2);
		if(ds.lineStyleString.equalsIgnoreCase("Dash-dotted"))
			lineStyle.setSelectedIndex(3);
		
		if(ds.shapeString.equalsIgnoreCase("Circle"))
			shape.setSelectedIndex(0);
		if(ds.shapeString.equalsIgnoreCase("Square"))
			shape.setSelectedIndex(1);
		if(ds.shapeString.equalsIgnoreCase("Triangle"))
			shape.setSelectedIndex(2);
		if(ds.shapeString.equalsIgnoreCase("Star"))
			shape.setSelectedIndex(3);
	}
	

	public void cancel(){
		setVisible(false);
	}

	public void configTable(){
		new DBTableConfigDialog(this,ds);
	}

	public void configSymbol(){

	}
	//fired when Color All pressed
	public void configColor() {
		//System.out.println("color all");
		ColorModPanel colorPanel = new ColorModPanel(color.getBackground().getRGB(), false);
		int rgb = colorPanel.showDialog(this);
		color.setBackground(new Color(rgb));
	}

	public void dispose() {
		setVisible( false );
		ds = null;
		getContentPane().removeAll();
		panel.removeAll();
		panel = null;
		bg.remove(station); bg.remove(track);
		bg = null;
		lat = lon = rgb = null;
		station = track = null;
		editable = null;
		stationL = null;
		symbolSize = null;
		name = null;
		annotation = null;
		anotCB = null;
		color = null;
		drawOutline = null;
		lineThickL = null;
		lineThick = null;
		lineStyleL = null;
		lineStyle = null;

		super.dispose();

	}
}
