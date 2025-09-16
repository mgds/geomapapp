package haxby.util;

import java.io.*;

public class InOut implements Runnable {
	InputStream in;
	OutputStream out;
	int size;
	public InOut(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		size = 4096;
	}
	public InOut(InputStream in, OutputStream out, int size) {
		this.in = in;
		this.out = out;
		this.size = size;
	}
	public void run() {
		byte[] buf = new byte[size];
		int n;
		try {
			while( (n = in.read(buf)) != -1) {
				if(n>0) out.write(buf, 0, n);
			}
			in.close();
			out.close();
		} catch (IOException e) {
		}
	}
}
