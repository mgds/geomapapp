package org.geomapapp.image;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class Palette implements Cloneable {
	public final static int DEFAULT = 0;
	public final static int HAXBY = 0;
	public final static int OCEAN = 1;
	public final static int LAND  = 2;
	public final static int BLACK_WHITE  = 3;
	public final static int WHITE_BLACK  = 3;
	public final static int RAINBOW  = 2;
	public final static int RAINBOWINVERT  = 2;
	public final static String NAMED_COLORS_FILE = "org/geomapapp/resources/cpt/named_colors.csv";
	//hash table containing RGB values for named colors 
	public final static Hashtable<String, List<Float>> color2RGB = getColor2RGB();

	int ID;
	public final static String[] basicResources = new String[] {
				"default",
				"ocean",
				"land",
				"black_white",
				"white_black",
				};
	public final static String[] resources = new String[] {
			"default",
			"ocean",
			"land",
			"black_white",
			"white_black",
			"rainbow",
			"rainbow_invert",
			"abyss",
			"allen",
			"bathy",
			"bouguer",
			"categorical",
			"cool",
			"copper",
			"copper2",
			"cubhelix",
			"cyclic",
			"dem1",
			"dem2",
			"dem3",
			"dem4",
			"drywet",
			"elevation",
			"etopo1",
			"gebco",
			"gebco_ocean",
			"globe",
			"globe_ocean",
			"gray",
			"haxby",
			"haxby2",
			"hot",
			"hot2",
			"hslice1",
			"ibcso",
			"jet",
			"jet2",
			"jet2_invert",
			"land_sea",
			"nighttime",
			"no_green",
			"no_green2",
			"ocean2",
			"ocean_ocean",
			"paired",
			"panoply",
			"polar",
			"polar2",
			"polar2_invert",
			"rainbow_hue300",
			"red2green",
			"relief",
			"relief_ocean",
			"seafloor",
			"sealand",
			"sealand_ocean",
			"seis",
			"seis2",
			"split",
			"split2",
			"topo",
			"topo_ocean",
			"wysiwyg",
			"wysiwyg2"
			};
	public boolean sunIllum = true;
	float[] red, green, blue, ht;
	float[] range;
	float hScale;
	int[] rgbmap = new int[1001];
	int minRGB = 50;
	int maxRGB = 256;
	double gamma,
			ve;		// vertical exaggeration;
	float interval;
	String name;
	JPanel savePanel;
	JTextField nameF;
	JCheckBox listCB, fileCB;
	JComboBox customPaletteList;
	private File root = org.geomapapp.io.GMARoot.getRoot();

	public Palette(float[] red, float[] green, float[] blue, float[] ht) {
		setGamma(1.);
		setModel( red, green, blue, ht);
		name = "unknown";
		ID = -1;
		interval = -1f;
		ve = 1.;
	}
	public Palette(int id) {
		setModel(id);
		setGamma(1.);
		ID = id;
	}
	public Palette(File file) throws IOException {
		readLUT( new FileInputStream(file) );
		name = file.getName();
		name = name.substring(0,name.indexOf(".lut"));
	}
	public Palette(String url) throws IOException {
		setGamma(1.);
		readLUT( (haxby.util.URLFactory.url(url)).openStream() );
		name = url.substring(url.lastIndexOf("/")+1);
		name = name.substring(0,name.indexOf(".lut"));
	}
	public int getID() {
		return ID;
	}
	public void setID(int id) {
		ID = id;
	}
	public String toString() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setModel(int id) {
		if(id<0 || id>=resources.length )id=0;
		try {
			ClassLoader cl = Palette.class.getClassLoader();
			String urlName = "org/geomapapp/resources/lut/"+resources[id]+".lut";
			java.net.URL url = cl.getResource(urlName);
			if (url == null) {
				//not a lut file, must be a cpt file
				urlName = "org/geomapapp/resources/cpt/"+resources[id]+".cpt";
				url = cl.getResource(urlName);
				readCPT( url.openStream() );
			} else {
				readLUT( url.openStream() );
			}
			name = resources[id];
		} catch(IOException ex) {
			red = new float[] {0f, 1f};
			green = new float[] {0f, 1f};
			blue = new float[] {0f, 1f};
			ht = new float[] {0f, 1f};
			range = new float[] {ht[0], ht[ht.length-1]};
			hScale = 1f;
			ve = 1.;
			name = "unknown";
		}
	}

	void initSaveDialog() {
		savePanel = new JPanel(new GridLayout(0,1,2,2));
		JLabel label = new JLabel("Enter a Palette Name:");
		savePanel.add(label);
		nameF = new JTextField("MyPalette");
		savePanel.add(nameF);
		listCB = new JCheckBox("Add to \"Palettes\" Menu", true);
		fileCB = new JCheckBox("Save to File", true);
		savePanel.add( listCB );
		savePanel.add( fileCB );
	}

	/*
	 * Create a hashtable to convert color names to RGB values.
	 * Read in values from a CSV file derived from 
	 * https://cloford.com/resources/colours/500col.htm
	 */
	public static Hashtable<String, List<Float>> getColor2RGB() {	
		Hashtable<String, List<Float>> color2RGB = new Hashtable<String, List<Float>>();			
		//read in named_colors.csv
		ClassLoader cl = Palette.class.getClassLoader();
		java.net.URL url = cl.getResource(NAMED_COLORS_FILE);
		BufferedReader in;
		try {
			in = new BufferedReader(
					new InputStreamReader(url.openStream()));

			String s;
			String[] e;
			String colorName;
			Float r, g, b;
			while( (s=in.readLine())!=null ) {
				//read in color names and R, G and B values
				e = s.split(",");
				colorName = e[0].replaceAll("\\s+", ""); //remove any spaces
				r = Float.parseFloat(e[2]);
				g = Float.parseFloat(e[3]);
				b = Float.parseFloat(e[4]);
				//add to the has table
				color2RGB.put(colorName, Arrays.asList(r, g, b));
			}
			return color2RGB;
		} catch (IOException e1) {
			System.out.println(NAMED_COLORS_FILE + "not found");
			return null;
		}	
	}
	
	public Palette savePalette(Component comp) throws IOException {
		if( savePanel==null )initSaveDialog();
		boolean rootTF = root!=null;
		fileCB.setEnabled( rootTF );
		int ok = JOptionPane.showConfirmDialog(comp, savePanel,
			"Save Palette", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) return null;
		Palette pal = (Palette)clone();
		pal.name = nameF.getText();
		pal.setVE( ve );
		//only save if the Save to File checkbox is selected
		if (fileCB.isSelected()) {
			File dir = new File( root, "lut");
			if( !dir.exists() ) {
				if(!dir.mkdir()) throw new IOException(
						"unable to create directory");
			}
			File file = new File( dir, nameF.getText()+".lut");
			PrintStream out = new PrintStream(
				new FileOutputStream(file));
			
			out.println( ve );
			for(int k=0 ; k<ht.length ; k++) {
				float h = range[0]+(ht[k]-ht[0])/hScale;
				pal.ht[k] = h;
				int r = (int)Math.rint(255.*red[k]);
				int g = (int)Math.rint(255.*green[k]);
				int b = (int)Math.rint(255.*blue[k]);
				out.println( pal.ht[k] +"\t"+ r +"\t"+ g +"\t"+ b);
			}

			out.close();
			JOptionPane.showMessageDialog( comp,
				"The Custom Palette \""+pal.name+ " is created in "+ dir.getPath(),
				"",
				JOptionPane.PLAIN_MESSAGE);
		}
		//if the Add to Palettes Menu checkbox is not selected, then set
		//palette to null and it won't be added
		if (!listCB.isSelected()) pal = null;
	
		return  pal;
	}

	public Palette deletePalette(Component comp) throws IOException {
		JPanel deletePanel = new JPanel(new GridLayout(0,1,2,2));
		JLabel label = new JLabel("Select a Custom Palette to Delete:");
		deletePanel.add(label);
		customPaletteList = new JComboBox();
		customPaletteList.addItem("- Select -");
		deletePanel.add(customPaletteList);

		if(root==null)return null;
		File dir = new File(root, "lut");
		if( !dir.exists())return null;
		File[] filesb = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(".lut");
			}
		});
		for( int k=0 ; k<filesb.length ; k++) {
			try {
				Palette p = new Palette(filesb[k]);
				customPaletteList.addItem(p);
			} catch(Exception ex) {
			}
		}

		int ok = JOptionPane.showConfirmDialog(comp, deletePanel,
			"Delete a Palette", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) {
			deletePanel = null;
			return null;
		}
		else if (ok == JOptionPane.OK_OPTION) { 
			Palette selected = (Palette) customPaletteList.getSelectedItem();
			File dir2 = new File(root, "lut");
			if( !dir.exists() ) throw new IOException("Could not Delete");

			File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file.getName().endsWith(".lut");
				}
			});
			for( int k=0 ; k<files.length ; k++) {
				try {
					Palette list = new Palette(files[k]);
					if(list.name.contains( selected.name)) {
						files[k].delete();
						customPaletteList.removeItem(selected.name);
						customPaletteList.updateUI();
						return selected;
					}
				} catch(Exception ex) {
				}
			}
		}
		return  null;
	}

	public File exportPalette(Component comp) throws IOException {
		JPanel exportPanel = new JPanel(new GridLayout(0,1,2,2));
		JLabel label = new JLabel("Select a Custom Palette to Export:");
		exportPanel.add(label);
		customPaletteList = new JComboBox();
		customPaletteList.addItem("- Select -");
		exportPanel.add(customPaletteList);

		if(root==null)return null;
		File dir = new File(root, "lut");
		if( !dir.exists())return null;
		File[] filesb = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(".lut");
			}
		});
		for( int k=0 ; k<filesb.length ; k++) {
			try {
				Palette p = new Palette(filesb[k]);
				customPaletteList.addItem(p);
			} catch(Exception ex) {
			}
		}
		int ok = JOptionPane.showConfirmDialog(comp, exportPanel,
				"Export a Palette", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) {
			exportPanel = null;
			return null;
		} else if (ok == JOptionPane.OK_OPTION) {
			Palette exSelected = (Palette) customPaletteList.getSelectedItem();
			if( !dir.exists() ) throw new IOException("Could not Export");

			File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file.getName().endsWith(".lut");
				}
			});
			for( int k=0 ; k<files.length ; k++) {
				try {
					Palette list = new Palette(files[k]);
					if(list.name.contains( exSelected.name)) {
						File exFile = files[k];
						return exFile;
					}
				} catch(Exception ex) {
				}
			}
		}
		return null;
	}

	public void setModel(float[] red, float[] green, float[] blue, float[] ht) {
		this.red = (float[])red.clone();
		this.green = (float[])green.clone();
		this.blue = (float[])blue.clone();
		this.ht = (float[])ht.clone();
		range = new float[] {ht[0], ht[ht.length-1]};
		hScale = 1f;
	}
	public void setVE(double ve) {
		this.ve = ve;
	}
	public double getVE() {
		return ve;
	}
	void readLUT( InputStream input ) throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(input));
		ve = 2.0;//Double.parseDouble(in.readLine());
		String s;
		StringTokenizer st;
		Vector lut = new Vector();
		while( (s=in.readLine())!=null ) {
			st = new StringTokenizer(s);
			if( st.countTokens()<4 ) continue;
			lut.add( new float[] {
				Float.parseFloat(st.nextToken()),
				Float.parseFloat(st.nextToken()),
				Float.parseFloat(st.nextToken()),
				Float.parseFloat(st.nextToken())
				});
		}
		in.close();
		int size = lut.size();
		red = new float[size];
		green = new float[size];
		blue = new float[size];
		ht = new float[size];
		for( int k=0 ; k<size ; k++) {
			float[] tab = (float[])lut.get(k);
			ht[k] = tab[0];
			red[k] = tab[1]/255f;
			green[k] = tab[2]/255f;
			blue[k] = tab[3]/255f;
		}
		range = new float[] {ht[0], ht[ht.length-1]};
		hScale = 1f;
	}
	/*
	 * read in CPT formatted color palettes
	 */
	void readCPT( InputStream input ) throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(input));
		ve = 2;
		String s;
		String[] elements;
		ArrayList<ArrayList<Float>> lut = new ArrayList<ArrayList<Float>>();
		ArrayList<Float> startPoint;
		ArrayList<Float> endPoint;
		
		while( (s=in.readLine())!=null ) {
			//if color_model = hsv, read in LUT using readHSV method
			if (s.toLowerCase().contains("hsv")) {
				lut = readHSV(in);
				break;
			}
			//ignore comment lines
			if (s.startsWith("#") || s.startsWith("B") || s.startsWith("F") || s.startsWith("N")) continue;
			//split line based on white space (incl tabs) or /
			elements = s.split("\\s+|\\/");
			//only read lines with at least 2 columns
			if( elements.length < 2 ) continue;
			int k = 0;
			//ignore any empty elements
			while (elements[k].equals("")) k++;
			//read in start point
			startPoint = new ArrayList<Float>();
			//check if color is given as a word, rather than RGB
			startPoint.add(Float.parseFloat(elements[k++]));
			try {
				startPoint.add(Float.parseFloat(elements[k++]));
				startPoint.add(Float.parseFloat(elements[k++]));
				startPoint.add(Float.parseFloat(elements[k++]));
			} catch(Exception e) {
				k--;
				String textColor = elements[k++];
				List<Float> RGB  = color2RGB.get(textColor);
				startPoint.addAll(RGB);
			}
			//only add start point if not already in table 
			if (!lut.contains(startPoint)) lut.add(startPoint);
			
			//if there is just one color value per line, then continue to the next line
			if (k >= elements.length) continue;
			
			//read in end point
			endPoint = new ArrayList<Float>();
			//check if color is given as a word, rather than RGB
			endPoint.add(Float.parseFloat(elements[k++]));
			try {
				endPoint.add(Float.parseFloat(elements[k++]));
				endPoint.add(Float.parseFloat(elements[k++]));
				endPoint.add(Float.parseFloat(elements[k++]));
			} catch(Exception e) {
				k--;
				String textColor = elements[k++];
				List<Float> RGB  = color2RGB.get(textColor);
				endPoint.addAll(RGB);
			}
			//only add end point if not already in table 
			if (!lut.contains(endPoint)) lut.add(endPoint);
		}
		in.close();

		int size = lut.size();
		red = new float[size];
		green = new float[size];
		blue = new float[size];
		ht = new float[size];
		for (int i=0; i<size; i++) {
			ArrayList<Float> tab = lut.get(i);
			ht[i] = tab.get(0);
			red[i] = tab.get(1)/255f;
			green[i] = tab.get(2)/255f;
			blue[i] = tab.get(3)/255f;
		}		
		range = new float[] {ht[0], ht[ht.length-1]};
		hScale = 1f;
	}
	
	/*
	 * Read in CPT files with color_model = hsv
	 */
	public ArrayList<ArrayList<Float>> readHSV( BufferedReader in ) throws IOException {
		String l;
		String[] elements, hsv;
		ArrayList<ArrayList<Float>> lut = new ArrayList<ArrayList<Float>>();
		ArrayList<Float> startPoint;
		ArrayList<Float> endPoint;

		while( (l=in.readLine())!=null ) {

			//ignore comment lines
			if (l.startsWith("#") || l.startsWith("B") || l.startsWith("F") || l.startsWith("N")) continue;
			//split line based on white space (incl tabs)
			elements = l.split("\\s+");
			//only read lines with at least 2 columns 
			if( elements.length < 2 ) continue;
			int k = 0;
			//read in start point
			startPoint = new ArrayList<Float>();
			startPoint.add(Float.parseFloat(elements[k++]));			
			//check if color is given as a word, rather than RGB
			try {
				//split HSV in based on hyphens
				hsv = elements[k++].split("-");
				//convert HSV to RGB
				startPoint.addAll(HSV2RGB(hsv));
			} catch(Exception e) {
				k--;
				String textColor = elements[k++];
				List<Float> RGB  = color2RGB.get(textColor);
				startPoint.addAll(RGB);
			}
			//only add start point if not already in table 
			if (!lut.contains(startPoint)) lut.add(startPoint);
			
			//if there is just one color value per line, then continue to the next line
			if (k >= elements.length) continue;
			
			//read in end point
			endPoint = new ArrayList<Float>();
			endPoint.add(Float.parseFloat(elements[k++]));			
			//check if color is given as a word, rather than RGB
			try {
				//split HSV in based on hyphens
				hsv = elements[k++].split("-");
				//convert HSV to RGB
				endPoint.addAll(HSV2RGB(hsv));
			} catch(Exception e) {
				k--;
				String textColor = elements[k++];
				List<Float> RGB  = color2RGB.get(textColor);
				endPoint.addAll(RGB);
			}
			//only add end point if not already in table 
			if (!lut.contains(endPoint)) lut.add(endPoint);
		}
		in.close();
		return lut;
	}
	
	/*
	 * Convert HSV to RGB using conversion formula found here:
	 * https://en.wikipedia.org/wiki/HSL_and_HSV#From_HSV
	 */
	public List<Float> HSV2RGB(String[] hsv) {
		List<Float> rgb = new ArrayList<Float>();
		float c, x, m, h_pr;
		float r1, g1, b1;
		float r, g, b;
		//extract H, S and V from input array
		float h = Float.parseFloat(hsv[0]);
		float s = Float.parseFloat(hsv[1]);
		float v = Float.parseFloat(hsv[2]);
		
		//apply conversion from hsv to rgb
		c = v * s;
		h_pr = h/60f;
		x = c * (1 - Math.abs(h_pr % 2 - 1));
		
		if (h_pr >= 0 && h_pr < 1) {
			r1 = c;
			g1 = x;
			b1 = 0;			
		}
		else if (h_pr >= 1 && h_pr < 2) {
			r1 = x;
			g1 = c;
			b1 = 0;
		}
		else if (h_pr >= 2 && h_pr < 3) {
			r1 = 0;
			g1 = c;
			b1 = x;
		}
		else if (h_pr >= 3 && h_pr < 4) {
			r1 = 0;
			g1 = x;
			b1 = c;
		}
		else if (h_pr >= 4 && h_pr < 5) {
			r1 = x;
			g1 = 0;
			b1 = c;
		}
		else if (h_pr >= 5 && h_pr < 6) {
			r1 = c;
			g1 = 0;
			b1 = x;
		}
		else {
			r1 = 0;
			g1 = 0;
			b1 = 0;
		}

		m = v - c;
		r = (float) Math.ceil((r1 + m) * 255);
		g = (float) Math.ceil((g1 + m) * 255);
		b = (float) Math.ceil((b1 + m) * 255);
	
		rgb = Arrays.asList(r, g, b);
		
		return rgb;
	}
	
	public float getDiscrete() {
		return interval; 
	}
	public void setDiscrete(double interval) {
		this.interval = (float)interval;
	}
	public float getScaledZ( int index ) {
		if( index<0 || index>=ht.length)return Float.NaN;
		return range[0] + (ht[index]-ht[0]) / hScale;
	}
	public void setScaledZ( int index, float z ) {
		if( index<0 || index>=ht.length)return;
		ht[index] = ht[0] + (z-range[0])*hScale;
	}
	float[] getColor(float h) {
		float z = ht[0] + (h-range[0]) * hScale;
		int nht = ht.length;
		if(z<=ht[0]) return new float[] {red[0], green[0], blue[0] };
		if(z>=ht[nht-1]) return new float[] {red[nht-1], green[nht-1], blue[nht-1] };
		int i=(nht-1)/2;
		int k1 = 0;
		int k2 = nht-1;
		while(true) {
			if(z>ht[i+1]) {
				k1=i;
				i=(i+k2)/2;
			} else if(z<ht[i]) {
				k2 = i;
				i=(i+k1)/2;
			} else break;
		}
		float dx = (z-ht[i]) / (ht[i+1]-ht[i]);
		float[] rgb = new float[] {red[i]+dx*(red[i+1]-red[i]),
					green[i] + dx * (green[i+1]-green[i]),
					blue[i] + dx * (blue[i+1]-blue[i]) };
		return rgb;
	}
	public float[][] getModel() {
		return new float[][] {
			(float[])ht.clone(),
			(float[])red.clone(),
			(float[])green.clone(),
			(float[])blue.clone()
		};
	}
	public float[] getControlPoints() {
		return (float[])ht.clone();
	}
	public int getRGB(float z) {
		if( interval>0f ) z = interval * (float)Math.floor(z/interval);
		float[] col = getColor(z);
		int rgb = (new java.awt.Color( col[0], col[1], col[2] )).getRGB();
		return rgb;
	}

	public int getRGB(float z, float shade ) {
		if(shade<.1f)shade=.1f;
		if( interval>0f ) z = interval * (float)Math.floor(z/interval);
		float[] rgb = getColor(z);

		if ( sunIllum ) {
			float s1 = (shade>.5f)? (shade-.5f)*(shade-.5f)/(.5f) : 0f;
			for(int i=0 ; i<3 ; i++) rgb[i] = shade*rgb[i]+s1*(1f-rgb[i]);
		}

		int c = 0xff000000 | rgbmap[(int)(rgb[0]*1000f)]<<16
				| rgbmap[(int)(rgb[1]*1000f)]<<8
				| rgbmap[(int)(rgb[2]*1000f)];
		return c;
	}
	public int getRGB(float z, float shade, double sunZ ) {
		if(shade<.1f)shade=.1f;
		if( interval>0f ) z = interval * (float)Math.floor(z/interval);
		float[] rgb = getColor(z);

		if ( sunIllum ) {
			if( shade>.4f) {
				float factor = (float)(sunZ);
				if( factor<.6f )factor = .6f;
				float s1 = (shade-.4f)*(shade-.4f)/(.6f*factor);
				for(int i=0 ; i<3 ; i++) rgb[i] = shade*rgb[i]+s1*(1f-rgb[i]);
			} else {
				for(int i=0 ; i<3 ; i++) rgb[i] = shade*rgb[i];
			}
		}
		int c = 0xff000000 | rgbmap[(int)(rgb[0]*1000f)]<<16
				| rgbmap[(int)(rgb[1]*1000f)]<<8
				| rgbmap[(int)(rgb[2]*1000f)];
		return c;
	}
	public ImageIcon getIcon() {
		int w = 80;
		int h = 22;
		BufferedImage image = new BufferedImage( w, h,
				BufferedImage.TYPE_INT_RGB);
		float scale = hScale;
		float[] rng = getRange();
		setRange( 0f, (w-1)*1f);
		for( int x=0 ; x<w ; x++) {
			int rgb = getRGB((float)x);
			for(int y=0 ; y<h ; y++) image.setRGB(x, y, rgb);
		}
		setRange( rng[0], rng[1]);
		return new ImageIcon( image);
	}
	public void setRGB(int index, float[] rgb) {
		if( index<0 || index>=ht.length)return;
		red[index] = rgb[0];
		green[index] = rgb[1];
		blue[index] = rgb[2];
	}
	public void resetRange() {
		range = new float[] {ht[0], ht[ht.length-1]};
		hScale = 1f;
	}
	public void setRange( float min, float max ) {
		range[0] = min;
		range[1] = max;
		hScale = (ht[ht.length-1] - ht[0]) / (range[1]-range[0] );
	}
	public float[] getRange() {
		return new float[] {range[0], range[1]};
	}
	public void setGamma(double g) {
		gamma = g;
		rgbmap[0] = 0;
		double scale = (double)(maxRGB-minRGB);
		for(int i=1 ; i<=1000 ; i++) {
			rgbmap[i] = minRGB + (int)Math.floor(scale*
				Math.pow( .001*(double)i, 1/gamma));
			if(rgbmap[i]>255) rgbmap[i]=255;
			if(rgbmap[i]<minRGB) rgbmap[i]=minRGB;
		}
	}
	public double getGamma() {
		return gamma;
	}
	public void saturate( float factor ) {
		if( factor<0f )return;
		if(factor>1f ) factor=1f;
		for( int i=0 ; i<red.length ; i++) {
			float max = red[i];
			if( green[i]>max) max=green[i];
			if( blue[i]>max) max=blue[i];
			float min = red[i];
			if( green[i]<min) min=green[i];
			if( blue[i]<min) min=blue[i];
			if( max==min) continue;
			float newMin =(1-factor)*min;
			red[i] = max - (max-red[i])*(max-newMin)/(max-min);
			green[i] = max - (max-green[i])*(max-newMin)/(max-min);
			blue[i] = max - (max-blue[i])*(max-newMin)/(max-min);
		}
	}
	public Object clone() {
		Palette pal = new Palette(red, green, blue, ht);
		pal.setRange( range[0], range[1] );
		pal.setVE(ve);
		pal.setName(name);
		return pal;
	}
	
	public void flipRGB() {
		reverseArray(red);
		reverseArray(green);
		reverseArray(blue);
	}
	void reverseArray(float[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			  float temp = array[i];
			  array[i] = array[array.length - 1 - i];
			  array[array.length - 1 - i] = temp;
			}
	}
	
	public float[] getRed() {
		return red;
	}
	
	public float[] getGreen() {
		return green;
	}
	
	public float[] getBlue() {
		return blue;
	}
	
	public float[] getHt() {
		return ht;
	}
	
	public String getName() {
		return name;
	}
}
