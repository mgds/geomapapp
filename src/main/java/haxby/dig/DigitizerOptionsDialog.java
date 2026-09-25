package haxby.dig;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class DigitizerOptionsDialog implements ActionListener,
					MouseListener {
	Color defaultColor;
	Color defaultFill;
	BasicStroke defaultStroke;
	Color color;
	Color fill;
	BasicStroke stroke;
	Color[] startingColor;
	Color[] startingFill;
	BasicStroke[] startingStroke;
	JComponent comp;
	JCheckBox colorB, fillB;
	ColorPanel cP;
	ColorSwatch colorSwatch;
	ColorSwatch fillSwatch;
	DigitizerObject[] obj;
	JTextField lineWidth;
	Vector types;
	JComboBox typeCB;
	JPanel strokePanel=null;
	JPanel colorPanel=null;
	JPanel dialogPanel=null;
	JDialog colorDialog = null;
	boolean initiallized;
	JFrame owner = null;
	JTextField annoTF;
	JTextField annoSize;
	JCheckBox bold, i;
	JTabbedPane dialogTabbedPane;
	JComboBox fontSelect;
	private Font startFont;
	private String startAnno;

	public DigitizerOptionsDialog(JComponent comp) {
		this.comp = comp;
		defaultColor = Color.black;
		defaultFill = null;
		defaultStroke = new BasicStroke( 3f );
		color = new Color( 0, 0, 255 );
		fill = null;
		stroke = new BasicStroke( 3f );
		cP = new ColorPanel( color, 255, new ColorSwatch( Color.blue ) );
		initiallized = false;
		initDialog();
	}
	JPanel getStrokePanel() {
		if( strokePanel != null ) {
			lineWidth.setText( ""+stroke.getLineWidth() );
			return strokePanel;
		}
		strokePanel = new JPanel( new FlowLayout() );
		strokePanel.add( new JLabel("Line Width"));
		lineWidth = new JTextField(""+stroke.getLineWidth());
		strokePanel.add( lineWidth );
		return strokePanel;
	}
	void initDialog() {
		if( initiallized ) return;
		colorPanel = new JPanel( new GridLayout(0, 1 ) );
		colorB = new JCheckBox("Draw Outline");
		colorB.addActionListener( this );
		colorPanel.add( colorB );
		colorSwatch = new ColorSwatch( defaultColor );
		colorPanel.add( colorSwatch );
		colorSwatch.addMouseListener( this );
		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( colorPanel, "West");
		panel.add( cP, "East");
		panel.add( this.getStrokePanel(), "South");
		panel.setBorder( new LineBorder( Color.black, 1) );

		JPanel bPanel = new JPanel( new GridLayout(0,3) );
		JButton b = new JButton( "OK" );
		b.setMnemonic( 'O' );
		b.addActionListener(this);
		bPanel.add( b );
		b = new JButton( "Preview" );
		b.setMnemonic( 'P' );
		b.addActionListener(this);
		bPanel.add( b );
		b = new JButton( "Reset" );
		b.setMnemonic( 'R' );
		b.addActionListener(this);
		bPanel.add( b );
	//	b = new JButton( "Defaults" );
	//	b.setMnemonic( 'D' );
	//	b.addActionListener(this);
	//	bPanel.add( b );
		b = new JButton( "Cancel" );
		b.setMnemonic( 'C' );
		b.addActionListener(this);
		bPanel.add( b );

		JPanel typesPanel = new JPanel(new GridLayout( 0, 1) );
		types = new Vector();
		types.add( new LineType("Default", 
					defaultColor, 
					defaultFill, 
					defaultStroke) ); 
		types.add( new LineType( "Sea Floor",
					new Color(0, 255, 0, 100),
					defaultFill,
					new BasicStroke(5f) ) );
		types.add( new LineType( "Basement",
					new Color(0, 0, 255, 100),
					defaultFill,
					new BasicStroke(5f) ) );
		types.add( new LineType( "Horizon 1",
					new Color(255, 255, 0, 100),
					defaultFill,
					new BasicStroke(5f) ) );
		types.add( new LineType( "Horizon 2",
					new Color(0, 255, 255, 100),
					defaultFill,
					new BasicStroke(5f) ) );
		types.add( new LineType( "Horizon 3",
					new Color(255, 0, 255, 100),
					defaultFill,
					new BasicStroke(5f) ) );
		typeCB = new JComboBox(types);
		typeCB.addActionListener( this );
		typesPanel.add( typeCB );

		JPanel colorChoiceP = new JPanel( new BorderLayout() );
		colorChoiceP.add( typesPanel, BorderLayout.NORTH);
		colorChoiceP.add( panel, BorderLayout.CENTER);

		JPanel annotationPanel = new JPanel( new GridLayout(0, 2));

		JLabel annoL = new JLabel("Annotation Font:");
		fontSelect = new JComboBox( GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() );
		fontSelect.setSelectedItem( "Times New Roman");
		annotationPanel.add(annoL);
		annotationPanel.add(fontSelect);

		annoL = new JLabel("Annotation Text:");
		annoTF = new JTextField();
		annotationPanel.add( annoL );
		annotationPanel.add( annoTF );

		annoL = new JLabel("Annotation Size:");
		annoSize = new JTextField();
		annotationPanel.add( annoL );
		annotationPanel.add( annoSize );

		bold = new JCheckBox("Bold", false);
		i = new JCheckBox("Italic", false);
		annotationPanel.add(bold);
		annotationPanel.add(i);

		dialogTabbedPane = new JTabbedPane();
		dialogTabbedPane.add( "Colors", colorChoiceP );
		dialogTabbedPane.add( "Annotation", annotationPanel );
		dialogTabbedPane.setEnabledAt( 1, false);

		dialogPanel = new JPanel( new BorderLayout() );

		dialogPanel.add( dialogTabbedPane, BorderLayout.CENTER);
		dialogPanel.add( bPanel, BorderLayout.SOUTH);

	//	colorDialog = new JDialog( owner, "Select Colors", true);
	//	colorDialog.getContentPane().setLayout( new BorderLayout() );
	//	colorDialog.getContentPane().add( typesPanel, BorderLayout.NORTH);
	//	colorDialog.getContentPane().add( panel, BorderLayout.CENTER);
	//	colorDialog.getContentPane().add( bPanel, BorderLayout.SOUTH);
	//	colorDialog.pack();
		initiallized = true;
	}
	public void showDialog( DigitizerObject[] obj ) {
		initDialog();
		
		this.obj = obj;
		if( obj.length==0 ) return;

		boolean ano = false;
		for (int i = 0; i<obj.length; i++) {
			if (obj[i] instanceof AnnotationObject) {
				ano = true;

				startFont = ((AnnotationObject)obj[i]).getFont();
				startAnno = ((AnnotationObject)obj[i]).getAnnotation();

				bold.setSelected( ((AnnotationObject)obj[i]).getFont().isBold() );
				this.i.setSelected( ((AnnotationObject)obj[i]).getFont().isItalic() );
				annoSize.setText("" + ((AnnotationObject)obj[i]).getFont().getSize() );
				annoTF.setText( ((AnnotationObject)obj[i]).getAnnotation() );
				fontSelect.setSelectedItem( startFont );
				break;
			}
		}

		dialogTabbedPane.setEnabledAt(1, ano);
		if (ano)
			dialogTabbedPane.setSelectedIndex( 0 );

		if( owner==null ) {
			owner = (JFrame)comp.getTopLevelAncestor();
			colorDialog = new JDialog( owner, "Select Colors", true);
			colorDialog.getContentPane().add( dialogPanel );
			colorDialog.pack();
		}
		startingColor = new Color[obj.length];
		startingFill = new Color[obj.length];
		startingStroke = new BasicStroke[obj.length];
		for( int i=0 ; i<obj.length ; i++ ) {
			startingColor[i] = obj[i].getColor();
			startingFill[i] = obj[i].getFill();
			startingStroke[i] = obj[i].getStroke();
		}
		stroke = startingStroke[0];
		color = startingColor[0];
		setColors();
		typeCB.setSelectedItem( obj[0].toString() );
		colorDialog.show();
	}

	void setColors() {
		if( color==null ) {
			colorB.setSelected(false);
			colorSwatch.setColor( defaultColor );
			colorSwatch.setVisible( false );
		} else {
			colorB.setSelected(true);
			colorSwatch.setColor( color );
			colorSwatch.setTransparency( color.getAlpha() );
			colorSwatch.setVisible( true );
			colorSwatch.setActive(true);
			cP.setSwatch( colorSwatch );
		}
	}

	void reset() {
		for( int i=0 ; i<obj.length ; i++ ) {
		//	if( obj[i].getColor()!=null || startingColor[i]!=null ) {
				obj[i].setColor( startingColor[i] );
		//	}
			obj[i].setStroke( startingStroke[i] );
		}
		color = startingColor[0];
		stroke = startingStroke[0];
		
		lineWidth.setText( ""+stroke.getLineWidth() );
		setColors();

		if (dialogTabbedPane.isEnabledAt(1)) {
			fontSelect.setSelectedItem( startFont );
			bold.setSelected(startFont.isBold());
			i.setSelected(startFont.isItalic());
			annoTF.setText( startAnno);
			annoSize.setText( Integer.toString(startFont.getSize()) );

			((AnnotationObject) obj[0]).setFont( startFont );
			((AnnotationObject) obj[0]).setAnnotation( startAnno );
		}
		comp.repaint();
	}

	void defaults() {
		for( int i=0 ; i<obj.length ; i++ ) {
			obj[i].setColor( defaultColor );
		//	obj[i].setFill( defaultFill );
			obj[i].setStroke( defaultStroke );
		}

		color = defaultColor;
		stroke = defaultStroke;
		lineWidth.setText( ""+stroke.getLineWidth() );
		setColors();

		if (dialogTabbedPane.isEnabledAt(1)) {
			((AnnotationObject)obj[0]).setFont( comp.getFont() );
			((AnnotationObject)obj[0]).setAnnotation( "----");
		}
		comp.repaint();
	}

	void apply() {
		color=null;
		String name = ((LineType)typeCB.getSelectedItem()).name;
		if( colorB.isSelected() ) {
			color = colorSwatch.getColor();
			color = new Color( color.getRed(), color.getGreen(), color.getBlue(),
					colorSwatch.getTransparency() );
		}
		float w = 3f;
		try {
			w = Float.parseFloat(lineWidth.getText());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		stroke = new BasicStroke( w, stroke.getEndCap(),
				stroke.getLineJoin(),
				stroke.getMiterLimit());
		for( int i=0 ; i<obj.length ; i++ ) {
			if( obj[i].getColor()!=null || color!=null ) {
				obj[i].setName(name);
				obj[i].setColor( color );
			}
			obj[i].setStroke( stroke );
		}
		setColors();
		
		if (dialogTabbedPane.isEnabledAt(1)) {
			int fType = 0;
			if (bold.isSelected())
				fType += Font.BOLD;
			if (i.isSelected())
				fType += Font.ITALIC;

			Font f = new Font( fontSelect.getSelectedItem().toString(), fType, Integer.parseInt(annoSize.getText()));

			((AnnotationObject)obj[0]).setFont( f );
			((AnnotationObject)obj[0]).setAnnotation( annoTF.getText());
		}
		comp.repaint();
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource() == colorB ) {
		//	for( int i=0 ; i<obj.length ; i++ ) {
		//		obj[i].setColor( null );
		//	}
			color = obj[0].getColor();
			if( color==null ) {
				if( startingColor[0]==null ) color=defaultColor;
				else color=startingColor[0];
			}
			colorSwatch.setColor( color );
			colorSwatch.repaint();
			colorSwatch.setVisible( colorB.isSelected() );
		} else if( evt.getSource()==typeCB ) {
			LineType type = (LineType)typeCB.getSelectedItem();
			color = type.color;
			fill = type.fill;
			stroke = type.stroke;
			lineWidth.setText( ""+stroke.getLineWidth() );
			colorSwatch.setActive( true );
			colorSwatch.setColor( color );
			colorSwatch.repaint();
			colorSwatch.setTransparency( color.getAlpha() );
		//	cP.setColor( color );
		//	cP.setTransparency( color.getAlpha() );
			cP.setSwatch( colorSwatch );
			cP.repaint();
		} else if( evt.getActionCommand().equals("OK") ) {
			apply();
			colorDialog.dispose();
		} else if( evt.getActionCommand().equals("Reset") ) {
			reset();
			color = startingColor[0];
			stroke = startingStroke[0];
			colorDialog.repaint();
			cP.repaint();
		//	colorDialog.dispose();
		//	this.showDialog( obj );
		} 
		else if( evt.getActionCommand().equals("Preview")) {
			this.apply();
			colorDialog.repaint();
		//	colorDialog.dispose();
		//	this.showDialog( obj );
		}
	//	else if( evt.getActionCommand().equals("Defaults"))
	//	{
	//		this.defaults();
	//		colorDialog.repaint();
		//	colorDialog.dispose();
		//	this.showDialog( obj );
	//	}
		else if (evt.getActionCommand().equals("Cancel")) {
			this.reset();
			colorDialog.dispose();
			obj = null;
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.getSource()== colorSwatch ) {
			if( !colorB.isSelected() )return;
			if( color==null ) color = defaultColor;
			colorSwatch.setActive(true);
			cP.setSwatch( colorSwatch );
		} else if(evt.getSource()== fillSwatch) {
			if( !fillB.isSelected() )return;
			colorSwatch.setActive(false);
			fillSwatch.setActive(true);
			cP.setSwatch( fillSwatch );
		}
		cP.reset();
	}
	public String getType() {
		LineType type = (LineType)typeCB.getSelectedItem();
		return type.name;
	}
	public Vector getLineTypes() {
		return types;
	}
}