package com.iped.ipcam.engine;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.MyVideoView.OnPutIndexListener;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.pojo.MpegImage;
import com.iped.ipcam.utils.VideoQueue;

public class PlayH264Thread extends DecoderFactory implements OnPutIndexListener {

	private VideoQueue queue = null;
	
	private int startFlagCount = 1;
	
	private Object lock = new Object();
	
	private byte[] rgbDataBuf = null;
	
	private int length = 1 * 400 * 1024;
	
	//int bufNeedLength = length - 5;
	
	private byte[] playH264Buf = null;
	
	private int usedBytes = length;
	
	private int unusedBytes = 0;
	
	private int mpegDataLength = 0;
	
	private boolean startFlag = false;
	
	private int headFlagCount = 0;
	
	private String time = "";
	
	private byte[] h264Buf = null;
	
	private boolean imageDataStart = false;
	
	private int mpegPakages = 3;
	
	private boolean stopPlay = false;
	
	private boolean DEBUG = true;
	
	private String TAG = "PlayMpegThread";
	
	private int indexForPut = 0;
	
	private int indexForGet = 0;
	
	private final static int NALBUFLENGTH = MyVideoView.NALBUFLENGTH;
	
	private boolean isMpeg4 = false;
	
	private OnMpegPlayListener listener;
	
	private String timeStr;
	
	private Bitmap video;
	
	//private int frameCount;
	
	private MyVideoView myVideoView;
	
	private boolean checkResulationFlag = true;
	
	private boolean canStartFlag = false;
	
	private static boolean flag = true;
	
	private int imageWidth = 0;
	
	public PlayH264Thread(boolean play,MyVideoView myVideoView, byte[] mpegBuf, String timeStr, Bitmap video, int frameCount ) {
		this.h264Buf = mpegBuf;
		this.timeStr = timeStr;
		this.video = video;
		queue = new VideoQueue();
		this.myVideoView = myVideoView;
		if(play) {
			myVideoView.setOnPutIndexListener(this);
		}
		//jpegByteBuf = new byte[jpegByteBufLength]; 
		playH264Buf = new byte[length];
	}
	
	@Override
	public void run() {
		stopPlay = false;
		flag = true;
		/*ShowMpeg showMpeg = new ShowMpeg();
		Thread thread = new Thread(showMpeg);
		thread.start();*/
		while(!stopPlay) {
			do{
				if((indexForGet+5)%NALBUFLENGTH == indexForPut){
					synchronized (playH264Buf) {
						try {
							playH264Buf.wait(20);
						} catch (InterruptedException e) {
							stopPlay = true;
							//showMpeg.setInerrupt();
							Log.e(TAG, "play h264 thread InterruptedException");
							break;
						}
					}  
				}else {
					byte b0 = h264Buf[indexForGet];
					byte b1 = h264Buf[(indexForGet+1)%NALBUFLENGTH];
					byte b2 = h264Buf[(indexForGet+2)%NALBUFLENGTH];
					byte b3 = h264Buf[(indexForGet+3)%NALBUFLENGTH];
					byte b4 = h264Buf[(indexForGet+4)%NALBUFLENGTH];
					if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
						//System.out.println("notify");
						myVideoView.notifyed();
						canStartFlag = true;
						playH264Buf[mpegDataLength] = b0;
						playH264Buf[mpegDataLength + 1] = b1;
						playH264Buf[mpegDataLength + 2] = b2;
						playH264Buf[mpegDataLength + 3] = b3;
						playH264Buf[mpegDataLength + 4] = b4;
						mpegDataLength += 5;
						indexForGet+=4;
						startFlag = true;
						isMpeg4 = true;
						imageDataStart = false;
						headFlagCount = 0;
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "### h264 data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
						}
					}else if(b0 == -1 &&  b1 == -40 &&  b2 == -1 && b3 == -37) {
						//jpegByteBuf[jpegBufUsed++] = -1;
						startFlag = false;
						imageDataStart = true;
						isMpeg4 = false;
						//jpegBufUsed = 1;
						//System.out.println("jpeg code = " + jpegTimeTmp);
					}/*else if(isMpeg4 && b0 == 0 &&  b1 == 0) {
						//System.out.println("startFlagCount = " + startFlagCount);
						if(startFlagCount++ % mpegPakages == 0 && canStartFlag){ //
							startFlagCount = 1;
							break;
						}
						if(imageDataStart) {
							imageDataStart = false;
						}
						isMpeg4 = false;
						playH264Buf[mpegDataLength++] = b0;
						playH264Buf[mpegDataLength++] = b1;
						indexForGet+=2;
					}else {
						if(startFlag) {
							playH264Buf[mpegDataLength++] = h264Buf[indexForGet];
							headFlagCount++;
							if(headFlagCount >= 18) { 
								startFlag = false;
								time = new String(playH264Buf, mpegDataLength-18, 18);
								queue.addNewTime(time);
							}
						} else {
							if(canStartFlag) {
								if(imageDataStart) {
									//jpegByteBuf[jpegBufUsed++] = b0;
									//tmpJpgBufUsed = jpegBufUsed;
								}else {
									playH264Buf[mpegDataLength++] = b0;
								}
							}
						}
					} */
					indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
				}
			} while(!stopPlay);
		}
		//showMpeg.setInerrupt();
		//onStop();
	}
	
	public void reset() {
		/*indexForPut = 0;
		indexForGet = 0;
		mpegPakages = 3;
		isMpeg4 = false;
		startFlag = false;
		canStartFlag = false;
		queue.clear();*/
		int imageHeight = caculateImageHeight(imageWidth);
		if(imageHeight != 480) {
			checkResulationFlag = false;
		}
	}
	
	private void onStop() {
		queue.clear();
		System.gc();
		stopPlay = true;
		if(video != null && !video.isRecycled()) {
			video.recycle();
			video = null;
		}
		myVideoView.setImage(null);
		this.rgbDataBuf = null;
		//this.jpegByteBuf = null;
		this.playH264Buf = null;
		this.listener = null;
		System.gc();
		System.runFinalization();
		UdtTools.freeDecorer();
		if(BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "### play mpeg thread exit ....");
		}
	}
	
	private class ShowMpeg implements Runnable {
		
		public void run() {
			while(!Thread.currentThread().isInterrupted() && !stopPlay) {
				String oldTime = queue.pollTime();
				if(null != oldTime) {
					timeStr = oldTime.substring(0,14);
					MpegImage mpegImage = queue.getMpegImage();
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.d(TAG, "### show mpegImage = " + mpegImage );
					}
					if(mpegImage != null) {
						byte[] tmpRgb = mpegImage.rgb;
						ByteBuffer sh = ByteBuffer.wrap(tmpRgb);
						if(video != null) {
							try {
								video.copyPixelsFromBuffer(sh);
							} catch (Exception e) {
								Log.e(TAG, "### copyPixelsFromBuffer exception!");
							}
						}
					}
					if(listener != null) {
						listener.invalide( timeStr);
					}
				} else {
					synchronized (lock) {
						try {
							lock.wait(10);
						} catch (InterruptedException e) {
							stopPlay = true;
							Log.e(TAG, e.getLocalizedMessage());
							break;
						}
					}  
				}
			}
		}
		
		public void setInerrupt() {
			if(!Thread.currentThread().isInterrupted()){
				Thread.currentThread().interrupt();
			}
			stopPlay = true;
		}
	}
	
	@Override
	public void updatePutIndex(int indexForPut) {
		this.indexForPut = indexForPut;
	}
	
	@Override
	public int getIndexForGet() {
		return indexForGet;
	}
	
	public interface OnMpegPlayListener {
		public void invalide(String timeStr);
	}
	
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void onStop(boolean stopPlay) {
		if(!Thread.currentThread().isInterrupted()){
			Thread.currentThread().interrupt();
		}
		this.stopPlay = stopPlay;
		if(BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "### onStop exec stoped ");
		}
	}
	
	/*@Override
	public void checkResulation(int resulation) {
		super.checkResulation(resulation);
		if(rgbDataBuf.length != resulation) {
			checkResulationFlag  = true;
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### check resulation  " + resulation);
			}
		}
	}*/
	
	private int caculateImageHeight(int newImageWidth) {
		int newImageHeight = 0;
		if(newImageWidth == 1280) {
			newImageHeight = 720;
		}else if(newImageWidth == 640) {
			newImageHeight = 480;
		}else if(newImageWidth == 352) {
			newImageHeight = 288;
		}
		return newImageHeight;
	}

	@Override
	public void setOnMpegPlayListener(
			com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener listener) {
		// TODO Auto-generated method stub
		
	}
	
}
