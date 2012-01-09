package com.iped.ipcam.gui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

/**
 * H.264的功能分为两层，
 * 视频编码层(VCL，Video Coding Layer)
 * 网络提取层(NAL，Network Abstraction Layer)
 * 
 * 在VCL和NAL之间定义了一个基于分组方式的接口，打包和相应的信令属于NAL的一部分。
 * 	   这样，高效率编码和网络适应性的任务分别由VCL和NAL来完成。VCL数据是编码处理后的输出，它表示被压缩编码后的视频数据序列。
 * 	   在VCL数据传输和存储之前，这些编码的VCL数据先被映射或封装进NAL单元中。
     VCL包括基于块的运动补偿、混合编码和一些新特性。
     NAL负责针对下层网络的特性对数据进行封装，包括成帧、发信号给逻辑信道、利用同步信息等。
     NAL从VCL获得数据，包括头信息、段结构信息和实际载荷，NAL的任务就是正确地将它们映射到传输协议上。
     NAL下面是各种具体的协议，
             如H.323、H.324、RTP/UDP/IP等。NAL层的引入大大提高了H.264适应复杂信道的能力。 
 * @author Administrator
 *
 */
public class CamVideoH264 extends Activity {
	
	private boolean flag = true;

	//private VideoView videoView = null;
	
	private MyVideoView myVideoView = null;
	
	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	//private  ImageView imageView = null;
	
	private Button leftUpButton = null;
	
	private Button midUpButton = null;
	
	private Button rightUpButton = null;
	
	private ControlPanel rightControlPanel = null;
	
	private boolean stop = false;
	
	private Thread thread = null;
	
	private static WakeLock mWakeLock;
	
	private String TAG = "CamVideoH264";

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			initThread();
		};
	};
	
	private void initThread() {
		if(thread != null || thread.isAlive()) {
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		thread = new Thread(myVideoView);
		thread.start();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// thread = new Thread(new SocketThread());
		// thread.start();
		DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        
        setContentView(R.layout.pre_videoview);
        myVideoView = (MyVideoView) findViewById(R.id.videoview);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
		rightControlPanel = new ControlPanel(this, myVideoView,  230, LayoutParams.FILL_PARENT);
		layout.addView(rightControlPanel);
		rightControlPanel.fillPanelContainer(view);
		thread = new Thread(myVideoView);
		
		
		//thread.start();
		
		
		
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotDimScreen");
		if(mWakeLock.isHeld() == false) {
	       System.out.println("lock ...");
	       mWakeLock.acquire();
	    }
		//new Thread(new QueryDeviceThread()).start();
		//videoView.init(screenWidth, screenHeight);
        //videoView.playVideo();
		//videoView = new VideoView(this);
		//setContentView(videoView);
		//videoView.playVideo();
		//new Thread(new SocketThread()).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		leftUpButton = (Button) findViewById(R.id.left_up);
		if(leftUpButton != null) {
			leftUpButton.measure(0, 0);
			rightControlPanel.updateControlView(leftUpButton.getMeasuredWidth() * 3);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(thread != null && !thread.isAlive()) {
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			stop = true;
			thread = null;
		}
		/*
		 * flag = false; if(thread != null || thread.isAlive()) { try {
		 * thread.join(); thread = null; } catch (InterruptedException e) {
		 * e.printStackTrace(); } }
		 */
	}

	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	 if(mWakeLock.isHeld() == true) {
    		 System.out.println("reelase ...");
    		 mWakeLock.release();
         }
    }

	private class QueryDeviceThread implements Runnable {

		private byte[] bufTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		
		@Override
		public void run() {
			System.out.println("started..");
			DatagramSocket datagramSocket = null;
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(600);
				byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
				System.arraycopy(tem, 0, bufTemp, 0, tem.length);
				System.out.println(InetAddress.getByName("192.168.1.211"));
				DatagramPacket packet = new DatagramPacket(tem, tem.length, InetAddress.getByName("192.168.1.141"), Constants.UDPPORT); 
				datagramSocket.send(packet);
				System.out.println("send..");
			} catch (SocketException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			DatagramPacket dp = new DatagramPacket(bufTemp, bufTemp.length);
			try {
				datagramSocket.receive(dp);
				String info = new String(bufTemp);
				System.out.println("receive inof = : " + info);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("----" + e.getLocalizedMessage());
			}
			System.out.println("over..");
		}
		
	}
	
	private class SocketThread implements Runnable {
		private Socket socket = null;
		private DataInputStream dis = null;
		/**
		 -   char[] head = {0,0,0,1,0xc}; 00 00 00 00 67           5个字节
		 -   char packageSequenceNumber[8],0,1,2,3,4....不重复              8个字节
		 报      char startTimeStamp[14] YYYYMMDDHHMMSS                14个字节
		 头      char lastTimeStamp[14]  开始为111111                  14个字节  
		 -   char frameRateUS[8]                                   8个字节
		 -   char frameWidth[4]                                    4个字节
		 -   char frameHeight[4]                                   4个字节
		 数据包头长：                                                                                                                              57个字节
		 数据内容   char rowData[]                                    余下为数据
		00 00 00 01 67
		42 00 1e ab 40 58 09 32
		00 00 00 01 68 ce 38 80 00 00 00 01 65 88
		82
		 */
		@Override
		public void run() {
			try {
				SocketAddress socketAddress = new InetSocketAddress("192.168.1.121", 1234);
				int i = 0;
				byte[] b = new byte[512];
				socket = new Socket();
				socket.connect(socketAddress, 15000);
				dis = new DataInputStream(socket.getInputStream());
				String temp = "";
				FileOutputStream fos = new FileOutputStream(new File("/sdcard/"+ System.currentTimeMillis() + ".h264"));
				while ((i = dis.read(b)) != -1 && flag) {
					temp = new String(b, 0, i, "ISO-8859-1");
					fos.write(b, 0, i);
					fos.flush();
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}


class MyVideoView extends View implements Runnable {


	private final static int NALBUFLENGTH = 320*480 * 2;
	
	private final static int SOCKETBUFLENGTH = 342000;
	
	private Bitmap video = Bitmap.createBitmap(320, 480, Config.RGB_565);
	
	byte[] pixel = new byte[NALBUFLENGTH];
	
	byte[] nalBuf = new byte[NALBUFLENGTH];// 80k

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
	
	private boolean stop = false;

	private SurfaceHolder holder;
	
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
		//video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
		if(video != null) {
			canvas.drawBitmap(video, 0, 0, null);
		}
	}
	
	private byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
	
	public void run() {
		try {
			byte [] tem = CamCmdListHelper.SetCmd_StartVideo.getBytes();
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
			//datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(Constants.DEFAULTSEARCHIP + i), Constants.UDPPORT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName("192.168.1.141"), 60000);
			datagramSocket.send(datagramPacket);
			System.out.println("send udp packet...");
			//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			System.out.println("ready rece ....");
			//datagramSocket.receive(rece);
			//String info = new String(buffTemp);
			//if(info != null) {
				//Log.d(TAG, "receive inof = : " + info);
				SocketAddress socketAddress = new InetSocketAddress("192.168.1.141", 1234);
				socket = new Socket();
				socket.connect(socketAddress, 15000);
				dis = new DataInputStream(socket.getInputStream());
				System.out.println("dis=" + dis);
			//}
			//fis = new FileInputStream(new File("/sdcard/video_test.dat"));
			//fis = new FileInputStream(new File("/sdcard/a.jpeg"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long start = System.currentTimeMillis();
		int i = 0;
		while (!Thread.currentThread().isInterrupted() && dis != null) {
			try {
				readLengthFromSocket = dis.read(socketBuf,0, SOCKETBUFLENGTH);//   从文件流里面读取的字节的长度  <0时读取完毕
				//System.out.println("rece........." + Thread.currentThread().isInterrupted());
			} catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}
			if (readLengthFromSocket <= 0) { // 读取完成
				System.out.println("read over break....");
				break;
			}
			sockBufferUsedLength = 0;
			while(readLengthFromSocket - sockBufferUsedLength>0) {// remain socket buf length
				try {
					nalSizeTemp = mergeBuffer(nalBuf, nalBufUsedLength, socketBuf, sockBufferUsedLength, (readLengthFromSocket - sockBufferUsedLength));
				}catch(Exception e) {
					
				}
				while(looperFlag && !stop) {
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
		if(fis != null) {
			try {
				fis.close();
				Log.d("CamVideoH264", "over......."  + Thread.currentThread().isInterrupted());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void copyPixl() {
		/*for(int i=0; i<nalBufUsedLength; i++) {
			System.out.print(byte2HexString(nalBuf[i]) + " ");
		}*/
		video = BitmapFactory.decodeByteArray(nalBuf, 0, nalBufUsedLength);
		if(video != null && !stop) {
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

}


