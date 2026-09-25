package org.geomapapp.credit;

import haxby.map.MapApp;
import haxby.util.PathUtil;

import java.net.*;
import java.io.*;

import javax.swing.*;

public class Flag {
	private static final int EXIT_ON_CLOSE = 0;
	static ClassLoader loader = null;
	static boolean loaded = true;
//	public static int WORLD = 0;
/*	public static int US = 0;
	public static int DE = 1;
	public static int ITALY = 2;
	public static int UK = 3;
	public static int FRANCE = 4;
	public static int JAPAN = 5;
	public static int CANADA = 6;
	public static int CHINA = 7;
	public static int AUSTRALIA = 8;
	public static int RUSSIA = 9;
	public static int SPAIN = 10;
	public static int GREECE = 11;
	public static int NORWAY = 12;
	public static int TURKEY = 13;
	public static int IRELAND = 14;
	public static int ISRAEL = 15;
*/	
	
	static ImageIcon missing, missingS;
	
	static ImageIcon[] flags;
	static ImageIcon[] flags_small;
	public static String FLAG_PROPERTIES = PathUtil.getPath("CREDIT_FLAG_PROPERTIES",
			MapApp.BASE_URL+"/gma_credit/flags/displayFlags.properties");
	static String[] names = {
		"United_States",		// 0
		"Germany",
		"Italy",
		"United_Kingdom",
		"France",
		"Japan", 				//05
		"Canada",
		"China",
		"Australia",
		"Russian_Federation",
		"Spain", 				//10
		"Greece",
		"Norway",
		"Turkey",
		"Ireland",
		"Israel", 				//15
		"European_Union",
		"Afghanistan",
		"African_Union",
		"Aland",
		"Albania",				//20
		"Algeria",
		"American_Samoa",
		"Andorra",
		"Angola",
		"Anguilla",				//25
		"Antarctica",
		"Antigua_&_Barbuda",
		"Arab_League",
		"Argentina",
		"Armenia",				//30
		"Aruba",
		"ASEAN",
		"Austria",
		"Azerbaijan",
		"Bahamas",				//35
		"Bahrain",
		"Bangladesh",
		"Barbados",
		"Basque_Country",
		"Belarus",				//40
		"Belgium",
		"Belize",
		"Benin",
		"Bermuda",
		"Bhutan",				//45
		"Bolivia",
		"Bosnia_&_Herzegovina",
		"Botswana",
		"Bouvet",
		"Brazil",				//50
		"British_Indian_Ocean_Territory",
		"Brunei",
		"Bulgaria",
		"Burkina_Faso",
		"Burundi",				//55
		"Cambodja",
		"Cameroon",
		"Cape_Verde",
		"CARICOM",
		"Catalonia",			//60
		"Cayman_Islands",
		"Central_African_Republic",
		"Chad",
		"Chile",
		"Christmas",			//65
		"CIS",
		"Cocos",
		"Colombia",
		"Commonwealth",
		"Comoros",				//70
		"Congo_Brazzaville",
		"Congo_Kinshasa",
		"Cook-Islands",
		"Costa_Rica",
		"Cote_d'Ivoire",		//75
		"Croatia",
		"Cuba",
		"Cyprus",
		"Czech_Republic",
		"Djibouti",				//80
		"Dominica",
		"Dominican-Republic",
		"Ecuador",
		"Egypt",
		"El_Salvador",			//85
		"England",
		"Equatorial-Guinea",
		"Eritrea",
		"Estonia",
		"Ethiopia",				//90
		"Falkland",
		"FAO",
		"Faroes",
		"Fiji",
		"Finland",				//95
		"French_Guiana",
		"French_Southern_Territories",
		"Gabon",
		"Galicia",
		"Gambia",				//100
		"Georgia",
		"Denmark",
		"Ghana",
		"Gibraltar",
		"Greenland",			//105
		"Grenada",
		"Guadeloupe",
		"Guam",
		"Guatemala",
		"Guernsey",				//110
		"Guinea_Bissau",
		"Guinea",
		"Guyana",
		"Haiti",
		"Heard_Island_and_McDonald", //115
		"Honduras",
		"Hong_Kong",
		"Hungary",
		"IAEA",
		"Iceland",				//120
		"IHO",
		"India",
		"Indonesia",
		"Iran",
		"Iraq",					//125
		"Islamic_Conference",
		"Isle_of_Man",
		"Jamaica",
		"Jersey",
		"Jordan",				//130
		"Kazakhstan",
		"Kenya",
		"Kiribati",
		"Kosovo",
		"Kuwait",				//135
		"Kyrgyzstan",
		"Laos",
		"Latvia",
		"Lebanon",
		"Lesotho",				//140
		"Liberia",
		"Libya",
		"Liechtenstein",
		"LIthuania",
		"Luxembourg",			//145
		"Macao",
		"Macedonia",
		"Madagascar",
		"Malawi",
		"Malaysia",				//150
		"Maldives",
		"Mali",
		"Malta",
		"Marshall_Islands",
		"Martinique",			//155
		"Mauritania",
		"Mauritius",
		"Mayotte",
		"Mexico",
		"Micronesia",			//160
		"Moldova",
		"Monaco",
		"Mongolia",
		"Montenegro",
		"Montserrat",			//165
		"Morocco",
		"Mozambique",
		"Myanmar",
		"Namibia",
		"NATO",					//170
		"Nauru",
		"Nepal",
		"Netherlands_Antilles",
		"Netherlands",
		"New_Caledonia",		//175
		"New_Zealand",
		"Nicaragua",
		"Niger",
		"Nigeria",
		"Niue",					//180
		"Norfolk",
		"North_Korea",
		"Northern_Cyprus",
		"Northern_Ireland",
		"Northern_Mariana",		//185
		"OAS",
		"OECD",
		"Olimpic_Movement",
		"Oman",
		"OPEC",					//190
		"Pakistan",
		"Palau",
		"Palestine",
		"Panama",
		"Papua_New_Guinea",		//195
		"Paraguay",
		"Peru",
		"Philippines",
		"Pitcairn",
		"Poland",				//200
		"Portugal",
		"Puerto_Rico",
		"Qatar",
		"Red-Cross",
		"Reunion",				//205
		"Romania",
		"Rwanda",
		"Saint_Barthelemy",
		"Saint_Helena",
		"Saint_Lucia",			//210
		"Saint_Martin",
		"Saint_Pierre_and_Miquelon",
		"Samoa",
		"San-Marino",
		"Sao_Tome_&_Principe",	//215
		"Saudi_Arabia",
		"Scotland",
		"Senegal",
		"Serbia",
		"Seychelles",			//220
		"Sierra_Leone",
		"Singapore",
		"Slovakia",
		"Slovenia",
		"Solomon_Islands",		//225
		"Somalia",
		"Somaliland",
		"South_Africa",
		"South_Georgia_and_South_Sandwich",
		"South_Korea",			//230
		"Sri_Lanka",
		"St_Kitts_&_Nevis",
		"St_Vincent_&_the_Grenadines",
		"Sudan",
		"Suriname",				//235
		"Svalbard_and_Jan_Mayen",
		"Swaziland",
		"Sweden",
		"Switzerland",
		"Syria",				//240
		"Tahiti",
		"Taiwan",
		"Tajikistan",
		"Tanzania",
		"Thailand",				//245
		"Timor_Leste",
		"Togo",
		"Tokelau",
		"Tonga",
		"Trinidad_&_Tobago",	//250
		"Tunisia",
		"Turkmenistan",
		"Turks_and_Caicos_Islands",
		"Tuvalu",
		"Uganda",				//255
		"Ukraine",
		"UNESCO",
		"UNICEF",
		"United_Arab_Emirates",
		"United_Nations",		//260
		"Uruguay",
		"Uzbekistan",
		"Vanutau",
		"Vatican_City",
		"Venezuela",			//265
		"Viet_Nam",
		"Virgin_Islands_British",
		"Virgin_Islands_US",
		"Wales",
		"Wallis_and_Futuna",	//270
		"Western-Sahara",
		"WHO",
		"WTO",
		"Yemen",
		"Zambia",
		"Zimbabwe",
		"Zblank"				// 276
	};
	/**
	 * Loads the info icon at the resource path if available.
	 * 
	 * @return Image icon or null
	 * @throws IOException If an input or output exception occurred
	 */
	public static ImageIcon getInfoIcon( ) throws IOException {
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		if( loader==null )init();
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		String path = "org/geomapapp/resources/icons/info_s.gif";
		URL url = loader.getResource( path );
		return new ImageIcon(url);
	}
	/**
	 * Loads the small flag image at the resource path if available.
	 * Checks flag name assignment to retrieve correct flag image size.
	 *  
	 * @param flag
	 * @return Image small flag or null
	 * @throws IOException If an input or output exception occurred
	 */
	public static ImageIcon getSmallFlag( int flag ) throws IOException {
		URL url;
		if( flag<0 || flag>=names.length ) return getMissingS();
		if( flags_small==null ) flags_small= new ImageIcon[names.length];
		if( flags_small[flag]!=null ) return flags_small[flag];
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		if( loader==null )init();
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		String path = "org/geomapapp/resources/flags/30x22/"+names[flag]+".gif";

		try {
			url = loader.getResource( path );
		}catch (NullPointerException ne){
			url = loader.getResource("org/geomapapp/resources/flags/30x22/Zblank.gif");
		}
		return new ImageIcon(url);
	}
	private static ImageIcon getMissingS() throws IOException {
		if (missingS != null) return missingS; 
		
		if( loader==null )init();
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		String path = "org/geomapapp/resources/flags/30x22/Zblank.gif";

		URL url = loader.getResource( path );
		missingS = new ImageIcon(url);
		return missingS;
	}
	private static ImageIcon getMissing() throws IOException {
		if (missing != null) return missing; 
		if( loader==null )init();
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		String path = "org/geomapapp/resources/flags/Q.gif";

		URL url = loader.getResource( path );
		missing = new ImageIcon(url);
		return missing;
	}
	/**
	 * Loads the flag image at the resource path if available.
	 * 
	 * @param flag
	 * @return Image flag or null
	 * @throws IOException If an input or output exception occurred
	 */
	public static ImageIcon getFlag( int flag ) throws IOException {
		if( flag<0 || flag>=names.length ) return getMissing();

		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		if( loader==null )init();
		if( !loaded ) throw new IOException("couldn\'t initiallize class loader");
		String path = "org/geomapapp/resources/flags/"+names[flag]+".gif";
		URL url = loader.getResource( path );
		return new ImageIcon(url);
	}
	static void init() {
		if( loader!=null ) return;
		try {
			loader = org.geomapapp.util.Icons.class.getClassLoader();
			loaded = true;
		} catch(Exception ex) {
			loaded = false;
		}
	}
	public static void main(String[] args) {
		JFrame f = new JFrame("flags");
		f.setDefaultCloseOperation(EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new java.awt.GridLayout(0,1,5,5));
		try {
			panel.add( new JLabel( "info.gif", getInfoIcon(), JLabel.CENTER ));
		} catch(Exception e) {
			e.printStackTrace();
		}
		for( int k=0 ; k<names.length ; k++) {
			try {
				panel.add( new JLabel( "("+k+") "+ names[k], 
					getFlag(k),
					JLabel.CENTER ));
				panel.add( new JLabel( "("+k+") "+ names[k], 
					getSmallFlag(k),
					JLabel.CENTER ));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		f.getContentPane().add(panel);
		f.pack();
		f.setVisible(true);
	}
}
