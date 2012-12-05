package com.iped.ipcam.gui;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.pojo.Image;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.VideoQueue;

public class MyVideoView extends ImageView implements Runnable {

	private boolean DEBUG = true;
	
	private Thread recvAudioThread = null;
	
	private final static int DELAY_RECONNECT = 1000 * 60 * 2* 1000;
	
	private final static int NALBUFLENGTH = 320 * 480 *2; // 600*800*2

	private static int VIDEOSOCKETBUFLENGTH = 1024 + 31;//  342000;

	private final static int RECEAUDIOBUFFERSIZE = 1600 * Command.CHANEL * 1;
	
	private final static int PLAYBACK_AUDIOBUFFERSIZE = 1024 * Command.CHANEL * 1;

	private Bitmap video;

	byte[] nalBuf = new byte[NALBUFLENGTH];//>100k

	int nalSizeTemp = 0;

	int nalBufUsedLength;

	byte[] videoSocketBuf ;

	byte[] bitmapTmpBuffer = new byte[NALBUFLENGTH];
	
	private int bitmapTmpBufferUsed;

	int readLengthFromVideoSocket = 0;

	int videoSockBufferUsedLength;

	boolean firstStartFlag = false;

	boolean looperFlag = false;

	private boolean stopPlay = true;
	
	private boolean stopRecvAudioFlag = true;

	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE];
	
	private int recfBufferlength = RECEAUDIOBUFFERSIZE/2;
	
	private short[] recfBuffer = new short[recfBufferlength];
	
	private boolean recFlag = false;
	
	private static final String TAG = "MyVideoView";

	private Handler handler;

	private Device device;

	private Rect rect = null;

	private Rect rect2 = null;

	private int frameCount;

	private int frameCountTemp;

	private String deviceId = "";

	private String devicenName = "";
	
	private Paint textPaint;
	
	private Paint bgPaint;
	
	private Paint infoPaint;

	private int temWidth;
	
	private String timeStr = "";

	// private int temHeigth;

	private Matrix matrix = new Matrix();

	private boolean reverseFlag = false;

	private byte [] table2 = null;
	
	private boolean playBackFlag = false;
	
	private boolean initPlayBackParaFlag = true;
	
	//接受回放信息表的buffer
	private byte[] tableBuffer;
	
	private int recvTableIndex = 0;
	
	private int remainTable2Data = 0;
	
	private boolean initTableInfo = true;
	
	private int t1Length = 0;
	
	private int audioBufferUsedLength;
	
	private int rate = 1;
	
	private byte isVideo = 1;

	private int MAX_FRAME = 32;

	private int TOTAL_FRAME_SIZE = 50 * MAX_FRAME;
	
	private byte[] amrBuffer = new byte[TOTAL_FRAME_SIZE];
	
	private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Command.CHANEL * 10;
	
	byte[] pcmArr = new byte[pcmBufferLength];
	
	private boolean openSendAudioFlag = false;
	
	private boolean mpeg4Decoder = false;
	
	private int indexForPut = 0; // put索引 （下一个要写入的位置）
	
	private int indexForGet = 0; // get索引 （下一个要读取的位置）
	
	public MyVideoView(Context context) {
		super(context);
	}

	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		textPaint = new Paint();
		matrix.setScale(-1, 1);
		rect = new Rect(0, 0, getWidth(), getHeight() - 10);
	}

	void init(Handler handler, int w, int h) {
		this.handler = handler;
		textPaint = new Paint();
		textPaint.setColor(Color.RED);
		matrix.setScale(-1, 1);
		rect = new Rect(0, 0, w, h);
		bgPaint = new Paint();
		bgPaint.setColor(Color.WHITE);
		infoPaint = new Paint();
		infoPaint.setColor(Color.BLUE);
		infoPaint.setTextSize(18);
		mpeg4Decoder = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (video != null) {
			if (temWidth != getWidth()) {
				temWidth = getWidth();
				int tmpHeight = getHeight();
				int imageviewWidth = video.getWidth();
				int imageViewHeight = video.getHeight();
				int left = 0;
				int top = 0;
				int right = 0;
				int bottom  = 0;
				if(temWidth>imageviewWidth) {
					left = (temWidth - imageviewWidth)/2;
					right = imageviewWidth;
				} else {
					right = temWidth;
				}
				
				if(tmpHeight > imageViewHeight) {
					top =  (tmpHeight - imageViewHeight)/2;
					bottom = imageViewHeight;
				} else {
					bottom = tmpHeight;
				}
				rect = new Rect(left, top, right + left, bottom + top);
			}
			canvas.drawBitmap(video, null, rect, textPaint);
			canvas.drawText(devicenName + "  " + deviceId + "  "	+ DateUtil.formatTimeStrToTimeStr(timeStr) + "  " + frameCountTemp + " p/s", rect.left + 20, rect.top + 25, textPaint);
		}else {
			String text = "  More : hangzhouiped.taobao.com";
			if(rect2 == null) {
				rect2 = new Rect(0, 0, getWidth(), getHeight() - 10);
			}
			infoPaint.getTextBounds(text, 0, text.length(), rect2);
			int x = getWidth() /2 - rect2.centerX();
			int y = getHeight() /2 - rect2.centerY();
			canvas.drawText(text, x, y, infoPaint);
		}
	}

	public void run() {
		deviceId = device.getDeviceID();
		devicenName = device.getDeviceName();
		nalBufUsedLength = 0;
		videoSockBufferUsedLength = 0;
		audioBufferUsedLength = 0;
		String tem = (CamCmdListHelper.SetCmd_StartVideo_Tcp + device.getUnDefine2() + "\0");
		int res = UdtTools.sendCmdMsg(tem, tem.length());
		if (res < 0) {
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			Log.d(TAG, "sendCmdMsgById result = " + res);
			onStop();
			return;
		}
		int bufLength = 100;
		byte[] b = new byte[bufLength];
		res = UdtTools.recvCmdMsg(b, bufLength);
		if (res < 0) {
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			onStop();
			Log.d(TAG, "recvCmdMsgById result = " + res);
			return;
		}
		String tlk = new String(b,0,3);
		Log.d(TAG, "tlk===" + tlk + " recv length after set trans port type = " + res);
		if("tlk".equalsIgnoreCase(tlk)) {
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			handler.sendEmptyMessage(Constants.WEB_CAM_CHECK_PWD_STATE_MSG);
			UdtTools.freeConnection();
			return;
		}
		BCVInfo info = new BCVInfo(b[3], b[4], b[5]);
		initBCV(info);
		if(res > 6) {
			mpeg4Decoder = true;
		}else {
			mpeg4Decoder = false;
		}
		if(mpeg4Decoder) {
			VIDEOSOCKETBUFLENGTH = 1024;
		} else {
			VIDEOSOCKETBUFLENGTH = 1024 + 31;
		}
		temWidth =  0;
		videoSocketBuf = new byte[VIDEOSOCKETBUFLENGTH];
		if(!playBackFlag) {
			temWidth = 1;
			//RecvAudio recvAudio = new RecvAudio();
			//recvAudioThread = new Thread(recvAudio);
			stopRecvAudioFlag = false;
			//recvAudioThread.start();
			if(mpeg4Decoder) {
				new Thread(new PaserMpeg4()).start();
			}else {
				new Thread(new RecvAudio()).start();
			}
			FileOutputStream fos = null;
			/*try {
				fos = new FileOutputStream("/mnt/sdcard/" + System.currentTimeMillis() +  ".bin");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			//new Thread(new WebCamAudioRecord()).start();
			while (!Thread.currentThread().isInterrupted() && !stopPlay) {
				readLengthFromVideoSocket = UdtTools.recvVideoMsg(videoSocketBuf, VIDEOSOCKETBUFLENGTH);
				//Log.d(TAG, "monitor readLengthFromVideoSocket=" +readLengthFromVideoSocket);
				if (readLengthFromVideoSocket <= 0) { // 读取完成
					System.out.println("read over break....");
					stopPlay = true;
					break;
				}
				/*try {
					fos.write(videoSocketBuf, 0, readLengthFromVideoSocket);
					fos.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				videoSockBufferUsedLength = 0;
				if(mpeg4Decoder) {
					int recvBufIndex = 0;
					do{
						//Log.d(TAG, "### indexForPut = " + ((indexForPut +1) % NALBUFLENGTH));
						if((indexForPut +1) % NALBUFLENGTH == indexForGet) {
							if(BuildConfig.DEBUG && !DEBUG) {
								Log.d(TAG, "### data buffer is full, please get data in time");
							}
							synchronized (nalBuf) {
								try {
									nalBuf.wait(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}else {
							 nalBuf[indexForPut]=videoSocketBuf[recvBufIndex];  
							 indexForPut = (indexForPut+1)%NALBUFLENGTH;  
						     recvBufIndex++;
						    // Log.d(TAG, "### indexForPut = " + indexForPut);
						}
					}while(readLengthFromVideoSocket>recvBufIndex && !stopPlay);
				} else {
					while (readLengthFromVideoSocket - videoSockBufferUsedLength > 0) {
						// remain socket  buf  length
						nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, videoSocketBuf,	videoSockBufferUsedLength, (readLengthFromVideoSocket - videoSockBufferUsedLength));
						// 根据nalSizeTemp的值决定是否刷新界面
						while (looperFlag) {
							looperFlag = false;
							if (nalSizeTemp == -2) {
								if (nalBufUsedLength > 0) {
									frameCount++;
									copyPixl();
								}
								nalBuf[0] = -1;
								nalBuf[1] = -40;
								nalBuf[2] = -1;
								nalBuf[3] = -32; // 刷新界面之后，再将jpeg数据头加入nalbuffer中
								videoSockBufferUsedLength += 4;
								nalBufUsedLength = 4;
								break;
							}
						}
					}
				}
			} 
		}else { // 回放
			initPlayBackParaFlag = true;
			initTableInfo = true;
			while (!Thread.currentThread().isInterrupted() && !stopPlay) {
				readLengthFromVideoSocket = UdtTools.recvVideoMsg(videoSocketBuf, VIDEOSOCKETBUFLENGTH);
				//Log.d(TAG, "playback readLengthFromVideoSocket=" +readLengthFromVideoSocket);
				if (readLengthFromVideoSocket <= 0) { // 读取完成
					System.out.println("read over break....");
					stopPlay = true;
					break;
				}
				if(playBackFlag && initPlayBackParaFlag) {
					initSeekTable();
				} else {
					playBack();
				}
			} 	
		}
		onStop();
		System.out.println("onstop====" + stopPlay);
	}

	private void playBack() {
		videoSockBufferUsedLength = 0;
		while ((readLengthFromVideoSocket - videoSockBufferUsedLength) > 0) {
			// remain socket  buf  length 将视频音频数据合并到缓冲区中
			//Log.d(TAG, "readLengthFromVideoSocket=" + readLengthFromVideoSocket);
			nalSizeTemp = play_back_mergeBuffer(nalBuf, nalBufUsedLength, videoSocketBuf,videoSockBufferUsedLength, (readLengthFromVideoSocket - videoSockBufferUsedLength));
			//Log.d(TAG, "### play_back_mergeBuffer return value = " +nalSizeTemp + " looperFlag=" + looperFlag);
			// 根据nalSizeTemp的值决定是否刷新界面
			while (looperFlag) {
				looperFlag = false;
				if (nalSizeTemp == -2) {
					if (nalBufUsedLength > 0) {
						synchronized (bitmapTmpBuffer) {
							System.arraycopy(nalBuf, 0, bitmapTmpBuffer, 0, nalBufUsedLength);
							bitmapTmpBufferUsed = nalBufUsedLength;
							hasVideoData = true;
							bitmapTmpBuffer.notify();
						}
						frameCount++;
						Message msg = handler.obtainMessage();
						msg.what = Constants.UPDATE_PLAY_BACK_TIME;
						msg.obj = timeStr;
						handler.sendMessage(msg);
						try {
							if(rate !=0) {
								Thread.sleep(1000/rate);
							} else {
								Thread.sleep(1000);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					nalBuf[0] = -1;
					nalBuf[1] = -40;
					nalBuf[2] = -1;
					nalBuf[3] = -32; // 刷新界面之后，再将jpeg数据头加入nalbuffer中
					videoSockBufferUsedLength += 4;
					nalBufUsedLength = 4;
					break;
				}
			}
		}
	}
	
	private int mergeBuffer(byte[] nalBuf, int nalBufUsed, byte[] socketBuf,
			int sockBufferUsed, int socketBufRemain) {

		if(playBackFlag && initPlayBackParaFlag) {
			initPlayBackParaFlag = false;
			byte[] t1 = new byte[4];
			
			System.arraycopy(socketBuf, 0, t1, 0 , t1.length);
			Log.d(TAG, "### 1111 = "  + t1[0] + " " + t1[1] + " " + t1[3] + " " + t1[3]);
			int t1Length =  ByteUtil.byteToInt4(t1,0);
			
			byte[] t2 = new byte[4];
			System.arraycopy(socketBuf, 4, t2, 0, t2.length);
			Log.d(TAG, "### 22222 = "  + t2[0] + " " + t2[1] + " " + t2[3] + " " + t2[3]);
			int t2Length =  ByteUtil.byteToInt4(t2,0);
			Log.d(TAG, "### t1Length=" + t1Length + "  t2Length=" + t2Length);
			if(t2Length<=0 || t2Length > 32 * 1024) {
				//disable seekbar
				handler.sendEmptyMessage(PlayBackConstants.DISABLE_SEEKBAR);
			}else {
				if(t2Length < 32 * 1024) {
					table2 = new byte[t2Length];
					System.arraycopy(socketBuf, 8 + t1Length, table2, 0, t2Length);
					// send table2 info
					Message message = handler.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putByteArray("TABLE2", table2);
					message.setData(bundle);
					message.what = PlayBackConstants.INIT_SEEK_BAR;
					handler.sendMessage(message);
				}
			}
			Log.d(TAG, "index=" + "#" + t1Length +  " " + t2Length);
		}
		int i = 0;
		for (; i < socketBufRemain; i++) {
			if (firstStartFlag && socketBuf[i] == 0 && socketBuf[i + 1] == 0 && socketBuf[i + 2] == 0 && socketBuf[i + 3] == 1 && socketBuf[i + 4] == 12) {
				firstStartFlag = false;
				looperFlag = true;
				if(playBackFlag) {
					//byte[] rateByte = new byte[8];
					//System.arraycopy(socketBuf, sockBufferUsed+i+49, rateByte, 0, rateByte.length);
					if((sockBufferUsed+i+49+8)<=socketBufRemain) {
						String rateStr = new String(socketBuf,sockBufferUsed+i+49,8);
						if(rateStr.matches("\\d+")) {
							rate = 1000*1000/Integer.parseInt(rateStr);
						}
						//Log.d(TAG, "### video start flag = " + rateStr + " rate = " + rate);
					}
				}
				videoSockBufferUsedLength += 65;
				return -1;
			} else if (socketBuf[i + sockBufferUsed] == -1
					&& socketBuf[i + 1 + sockBufferUsed] == -40
					&& socketBuf[i + 2 + sockBufferUsed] == -1
					&& socketBuf[i + 3 + sockBufferUsed] == -32) { // 每检测到jpeg的开头刷新图片
				looperFlag = true;
				return -2;
			}  else {
					if(socketBuf[sockBufferUsed] == 0 && socketBuf[sockBufferUsed + 1] == 0
							&& socketBuf[sockBufferUsed + 2] == 0 && socketBuf[sockBufferUsed + 3] == 1 && socketBuf[i + 4] == 12) {
						if(!playBackFlag && (sockBufferUsed+5+14) <= socketBufRemain) { 
							timeStr = new String(socketBuf, sockBufferUsed+5,14);
						}
						//Log.d(TAG, "====" + timeStr);
					}
				nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
				nalBufUsedLength++;
				videoSockBufferUsedLength++;
			}
		}
		looperFlag = true;
		//System.out.println("monitor videoSockBufferUsedLength = " + videoSockBufferUsedLength);
		return i;
	}
		
	private int play_back_mergeBuffer(byte[] nalBuf, int nalBufUsed, byte[] socketBuf,
			int sockBufferUsed, int videoSocketBufRemain) {
		int i = 0;
		for (; i < videoSocketBufRemain; i++) {
			if ( socketBuf[i] == 0 && socketBuf[i + 1] == 0 && socketBuf[i + 2] == 0 && socketBuf[i + 3] == 1 && socketBuf[i + 4] == 12) {
				looperFlag = true;
				if(i+9+14<=videoSocketBufRemain){
					timeStr =  new String(socketBuf, i + 9, 14);
				}
				if(i+6<=videoSocketBufRemain) {
					isVideo = socketBuf[i + 6];
				}
				if(i+23+8<=videoSocketBufRemain) {
					String rateStr = new String(socketBuf, i + 23, 8);
					if(rateStr.matches("\\d+")) {
						rate = 1000*1000/Integer.parseInt(rateStr);
					}
					//Log.d(TAG, "### video start flag = " + rateStr + " rate = " + rate);
				}
			}			
			if (isVideo == 2) {
					if(socketBuf[i + sockBufferUsed] == 60) {
						if(audioBufferUsedLength >= TOTAL_FRAME_SIZE) {
							//Log.d(TAG, "### audioBufferUsedLength=" + audioBufferUsedLength);
							audioTmpBufferUsed = audioBufferUsedLength;
							audioBufferUsedLength = 0;
							synchronized (audioTmpBuffer) {
								System.arraycopy(amrBuffer, 0, audioTmpBuffer, 0, audioTmpBufferUsed);
								hasAudioData = true;
								audioTmpBuffer.notify();
							}
						}
						int audioHeadIndex = videoSocketBufRemain - i;
						if(audioHeadIndex>=32) {
							System.arraycopy(socketBuf,i + sockBufferUsed, amrBuffer, audioBufferUsedLength, 32);
							audioBufferUsedLength += 32;
							i += 31;
							videoSockBufferUsedLength+=32;
						}else {
							i += (audioHeadIndex-1);
							videoSockBufferUsedLength+=audioHeadIndex;
						}
					}else {
						videoSockBufferUsedLength++;
					}
			}else {
				if(socketBuf[i + sockBufferUsed] == -1
						&& socketBuf[i + 1 + sockBufferUsed] == -40
						&& socketBuf[i + 2 + sockBufferUsed] == -1
						&& socketBuf[i + 3 + sockBufferUsed] == -32) { // video
						looperFlag = true;
						return -2;
					}else {
						nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
						nalBufUsedLength++;
						videoSockBufferUsedLength++;
					}
			}
		}
		looperFlag = true;
		return i;
	}
	
	public void copyPixl() {
		if (reverseFlag) {
			Bitmap tmp = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
			if (tmp != null) {
				video = Bitmap.createBitmap(tmp, 0, 0, tmp.getWidth(),
						tmp.getHeight(), matrix, true);
				matrix.setRotate(180);
				postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
				if (!tmp.isRecycled()) {
					tmp.recycle();
				}
			}
		} else {
			video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
			postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
		}
	}

	public boolean takePic() {
		if(null != video){
			return FileUtil.takePicture(video, timeStr +".jpg");
		}
		return false;
	}
	public void onStop() {
		if(!playBackFlag){
			handler.removeMessages(Constants.WEB_CAM_RECONNECT_MSG);
			handler.sendEmptyMessageDelayed(Constants.WEB_CAM_RECONNECT_MSG, DELAY_RECONNECT);
		}
		release();
		flushBitmap();
	}

	public boolean isStopPlay() {
		return stopPlay;
	}

	public void setStopPlay(boolean stopPlay) {
		this.stopPlay = stopPlay;
		stopRecvAudioFlag = stopPlay;
		if(recvAudioThread != null && recvAudioThread.isInterrupted()) {
			System.out.println("---------- interrutp.");
			recvAudioThread.interrupt();
			recvAudioThread = null;
		}
	}

	private void release() {
		handler.removeCallbacks(calculateFrameTask);
		handler.sendEmptyMessage(PlayBackConstants.DISABLE_SEEKBAR);
		handler.sendEmptyMessage(Constants.WEB_CAM_CONNECT_INIT_MSG);
		UdtTools.exit();
	}

	private void flushBitmap() {
		if(video != null && !video.isRecycled()) {
			video.recycle();
			video = null;
		}
		postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
	}

	public void onStart() {
		stopPlay = false;
	}

	public boolean getPlayStatus() {
		return stopPlay;
	}

	public boolean isReverseFlag() {
		return reverseFlag;
	}

	public void setReverseFlag(boolean reverseFlag) {
		this.reverseFlag = reverseFlag;
	}

	public void setPlayBackFlag(boolean playBackFlag) {
		this.playBackFlag = playBackFlag;
	}

	public boolean isPlayBackFlag() {
		return playBackFlag;
	}
	
	private boolean hasAudioData;
	
	private boolean hasVideoData;
	
	private class PlayBackVideo implements Runnable {
		
		public PlayBackVideo() {
			
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(hasVideoData) {
					hasVideoData = false;
					if (reverseFlag) {
						Bitmap tmp = BitmapFactory.decodeByteArray(bitmapTmpBuffer, 0, bitmapTmpBufferUsed);
						if (tmp != null) {
							video = Bitmap.createBitmap(tmp, 0, 0, tmp.getWidth(),
									tmp.getHeight(), matrix, true);
							matrix.setRotate(180);
							if (!tmp.isRecycled()) {
								tmp.recycle();
							}
						}
					} else {
						video = BitmapFactory.decodeByteArray(bitmapTmpBuffer, 0, bitmapTmpBufferUsed);
					}
					if(video != null) {
						postInvalidate();
					}
				}else {
					synchronized (bitmapTmpBuffer) {
						try {
							bitmapTmpBuffer.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
 
	private byte[] audioTmpBuffer = new byte[PLAYBACK_AUDIOBUFFERSIZE * 10];
	
	private int audioTmpBufferUsed;
	
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
	
	private int zeroIndex32 = 0;
	
	class RecvAudio implements Runnable {

		private AudioTrack m_out_trk = null;

		public RecvAudio() {

		}

		@Override
		public void run() {
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
					AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size * 10,
					AudioTrack.MODE_STREAM);
			m_out_trk.play();
			int recvDataLength = -1;
			while (!stopRecvAudioFlag && !stopPlay) {
				recvDataLength = UdtTools.recvAudioMsg(RECEAUDIOBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
				//Log.d(TAG, "audio recv audio DataLength===" + recvDataLength);
				if(recvDataLength<=0) {
					Log.d(TAG, "### audio recv audio over");
					stopRecvAudioFlag = true;
					break;
				}
				/*if(recFlag) {
					synchronized (recfBuffer) {
						recFlag = false;
					}
					ByteUtil.bytesToShorts(audioBuffer,RECEAUDIOBUFFERSIZE, recfBuffer);//转换参考数据
				}else {
					synchronized (recfBuffer) {
						try {
							//Log.d(TAG, "### audio recv audio wait. ###");
							recfBuffer.wait();
						} catch (InterruptedException e) {
							stopRecvAudioFlag = true;
							e.printStackTrace();
						}
					}
				}*/
				zeroIndex32 = 0;
				for(int i = 0; i<recvDataLength; i++) {
					if(audioBuffer[i]==0) {
						zeroIndex32++;
						if(zeroIndex32 == 32) {
							stopRecvAudioFlag = true;
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
			Log.d(TAG, "### audio amr decoder exit.");
		}

	}
	
	private class WebCamAudioRecord implements Runnable {
		
		private static final int RECORDER_BPP = 16;
		
		private static final int frequency = 8000;
		
		private static final int EncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
		 
		protected int miniRecoderBufSize;
		
		private int pcmBufferLength = 0;
		
		private int amrBufferLength = 0;
		
		private int sendAudioToCamLength = 0;
		
		private byte[] sendAudioBufferToCam;
		
		private int index = 0;
		
		protected AudioRecord audioRecord;

		private byte[] amrBuffer;
		
		//private short[] pcmBuffer;
		
		private byte[] micBuffer; //
		
		private WebCamAudioRecord() {
			Speex.initEcho(160, 160*10);
			UdtTools.initAmrEncoder();
			createAudioRecord();
			pcmBufferLength = RECEAUDIOBUFFERSIZE;
			amrBufferLength = RECEAUDIOBUFFERSIZE/10;
			sendAudioToCamLength = amrBufferLength * 2;
			//pcmBuffer = new short[pcmBufferLength];
			micBuffer = new byte[pcmBufferLength];
			amrBuffer = new byte[amrBufferLength];
			sendAudioBufferToCam = new byte[sendAudioToCamLength];
		}
		
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
			  //byte data[] = new byte[miniRecoderBufSize];
              int read = 0;
              while(!stopPlay && openSendAudioFlag){
                      read = audioRecord.read(micBuffer, 0, pcmBufferLength);
                      //Log.d(TAG, "### recording recFlag " + recFlag + " read= " + read + " openSendAudioFlag=" + openSendAudioFlag);
                      if(AudioRecord.ERROR_INVALID_OPERATION == read){
                    	  Log.d(TAG, "### recorder over !");
                    	  break;
                      }
                	  // Speex.cancellation(micBuffer, recfBuffer, amrBuffer);
                      if(UdtTools.EncoderPcm(micBuffer, pcmBufferLength, amrBuffer, amrBufferLength)==0){
                    	  UdtTools.sendAudioMsg(amrBuffer, amrBufferLength);
                      }
                      //ByteUtil.shortsToBytes(amrBuffer, amrBufferLength, sendAudioBufferToCam);
                      //Log.d(TAG, "### send audio length = " + res);
              }
              stopRecording();
		  }
	
		  private void stopRecording(){
			  Log.d(TAG, "### stop recording !");
			  openSendAudioFlag = false;
			 // Speex.stopEcho();
              if(null != audioRecord){
                      audioRecord.stop();
                      audioRecord.release();
                      audioRecord = null;
              }
      }
	}
	
	public void setDevice(Device device) {
		this.device = device;
	}

	private Runnable calculateFrameTask = new Runnable() {

		@Override
		public void run() {
			frameCountTemp = frameCount;
			if(frameCountTemp<=2) {
				if(video != null) {
					invalidate();
				}
			}
			frameCount = 0;
			handler.postDelayed(calculateFrameTask, 1000);
		}
	};

	private void initBCV(BCVInfo info) {
		Message m = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putSerializable("UPDATEBCV", info);
		m.what = Constants.SEND_UPDATE_BCV_INFO_MSG;
		m.setData(bundle);
		handler.sendMessage(m);
		stopPlay = false;
		handler.removeCallbacks(calculateFrameTask);
		handler.post(calculateFrameTask);
		handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		handler.removeMessages(Constants.WEB_CAM_RECONNECT_MSG);
		Log.d(TAG, "### playBackFlag = " + playBackFlag);
		firstStartFlag = true;
		timeStr = "";
		nalBufUsedLength = 0;
		bitmapTmpBufferUsed = 0;
		audioTmpBufferUsed = 0;
	}
	
	private void initSeekTable() {
		if(initTableInfo) { // 初始化回放表的长度信息 仅且执行一次
			new Thread(new PlayBackVideo()).start();
			new Thread(new PalyBackAudio()).start();
			initTableInfo = false;
			byte[] t1 = new byte[4];
			System.arraycopy(videoSocketBuf, 0, t1, 0 , t1.length);
			Log.d(TAG, "### 1111 = "  + t1[0] + " " + t1[1] + " " + t1[3] + " " + t1[3]);
			t1Length =  ByteUtil.byteToInt4(t1,0);
			
			byte[] t2 = new byte[4];
			System.arraycopy(videoSocketBuf, 4, t2, 0, t2.length);
			Log.d(TAG, "### 22222 = "  + t2[0] + " " + t2[1] + " " + t2[3] + " " + t2[3]);
			int t2Length =  ByteUtil.byteToInt4(t2,0);
			Log.d(TAG, "### t1Length=" + t1Length + "  t2Length=" + t2Length);
			
			if(t2Length<=0 || t2Length > 32 * 1024) {
				initPlayBackParaFlag = false;
				//disable seekbar
				handler.sendEmptyMessage(PlayBackConstants.DISABLE_SEEKBAR);
			}
			if(8+t1Length+t2Length<=readLengthFromVideoSocket) {// 数据一次接收完毕
				initPlayBackParaFlag = false;
				table2 = new byte[t2Length];
				System.arraycopy(videoSocketBuf, 8 + t1Length, table2, 0, t2Length);
				// send table2 info
				Message message = handler.obtainMessage();
				Bundle b2 = new Bundle();
				b2.putByteArray("TABLE2", table2);
				message.setData(b2);
				message.what = PlayBackConstants.INIT_SEEK_BAR;
				handler.sendMessage(message);
				
				for(int i=8+t1Length+t2Length;i<readLengthFromVideoSocket;i++) {
					if ( videoSocketBuf[i] == 0 && videoSocketBuf[i + 1] == 0 && videoSocketBuf[i + 2] == 0 && 
							videoSocketBuf[i + 3] == 1 && videoSocketBuf[i + 4] == 12) {
						looperFlag = true;
						if(i+13<=readLengthFromVideoSocket){
							timeStr =  new String(videoSocketBuf, i + 13, 14);
						}
						if(i+6<=readLengthFromVideoSocket) {
							isVideo = videoSocketBuf[i + 6];
						}
						if(isVideo == 2) {
							if(videoSocketBuf[i] == 60) {
								if(audioBufferUsedLength >= TOTAL_FRAME_SIZE) {
									//Log.d(TAG, "### audioBufferUsedLength=" + audioBufferUsedLength);
									audioTmpBufferUsed = audioBufferUsedLength;
									audioBufferUsedLength = 0;
									synchronized (audioTmpBuffer) {
										System.arraycopy(amrBuffer, 0, audioTmpBuffer, 0, audioTmpBufferUsed);
										hasAudioData = true;
										//audioTmpBuffer.notify();
									}
								}
								int audioHeadIndex = readLengthFromVideoSocket - i;
								if(audioHeadIndex>=32) {
									System.arraycopy(videoSocketBuf,i, amrBuffer, audioBufferUsedLength, 32);
									audioBufferUsedLength += 32;
									i += 31;
									videoSockBufferUsedLength+=32;
								}else {
									i += (audioHeadIndex-1);
									videoSockBufferUsedLength+=audioHeadIndex;
								}
							}else {
								videoSockBufferUsedLength++;
							}
						} else {
							if(videoSocketBuf[i] == -1
									&& videoSocketBuf[i + 1] == -40
									&& videoSocketBuf[i + 2] == -1
									&& videoSocketBuf[i + 3] == -32) { // video
								looperFlag = true;
								nalBuf[0] = -1;
								nalBuf[1] = -40;
								nalBuf[2] = -1;
								nalBuf[3] = -32; // 刷新界面之后，再将jpeg数据头加入nalbuffer中
								videoSockBufferUsedLength += 4;
								nalBufUsedLength = 4;
							}else {
								nalBuf[nalBufUsedLength++] = videoSocketBuf[i];
								videoSockBufferUsedLength++;
							}
						}
					}
				}
			} else {//
				tableBuffer = new byte[t2Length];
				recvTableIndex = readLengthFromVideoSocket - (8 + t1Length); //第一次复制的数据长度
				System.arraycopy(videoSocketBuf, 8 + t1Length, tableBuffer, 0, recvTableIndex);
				remainTable2Data = t2Length - recvTableIndex;// 还需要拷贝的数据长度
				Log.d(TAG, "remainTable2Data=" + remainTable2Data + " recvTableIndex=" + recvTableIndex);
				return;
			}
		}
		
		while(initPlayBackParaFlag) {
			if(remainTable2Data > readLengthFromVideoSocket) {//还未接收完毕
				Log.d(TAG, "buffersize=" + tableBuffer.length + " recvTableIndex=" + recvTableIndex);
				System.arraycopy(videoSocketBuf, 0, tableBuffer, recvTableIndex, readLengthFromVideoSocket);
				remainTable2Data-=readLengthFromVideoSocket;
				recvTableIndex+=readLengthFromVideoSocket;
				break;
			}else {/// 多次接收完毕
				System.arraycopy(videoSocketBuf, 0, tableBuffer, recvTableIndex, remainTable2Data);
				initPlayBackParaFlag = false;
				Message message = handler.obtainMessage();
				Bundle b2 = new Bundle();
				b2.putByteArray("TABLE2", tableBuffer);
				message.setData(b2);
				message.what = PlayBackConstants.INIT_SEEK_BAR;
				handler.sendMessage(message);
				for(int i=remainTable2Data;i<readLengthFromVideoSocket;i++) {
					if ( videoSocketBuf[i] == 0 && videoSocketBuf[i + 1] == 0 && videoSocketBuf[i + 2] == 0 && 
							videoSocketBuf[i + 3] == 1 && videoSocketBuf[i + 4] == 12) {
						looperFlag = true;
						if(i+13<=readLengthFromVideoSocket){
							timeStr =  new String(videoSocketBuf, i + 13, 14);
						}
						if(i+6<=readLengthFromVideoSocket) {
							isVideo = videoSocketBuf[i + 6];
						}
						if(isVideo == 2) {
							if(videoSocketBuf[i] == 60) {
								if(audioBufferUsedLength >= TOTAL_FRAME_SIZE) {
									//Log.d(TAG, "### audioBufferUsedLength=" + audioBufferUsedLength);
									audioTmpBufferUsed = audioBufferUsedLength;
									audioBufferUsedLength = 0;
									synchronized (audioTmpBuffer) {
										System.arraycopy(amrBuffer, 0, audioTmpBuffer, 0, audioTmpBufferUsed);
										hasAudioData = true;
										audioTmpBuffer.notify();
									}
								}
								int audioHeadIndex = readLengthFromVideoSocket - i;
								if(audioHeadIndex>=32) {
									System.arraycopy(videoSocketBuf,i, amrBuffer, audioBufferUsedLength, 32);
									audioBufferUsedLength += 32;
									i += 31;
									videoSockBufferUsedLength+=32;
								}else {
									i += (audioHeadIndex-1);
									videoSockBufferUsedLength+=audioHeadIndex;
								}
							}else {
								videoSockBufferUsedLength++;
							}
						} else {
							if(videoSocketBuf[i] == -1
									&& videoSocketBuf[i + 1] == -40
									&& videoSocketBuf[i + 2] == -1
									&& videoSocketBuf[i + 3] == -32) { // video
								looperFlag = true;
								nalBuf[0] = -1;
								nalBuf[1] = -40;
								nalBuf[2] = -1;
								nalBuf[3] = -32; // 刷新界面之后，再将jpeg数据头加入nalbuffer中
								videoSockBufferUsedLength += 4;
								nalBufUsedLength = 4;
							} else {
								nalBuf[nalBufUsedLength++] = videoSocketBuf[i];
								videoSockBufferUsedLength++;
							}
						}
					}
				}
				
				break;
			}
		}
	}
	
	
	public void setTime(String timeStr) {
		this.timeStr = timeStr;
	}

	public void setOpenSendAudioFlag(boolean openSendAudioFlag) {
		this.openSendAudioFlag = openSendAudioFlag;
		System.out.println("openSendAudioFlag=" + openSendAudioFlag);
		if(openSendAudioFlag) {
			new Thread(new WebCamAudioRecord()).start();
		}
	}
	
	
	class PaserMpeg4 implements Runnable {
		
		/**
		 * 1、首先判断是否以0001c开头
		 * 		a、是 则截取时间戳
		 * 		b、否进行 2
		 * 2、再判断是否是FFD8FFE0开头
		 * 		a、是 则拷贝数据jpeg缓冲区
		 * 		b、否 则拷贝数据至mpeg4 缓冲区
		 * 
		 */
		private VideoQueue queue = null;
		
		private int startFlagCount = 1;
		
		@Override
		public void run() {
			int res = UdtTools.initXvidDecorer();
			if(res != 0) {
				stopPlay = true;
				Log.d(TAG, "xvid init decoder error " + res);
				return ;
			}
			byte[] rgbDataBuf = null;
			int length = 1 * 1024 * 1024;
			byte[] mpegBuf = new byte[length];
			byte[] transBuf = new byte[length];
			int readLength = 0;
			int usedBytes = length;
			int unusedBytes = 0;
			int ableReadSize = 0;
			boolean startFlag = false;
			
			int headFlagCount = 0;
			
			int jpegByteBufLength = 2 * 200 * 1024;
			
			byte[] jpegByteBuf = new byte[jpegByteBufLength]; 
			
			int jpegBufUsed = 0;
			
			int tmpJpgBufUsed = 0;
			
			String time = "";
			
			boolean imageDataStart = false;
			
			queue = new VideoQueue();
			
			while(!stopPlay) {
				do{
					if((indexForGet+5)%NALBUFLENGTH == indexForPut){
						synchronized (mpegBuf) {
							if(BuildConfig.DEBUG && !DEBUG) {
								Log.d(TAG, "### data buffer is empty! ---->");
							}
							try {
								mpegBuf.wait(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}  
					} else {
						byte b0 = nalBuf[indexForGet];
						byte b1 = nalBuf[(indexForGet+1)%NALBUFLENGTH];
						byte b2 = nalBuf[(indexForGet+2)%NALBUFLENGTH];
						byte b3 = nalBuf[(indexForGet+3)%NALBUFLENGTH];
						byte b4 = nalBuf[(indexForGet+4)%NALBUFLENGTH];
						if(b0 == 0 && b1 == 0 && b2 == 0 && b3 == 1 && b4 == 12 ) { // 0001C
							if(startFlagCount++%5==0) {
								startFlagCount = 1;
								break;
							}
							mpegBuf[ableReadSize] = b0;
							mpegBuf[ableReadSize + 1] = b1;
							mpegBuf[ableReadSize + 2] = b2;
							mpegBuf[ableReadSize + 3] = b3;
							mpegBuf[ableReadSize + 4] = b4;
							ableReadSize += 5;
							indexForGet+=4;
							startFlag = true;
							imageDataStart = false;
							headFlagCount = 0;
							if(BuildConfig.DEBUG && !DEBUG) {
								Log.d(TAG, "### data start flag ->" + b0 + "  " + b1 + " " + b2 + " " + b3 + " " + b4);
							}
						}else if(b0 == -1 &&  b1 == -40 &&  b2 == -1 && b3 == -37) {
							jpegByteBuf[jpegBufUsed++] = -1;
							startFlag = false;
							imageDataStart = true;
							jpegBufUsed = 1;
							Bitmap v = BitmapFactory.decodeByteArray(jpegByteBuf, 0, tmpJpgBufUsed);
							if(v != null) {
								//postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
								frameCount++;
								queue.addNewImage(new Image(v, time));
							}
						}else {
							if(startFlag) {
								if(headFlagCount >= 18) { 
									startFlag = false;
									time = new String(mpegBuf, ableReadSize-18, 18);
									if(BuildConfig.DEBUG && !DEBUG) {
										Log.d(TAG, "###  time=" + time);
									}
								} else {
									mpegBuf[ableReadSize++] = nalBuf[indexForGet];
									headFlagCount++;
								}
							} else {
								if(imageDataStart) {
									jpegByteBuf[jpegBufUsed++] = b0;
									tmpJpgBufUsed = jpegBufUsed;
								}else {
									mpegBuf[ableReadSize++] = b0;
								}
							}
						} 
						indexForGet = (indexForGet + 1)%NALBUFLENGTH;  
					}
				} while (ableReadSize<usedBytes && !stopPlay);
				startFlag = false;
				readLength = ableReadSize;
				System.arraycopy(mpegBuf, 0, transBuf, unusedBytes, readLength);
				if(rgbDataBuf == null) {
					int[] headInfo = UdtTools.initXvidHeader(transBuf,(readLength + unusedBytes));
					int imageWidth = headInfo[0];
					int imageHeight = headInfo[1];
					usedBytes = headInfo[2];
					unusedBytes = (readLength - usedBytes);
					System.arraycopy(transBuf, usedBytes, transBuf, 0, unusedBytes);
					Log.d(TAG, "### imageWidht = " + imageWidth + " imageWidht = " + imageHeight + " used_bytes = " + usedBytes);
					rgbDataBuf = new byte[imageWidth * imageHeight * 4];
					video = Bitmap.createBitmap(imageWidth, imageHeight, Config.RGB_565);
					postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
				} else {
					usedBytes = UdtTools.xvidDecorer(transBuf,(readLength + unusedBytes), rgbDataBuf);
					ByteBuffer sh = ByteBuffer.wrap(rgbDataBuf);
					if(video != null) {
						Image image = queue.getFirstImage();
						if(image != null && image.time.compareTo(time)<0) {
							video = image.bitmap;
							postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
							frameCount++;
							queue.removeImage();
						}
						video.copyPixelsFromBuffer(sh);
						postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
						frameCount++;
					}
					unusedBytes = (readLength + unusedBytes - usedBytes);
					System.arraycopy(transBuf, usedBytes, transBuf, 0, unusedBytes);
				}
				ableReadSize = 0;
			}
			UdtTools.freeDecorer();
		}
	}
}
