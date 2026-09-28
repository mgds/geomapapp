package org.geomapapp.db.dsdp;

import java.util.Vector;

public class AgeInterval {
	public static final short NULL = (short)-1;
	public short age, aux_age;
	public float top;
	public float bottom;
	public AgeInterval( short age, short aux_age, float top, float bottom ) {
		this.age = age;
		this.top = top;
		this.bottom = bottom;
		this.aux_age = aux_age;
	}
	public AgeInterval( short age, float top, float bottom ) {
		this( age, NULL, top, bottom );
	}
	public String getAgeName() {
		String name = Ages.getAgeName((int)age);
		if( aux_age!=NULL) name += "/"+Ages.getAgeName((int)aux_age);
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
