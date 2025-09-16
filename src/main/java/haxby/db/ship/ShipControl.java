package haxby.db.ship;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.util.URLFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;
import java.util.HashMap;
/**
 * Generate a control file from an XML document
 * @author Donald Pomeroy
 *
 */

public class ShipControl extends DefaultHandler
{
	private StringBuffer buf;
	private ArrayList<RawShipData> dataList;


	public ShipControl()
	{
		dataList = new ArrayList<RawShipData>();
		buf = new StringBuffer();
	}

	public void parseXMLFile(String url) {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			Scanner sc = new Scanner(new File(url), "UTF-8");
			
			while(sc.hasNextLine())
			{
				sc.nextLine().trim().replaceFirst("^([\\W]+)<","<");
				break;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			//parse the file and also register this class for call backs
			sp.parse(url, this);


		}catch(SAXException se) {
			se.printStackTrace();
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (buf!=null) {
			for (int i=start; i<start+length; i++) {
				buf.append(ch[i]);
			}
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(qName.equalsIgnoreCase("info")){

			dataList.get(dataList.size()-1).setInfo(buf.toString().trim());
		}
		if (qName.equalsIgnoreCase("segment" )) {
			dataList.get(dataList.size()-1).addSegment(buf.toString().trim());
		}
		if (qName.equalsIgnoreCase("url")) {
			dataList.get(dataList.size()-1).setURL(buf.toString().trim());
		}
		if(qName.equalsIgnoreCase("keywords")) {
			dataList.add(new RawShipData());
			dataList.get(dataList.size()-1).setKeywords(buf.toString().trim());
		}
		
		
		buf = new StringBuffer();
	}

	public void generateControl(){
		try {
		File outputFile = new File("Ship_All_Cruises.control");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream( new FileOutputStream(outputFile)));

			for(RawShipData j: dataList){
				out.writeUTF(j.getName());
				out.writeInt(j.getSegments().size());
				out.writeInt(j.getStart());
				out.writeInt(j.getEnd());
				for(ArrayList<String[]> seg:j.getSegments()){
					out.writeInt(seg.size());
					for(String[] pt:seg){
						out.writeDouble(Double.parseDouble(pt[0]));
						out.writeDouble(Double.parseDouble(pt[1]));
						
					}
				}
				out.writeUTF(j.getKeywords());
				out.writeUTF(j.getInfo());
			}
			out.flush();
			out.close();

			//TODO
			URL expeditionURL = URLFactory.url(MapApp.BASE_URL+"/htdocs/data/portals/ship/");
			//DataInputStream in = new DataInputStream( new BufferedInputStream( ControlFileURL.openStream() ) );

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void parseData()
	{
		for(RawShipData i:dataList){
			i.createSegments();
			i.parseName();	
			i.parseStart();
			i.parseEnd();
		}
	}

	public void printData()
	{
		for(RawShipData i : dataList) {
			System.out.println(i.getName());
			System.out.println(i.getStart());
			System.out.println(i.getEnd());
			System.out.println(i.getInfo());
			System.out.println(i.getURL());
			System.out.println(i.getKeywords());
			//System.out.println(i.getControl());
			System.out.println(i.getSegments().size());
		}
	}

	public ArrayList<String> getNameFromKeyword(String key){
		ArrayList<String> names = new ArrayList<String>();
		for(RawShipData i : dataList) {
			if(i.getKeywords().lastIndexOf(key)!=-1)
				names.add(i.getName());
		}
		return names;
	}


}
