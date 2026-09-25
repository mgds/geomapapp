package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JOptionPane;

public class Janus {
	public static final String BASE = "https://iodp.tamu.edu/janusweb/";
	public static final int SITE_SUMM = 0; 	// Site/Hole Summary (meters recovered)
	public static final int CORE_SUMM = 1; 	// Hole/Core Summary (cores)
	public static final int SECT_SUMM = 2; 	// Core/Section Summary (sections)
	public static final int SAMPLES = 3; 	// Corelog (samples)
	public static final int B_DENSITY = 4; 	// GRA Bulk Density (sections)
	public static final int MAG_SUSCEPT = 5; 	// Magnetic Susceptibility (sections)
	public static final int NGRA = 6; 	// Natural Gamma Radiation (sections)
	public static final int PVEL_WHOLE = 7; 	// P-Wave Vel (Whole Core) (sections)
	public static final int PVEL_SPLIT = 8; 	// P-Wave Vel (Split Core) (samples)
	public static final int M_DENSITY = 9; 	// Moisture Density (samples)
	public static final int THERMCON = 10; 	// Thermcon (samples)
	public static final int SHEAR = 11; 	// Shear Strength (samples)
	public static final int COLOR = 12; 	// Color Reflectance (sections)
	public static final int MS2F = 13; 	// Point Susceptibility - MS2F (sections)
	public static final int TEMP = 14; 	// Downhole Temp. - Adara (samples)
	public static final int SPLICER = 15; 	// Splicer (tie points)
	public static final int TENSOR = 16; 	// Tensor (cores)
	public static final int CRYOMAG = 17; 	// Cryomag (sections)
	public static final int PALEO_SAMP = 18; 	// Paleo Investigation (samples)
	public static final int RANGE = 19; 	// Range Table (taxa)
	public static final int AGE_PROF = 20; 	// Ageprofile (Datum list)
	public static final int AGE_MODEL = 21; 	// Depth-Age Model
	public static final int XRD = 22; 	// X-Ray Diffraction (samples)
	public static final int XRD_IMAGE = 23; 	// XRD Images (samples)
	public static final int XRF = 24; 	// X-Ray Fluorescence (samples)
	public static final int ICP = 25; 	// ICP (samples)
	public static final int ROCK = 26; 	// Chem: Rock Eval (samples)
	public static final int CARBS = 27; 	// Chem: Carbonates (samples)
	public static final int GAS = 28;	// Chem: Gas Elements (samples)
	public static final int WATER = 29; 	// Chem: Interstitial Water (samples)
	public static final int SMEAR = 30; 	// Smear Slides (samples)
	public static final int SED_TS = 31; 	// Sedimentary Thin Sections (samples)
	public static final int HR_TS = 32; 	// Hard Rock Thin Sections (samples)
	public static final int VCD = 33; 	// Visual Core Description (sections)
	public static final int CORE_IMAGES = 34; 	// Core Photo Images
	public static final int SECTION_IMAGES = 35; 	// Section Photo Images
	public static final int CLOSEUP = 36; 	// Closeup Info

	public static final String[][] description = new String[][] {
		{"sites", "Site/Hole Summary (meters recovered)"},
		{"cores", "Hole/Core Summary (cores)"},
		{"sections", "Core/Section Summary (sections)"},
		{"sample", "Corelog (samples)"},
		{"gra", "GRA Bulk Density (sections)"},
		{"msl", "Magnetic Susceptibility (sections)"},
		{"ngr", "Natural Gamma Radiation (sections)"},
		{"pwl", "P-Wave Vel (Whole Core) (sections)"},
		{"pws", "P-Wave Vel (Split Core) (samples)"},
		{"mad", "Moisture Density (samples)"},
		{"tcon", "Thermcon (samples)"},
		{"avspentor", "Shear Strength (samples)"},
		{"color", "Color Reflectance (sections)"},
		{"ms2f", "Point Susceptibility - MS2F (sections)"},
		{"adara", "Downhole Temp. - Adara (samples)"},
		{"splice", "Splicer (tie points)"},
		{"tens", "Tensor (cores)"},
		{"cryomag", "Cryomag (sections)"},
		{"palinv", "Paleo Investigation (samples)"},
		{"range", "Range Table (taxa)"},
		{"ageprof", "Ageprofile (Datum list)"},
		{"agemod", "Depth-Age Model"},
		{"xrd", "X-Ray Diffraction (samples)"},
		{"xrdi", "XRD Images (samples)"},
		{"xrf", "X-Ray Fluorescence (samples)"},
		{"icp", "ICP (samples)"},
		{"rock", "Chem: Rock Eval (samples)"},
		{"carb", "Chem: Carbonates (samples)"},
		{"gas", "Chem: Gas Elements (samples)"},
		{"water", "Chem: Interstitial Water (samples)"},
		{"smear", "Smear Slides (samples)"},
		{"sts", "Sedimentary Thin Sections (samples)"},
		{"tsmicro", "Hard Rock Thin Sections (samples)"},
		{"vcd", "Visual Core Description (sections)"},
		{"photo", "Core Photo Images"},
		{"photo", "Section Photo Images"},
		{"closup", "Closeup Info"}
	};

	static String[] cgi = new String[] {
		"coring_summaries/sitesumm.cgi?",
		"coring_summaries/holesumm.cgi?",
		"coring_summaries/coresumm.cgi?",
		"sample/sample.cgi?",
		"physprops/gradat.cgi?",
		"physprops/msldat.cgi?",
		"physprops/ngrdat.cgi?",
		"physprops/pwldat.cgi?",
		"physprops/pwsdat.cgi?",
		"physprops/maddat.cgi?",
		"physprops/tcondat.cgi?",
		"physprops/avspentordat.cgi?",
		"physprops/colordat.cgi?",
		"physprops/ms2fdat.cgi?",
		"physprops/adara.cgi?",
		"general/splice.cgi?",
		"paleomag/tensdat.cgi?",
		"paleomag/cryomag.cgi?",
		"paleo/palinv.cgi?",
		"paleo/range.cgi?",
		"paleo/ageprofile.cgi?",
		"paleo/agemodel.cgi?",
		"xray/xrddat.cgi?",
		"imaging/primedataimages.cgi?dataType=XRD?",
		"xray/xrfdat.cgi?",
		"xray/icpdat.cgi?",
		"chemistry/rockeval.cgi?",
		"chemistry/chemcarb.cgi?",
		"chemistry/chemgas.cgi?",
		"chemistry/chemiw.cgi?",
		"physprops/sslide.cgi?",
		"physprops/sts.cgi?",
		"imaging/tsmicro.cgi?",
		"imaging/primedataimages.cgi?dataType=VCD?",
		"imaging/photo.cgi?",
		"imaging/photo.cgi?",
		"imaging/closeup.cgi?"
	};
	String leg;
	String site;
	String hole;
	int dataID;
	static JanusDialog jdial;
	public Janus(int dataID, String leg, String site, String hole) {
		this.dataID = dataID;
		this.leg = leg;
		this.site = site;
		this.hole = hole;
	}
	public void setDataID(int dataID) {
		this.dataID = dataID;
	}
	public int getDataID() {
		return dataID;
	}
	public void setLeg(String leg ) {
		this.leg = leg;
	}
	public String getLeg() {
		return leg;
	}
	public void setSite(String site) {
		this.site = site;
	}
	public String getSite() {
		return site;
	}
	public void setHole(String hole) {
		this.hole = hole;
	}
	public String getHole() {
		return hole;
	}
	public String urlString() {
		String url = BASE + cgi[dataID] + "leg="+ leg +"&site="+site+"&hole="+hole;
		if( dataID==SPLICER ) url += "&outputformat=T";
		return url;
	}
	public Vector[] getDataTable() throws IOException {
		return parseDataTable(urlString());
	}
	public static Vector[] parseHTMLTable(String urlString) throws IOException {
		URL url = URLFactory.url(urlString);
		BufferedReader in = new BufferedReader(
			new InputStreamReader( url.openStream() ));
		String s = in.readLine();
		while( !(s=in.readLine()).startsWith("<table") );
		Vector headings = parseHTMLRow(in);
		Vector rows = new Vector();
		while(true) {
			try {
				Vector row = parseHTMLRow(in);
				if( row==null )break;
				rows.add( row );
			} catch( NullPointerException e) {
				break;
			}
		}
		in.close();
		return new Vector[] {headings, rows};
	}
	public static Vector parseHTMLRow(BufferedReader in) throws IOException {
		String s;
		while( !(s=in.readLine()).startsWith("<tr") ) {
			if( s.startsWith("</table>") ) return null;
		}
		Vector row = new Vector();
		while( !(s=in.readLine()).startsWith("</tr") ) {
			if( !s.startsWith("<td") )continue;
			String[] entry = parseTD(s);
			if( entry==null )continue;
			row.add(entry);
		}
		return row;
	}
	public static String[] parseTD(String s) {
		String[] entry = new String[2];
		int k=s.indexOf("<td");
		if( k<0 )return null;
		while(true) {
			if( s.startsWith("</td>") ) {
				break;
			} else if( s.startsWith("<a href=") ) {
				entry[1] = s.substring(s.indexOf("\"")+1, s.indexOf(">")-1 );
				s = s.substring(s.indexOf(">")+1);
			} else if(s.startsWith("<")) {
				s = s.substring(s.indexOf(">")+1);
			} else {
				k = s.indexOf("<");
				entry[0] = s.substring(0, k);
				s = s.substring(k);
			}
		}
		if( entry[1]==null )return new String[] {entry[0]};
		return entry;
	}
	public static Vector[] parseDataTable(String urlString) throws IOException {
		URL url = URLFactory.url(urlString);
	//	URLConnection con = url.openConnection();
	//	con.setDoOutput(true);
	//	PrintStream out = new PrintStream(con.getOutputStream());
	//	out.print("JanusWeb_header_footer=false");
	//	out.close();
		BufferedReader in = new BufferedReader(
			new InputStreamReader( url.openStream() ));
		String s;
		while( (s=in.readLine()).indexOf("<pre>")<0 );
		Vector headings = parseRow(in.readLine());
		Vector rows = new Vector();
		while( (s=in.readLine())!=null ) {
			if( s.indexOf("</pre>")>=0) break;
			Vector row = parseRow(s);
			if( row==null )continue;
			rows.add( row );
		}
		in.close();
		return new Vector[] {headings, rows};
	}
	public static Vector parseRow(String s) {
		if( s.trim().length()==0 )return null;
		StringTokenizer st = new StringTokenizer(s,"\t",true);
		Vector row = new Vector();
		while( st.hasMoreTokens() ) {
			s = st.nextToken();
			if( s.equals("\t") ) {
				row.add(null);
			} else {
				s = s.trim();
				if( s.startsWith("<a href") ) {
					String[] entry = new String[2];
					entry[1] = s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"") );
				//	s = s.substring(s.indexOf(">")+1);
					entry[0] = s.substring(s.indexOf(">")+1, s.indexOf("</a>"));
					row.add(entry);
				} else {
					row.add(new String[] {s});
				}
				if(st.hasMoreTokens())st.nextToken();
			}
		}
		return row;
	}
	public static String showOpenDialog(java.awt.Component comp) {
		if( jdial==null )jdial = new JanusDialog();
		int ok = jdial.showDialog(comp);
		if( ok==JOptionPane.CANCEL_OPTION )return null;
		Janus j = new Janus(jdial.getDataID(), jdial.getLeg(), jdial.getSite(), jdial.getHole());
		return j.urlString();
	}
	public static void main(String[] args) {
		if( args.length!=4 ) {
			try {
				String url = Janus.BASE+"general/dbtable.cgi";
				Vector[] table = Janus.parseHTMLTable(url);
				String[][] tbl = new String[table[1].size()-1][table[0].size()];
				PrintStream out = new PrintStream(
					new FileOutputStream("janus_overview.tsf"));
				StringBuffer sb = new StringBuffer();
				Vector row = table[0];
				String[] str = (String[])row.get(0);
				if( str[0]!=null )sb.append(str[0]);
				for( int k=1 ; k<row.size() ; k++) {
					sb.append("\t");
					str = (String[])row.get(k);
					if( str[0]!=null )sb.append(str[0]);
				}
				out.println( sb.toString() );
				for( int i=0 ; i<table[1].size() ; i++) {
					sb.setLength(0);
					row = (Vector)table[1].get(i);
					str = (String[])row.get(0);
					if( str[0]!=null )sb.append(str[0]);
					for( int k=1 ; k<row.size() ; k++) {
						sb.append("\t");
						str = (String[])row.get(k);
						if( str[0]!=null )sb.append(str[0]);
					}
					out.println( sb.toString() );
				}
				out.close();
				System.exit(0);
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		//	System.out.println( "usage: java org.geomapapp.db.dsdp.Janus leg site hole dataID");
		//	System.exit(0);
		}
		try {
			Janus j = new Janus( Integer.parseInt(args[3]),
					args[0], args[1], args[2]);
			Vector[] table = j.getDataTable();
			System.out.println( table[0].size() +" columns, "+table[1].size()+" rows");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
