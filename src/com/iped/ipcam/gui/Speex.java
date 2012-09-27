package com.iped.ipcam.gui;

public class Speex {

	private static final int DEFAULT_COMPRESSION = 8;

	static {
		try {
			System.loadLibrary("speex");
		} catch (Throwable e) {
			System.out.println("### " + e.getLocalizedMessage());
		}
	}

	private Speex() {
		
	}

	public void init() {
		open(DEFAULT_COMPRESSION);
	}

	public static native int initEcho(int frameLength, int taliLength);
	
	public static native int cancellation(short[] mic, short[] ref, short[] out);
	
	public static native int stopEcho();
	
	public static native int open(int compression);

	public static native int getFrameSize();

	public static native int decode(byte encoded[], short lin[], int size);

	public static native int encode(short lin[], int offset, byte encoded[], int size);

	public static native void close();
}
