package com.iped.ipcam.engine;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

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

public class PlayMpegThread extends DecoderFactory implements OnPutIndexListener {

	private VideoQueue queue = null;
	
	private int startFlagCount = 1;
	
	private Object lock = new Object();
	
	private byte[] rgbDataBuf = null;
	
	private int length = 1 * 400 * 1024;
	
	//int bufNeedLength = length - 5;
	
	private byte[] mpegBuf = null;
	
	private int usedBytes = length;
	
	private int unusedBytes = 0;
	
	private int mpegDataLength = 0;
	
	private boolean startFlag = false;
	
	private int headFlagCount = 0;
	
	private int jpegByteBufLength = 2 * 150 * 1024;
	
	private byte[] jpegByteBuf = null; 
	
	private int jpegBufUsed = 0;
	
	private int tmpJpgBufUsed = 0;
	
	private String time = "";
	
	private byte[] nalBuf = null;
	
	private boolean imageDataStart = false;
	
	///queue = new VideoQueue();
	
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
	
	private int frameCount;
	
	private MyVideoView myVideoView;
	
	private String jpegTimeTmp = "";
	
	private boolean checkResulationFlag = false;
	
	private boolean canStartFlag = false;
	
	private static boolean flag = true;
	
	public PlayMpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount ) {
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		queue = new VideoQueue();
		this.myVideoView = myVideoView;
		myVideoView.setOnPutIndexListener(this);
		jpegByteBuf = new byte[jpegByteBufLength]; 
		mpegBuf = new byte[length];
	}
	
	@Override
	public void run() {
		stopPlay = false;
		int res = UdtTools.initXvidDecorer();
		if(res != 0) {
			stopPlay = true;
			Log.d(TAG, "xvid init decoder error " + res);
			return ;
		}
		new Thread(new ShowMpeg()).start();
		while(!stopPlay) {
			do{
				if((indexForGet+5)%NALBUFLENGTH == indexForPut){
					synchronized (mpegBuf) {
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "### data buffer is empty! ---->");
						}
						try {
							mpegBuf.wait(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}  
				}else {
					byte b0 = nalBuf[indexForGet];
					byte b1 = nalBuf[(indexForGet+1)%NALBUFLENGTH];
					byte b2 = nalBuf[(indexForGet+2)%NALBUFLENGTH];
					byte b3 = nalBuf[(indexForGet+3)%NALBUFLENGTH];
					byte b4 = nalBuf[(indexForGet+4)%NALBUFLENGTH];
					if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
						canStartFlag = true;
						mpegBuf[mpegDataLength] = b0;
						mpegBuf[mpegDataLength + 1] = b1;
						mpegBuf[mpegDataLength + 2] = b2;
						mpegBuf[mpegDataLength + 3] = b3;
						mpegBuf[mpegDataLength + 4] = b4;
						mpegDataLength += 5;
						indexForGet+=4;
						startFlag = true;
						isMpeg4 = true;
						imageDataStart = false;
						headFlagCount = 0;
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
						}
					}else if(b0 == -1 &&  b1 == -40 &&  b2 == -1 && b3 == -37) {
						jpegByteBuf[jpegBufUsed++] = -1;
						startFlag = false;
						imageDataStart = true;
						isMpeg4 = false;
						jpegBufUsed = 1;
						jpegTimeTmp  = time;
					}else if(isMpeg4 && b0 == 0 &&  b1 == 0) {
						if(startFlagCount++ %mpegPakages == 0 && canStartFlag){
							startFlagCount = 1;
							break;
						}
						if(imageDataStart) {
							imageDataStart = false;
							//ignore jpeg
							/*Bitmap v = BitmapFactory.decodeByteArray(jpegByteBuf, 0, tmpJpgBufUsed);
							if(v != null) {
								queue.addJpegImage(new JpegImage(v, jpegTimeTmp));
								if(BuildConfig.DEBUG && DEBUG) {
									Log.d(TAG, "### add jpeg  time=" + jpegTimeTmp);
								}
							}*/
						}
						isMpeg4 = false;
						mpegBuf[mpegDataLength++] = b0;
						mpegBuf[mpegDataLength++] = b1;
						indexForGet+=2;
					}else {
						if(startFlag) {
							mpegBuf[mpegDataLength++] = nalBuf[indexForGet];
							headFlagCount++;
							if(headFlagCount >= 18) { 
								startFlag = false;
								time = new String(mpegBuf, mpegDataLength-18, 18);
								queue.addNewTime(time);
								if(BuildConfig.DEBUG && !DEBUG) {
									Log.d(TAG, "### add new   time=" + time + "  time list length " + queue.getTimeListLength());
								}
							}
						} else {
							if(canStartFlag) {
								if(imageDataStart) {
									jpegByteBuf[jpegBufUsed++] = b0;
									tmpJpgBufUsed = jpegBufUsed;
								}else {
									mpegBuf[mpegDataLength++] = b0;
								}
							}
						}
					} 
					indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
				}
			} while(!stopPlay);
			startFlag = false;
			if(rgbDataBuf == null && !stopPlay) {
				mpegPakages = 2;
				int[] headInfo = UdtTools.initXvidHeader(mpegBuf, length);//length的长度即为out_buffer的长度，所以length要足够长。
				int imageWidth = headInfo[0];
				int imageHeight = headInfo[1];
				usedBytes = headInfo[2];
				unusedBytes = (mpegDataLength - usedBytes);
				System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
				mpegDataLength = unusedBytes;
				System.gc();
				rgbDataBuf = new byte[imageWidth * imageHeight * 4];
				Log.d(TAG, "### W = " + imageWidth + " H = " + imageHeight + " used_bytes = " + usedBytes + " rgb length = " + rgbDataBuf.length);
				synchronized (lock) {
					if(video != null && !video.isRecycled()) {
						video.recycle();
						video = null;
					}
					video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
					myVideoView.setImage(video);
				}
				if(flag) {
					flag = false;
					myVideoView.updateResulation(imageWidth);
				}
			} else if(!stopPlay){
				usedBytes = UdtTools.xvidDecorer(mpegBuf, mpegDataLength, rgbDataBuf);
				if(checkResulationFlag) {
					checkResulationFlag = false;
					mpegDataLength = 0;
					mpegPakages = 3;
					indexForGet = indexForPut;
					Arrays.fill(rgbDataBuf,(byte) 0);
					Arrays.fill(mpegBuf,(byte) 0);
					rgbDataBuf = null;
					canStartFlag = false;
					continue;
				}
				if(usedBytes>999999) {//(XDIM * 100000) + used_bytes;
					int newImageWidth = usedBytes / 1000000;
					int useBytes = usedBytes%1000000;
					int newImageHeight = caculateImageHeight(newImageWidth);
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### return value " + usedBytes + " useBytes = " + useBytes + " newWidth = " + newImageWidth + " newHeight = "+ newImageHeight);
					}
					rgbDataBuf = new byte[newImageWidth * newImageHeight * 4];
					if(video != null && !video.isRecycled()) {
						video.recycle();
						video = null;
					}
					video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
					myVideoView.setImage(video);
					myVideoView.updateRect();
					myVideoView.updateResulation(newImageWidth);
					unusedBytes = (mpegDataLength - useBytes);
					if(unusedBytes<=0) {
						unusedBytes = 0;
					}
					System.arraycopy(mpegBuf, useBytes, mpegBuf, 0, unusedBytes);
					mpegDataLength = unusedBytes;
				} else {
					MpegImage mpegImage = new MpegImage(rgbDataBuf, time);
					queue.addMpegImage(mpegImage);
					unusedBytes = (mpegDataLength - usedBytes);
					System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
					mpegDataLength = unusedBytes;
				}
			}
		}
		UdtTools.freeDecorer();
		if(video != null && !video.isRecycled()) {
			video.recycle();
			video = null;
		}
		this.rgbDataBuf = null;
		jpegByteBuf = null;
		mpegBuf = null;
		System.gc();
		if(BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "### exit ...." + Runtime.getRuntime().freeMemory());
		}
	}
	
	private class ShowMpeg implements Runnable {
		
		public void run() {
			while(!stopPlay) {
				if(queue.getMpegLength()>0) {
					String oldTime = queue.removeTime();
					timeStr = oldTime.substring(0,14);
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.d(TAG, "### remove new   time=" + oldTime + "  time list length " + queue.getTimeListLength() + " timeStr = " + timeStr);
					}
					if(!popuJpeg(oldTime)){
						byte[] tmpRgb = queue.getMpegImage().rgb;
						ByteBuffer sh = ByteBuffer.wrap(tmpRgb);
						//Log.d(TAG, "timeStr=" + timeStr + " frameCount =" + frameCount);
						if(video != null) {
							video.copyPixelsFromBuffer(sh);
							frameCount = myVideoView.getFrameCount();
							frameCount++;
							if(listener != null) {
								listener.invalide(frameCount, timeStr);
							}
						}
					}
				}else {
					synchronized (lock) {
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### no image data ---->");
						}
						try {
							lock.wait(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}  
				}
			}
			if(rgbDataBuf != null) {
				rgbDataBuf = null;
				System.gc();
			}
			
			int imageRemain = queue.getMpegLength();
			if(imageRemain>0) {
				for(int i=0;i<imageRemain;i++) {
					MpegImage image =queue.getMpegImage();
					if(image.rgb != null) {
						image.rgb = null;
						image = null;
						Log.d(TAG, "### play mpeg thread recycle remain mepg image!");
					}
				}
			}
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### play mpeg thread exit!");
			}
		}
		
		public boolean popuJpeg(String oldTime) {
			if(queue.getImageListLength()<=0) {
				return false;
			}
			JpegImage image = queue.getFirstImage();
			if(BuildConfig.DEBUG && !DEBUG) {
				Log.d(TAG, "### show time = " + oldTime + " == " + image.time);
			}
			if(oldTime.compareTo(image.time) ==0) {
				if(BuildConfig.DEBUG && DEBUG) {
					Log.d(TAG, "### popu jpeg " + image.time);
				}
				queue.removeImage();
				video = image.bitmap;
				myVideoView.setImage(video);
				frameCount = myVideoView.getFrameCount();
				frameCount++;
				if(listener != null) {
					listener.invalide(frameCount, timeStr);
				}
				return true;
			} else if(oldTime.compareTo(image.time) > 0) {
				queue.clear();
			}
			return false;
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
		public void invalide(int frameCount, String timeStr);
	}
	
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void onStop(boolean stopPlay) {
		this.stopPlay = stopPlay;
		if(BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "### stoped ....");
		}
	}
	
	@Override
	public void checkResulation(int resulation) {
		super.checkResulation(resulation);
		if(rgbDataBuf.length != resulation) {
			checkResulationFlag  = true;
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### check resulation  " + resulation);
			}
		}
	}
	
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
	
}
