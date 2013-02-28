package com.iped.ipcam.utils;

import android.os.Handler;
import android.os.Message;

public class MessageUtils {

	private MessageUtils() {
		
	}
	
	public static void sendErrorMessage(Handler mHandler, int errorStrId) {
		Message msg = mHandler.obtainMessage();
		msg.what = Constants.CONNECTERROR;
		msg.arg1 = errorStrId;
		mHandler.sendMessage(msg);
	}
	
	public static void sendErrorMessage(Handler mHandler, String errorInfo) {
		Message msg = mHandler.obtainMessage();
		msg.what = Constants.CONNECTERRORINFO;
		msg.obj = "¡¨Ω” ß∞‹ " + errorInfo;
		mHandler.sendMessage(msg);
	}
}
