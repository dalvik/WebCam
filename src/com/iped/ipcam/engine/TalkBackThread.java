package com.iped.ipcam.engine;

import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.Speex;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.utils.Command;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class TalkBackThread implements Runnable {

	private static final int frequency = 8000;
	
	private static final int EncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
	
	private final static int RECEAUDIOBUFFERSIZE = 1600 * Command.CHANEL * 1;
	 
	protected int miniRecoderBufSize;
	
	private int pcmBufferLength = 0;
	
	private int amrBufferLength = 0;
	
	//private int sendAudioToCamLength = 0;
	
	//private byte[] sendAudioBufferToCam;
	
	//private int index = 0;
	
	protected AudioRecord audioRecord;

	private byte[] amrBuffer;
	
	//private short[] pcmBuffer;
	
	private byte[] micBuffer; //
	
	private String TAG = "TalkBackThread";
	
	private static boolean stopPlay = false;
	
	private boolean openSendAudioFlag = false;
	
	private boolean DEBUG = true;
	
	public TalkBackThread() {
		Speex.initEcho(160, 160*10);
		UdtTools.initAmrEncoder();
		createAudioRecord();
		pcmBufferLength = RECEAUDIOBUFFERSIZE;
		amrBufferLength = RECEAUDIOBUFFERSIZE/10;
		//sendAudioToCamLength = amrBufferLength * 2;
		//pcmBuffer = new short[pcmBufferLength];
		micBuffer = new byte[pcmBufferLength];
		amrBuffer = new byte[amrBufferLength];
		//sendAudioBufferToCam = new byte[sendAudioToCamLength];
		stopPlay = false;
	}
	
	@Override
	public void run() {
		startRecording();
		writeAudioDataToFile();
	}

	public void createAudioRecord(){
		  miniRecoderBufSize = AudioRecord.getMinBufferSize(frequency,
				  AudioFormat.CHANNEL_CONFIGURATION_MONO, EncodingBitRate);
  		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
  				AudioFormat.CHANNEL_CONFIGURATION_MONO, EncodingBitRate, miniRecoderBufSize*10);  
  	}
	
	private void startRecording(){
		  audioRecord.startRecording();
	}
	
	 private void writeAudioDataToFile(){
         int read = 0;
         while(!stopPlay && openSendAudioFlag){
             read = audioRecord.read(micBuffer, 0, pcmBufferLength);
             if(AudioRecord.ERROR_INVALID_OPERATION == read){
           	  Log.d(TAG, "### recorder over !");
           	  break;
             }
       	  // Speex.cancellation(micBuffer, recfBuffer, amrBuffer);
             if(UdtTools.EncoderPcm(micBuffer, pcmBufferLength, amrBuffer, amrBufferLength)==0){
           	  	UdtTools.sendAudioMsg(amrBuffer, amrBufferLength);
             }
         }
         stopRecording();
	  }

	 private void stopRecording(){
		 if(BuildConfig.DEBUG && DEBUG) {
			 Log.d(TAG, "### stop recording !");
		 }
		 // Speex.stopEcho();
         if(null != audioRecord){
             audioRecord.stop();
             audioRecord.release();
             audioRecord = null;
         }
	 }
	 
	 public static void stopTalkBack() {
		 stopPlay = true;
	 }
}
