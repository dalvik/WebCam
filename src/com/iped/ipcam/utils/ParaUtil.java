package com.iped.ipcam.utils;

import java.util.Map;
import java.util.Set;

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
				Log.d(TAG, "put para   " + key  + "=" + value);
				paraMap.put(key, value);
			}
		}
	}
	
	public static String enCapsuPara(Map<String,String> paraMap) {
		Set<String> keySet = paraMap.keySet();
		StringBuffer sb = new StringBuffer();
		for(String s:keySet) {
			sb.append(s + "=" + paraMap.get(s) + "\n");
		}
		return sb.toString();
	}
}
