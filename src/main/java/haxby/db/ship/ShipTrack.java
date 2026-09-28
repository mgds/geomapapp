package haxby.db.ship;

import haxby.map.*;
import haxby.nav.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

public class ShipTrack {
	XMap map;
	TrackLine nav;
	HashSet<String> keywords;
	String info;
	String cruiseID;
	String ship;
	String chief_scientist;
	String initiative;
	String start;
	String end;
	String url;
	String multibeam;
	String singlebeam;
	String sidescan;
	String photograph;
	String mag_grav;
	String ctd;
	String adcp;
	String samples;
	String auv_rov_hov;
	String chemistry;
	String biology;
	String temperature;
	String seismic_reflection;
	String seismic_refraction;
	String seismicity;
	String radar;
	String visualization;
	
	
	/**
	 * Note to future programmers - all keywords and search features are converted to lowercase
	 * @author Donald Pomeroy
	 *
	 */
	
	
	public ShipTrack(TrackLine nav, HashSet<String> keywords, String info){
		this.nav = nav;
		this.keywords = keywords;
		this.info = info;
		
		cruiseID = info.substring(info.indexOf("CruiseID:")+ 9, info.indexOf("Ship:")-5);
		cruiseID = cruiseID.trim();
		if(cruiseID.length()!=0){
			keywords.add("cruiseid");
			keywords.add(cruiseID.toLowerCase());
			StringTokenizer moreTokens = new StringTokenizer(this.cruiseID, " :/");
			while(moreTokens.hasMoreTokens())
				keywords.add(moreTokens.nextToken().toLowerCase());
		}
				
		ship = info.substring(info.indexOf("Ship:")+5, info.indexOf("Chief Scientist:"));
		ship = ship.trim();
		if(ship.length()!=0) {
			
			keywords.add(ship.toLowerCase());
			StringTokenizer moreTokens = new StringTokenizer(this.ship, " :/");
			while(moreTokens.hasMoreTokens())
				keywords.add(moreTokens.nextToken().toLowerCase());
		}
				
		chief_scientist = info.substring(info.indexOf("Chief Scientist:")+16, info.indexOf("Initiative"));
		chief_scientist = chief_scientist.trim();
		if(chief_scientist.length()!=0)	{
			
			keywords.add(chief_scientist.toLowerCase());
			StringTokenizer moreTokens = new StringTokenizer(this.chief_scientist, " :/");
			while(moreTokens.hasMoreTokens())
				keywords.add(moreTokens.nextToken().toLowerCase());
		}
		
		initiative = info.substring(info.indexOf("Initiative:")+11, info.indexOf("Start Date:"));
		initiative = initiative.trim();
		if(initiative.length()!=0) {
			HashSet<String> initiativeTokens = new HashSet<String>();
			StringTokenizer st = new StringTokenizer(initiative," ,");
			while(st.hasMoreTokens())
			{
				String tempToken = st.nextToken().toLowerCase();
				keywords.add(tempToken);
				initiativeTokens.add(tempToken);
			}
			
			
			for(String kw: initiativeTokens){						
				StringTokenizer moreTokens = new StringTokenizer(kw, " :/");
				while(moreTokens.hasMoreTokens())
					keywords.add(moreTokens.nextToken().toLowerCase());
				
			}		
			
			keywords.add(initiative.toLowerCase());
			
			
		}
			
		
		start = info.substring(info.indexOf("Start Date:")+11,info.indexOf("End Date:"));
		start = start.trim();
		if(start.length()!=0){
			keywords.add(start);			
			
			//keywords.add("date");
			StringTokenizer moreTokens = new StringTokenizer(this.start, " :/-");
			while(moreTokens.hasMoreTokens()){
				keywords.add(moreTokens.nextToken().toLowerCase());
				break;
			}
		}
		
		end = info.substring(info.indexOf("End Date:")+9,info.indexOf("URL:"));
		end = end.trim();
		if(end.length()!=0){
			keywords.add(end);
			
			StringTokenizer moreTokens = new StringTokenizer(this.end, " :/-");
			while(moreTokens.hasMoreTokens()){
				keywords.add(moreTokens.nextToken().toLowerCase());
				break;
			}
		}
		
		url = info.substring(info.indexOf("URL:")+4,info.indexOf("Multibeam/Phase:"));
		url = url.trim();
		if(url.length()!=0){			
			keywords.add(url);
		}
		
		multibeam = info.substring(info.indexOf("Multibeam/Phase:")+"Multibeam/Phase:".length(), info.indexOf("SingleBeam:"));
		
		
		singlebeam = info.substring(info.indexOf("SingleBeam:")+"SingleBeam:".length(), info.indexOf("Sidescan:"));
		
		
		sidescan = info.substring(info.indexOf("Sidescan:")+"Sidescan:".length(), info.indexOf("Photograph:"));
		
		
		photograph = info.substring(info.indexOf("Photograph:")+"Photograph:".length(), info.indexOf("Mag/Grav:"));
		
		
		mag_grav = info.substring(info.indexOf("Mag/Grav:")+"Mag/Grav:".length(), info.indexOf("CTD:"));
		
		
		ctd = info.substring(info.indexOf("CTD:")+ 4, info.indexOf("ADCP:"));
		
		
		adcp = info.substring(info.indexOf("ADCP:") + "ADCP:".length(), info.indexOf("Samples:"));
		
		
		samples = info.substring(info.indexOf("Samples:")+"Samples:".length(), info.indexOf("AUV/ROV/HOV:"));
		
		
		auv_rov_hov = info.substring(info.indexOf("AUV/ROV/HOV:")+"AUV/ROV/HOV:".length(), info.indexOf("Chemistry:"));
		
		
		chemistry = info.substring(info.indexOf("Chemistry:") + "Chemistry:".length(), info.indexOf("Biology:"));
		
		
		biology = info.substring(info.indexOf("Biology:")+ "Biology:".length(), info.indexOf("Temperature:"));
		
		
		temperature = info.substring(info.indexOf("Temperature:")+"Temperature:".length(), info.indexOf("Seismic Reflection:"));
		
		
		seismic_reflection = info.substring(info.indexOf("Seismic Reflection:")+"Seismic Reflection:".length(), info.indexOf("Seismic Refraction:"));
		
		
		seismic_refraction = info.substring(info.indexOf("Seismic Refraction:")+"Seismic Refraction:".length(), info.indexOf("Seismicity:"));
		
		seismicity = info.substring(info.indexOf("Seismicity:")+"Seismicity:".length(), info.indexOf("Visualization:"));
		
		
		//radar = info.substring(info.indexOf("Radar:")+"Radar:".length(), info.indexOf("Visualization:"));
		
		visualization = info.substring(info.indexOf("Visualization:")+"Visualization:".length(), info.length());
		
	}
	
	public boolean hasKeyworkd(String word)
	{
		return keywords.contains(word);
	}
	
	public ShipTrack(TrackLine nav){
		this.nav = nav;
	}
	
	public String getName() {
		return nav.getName();
	}
	public int getStart() {
		return nav.getStart();
	}
	public int getEnd() {
		return nav.getEnd();
	}


	public int getTypes() {	
		return nav.getTypes();
	}

	public TrackLine getNav() {
		return nav;
	}

	public void draw(Graphics2D g2) {
		nav.draw(g2);
	}
	
	public boolean contains( double x, double y ) {
		return nav.contains(x, y);
	}
	
	public boolean intersects( Rectangle2D rect ) {
		return nav.intersects(rect);
	}
	
	public boolean firstNearPoint( double x, double y, Nearest n) {
		return nav.firstNearPoint( x, y, n);
	}
	
	/*Checks to see if a Ship has all of the keywords contained in the argument*/
	public boolean hasAllKeywords(ArrayList<String> keys)
	{		
		for(String key:keys)
		{
			if((keywords.contains(key.toLowerCase()))==false)
				return false;
		}
		
		return true;
	}
	
	public boolean hasOneKeyword(ArrayList<String> keys)
	{		
		for(String key:keys)
		{
			if(keywords.contains(key.toLowerCase()))
				return true;
		}
		
		return false;
	}
	
	
	public String get_field_by_col(int col)
	{
		switch(col){
			case 0:
				return this.cruiseID;
				
			case 1:
				return this.ship;
				
			case 2:
				return this.chief_scientist;
			case 3:
				return this.initiative;
			case 4:
				return this.start;
			case 5:
				return this.end;
			case 6:
				return this.url;
			case 7:
				return this.multibeam;
			case 8:
				return this.singlebeam;
			case 9:
				return this.sidescan;
			case 10:
				return this.photograph;
			case 11:
				return this.mag_grav;
			case 12:
				return this.ctd;
			case 13:
				return this.adcp;
			case 14:
				return this.samples;
			case 15:
				return this.auv_rov_hov;
			case 16:
				return this.chemistry;
			case 17:
				return this.biology;
			case 18:
				return this.temperature;
			case 19:
				return this.seismic_reflection;
			case 20:
				return this.seismic_refraction;
			case 21:
				return this.seismicity;
			case 22:
				return this.visualization;
		/*	case 23:
				return this.visualization;*/
			default:
				return null;
		}
	}
	
}



