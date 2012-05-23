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
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ThroughNetUtil;

public class MyVideoView extends View implements Runnable {

	private final static int NALBUFLENGTH = 320*480 ; //320*480 * 2
	
	private final static int SOCKETBUFLENGTH = 3420;
	
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
	
	private static final String TAG = "ReadStreamThread";
	
	private Handler handler;
	
	public MyVideoView(Context context) {
		super(context);
	}
	
	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
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
			//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			System.out.println("ready rece ...." + " " + CamVideoH264.currIpAddress + " " + CamVideoH264.currPort);
			int localPort2 =  port2.getLocalPort();
			//port2.close();
			result = UdtTools.initSocket(CamVideoH264.currIpAddress,localPort2, CamVideoH264.currPort);
			System.out.println("result=" + result);
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
		video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
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
					} 
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
		video = Bitmap.createBitmap(320, 480, Config.RGB_565);
		postInvalidate();
	}
	public void onStart() {
		stopPlay = false;
	}
	
	public boolean getPlayStatus() {
		return stopPlay;
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
