package com.iped.ipcam.utils;

public class URLs {
	public final static String HOST = "www.drovik.com";//192.168.1.213
	public final static String HTTP = "http://";
	public final static String HTTPS = "https://";
	
	private final static String URL_SPLITTER = "/";
	private final static String URL_API_HOST = HTTP + HOST + URL_SPLITTER;
	public final static String UPDATE_VERSION = URL_API_HOST+"apk_down_load/AppVersion.xml";
}
