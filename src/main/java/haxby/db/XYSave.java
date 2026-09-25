package haxby.db;

import haxby.map.*;

import java.io.*;
import java.util.*;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;

import javax.swing.*;
import javax.imageio.*;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class XYSave {
	XYGraph graph;
	JToggleButton saveJPEG;
	JToggleButton savePNG;
	JToggleButton saveToFile;
	JToggleButton saveToClipboard;
	JToggleButton print;
	JToggleButton saveToExcel;
	JToggleButton autoX, autoY;
	JTextField xScaleT, yScaleT;
	JComboBox cb;
	public XYSave( XYGraph graph ) {
		this.graph = graph;
	}
	private JComponent makeMenu(boolean popup) {
		JComponent fileMenu;
		if (popup) fileMenu = new JPopupMenu();
		else fileMenu = new JMenu("Save");
		JMenuItem mi;
		JMenu dataMenu = new JMenu("Save Graph Data");
		JMenu imageMenu = new JMenu("Save Graph Image");
		mi = new JMenuItem("Copy to clipboard");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToClipboard.doClick();
				save();
			}
		});
		dataMenu.add(mi);
		mi = new JMenuItem("Save JPEG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveJPEG.doClick();
				save();
			}
		});
		imageMenu.add(mi);
		mi = new JMenuItem("Save PNG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				savePNG.doClick();
				save();
			}
		});
		imageMenu.add(mi);
		mi = new JMenuItem("Save ASCII table");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToFile.doClick();
				save();
			}
		});
		dataMenu.add(mi);
		mi = new JMenuItem("Save as Excel");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToExcel.doClick();
				save();
			}
		});
		dataMenu.add(mi);
		mi = new JMenuItem("Print");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print.doClick();
				save();
			}
		});
		fileMenu.add(dataMenu);
		fileMenu.add(imageMenu);
		fileMenu.add(mi);

		return fileMenu;
	}
	public JMenu getSaveMenu() {
		initSave();
		return (JMenu) makeMenu(false);
	}
	public JPopupMenu getPopupSaveMenu() {
		initSave();
		return (JPopupMenu) makeMenu(true); 
	}
	void save() {
		Container dialog = graph.getTopLevelAncestor();
		if( dialog==null )return;
		int ok= JOptionPane.CANCEL_OPTION;
		if( print.isSelected() ) {
			PrinterJob job = PrinterJob.getPrinterJob();
			if( fmt==null) {
				PageFormat f = job.defaultPage();
				PageFormat newF = job.pageDialog(f);
				if( newF==f )return;
				fmt = newF;
			} else {
				PageFormat f = job.pageDialog(fmt);
				if( f==fmt ) return;
				fmt = f;
			}
			job.setPrintable(graph, fmt);
			try {
				if(job.printDialog()) job.print();
			} catch (PrinterException pe) {
			}
			return;
		} else if( saveToFile.isSelected() ) { 
			JFileChooser chooser = MapApp.getFileChooser();
			File f = new File("untitled.txt");
			chooser.setSelectedFile(f);
			ok = chooser.showSaveDialog(dialog);
			if( ok==chooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			try {
				PrintStream out = new PrintStream(
					new FileOutputStream( chooser.getSelectedFile() ));
				Iterator it = ((XYPoints2)graph.xy).getData(0);
				if (it!=null)
					while (it.hasNext())
						out.println(it.next());
				out.close();
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( saveToClipboard.isSelected() ) {
			JTextArea text = new JTextArea();
			Iterator it = ((XYPoints2)graph.xy).getData(0);
			while (it.hasNext())
				text.append(it.next().toString()+"\n");
			text.selectAll();
			text.copy();

		} else if( saveJPEG.isSelected() ) {
			JFileChooser chooserb = MapApp.getFileChooser();
			File f=new File("untitled.jpg");
			chooserb.setSelectedFile(f);
			ok = chooserb.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooserb.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			BufferedImage image = graph.getImage();
			File jpgFile = chooserb.getSelectedFile();

			while( !jpgFile.getName().endsWith(".jpg") ) {
				JOptionPane.showMessageDialog(dialog, 
				"File name must end with \".jpg\"");
				ok = chooserb.showSaveDialog(dialog);
				if( ok==JFileChooser.CANCEL_OPTION ) {
					return;
				}
				jpgFile = chooserb.getSelectedFile();
			}

			try {
				ImageIO.write(image,
					"jpg",
					jpgFile);
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( savePNG.isSelected() ) {
			JFileChooser chooserc = MapApp.getFileChooser();
			File f = new File("untitled.png");
			chooserc.setSelectedFile(f);
			ok = chooserc.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooserc.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			BufferedImage image = graph.getImage();
			File pngFile = chooserc.getSelectedFile();

			while( !pngFile.getName().endsWith(".png") ) {
				JOptionPane.showMessageDialog(dialog, 
				"File name must end with \".png\"");
				ok = chooserc.showSaveDialog(dialog);
				if( ok==JFileChooser.CANCEL_OPTION ) {
					return;
				}
				pngFile = chooserc.getSelectedFile();
			}

			try {
				ImageIO.write(image, "png", pngFile);
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( saveToExcel.isSelected()) {
			JFileChooser chooserd = MapApp.getFileChooser();
			File f = new File("untitled.xls");
			chooserd.setSelectedFile(f);
			ok = chooserd.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooserd.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			try {
				WritableWorkbook wb = Workbook.createWorkbook(chooserd.getSelectedFile());
				WritableSheet sheet = wb.createSheet("First Sheet", 0);
				Iterator it = ((XYPoints2)graph.xy).getData(0);
				int j=0;
				while (it.hasNext()) {
					String s = it.next().toString();
					int i=0;
					while (true) {
						if (s.indexOf("\t")<0){
							sheet.addCell( new Label(i,j,s));
							break;
						}
						String s2 = s.substring(0, s.indexOf("\t"));
						s = s.substring(s.indexOf("\t")+1);
						sheet.addCell( new Label(i,j,s2));
						i++;
					}
					j++;
				}
				wb.write();
				wb.close();
			} catch(Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	int askOverWrite() {
		Container dialog = graph.getTopLevelAncestor();
		JFileChooser choosere = MapApp.getFileChooser();
		int ok = JOptionPane.NO_OPTION;
		while( true ) {
			ok = JOptionPane.showConfirmDialog(dialog,
				"File exists. Overwrite?",
				"Overwrite?",
				JOptionPane.YES_NO_CANCEL_OPTION);
			if( ok!=JOptionPane.NO_OPTION) return ok;
			ok = choosere.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return JOptionPane.CANCEL_OPTION;
			if( !choosere.getSelectedFile().exists() ) return JOptionPane.YES_OPTION;
		}
	}
	PageFormat fmt;
	JPanel savePanel;
	void initSave() {
		savePanel = new JPanel(new GridLayout(0,1));
		savePanel.setBorder( BorderFactory.createTitledBorder("Save Options"));
		ButtonGroup gp = new ButtonGroup();
		saveToFile = new JToggleButton("Save ASCII table");
		savePanel.add( saveToFile );
		gp.add( saveToFile );
		saveToClipboard = new JToggleButton("Copy to clipboard");
		savePanel.add( saveToClipboard );
		gp.add( saveToClipboard );
		saveJPEG = new JToggleButton("Save JPEG image");
		savePanel.add( saveJPEG );
		gp.add( saveJPEG );
		savePNG = new JToggleButton("Save PNG image");
		savePanel.add( savePNG );
		gp.add( savePNG );
		saveToExcel = new JToggleButton("Save Excel table");
		savePanel.add( saveToExcel );
		gp.add( saveToExcel );
		print = new JToggleButton("Print");
		savePanel.add( print );
		gp.add( print );
	}

	public void dispose() {
		this.graph = null;
	}
}