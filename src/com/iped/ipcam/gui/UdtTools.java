package com.iped.ipcam.gui;

public class UdtTools {

	static {
		//System.loadLibrary("m");
		System.loadLibrary("udt");
		System.loadLibrary("stlport_shared");
		System.loadLibrary("RecvFile");
	}
	
	static public native int recvFile(String ip,String port, String remoteName,String localName);
	static public native int sendFile(String sendFileName, String port);
}
