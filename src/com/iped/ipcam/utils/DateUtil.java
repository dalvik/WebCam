package com.iped.ipcam.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	
	public static Date formatTimeToDate(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			return new Date();
		}
		return date;
	}
	
	public static Date formatTimeToDate2(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			return new Date();
		}
		return date;
	}
	
	public static String formatTimeStrToTimeStr(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if(date.getYear()<70) {
				return "";
			}
			return sdf.format(date);
		} catch (ParseException e) {
			return "";
		}
	}
	
	public static String formatTimeToDate3(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdf.format(new Date(time));
	}
	
	public static String formatTimeToDate4(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(new Date(time));
	}
	
	public static String formatTimeToDate5(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
		return sdf.format(new Date(time));
	}
	
	public static String formatTimeToDate6(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(time);
		if(date.getYear()<=80) {
			return sdf.format(new Date());
		}
		return sdf.format(date);
	}
	
	public static Date formatTimeToDate5(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			return new Date();
		}
		return date;
	}
	
	public static Date formatTimeToDate6(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			return new Date();
		}
		return date;
	}
	
	public static String formatTimeStrToTimeStr2(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
			sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.format(date);
		} catch (ParseException e) {
			return dateStr;
		}
	}
	
	public static String formatTimeStrToTimeStr3(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
			sdf = new SimpleDateFormat("HH:mm:ss");
			return sdf.format(date);
		} catch (ParseException e) {
			return dateStr;
		}
	}
	
	public static long formatTimeStrToLong(String timeStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			return sdf.parse(timeStr).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}
	
	public static long formatTimeStrToMillionSecond(String timeStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		try {
			return sdf.parse(timeStr).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}
}
