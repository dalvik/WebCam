package com.iped.ipcam.utils;

import java.io.File;
import java.util.Comparator;


/** ���� **/
public class FileComparator implements Comparator<String> {

	public int compare(String s1, String s2) {
		// �ļ�������ǰ��
		File file1 = new File(s1);
		File file2 = new File(s2);
		if (file1.isDirectory() && !file2.isDirectory()) {
			return -1000;
		} else if (!file1.isDirectory() && file2.isDirectory()) {
			return 1000;
		}
		// ��ͬ���Ͱ���������
		return s1.compareTo(s2);
	}
}