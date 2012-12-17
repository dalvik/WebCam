package com.iped.ipcam.engine;

import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.MyVideoView;
import com.iped.ipcam.gui.MyVideoView.OnPutIndexListener;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.utils.Command;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class DecodeAudioThread extends DecoderFactory implements OnPutIndexListener{

	private AudioTrack m_out_trk = null;

	private String TAG = "DecodeAudioThread";
	
	private boolean DEBUG = true;
	
	private boolean stopPlay = false;
	
	private final static int RECEAUDIOBUFFERSIZE = 1600 * Command.CHANEL * 1;
	
	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE];
	
	private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Command.CHANEL * 10;
	
	private byte[] pcmArr = new byte[pcmBufferLength];
	
	private int zeroIndex32 = 0;
	
	
	public DecodeAudioThread(MyVideoView myVideoView) {
		myVideoView.setOnPutIndexListener(this);
	}
	
	
	@Override
	public void run() {
		int init = UdtTools.initAmrDecoder();
		if(BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "amr deocder init " + init);
		}
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
				AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size * 10,
				AudioTrack.MODE_STREAM);
		m_out_trk.play();
		
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
			zeroIndex32 = 0;
			for(int i = 0; i<recvDataLength; i++) {// ÅÐ¶ÏÊÇ·ñº¬ÓÐ32¸öÁã
				if(audioBuffer[i]==0) {
					zeroIndex32++;
					if(zeroIndex32 == 32) {
						stopPlay = true;
						break;
					}
				}else {
					zeroIndex32 = 0;
				}
			}
			UdtTools.amrDecoder(audioBuffer, recvDataLength, pcmArr, 0, Command.CHANEL);
			m_out_trk.write(pcmArr, 0, pcmBufferLength);
		}
		
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

	@Override
	public void onStop(boolean stopPlay) {
		this.stopPlay = stopPlay;
	}


	@Override
	public void updatePutIndex(int putIndex) {
		
	}
	
	@Override
	public int getIndexForGet() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void setOnMpegPlayListener(OnMpegPlayListener listener) {
		// TODO Auto-generated method stub
		
	}
}
