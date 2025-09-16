package org.geomapapp.db.dsdp;

import java.io.*;
import java.util.Vector;

public class JanusMaster {
	static String BASE = "http://iodp.tamu.edu/janusweb/general/dbtable.cgi?";
	public static void main(String[] args) {
		try {
			PrintStream out = new PrintStream(
				new FileOutputStream("JanusMaster.tsf"));

			StringBuffer sb = new StringBuffer();
			sb.append("leg\tsite\thole");
			for( int k=0 ; k<Janus.description.length ; k++)sb.append("\t"+Janus.description[k][0]);
			out.println( sb );
		//	Vector[] master = Janus.parseHTMLTable(BASE);
			
			Vector[] master = Janus.parseHTMLTable(BASE+"dsdp=on");

		//	Vector[] dsdp = Janus.parseHTMLTable(BASE+"dsdp=on");
		//	for( int k=2 ; k<dsdp[0].size() ; k++) master[0].add(dsdp[0].get(k));
		//	master[1].addAll( dsdp[1] );
		//	master[0].trimToSize();
		//	master[1].trimToSize();
			Vector[] dsdp = Janus.parseHTMLTable(BASE+"dsdp=on");
			Vector legs = master[0];
			String[] last = (String[])legs.get(legs.size()-1);
			if( last[0]==null||last[0].equals("") )legs.remove(legs.size()-1);
			
			for( int i=2 ; i<legs.size() ; i++) {
				String leg = ((String[])legs.get(i))[0];
				String url = BASE+"leg="+leg;
				Vector sites = Janus.parseHTMLTable(url)[0];
				last = (String[])sites.get(sites.size()-1);
				if( last[0]==null||last[0].equals("") )sites.remove(sites.size()-1);
				for( int j=2 ; j<sites.size() ; j++) {
					String site = ((String[])sites.get(j))[0];
					Vector[] holes = Janus.parseHTMLTable(url+"&site="+site);
					last = (String[])holes[0].get(holes[0].size()-1);
					if( last[0]==null||last[0].equals("") )holes[0].remove(holes[0].size()-1);
					for( int k=2 ; k<holes[0].size() ; k++) {
						sb.setLength(0);
						String hole = ((String[])holes[0].get(k))[0];
						sb.append( leg +"\t"+ site +"\t"+ hole );
				System.out.println( sb +"\t"+ holes[0].size());
						for( int kk=0 ; kk<holes[1].size() ; kk++) {
							Vector line = (Vector)holes[1].get(kk);
							String entry = ((String[])line.get(k))[0];
							int count = Integer.parseInt( entry );
							sb.append( "\t"+ (count==0 ? "" : "t") );
						}
						out.println( sb );
					}
				}
			}
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
