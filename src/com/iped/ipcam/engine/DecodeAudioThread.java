package com.iped.ipcam.engine;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.utils.Command;

public class DecodeAudioThread extends DecoderFactory{

	private String TAG = "DecodeAudioThread";
	
	private boolean DEBUG = true;
	
	private boolean stopPlay = false;
	
	private final static int RECEAUDIOBUFFERSIZE = 1600 * Command.CHANEL * 1;
	
	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE];
	
	private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Command.CHANEL * 10;
	
	private byte[] pcmArr = new byte[pcmBufferLength];
	
	private byte[] playAudioBuf = new byte[RECEAUDIOBUFFERSIZE];
	
	private int zeroIndex32 = 0;
	
	private int recvAudioBufLen = 50 * 1024;
	private byte[] recvAudioBuf = null; 
	private int indexForPut = 0; // put索引 （下一个要写入的位置）
	private int indexForGet = 0;
	
	public DecodeAudioThread(MyVideoView myVideoView) {
		recvAudioBuf = new byte[recvAudioBufLen];
	}
	
	
	@Override
	public void run() {
		Thread thread = new Thread(new PlaySoundThread());
		thread.start();
		int recvDataLength = -1;
		while (!stopPlay) {
			recvDataLength = UdtTools.recvAudioMsg(RECEAUDIOBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
			if(recvDataLength<=0) {
				if(BuildConfig.DEBUG && DEBUG) {
					Log.d(TAG, "### audio recv audio over");
				}
				stopPlay = true;
				break;
			}
			int recvBufIndex = 0;
			do{
				if((indexForPut +1) % recvAudioBufLen == indexForGet) {
					synchronized (recvAudioBuf) {
						try {
							recvAudioBuf.wait(10);
							Log.d(TAG, "### audio buffer is full! ---->");
						} catch (InterruptedException e) {
							stopPlay = true;
							e.printStackTrace();
						}
					}
				}else {
					recvAudioBuf[indexForPut]= audioBuffer[recvBufIndex];  
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

		private AudioTrack m_out_trk = null;
		
		private int playAudioBufIndex = 0;
		
		public PlaySoundThread() {
			int init = UdtTools.initAmrDecoder();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "amr deocder init " + init);
			}
		}
		
		@Override
		public void run() {
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
			}
			int m_out_buf_size = android.media.AudioTrack.getMinBufferSize(
					8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
			m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,	AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size * 10,
					AudioTrack.MODE_STREAM);
			m_out_trk.play();
			zeroIndex32 = 0;
			do{
				if((indexForGet+1) % recvAudioBufLen == indexForPut){
					synchronized (pcmArr) {
						if(BuildConfig.DEBUG && !DEBUG) {
							Log.d(TAG, "### audio buffer is empty! ---->");
						}
						try {
							pcmArr.wait(300);
						} catch (InterruptedException e) {
							stopPlay = true;
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
					playAudioBuf[playAudioBufIndex] = b0;
					if(++playAudioBufIndex % RECEAUDIOBUFFERSIZE == 0) {
						UdtTools.amrDecoder(playAudioBuf, playAudioBufIndex, pcmArr, 0, Command.CHANEL);
						playAudioBufIndex = 0;
						m_out_trk.write(pcmArr, 0, pcmBufferLength);
					}
					indexForGet = (indexForGet + 1)%recvAudioBufLen;  
				}
			} while(!stopPlay);
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
			}
			UdtTools.exitAmrDecoder();
			if(BuildConfig.DEBUG && DEBUG) {
				Log.d(TAG, "### audio amr decoder exit.");
			}
		}
	}
}
