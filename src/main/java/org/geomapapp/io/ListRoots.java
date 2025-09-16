package org.geomapapp.io;

import java.io.*;

public class ListRoots {
	public static void main(String[] args) {
		File[] roots = File.listRoots();
		for( int k=0 ; k<roots.length ; k++) System.out.println(roots[k].getPath());
	}
}
