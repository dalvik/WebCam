package com.iped.ipcam.gui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
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

import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

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
	
	private Thread thread = null;

	private boolean flag = true;

	private VideoView videoView = null;

	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	//private  ImageView imageView = null;
	
	private Button leftUpButton = null;
	
	private Button midUpButton = null;
	
	private Button rightUpButton = null;
	
	private ControlPanel rightControlPanel = null;
	
	private String TAG = "CamVideoH264";

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
        //imageView = (ImageView) findViewById(R.id.imageview);
        videoView = (VideoView) findViewById(R.id.videoview);
        //imageView.setBackgroundResource(R.drawable.shutdown_bg);
        
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
        
		rightControlPanel = new ControlPanel(this, videoView,  230, LayoutParams.FILL_PARENT);
		
		layout.addView(rightControlPanel);
		//new Thread(new QueryDeviceThread()).start();
		rightControlPanel.fillPanelContainer(view);
		videoView.init(screenWidth, screenHeight);
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
		/*
		 * flag = false; if(thread != null || thread.isAlive()) { try {
		 * thread.join(); thread = null; } catch (InterruptedException e) {
		 * e.printStackTrace(); } }
		 */
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
				DatagramPacket packet = new DatagramPacket(tem, tem.length, InetAddress.getByName("192.168.1.210"), Constants.UDPPORT); 
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
				SocketAddress socketAddress = new InetSocketAddress("192.168.1.211", 1234);
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

class VideoView extends View implements Runnable {

	static {
		System.loadLibrary("H264Android");
	}

	public native int InitDecoder();

	public native int UninitDecoder();

	public native int DecoderNal(byte[] in, int insize, byte[] out);

	byte[] mPixel = new byte[240 * 320 * 2];

	ByteBuffer buffer = ByteBuffer.wrap(mPixel);

	Bitmap VideoBit = Bitmap.createBitmap(240, 320, Config.RGB_565);

	int mTrans = 0x0F0F0F0F;

	private int width = 0;
	
	private int height = 0;
	
	private String TAG = "VideoView";

	public VideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void playVideo() {
		new Thread(this).start();
	}

	public void init(int screenWidth, int screenHeight) {
		this.width = screenWidth;
		this.height = screenHeight;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
/*		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
		canvas.drawBitmap(bitmap, 0,0, new Paint(Color.WHITE));
		System.out.println("ondraw");
*/		//VideoBit.copyPixelsFromBuffer(buffer);// makeBuffer(data565, N));
		
		 VideoBit.copyPixelsFromBuffer(buffer);//makeBuffer(data565, N));
	    	
	     canvas.drawBitmap(VideoBit, 0, 0, null); 
	}

	@Override
	public void run() {
		InputStream is = null;
		
		FileInputStream fileIS = null;
		
		int decodeResult = 0;
		
		int nalLength;
		
		int nalBufferUsedLength = 0;

		byte[] nalBuffer = new byte[40980]; // 40k
		
		boolean isStartFlag = true;
		
		boolean findPPSFlag = true;
		
		int readByteLength = 0;

		byte[] sockBuffer = new byte[2048];

		int sockBufferUsedLength = 0;
		
		DataInputStream dis = null;
		InitDecoder();

		try {
			fileIS = new FileInputStream("/sdcard/butterfly.h264");
			//SocketAddress socketAddress = new InetSocketAddress("192.168.1.211", 1234);
			//Socket socket = new Socket();
			//socket.connect(socketAddress, 15000);
			//dis = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				//  读取2048字节到缓冲区
				readByteLength = fileIS.read(sockBuffer, 0, 2048);//   从文件流里面读取的字节的长度  <0时读取完毕
				//bytesRead = dis.read(SockBuf, 0, 2048);
			} catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}

			if (readByteLength <= 0) { // 读取完成
				break;
			}
			sockBufferUsedLength = 0;
			while (readByteLength - sockBufferUsedLength > 0) {
				nalLength = mergeBuffer(nalBuffer, nalBufferUsedLength, sockBuffer, sockBufferUsedLength, readByteLength - sockBufferUsedLength);
				nalBufferUsedLength += nalLength;
				sockBufferUsedLength += nalLength;
				while (mTrans == 1) {
					mTrans = 0xFFFFFFFF;
					if (isStartFlag == true) {// the first start flag
						isStartFlag = false;
					} else { // a complete NAL data, include 0x00000001 trail.
						if (findPPSFlag == true) { // true
							if ((nalBuffer[4] & 0x1F) == 7) {
								findPPSFlag = false;
							} else {
								nalBuffer[0] = 0;
								nalBuffer[1] = 0;
								nalBuffer[2] = 0;
								nalBuffer[3] = 1;
								nalBufferUsedLength = 4;
								break;
							}
						}
						// decode nal
						decodeResult = DecoderNal(nalBuffer, nalBufferUsedLength - 4, mPixel);
						if (decodeResult > 0){
							postInvalidate(); // 使用postInvalidate可以直接在线程中更新界面 // postInvalidate();
						}
					}
					nalBuffer[0] = 0;
					nalBuffer[1] = 0;
					nalBuffer[2] = 0;
					nalBuffer[3] = 1;
					nalBufferUsedLength = 4;
				}
			}
		}
		try {
			if (fileIS != null)
				fileIS.close();
			if (is != null)
				is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		UninitDecoder();
	}

	// 将读取到sockBuffer中的数据转移至nalBuffer
	int mergeBuffer(byte[] nalBuf, int nalBufUsed, byte[] sockBuf, int sockBufferUsedSize, int sockRemain) {
		int i = 0;
		byte temp;
		for (i = 0; i < sockRemain; i++) {
			temp = sockBuf[i + sockBufferUsedSize];
			nalBuf[i + nalBufUsed] = temp;
			mTrans <<= 8;
			mTrans |= temp;
			if (mTrans == 1){ // 找到一个开始字
				i++;
				break;
			}
		}
		return i;
	}
}
