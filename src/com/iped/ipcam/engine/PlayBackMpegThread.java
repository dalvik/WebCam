package com.iped.ipcam.engine;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.MyVideoView.OnPutIndexListener;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.pojo.MpegImage;
import com.iped.ipcam.pojo.PlayBackMpegInfo;
import com.iped.ipcam.utils.AudioUtil;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.PlayBackMpegQueue;
import com.iped.ipcam.utils.VideoQueue;

public class PlayBackMpegThread extends DecoderFactory implements Runnable, OnPutIndexListener  {

	private byte[] nalBuf = null;;
	
	private String timeStr;
	
	private Bitmap video;
	
	private MyVideoView myVideoView;
	
	private boolean stopPlay = false;
	
	private String TAG = "PlayBackThread";
	
	private boolean DEBUG = true;
	
	private int indexForPut = 0;
	
	private int indexForGet = 0;
	
	private final static int NALBUFLENGTH = MyVideoView.NALBUFLENGTH;
	
	private OnMpegPlayListener listener;
	
	// default  true not recv over or uncomplete; recv over false
	private boolean initTableInfo = true; //start recv playback table
	
	private int initTableHeadCount = 0;//用以计算回放表的八个字节的长度信息
	
	private int maxHeadLength = 32 * 1024;
	
	private int targetTableLength = 0;
	
	private byte[] headerInfoByte = new byte[maxHeadLength];
	
	private Handler handler;
	
	private int t1Length =  0;
	
	private int t2Length =  0;
	
	private int insideHeadCount = 0;// 索引 接收的18个时间字符串
	
	private boolean insideHeaderFlag = false;// 内部头开始标记，截取时间戳
	
	//收到0001c后 标记startFlag = true,为判断是mpeg4还是jpeg做准备 判断出结果后 值回false
	private boolean startFlag = false;
	
	private byte[] timeByte = new byte[100];
	
	private VideoQueue queue = null;
	
	private PlayBackMpegQueue rawDataQueue;
	
	private Object lock = new Object();
	
	private int audioBufferUsedLength;
	
	private int MAX_FRAME = 256;

	private int TOTAL_FRAME_SIZE = 50 * MAX_FRAME;
	
	private boolean hasAudioData = false;
	
	private byte[] amrBuffer = new byte[MyVideoView.PLAYBACK_AUDIOBUFFERSIZE * 10];
	
	private int audioTmpBufferUsed;
	
	private byte[] audioData = new byte[TOTAL_FRAME_SIZE];
	
	private int rate = 1;
	
	private int length = 1 * 400 * 1024;
	
	//private byte[] jpegByteBuf = null; 
	
	private byte[] mpegBuf = null;
	
	private int mpegDataIndex = 0;
	
	private int dataType = -1; // -1 default  0 mpeg  1 jpeg  2 audio
	
	private byte[] mpegRawDataTmp = null;
	
	private int mpegRawDataLen = 0;
	
	private boolean hasMpegRawData = false; //是否有mpeg raw data需要处理
	
	private Object rawDataLock = new Object();
	
	private Object lock2 = new Object();
	
	// rgb data
	private byte[] rgbDataBuf = null;
	
	private boolean canStartFlag = false; //开始解码标记
	
	private int startFlagCount = 1;
	
	//private boolean mpegStartFlag = false; // mpeg4开始标记
	
	public static final int defineImageBufferLength = 1;
	
	public PlayBackMpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount, Handler handler){
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.myVideoView = myVideoView;
		this.handler = handler;
		myVideoView.setOnPutIndexListener(this);
		queue = new VideoQueue();
		rawDataQueue = new PlayBackMpegQueue();
		//jpegByteBuf = new byte[jpegByteBufLength]; 
		mpegBuf = new byte[length];
	}
	
	
	@Override
	public void run() {
		stopPlay = false;
		initTableHeadCount = 0;
		Thread playAudioThread = new Thread(new PalyBackAudio());
		playAudioThread.start();
		new Thread(new DecodeMpegThread()).start();
		do{
			if((indexForGet+5)%NALBUFLENGTH == indexForPut){
				synchronized (mpegBuf) {
					try {
						mpegBuf.wait(50);
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
				if(initTableInfo) {
					headerInfoByte[initTableHeadCount++] = b0;
					if(initTableHeadCount==8) {
						t1Length =  ByteUtil.byteToInt4(headerInfoByte,0);
						t2Length =  ByteUtil.byteToInt4(headerInfoByte,4);
						if(t2Length<=0 || t2Length>=maxHeadLength) {
							initTableInfo = false;
						}
						targetTableLength = 8 + t1Length + t2Length;
					}
					if(initTableHeadCount == targetTableLength) {//索引表接收完毕，更新拖动条信息
						initTableInfo = false;
						byte[] table2 = new byte[t2Length];
						System.arraycopy(headerInfoByte, 8 + t1Length, table2, 0, t2Length);
						Message message = handler.obtainMessage();
						Bundle bundle = new Bundle();
						bundle.putByteArray("TABLE2", table2);
						message.setData(bundle);
						message.what = PlayBackConstants.INIT_SEEK_BAR;
						handler.sendMessage(message);
					}
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
					if(dataType == 0) {
						//Log.e(TAG, "### mpeg length = " + mpegDataIndex + "  timeStr = " + timeStr);
						byte[] tmp = new byte[mpegDataIndex];
						System.arraycopy(mpegBuf, 0, tmp, 0, mpegDataIndex);
						PlayBackMpegInfo pbmi = new PlayBackMpegInfo(tmp, mpegDataIndex, timeStr);
						do {
							if(rawDataQueue.getMpegLength()<=defineImageBufferLength) {
								rawDataQueue.addMpeg(pbmi);
								mpegDataIndex = 0;
								break;
							}else {
								synchronized (mpegBuf) {
									try {
										mpegBuf.wait(10);
									} catch (InterruptedException e) {
										e.printStackTrace();
										Log.d(TAG, "### PlayBackMpegThread wait for mpeg queue space interrupt.");
									}
								}  
							}
						}while(!stopPlay);
					}
					if(dataType == 3) {
						audioTmpBufferUsed = audioBufferUsedLength;
						audioBufferUsedLength = 0;
						synchronized (amrBuffer) {
							System.arraycopy(audioData, 0, amrBuffer, 0, audioTmpBufferUsed);
							hasAudioData = true;
							amrBuffer.notify();
						}
					}
					//dataType = -1;
					canStartFlag = true;
					startFlag = true;//可以开始分离音视频数据了
					if(!initTableInfo) { // 完成接收索引表
						insideHeaderFlag = true;//可以截取 time info
					}
					insideHeadCount = 0;//  收到0001c后将initTableInfo值0
					indexForGet+=4;
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 11 ) { // 0001b
					dataType = 3;//audio
					indexForGet+=4;
				} else {
					if(insideHeaderFlag) {//首先接收时间戳
						timeByte[insideHeadCount] = b0;
						insideHeadCount++;
						if(insideHeadCount >= 18) { 
							insideHeaderFlag = false;	
							timeStr = new String(timeByte, 0, 14);
						}
					} else {//根据标记分离视频（mpeg4、jpeg）和音频数据
						if(startFlag) {
							startFlag = false;
							if(b0 == 0 &&  b1 == 0 &&  b2 == 0 && b3 == 0 && b4 == 0) {//mpeg4
								dataType = 0;//mpeg4
								indexForGet+=8;//跳过9个0
							} else { 
								dataType = 1;//jpeg
								if(BuildConfig.DEBUG && DEBUG) {
									Log.e(TAG, "### jpeg data start ------");
								}
							}
						} else {
							if(dataType == 0) {//mpeg4
								if(mpegDataIndex>=length) {
									Log.e(TAG, "### mpegDataIndex = " + mpegDataIndex);
								}
								mpegBuf[mpegDataIndex++] = b0;
							} else if(dataType == 3){ //A audio 
								if(audioBufferUsedLength>= TOTAL_FRAME_SIZE) {
									System.out.println("audioBufferUsedLength = " + audioBufferUsedLength);
								}
								audioData[audioBufferUsedLength++] = b0;
							} else {// jpeg do nothing
							}
						}
					}
				}
				indexForGet = (indexForGet + 1)%NALBUFLENGTH; 
			}
		} while(!stopPlay);
		if(playAudioThread != null && !playAudioThread.isInterrupted()) {
			playAudioThread.interrupt();
		}
		if(!Thread.currentThread().isInterrupted()) {
			Thread.currentThread().interrupt();
		}
	}

	private class DecodeMpegThread implements Runnable {
	
		private int playBackMpegDataLength = 400 * 1024;
		
		private int playBackMpegDataIndex = 0;
		
		private byte[] playBackMpegData = null;
		
		private int usedBytes = 0;
		
		private int remainBytes = 0;
		
		private String TAG = "DecodePlayBackMpeg";
		
		private boolean flag = true;
		
		private int appendByteIndex = 0;
		
		public DecodeMpegThread()  {
			playBackMpegData = new byte[playBackMpegDataLength];
		}
		
		@Override
		public void run() {
			int res = UdtTools.initXvidDecorer();
			int packetNum = 1;
			//int dstPos = 0;
			if(res != 0) {
				Log.d(TAG, "xvid init decoder error " + res);
				return ;
			}
			Thread displayMpegThread = new Thread(new DisplayMpegThread());
			displayMpegThread.start();
			while(!stopPlay) {
				if(rgbDataBuf == null) {//初始化头信息
					while(true) {
						PlayBackMpegInfo pbmi = rawDataQueue.getMpeg();
						if(pbmi != null) {
							byte[] tmp = pbmi.getData();
							int len = pbmi.getLen();
							Log.d(TAG, "### get raw data = " +tmp.length + "  data len = " + len );
							System.arraycopy(tmp, 0, playBackMpegData, playBackMpegDataIndex, len);
							playBackMpegDataIndex+=len;
							packetNum++;
						}
						if(packetNum%4 == 0) {
							int[] headInfo = UdtTools.initXvidHeader(playBackMpegData, playBackMpegDataIndex);//length的长度即为out_buffer的长度，所以length要足够长。
							int imageWidth = headInfo[0];
							int imageHeight = headInfo[1];
							usedBytes = headInfo[2];
							remainBytes = (playBackMpegDataIndex - usedBytes);
							Log.d(TAG, "### decode mpeg4 res = " + imageWidth + "  " + imageHeight + "  " + remainBytes);
							if(remainBytes<=0) {
								remainBytes = 0;
							}
							System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, remainBytes);//从用过的字节位置起，向左移动 remainBytes 个字节长度
							playBackMpegDataIndex = remainBytes;//剩余的mpeg raw data
							if(imageWidth<=0) {
								Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
								continue;
							}
							System.gc();
							rgbDataBuf = new byte[imageWidth * imageHeight * 4];
							Log.d(TAG, "### W = " + imageWidth + " H = " + imageHeight + " used_bytes = " + usedBytes + " rgb length = " + rgbDataBuf.length);
							synchronized (lock) {
								if(video == null) {
									video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
								}else { 
									Bitmap tmp = Bitmap.createScaledBitmap(video, imageWidth, imageHeight, false);
									if(!video.isRecycled()) {
										video.recycle();
										video = null;
									}
									video = tmp;
								}
								myVideoView.setImage(video);
							}
							break;
						}
					}
				}else {//解码
					PlayBackMpegInfo pbmi = rawDataQueue.getMpeg();
					if(pbmi != null) {
						byte[] tmp = pbmi.getData();
						int len = pbmi.getLen();
						if((playBackMpegDataIndex + len) <= playBackMpegDataLength) {
							System.arraycopy(tmp, 0, playBackMpegData, playBackMpegDataIndex, len);
							playBackMpegDataIndex += len;
						}
						usedBytes = UdtTools.xvidDecorer(playBackMpegData, playBackMpegDataIndex, rgbDataBuf, BuildConfig.DEBUG?1:0);
//						System.out.println("playBackMpegDataIndex = " +playBackMpegDataIndex + "appen len=" + len +" usedBytes = " + usedBytes);
						if(usedBytes>999999) {
							int newImageWidth = usedBytes / 1000000;
							int useBytes = usedBytes%1000000;
							usedBytes = useBytes;
							int newImageHeight = caculateImageHeight(newImageWidth);
							if(BuildConfig.DEBUG && DEBUG) {
								Log.d(TAG, "### return value " + usedBytes + " useBytes = " + useBytes + " newWidth = " + newImageWidth + " newHeight = "+ newImageHeight);
							}
							rgbDataBuf = new byte[newImageWidth * newImageHeight * 4];
							synchronized (lock) {
								if(video == null) {
									video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
								}else {
									//video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
									Bitmap tmp2 = Bitmap.createScaledBitmap(video, newImageWidth, newImageHeight, false);
									if(!video.isRecycled()) {
										video.recycle();
										video = null;
									}
									video = tmp2;
								}
								myVideoView.setImage(video);
							}
							myVideoView.updateRect();
							myVideoView.updateResulation(newImageWidth);
							remainBytes = (playBackMpegDataIndex - useBytes);
							if(remainBytes<=0) {
								remainBytes = 0;
							}
							System.arraycopy(playBackMpegData, useBytes, playBackMpegData, 0, remainBytes);
							playBackMpegDataLength = remainBytes;
						} else {
							MpegImage mpegImage = new MpegImage(rgbDataBuf, timeStr);
							queue.addMpegImage(mpegImage);
							remainBytes = (playBackMpegDataIndex - usedBytes);
							if(remainBytes<=0) {
								remainBytes = 0;
							}
							System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, remainBytes);
							playBackMpegDataIndex = remainBytes;
						}
					}
				}
			}
			if(displayMpegThread != null && !displayMpegThread.isInterrupted()) {
				displayMpegThread.interrupt();
			}
			if(displayMpegThread != null && displayMpegThread.isInterrupted()) {
				displayMpegThread.interrupt();
			}
			onStop();
		}
		
		private void onStop() {
			stopPlay = true;
			myVideoView.setImage(null);
			if(video != null && !video.isRecycled()) {
				video.recycle();
				video = null;
			}
			UdtTools.freeDecorer();
			System.gc();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### play mpeg thread exit ....");
			}
		}
	}
	
	private class DisplayMpegThread implements Runnable {
		
		public DisplayMpegThread() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				MpegImage mpegImage = queue.getMpegImage();
				if(null != mpegImage) {
					byte[] tmpRgb = mpegImage.rgb;
					ByteBuffer sh = ByteBuffer.wrap(tmpRgb);
					if(video != null) {
						try {
							video.copyPixelsFromBuffer(sh);
						} catch (Exception e) {
							Log.e(TAG, "### copyPixelsFromBuffer exception!");
						}
					}
					if(listener != null) {
						listener.invalide( timeStr);
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
							Log.d(TAG, "### DisplayMpegThread interrupt ---->");
							e.printStackTrace();
						}
					}  
				}
			}
			Log.d(TAG, "### DisplayMpegThread exit ---->");
		}
	}

	private class PalyBackAudio implements Runnable {
		
		private int pcmBufferLength = TOTAL_FRAME_SIZE * 10; //PLAYBACK_AUDIOBUFFERSIZE * Command.CHANEL * 50;
		
		private byte[] pcmArr = new byte[pcmBufferLength];
		

		private AudioTrack m_out_trk = null;
		
		public PalyBackAudio(){
			int init = UdtTools.initAmrDecoder();
			Log.d(TAG, "amr deocder init " + init);
			m_out_trk = AudioUtil.getAudioTrackInstance();
			m_out_trk.play();
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(hasAudioData) {
					UdtTools.amrDecoder(amrBuffer, audioTmpBufferUsed, pcmArr, 0, Command.CHANEL);
					m_out_trk.write(pcmArr, 0, pcmBufferLength);
					hasAudioData = false;
				}else {
					synchronized (amrBuffer) {
						try {
							amrBuffer.wait();
						} catch (InterruptedException e) {
							stopPlay = true;
							release();
							e.printStackTrace();
						}
					}
					}
			}
			release();
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "PalyBackAudio Thread exit  ----> ");
			}
		}
		
		private void release() {
			AudioUtil.exitAudioTrack();
			UdtTools.exitAmrDecoder();
		}
	}

	@Override
	public int getIndexForGet() {
		return indexForGet;
	}
	
	@Override
	public void onStop(boolean stopPlay) {
		this.stopPlay = stopPlay;
		Log.d(TAG, "### play back set stop");
	}
	
	@Override
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
		this.listener = listener;		
	}
	
	@Override
	public void updatePutIndex(int putIndex) {
		this.indexForPut = putIndex;	
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
