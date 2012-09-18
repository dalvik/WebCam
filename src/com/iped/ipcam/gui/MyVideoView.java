package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PlayBackConstants;

public class MyVideoView extends ImageView implements Runnable {

	private final static int DELAY_RECONNECT = 1000 * 60 * 2;
	
	private final static int NALBUFLENGTH = 320 * 480 *2; // 600*800*2

	private final static int VIDEOSOCKETBUFLENGTH = 642000;//342000;

	private final static int RECEAUDIOBUFFERSIZE = 1024 * Command.CHANEL * 1;

	private Bitmap video;

	byte[] pixel = new byte[NALBUFLENGTH];

	byte[] nalBuf = new byte[NALBUFLENGTH];//

	int nalSizeTemp = 0;

	int nalBufUsedLength;

	byte[] videoSocketBuf ;

	int readLengthFromVideoSocket = 0;

	int videoSockBufferUsedLength;

	boolean firstStartFlag = true;

	boolean looperFlag = false;

	private boolean stopPlay = true;

	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE * 10];

	private static final String TAG = "MyVideoView";

	private Handler handler;

	private Device device;

	private Rect rect = null;

	private Rect rect2 = null;

	private int frameCount;

	private int frameCountTemp;

	private static int timeUpdate = 0;
	
	private String deviceId = "";

	private Paint textPaint;
	
	private Paint bgPaint;
	
	private Paint infoPaint;

	private int temWidth;
	
	private String timeStr = "";

	// private int temHeigth;

	private Matrix m = new Matrix();

	private boolean reverseFlag = false;

	private byte [] table2 = null;
	
	private boolean playBackFlag = false;
	
	private boolean initPlayBackParaFlag = true;
	
	private int frameRate =0;
	
	//接受回放信息表的buffer
	private byte[] tableBuffer;
	
	private int recvTableIndex = 0;
	
	private int remainTable2Data = 0;
	
	private boolean initTableInfo = true;
	
	private int t1Length = 0;
	
	private int t2Length = 0;
	
	private int audioBufferUsedLength;
	
	private int rate = 1;
	
	private byte isVideo = 0;
	
	private AudioTrack m_out_trk = null;
	
	private int pcmBufferLength = audioBuffer.length * Command.CHANEL * 10;
	
	byte[] pcmArr = new byte[pcmBufferLength];

	
	public MyVideoView(Context context) {
		super(context);
	}

	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		textPaint = new Paint();
		m.setScale(-1, 1);
		rect = new Rect(0, 0, getWidth(), getHeight() - 10);
	}

	void init(Handler handler, int w, int h) {
		this.handler = handler;
		textPaint = new Paint();
		textPaint.setColor(Color.BLUE);
		m.setScale(-1, 1);
		rect = new Rect(0, 0, getWidth(), getHeight() - 10);
		bgPaint = new Paint();
		bgPaint.setColor(Color.WHITE);
		infoPaint = new Paint();
		infoPaint.setColor(Color.BLUE);
		infoPaint.setTextSize(18);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (video != null) {
			if (temWidth != getWidth()) {
				temWidth = getWidth();
				rect = new Rect(0, 0, getWidth(), getHeight() - 10);
			}
			canvas.drawBitmap(video, null, rect, textPaint);
			canvas.drawText(deviceId + "  "	+ timeStr + "  " + frameCountTemp + " p/s", 20, 25, textPaint);
		}else {
			String text = "More : hangzhouiped.taobao.com";
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
		nalBufUsedLength = 0;
		videoSockBufferUsedLength = 0;
		audioBufferUsedLength = 0;
		String tem = (CamCmdListHelper.SetCmd_StartVideo_Tcp + device.getUnDefine2() + "\0");
		int res = UdtTools.sendCmdMsgById(deviceId, tem, tem.length());
		if (res < 0) {
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			Log.d(TAG, "sendCmdMsgById result = " + res);
			onStop();
			return;
		}
		int bufLength = 10;
		byte[] b = new byte[bufLength];
		res = UdtTools.recvCmdMsgById(deviceId,b, bufLength);
		if (res < 0) {
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			onStop();
			Log.d(TAG, "recvCmdMsgById result = " + res);
			return;
		}
		BCVInfo info = new BCVInfo(b[3], b[4], b[5]);
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
		timeUpdate = 0;
		timeStr = "";
		videoSocketBuf = new byte[VIDEOSOCKETBUFLENGTH];
		if(!playBackFlag) {
			new Thread(new RecvAudio()).start();
			while (!Thread.currentThread().isInterrupted() && !stopPlay) {
				readLengthFromVideoSocket = UdtTools.recvVideoMsg(videoSocketBuf, VIDEOSOCKETBUFLENGTH);
				if (readLengthFromVideoSocket <= 0) { // 读取完成
					System.out.println("read over break....");
					break;
				}
				videoSockBufferUsedLength = 0;
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
								timeUpdate+=1000/rate;
								if(timeUpdate%1000 == 0) {
									handler.sendEmptyMessage(Constants.UPDATE_PLAY_BACK_TIME);
									timeUpdate = 0;
								}
								try {
									Thread.sleep(1000/rate - 3);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								//firstStartFlag = true;
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
		}else { // 回放
			initPlayBackParaFlag = true;
			initTableInfo = true;
			while (!Thread.currentThread().isInterrupted() && !stopPlay) {
				readLengthFromVideoSocket = UdtTools.recvVideoMsg(videoSocketBuf, VIDEOSOCKETBUFLENGTH);
				//Log.d(TAG, "readLengthFromVideoSocket=" + readLengthFromVideoSocket);
				if (readLengthFromVideoSocket <= 0) { // 读取完成
					System.out.println("read over break....");
					break;
				}
				if(playBackFlag && initPlayBackParaFlag) {
					if(initTableInfo) { // 初始化回放表的长度信息 仅且执行一次
						//TODO
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
						} else {//
							tableBuffer = new byte[t2Length];
							recvTableIndex = readLengthFromVideoSocket - (8 + t1Length); //第一次复制的数据长度
							System.arraycopy(videoSocketBuf, 8 + t1Length, tableBuffer, 0, recvTableIndex);
							remainTable2Data = t2Length - recvTableIndex;// 还需要拷贝的数据长度
							Log.d(TAG, "remainTable2Data=" + remainTable2Data + " recvTableIndex=" + recvTableIndex);
							continue;
						}
					}
					
					while(initPlayBackParaFlag) {
						if(remainTable2Data>readLengthFromVideoSocket) {//还未接收完毕
							Log.d(TAG, "buffersize=" + tableBuffer.length + " recvTableIndex=" + recvTableIndex);
							System.arraycopy(videoSocketBuf, 0, tableBuffer, recvTableIndex, readLengthFromVideoSocket);
							remainTable2Data-=readLengthFromVideoSocket;
							recvTableIndex+=readLengthFromVideoSocket;
							break;
						}else {
							System.arraycopy(videoSocketBuf, 0, tableBuffer, recvTableIndex, remainTable2Data);
							initPlayBackParaFlag = false;
							Message message = handler.obtainMessage();
							Bundle b2 = new Bundle();
							b2.putByteArray("TABLE2", tableBuffer);
							message.setData(b2);
							message.what = PlayBackConstants.INIT_SEEK_BAR;
							handler.sendMessage(message);
							break;
						}
					}
				} else {
					videoSockBufferUsedLength = 0;
					while (readLengthFromVideoSocket - videoSockBufferUsedLength > 0) {
						// remain socket  buf  length 将视频音频数据合并到缓冲区中
						nalSizeTemp = play_back_mergeBuffer(nalBuf, nalBufUsedLength, audioBufferUsedLength, videoSocketBuf,videoSockBufferUsedLength, (readLengthFromVideoSocket - videoSockBufferUsedLength));
						//Log.d(TAG, "### nalSizeTemp = " +nalSizeTemp);
						// 根据nalSizeTemp的值决定是否刷新界面
						while (looperFlag) {
							looperFlag = false;
							if (nalSizeTemp == -2) {
								if (nalBufUsedLength > 0) {
									frameCount++;
									copyPixl();
									timeUpdate+=1000/rate;
									if(timeUpdate%1000 == 0) {
										handler.sendEmptyMessage(Constants.UPDATE_PLAY_BACK_TIME);
										timeUpdate = 0;
									}
									try {
										Thread.sleep(1000/rate - 3);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									//firstStartFlag = true;
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
		}
		onStop();
		System.out.println("onstop====" + stopPlay);
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
					byte[] rateByte = new byte[8];
					System.arraycopy(socketBuf, sockBufferUsed+i+49, rateByte, 0, rateByte.length);
					String rateStr = new String(rateByte);
					if(rateStr.matches("\\d+")) {
						rate = 1000*1000/Integer.parseInt(rateStr);
					}
					Log.d(TAG, "### video start flag = " + rateStr + " rate = " + rate);
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
						if(!playBackFlag) { 
							timeStr = new String(socketBuf, sockBufferUsed+5,14);
						}else { // 回放
							// timeStr = new String(socketBuf, sockBufferUsed+9,14);
							//Log.d(TAG, "====" + new String(socketBuf, i+sockBufferUsed+9,14));
						}
						Log.d(TAG, "====" + new String(socketBuf, i + sockBufferUsed + 9, 14));
					}
				nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
				nalBufUsedLength++;
				videoSockBufferUsedLength++;
			}
		}
		looperFlag = true;
		return i;
	}
	
	private int play_back_mergeBuffer(byte[] nalBuf, int nalBufUsed, int audioBufUsed, byte[] socketBuf,
			int sockBufferUsed, int videoSocketBufRemain) {
		int i = 0;
		for (; i < videoSocketBufRemain; i++) {
			if ( socketBuf[i] == 0 && socketBuf[i + 1] == 0 && socketBuf[i + 2] == 0 && socketBuf[i + 3] == 1 && socketBuf[i + 4] == 12) {
				looperFlag = true;
				timeStr =  new String(socketBuf, i + 9, 14);
				isVideo = socketBuf[i + 6];
			}			
			if(isVideo == 1) {
				if(socketBuf[i + sockBufferUsed] == -1
						&& socketBuf[i + 1 + sockBufferUsed] == -40
						&& socketBuf[i + 2 + sockBufferUsed] == -1
						&& socketBuf[i + 3 + sockBufferUsed] == -32) { // video
						looperFlag = true;
						hasAudioData = true;
						Log.d(TAG, "audioBufferUsedLength=" + audioBufferUsedLength);
						//UdtTools.amrDecoder(audioBuffer, audioBufferUsedLength, pcmArr, 0, Command.CHANEL);
						//m_out_trk.write(pcmArr, 0, pcmBufferLength);
						return -2;
					}else {
						nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
						nalBufUsedLength++;
						videoSockBufferUsedLength++;
					}
			} else if (isVideo == 2) {
				if(socketBuf[i + sockBufferUsed] == 60) {
					audioBuffer[audioBufferUsedLength] = 60;
					audioBufferUsedLength++;
					videoSockBufferUsedLength++;
 				}else {
 					if(audioBufferUsedLength>=audioBuffer.length) {
 						Log.d(TAG, "######=" + audioBufferUsedLength);
 					}
 					audioBuffer[audioBufferUsedLength] = socketBuf[i + sockBufferUsed];
					audioBufferUsedLength++;
 					videoSockBufferUsedLength++;
 				}
			}else {
				videoSockBufferUsedLength++;
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
						tmp.getHeight(), m, true);
				m.setRotate(180);
				postInvalidate();
				if (!tmp.isRecycled()) {
					tmp.recycle();
				}
			}
		} else {
			video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
			postInvalidate();
		}
	}

	public void onStop() {
		stopPlay = true;
		handler.removeMessages(Constants.WEB_CAM_RECONNECT_MSG);
		handler.sendEmptyMessageDelayed(Constants.WEB_CAM_RECONNECT_MSG, DELAY_RECONNECT);
		release();
		flushBitmap();
	}

	public boolean isStop() {
		return stopPlay;
	}

	private void release() {
		handler.removeCallbacks(calculateFrameTask);
		handler.sendEmptyMessage(PlayBackConstants.DISABLE_SEEKBAR);
		if (m_out_trk != null) {
			m_out_trk.stop();
			m_out_trk.release();
			m_out_trk = null;
			UdtTools.exitAmrDecoder();
		}
		//UdtTools.exit();
	}

	private void flushBitmap() {
		if(video != null && !video.isRecycled()) {
			video.recycle();
			video = null;
		}
		postInvalidate();
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

	private boolean hasAudioData;
	
	private class PalyBackAudio implements Runnable {
		
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
					AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size,
					AudioTrack.MODE_STREAM);
			m_out_trk.play();
		}
		
		@Override
		public void run() {
			while(!stopPlay) {
				if(hasAudioData) {
					UdtTools.amrDecoder(audioBuffer, audioBufferUsedLength, pcmArr, 0, Command.CHANEL);
					hasAudioData = false;
					audioBufferUsedLength =1;
					m_out_trk.write(pcmArr, 0, pcmBufferLength);
					Log.d(TAG, "pcmBufferLength=" + pcmBufferLength + " audioBufferUsedLength=" + audioBufferUsedLength);			
				}else {
					try{
						Thread.sleep(1);
						}catch(Exception e){};
					}
				}
		}
	}
	
	class RecvAudio implements Runnable {

		private AudioTrack m_out_trk = null;
		private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Command.CHANEL * 10;
		byte[] pcmArr = new byte[pcmBufferLength];

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
					AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size,
					AudioTrack.MODE_STREAM);
			m_out_trk.play();
			stopPlay = false;
			int recvDataLength = -1;
			while (!stopPlay) {
				//recvDataLength = audioDis.read(audioBuffer, 0, RECEAUDIOBUFFERSIZE);
				recvDataLength = UdtTools.recvAudioMsg(RECEAUDIOBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
				Log.d(TAG, "audio recvDataLength===" + recvDataLength);
				if(recvDataLength<=0) {
					stopPlay = true;
					break;
				}
				UdtTools.amrDecoder(audioBuffer, recvDataLength, pcmArr, 0, Command.CHANEL);
				//m_out_trk.write(pcmArr, 0, AUDIOBUFFERTMPSIZE);
				m_out_trk.write(pcmArr, 0, pcmBufferLength);
			}
			if (m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
				UdtTools.exitAmrDecoder();
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
				//invalidate();
			}
			frameCount = 0;
			handler.postDelayed(calculateFrameTask, 1000);
		}
	};

	
}
