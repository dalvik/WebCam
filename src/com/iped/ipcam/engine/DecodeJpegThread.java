package com.iped.ipcam.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.MyVideoView.OnPutIndexListener;
import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.utils.VideoQueue;

public class DecodeJpegThread extends DecoderFactory implements Runnable, OnPutIndexListener {

	private boolean stopPlay = false;
	
	private String TAG = "DecodeJpegThread";
	
	private boolean DEBUG = true;
	
	private int indexForPut = 0;
	
	private int indexForGet = 0;
		
	private final static int NALBUFLENGTH = MyVideoView.NALBUFLENGTH;
	
	private byte[] nalBuf = null;
	
	private int length = 1 * 512 * 1024;
	
	private byte[] jpegBuf = new byte[length];
	
	private	int jpegDataLength = 0;
	
	private boolean insideHeaderFlag = false;
	
	private	int insideHeadCount = 0;
	
	private String timeStr;
	
	private Bitmap video;
	
	private int frameCount;
	
	private MyVideoView myVideoView;
	
	private OnMpegPlayListener listener;
	
	private VideoQueue queue = null;
	
	private Object lock = new Object();
	
	private int timeOutCount = 1;
	
	public DecodeJpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount ) {
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		this.myVideoView = myVideoView;
		queue = new VideoQueue();
		myVideoView.setOnPutIndexListener(this);
	}
	
	@Override
	public void run() {
		stopPlay = false;
		Thread playAudioThread = new Thread(new PlayJpegThread());
		playAudioThread.start();
		do{
			if((indexForGet+5)%NALBUFLENGTH == indexForPut){
				synchronized (jpegBuf) {
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### data buffer is empty! ---->");
					}
					if(timeOutCount++ % 150 == 0) {
						stopPlay = true;
						if(playAudioThread != null && !playAudioThread.isInterrupted()) {
							playAudioThread.interrupt();
						}
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "### timeout exit ----------->");
						}
						break;
					}
					try {
						jpegBuf.wait(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}  
			}else {
				timeOutCount = 1;
				byte b0 = nalBuf[indexForGet];
				byte b1 = nalBuf[(indexForGet+1)%NALBUFLENGTH];
				byte b2 = nalBuf[(indexForGet+2)%NALBUFLENGTH];
				byte b3 = nalBuf[(indexForGet+3)%NALBUFLENGTH];
				byte b4 = nalBuf[(indexForGet+4)%NALBUFLENGTH];
				if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
					myVideoView.notifyed();
					indexForGet+=4;
					insideHeadCount = 0;
					insideHeaderFlag = true;
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
					}
				}else if(b0 == -1 &&  b1 == -40 &&  b2 == -1 && b3 == -32) {
					jpegBuf[0] = b0;
					jpegBuf[1] = b1;
					jpegBuf[2] = b2;
					jpegBuf[3] = b3;
					indexForGet+=3;
					video = BitmapFactory.decodeByteArray(jpegBuf, 0, jpegDataLength);
					queue.addJpegImage(new JpegImage(video, timeStr));
					jpegDataLength = 4;
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.d(TAG, "### jpeg start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
					}
				} else {
					if(insideHeaderFlag) {
						jpegBuf[jpegDataLength++] = nalBuf[indexForGet];
						insideHeadCount++;
						if(insideHeadCount >= 14) { 
							insideHeaderFlag = false;	
							timeStr = new String(jpegBuf, jpegDataLength-14, 14);
							//System.out.println("time = " + time + " " + jpegBuf[jpegDataLength-14] + " " + jpegBuf[jpegDataLength-13] + " " + jpegBuf[jpegDataLength-12] + " " + jpegBuf[jpegDataLength-11]);
						}
					}else {
						jpegBuf[jpegDataLength++] = b0;
					}
				}
				indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
			}
		} while(!stopPlay);
	}

	private class PlayJpegThread implements Runnable {
		
		public PlayJpegThread() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(queue.getImageListLength()>0) {
					JpegImage image = queue.removeImage();
					if(image != null) {
						myVideoView.setImage(image.bitmap);
						frameCount = myVideoView.getFrameCount();
						frameCount++;
						if(listener != null) {
							listener.invalide(image.time);
						}
					}
				}else {
					synchronized (lock) {
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### no image data ---->");
						}
						try {
							lock.wait(5);
						} catch (InterruptedException e) {
							stopPlay = true;
							queue.clear();
							e.printStackTrace();
						}
					}  
				}
			}
		}
	}
	
	@Override
	public void updatePutIndex(int putIndex) {
		this.indexForPut = putIndex;		
	}
	
	@Override
	public int getIndexForGet() {
		return indexForGet;
	}
	
	@Override
	public void onStop(boolean stopPlay) {
		this.stopPlay = stopPlay;
	}
	
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
		this.listener = listener;
	}
}
