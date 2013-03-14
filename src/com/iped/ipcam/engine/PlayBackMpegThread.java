package com.iped.ipcam.engine;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.AudioFormat;
import android.media.AudioManager;
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
import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.pojo.MpegImage;
import com.iped.ipcam.pojo.PlayBackMpegInfo;
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
	
	private int frameCount;
	
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
	
	private int MAX_FRAME = 32;

	private int TOTAL_FRAME_SIZE = 50 * MAX_FRAME;
	
	private boolean hasAudioData = false;
	
	private byte[] audioTmpBuffer = new byte[MyVideoView.PLAYBACK_AUDIOBUFFERSIZE * 10];
	
	private int audioTmpBufferUsed;
	
	private byte[] amrBuffer = new byte[TOTAL_FRAME_SIZE];
	
	private int rate = 1;
	
	private boolean andioStartFlag = false;
	
	private int mpegByteBufLength = 2 * 150 * 1024;
	
	private int length = 1 * 400 * 1024;
	
	//private byte[] jpegByteBuf = null; 
	
	private byte[] play_back_mpegBuf = null;
	
	private int jpegBufUsed = 0;
	
	private int tmpMpgBufUsed = 0;
	
	private int mpegDataIndex = 0;
	
	private int dataType = -1; // -1 default  0 mpeg  1 jpeg  2 audio
	
	private byte[] mpegRawDataTmp = null;
	
	private int mpegRawDataLen = 0;
	
	private boolean hasMpegRawData = false; //是否有mpeg raw data需要处理
	
	private Object rawDataLock = new Object();
	
	private Object lock2 = new Object();
	
	// rgb data
	private byte[] rgbDataBuf = null;
	
	//private int usedBytes = 0;
	
	//private int unusedBytes = 0;
	
	private int mpegPakages = 3;
	
	private boolean canStartFlag = false; //开始解码标记
	
	private int startFlagCount = 1;
	
	//private boolean mpegStartFlag = false; // mpeg4开始标记
	
	//模拟缓冲区
	private PlayMpegThread playMpegThread = null;
	
	private int playBackIndexPut = 0;
	
	public PlayBackMpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount, Handler handler){
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		this.myVideoView = myVideoView;
		this.handler = handler;
		myVideoView.setOnPutIndexListener(this);
		queue = new VideoQueue();
		rawDataQueue = new PlayBackMpegQueue();
		//jpegByteBuf = new byte[jpegByteBufLength]; 
		play_back_mpegBuf = new byte[NALBUFLENGTH];
	}
	
	
	@Override
	public void run() {
		stopPlay = false;
		initTableHeadCount = 0;
		//Thread playAudioThread = new Thread(new PalyBackAudio());
		//playAudioThread.start();
		//new Thread(new DecodeMpegThread()).start();
		//new Thread(new DisplayMpegThread()).start();
		playMpegThread = new PlayMpegThread(false, myVideoView, play_back_mpegBuf, timeStr, video, frameCount);
		new Thread(playMpegThread).start();
		do{
			if((indexForGet+5)%NALBUFLENGTH == indexForPut){
				synchronized (play_back_mpegBuf) {
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### mpeg video data buffer is empty! ---->");
					}
					try {
						play_back_mpegBuf.wait(50);
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
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "t1Length=" + t1Length +  "  t2Length = " + t2Length + " targetTableLength = " + targetTableLength);
						}
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
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "### recv play back table header complete! ");
						}
					}
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
					if(dataType == 0) {
						//Log.e(TAG, "### mpeg length = " + mpegDataIndex);
						mpegDataIndex = 0;
						
						while(true) {
							if((playBackIndexPut +5) % NALBUFLENGTH == playMpegThread.getIndexForGet()) {	
								synchronized (play_back_mpegBuf) {
									try {
										play_back_mpegBuf.wait(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}  
							} else {
								play_back_mpegBuf[playBackIndexPut] = b0;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b1;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b2;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b3;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b4;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								playMpegThread.updatePutIndex(playBackIndexPut);
								break;
							}
						}
					}
					dataType = -1;
					canStartFlag = true;
					startFlag = true;
					indexForGet+=4;
					if(!initTableInfo) { // 完成接收索引表
						insideHeaderFlag = true;// time info
						audioBufferUsedLength = 0;
						/*mpegBuf[mpegDataIndex] = b0;
						mpegBuf[mpegDataIndex + 1] = b1;
						mpegBuf[mpegDataIndex + 2] = b2;
						mpegBuf[mpegDataIndex + 3] = b3;
						mpegBuf[mpegDataIndex + 4] = b4;
						mpegDataIndex += 5;*/
						
						while(true) {
							if((playBackIndexPut +5) % NALBUFLENGTH == playMpegThread.getIndexForGet()) {	
								synchronized (play_back_mpegBuf) {
									try {
										play_back_mpegBuf.wait(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}  
							} else {
								play_back_mpegBuf[playBackIndexPut] = b0;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b1;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b2;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b3;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								play_back_mpegBuf[playBackIndexPut] = b4;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								
								playMpegThread.updatePutIndex(playBackIndexPut);
								break;
							}
						}
					}
					insideHeadCount = 0;//  收到0001c后将initTableInfo值0
					andioStartFlag = false;
					
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
					}
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 11 ) { // 0001b
					dataType = 3;//audio
					indexForGet+=4;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.e(TAG, "### audio data start ------");
					}
				} else {
					if(insideHeaderFlag) {//首先接收时间戳
						timeByte[insideHeadCount] = b0;
						while(true) {
							if((playBackIndexPut +1) % NALBUFLENGTH == playMpegThread.getIndexForGet()) {	
								Log.d(TAG, "=====111111111=====");
								synchronized (play_back_mpegBuf) {
									try {
										play_back_mpegBuf.wait(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}  
							} else {
								play_back_mpegBuf[playBackIndexPut] = b0;
								playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
								playMpegThread.updatePutIndex(playBackIndexPut);
								break;
							}
						}
						insideHeadCount++;
						if(insideHeadCount >= 18) { 
							insideHeaderFlag = false;	
							timeStr = new String(timeByte, 0, 14);
							Log.d(TAG, "### timeStr = " + timeStr);
						}
					}else {//根据标记分离视频（mpeg4、jpeg）和音频数据
						if(startFlag) {
							startFlag = false;
							if(b0 == 0 &&  b1 == 0 &&  b2 == 0 && b3 == 0 && b4 == 0 && !insideHeaderFlag) {//mpeg4
								dataType = 0;//mpeg4
								indexForGet+=9;
								if(BuildConfig.DEBUG && DEBUG) {
									Log.e(TAG, "### mpeg data start ------");
								}
							} else { //jpeg
								dataType = 1;//jpeg
								mpegDataIndex = 0;
								if(BuildConfig.DEBUG && DEBUG) {
									Log.e(TAG, "### jpeg data start ------");
								}
							}
							continue;
						}
						if(dataType == 0) {//mpeg4
							while(true) {
								if((playBackIndexPut +1) % NALBUFLENGTH == playMpegThread.getIndexForGet()) {	
									synchronized (play_back_mpegBuf) {
										try {
											play_back_mpegBuf.wait(50);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}  
								} else {
									play_back_mpegBuf[playBackIndexPut] = b0;
									playBackIndexPut = (playBackIndexPut+1)%NALBUFLENGTH;
									playMpegThread.updatePutIndex(playBackIndexPut);
									break;
								}
							}
						} else if(dataType == 3){ //A audio 
							/*if(b0 == 60) {
								andioStartFlag = true;
							} 
							if(andioStartFlag) {
								amrBuffer[audioBufferUsedLength++] = b0;
								if(audioBufferUsedLength >= TOTAL_FRAME_SIZE) {
									audioTmpBufferUsed = audioBufferUsedLength;
									audioBufferUsedLength = 0;
									synchronized (audioTmpBuffer) {
										System.arraycopy(amrBuffer, 0, audioTmpBuffer, 0, audioTmpBufferUsed);
										hasAudioData = true;
										audioTmpBuffer.notify();
									}
								}
							}*/
						} else {// jpeg do nothing
							/*if(isMpeg4) { //mpeg4
								
							} */
						}
					}
				}
				indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
			}
		} while(!stopPlay);
		/*if(playJpegThread != null && !playJpegThread.isInterrupted()) {
			playJpegThread.interrupt();
		}*/
		
		//if(playAudioThread != null && !playAudioThread.isInterrupted()) {
		//	playAudioThread.interrupt();
		//}
	}

	private class AnalizeMpegRawData implements Runnable {
		
		
		public AnalizeMpegRawData() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(hasMpegRawData) {
					hasMpegRawData = false;
					if(rgbDataBuf == null && !stopPlay) {
						int[] headInfo = UdtTools.initXvidHeader(mpegRawDataTmp, mpegRawDataLen);//length的长度即为out_buffer的长度，所以length要足够长。
						int imageWidth = headInfo[0];
						int imageHeight = headInfo[1];
						//usedBytes = headInfo[2];
						//unusedBytes = (mpegRawDataLen - usedBytes);
						//if(unusedBytes<=0) {
						//	unusedBytes = 0;
						//}
						//System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
						//mpegDataIndex = unusedBytes;
						if(imageWidth<=0) {
							Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
							continue;
						}
						System.gc();
						rgbDataBuf = new byte[imageWidth * imageHeight * 4];
					//	Log.d(TAG, "### W = " + imageWidth + " H = " + imageHeight + " used_bytes = " + usedBytes + " rgb length = " + rgbDataBuf.length);
						synchronized (lock2) {
							if(video != null && !video.isRecycled()) {
								video.recycle();
								video = null;
							}
							video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
							myVideoView.setImage(video);
						}
					}
				}else {
					synchronized (rawDataLock) {
						try {
							rawDataLock.wait();
						} catch (InterruptedException e) {
							stopPlay = false;
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	private class PalyBackAudio implements Runnable {
		
		private int pcmBufferLength = TOTAL_FRAME_SIZE * 10; //PLAYBACK_AUDIOBUFFERSIZE * Command.CHANEL * 50;
		
		private byte[] pcmArr = new byte[pcmBufferLength];
		

		private AudioTrack m_out_trk = null;
		
		public PalyBackAudio(){
			int init = UdtTools.initAmrDecoder();
			Log.d(TAG, "amr deocder init " + init);
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
			}
			int m_out_buf_size = android.media.AudioTrack.getMinBufferSize(
					TalkBackThread.frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, TalkBackThread.frequency,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size * 5,
					AudioTrack.MODE_STREAM);
			m_out_trk.play();
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(hasAudioData) {
					//Log.d(TAG, "audio decode =" + audioTmpBufferUsed);
					UdtTools.amrDecoder(audioTmpBuffer, audioTmpBufferUsed, pcmArr, 0, Command.CHANEL);
					m_out_trk.write(pcmArr, 0, pcmBufferLength);
					hasAudioData = false;
					//Log.d(TAG, "del ==== " + del);
				}else {
					synchronized (audioTmpBuffer) {
						try {
							audioTmpBuffer.wait();
						} catch (InterruptedException e) {
							stopPlay = true;
							release();
							e.printStackTrace();
						}
					}
					}
			}
			release();
		}
		
		private void release() {
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
				UdtTools.exitAmrDecoder();
			}
		}
	}

	private class DecodeMpegThread implements Runnable {
	
		private int playBackMpegDataLength = 400 * 1024;
		
		private int playBackMpegDataIndex = 0;
		
		private byte[] playBackMpegData = null;
		
		private int usedBytes = 0;
		
		private int unusedBytes = 0;
		
		private String TAG = "DecodePlayBackMpeg";
		
		public DecodeMpegThread()  {
			playBackMpegData = new byte[playBackMpegDataLength];	
		}
		
		@Override
		public void run() {
			int res = UdtTools.initXvidDecorer();
			int packetNum = 1;
			int dstPos = 0;
			if(res != 0) {
				Log.d(TAG, "xvid init decoder error " + res);
				return ;
			}
			while(!stopPlay) {
				if(rgbDataBuf == null) {//初始化头信息
					while(true) {
						PlayBackMpegInfo pbmi = rawDataQueue.getMpeg();
						if(pbmi != null) {
							byte[] tmp = pbmi.getData();
							int len = pbmi.getLen();
							Log.d(TAG, "### get raw data = " +tmp.length + "  data len = " + len );
							System.arraycopy(tmp, 0, playBackMpegData, dstPos, len);
							dstPos+=len;
							playBackMpegDataIndex = dstPos;
							packetNum++;
						}
						if(packetNum%5 == 0) {
							int[] headInfo = UdtTools.initXvidHeader(playBackMpegData, dstPos);//length的长度即为out_buffer的长度，所以length要足够长。
							int imageWidth = headInfo[0];
							int imageHeight = headInfo[1];
							usedBytes = headInfo[2];
							unusedBytes = (dstPos - usedBytes);
							Log.d(TAG, "### decode mpeg4 res = " + imageWidth + "  " + imageHeight + "  " + unusedBytes);
							if(unusedBytes<=0) {
								unusedBytes = 0;
							}
							System.arraycopy(playBackMpegData, usedBytes, playBackMpegData, 0, unusedBytes);
							playBackMpegDataIndex = unusedBytes;//剩余的mpeg raw data
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
						if((playBackMpegDataLength - playBackMpegDataIndex)>len) {							
							System.arraycopy(tmp, 0, playBackMpegData, playBackMpegDataIndex, len);
							playBackMpegDataIndex += len;
							usedBytes = UdtTools.xvidDecorer(playBackMpegData, playBackMpegDataIndex, rgbDataBuf, BuildConfig.DEBUG?1:0); 
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
									}/**/
									myVideoView.setImage(video);
								}
								myVideoView.updateRect();
								myVideoView.updateResulation(newImageWidth);
								unusedBytes = (playBackMpegDataIndex - useBytes);
								if(unusedBytes<=0) {
									unusedBytes = 0;
								}
								System.arraycopy(playBackMpegData, useBytes, playBackMpegData, 0, unusedBytes);
								playBackMpegDataLength = unusedBytes;
							} else {
								MpegImage mpegImage = new MpegImage(rgbDataBuf, timeStr);
								queue.addMpegImage(mpegImage);
								unusedBytes = (playBackMpegDataIndex - usedBytes);
								if(unusedBytes<=0) {
									unusedBytes = 0;
								}
								System.arraycopy(play_back_mpegBuf, usedBytes, play_back_mpegBuf, 0, unusedBytes);
								//System.out.println("### move ========= " + (SystemClock.currentThreadTimeMillis() - curr));
								playBackMpegDataIndex = unusedBytes;
							}
						}
					}
				}
			}
			
		}
	}
	
	private class DisplayMpegThread implements Runnable {
		
		public DisplayMpegThread() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
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
				}else {
					synchronized (lock) {
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### no image data ---->");
						}
						try {
							lock.wait(5);
						} catch (InterruptedException e) {
							stopPlay = true;
							e.printStackTrace();
						}
					}  
				}
			}
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
