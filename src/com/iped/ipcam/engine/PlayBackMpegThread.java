package com.iped.ipcam.engine;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.MyVideoView.OnPutIndexListener;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.MpegImage;
import com.iped.ipcam.pojo.PlayBackMpegInfo;
import com.iped.ipcam.utils.AudioUtil;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.PlayBackMpegQueue;
import com.iped.ipcam.utils.VideoQueue;

public class PlayBackMpegThread extends DecoderFactory implements OnPutIndexListener  {

	private final static int MPEG_DATA_TYPE = 0;
	
	private final static int JPEG_DATA_TYPE = 1;
	
	private final static int AUDIO_DATA_TYPE = 2;
	
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
	
	private int length = 1 * 400 * 1024;
	
	private int jpegBufLen = 1 * 350 * 1024;
	
	//private byte[] jpegByteBuf = null; 
	
	private byte[] mpegBuf = null;
	
	private byte[] jpegBuf = null;
	
	private int mpegDataIndex = 0;
	
	private int jpegDataIndex = 0;
	
	private int dataType = -1; // -1 default  0 mpeg  1 jpeg  2 audio
	
	//private boolean mpegStartFlag = false; // mpeg4开始标记
	
	public static final int defineImageBufferLength = 5;
	
	private static final int defintImageQueueLength = 3;
	
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
					initPlayBackTable(b0);
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
					if(dataType == MPEG_DATA_TYPE) {
						moveMpegRawDataToQueue();
					}
					if(dataType == JPEG_DATA_TYPE) {
						moveJpegRawDataToQueue();
					}
					if(dataType == AUDIO_DATA_TYPE) {
						moveAudioAmrDataToDecode();
					}
					startFlag = true;//可以开始分离音视频数据了
					if(!initTableInfo) { // 完成接收索引表
						insideHeaderFlag = true;//可以截取 time info
					}
					insideHeadCount = 0;//  收到0001c后将initTableInfo值0
					indexForGet+=4;
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 11 ) { // 0001b
					if(dataType == MPEG_DATA_TYPE) {
						addMpegData();
					}
					if(dataType == JPEG_DATA_TYPE) {
						addJpegData();
					}
					dataType = 2;//audio
					indexForGet+=4;
				} else {
					if(insideHeaderFlag) {//首先接收时间戳
						timeByte[insideHeadCount] = b0;
						insideHeadCount++;
						if(insideHeadCount >= 18) { 
							insideHeaderFlag = false;	
							timeStr = new String(timeByte, 0, 18);
						}
					} else {//根据标记分离视频（mpeg4、jpeg）和音频数据
						if(startFlag) {
							startFlag = false;
							if(b0 == 0 &&  b1 == 0 &&  b2 == 0 && b3 == 0 && b4 == 0) {//mpeg4
								dataType = MPEG_DATA_TYPE;//mpeg4
								indexForGet+=8;//跳过9个0
							} else { 
								dataType = JPEG_DATA_TYPE;//jpeg
								if(jpegBuf == null) {
									jpegBuf = new byte[jpegBufLen];
								}
								jpegBuf[jpegDataIndex++] = -1;
							}
						} else {
							if(dataType == MPEG_DATA_TYPE) {//mpeg4
								mpegBuf[mpegDataIndex++] = b0;
							} else if(dataType == AUDIO_DATA_TYPE){ //A audio 
								audioData[audioBufferUsedLength++] = b0;
							} else {// jpeg do nothing
								jpegBuf[jpegDataIndex++] = b0;
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
		
		this.jpegBuf = null;
		//this.mpegBuf = null;
		System.gc();
	}

	private void initPlayBackTable(byte b0) {
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
	}
	
	private void addMpegData() {
		byte[] tmp = new byte[mpegDataIndex];
		System.arraycopy(mpegBuf, 0, tmp, 0, mpegDataIndex);
		PlayBackMpegInfo pbmi = new PlayBackMpegInfo(tmp, mpegDataIndex, timeStr, dataType);
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
	
	private void addJpegData() {
		byte[] tmp = new byte[jpegDataIndex];
		System.arraycopy(jpegBuf, 0, tmp, 0, jpegDataIndex);
		PlayBackMpegInfo pbmi = new PlayBackMpegInfo(tmp, jpegDataIndex, timeStr, dataType);
		do {
			if(rawDataQueue.getMpegLength()<=defineImageBufferLength) {
				rawDataQueue.addMpeg(pbmi);
				jpegDataIndex = 0;
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
	
	private void moveMpegRawDataToQueue() {
		byte[] tmp = new byte[mpegDataIndex];
		System.arraycopy(mpegBuf, 0, tmp, 0, mpegDataIndex);
		PlayBackMpegInfo pbmi = new PlayBackMpegInfo(tmp, mpegDataIndex, timeStr, dataType);
		do {
			if(rawDataQueue.getMpegLength()<=defineImageBufferLength) {
				rawDataQueue.addMpeg(pbmi);
				mpegDataIndex = 0;
				break;
			}else {
				synchronized (mpegBuf) {
					try {
						mpegBuf.wait(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Log.d(TAG, "### PlayBackMpegThread wait for mpeg queue space interrupt.");
					}
				}  
			}
		}while(!stopPlay);
	}
	
	private void moveJpegRawDataToQueue() {
		byte[] tmp = new byte[jpegDataIndex];
		System.arraycopy(jpegBuf, 0, tmp, 0, jpegDataIndex);
		PlayBackMpegInfo pbmi = new PlayBackMpegInfo(tmp, jpegDataIndex, timeStr, dataType);
		do {
			if(rawDataQueue.getMpegLength()<=defineImageBufferLength) {
				rawDataQueue.addMpeg(pbmi);
				jpegDataIndex = 0;
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
	
	private void moveAudioAmrDataToDecode() {
		audioTmpBufferUsed = audioBufferUsedLength;
		audioBufferUsedLength = 0;
		synchronized (amrBuffer) {
			System.arraycopy(audioData, 0, amrBuffer, 0, audioTmpBufferUsed);
			hasAudioData = true;
			amrBuffer.notify();
		}
	}
	
	private class DecodeMpegThread implements Runnable {
	
		private int playBackMpegDataLength = 400 * 1024;
		
		private int playBackMpegDataIndex = 0;
		
		private byte[] playBackMpegData = null;
		
		private int remainBytes = 0;
		
		private String TAG = "DecodePlayBackMpeg";
		
		private boolean flag = true;

		private boolean initXVIDHead = false;
		
		// rgb data
		private byte[] rgbDataBuf = null;
		
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
					while(!initXVIDHead) {
						PlayBackMpegInfo pbmi = rawDataQueue.getMpeg();
						if(pbmi != null) {
							if(pbmi.type == JPEG_DATA_TYPE) {
								do {
									if(queue.getMpegLength()>= defintImageQueueLength) {
										synchronized (mpegBuf) {
											try {
												mpegBuf.wait(5);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
										}
									} else  {
										MpegImage mpegImage = new MpegImage(pbmi.data, timeStr, pbmi.type, pbmi.len);
										queue.addMpegImage(mpegImage);
										break;
									}
								}while(true);
							}else {
								byte[] tmp = pbmi.data;
								int len = pbmi.len;
								Log.d(TAG, "### get raw data = " +tmp.length + "  data len = " + len );
								System.arraycopy(tmp, 0, playBackMpegData, playBackMpegDataIndex, len);
								playBackMpegDataIndex+=len;
								packetNum++;
							}
						}
						if(packetNum%4 == 0) {
							fillXVIDHeader();
						}
					}
				}else {//解码
					if(flag) {
						flag = false;
						int usedBytes = UdtTools.xvidDecorer(playBackMpegData, playBackMpegDataIndex, rgbDataBuf, BuildConfig.DEBUG?1:0);
						do {
							if(queue.getMpegLength()>= defintImageQueueLength) {
								synchronized (mpegBuf) {
									try {
										mpegBuf.wait(5);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							} else  {
								MpegImage mpegImage = new MpegImage(rgbDataBuf, timeStr, MPEG_DATA_TYPE, 0);
								queue.addMpegImage(mpegImage);
								break;
							}
						}while(true);
						remainBytes = (playBackMpegDataIndex - usedBytes);
						if(remainBytes<=0) {
							remainBytes = 0;
						}
						System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, remainBytes);
						playBackMpegDataIndex = remainBytes;
					}else {
						decode();
					}
				}
			}
			if(displayMpegThread != null && !displayMpegThread.isInterrupted()) {
				displayMpegThread.interrupt();
			}
			onStop();
		}
		
		private void fillXVIDHeader() {
			int[] headInfo = UdtTools.initXvidHeader(playBackMpegData, playBackMpegDataIndex);//length的长度即为out_buffer的长度，所以length要足够长。
			int imageWidth = headInfo[0];
			int imageHeight = headInfo[1];
			int usedBytes = headInfo[2];
			remainBytes = (playBackMpegDataIndex - usedBytes);
			Log.d(TAG, "### decode mpeg4 res = " + imageWidth + "  " + imageHeight + "  " + remainBytes);
			if(remainBytes<=0) {
				remainBytes = 0;
			}
			System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, remainBytes);//从用过的字节位置起，向左移动 remainBytes 个字节长度
			playBackMpegDataIndex = remainBytes;//剩余的mpeg raw data
			if(imageWidth<=0) {
				Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
				return;
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
			initXVIDHead = true;
		}
		
		private void decode() {
			PlayBackMpegInfo pbmi = rawDataQueue.getMpeg();
			if(pbmi != null) {
				if(pbmi.type == MPEG_DATA_TYPE) {
					byte[] tmp = pbmi.data;
					int len = pbmi.len;
					if((playBackMpegDataIndex + len) <= playBackMpegDataLength) {
						System.arraycopy(tmp, 0, playBackMpegData, playBackMpegDataIndex, len);
						playBackMpegDataIndex += len;
					}
					//System.out.println("playBackMpegDataIndex = " +playBackMpegDataIndex + "appen len=" + len);
					int usedBytes = UdtTools.xvidDecorer(playBackMpegData, playBackMpegDataIndex, rgbDataBuf, BuildConfig.DEBUG?1:0);
					//System.out.println("usedBytes = " + usedBytes);
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
							if(video != null && !video.isRecycled()) {
								video.recycle();
							}
							video = Bitmap.createBitmap(newImageWidth, newImageHeight, Config.RGB_565);
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
						do {
							if(queue.getMpegLength()>= defintImageQueueLength) {
								synchronized (mpegBuf) {
									try {
										mpegBuf.wait(5);
									} catch (InterruptedException e) {
										e.printStackTrace();
										break;
									}
								}
							} else  {
								MpegImage mpegImage = new MpegImage(rgbDataBuf, timeStr, MPEG_DATA_TYPE, 0);
								queue.addMpegImage(mpegImage);
								break;
							}
						}while(true);
						remainBytes = (playBackMpegDataIndex - usedBytes);
						if(remainBytes<=0) {
							remainBytes = 0;
						}
						System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, remainBytes);
						playBackMpegDataIndex = remainBytes;
					}
				} else {
					//byte[] jpeg = new byte[pbmi.type];
					//sSystem.arraycopy(src, srcPos, dst, dstPos, playBackMpegDataLength)
					do {
						if(queue.getMpegLength()>= defintImageQueueLength) {
							synchronized (mpegBuf) {
								try {
									mpegBuf.wait(5);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} else  {
							synchronized (lock) {
								MpegImage mpegImage = new MpegImage(pbmi.data, timeStr, pbmi.type, pbmi.len);
								queue.addMpegImage(mpegImage);
								break;
							}
						}
					}while(true);
				}
			}
		}
		
		private void onStop() {
			System.gc();
			stopPlay = true;
			myVideoView.setImage(null);
			if(video != null && !video.isRecycled()) {
				video.recycle();
				video = null;
			}
			UdtTools.freeDecorer();
			this.rgbDataBuf = null;
			System.gc();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### play mpeg thread exit ....");
			}
		}
	}
	
	private class DisplayMpegThread implements Runnable {
		
		private long rate = -1;
		
		private long firstTime = 0;
		
		public DisplayMpegThread() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				MpegImage mpegImage = queue.getMpegImage();
				if(null != mpegImage) {
					String t = mpegImage.time;
					long tmp = DateUtil.formatTimeStrToMillionSecond(t);
					if(firstTime != tmp) {
						if(rate == -1) {
							rate = 1;
						} else {
							rate = tmp - firstTime;
						}
						firstTime = tmp;
					}
					if(rate >0 && rate <= 60000) {
						try {
							Thread.sleep(rate);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					if(mpegImage.dataType == MPEG_DATA_TYPE) {
						byte[] tmpRgb = mpegImage.rgb;
						ByteBuffer sh = ByteBuffer.wrap(tmpRgb);
						synchronized (lock) {
							if(video != null) {
								try {
									video.copyPixelsFromBuffer(sh);
								} catch (Exception e) {
									Log.e(TAG, "### copyPixelsFromBuffer exception!");
								}
							}
						}
					}else {
						synchronized (lock) {
							Bitmap b = BitmapFactory.decodeByteArray(mpegImage.rgb, 0, mpegImage.dataLength);
							if(b != null) {
								if(video != null && !video.isRecycled()) {
									video.recycle();
								}
								video = b;
								myVideoView.setImage(video);
							}
						}
					}
					
					System.out.println("rate = " + rate);
					if(listener != null) {
						listener.invalide( t.substring(0,14));
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
