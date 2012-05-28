package com.iped.ipcam.utils;

import java.util.Map;

import android.util.Log;

public class ParaUtil {

	private static String TAG = "ParaUtil";
	
	public static void putParaByString(String para, Map<String,String> paraMap) {
		String[] info = para.split("\n");
		//record_sensitivity=1
		for(String s:info) {
			int splitFlagIndex = s.indexOf("=");
			if(splitFlagIndex > 0 ) {
				String key = s.substring(0, splitFlagIndex).trim();
				String value = s.substring(splitFlagIndex + 1).trim();
				Log.d(TAG, "put para key = " + key  + "  value = " + value);
				paraMap.put(key, value);
			}
		}
	}
}
