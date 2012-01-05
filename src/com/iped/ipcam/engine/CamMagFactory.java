package com.iped.ipcam.engine;


public class CamMagFactory {

	public static ICamManager camManager = null;
	
	public static IVideoManager videoManager = null;
	
	public static ICamManager getCamManagerInstance() {
		if(camManager == null) {
			camManager = new CamManagerImp();
		}
		return camManager;
	}
	
	public static IVideoManager getVideoManagerInstance() {
		if(videoManager == null) {
			videoManager = new VideoManagerImp();
		}
		return videoManager;
	}
}
