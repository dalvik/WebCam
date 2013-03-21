package com.iped.ipcam.factory;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;

public abstract class DecoderFactory implements Runnable {

	public abstract int getIndexForGet();
	
	public abstract void setOnMpegPlayListener(OnMpegPlayListener listener);
	
	public abstract void onStop(boolean stopPlay);
	
	
	public void reset(){
		
	}
	
	public void checkResulation(int resulation) {
		
	}
}
