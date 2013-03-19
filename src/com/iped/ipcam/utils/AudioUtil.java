package com.iped.ipcam.utils;

import com.iped.ipcam.engine.TalkBackThread;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioUtil {

	private static AudioTrack m_out_trk = null;
	
	private static final String TAG = "AudioUtil";
	
	private AudioUtil() {
		
	}
	
	public static AudioTrack getAudioTrackInstance() {
		if(m_out_trk == null) {
			int m_out_buf_size = android.media.AudioTrack.getMinBufferSize(
					TalkBackThread.frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
			m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, TalkBackThread.frequency,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,	AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size * 10,
					AudioTrack.MODE_STREAM);
		}
		return m_out_trk;
	}
	
	public static void exitAudioTrack() {
		if (m_out_trk != null) {
			m_out_trk.stop();
			m_out_trk.release();
			m_out_trk = null;
			Log.d(TAG, "### audio track release.");
		}
	}
}
