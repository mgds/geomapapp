package org.geomapapp.db.dsdp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.*;

public class FossilAssembly extends JComponent {
	DSDPHole hole;
	double zScale;
	JTextField text;
	MouseAdapter mouse;
	static Comparator compare;
	FossilGroup group;
	public FossilEntry[] entries;				// 1 entry per depth
	Vector refs;
	short[] codes;
	public FossilAssembly(DSDPHole hole,
			FossilGroup group, 
			FossilEntry[] entries,
			Vector refs) {
		this.hole = hole;
		this.group = group;
		this.entries = entries;
		this.refs = refs;
		zScale = 2.;
	}
	public void setTextField( JTextField text ) {
		this.text = text;
	}
	public String getGroupName() {
		return group.getGroupName();
	}
	public String getReference(int refNumber) {
		if(refs==null||refNumber>=refs.size()) return "";
		String ref = (String)refs.get(refNumber);
		StringTokenizer st = new StringTokenizer(ref, "+");
		StringBuffer sb = new StringBuffer(AuthorGlossary.get(Integer.parseInt(st.nextToken())));
		sb.append( ", v. "+st.nextToken()+", p. "+st.nextToken());
		return sb.toString();
	}
	void initCompare() {
		compare = new Comparator() {
			public int compare(Object o1, Object o2) {
				float[] f1 = (float[])o1;
				float[] f2 = (float[])o2;
				if( f1[1]<f2[1] ) return -1;
				else if( f1[1]>f2[1] ) return 1;
				else {
					if( f1[2]<f2[2] ) return -1;
					else if( f1[2]>f2[2] ) return 1;
				}
				return 0;
			}
			public boolean equals(Object o) {
				return o==this;
			}
		};
	}
	public short[] getAllCodes() {
		if( codes!=null )return codes;
		if( compare==null ) initCompare();
		Vector v = new Vector();
		Vector minmax = new Vector();
		for( int k=0 ; k<entries.length ; k++) {
			int[] codes = entries[k].getCodes();
			float depth = entries[k].depth;
			for( int i=0 ; i<codes.length ; i++) {
				Integer pick = new Integer(codes[i]);
				int j = v.indexOf(pick);
				if( j<0 ) {
					v.add(pick);
					minmax.add(new float[] {(float)v.size()-1f, depth, depth});
				} else {
					float[] range = (float[])minmax.get(j);
					if( depth>range[2] )range[2]=depth;
					else if( depth<range[1] )range[1]=depth;
				}
			}
		}
		Collections.sort( minmax, compare);
		codes = new short[v.size()];
		for( int k=0 ; k<v.size() ; k++) {
			float[] range = (float[])minmax.get(k);
			int i=(int)range[0];
			codes[k] = (short)((Integer)v.get(i)).intValue();
		}
		return codes;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil(hole.totalPen*zScale);
		int w = getAllCodes().length * 8;
		return new Dimension(w, h);
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		getAllCodes();
		for( int i=0 ; i<codes.length ; i++) {
			for( int k=0 ; k<entries.length ; k++) {
				double a = (double)entries[k].abundanceForCode((int)codes[k]);
				if( a==-2. )continue;
				a = (a+1.)/2.;
				double z = zScale * entries[k].depth;
				Line2D.Double line = new Line2D.Double(3.-a, z, 4.+a, z);
				g.draw(line);
			}
			g.translate( 7., 0.);
		}
	}
}
