package org.geomapapp.util;

import java.util.*;

public class DateFmt {
	Calendar cal;
	public DateFmt() {
		cal = Calendar.getInstance();
	}
	public DateFmt(String timeZone) {
		cal = Calendar.getInstance( TimeZone.getTimeZone(timeZone) );
	}
	public String format(int secs) {
		cal.setTime( new Date( 1000L*(long)secs ));
		int hr = cal.get(cal.HOUR_OF_DAY) ;
		int day = cal.get(cal.DAY_OF_MONTH);
		int mo = cal.get(cal.MONTH)+1;
		int min = cal.get(cal.MINUTE);
		int sec = cal.get(cal.SECOND);
		String date = cal.get(cal.YEAR) +"/"+
			( mo<10 ? "0" : "") +
			mo +"/"+
			( day<10 ? "0" : "") +
			day +" "+
			( hr<10 ? "0" : "") +
			hr+":"+
			( min<10 ? "0" : "") +
			min+
			( sec<10 ? ":0": ":") +
			sec;
		return date;
	}
}
