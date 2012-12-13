package com.iped.ipcam.engine;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class PlayMpegThread extends DecoderFactory implements Runnable, OnPutIndexListener {

	private VideoQueue queue = null;
	
	private int startFlagCount = 1;
	
	private Object lock = new Object();
	
	byte[] rgbDataBuf = null;
	int length = 1 * 512 * 1024;
	//int bufNeedLength = length - 5;
	byte[] mpegBuf = new byte[length];
	int usedBytes = length;
	int unusedBytes = 0;
	int mpegDataLength = 0;
	boolean startFlag = false;
	
	int headFlagCount = 0;
	
	int jpegByteBufLength = 2 * 200 * 1024;
	
	byte[] jpegByteBuf = new byte[jpegByteBufLength]; 
	
	int jpegBufUsed = 0;
	
	int tmpJpgBufUsed = 0;
	
	String time = "";
	
	private byte[] nalBuf = null;
	
	private boolean imageDataStart = false;
	
	///queue = new VideoQueue();
	
	int mpegPakages = 3;
	
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
	
	public PlayMpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount ) {
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		queue = new VideoQueue();
		this.myVideoView = myVideoView;
		myVideoView.setOnPutIndexListener(this);
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
						if(startFlagCount++ %mpegPakages == 0){
							startFlagCount = 1;
							break;
						}
						if(imageDataStart) {
							imageDataStart = false;
							Bitmap v = BitmapFactory.decodeByteArray(jpegByteBuf, 0, tmpJpgBufUsed);
							if(v != null) {
								queue.addJpegImage(new JpegImage(v, jpegTimeTmp));
								if(BuildConfig.DEBUG && DEBUG) {
									Log.d(TAG, "### add jpeg  time=" + jpegTimeTmp);
								}
							}
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
							if(imageDataStart) {
								jpegByteBuf[jpegBufUsed++] = b0;
								tmpJpgBufUsed = jpegBufUsed;
							}else {
								mpegBuf[mpegDataLength++] = b0;
							}
						}
					} 
					indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
				}
			} while(!stopPlay);
			startFlag = false;
			if(rgbDataBuf == null) {
				mpegPakages = 2;
				int[] headInfo = UdtTools.initXvidHeader(mpegBuf, length);//length的长度即为out_buffer的长度，所以length要足够长。
				int imageWidth = headInfo[0];
				int imageHeight = headInfo[1];
				usedBytes = headInfo[2];
				unusedBytes = (mpegDataLength - usedBytes);
				System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
				mpegDataLength = unusedBytes;
				Log.d(TAG, "### imageWidht = " + imageWidth + " imageWidht = " + imageHeight + " used_bytes = " + usedBytes);
				rgbDataBuf = new byte[imageWidth * imageHeight * 4];
				video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
				myVideoView.setImage(video);
			} else if(!stopPlay){
				usedBytes = UdtTools.xvidDecorer(mpegBuf, mpegDataLength, rgbDataBuf);
				MpegImage mpegImage = new MpegImage(rgbDataBuf, time);
				queue.addMpegImage(mpegImage);
				unusedBytes = (mpegDataLength - usedBytes);
				System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
				mpegDataLength = unusedBytes;
			}
		}
		UdtTools.freeDecorer();
		rgbDataBuf = null;
		System.gc();
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
	}
	
	public void decodeYUV420SP(int[] rgba, byte[] yuv420sp, int width,  int height) {
		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
		    int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
		    for (int i = 0; i < width; i++, yp++) {
		        int y = (0xff & ((int) yuv420sp[yp])) - 16;
		        if (y < 0)
		            y = 0;
		        if ((i & 1) == 0) {
		        	System.out.println(uvp +  " ");
		            v = (0xff & yuv420sp[uvp++]) - 128;
		            u = (0xff & yuv420sp[uvp++]) - 128;
		        }

		        int y1192 = 1192 * y;
		        int r = (y1192 + 1634 * v);
		        int g = (y1192 - 833 * v - 400 * u);
		        int b = (y1192 + 2066 * u);

		        if (r < 0)
		            r = 0;
		        else if (r > 262143)
		            r = 262143;
		        if (g < 0)
		            g = 0;
		        else if (g > 262143)
		            g = 262143;
		        if (b < 0)
		            b = 0;
		        else if (b > 262143)
		            b = 262143;

		        // rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
		        // 0xff00) | ((b >> 10) & 0xff);
		        // rgba, divide 2^10 ( >> 10)
		        rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
		                | ((b >> 2) | 0xff00);
		    }
		}
	}

	
	public void decodeYUV(int[] out, byte[] fg, int width, int height) {
		int sz = width * height;
	    int i, j;
	    int Y, Cr = 0, Cb = 0;
	    for (j = 0; j < height; j++) {
	        int pixPtr = j * width;
	        final int jDiv2 = j >> 1;
	        for (i = 0; i < width; i++) {
	            Y = fg[pixPtr];
	            if (Y < 0)
	                Y += 255;
	            if ((i & 0x1) != 1) {
	                final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
	                Cb = fg[cOff];
	                if (Cb < 0)
	                    Cb += 127;
	                else
	                    Cb -= 128;
	                Cr = fg[cOff + 1];
	                if (Cr < 0)
	                    Cr += 127;
	                else
	                    Cr -= 128;
	            }
	            int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
	            if (R < 0)
	                R = 0;
	            else if (R > 255)
	                R = 255;
	            int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
	                    + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	            if (G < 0)
	                G = 0;
	            else if (G > 255)
	                G = 255;
	            int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	            if (B < 0)
	                B = 0;
	            else if (B > 255)
	                B = 255;
	            out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
	        }
	    }
	}
	
}
