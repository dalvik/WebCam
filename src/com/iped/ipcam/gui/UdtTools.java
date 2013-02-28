package com.iped.ipcam.gui;

public class UdtTools {

	static {
		System.loadLibrary("udt");
		System.loadLibrary("RecvFile");
		System.loadLibrary("amr");
		System.loadLibrary("pcm");
		System.loadLibrary("xvid");
	}
	public static String getTime() {
		System.out.println("-------qqqq");
		return "aaaa--------";
	}
	//public static void myCallBackFunc(String msg) {
		
		//System.out.println("myCallBackFunc ==>" + msg);
	//}
	
	// recv video and audio
	public static native int initSocket(String remoteIp, int localVideoPort, int remoteVideoPort, int localAudioPort, int remoteAudioPort, int videoBufferLength, int audioBufferLength);
	public static native int recvVideoData(byte[] buf, int bufferLength);
	public static native int recvAudioData(int serverSendAudioBufferLength, byte[] clientRecvBuffer, int clientRecvBufferLength);
	
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
	
	public static native String monitorSocket(String camId);
	
	public static native String monitorCmdSocket(String camId, String random);
	
	public static native int freeCmdSocket();
	
	//public static native int freeConnectionById(String camId);
	
	//public static native int checkCmdSocketEnable(String camId);
	
	public static native int sendCmdMsg(String cmd, int cmdLength);
	
	public static native int sendCmdMsgById(String camId, String cmd, int cmdLength);
	
	public static native int recvCmdMsg(byte[] buf, int bufLength);
	
	public static native int recvCmdMsgById(String camId, byte[] buf, int bufLength);
	
	public static native int recvVideoMsg(byte[] buf, int bufLength);
	
	public static native int recvAudioMsg(int smalBuffLength, byte[] buf, int bufLength);
	
	public static native int sendAudioMsg(byte[] buf, int sendLength);
	
	public static native int sendPTZMsg(byte[] comm);
	
	//app exit
	public static native int close();
	
	//video thread interrupt
	public static native int exit();
	
	public static native int startUp();
	
	public static native int cleanUp();
	
	//mpeg4 decoder
	public static native int initXvidDecorer();
	
	public static native int xvidDecorer(byte[] src, int length, byte[] rgb, int flag);

	public static native int[] initXvidHeader(byte[] src, int length);
	
	public static native void freeDecorer();
	
	public static native int[] decodeYUV420SP(byte[] data, int width, int height);
	
}
