package haxby.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * FileUtil checks if selected file is in parameter. Constructs
 * a temporary file excluding the omitted line. The temp
 * file will replace original file.
 *
 * @author Samantha Chan
 * @version 2.4.4
 * @since 2.0.4
 */

public class FilesUtil{

	protected static File gmaRoot = org.geomapapp.io.GMARoot.getRoot();

	public static void removeLineinFile(String file, String lineDelete){
		try{
			File selectFile = new File(gmaRoot + "/places/" + file);
			if(!selectFile.isFile()){
				System.out.println("The selected file is out of parameter");
				return;
			}

			File tempFile = new File(gmaRoot + "/places/" + file+ ".tmp");
				BufferedReader br = new BufferedReader(new FileReader(selectFile));
				PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
				String line = null;

				//Read from the original file and write to the new
				//unless content matches data to be removed.
				while ((line = br.readLine()) != null) {
				if (!line.trim().equals(lineDelete)) {
				pw.println(line);
				pw.flush();
				}
			}
			pw.close();
			br.close();

			//Delete the original file
			if (!selectFile.delete()) {
				System.out.println("Could not delete file");
				return;
			}

			//Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(selectFile))
				System.out.println("Could not rename files");
			}
			catch (FileNotFoundException ex) {
				ex.printStackTrace();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	public static void insertLineinFile(String file1, String file2){
		try{
			File selectFile1 = new File(gmaRoot + "/places/" + file1);
			File selectFile2 = new File(file2);
			if(!selectFile2.isFile()){
				System.out.println("The selected file is out of parameter");
				return;
			}
			File tempFile = new File(gmaRoot + "/places/My Places.loc.tmp");
			BufferedReader br1 = new BufferedReader(new FileReader(selectFile1));
			BufferedReader br2 = new BufferedReader(new FileReader(selectFile2));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line1 = null;
			String line2 =null;

			//Copy existing My Places to the .tmp file
			while ((line1 = br1.readLine())!= null) {
				pw.println(line1);
				pw.flush();
			}
			//Copy import file to the .tmp file
			while((line2 = br2.readLine()) != null){
				pw.println(line2);
				pw.flush();
			}

			pw.close();
			br2.close();
			br1.close();

			//Delete the original file
			if (!selectFile1.delete()) {
				System.out.println("Could not delete file");
				return;
			}

			//Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(selectFile1))
			System.out.println("Could not rename files");
			}
			catch (FileNotFoundException ex) {
			ex.printStackTrace();
			}
			catch (IOException ex) {
			ex.printStackTrace();
			}
		}

	public static void addLineinFile(String orig, String lineAdd){
		try{
			File selectFile1 = new File(gmaRoot + "/places/" + orig);
			File tempFile = new File(gmaRoot + "/places/My Places.loc.tmp");
			BufferedReader br1 = new BufferedReader(new FileReader(selectFile1));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line1 = null;

			//Copy existing My Places to the .tmp file
			while ((line1 = br1.readLine())!= null) {
				pw.println(line1);
				pw.flush();
			}
			//Copy import file to the .tmp file
				pw.println(lineAdd);
				pw.flush();

				pw.close();
				br1.close();

			//Delete the original file
			if (!selectFile1.delete()) {
				System.out.println("Could not delete file");
				return;
			}

			//Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(selectFile1))
				System.out.println("Could not rename files");
			}
			catch (FileNotFoundException ex) {
				ex.printStackTrace();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	public static void clearFile(String file){
		File selectFile = new File(file);
		if(!selectFile.isFile()){
			System.out.println("The selected file is out of parameter");
			return;
		}
		File tempFile = new File(file + ".tmp");
		//Delete the original file
		if (!selectFile.delete()) {
			System.out.println("Could not delete file");
			return;
		}
		//Rename the new file to the filename the original file had.
		if (!tempFile.renameTo(selectFile))
			System.out.println("Could not rename files");
		}

	public static void copyFile(String origFile, String cloneFile){
		File originalFile = new File(origFile);
		File clonedFile = new File(cloneFile);
		if(originalFile.isFile()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(originalFile));
				PrintWriter pw = new PrintWriter(new FileWriter(clonedFile));
				String readLine = null;
				while ((readLine = br.readLine())!= null) {
					pw.println(readLine);
					pw.flush();
				}
				pw.close();
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
		System.gc();
	}

	public static String fileTimeEST(){
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		String DATE_FORMAT = "yyyy-MM-dd_HH:mm:ss-'GMT'Z";
		SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
		//sdf.setTimeZone(TimeZone.getTimeZone("EST"));
		String tstamp = sdf.format(cal.getTime());
		return tstamp;
	}
	
	public static String createSha1(File oriFileLocal) {
		try {
     		return org.apache.commons.codec.digest.DigestUtils.sha1Hex(new FileInputStream(oriFileLocal));
		} catch(IOException io) {
			return null;
		}
	}

	/* Creates the necessary directories if it doesn't already exist,
	 * Writes the XML layer information into a saved file.
	 */
	public static void writeLayerToFile(String fileContents, File xmlFile){
		//Creates layers directory if one does not exists
		File layerDir = new File(gmaRoot + File.separator +"layers");
		if( !layerDir.exists() ) {
			layerDir.mkdirs();
		}
		// Layers directory exists then writes to tempLayerFile
		if ( layerDir.exists() ) {
			BufferedWriter bufferedWriter = null;
			//File tempLayerFile = new File(layerDir + File.separator + "layers.tmp");
			try{
				if(!xmlFile.exists()){
					bufferedWriter = new BufferedWriter(new PrintWriter(xmlFile));
					bufferedWriter.write(fileContents);
					bufferedWriter.newLine();
				}
				// If tempLayerFile exists append output to file
				if(xmlFile.exists()){
					BufferedReader br = new BufferedReader(new FileReader(xmlFile));
					bufferedWriter = new BufferedWriter(new FileWriter(xmlFile,true));
					bufferedWriter.write(fileContents);
					bufferedWriter.newLine();
				}

			}catch(FileNotFoundException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}finally{
				try{
					if(bufferedWriter !=null){
					bufferedWriter.flush();
					//Close the output stream
					bufferedWriter.close();
				}
				}catch (IOException ex){
					ex.printStackTrace();
				}
			}
		}
	}

	public static void processFileToLayer(File xmlImportFile, String xmlSessionFile) throws IOException{
		System.out.println("The Session File to import" + xmlImportFile);
		//Creates layers directory if one does not exists
		File layerDir = new File(gmaRoot + File.separator +"layers");
		if( !layerDir.exists() ) {
			layerDir.mkdirs();
		}

		File mySessionFile = new File(layerDir + File.separator + xmlSessionFile);
		BufferedReader brProcess = new BufferedReader(new FileReader(xmlImportFile)); //desktop
		PrintWriter pw = new PrintWriter(new FileWriter(mySessionFile));
		String s1;
		s1 = brProcess.readLine();

				pw.println("<My_Sessions_Menu>");
				while(s1!=null){
					pw.println(s1);
					s1 = brProcess.readLine();
					pw.flush();
				}
				// Write Reload Sessions
				pw.write("<layer" + '\r' + "\t name=" + '"' + "Refresh My Sessions" +
						'"' + '\n' + '\t'+ "proj=" + '"' + "nsm" + '"' +
						'\n' + '\t' + "separator_bar=" + '"' + "above" + '"' +
						'\n' + '\t'+ "command=" + '"' + "reload_layer_session_cmd" + '"' + "/>" + '\n');
				// Write Import Sessions
				pw.write("<layer" + '\r' + "\t name=" + '"' + "Import Another Session" +
						'"' + '\n' + '\t'+ "proj=" + '"' + "nsm" + '"' +
						'\n' + '\t'+ "command=" + '"' + "import_layer_session_cmd" + '"' + "/>" + '\n');
				// Write Close Layer
				pw.write("<layer" + '\r' + "\t name=" + '"' + "Close and Discard My Sessions Menu" +
						'"' + '\n' + '\t'+ "proj=" + '"' + "nsm" + '"' +
						 '\n' + '\t' + "separator_bar=" + '"' + "above" + '"' +
						 '\n' + '\t' + "command=" + '"' + "close_layer_session_cmd" + '"' + "/>" + '\n');
				pw.write("</My_Sessions_Menu>");
				pw.flush();
				pw.close();
				brProcess.close();
	}

	/*
	 * search through the SessionFile to see if it already contains the import file
	 */
	public static boolean sessionAlreadyImported(File xmlImportFile, File xmlSessionFile) throws IOException {
		BufferedReader br1 = new BufferedReader(new FileReader(xmlSessionFile)); //sessionfile
		BufferedReader br2 = new BufferedReader(new FileReader(xmlImportFile)); //desktop import
		String s1, s2;
		
		s1 = br1.readLine();
		s2 = br2.readLine();
		s2 = br2.readLine();
		br2.mark(2000);;
		while(s1 != null) {		
			while (s1 != null && s2 != null && s1.equals(s2)) {
				s1 = br1.readLine();
				s2 = br2.readLine();
				if (s2 == null) {
					br1.close();
					br2.close();
					return true;
				}
			}
			br2.reset();
			s1 = br1.readLine();
		}
		br1.close();
		br2.close();
		return false;
	}
	
	public static void multiFileToLayer(File xmlImportFile, File xmlSessionFile) throws IOException{
		if (sessionAlreadyImported(xmlImportFile, xmlSessionFile)) return;
		
		File layerDir = new File(gmaRoot + File.separator +"layers");
		File mySessionTmp = new File(xmlSessionFile +".tmp");
		PrintWriter pw2 = new PrintWriter(new FileWriter(mySessionTmp,false));
		BufferedReader br1 = new BufferedReader(new FileReader(xmlSessionFile)); //sessionfile
		BufferedReader br2 = new BufferedReader(new FileReader(xmlImportFile)); //desktop import
		String s1, s2;
		s1 = br1.readLine();
		s2 = br2.readLine();

		//Copy import session to temp file
		pw2.println("<My_Sessions_Menu>");
		while(s2!=null){
			pw2.println(s2);
			s2 = br2.readLine();
		pw2.flush();
		}
		//Copy previous imported session to temp file
		while(s1!=null){
			if(s1.contains("<My_Sessions_Menu>")){
				s1 = br1.readLine();
			}
			pw2.println(s1);
			s1 = br1.readLine();
			pw2.flush();
		}
		pw2.close();
		if(xmlSessionFile.exists()){
			 PrintWriter pw1 = new PrintWriter(new FileWriter(xmlSessionFile,false));
			 BufferedReader br3 = new BufferedReader(new FileReader(mySessionTmp)); 
			 String s3;
				s3 = br3.readLine();
				while(s3!=null){
					pw1.println(s3);
					s3 = br3.readLine();
					pw1.flush();
				}
				pw1.close();
				mySessionTmp.delete(); //Delete Temp For Now.
		}
	}

	public static void renameFileToLayer(String tempName) throws IOException{
		//Creates layers directory if one does not exists
		File layerDir = new File(gmaRoot + File.separator + "layers");
		File tempFile = new File(layerDir + File.separator + tempName);
		File origFile = new File(layerDir + File.separator + "MySessions.xml");
		if( tempFile.exists() ) {
			if (!origFile.delete()) {
				System.out.println("Could not delete file");
			return;
			}
			//Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(origFile))
			System.out.println("Could not rename files");
		}
	}

	public static void clearLayerFile(String file){
		File selectFile = new File(file);
		if(!selectFile.isFile()){
			System.out.println("The selected file is out of parameter");
			return;
		}
		//Delete the original file
		if (!selectFile.delete()) {
			System.out.println("Could not delete file");
			return;
		}
	}

	/*
	 * Checks the layers temp file to see if the item already exists.
	 * Returns a boolean
	 */
	public static boolean containsItemCheck(String fileContents)throws IOException{
		File layerDir = new File(gmaRoot + File.separator +"layers");
		File tempLayerFile = new File(layerDir + File.separator + "layers.tmp");

		if( !tempLayerFile.exists()) {
			return false;
		}
		if((tempLayerFile.exists()) && (tempLayerFile!=null)){
			BufferedReader bufferedReader = null;
			bufferedReader = new BufferedReader(new FileReader(tempLayerFile));
			String s;
			s = bufferedReader.readLine();

			/* Read each line in file and if it contains the contents return
			 * true otherwise return false. Keep doing this till you reach
			 * the end of the file and throw a null exception.
			 */
			try{
				while ( (tempLayerFile!=null) && (!s.contains(fileContents))) {
					s = bufferedReader.readLine();
					//System.out.println("String in file " + s);
					while ( s.contains(fileContents)) {
						return true;
					}
				}
			}catch (NullPointerException e) {
				return false;
			}
		}
		return false;
	}

	public static void main(String[]args) {
		//FilesUtil.removeLineinFile("My Places.loc", "A Place Two	128.8125	50.01903486901741	2.0");
		//FilesUtil.insertLineinFile("My Places.loc", "/Bookmarks.loc")
		//FilesUtil.clearFile("/../Bookmarks.loc")
		//fileTimeEST();
	}
}