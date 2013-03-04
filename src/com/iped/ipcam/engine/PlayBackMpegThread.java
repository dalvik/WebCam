package com.iped.ipcam.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PlayBackConstants;
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
	
	private boolean initTableInfo = true;
	
	private int initTableHeadCount = 0;//用以计算回放表的八个字节的长度信息
	
	private int maxHeadLength = 32 * 1024;
	
	private int targetTableLength = 0;
	
	private byte[] headerInfoByte = new byte[maxHeadLength];
	
	private Handler handler;
	
	private int t1Length =  0;
	
	private int t2Length =  0;
	
	private int insideHeadCount = 0;// 记录接收内部头的长度 一定要>=26
	
	private boolean insideHeaderFlag = false;// 内部头开始标记，截取时间戳
	
	private byte[] timeByte = new byte[100];
	
	private VideoQueue queue = null;
	
	private Object lock = new Object();
	
	private int isVideo = 0;
	
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
	
	private byte[] mpegBuf = null;
	
	private int jpegBufUsed = 0;
	
	private int tmpMpgBufUsed = 0;
	
	private int mpegDataIndex = 0;
	
	private boolean isMpeg4 = false;
	
	private byte[] mpegRawDataTmp = null;
	
	private int mpegRawDataLen = 0;
	
	private boolean hasMpegRawData = false; //是否有mpeg raw data需要处理
	
	private Object rawDataLock = new Object();
	
	private Object lock2 = new Object();
	
	// rgb data
	private byte[] rgbDataBuf = null;
	
	private int usedBytes = 0;
	
	private int unusedBytes = 0;
	
	private int mpegPakages = 3;
	
	private boolean canStartFlag = false; //开始解码标记
	
	private int startFlagCount = 1;
	
	//private boolean mpegStartFlag = false; // mpeg4开始标记
	
	public PlayBackMpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount, Handler handler){
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		this.myVideoView = myVideoView;
		this.handler = handler;
		myVideoView.setOnPutIndexListener(this);
		queue = new VideoQueue();
		//jpegByteBuf = new byte[jpegByteBufLength]; 
		mpegBuf = new byte[length];
	}
	
	
	@Override
	public void run() {
		int res = UdtTools.initXvidDecorer();
		if(res != 0) {
			stopPlay = true;
			Log.d(TAG, "xvid init decoder error " + res);
			return ;
		}
		stopPlay = false;
		initTableHeadCount = 0;
		/*Thread playJpegThread = new Thread(new PlayJpegThread());
		playJpegThread.start();*/
		Thread playAudioThread = new Thread(new PalyBackAudio());
		playAudioThread.start();
		do{
			if((indexForGet+5)%NALBUFLENGTH == indexForPut){
				synchronized (mpegBuf) {
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### mpeg video data buffer is empty! ---->");
					}
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
					canStartFlag = true;
					isVideo = 0;
					indexForGet+=4;
					if(!initTableInfo) {						
						insideHeaderFlag = true;// time info
					}
					insideHeadCount = 0;
					andioStartFlag = false;
					audioBufferUsedLength = 0;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
					}
				}else if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 11 ) { // 0001b
					isMpeg4 = false;
					indexForGet+=4;
					isVideo = 65;
				}else if(b0 == 0 &&  b1 == 0 &&  b2 == 0 && b3 == 0 && !insideHeaderFlag) {//mpeg4
					if(startFlagCount++ % mpegPakages == 0 && canStartFlag){
						decodeMpeg();
					}
					mpegBuf[mpegDataIndex] = b0;
					mpegBuf[mpegDataIndex + 1] = b1;
					mpegBuf[mpegDataIndex + 2] = b2;
					mpegBuf[mpegDataIndex + 3] = 1;
					mpegBuf[mpegDataIndex + 4] = 12;
					mpegDataIndex += 5;
					
					isMpeg4 = true;
					/*for(int i =0;i<9;i++) { //此处跳过11个0 没有原因
						mpegBuf[mpegDataIndex++] = b0;
					}*/
					indexForGet+=8;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### mpeg start flag ->" + video);
					}
				} else if(b0 == -1 &&  b1 == -40 &&  b2 == -1) {
					isMpeg4 = false;
					indexForGet+=2;
				} else {
					if(insideHeaderFlag) {
						timeByte[insideHeadCount] = b0;
						if(insideHeadCount == 0) {
							isVideo = b0;
						}
						//mpegBuf[mpegDataIndex++] = b0;
						insideHeadCount++;
						if(insideHeadCount >= 18) { 
							insideHeaderFlag = false;	
							timeStr = new String(timeByte, 0, 14);
							Log.d(TAG, "### timeStr = " + timeStr);
							/*try {
								rate = 1000*1000/Integer.parseInt(new String(timeByte, 18, 8));
							} catch (Exception e) {
								rate = 2;
							}*/
						}
					}else {
						if(isVideo == 65){ //A audio 
							if(b0 == 60) {
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
							}
						} else {// video
							if(isMpeg4) { //mpeg4
								if(mpegDataIndex<length) {
									mpegBuf[mpegDataIndex++] = b0;
								}
							} 
						}
					}
				}
				indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
			}
		} while(!stopPlay);
		/*if(playJpegThread != null && !playJpegThread.isInterrupted()) {
			playJpegThread.interrupt();
		}*/
		
		if(playAudioThread != null && !playAudioThread.isInterrupted()) {
			playAudioThread.interrupt();
		}
	}

	private void decodeMpeg() {
		if(rgbDataBuf == null && !stopPlay) {
			int[] headInfo = UdtTools.initXvidHeader(mpegBuf, length);//length的长度即为out_buffer的长度，所以length要足够长。
			int imageWidth = headInfo[0];
			int imageHeight = headInfo[1];
			usedBytes = headInfo[2];
			unusedBytes = (mpegDataIndex - usedBytes);
			if(unusedBytes<=0) {
				unusedBytes = 0;
			}else {
				System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
				mpegDataIndex = unusedBytes;
			}
			if(imageWidth<=0) {
				Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
				return;
			}
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
			mpegPakages = 2;
			startFlagCount = 1;
		} else if(!stopPlay){
			usedBytes = UdtTools.xvidDecorer(mpegBuf, mpegDataIndex, rgbDataBuf, BuildConfig.DEBUG?1:0); //flag == 1 printf decode time
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
				unusedBytes = (mpegDataIndex - useBytes);
				if(unusedBytes<=0) {
					unusedBytes = 0;
				}
				System.arraycopy(mpegBuf, useBytes, mpegBuf, 0, unusedBytes);
				mpegDataIndex = unusedBytes;
			} else {
				MpegImage mpegImage = new MpegImage(rgbDataBuf, timeStr);
				queue.addMpegImage(mpegImage);
				unusedBytes = (mpegDataIndex - usedBytes);
				if(unusedBytes<=0) {
					unusedBytes = 0;
				}
				System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
				mpegDataIndex = unusedBytes;
			}
		}
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
						usedBytes = headInfo[2];
						unusedBytes = (mpegRawDataLen - usedBytes);
						if(unusedBytes<=0) {
							unusedBytes = 0;
						}
						System.arraycopy(mpegBuf, usedBytes, mpegBuf, 0, unusedBytes);
						mpegDataIndex = unusedBytes;
						if(imageWidth<=0) {
							Log.d(TAG, "### imageWidth = " + imageWidth + "  xvid find header fail");
							continue;
						}
						System.gc();
						rgbDataBuf = new byte[imageWidth * imageHeight * 4];
						Log.d(TAG, "### W = " + imageWidth + " H = " + imageHeight + " used_bytes = " + usedBytes + " rgb length = " + rgbDataBuf.length);
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
					8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
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

	private class PlayMpegThread implements Runnable {
		
		public PlayMpegThread() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(queue.getImageListLength()>0) {
					JpegImage image = queue.removeImage();
					myVideoView.setImage(image.bitmap);
					frameCount = myVideoView.getFrameCount();
					frameCount++;
					Message msg = handler.obtainMessage();
					msg.what = Constants.UPDATE_PLAY_BACK_TIME;
					msg.obj = image.time;
					handler.sendMessage(msg);
					if(listener != null) {
						listener.invalide(image.time);
					}
					try {
						if(rate !=0) {
							Thread.sleep(1000/rate);
						} else {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
						stopPlay = true;
						e.printStackTrace();
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
