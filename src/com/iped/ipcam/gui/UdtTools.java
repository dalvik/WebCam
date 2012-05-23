package com.iped.ipcam.gui;

public class UdtTools {

	static {
		//System.loadLibrary("m");
		System.loadLibrary("udt");
		System.loadLibrary("stlport_shared");
		System.loadLibrary("RecvFile");
	}
	
	public static  native int recvFile(String ip,String port, String remoteName,String localName);
	public static native int sendFile(String sendFileName, String port);
	public static native int initSocket(String remoteIp, int localPort, int remotePort);
	public static native int recvVideoData(byte[] buf, int bufferLength);
	public static native int release();
	
}
