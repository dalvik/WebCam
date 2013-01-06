package com.iped.ipcam.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.VideoQueue;

public class PlayBackJpegThread extends DecoderFactory implements Runnable, OnPutIndexListener  {

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
	
	byte[] jpegBuf = new byte[NALBUFLENGTH];
	
	private OnMpegPlayListener listener;
	
	private boolean initTableInfo = true;
	
	private int initTableHeadCount = 0;//用以计算回放表的八个字节的长度信息
	
	private int maxHeadLength = 32 * 1024;
	
	private int targetTableLength = 0;
	
	private byte[] headerInfoByte = new byte[maxHeadLength];
	
	private Handler handler;
	
	private int t1Length =  0;
	
	private int t2Length =  0;
	
	private int insideHeadCount = 0;
	
	private boolean insideHeaderFlag = false;
	
	private	int jpegDataLength = 0;
	
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
	
	public PlayBackJpegThread(MyVideoView myVideoView, byte[] nalBuf, String timeStr, Bitmap video, int frameCount, Handler handler){
		this.nalBuf = nalBuf;
		this.timeStr = timeStr;
		this.video = video;
		this.frameCount = frameCount;
		this.myVideoView = myVideoView;
		this.handler = handler;
		myVideoView.setOnPutIndexListener(this);
		queue = new VideoQueue();
	}
	
	
	@Override
	public void run() {
		stopPlay = false;
		initTableHeadCount = 0;
		//new Thread(new PlayJpegThread()).start();
		//new Thread(new PalyBackAudio()).start();
		do{
			if((indexForGet+5)%NALBUFLENGTH == indexForPut){
				synchronized (jpegBuf) {
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### data buffer is empty! ---->");
					}
					try {
						jpegBuf.wait(50);
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
					if(initTableHeadCount == targetTableLength) {
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
					indexForGet+=4;
					insideHeaderFlag = true;
					insideHeadCount = 0;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
					}
				}else if(b0 == -1 &&  b1 == -40 &&  b2 == -1 && b3 == -32) {
					video = BitmapFactory.decodeByteArray(jpegBuf, 0, jpegDataLength);
					queue.addJpegImage(new JpegImage(video, timeStr));
					jpegBuf[0] = b0;
					jpegBuf[1] = b1;
					jpegBuf[2] = b2;
					jpegBuf[3] = b3;
					indexForGet += 3;
					jpegDataLength = 4;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.d(TAG, "### jpeg start flag ->" + video);
					}
				} else {
					if(insideHeaderFlag) {
						///System.out.println(insideHeadCount + " " + b0);
						timeByte[insideHeadCount++] = b0;
						if(insideHeadCount == 2) {
							isVideo = b0;
						}
						if(insideHeadCount >= 26) { 
							insideHeaderFlag = false;	
							timeStr = new String(timeByte, 4, 14);
							try {
								rate = 1000*1000/Integer.parseInt(new String(timeByte, 18, 8));
							} catch (Exception e) {
								rate = 2;
							}
						}
					}else {
						if(isVideo == 1){ //video
							andioStartFlag = false;
							if(jpegDataLength<NALBUFLENGTH) {
								jpegBuf[jpegDataLength++] = b0;
							}
						}else if(isVideo == 2) {//audio
							if(b0 == 60) {
								andioStartFlag = true;
								//amrBuffer[audioBufferUsedLength++] = b0;
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
							
						}
					}
				}
				indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
			}
		} while(!stopPlay);
		synchronized (audioTmpBuffer) {
			audioTmpBuffer.notify();
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
							e.printStackTrace();
						}
					}
					}
				}
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
				UdtTools.exitAmrDecoder();
			}
		}
	}
	private class PlayJpegThread implements Runnable {
		
		public PlayJpegThread() {
			
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
						listener.invalide(frameCount, image.time);
					}
					try {
						if(rate !=0) {
							Thread.sleep(1000/rate);
						} else {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
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
}
