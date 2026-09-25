package haxby.dig;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import haxby.image.Icons;
import haxby.map.Overlay;
import haxby.util.ScaledComponent;

public class Digitizer implements Overlay,
				MouseListener,
				MouseMotionListener,
				ActionListener,
				ListSelectionListener,
				KeyListener {
	ScaledComponent map;
	Vector objects;
	DigitizerObject currentObject;
	int lastSelectedIndex;			//Must be reset
	JList objectList;
	DigListModel model;
	DigitizerOptionsDialog options;
	Vector classes;
	Vector buttons;
//	JPanel tools;
	Box tools;
	ButtonGroup buttonGroup;
	JToggleButton selectB, digB, annotB;
	int mode;

	public Digitizer( ScaledComponent map ) {
		this.map = map;
		objects = new Vector();
		classes = new Vector();

		initTools();

		model = new DigListModel( this );
		objectList = new JList(model);
		objectList.setCellRenderer( new DigCellRenderer() );
		objectList.addListSelectionListener( this);
		objectList.addMouseListener( this);
		lastSelectedIndex = -1;

		options = new DigitizerOptionsDialog( map );

		currentObject = null;
		mode = 0;
	}
	public JList getList() {
		return objectList;
	}
	public JToggleButton getSelectB() {
		return selectB;
	}
	
	public JToggleButton getDigB() {
		return digB;
	}
	
	public JToggleButton getAnnotB() {
		return annotB;
	}
	
	void initTools() {
	//	tools = new JPanel( new GridLayout(1, 0 ));
		tools = Box.createHorizontalBox();
		buttonGroup = new ButtonGroup();
		buttons = new Vector();
		selectB = new JToggleButton( 
				Icons.getIcon( Icons.SELECT, false ));
		selectB.setSelectedIcon( Icons.getIcon( Icons.SELECT, true ) );
		buttons.add( selectB );
		buttonGroup.add( selectB );
		tools.add( selectB );
		selectB.addActionListener( this );
		selectB.setActionCommand( "select" );
		selectB.setBorder( null );
		try {
			addType( Class.forName("haxby.dig.LineSegmentsObject") );
			addType( Class.forName("haxby.dig.AnnotationObject") );
		} catch(Exception ex ) {
			ex.printStackTrace();
		}
	}
	public Box getPanel() {
		return tools;
	}

	public Vector getButtons() {
		return buttons;
	}

	public void addType( Class clas ) {
		if( classes.contains( clas ) ) return;
		try {
			Class[] xf = clas.getInterfaces();
			boolean ok = false;
			for( int k=0 ; k<xf.length ; k++) {
				if( xf[k].equals( Class.forName("haxby.dig.DigitizerObject") )) {
					ok=true;
					break;
				}
			}
			if( !ok ) {
				System.out.println( "could not add "+clas.getName());
				return;
			}
			classes.add( clas );
			JToggleButton tb;
			if( clas.equals(Class.forName("haxby.dig.LineSegmentsObject")) ) {
				ImageIcon icon = Icons.getIcon(Icons.SEGMENTS, false);
				tb = new JToggleButton( icon );
				icon = Icons.getIcon(Icons.SEGMENTS, true);
				tb.setSelectedIcon( icon );
				tb.setToolTipText("Digitize a Reflector");
				digB = tb;
			}
			else if( clas.equals(Class.forName("haxby.dig.AnnotationObject")) ) {
				ImageIcon icon = Icons.getIcon(Icons.ANNOTATION, false);
				tb = new JToggleButton( icon );
				icon = Icons.getIcon(Icons.ANNOTATION, true);
				tb.setSelectedIcon( icon );
				tb.setToolTipText("Add Text Annotation");
				annotB = tb;
			} 
			else {
				java.lang.reflect.Field field = clas.getField( "ICON" );
				ImageIcon icon = (ImageIcon)field.get(null);
				tb = new JToggleButton( icon );
				field = clas.getField( "SELECTED_ICON" );
				icon = (ImageIcon)field.get(null);
				tb.setSelectedIcon( icon );
			}
			tb.setBorder( null );
			tools.add( tb );
			buttonGroup.add( tb );
			buttons.add( tb );
			tb.addActionListener( this );
			tb.setActionCommand( Integer.toString( classes.size()-1 ) );
		} catch(Exception ex ) {
			ex.printStackTrace();
		}
	}
	public void valueChanged(ListSelectionEvent evt) {
		if(objects.size() == 0)return;
		int[] indices = objectList.getSelectedIndices();
		for( int i=0 ; i<objects.size(); i++ ) {
			try {
				DigitizerObject obj = (DigitizerObject) objects.get(i);
				obj.setSelected( false );
			} catch( Exception ex) {
			}
		}
		for( int i=0 ; i<indices.length ; i++ ) {
			try {
				DigitizerObject obj = (DigitizerObject) objects.get(indices[i]);
				obj.setSelected( true );
			} catch( Exception ex) {
			}
		}
		map.repaint();
	}
	public void actionPerformed( ActionEvent evt ) {
		if( currentObject!=null ) {
			if( !currentObject.finish()) {
				objects.remove(currentObject);
				model.objectRemoved();
			}
		}
		if( selectB.isSelected() ) {
			map.removeMouseListener( this );
			map.removeMouseMotionListener( this );
			map.removeKeyListener( this );
			map.addMouseListener( this );
			map.addMouseMotionListener( this );
			map.addKeyListener( this );
			for( int k=0 ; k<objects.size() ; k++ ) {
				((DigitizerObject)objects.get(k)).setSelected(false);
			}
			if( currentObject != null ) {
				objectList.setSelectedValue(currentObject, true);
				currentObject.setSelected(true);
			} else {
				objectList.setSelectedIndices( new int[] {} );
			}
			return;
		}
		int i = Integer.parseInt( evt.getActionCommand() );
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.removeKeyListener( this );
		try {
			Class[] c = new Class[] { Class.forName("haxby.util.ScaledComponent"), getClass() };
			Object[] obj= new Object[] { map, this };
			Class clas = (Class)classes.get(i);
			currentObject = (DigitizerObject)clas.getConstructor(c).newInstance(obj);
			objects.add( currentObject );
			currentObject.setName( options.getType() );
			model.objectAdded();
			currentObject.start();
		} catch(Exception ex ) {
			ex.printStackTrace();
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
		if(this.selectB.isSelected() == false)
			return;

		if (SwingUtilities.isRightMouseButton(evt)) return;
		
		if( evt.getSource()==objectList ) {
			if( objects.size()==0 || evt.getX()>16 ) return;
			try {
				int i = objectList.locationToIndex( evt.getPoint() );
				if(i>=0) {
					DigitizerObject obj = (DigitizerObject) objects.get(i);
					obj.setVisible( !obj.isVisible() );
					if( obj.isVisible() ) obj.redraw();
					else map.repaint();
					objectList.repaint();
				}
			} catch (Exception ex) {
			}
		} else if( evt.getSource()==map ) {
			if( evt.isControlDown() ) return;
			DigitizerObject obj = null;
			double[] scales = map.getScales();
			Insets insets = map.getInsets();
			Point p = evt.getPoint();
			p.x -= insets.left;
			p.y -= insets.top;
			if( !evt.isShiftDown() ) {
				for( int i=0 ; i<objects.size(); i++ ) {
					try {
						obj = (DigitizerObject) objects.get(i);
						obj.setSelected( false );
					} catch( Exception ex) {
					}
				}
			}
			int j = lastSelectedIndex+1;
			boolean selection = false;
			for( int i=0 ; i<objects.size() ; i++,j++ ) {
				j %= objects.size();
				try {
					obj = (DigitizerObject) objects.get(j);
				} catch (Exception ex) {
					continue;
				}
				if( obj.select( p.x, p.y, scales ) ) {
					selection = true;
					break;
				}
			}
			if( selection ) {
				lastSelectedIndex = j;
				try {
					obj = (DigitizerObject) objects.get(j);
				} catch( Exception ex) {
					return;
				}
				if( evt.isShiftDown() ) {
					if( obj.isSelected() ) {
						obj.setSelected( false );
						objectList.removeSelectionInterval(j, j);
					} else {
						obj.setSelected( true );
						objectList.addSelectionInterval(j, j);
					}
				} else {
					if( obj.isSelected() && objectList.getSelectedIndices().length==1 )return;
					objectList.setSelectedIndices( new int[] {j} );
					obj.setSelected( true );
				}
			} else if( !evt.isShiftDown() ) {
				if(objectList.getSelectedIndices().length != 0 ) {
					objectList.setSelectedIndices( new int[] {} );
				}
			}
		}
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.isControlDown() ) return;
		if( evt.getSource()==map ) {
			if( objects.size()==0 ) return;
			int[] indices = objectList.getSelectedIndices();
			if(objects.size()==0 || indices.length == 0) return;
			DigitizerObject[] obj = new DigitizerObject[indices.length];
			for( int i=0 ; i<obj.length ; i++) {
				if( indices[i]>=objects.size() )return;
				obj[i] = (DigitizerObject)objects.get(indices[i]);
			}
			if( evt.getKeyCode() == evt.VK_SPACE ) 
			{
				options.showDialog( obj );
				map.repaint();
			} else if( evt.getKeyCode() == evt.VK_DELETE ) {
				for( int i=0 ; i<obj.length ; i++) {
					if( !obj[i].finish() ) obj[i].setSelected( false );
					objects.remove( obj[i] );
				}
				model.contentsChanged();
				map.repaint();
			} else if( evt.getKeyCode() == evt.VK_A ) {
				map.removeMouseListener( this );
				map.removeMouseMotionListener( this );
				map.removeKeyListener( this );
				currentObject = obj[0];
				currentObject.setSelected( false );
				currentObject.start();
			}
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void draw( Graphics2D g ) {
		if( objects.size()==0 ) return;
		
		double[] scales = map.getScales();
		Insets ins = map.getInsets();
		g.translate( -ins.left, -ins.top );

		Rectangle bounds = g.getClipBounds();
		if( bounds==null ) {
			bounds = map.getVisibleRect();
			bounds.x -= ins.left;
			bounds.y -= ins.top;
			bounds.width -= ins.left + ins.right;
			bounds.height -= ins.top + ins.bottom;
		} else {
			bounds.x -= ins.left;
			bounds.y -= ins.top;
		}
		draw( g, scales, bounds, ins);
	}
	public void draw( Graphics2D g, double[] scales, Rectangle bounds, Insets ins ) {
		for( int i=0 ; i<objects.size() ; i++ ) {
			((DigitizerObject)objects.get(i)).draw(g, scales, bounds, ins);
		}
	}
	public void reset() {
		objects = new Vector();
		currentObject = null;
	}
	public Vector getObjects() {
		return objects;
	}
	public DigitizerOptionsDialog getOptionsDialog() {
		return options;
	}	
	
	public void delete()
	{
		int[] indices = objectList.getSelectedIndices();
		DigitizerObject[] obj = new DigitizerObject[indices.length];
		for( int i=0 ; i<obj.length ; i++) {
			if( indices[i]>=objects.size() )return;
			obj[i] = (DigitizerObject)objects.get(indices[i]);
		}
		for( int i=0 ; i<obj.length ; i++) {
			if( !obj[i].finish() ) obj[i].setSelected( false );
			objects.remove( obj[i] );
		}
		model.contentsChanged();
		map.repaint();
	}
	public void deleteLastObject() {
		//this version called by Delete Last Horizon
		deleteLastObject(true);
	}
	
	public void deleteLastObject(boolean createNew) {
		//create New = false if called by Delete Last Pick, when no more picks left in line
		currentObject.finish();
		//if the currentObject has no points, get rid of that one, then get rid of the new Object
		if (currentObject instanceof LineSegmentsObject && ((LineSegmentsObject)currentObject).getPoints().size() == 0) {
			objects.remove(currentObject);
			model.objectRemoved();
			if (objects.size() > 0) currentObject = (DigitizerObject) objects.lastElement();	
		}
		if (createNew) {
			objects.remove(currentObject);
			model.objectRemoved();
			digB.doClick(); // this will create a new current object
			selectB.doClick(); // but don't want to leave the cursor in digitize mode - can cause problems later (eg when reloading horizons)
		}
		map.repaint();
		
	}
	
	public void setCurrentObject(DigitizerObject obj) {
		currentObject = obj;
	}
	
	public DigListModel getModel() {
		return model;
	}
	
	public void setMouseListeners() {
		if (map == null) return;
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.removeKeyListener( this );
		map.addMouseListener( this );
		map.addMouseMotionListener( this );
		map.addKeyListener( this );
	}
	
}