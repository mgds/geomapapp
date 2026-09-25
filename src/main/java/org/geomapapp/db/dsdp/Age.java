package org.geomapapp.db.dsdp;

import java.util.Vector;

public class Age {
	public static final short NULL = (short)-1;
	public short age, aux_age;
	public float top;
	public float bottom;
	public Age( short age, short aux_age, float top, float bottom ) {
		this.age = age;
		this.top = top;
		this.bottom = bottom;
		this.aux_age = aux_age;
	}
	public Age( short age, float top, float bottom ) {
		this( age, (short)-1, top, bottom );
	}
	public String getAgeName() {
		String name = Ages.getAgeName((int)age);
		if( aux_age>=0) name += "/"+Ages.getAgeName((int)aux_age);
	System.out.println( age +"\t"+ aux_age +"\t"+ name);
		return name;
	}
	public float[] getAgeRange() {
		float[] range = Ages.getAgeRange((int)age);
		if( aux_age!=NULL) {
			float[] r = Ages.getAgeRange((int)aux_age);
			if( r[0]<range[0] ) range[0]=r[0];
			if( r[1]>range[1] ) range[1]=r[1];
		}
		return range;
	}
}
