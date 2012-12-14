package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.iped.ipcam.engine.DecodeAudioThread;
import com.iped.ipcam.engine.DecodeJpegThread;
import com.iped.ipcam.engine.PlayBackThread;
import com.iped.ipcam.engine.PlayMpegThread;
import com.iped.ipcam.engine.PlayMpegThread.OnMpegPlayListener;
import com.iped.ipcam.engine.TalkBackThread;
import com.iped.ipcam.factory.DecoderFactory;
import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PlayBackConstants;

public class MyVideoView extends ImageView implements Runnable, OnMpegPlayListener {

	private boolean DEBUG = true;
	
	private final static int DELAY_RECONNECT = 1000 * 60 * 2* 1000;
	
	public final static int NALBUFLENGTH = 320 * 480 *2; // 600*800*2

	private static int VIDEOSOCKETBUFLENGTH = 1024 + 31;//  342000;

	public final static int PLAYBACK_AUDIOBUFFERSIZE = 1024 * Command.CHANEL * 1;

	private Bitmap video;

	private byte[] nalBuf = null;

	private int nalBufUsedLength;

	private byte[] videoSocketBuf ;

	private int readLengthFromVideoSocket = 0;

	//private int videoSockBufferUsedLength;

	private boolean stopPlay = true;
	
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

	private boolean playBackFlag = false;
	
	private boolean mpeg4Decoder = false;
	
	private int indexForPut = 0; // put索引 （下一个要写入的位置）
	
	private OnPutIndexListener listener;
	
	private DecoderFactory decoderFactory = null;
	
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
				getMeasuredHeight();
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
			
			if(reverseFlag) {
				//canvas.rotate(180,getWidth() /2, getHeight() /2);
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
		videoSocketBuf = new byte[VIDEOSOCKETBUFLENGTH];
		nalBuf = new byte[NALBUFLENGTH];//>100k
		indexForPut = 0;
		temWidth = 1;
		if(!playBackFlag) {
			if(mpeg4Decoder) {
				if(decoderFactory != null && !decoderFactory.isInterrupted()) {
					Log.d(TAG, "############## interrupt.");
					decoderFactory.isInterrupted();
					decoderFactory = null;
				}
				decoderFactory = new PlayMpegThread(this,nalBuf, timeStr, video, frameCount);
				decoderFactory.setOnMpegPlayListener(this);
				new Thread(decoderFactory).start();
			}else {
				new Thread(new DecodeAudioThread(this)).start();
				decoderFactory = new DecodeJpegThread(this, nalBuf, timeStr, video, frameCount);
				decoderFactory.setOnMpegPlayListener(this);
				new Thread(decoderFactory).start();
			}
		}else { // 回放
			decoderFactory = new PlayBackThread(this, nalBuf, timeStr, video, frameCount, handler);
			decoderFactory.setOnMpegPlayListener(this);
			new Thread(decoderFactory).start();	
		}
		while (!Thread.currentThread().isInterrupted() && !stopPlay) {
			readLengthFromVideoSocket = UdtTools.recvVideoMsg(videoSocketBuf, VIDEOSOCKETBUFLENGTH);
			if (readLengthFromVideoSocket <= 0) { // 读取完成
				if(BuildConfig.DEBUG && !DEBUG) {
					Log.d(TAG, "### read over break....");
				}
				stopPlay = true;
				break;
			}
			int recvBufIndex = 0;
			do{
				if((indexForPut +1) % NALBUFLENGTH == decoderFactory.getIndexForGet()) {
					if(BuildConfig.DEBUG && !DEBUG) {
						Log.d(TAG, "### data buffer is full, please get data in time " + decoderFactory.getIndexForGet() );
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
					 if(listener != null) {
						 listener.updatePutIndex(indexForPut);
					 }
				     recvBufIndex++;
				}
			}while(readLengthFromVideoSocket>recvBufIndex && !stopPlay);
		} 
		onStop();
	}
	
	public boolean takePic() {
		if(null != video){
			return FileUtil.takePicture(video, timeStr +".jpg");
		}
		return false;
	}
	
	public void onStop() {
		TalkBackThread.stopTalkBack();
		if(decoderFactory != null) {
			decoderFactory.onStop(stopPlay);
		}
		if(listener != null) {
			listener.onStop(true);
		}
		
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
	
	public void setDevice(Device device) {
		this.device = device;
	}

	private Runnable calculateFrameTask = new Runnable() {

		@Override
		public void run() {
			frameCountTemp = frameCount;
			if(frameCountTemp<=2) {
				if(video != null) {
					invalidate(rect);
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
		timeStr = "";
		nalBufUsedLength = 0;
	}
	
	
	public void setTime(String timeStr) {
		this.timeStr = timeStr;
	}

	public void setOpenSendAudioFlag(boolean openSendAudioFlag) {
		if(openSendAudioFlag) {
			new Thread(new TalkBackThread()).start();
		}else {
			TalkBackThread.stopTalkBack();
		}
	}
	
	public interface OnPutIndexListener {
		public void updatePutIndex(int putIndex);
		public void onStop(boolean stopPlay);
	}
	
	public void setOnPutIndexListener(OnPutIndexListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void invalide(int frameCount, String timeStr) {
		this.frameCount = frameCount;
		this.timeStr = timeStr;
		postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
	}
	
	public void setImage(Bitmap video) {
		this.video = video;
	}
	
	public int getFrameCount() {
		return frameCount;
	}
}
