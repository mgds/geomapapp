package org.geomapapp.db.dsdp;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Taxon {
//	public final static String BASE = ODP.BASE+"fauna/";
	public static void main(String[] args) {
		byte[] group = new byte[60000];
		try {
			String dir = "/home/bill/projects/DSDP2000/janus/fauna";
			FossilGroups gps = FossilGroups.getFossilGroups();
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
				new GZIPInputStream(
				new FileInputStream(new File(dir,"taxon_all.tsf.gz")))));
			StringTokenizer st;
			String s = in.readLine();
			Vector[] taxa = new Vector[10];
			for( int k=0 ; k<10 ; k++ ) taxa[k] = new Vector();
			Vector[] genera = new Vector[10];
			for( int k=0 ; k<10 ; k++ ) genera[k] = new Vector();
			Vector species = new Vector();
			
			Integer lastID = new Integer(-1);
			String lastGenus = "";
			while( (s=in.readLine()) != null ) {
				st = new StringTokenizer(s, "\t");
				if( st.countTokens()<4 )continue;
				int gp = Integer.parseInt( st.nextToken().trim() );
				Integer id = new Integer(st.nextToken().trim());
				if( id.equals(lastID) )continue;
				lastID = id;
				group[id.intValue()] = (byte)gp;
				String genus = st.nextToken().trim().toUpperCase();
				String name = st.nextToken().trim().toUpperCase();
				if( !species.contains(name) ) species.add(name);
				if( !genus.equals(lastGenus) )genera[gp-1].add(genus);
				lastGenus = genus;
				taxa[gp-1].add( new Object[] {id, genus, name, new Boolean(false)} );
			}
			in.close();
			System.out.println( species.size() +" species names");
			int total=0;
			int nGenera=0;
			for( int k=0 ; k<10 ; k++ ) {
				System.out.println( taxa[k].size() +" entries,\t"+ genera[k].size() +" genera\t"+ gps.getGroupName(k+1) );
				total += taxa[k].size();
				nGenera += genera[k].size();
			}
			System.out.println( total +" entries total");
			System.out.println( "\t"+species.size() +" species names");
			System.out.println( "\t"+nGenera +" genera names");

			dir = "/home/bill/projects/DSDP2000/database/fauna";

			in = new BufferedReader(
				new InputStreamReader(
				new GZIPInputStream(
				new FileInputStream(new File(dir,"glossary.gz")))));
			Vector glossary = new Vector();
			while( (s=in.readLine())!=null ) glossary.add( s.trim() );
			String[] gloss = new String[glossary.size()];
			for( int i=0 ; i<gloss.length ; i++) {
				gloss[i]=glossary.get(i).toString();
				int k = gloss[i].indexOf("(Q)");
				if( k>=0 ) {
					gloss[i]=gloss[i].substring(0, k+1) +"?"+ gloss[i].substring(k+2);
System.out.println( gloss[i] );
				}
			}
			in.close();

			File[] codes = (new File(dir, "test")).listFiles( new FileFilter() {
				public boolean accept(File f) {
					if( f.isDirectory() )return false;
					return f.getName().endsWith("code.gz");
				}
			});

			for( int i=0 ; i<codes.length ; i++) {
				in = new BufferedReader(
					new InputStreamReader(
					new GZIPInputStream(
					new FileInputStream(codes[i]))));
				s = in.readLine();
				s = codes[i].getName();
				Vector unknownGenera = new Vector();
				Vector knownGenera = new Vector();
System.out.println( s.substring(0, s.indexOf(".code.gz")) );
				int found = 0;
				int notFound = 0;
				while( (s=in.readLine())!=null ) {
					st = new StringTokenizer(s,",");
					String genus = gloss[Integer.parseInt(st.nextToken())];
					int gp = -1;
					for( int k=0 ; k<10 ; k++) {
						if( genera[k].contains(genus) ) {
							gp = k+1;
							break;
						}
					}
					if( gp<0 ) {
						notFound++;
						if( !unknownGenera.contains(genus) ) {
							unknownGenera.add(genus);
						//	System.out.println("\t"+genus);
						}
					} else {
						if( !knownGenera.contains(genus) ) {
							knownGenera.add(genus);
						//	System.out.println("\t"+gp+"\t"+genus);
						}
						if( !st.hasMoreTokens() ) {
							notFound++;
							continue;
						}
						String sp = gloss[Integer.parseInt(st.nextToken())];
						while( st.hasMoreTokens() )sp += " "+gloss[Integer.parseInt(st.nextToken())];
						int idx = -1;
						for( int k=0 ; k<taxa[gp-1].size() ; k++) {
							Object[] t = (Object[])taxa[gp-1].get(k);
							if( !genus.equals(t[1]) )continue;
							if( sp.equals(t[2]) ) {
								idx = ((Integer)t[0]).intValue();
								break;
							}
						}
						if( idx>=0 ) found++;
						else notFound++;

					//	System.out.println("**\t"+genus+" "+sp+"\t"+idx);
					}
				}
				System.out.println( "\t"+found+" found\t"+notFound+" not found");
				in.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}
}
