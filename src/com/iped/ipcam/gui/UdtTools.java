package com.iped.ipcam.gui;

public class UdtTools {

	static {
		System.loadLibrary("udt");
		System.loadLibrary("RecvFile");
		System.loadLibrary("amr");
		System.loadLibrary("pcm");
	}
	
	public static  native int recvFile(String ip,String port, String remoteName,String localName);
	public static native int sendFile(String sendFileName, String port);
	// recv video and audio
	public static native int initSocket(String remoteIp, int localVideoPort, int remoteVideoPort, int localAudioPort, int remoteAudioPort, int videoBufferLength, int audioBufferLength);
	public static native int recvVideoData(byte[] buf, int bufferLength);
	public static native int recvAudioData(int serverSendAudioBufferLength, byte[] clientRecvBuffer, int clientRecvBufferLength);
	//public static native int release();
	
	// decode amr to pcm
	public static native int initAmrDecoder();
	
	public static native int amrDecoder(byte[] src, int srcLength, byte[] des, int desLength, int chanel);
	
	public static native void exitAmrDecoder();
	
	//decode pcm to amr
	public static native int initAmrEncoder();
	
	public static native int EncoderPcm(byte[] pcmBuffer, int pcmLength, byte[] amrBuffer, int amrLength);
	
	// change throughnet type
	
	public static native int startSearch();
	
	public static native void stopSearch();
	
	public static native String fetchCamId();
	
	public static native int monitorSocket(String camId);
	
	public static native int monitorCmdSocket(String camId, String random);
	
	public static native int checkCmdSocketEnable(String camId);
	
	public static native int initialSocket(String camId, String random);
	
	public static native int sendCmdMsgById(String camId, String cmd, int cmdLength);
	
	public static native int recvCmdMsgById(String camId, byte[] buf, int bufLength);
	
	public static native int recvVideoMsg(byte[] buf, int bufLength);
	
	public static native int recvAudioMsg(int smalBuffLength, byte[] buf, int bufLength);
	
	public static native int sendAudioMsg(byte[] buf, int sendLength);
	
	public static native int sendPTZMsg(byte[] comm);
	
	public static native int close();
	
	public static native int exit();
	
	public static native int startUp();
	
	public static native int cleanUp();
	
	
}
