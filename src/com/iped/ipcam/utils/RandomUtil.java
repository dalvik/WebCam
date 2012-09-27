package com.iped.ipcam.utils;

import java.util.Random;

public class RandomUtil {

	public static int getRandInt() {
		Random random = new Random(System.currentTimeMillis());
		return random.nextInt();
	}

	public static String generalRandom() {
		StringBuffer randomStr = new StringBuffer();
		for (int i = 0; i < 10; i++) {
			randomStr.append(getRandInt());
		}
		return randomStr.toString();
	}
}
