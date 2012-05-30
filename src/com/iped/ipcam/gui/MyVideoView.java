package com.iped.ipcam.gui;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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

import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Common;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ThroughNetUtil;

public class MyVideoView extends View implements Runnable {

	private final static int NALBUFLENGTH = 320*480 * 2 ; //600*800*2
	
	private final static int SOCKETBUFLENGTH = 342000;
	
	private final static int RECEAUDIOBUFFERSIZE = 1024 * Common.CHANEL * 1;
	
	private final static int SERVERSENDBUFFERSIZE = 1024;
	
	//private final static int AUDIOBUFFERTMPSIZE = 1280;
	
	//private final static int AUDIOBUFFERSTOERLENGTH = 12800;
	
	private Bitmap video = Bitmap.createBitmap(600, 800, Config.RGB_565);
	
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
	
	private DataInputStream videoDis = null;
	
	private DataInputStream audioDis = null;
	
	private DatagramSocket datagramSocket = null;
	
	private boolean stopPlay = false;

	private int result = -1;
	
	private byte[] audioBuffer = new byte[RECEAUDIOBUFFERSIZE * 1];
	
	private static final String TAG = "ReadStreamThread";
	
	private Handler handler;
	
	private Context context;
	
	private Device device;
	
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
		if(device.getDeviceNetType()) {
			try {
				byte [] tem = CamCmdListHelper.SetCmd_StartVideo_Udp.getBytes();
				ThroughNetUtil netUtil = CamVideoH264.getInstance();
				datagramSocket = netUtil.getPort1();
				datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
				String ipAdd = device.getUnDefine1();
				DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ipAdd), device.getDeviceRemoteCmdPort());
				datagramSocket.send(datagramPacket);
				DatagramSocket port2 = netUtil.getPort2();
				DatagramSocket port3 = netUtil.getPort3();
				System.out.println("ready rece ...." + " " + ipAdd + " " + device.getDeviceRemoteCmdPort() + " remote video Port=" + device.getDeviceRemoteAudioPort() + " remote audio port=" +device.getDeviceRemoteVideoPort());
				int localPort2 =  port2.getLocalPort();
				result = UdtTools.initSocket(ipAdd, localPort2, device.getDeviceRemoteVideoPort(), port3.getLocalPort(), device.getDeviceRemoteAudioPort(), RECEAUDIOBUFFERSIZE,RECEAUDIOBUFFERSIZE);
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
			}
			
		}else {
			Socket socket = new Socket();
			 try {
					byte [] tem = CamCmdListHelper.SetCmd_StartVideo_Tcp.getBytes();
					datagramSocket = new DatagramSocket();
					datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
					String ipAddress = device.getDeviceWlanIp();
					DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ipAddress), device.getDeviceRemoteCmdPort());
					datagramSocket.send(datagramPacket);
					//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
					System.out.println("ready rece ....");
					SocketAddress socketAddress = new InetSocketAddress(ipAddress, device.getDeviceRemoteVideoPort());
					socket.connect(socketAddress, Constants.VIDEOSEARCHTIMEOUT);
					if(videoDis != null) {
						videoDis.close();
					}
					videoDis = new DataInputStream(socket.getInputStream());
					handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
					System.out.println("dis=" + videoDis);
				}catch (IOException e) {
					releaseTcpSocket(videoDis, socket);
					onStop();
					handler.sendEmptyMessage(Constants.HIDECONNDIALOG);
					handler.sendEmptyMessage(Constants.CONNECTERROR);
					return ;
				}
			 
			    long start = System.currentTimeMillis();
				int i = 0;
				//byte[] buf = new byte[1024];
				new Thread(new RecvAudio()).start();
				while (!Thread.currentThread().isInterrupted() && !stopPlay) {
					try {
						readLengthFromSocket = videoDis.read(socketBuf,0, SOCKETBUFLENGTH);
					} catch (IOException e) {
						e.printStackTrace();
						releaseTcpSocket(videoDis, socket);
						System.out.println("read exception break....");
						break;
					}
					if (readLengthFromSocket <= 0) { // 读取完成
						System.out.println("read over break....");
						releaseTcpSocket(videoDis, socket);
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
				}
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
		video = Bitmap.createBitmap(320, 480, Config.RGB_565);
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
			stopPlay = false;
			if(device.getDeviceNetType()) {
				try{
					while(!stopPlay) {
						//arg[0] server send audio buffer length arg[1] client recv big audio buffer 
						//arg[2] client recv big audio buffer length same length with audio init  
						int recvDataLength = UdtTools.recvAudioData(SERVERSENDBUFFERSIZE, audioBuffer, RECEAUDIOBUFFERSIZE);
						//System.out.println(recvDataLength + "----------------------");
						if(recvDataLength <=0) {
							stopPlay = true;
							break;
						}
						int decoderLength = UdtTools.amrDecoder(audioBuffer, recvDataLength , pcmArr, 0, Common.CHANEL);
						//System.out.println("recvDataLength=" + recvDataLength + " decoderLength=" + decoderLength);
						//m_out_trk.write(pcmArr, 0, AUDIOBUFFERTMPSIZE);
						//System.out.println("audio size = " + size + "  "+ returnSize);
						m_out_trk.write(pcmArr, 0, pcmBufferLength);
					}
				}catch(Exception e) {
					stopPlay = true;
				}
			} else {
				Socket socket = new Socket();
				SocketAddress socketAddress = new InetSocketAddress(device.getDeviceWlanIp(), device.getDeviceRemoteAudioPort());
				try {
					socket.connect(socketAddress, Constants.VIDEOSEARCHTIMEOUT);
					if(audioDis != null) {
						audioDis.close();
					}
					audioDis = new DataInputStream(socket.getInputStream());
					System.out.println("audio dis=" + videoDis);
				} catch (IOException e) {
					e.printStackTrace();
					stopPlay = true;
					releaseTcpSocket(videoDis, socket);
					return;
				}
				stopPlay = false;
				while(!stopPlay) {
					int recvDataLength = -1;
					try {
						recvDataLength = audioDis.read(audioBuffer,0, RECEAUDIOBUFFERSIZE);
					} catch (IOException e) {
						stopPlay = true;
						e.printStackTrace();
						break;
					}
					//System.out.println(recvDataLength + "----------------------");
					if(recvDataLength <=0) {
						stopPlay = true;
						break;
					}
					int decoderLength = UdtTools.amrDecoder(audioBuffer, recvDataLength , pcmArr, 0, Common.CHANEL);
					//System.out.println("recvDataLength=" + recvDataLength + " decoderLength=" + decoderLength);
					//m_out_trk.write(pcmArr, 0, AUDIOBUFFERTMPSIZE);
					//System.out.println("audio size = " + size + "  "+ returnSize);
					m_out_trk.write(pcmArr, 0, pcmBufferLength);
				}
			}
			if(m_out_trk != null) {
				m_out_trk.stop();
				m_out_trk.release();
				m_out_trk = null;
				UdtTools.exitAmrDecoder();
			}
		}
		
	}

	public void releaseTcpSocket(DataInputStream dis, Socket socket) {
		if(videoDis != null) {
			try {
				videoDis.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public void setDevice(Device device) {
		this.device = device;
	}
	
	/*
	
	 
	 */
	
	
}
