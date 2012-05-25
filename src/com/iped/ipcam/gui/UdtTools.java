package com.iped.ipcam.gui;

public class UdtTools {

	static {
		//System.loadLibrary("m");
		System.loadLibrary("udt");
		System.loadLibrary("stlport_shared");
		System.loadLibrary("RecvFile");
		System.loadLibrary("amr");
	}
	
	public static  native int recvFile(String ip,String port, String remoteName,String localName);
	public static native int sendFile(String sendFileName, String port);
	// recv video and audio
	public static native int initSocket(String remoteIp, int localVideoPort, int remoteVideoPort, int localAudioPort, int remoteAudioPort, int videoBufferLength, int audioBufferLength);
	public static native int recvVideoData(byte[] buf, int bufferLength);
	public static native int recvAudioData(int serverSendAudioBufferLength, byte[] clientRecvBuffer, int clientRecvBufferLength);
	public static native int release();
	
	// decode amr to pcm
	public static native int initAmrDecoder();
	
	public static native int amrDecoder(byte[] src, int srcLength, byte[] des, int desLength, int chanel);
	
	public static native void exitAmrDecoder();
	
	
	
	
}
