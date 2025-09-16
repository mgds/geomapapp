package haxby.db.ship;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

public class RawShipData {
	private String info;
	private String control;
	private ArrayList<String[]> controlPoints;
	private String name;
	private int start;
	private int end;
	private String url;
	private String keywords;
	private ArrayList<String> rawSeg;
	private ArrayList<ArrayList<String[]>> processedSegments;
	
	public RawShipData()
	{
		processedSegments = new ArrayList<ArrayList<String[]>>();
		rawSeg = new ArrayList<String>();
		controlPoints = new ArrayList<String[]>();
		info = "";
		control = "";	
		url = "";
		keywords = "";
	}
	
	
	
	public void setKeywords(String s)
	{
		keywords = s;
	}
	
	public void setInfo(String s)
	{
		info = s;		
	}
	
	public void setURL(String s)
	{
		url = s;
	}
	
	public void setControl(String s)
	{
		control = s;
	}
	
	public void addSegment(String seg){
		rawSeg.add(seg);
	}
	
	public void parseStart(){
		int index = info.indexOf("Start Date:"); 
		String startString = info.substring(index+12, index+22);
		StringTokenizer strTok = new StringTokenizer(startString, "-");
		int year = Integer.parseInt(strTok.nextToken());
		int month = Integer.parseInt(strTok.nextToken())-1;
		int day = Integer.parseInt(strTok.nextToken());
		
		Calendar cal = new GregorianCalendar(year,month,day);
		start = (int)(cal.getTimeInMillis()/1000);
	}
	
	public void parseEnd(){
		int index = info.indexOf("End Date:"); 
		String endString = info.substring(index+10, index+20);	
		StringTokenizer strTok = new StringTokenizer(endString, "-");
		int year = Integer.parseInt(strTok.nextToken());
		int month = Integer.parseInt(strTok.nextToken())-1;
		int day = Integer.parseInt(strTok.nextToken());
		
		Calendar cal = new GregorianCalendar(year,month,day);
		end = (int)(cal.getTimeInMillis()/1000);	
	}
	
	
	public void createPoints()
	{
		StringTokenizer strTk = new StringTokenizer(control, ",");
		
		while(strTk.hasMoreTokens()){
			controlPoints.add(new String[]{strTk.nextToken(), strTk.nextToken()});
			
		}
		
	}
	
	public void createSegments()
	{
		for(String segStr: rawSeg){
			StringTokenizer strTk = new StringTokenizer(segStr, ",");
			ArrayList<String[]> temp = new ArrayList<String[]>();			
			while(strTk.hasMoreTokens()){				
				temp.add(strTk.nextToken().split(" "));
			}
			processedSegments.add(temp);
		}
	}
	
	public int getStart()
	{
		return start;
	}
	
	public int getEnd()
	{
		return end;
	}
	
	public ArrayList<String[]> getPoints()
	{
		return controlPoints;
	}
	
	public String getInfo(){
		return info;
	}
	
	
	public String getControl(){
		return control;
	}
	
	public void parseName()
	{
		StringTokenizer strTok = new StringTokenizer(info);
		strTok.nextToken();
		name = strTok.nextToken();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getURL(){
		return url;
	}
	
	public String getKeywords(){
		return keywords;
	}
	
	public ArrayList<ArrayList<String[]>> getSegments(){
		return processedSegments;
	}
	
}