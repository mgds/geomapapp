package haxby.db.scs;

import haxby.image.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SCSTools implements ActionListener {
	SCSRegister register;
	SCSLineDigitizer dig;
	Box panel = null;
	JLabel label;
	JToggleButton[] buttons;
	JButton save;
	JDialog colorDialog = null;
	BalancePanel balance;

	public SCSTools( SCSRegister register, SCSLineDigitizer dig ) {
		this.register = register;
		this.dig = dig;
		dig.reset();
	}
	public JDialog getColorDialog() {
		if( colorDialog==null ) {
			balance = new BalancePanel( this );
			colorDialog = new JDialog( (JFrame)register.getTopLevelAncestor(), "color balance" );
			colorDialog.getContentPane().add( balance );
			colorDialog.pack();
		}
		return colorDialog;
	}
	public void showColorDialog() {
		getColorDialog().show();
	}
	public Box getPanel() {
		if( panel==null ) initPanel();
		return panel;
	}
	void initPanel() {
		panel = Box.createHorizontalBox();
		ButtonGroup gp = new ButtonGroup();
		buttons = new JToggleButton[6];
		buttons[0] = new JToggleButton( Icons.getIcon( Icons.SELECT, false));
		buttons[0].setSelectedIcon( Icons.getIcon( Icons.SELECT, true) );
		Color c = new Color(0,0,0,0);

		byte[][] map = new byte[][] {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,0,0,0,0,1,1,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,1,1,0,1,1,0,0,0,0},
			{0,0,0,0,0,0,0,1,1,0,0,0,1,1,1,0,0,0},
			{0,0,0,0,0,0,1,1,0,0,0,0,1,1,1,1,0,0},
			{0,0,0,0,1,1,0,0,1,0,0,0,1,1,1,1,1,0},
			{1,1,1,1,0,0,0,0,1,0,0,0,1,1,1,1,0,0},
			{1,0,1,0,0,0,0,0,0,1,0,0,1,0,1,1,0,0},
			{1,1,1,1,0,0,0,0,0,1,0,0,0,0,0,1,0,0},
			{0,0,0,0,1,1,0,0,0,0,1,0,0,0,0,1,1,0},
			{0,0,0,0,0,0,1,1,0,0,1,0,0,0,0,0,1,0},
			{0,0,0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
		};
		buttons[1] = new JToggleButton( Icons.doIcon( map, false, c));
		buttons[1].setSelectedIcon( Icons.doIcon( map, true, c));
		buttons[1].setToolTipText( "digitize horizontal" );

		map = new byte[][] {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0},
			{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0},
			{0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0},
			{0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0},
			{0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
			{0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
		};
		buttons[2] = new JToggleButton( Icons.doIcon( map, false, c));
		buttons[2].setSelectedIcon( Icons.doIcon( map, true, c));
		buttons[2].setToolTipText( "digitize time gap" );
		
		map = new byte[][] {
			{0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,1,1,0,0,0,1,1,0,0,0},
			{0,0,1,0,0,0,0,1,0,0,0,1,0,0,0,1,0,0},
			{0,0,1,0,0,0,1,0,0,0,1,1,1,0,0,0,1,0},
			{0,0,1,1,0,0,1,0,0,0,0,1,0,0,0,0,1,0},
			{0,0,1,1,1,1,0,0,0,0,0,1,0,1,0,0,0,1},
			{0,0,1,1,1,1,0,0,0,0,0,1,1,1,1,0,0,1},
			{0,0,1,1,1,1,1,0,0,0,0,0,0,1,0,0,0,1},
			{0,0,1,1,1,1,1,1,0,0,0,0,0,0,0,0,1,0},
			{0,0,1,1,1,1,0,0,0,0,0,0,0,0,0,0,1,0},
			{0,0,1,1,0,1,0,1,0,0,0,0,0,0,0,1,0,0},
			{0,0,1,0,0,0,1,0,1,1,0,0,0,1,1,0,0,0},
			{0,0,1,0,0,0,1,0,0,0,1,1,1,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
		};
		buttons[3] = new JToggleButton( Icons.doIcon( map, false, c));
		buttons[3].setSelectedIcon( Icons.doIcon( map, true, c));
		buttons[3].setToolTipText( "digitize time stamp" );
		
		map = new byte[][] {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
			{0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
		};
		buttons[4] = new JToggleButton( Icons.doIcon( map, false, c));
		buttons[4].setSelectedIcon( Icons.doIcon( map, true, c));
		buttons[4].setToolTipText( "digitize 2-way time" );

		map = new byte[][] {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,1},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1},
			{0,0,0,0,0,0,0,0,1,1,0,0,0,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,1,1},
			{0,0,0,0,0,0,0,0,1,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,1},
			{0,0,0,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0},
			{1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0},
			{1,1,1,1,1,1,0,0,1,0,0,0,0,0,0,0,0,0},
			{1,1,1,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0},
		};
		buttons[5] = new JToggleButton( Icons.doIcon( map, false, c));
		buttons[5].setSelectedIcon( Icons.doIcon( map, true, c));
		buttons[5].setToolTipText( "digitize 2-way time offset" );
		
		buttons[0].setSelected(true);
		for(int k=0 ; k<buttons.length ; k++) {
			panel.add( buttons[k] );
			gp.add( buttons[k] );
			buttons[k].addActionListener( this );
			buttons[k].setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		}
		save = new JButton(Icons.getIcon(Icons.SAVE,false));
		save.setPressedIcon( Icons.getIcon(Icons.SAVE,true) );
		save.setToolTipText( "save panels" );
		save.setActionCommand( "save" );
		save.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		panel.add( Box.createHorizontalStrut( 5 ) );
		panel.add( save );
		save.addActionListener( register );

		label = new JLabel(" ");
		panel.add( label );
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource()==save ) {
			return;
		}
		if( evt.getSource() == balance ) {
			register.setLookup( balance.getLookup() );
		} else if( buttons[0].isSelected() ) {
			label.setText(" ");
			dig.edit();
		} else if( buttons[1].isSelected() ) {
			label.setText(" digitize horizontal - press RETURN to finish, DELETE to restart ");
			dig.digitize(1);
		} else if( buttons[2].isSelected() ) {
			label.setText(" digitize time gap ");
			dig.digitize(2);
		} else if( buttons[3].isSelected() ) {
			label.setText(" digitize time stamp ");
			dig.digitize(3);
		} else if( buttons[4].isSelected() ) {
			label.setText(" digitize 2-way time ");
			dig.digitize(4);
		} else if( buttons[5].isSelected() ) {
			label.setText(" digitize 2-way time offset");
			dig.digitize(5);
		}
	}
	void setMode( int mode ) {
		for(int k=1 ; k<6 ; k++ ) {
			buttons[k].setEnabled( ENABLE[mode][k-1] );
		}
		save.setEnabled( !ENABLE[mode][0] );
		buttons[0].doClick();
	}
	static boolean[][] ENABLE = {
		{ true, false, false, false, false },
		{ false, true, true, true, true }
	};
}