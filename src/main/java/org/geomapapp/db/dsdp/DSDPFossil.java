package org.geomapapp.db.dsdp;

public class DSDPFossil {
	short code;
	public DSDPFossil( int code, int abundance ) {
		int c = code<<3 + abundance;
		this.code = (short)c;
	}
	public int getFossilCode() {
		int c = (code>>3)&0x0000ffff;
		return c;
	}
	public int getAlterationCode() {
		return (int)(code&7);
	}
}
