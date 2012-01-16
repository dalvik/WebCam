package com.iped.ipcam.utils;

import java.io.File;
import java.util.Comparator;


/** 排序 **/
public class FileComparator implements Comparator<String> {

	public int compare(String s1, String s2) {
		// 文件夹排在前面
		File file1 = new File(s1);
		File file2 = new File(s2);
		if (file1.isDirectory() && !file2.isDirectory()) {
			return -1000;
		} else if (!file1.isDirectory() && file2.isDirectory()) {
			return 1000;
		}
		// 相同类型按名称排序
		return s1.compareTo(s2);
	}
}