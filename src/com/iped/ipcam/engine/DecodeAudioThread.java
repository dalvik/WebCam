package com.iped.ipcam.engine;

import android.media.AudioTrack;
import android.util.Log;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.utils.AudioUtil;
import com.iped.ipcam.utils.Command;

public class DecodeAudioThread extends DecoderFactory{

	private String TAG = "DecodeAudioThread";
	
	private boolean DEBUG = true;
	
	private boolean stopPlay = false;
	
	private final static int RECEAUDIOBUFFERSIZE = 5120 * Command.CHANEL * 1;
	
	private final static int DECODE_AUDIO_DATA_LENGTH = 800;
	
	private byte[] recvAudioBuffer = new byte[RECEAUDIOBUFFERSIZE];// recv audio data by native
	
	private int pcmBufferLength = DECODE_AUDIO_DATA_LENGTH * Command.CHANEL * 10;
	
	private byte[] pcmArr = new byte[pcmBufferLength];
	
	private byte[] amrAudioBuf = new byte[DECODE_AUDIO_DATA_LENGTH];
	
	private int zeroIndex32 = 0;
	
	private int recvAudioBufLen = 50 * 1024;
	private byte[] recvAudioBuf = null; 
	private int indexForPut = 0; // put索引 （下一个要写入的位置）
	private int indexForGet = 0;
	private MyVideoView myVideoView;
	
	public DecodeAudioThread(MyVideoView myVideoView) {
		recvAudioBuf = new byte[recvAudioBufLen];
		this.myVideoView = myVideoView;
	}
	
	
	@Override
	public void run() {
		Thread thread = new Thread(new PlaySoundThread());
		thread.start();
		int recvDataLength = -1;
		int timeoutCounter = 0;
		while (!stopPlay) {
			recvDataLength = UdtTools.recvAudioMsg(RECEAUDIOBUFFERSIZE, recvAudioBuffer, RECEAUDIOBUFFERSIZE);
			//Log.d(TAG, "### recv audio DataLength = " + recvDataLength);
			if(recvDataLength<=0) {
				timeoutCounter++;
				if(recvDataLength == -1) {
					stopPlay = true;
					if(BuildConfig.DEBUG && DEBUG) {
						Log.e(TAG, "### thread recv audio over! recvDataLength = " + recvDataLength );
					}
					myVideoView.callBackStop();
					break;
				}
				if(timeoutCounter>15) {
					stopPlay = true;
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.e(TAG, "### recv audio data over break....! timeoutCounter = " + timeoutCounter);
					}
					break;
				}else {
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.e(TAG, "### recv audio data timeout....");
					}
					continue;
				}
			}
			timeoutCounter = 0;
			int recvBufIndex = 0;
			do{
				if((indexForPut +1) % recvAudioBufLen == indexForGet) {
					synchronized (recvAudioBuf) {
						try {
							recvAudioBuf.wait(100);
							//myVideoView.callBackStop();
							//stopPlay = true;
							Log.e(TAG, "### audio data buffer is full! ---->");
							//throw new CamManagerException("audio data buffer is full!");
						} catch (Exception e) {
							stopPlay = true;
							if(thread != null && !thread.isInterrupted()) {
								thread.interrupt();
							}
							e.printStackTrace();
						}
					}
				}else {
					recvAudioBuf[indexForPut]= recvAudioBuffer[recvBufIndex];  
					indexForPut = (indexForPut+1) % recvAudioBufLen;  
				    recvBufIndex++;
				}
			}while(recvDataLength>recvBufIndex && !stopPlay);
		}
		if(thread != null && !thread.isInterrupted()) {
			thread.interrupt();
		}
	}

	@Override
	public void onStop(boolean stopPlay) {
		this.stopPlay = stopPlay;
	}


	@Override
	public int getIndexForGet() {
		return 0;
	}
	
	@Override
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
	}
	
	private class PlaySoundThread implements Runnable {

		//private AudioTrack m_out_trk = null;
		
		private int playAudioBufIndex = 0;
		
		public PlaySoundThread() {
			int init = UdtTools.initAmrDecoder();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "amr deocder init " + init);
			}
		}
		
		@Override
		public void run() {
			AudioTrack m_out_trk = AudioUtil.getAudioTrackInstance();
			m_out_trk.play();
			zeroIndex32 = 0;
			do{
				if(indexForGet % recvAudioBufLen == indexForPut){
					synchronized (pcmArr) {
						if(BuildConfig.DEBUG && DEBUG) {
							Log.d(TAG, "### audio buffer is empty! ---->");
						}
						try {
							pcmArr.wait(100);
						} catch (InterruptedException e) {
							stopPlay = true;
							release();
							e.printStackTrace();
						}
					}  
				}else {
					byte b0 = recvAudioBuf[indexForGet];
					if(b0 == 0) {
						zeroIndex32++;
						if(zeroIndex32 == 32) {
							stopPlay = true;
							Log.d(TAG, "### 0000000000000000000000000000000");
							break;
						}
					}else {
						zeroIndex32 = 0;
					}
					amrAudioBuf[playAudioBufIndex] = b0;
					if(++playAudioBufIndex % DECODE_AUDIO_DATA_LENGTH == 0) {
						UdtTools.amrDecoder(amrAudioBuf, playAudioBufIndex, pcmArr, 0, Command.CHANEL);
						playAudioBufIndex = 0;
						m_out_trk.write(pcmArr, 0, pcmBufferLength);
					}
					indexForGet = (indexForGet + 1)%recvAudioBufLen;  
				}
			} while(!stopPlay);
			release();
			//myVideoView.callBackStop();
		}
		
		private void release()  {
			AudioUtil.exitAudioTrack();
			UdtTools.exitAmrDecoder();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### audio amr decoder exit.");
			}
		}
	}
}
