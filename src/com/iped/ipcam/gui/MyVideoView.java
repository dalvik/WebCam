package com.iped.ipcam.gui;

import java.nio.ByteBuffer;

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
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;

public class MyVideoView extends ImageView implements Runnable {

	private final static int DELAY_RECONNECT = 1000 * 60 * 2;
	
	private final static int NALBUFLENGTH = 320 * 480; // 600*800*2

	private final static int SOCKETBUFLENGTH = 34200;//342000;

	private final static int RECEAUDIOBUFFERSIZE = 1024 * Command.CHANEL * 1;

	private final static int SERVERSENDBUFFERSIZE = 1024;

	// private final static int AUDIOBUFFERTMPSIZE = 1280;

	// private final static int AUDIOBUFFERSTOERLENGTH = 12800;

	private Bitmap video;

	byte[] pixel = new byte[NALBUFLENGTH];

	byte[] nalBuf = new byte[NALBUFLENGTH];//

	ByteBuffer buffer = ByteBuffer.wrap(nalBuf);

	int nalSizeTemp = 0;

	int nalBufUsedLength = 0;

	byte[] socketBuf = new byte[SOCKETBUFLENGTH];

	int readLengthFromSocket = 0;

	int sockBufferUsedLength;

	boolean firstStartFlag = true;

	boolean looperFlag = false;

	private boolean stopPlay = true;

	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE * (1 + 1 / 2)];

	private static final String TAG = "MyVideoView";

	private Handler handler;

	private Device device;

	private Rect rect = null;

	private Rect rect2 = null;

	private int frameCount;

	private int frameCountTemp;

	private String deviceId = "";

	private Paint textPaint;
	
	private Paint bgPaint;
	
	private Paint infoPaint;

	private int temWidth;
	
	private String timeStr = "";

	// private int temHeigth;

	private Matrix m = new Matrix();

	private boolean reverseFlag = false;

	
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
		String tem = (CamCmdListHelper.SetCmd_StartVideo_Tcp + device.getUnDefine2() + "\0");
		int res = UdtTools.sendCmdMsgById(deviceId, tem, tem.length());
		if (res < 0) {
			//handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			//handler.sendEmptyMessageDelayed(Constants.WEB_CAM_RECONNECT_MSG, DELAY_RECONNECT);
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
			//handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			onStop();
			//handler.sendEmptyMessageDelayed(Constants.WEB_CAM_RECONNECT_MSG, DELAY_RECONNECT);
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
		//handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
		stopPlay = false;
		handler.removeCallbacks(calculateFrameTask);
		handler.post(calculateFrameTask);
		handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		handler.removeMessages(Constants.WEB_CAM_RECONNECT_MSG);
		new Thread(new RecvAudio()).start();
		while (!Thread.currentThread().isInterrupted() && !stopPlay) {
			readLengthFromSocket = UdtTools.recvVideoMsg(socketBuf, SOCKETBUFLENGTH);
			if (readLengthFromSocket <= 0) { // 读取完成
				System.out.println("read over break....");
				break;
			}
			sockBufferUsedLength = 0;
			while (readLengthFromSocket - sockBufferUsedLength > 0) {
				// remain socket  buf  length
				nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, socketBuf,	sockBufferUsedLength, (readLengthFromSocket - sockBufferUsedLength));
				// 根据nalSizeTemp的值决定是否刷新界面
				while (looperFlag) {
					looperFlag = false;
					if (nalSizeTemp == -2) {
						if (nalBufUsedLength > 0) {
							frameCount++;
							copyPixl();
							//firstStartFlag = true;
						}
						nalBuf[0] = -1;
						nalBuf[1] = -40;
						nalBuf[2] = -1;
						nalBuf[3] = -32; // 刷新界面之后，再将jpeg数据头加入nalbuffer中
						sockBufferUsedLength += 4;
						nalBufUsedLength = 4;
						break;
					}
				}
			}
		}
		onStop();
		System.out.println("onstop====" + stopPlay);
	}

	private int mergeBuffer(byte[] nalBuf, int nalBufUsed, byte[] socketBuf,
			int sockBufferUsed, int socketBufRemain) {
		int i = 0;
		for (i = 0; i < socketBufRemain; i++) {
			if (firstStartFlag && socketBuf[i] == 0 && socketBuf[i + 1] == 0
					&& socketBuf[i + 2] == 0 && socketBuf[i + 3] == 1) {
				firstStartFlag = false;
				sockBufferUsedLength += 65;
				looperFlag = true;
				return -1;
			} else if (socketBuf[i + sockBufferUsed] == -1
					&& socketBuf[i + 1 + sockBufferUsed] == -40
					&& socketBuf[i + 2 + sockBufferUsed] == -1
					&& socketBuf[i + 3 + sockBufferUsed] == -32) {// 每检测到jpeg的开头刷新图片
				looperFlag = true;
				return -2;
			}  else {
				if(socketBuf[sockBufferUsed] == 0 && socketBuf[sockBufferUsed + 1] == 0
						&& socketBuf[sockBufferUsed + 2] == 0 && socketBuf[sockBufferUsed + 3] == 1) {
					timeStr = new String(socketBuf,sockBufferUsed+5,14);
				}
				nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
				nalBufUsedLength++;
				sockBufferUsedLength++;
			}
		}
		looperFlag = true;
		return i;
	}
	
	public void copyPixl() {
		if (reverseFlag) {
			Bitmap tmp = BitmapFactory.decodeByteArray(nalBuf, 0,
					nalBufUsedLength);
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
				invalidate();
			}
			frameCount = 0;
			handler.postDelayed(calculateFrameTask, 1000);
		}
	};

}
