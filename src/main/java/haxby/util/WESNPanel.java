package haxby.util;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class WESNPanel extends JPanel{
	public JTextField north;
	public JTextField east;
	public JTextField south;
	public JTextField west;

	public WESNPanel() {
		super(new BorderLayout());
		JPanel base = new JPanel(new GridLayout(2, 2, 2, 2));

		JPanel p2 = new JPanel();
		p2.add(new JLabel("North"));
		north = new JTextField(10);
		north.addFocusListener( new FocusListener() {
			public void focusLost(FocusEvent e) {}

			public void focusGained(FocusEvent e) {
				north.selectAll();
			}
		});
		p2.add(north);
		base.add(p2);

		p2 = new JPanel();
		p2.add(new JLabel("East"));
		east = new JTextField(10);
		east.addFocusListener( new FocusListener() {
			public void focusLost(FocusEvent e) {}

			public void focusGained(FocusEvent e) {
				east.selectAll();
			}
		});
		p2.add(east);
		base.add(p2);

		p2 = new JPanel();
		p2.add(new JLabel("South"));
		south = new JTextField(10);
		south.addFocusListener( new FocusListener() {
			public void focusLost(FocusEvent e) {}

			public void focusGained(FocusEvent e) {
				south.selectAll();
			}
		});
		p2.add(south);
		base.add(p2);

		p2 = new JPanel();
		p2.add(new JLabel("West"));
		west = new JTextField(10);
		west.addFocusListener( new FocusListener() {
			public void focusLost(FocusEvent e) {

			}
			public void focusGained(FocusEvent e) {
				west.selectAll();
			}
		});
		p2.add(west);
		base.add(p2);
		add(base, BorderLayout.CENTER);

		JLabel format = new JLabel(" Format +-ddd.dd or +-dd mm ss");
		format.setBorder(BorderFactory.createEtchedBorder());
		add(format, BorderLayout.SOUTH);
	}

	public double[] getWESN() {
		double[] wesn = new double[4];
		wesn[0] = processText(west.getText(), false);
		wesn[1] = processText(east.getText(), false);
		wesn[2] = processText(south.getText(), true);
		wesn[3] = processText(north.getText(), true);

		for (double d : wesn)
			if (Double.isNaN(d))
				return null;
		return wesn;
	}

	public double processText(String text, boolean lat) {
		boolean negate = false;

		text = text.trim();
		text = text.toUpperCase();
		if (lat)
			if (text.contains("S")) {
				text = text.replaceFirst("S", "");
				negate = true;
			} else {
				text = text.replaceFirst("N", "");
			}
		else
			if (text.contains("W")) {
				text = text.replaceFirst("W", "");
				negate = true;
			} else {
				text = text.replaceFirst("E", "");
			}

		String[] split = text.split(" ");
		double degrees;
		try {
			degrees = Double.parseDouble(split[0]);
		} catch (NumberFormatException ex) {
			return Double.NaN;
		}

		if (split.length == 1)
			return negate ? -degrees : degrees;

		double minutes;
		try {
			minutes = Double.parseDouble(split[1]) / 60;
			if (degrees < 0) degrees -= minutes;
			else degrees += minutes;
		} catch (NumberFormatException ex) {
			return Double.NaN;
		}

		if (split.length == 2)
			return negate ? -degrees : degrees ;

		double seconds;
		try {
			seconds = Double.parseDouble(split[2]) / 3600;
			if (degrees < 0) degrees -= seconds;
			else degrees += seconds;
		} catch (NumberFormatException ex) {
			return Double.NaN;
		}

		if(split.length == 3)
			return degrees;
		else
			return Double.NaN;
	}

	public void setWESN(double[] wesn) {
		if (wesn == null) {
			north.setText("");
			south.setText("");
			east.setText("");
			west.setText("");
		}
		else {
			west.setText(""+wesn[0]);
			east.setText(""+wesn[1]);
			south.setText(""+wesn[2]);
			north.setText(""+wesn[3]);
		}
	}

	public static void main(String[] args) {
		JDialog d = new JDialog();
		final WESNPanel p = new WESNPanel();
		d.getContentPane().add(p);
		JButton b = new JButton("Parse!");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double wesn[] = p.getWESN();
				if (wesn == null)
					System.out.println("null");
				else {
					for (double d : wesn)
						System.out.print(d + " ");
				}
			}
		});
		d.getContentPane().add(b, BorderLayout.SOUTH);
		d.pack();
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setVisible(true);
	}
}
