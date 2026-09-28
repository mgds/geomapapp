package org.geomapapp.util;

import haxby.util.URLFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;

public class NetUtil {

	public static boolean ping(URL url) {
		return ping(url.getHost());
	}

	public static boolean ping(String host) {
		try { 
			URL url = URLFactory.url(host);
			url.openConnection().getInputStream().read();
		}
		catch (UnknownHostException ex) {
			return false;
		}
		catch (ConnectException ex){
			if (ex.getMessage().startsWith("Connection refused"))
				return true;
			else if (ex.getMessage().startsWith("Connection timed"))
				return false;
			else if (ex.getMessage().startsWith("Software caused connection abort"))
				return false;
			else {
				ex.printStackTrace();
				return false;
			}
		} catch (IOException ex) { ex.printStackTrace(); return false; }
		
		return true;
	}

	public static void main(String[] args){
		System.out.println(ping("127.0.0.1"));
		System.out.println(ping("127.0.0.2"));
		System.out.println(ping("999.999.99.99"));
	}
}