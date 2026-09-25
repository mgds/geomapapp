package haxby.db.custom;

import haxby.util.URLFactory;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geomapapp.image.ImageBox;
import org.geomapapp.util.Icons;

public class DataSetImageViewer extends JDialog implements ListSelectionListener {
	UnknownDataSet data;
	ImageBox image;
	Vector<URL> imgs;
	Vector<String> imgNames;
	int col;
	int row=-1;
	JComboBox combo;

	public DataSetImageViewer(UnknownDataSet data, JFrame owner) {
		super(owner);
		this.data = data;
		imgs = new Vector<URL>();
		imgNames = new Vector<String>();
		initGUI();
		updateRow();
	}

	public DataSetImageViewer(UnknownDataSet data, JDialog owner) {
		super(owner);
		this.data = data;
		imgs = new Vector<URL>();
		imgNames = new Vector<String>();
		initGUI();
		updateRow();
	}

	void initGUI() {
		JPanel panel = new JPanel();

		combo = new JComboBox(imgNames);
		combo.setPreferredSize(new Dimension(100,30));
		combo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		panel.add(combo);

		JButton back = new JButton(Icons.getIcon(Icons.BACK, false));
		back.setPressedIcon( Icons.getIcon(Icons.BACK, true ));
		back.setBorder( BorderFactory.createEmptyBorder(2,2,2,2));
		back.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				back();
			}
		});
		panel.add(back);

		JButton forward = new JButton(Icons.getIcon(Icons.FORWARD, false));
		forward.setPressedIcon( Icons.getIcon(Icons.FORWARD, true ));
		forward.setBorder( BorderFactory.createEmptyBorder(2,2,2,2));
		forward.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				forward();
			}
		});
		panel.add(forward);

		getContentPane().add(panel, "North");
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );

		image = new ImageBox(null);
		getContentPane().add(image);

		addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				data.db.thumbsB.setSelected(false);
				data.thumbs = false;
				setVisible(false);
			}
		});

		pack();
		data.dataT.getSelectionModel().addListSelectionListener(this);
	}
	void back() {
		int row = data.dataT.getSelectedRow();
		if( row==0 )return;
		row--;
		data.dataT.setRowSelectionInterval(row, row);
		data.dataT.ensureIndexIsVisible( row );
	}
	void forward() {
		int row = data.dataT.getSelectedRow();
		if( row==data.dataT.getRowCount()-1 )return;
		row++;
		data.dataT.setRowSelectionInterval(row, row);
		data.dataT.ensureIndexIsVisible( row );
	}
	void open() {
		int[] selectedRows = data.dataT.getSelectedRows();
		if (selectedRows.length == 0) return;
		int row = selectedRows[0];
		if( row==-1 )return;
		int col = combo.getSelectedIndex();
		if( row==this.row && col==this.col ) return;
		if (imgs.size()==0) return;
		this.row=row;
		this.col=col;
		image.setImage(imgs.get(col));
	}

	public void updateRow() {
		int r = data.dataT.getSelectedRow();
		if (r==-1 || !data.thumbs) {
			setVisible(false);
		} else if (r!=row){
			imgs.clear();
			imgNames.clear();
			for (int i = 0; i < data.dataT.getColumnCount(); i++) {
				// JOC: Fixed a casting problem
				String str2 = data.dataT.getValueAt(r, i).toString();
				String str = str2.toLowerCase();
				if (str.endsWith(".gif") || str.endsWith(".jpg")
						|| str.endsWith(".jpeg") || str.endsWith(".bmp")
						|| str.endsWith(".tif") || str.endsWith(".png")) {
					try {
						imgs.add(URLFactory.url(str2));
						imgNames.add(str2.substring(str2.lastIndexOf("/")+1));
					} catch (MalformedURLException ex) {
						
					}
				}
			}
			combo.setSelectedIndex(Math.min(col, imgs.size()-1));
			combo.invalidate();
			this.row = r;
			setVisible(imgs.size()!=0);
			
		} else {
			open();
			setVisible(imgs.size()!=0);
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;

		updateRow();
	}

	@Override
	public void dispose() {
		imgs.clear();
		imgNames.clear();
		data = null;

		setVisible(false);
		try {
			super.dispose();
		} finally
		{
			try {
				finalize();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
