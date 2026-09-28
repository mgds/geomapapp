package org.geomapapp.credit;

import org.geomapapp.grid.*;

public class GMAObject {
	public Grid2D.Boolean mask;
	public String name;
	public String url;
	public int nation;
	/**
	 * Class constructor specifying objects to create.
	 * 
	 * @param name
	 * @param url
	 * @param nation
	 * @param mask
	 */
	public GMAObject(String name, String url, int nation, Grid2D.Boolean mask) {
		this.mask = mask;
		this.name = name;
		this.url = url;
		this.nation = nation;
	}
	public String toString() {
		return name;
	}
}
