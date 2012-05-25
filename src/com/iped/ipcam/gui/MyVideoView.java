package com.iped.ipcam.gui;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Common;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ThroughNetUtil;

public class MyVideoView extends View implements Runnable {

	private final static int NALBUFLENGTH = 320*480 ; //320*480 * 2
	
	private final static int SOCKETBUFLENGTH = 3420;
	
	private final static int RECEAUDIOBUFFERSIZE = 128 * Common.CHANEL * 3;
	
	private final static int SERVERSENDBUFFERSIZE = 128;
	
	//private final static int AUDIOBUFFERTMPSIZE = 1280;
	
	//private final static int AUDIOBUFFERSTOERLENGTH = 12800;
	
	private Bitmap video = Bitmap.createBitmap(320, 480, Config.RGB_565);
	
	byte[] pixel = new byte[NALBUFLENGTH];
	
	byte[] nalBuf = new byte[NALBUFLENGTH];// 

	ByteBuffer buffer = ByteBuffer.wrap(nalBuf);
	
	int nalSizeTemp = 0;
	
	int nalBufUsedLength = 0;
	
	byte[] socketBuf = new byte[SOCKETBUFLENGTH];
	
	FileInputStream fis = null;
	
	int readLengthFromSocket = 0;
	
	int sockBufferUsedLength;
	
	boolean firstStartFlag = true;
	
	boolean looperFlag = false;
	
	private Socket socket = null;
	
	private DataInputStream dis = null;
	
	private DatagramSocket datagramSocket = null;
	
	private boolean stopPlay = false;

	private int result = -1;
	
	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE * 1];
	
	//private byte[] audioBufferStore = new byte[AUDIOBUFFERSTOERLENGTH];
	
	private static final String TAG = "ReadStreamThread";
	
	private Handler handler;
	
	private Context context;
	
	public MyVideoView(Context context) {
		super(context);
		this.context = context;
	}
	
	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	void init(Handler handler) {
		this.handler = handler;
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(video != null) {
			canvas.drawBitmap(video, 0, 0, null);
		}
	}
	
	public void run() {
		try {
			byte [] tem = CamCmdListHelper.SetCmd_StartVideo_Udp.getBytes();
			ThroughNetUtil netUtil = CamVideoH264.getInstance();
			datagramSocket = netUtil.getPort1();
			datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(CamVideoH264.currIpAddress), CamVideoH264.port1);
			datagramSocket.send(datagramPacket);
			DatagramSocket port2 = netUtil.getPort2();
			DatagramSocket port3 = netUtil.getPort3();
			//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			System.out.println("ready rece ...." + " " + CamVideoH264.currIpAddress + " " + CamVideoH264.currPort + " remote video Port=" + CamVideoH264.port2 + " remote audio port=" +CamVideoH264.port3);
			int localPort2 =  port2.getLocalPort();
			//port2.close();
			result = UdtTools.initSocket(CamVideoH264.currIpAddress, localPort2, CamVideoH264.port2, port3.getLocalPort(), CamVideoH264.port3, RECEAUDIOBUFFERSIZE,RECEAUDIOBUFFERSIZE);
			System.out.println("socket init result = " + result);
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			/*SocketAddress socketAddress = new InetSocketAddress(CamVideoH264.currIpAddress, Constants.TCPPORT);
			socket = new Socket();
			socket.connect(socketAddress, Constants.VIDEOSEARCHTIMEOUT);
			if(dis != null) {
				dis.close();
			}
			dis = new DataInputStream(socket.getInputStream());
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);*/
		}catch (IOException e) {
			e.printStackTrace();
			onStop();
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			handler.sendEmptyMessage(Constants.CONNECTERROR);
			return ;
		}
		long start = System.currentTimeMillis();
		int i = 0;
		//byte[] buf = new byte[1024];
		new Thread(new RecvAudio()).start();
		while (!Thread.currentThread().isInterrupted() && result>0 && !stopPlay) {
			readLengthFromSocket = UdtTools.recvVideoData(socketBuf, SOCKETBUFLENGTH);
			if (readLengthFromSocket <= 0) { // 读取完成
				System.out.println("read over break....");
				break;
			}
			sockBufferUsedLength = 0;
			while(readLengthFromSocket - sockBufferUsedLength>0) {// remain socket buf length
				nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, socketBuf, sockBufferUsedLength, (readLengthFromSocket - sockBufferUsedLength));
				while(looperFlag) {
					looperFlag = false;
					if(nalSizeTemp == -2) {
						if(nalBufUsedLength>0) {
							i++;
							if(i%50 ==0) {
								long end = System.currentTimeMillis() / 1000 - start / 1000;
								System.out.println("pic index=" + i +" use time" + end +  " rate:" + i/(end)+ " p/s");
							}
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
			//System.out.println("size = " + size + "   " + buf[100]);
			/*try {
				readLengthFromSocket = dis.read(socketBuf,0, SOCKETBUFLENGTH);//   从流里面读取的字节的长度  <0时读取完毕
			} catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}
			if (readLengthFromSocket <= 0) { // 读取完成
				System.out.println("read over break....");
				break;
			}
			sockBufferUsedLength = 0;
			while(readLengthFromSocket - sockBufferUsedLength>0) {// remain socket buf length
				nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, socketBuf, sockBufferUsedLength, (readLengthFromSocket - sockBufferUsedLength));
				while(looperFlag) {
					looperFlag = false;
					if(nalSizeTemp == -2) {
						if(nalBufUsedLength>0) {
							i++;
							if(i%50 ==0) {
								long end = System.currentTimeMillis() / 1000 - start / 1000;
								System.out.println("pic index=" + i +" use time" + end +  " rate:" + i/(end)+ " p/s");
							}
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
			}*/
		}
		onStop();
		release();
		//Thread.currentThread().destroy();
		System.out.println("onstop===="  + stopPlay);
	}
	
	public void copyPixl() {
		//video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
		if(video != null) {
			postInvalidate();
		}
	}
	 
	private int mergeBuffer(byte[] nalBuf, int nalBufUsed, byte[] socketBuf, int sockBufferUsed, int socketBufRemain) {
		int i = 0;
		for(i=0; i<socketBufRemain; i++) {
			if(firstStartFlag && socketBuf[i] == 0 && socketBuf[i+1] == 0 && socketBuf[i+2] == 0 && socketBuf[i+3] == 1) {
				firstStartFlag = false;
				sockBufferUsedLength += 65;
				looperFlag = true;
				return -1;
			} else if(socketBuf[i + sockBufferUsed] == -1 && socketBuf[i + 1 + sockBufferUsed] == -40 && socketBuf[i + 2 + sockBufferUsed] == -1 && socketBuf[i + 3+ sockBufferUsed] == -32) {
				/*if((i + 3+ sockBufferUsed) < SOCKETBUFLENGTH) {
					if(socketBuf[i + sockBufferUsed] == -1 && socketBuf[i + 1 + sockBufferUsed] == -40 && socketBuf[i + 2 + sockBufferUsed] == -1 && socketBuf[i + 3+ sockBufferUsed] == -32) {
						looperFlag = true;
						return -2;
					} Synthesis
				} else {
					nalBuf[i+nalBufUsed] = socketBuf[i + sockBufferUsed];
					nalBufUsedLength++;
					sockBufferUsedLength++;
				}*/
				looperFlag = true;
				return -2;
			}else {
				nalBuf[i+nalBufUsed] = socketBuf[i + sockBufferUsed];
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
		flushBitmap();
	}

	private void release() {
		UdtTools.release();
		if(fis != null) {
			try {
				fis.close();
				Log.d("CamVideoH264", "over......."  + Thread.currentThread().isInterrupted());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(datagramSocket != null) {
			datagramSocket.close();
			datagramSocket = null;
		}
	}
	
	private void flushBitmap() {
		//video = Bitmap.createBitmap(320, 480, Config.RGB_565);
		postInvalidate();
	}
	public void onStart() {
		stopPlay = false;
	}
	
	public boolean getPlayStatus() {
		return stopPlay;
	}
	
	class RecvAudio implements Runnable {
		
		private int num = 0;
		private AudioTrack m_out_trk = null; 
		private int pcmBufferLength = RECEAUDIOBUFFERSIZE * Common.CHANEL * 10;
		byte[] pcmArr = new byte[pcmBufferLength];
				
		public RecvAudio() {
			
		}
		
		@Override
		public void run() {
			int init = UdtTools.initAmrDecoder();
			System.out.println("amr deocder init " + init);
			int m_out_buf_size = android.media.AudioTrack.getMinBufferSize(8000,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
			m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                     AudioFormat.CHANNEL_CONFIGURATION_MONO,
                     AudioFormat.ENCODING_PCM_16BIT,
                     m_out_buf_size,
                     AudioTrack.MODE_STREAM);
			m_out_trk.play();
			while(!stopPlay) {
				//arg[0] server send audio buffer length
				//arg[1] client recv big audio buffer 
				//arg[2] client recv big audio buffer length same length with audio init  
				int recvDataLength = UdtTools.recvAudioData(SERVERSENDBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
				if(recvDataLength <=0) {
					break;
				}
				if(audioBuffer[0] != 60 || audioBuffer[32] != 60 || audioBuffer[64] != 60 || audioBuffer[96] != 60) {
					System.out.println(audioBuffer[0] + " " + audioBuffer[32] + "  " + audioBuffer[64] + " " + audioBuffer[96]);
				}
				int decoderLength = UdtTools.amrDecoder(audioBuffer, recvDataLength , pcmArr, 0, Common.CHANEL);
				//System.out.println("recvDataLength=" + recvDataLength + " decoderLength=" + decoderLength + " " + RECEAUDIOBUFFERSIZE * Common.CHANEL * 100);
				//m_out_trk.write(pcmArr, 0, AUDIOBUFFERTMPSIZE);
				//System.out.println("audio size = " + size + "  "+ returnSize);
				m_out_trk.write(pcmArr, 0, RECEAUDIOBUFFERSIZE * Common.CHANEL * 10);
				//mergeAudioBuffer(pcmArr,pcmBufferSize);
			}
			if(m_out_trk != null) {
				UdtTools.exitAmrDecoder();
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
			}
		}
		
		private void mergeAudioBuffer(byte[] pcmBuffer, int pcmBufferLength) {
			for(int i=0; i<pcmBufferLength;i++) {
				int tmpIndex = i + pcmBufferLength * num;
				//if(tmpIndex > AUDIOBUFFERSTOERLENGTH -1) {
					//audioBufferStore[tmpIndex] = pcmBuffer[i];
				//}
			}
			if(num % 8 == 0) {
				num = 0;
				//m_out_trk.write(audioBufferStore, 0, AUDIOBUFFERSTOERLENGTH);
			}
		}
	}
	
	/*
	 try {
			byte [] tem = CamCmdListHelper.SetCmd_StartVideo_Tcp.getBytes();
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(CamVideoH264.currIpAddress), CamVideoH264.currPort);
			datagramSocket.send(datagramPacket);
			//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			System.out.println("ready rece ....");
			SocketAddress socketAddress = new InetSocketAddress(CamVideoH264.currIpAddress, Constants.TCPPORT);
			socket = new Socket();
			socket.connect(socketAddress, Constants.VIDEOSEARCHTIMEOUT);
			if(dis != null) {
				dis.close();
			}
			dis = new DataInputStream(socket.getInputStream());
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			System.out.println("dis=" + dis);
		}catch (IOException e) {
			e.printStackTrace();
			onStop();
			handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
			handler.sendEmptyMessage(Constants.CONNECTERROR);
			return ;
		}
	 
	 */
}
