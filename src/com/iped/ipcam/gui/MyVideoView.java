package com.iped.ipcam.gui;

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
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Command;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;

public class MyVideoView extends ImageView implements Runnable {

	private final static int NALBUFLENGTH = 320 * 480 * 2; // 600*800*2

	private final static int SOCKETBUFLENGTH = 342000;

	private final static int RECEAUDIOBUFFERSIZE = 1024 * Command.CHANEL * 1;

	private final static int SERVERSENDBUFFERSIZE = 1024;

	// private final static int AUDIOBUFFERTMPSIZE = 1280;

	// private final static int AUDIOBUFFERSTOERLENGTH = 12800;

	private Bitmap video = Bitmap.createBitmap(320, 480, Config.RGB_565);

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

	private int result = -1;

	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE * (1 + 1 / 2)];

	private static final String TAG = "MyVideoView";

	private Handler handler;

	private Device device;

	private Rect rect = null;

	private int frameCount;

	private String frameCountTemp = "";

	private String deviceId = "";

	private Paint textPaint;

	private int temWidth;

	// private int temHeigth;

	private Matrix m = new Matrix();

	private boolean reverseFlag = false;

	public MyVideoView(Context context) {
		super(context);
	}

	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		textPaint = new Paint(Color.RED);
		m.setScale(-1, 1);
		rect = new Rect(0, 0, getWidth(), getHeight() - 10);
	}

	void init(Handler handler, int w, int h) {
		this.handler = handler;
		textPaint = new Paint(Color.RED);
		m.setScale(-1, 1);
		rect = new Rect(0, 0, getWidth(), getHeight() - 10);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (video != null) {
			if (temWidth != getWidth()) {
				temWidth = getWidth();
				rect = new Rect(0, 0, getWidth(), getHeight() - 10);
			}
			canvas.drawBitmap(video, null, rect, null);
		}
		canvas.drawText(
				deviceId
						+ "  "
						+ DateUtil.formatTimeToDate5(System.currentTimeMillis())
						+ "  " + frameCountTemp + " p/s", 20, 25, textPaint);
	}

	public void run() {
		deviceId = device.getDeviceID();
		String tem = (CamCmdListHelper.SetCmd_StartVideo_Tcp + device.getUnDefine2() + "\0");
		int res = UdtTools.sendCmdMsgById(deviceId, tem, tem.length());
		if (res < 0) {
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			//onStop();
			return;
		}
		int bufLength = 10;
		byte[] b = new byte[bufLength];
		res = UdtTools.recvCmdMsgById(deviceId,b, bufLength);
		if (res < 0) {
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			//onStop();
			return;
		}
		BCVInfo info = new BCVInfo(b[3], b[4], b[5]);
		Message m = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putSerializable("UPDATEBCV", info);
		m.what = Constants.SEND_UPDATE_BCV_INFO_MSG;
		m.setData(bundle);
		handler.sendMessage(m);
		handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
		stopPlay = false;
		handler.removeCallbacks(calculateFrameTask);
		handler.post(calculateFrameTask);
		// TODO
		///new Thread(new RecvAudio()).start();
		while (!Thread.currentThread().isInterrupted() && !stopPlay) {
			try {
				readLengthFromSocket = UdtTools.recvVideoMsg(socketBuf, SOCKETBUFLENGTH);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("read exception break...." + e.getMessage());
				break;
			}
			if (readLengthFromSocket <= 0) { // ¶ÁÈ¡Íê³É
				System.out.println("read over break....");
				break;
			}
			sockBufferUsedLength = 0;
			while (readLengthFromSocket - sockBufferUsedLength > 0) {
				// remain socket  buf  length
				nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, socketBuf,
						sockBufferUsedLength,
						(readLengthFromSocket - sockBufferUsedLength));
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
						nalBuf[3] = -32;
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
					&& socketBuf[i + 3 + sockBufferUsed] == -32) {
				/*
				 * if((i + 3+ sockBufferUsed) < SOCKETBUFLENGTH) {
				 * if(socketBuf[i + sockBufferUsed] == -1 && socketBuf[i + 1 +
				 * sockBufferUsed] == -40 && socketBuf[i + 2 + sockBufferUsed]
				 * == -1 && socketBuf[i + 3+ sockBufferUsed] == -32) {
				 * looperFlag = true; return -2; } Synthesis } else {
				 * nalBuf[i+nalBufUsed] = socketBuf[i + sockBufferUsed];
				 * nalBufUsedLength++; sockBufferUsedLength++; }
				 */
				looperFlag = true;
				return -2;
			} else {
				nalBuf[i + nalBufUsed] = socketBuf[i + sockBufferUsed];
				nalBufUsedLength++;
				sockBufferUsedLength++;
			}
		}
		looperFlag = true;
		return i;
	}

	public void onStop() {
		stopPlay = true;
		release();
		//flushBitmap();
	}

	public boolean isStop() {
		return stopPlay;
	}

	private void release() {
		//UdtTools.close();
		handler.removeCallbacks(calculateFrameTask);
	}

	private void flushBitmap() {
		video = Bitmap.createBitmap(320, 480, Config.RGB_565);
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

		private int num = 0;
		private AudioTrack m_out_trk = null;
		private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Command.CHANEL * 10;
		byte[] pcmArr = new byte[pcmBufferLength];

		public RecvAudio() {

		}

		@Override
		public void run() {
			int init = UdtTools.initAmrDecoder();
			System.out.println("amr deocder init " + init);
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
			while (!stopPlay) {
				int recvDataLength = -1;
				//recvDataLength = audioDis.read(audioBuffer, 0, RECEAUDIOBUFFERSIZE);
				recvDataLength = UdtTools.recvAudioMsg(RECEAUDIOBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
				System.out.println(recvDataLength + "----------------------");
				//UdtTools.amrDecoder(audioBuffer, recvDataLength, pcmArr, 0, Command.CHANEL);
				// System.out.println("recvDataLength=" + recvDataLength +
				// " decoderLength=" + decoderLength);
				// m_out_trk.write(pcmArr, 0, AUDIOBUFFERTMPSIZE);
				// System.out.println("audio size = " + size + "  "+
				// returnSize);
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
			if (frameCount < 10) {
				frameCountTemp = " " + frameCount;
			} else {
				frameCountTemp = "" + frameCount;
			}
			frameCount = 0;
			invalidate();
			handler.postDelayed(calculateFrameTask, 1000);
		}
	};

}
