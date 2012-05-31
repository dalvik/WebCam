package com.iped.ipcam.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iped.ipcam.pojo.WifiConfig;

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
		Log.d(TAG, sb.toString());
		return sb.toString();
	}
	
	public static List<WifiConfig> encapsuWifiConfig(String wifiInfo) {
		String[] info = wifiInfo.split("\n");
		List<WifiConfig> list = new ArrayList<WifiConfig>();
		for(String s:info) {
			String[] ss = s.split("\t");
			int l = ss.length;
			if(l>=4){
				WifiConfig config = new WifiConfig();
				config.setSsid(s.substring(ss[0].indexOf("=") + 1).trim());
				config.setSignal_level(s.substring(ss[1].indexOf("=") + 1).trim());
				config.setProto(s.substring(ss[2].indexOf("=") + 1).trim());
				config.setKey_mgmt(s.substring(ss[3].indexOf("=") + 1).trim());
				config.setPairwise(s.substring(ss[4].indexOf("=") + 1).trim());
				config.setGroup(s.substring(ss[5].indexOf("=") + 1).trim());
				System.out.println("wifi config = " + config);
				list.add(config);
			}
		}
		return list;
	}
}
