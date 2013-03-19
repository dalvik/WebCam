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

public class PlayMpegThread extends DecoderFactory implements OnPutIndexListener {

	private VideoQueue queue = null;
	
	private int startFlagCount = 1;
	
	private Object lock = new Object();
	
	private byte[] rgbDataBuf = null;
	
	private int length = 1 * 400 * 1024;
	
	//int bufNeedLength = length - 5;
	
	private byte[] playMpegBuf = null;
	
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
	
	private byte[] mpegBuf = null;
	
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
	
	private String jpegTimeTmp = "";
	
	private boolean checkResulationFlag = false;
	
	private boolean canStartFlag = false;
	
	private static boolean flag = true;
	
	private Bitmap qvga  = null;
	
	private Bitmap vga = null;
	
	private Bitmap qp = null;
	
	public PlayMpegThread(boolean play,MyVideoView myVideoView, byte[] mpegBuf, String timeStr, Bitmap video, int frameCount ) {
		this.mpegBuf = mpegBuf;
		this.timeStr = timeStr;
		this.video = video;
		//this.frameCount = frameCount;
		queue = new VideoQueue();
		this.myVideoView = myVideoView;
		if(play) {
			myVideoView.setOnPutIndexListener(this);
		}
		jpegByteBuf = new byte[jpegByteBufLength]; 
		playMpegBuf = new byte[length];
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
		flag = true;
		ShowMpeg showMpeg = new ShowMpeg();
		Thread thread = new Thread(showMpeg);
		thread.start();
		while(!stopPlay) {
			do{
				if((indexForGet+5)%NALBUFLENGTH == indexForPut){
					synchronized (playMpegBuf) {
						try {
							playMpegBuf.wait(20);
						} catch (InterruptedException e) {
							stopPlay = true;
							showMpeg.setInerrupt();
							Log.e(TAG, "play mpeg thread InterruptedException");
							break;
						}
					}  
				}else {
					byte b0 = mpegBuf[indexForGet];
					byte b1 = mpegBuf[(indexForGet+1)%NALBUFLENGTH];
					byte b2 = mpegBuf[(indexForGet+2)%NALBUFLENGTH];
					byte b3 = mpegBuf[(indexForGet+3)%NALBUFLENGTH];
					byte b4 = mpegBuf[(indexForGet+4)%NALBUFLENGTH];
					if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
						//System.out.println("notify");
						myVideoView.notifyed();
						canStartFlag = true;
						playMpegBuf[mpegDataLength] = b0;
						playMpegBuf[mpegDataLength + 1] = b1;
						playMpegBuf[mpegDataLength + 2] = b2;
						playMpegBuf[mpegDataLength + 3] = b3;
						playMpegBuf[mpegDataLength + 4] = b4;
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
						//System.out.println("jpeg code = " + jpegTimeTmp);
					}else if(isMpeg4 && b0 == 0 &&  b1 == 0) {
						//System.out.println("startFlagCount = " + startFlagCount);
						if(startFlagCount++ % mpegPakages == 0 && canStartFlag){ //
							startFlagCount = 1;
							break;
						}
						if(imageDataStart) {
							imageDataStart = false;
						}
						isMpeg4 = false;
						playMpegBuf[mpegDataLength++] = b0;
						playMpegBuf[mpegDataLength++] = b1;
						indexForGet+=2;
					}else {
						if(startFlag) {
							playMpegBuf[mpegDataLength++] = mpegBuf[indexForGet];
							headFlagCount++;
							if(headFlagCount >= 18) { 
								startFlag = false;
								time = new String(playMpegBuf, mpegDataLength-18, 18);
								queue.addNewTime(time);
								//System.out.println("### time ========= " + time);
								/*if(BuildConfig.DEBUG && !DEBUG) {
									//Log.d(TAG, "### recv time= " + time);
									//System.out.println("### recv time= " + time);
								}*/
							}
						} else {
							if(canStartFlag) {
								if(imageDataStart) {
									jpegByteBuf[jpegBufUsed++] = b0;
									tmpJpgBufUsed = jpegBufUsed;
								}else {
									playMpegBuf[mpegDataLength++] = b0;
								}
							}
						}
					} 
					indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
				}
			} while(!stopPlay);
			startFlag = false;
			if(rgbDataBuf == null && !stopPlay) {
				int[] headInfo = UdtTools.initXvidHeader(playMpegBuf, length);//length的长度即为out_buffer的长度，所以length要足够长。
				int imageWidth = headInfo[0];
				int imageHeight = headInfo[1];
				usedBytes = headInfo[2];
				unusedBytes = (mpegDataLength - usedBytes);
				if(unusedBytes<=0) {
					unusedBytes = 0;
				}
				System.arraycopy(playMpegBuf, usedBytes, playMpegBuf, 0, unusedBytes);
				mpegDataLength = unusedBytes;
				if(imageWidth<=0) {
					Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
					continue;
				}
				System.gc();
				rgbDataBuf = new byte[imageWidth * imageHeight * 4];
				Log.d(TAG, "### W = " + imageWidth + " H = " + imageHeight + " used_bytes = " + usedBytes + " rgb length = " + rgbDataBuf.length);
				synchronized (lock) {
					/*if((imageWidth * imageHeight) == (352 * 288)) {
						if(qvga == null) {
							qvga = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
						}
						this.video = qvga;
						if(BuildConfig.DEBUG) {
							System.out.println("### qvga " + imageWidth + " x " + imageHeight);
						}
					} else if((imageWidth * imageHeight) == (640 * 480)) {
						if(vga == null) {
							vga = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
						}
						this.video = vga;
						if(BuildConfig.DEBUG) {
							System.out.println("### vga " + imageWidth + " x " + imageHeight);
						}
					} else if((imageWidth * imageHeight) == (1280 * 720)) {
						if(qp == null) {
							qp = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
						}
						this.video = qp;
						if(BuildConfig.DEBUG) {
							System.out.println("### 720p " + imageWidth + " x " + imageHeight);
						}
					}*/
					if(video == null) {
						video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
					}else { 
						Bitmap tmp = Bitmap.createScaledBitmap(video, imageWidth, imageHeight, false);
						if(!video.isRecycled()) {
							video.recycle();
							video = null;
						}
						video = tmp;
					}/**/
					myVideoView.setImage(video);
				}
				if(flag) {
					flag = false;
					myVideoView.updateResulation(imageWidth);
				}
				mpegPakages = 2;
			}else if(!stopPlay){
				usedBytes = UdtTools.xvidDecorer(playMpegBuf, mpegDataLength, rgbDataBuf, BuildConfig.DEBUG?1:0); //flag == 1 printf decode time
				//System.out.println("### decode ========= " + (SystemClock.currentThreadTimeMillis() - curr));
				//UdtTools.xvidDecorer(mpegBuf, mpegDataLength, rgbDataBuf, BuildConfig.DEBUG?1:0); //flag == 1 printf decode time
				if(usedBytes>999999) {//(XDIM * 100000) + used_bytes;
					int newImageWidth = usedBytes / 1000000;
					int useBytes = usedBytes%1000000;
					usedBytes = useBytes;
					int newImageHeight = caculateImageHeight(newImageWidth);
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### return value " + usedBytes + " useBytes = " + useBytes + " newWidth = " + newImageWidth + " newHeight = "+ newImageHeight);
					}
					rgbDataBuf = new byte[newImageWidth * newImageHeight * 4];
					synchronized (lock) {
						/*int wipe  = newImageWidth * newImageHeight;
						if(wipe == (352 * 288)) {
							if(qvga == null) {
								qvga = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
							}
							this.video = qvga;
							if(BuildConfig.DEBUG) {
								System.out.println("### qvga " + newImageWidth + " x " + newImageHeight);
							}
						} else if(wipe == (640 * 480)) {
							if(vga == null) {
								vga = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
							}
							this.video = vga;
							if(BuildConfig.DEBUG) {
								System.out.println("### vga " + newImageWidth + " x " + newImageHeight);
							}
						} else if(wipe == (1280 * 720)) {
							if(qp == null) {
								qp = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
							}
							this.video = qp;
							if(BuildConfig.DEBUG) {
								System.out.println("### 720p " + newImageWidth + " x " + newImageHeight);
							}
						}*/
						if(video == null) {
							video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
						}else {
							//video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
							Bitmap tmp = Bitmap.createScaledBitmap(video, newImageWidth, newImageHeight, false);
							if(!video.isRecycled()) {
								video.recycle();
								video = null;
							}
							video = tmp;
						}/**/
						myVideoView.setImage(video);
					}
					myVideoView.updateRect();
					myVideoView.updateResulation(newImageWidth);
					unusedBytes = (mpegDataLength - useBytes);
					if(unusedBytes<=0) {
						unusedBytes = 0;
					}
					System.arraycopy(playMpegBuf, useBytes, playMpegBuf, 0, unusedBytes);
					mpegDataLength = unusedBytes;
				} else {
					MpegImage mpegImage = new MpegImage(rgbDataBuf, time,0, 0);
					queue.addMpegImage(mpegImage);
					unusedBytes = (mpegDataLength - usedBytes);
					if(unusedBytes<=0) {
						unusedBytes = 0;
					}
					System.arraycopy(playMpegBuf, usedBytes, playMpegBuf, 0, unusedBytes);
					//System.out.println("### move ========= " + (SystemClock.currentThreadTimeMillis() - curr));
					mpegDataLength = unusedBytes;
				}
				//System.out.println("unusedBytes = " + unusedBytes + " mpegDataLength = " + mpegDataLength + " usedBytes= " + usedBytes);
				/*unusedBytes = (mpegDataLength - usedBytes);
				if(usedBytes + unusedBytes >=length) {
					unusedBytes = length - usedBytes;
				}*/
				if(unusedBytes<=0) {
					unusedBytes = 0;
				}
				System.arraycopy(playMpegBuf, usedBytes, playMpegBuf, 0, unusedBytes);
				mpegDataLength = unusedBytes;
			}
			//mpegDataLength = 0;
		}
		showMpeg.setInerrupt();
		onStop();
	}
	
	private void onStop() {
		System.gc();
		stopPlay = true;
		if(video != null && !video.isRecycled()) {
			video.recycle();
			video = null;
		}
		myVideoView.setImage(null);
		UdtTools.freeDecorer();
		this.rgbDataBuf = null;
		jpegByteBuf = null;
		playMpegBuf = null;
		System.gc();
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
					//if(!popuJpeg(oldTime)){
						MpegImage mpegImage = queue.getMpegImage();
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### show mpegImage = " + mpegImage );
						}
						if(mpegImage != null) {
							byte[] tmpRgb = mpegImage.rgb;
							ByteBuffer sh = ByteBuffer.wrap(tmpRgb);
							//Log.d(TAG, "timeStr=" + timeStr + " frameCount =" + frameCount);
							if(video != null) {
								try {
									video.copyPixelsFromBuffer(sh);
								} catch (Exception e) {
									Log.e(TAG, "### copyPixelsFromBuffer exception!");
								}
								//frameCount = myVideoView.getFrameCount();
								//frameCount++;
							}
						}
						if(listener != null) {
							listener.invalide( timeStr);
						}
					//}
				} else {
					synchronized (lock) {
						/*if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### queue no image  ---->");
						}*/
						try {
							lock.wait(10);
						} catch (InterruptedException e) {
							stopPlay = true;
							Log.e(TAG, e.getLocalizedMessage());
							onStop();
							break;
						}
					}  
				}
			}
			if(rgbDataBuf != null) {
				rgbDataBuf = null;
				System.gc();
			}
			onStop();
		}
		
		private void onStop() {
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
				Log.d(TAG, "### show mpeg image thread exit!");
			}
		}
		
		public boolean popuJpeg(String oldTime) {
			JpegImage image = queue.getFirstImage();
			if(null == image) {
				return false;
			}else {
				if(oldTime.compareTo(image.time) ==0) {
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### popu jpeg " + image.time);
					}
					queue.removeImage();
					video = image.bitmap;
					myVideoView.setImage(video);
					//frameCount = myVideoView.getFrameCount();
					//frameCount++;
					if(listener != null) {
						listener.invalide(timeStr);
					}
					return true;
				} else if(oldTime.compareTo(image.time) > 0) {
					queue.clear();
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### queue clear");
					}
				}
			}
			return false;
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
	
}
