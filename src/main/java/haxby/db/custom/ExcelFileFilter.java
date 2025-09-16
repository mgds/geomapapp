package haxby.db.custom;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ExcelFileFilter extends FileFilter {

	public boolean accept(File f){
		if (f.isDirectory()) return true;
		String ext = getExtension(f);
		// For excel files formats of xls or xlsx are accepted.
		if (ext != null && ext.contentEquals("xls")) return ext.equalsIgnoreCase("xls");
		if (ext != null && ext.contentEquals("xlsx")) return ext.equalsIgnoreCase("xlsx");
		return false;
	}

	public String getDescription() {
		return "Excel Files (.xls .xlsx)";
	}

	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}
}