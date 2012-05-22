package com.iped.ipcam.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ThroughNetUtil implements Runnable {

	private Handler handler;
	
	private String TAG = "ThroughNet";

	public enum SendUDTCommon {
		/**
		 * 提示对方接收到的数据不完整， 或其他错误 0
		 */
		NET_CAMERA_FAILED,

		/**
		 * 提示接收方受到完整数据 1
		 */
		NET_CAMERA_OK,

		/**
		 * 客户端发送给服务器的命令 客户端收到NET_CAMERA_SEND_PORTS 后，在用到的端口创建socket 向服务器发送这个命令 2
		 */
		NET_CAMERA_PORT,

		/**
		 * 服务器发送给客户端的命令 要求客户端发送 将用到的端口都发送一个NET_CAMERA_PORT 命令过来 3
		 */
		NET_CAMERA_SEND_PORTS,

		/**
		 * 客户端发送给服务器的命令 camera 发送注册id， monitor发送要求链接到指定id的camera 4
		 */
		NET_CAMERA_ID,
		/**
		 * 服务器发送给客户端的命令. 说明这个数据包是你要链接的远程ip 和 端口 数据格式 byte1 :
		 * NET_CAMERA_PEER_PORTS; byte2,3: size byte3，size-2: 远程的ip和所有端口号 5
		 */
		NET_CAMERA_PEER_PORTS,
	};

	
	public ThroughNetUtil(Handler handler) {
		this.handler = handler;
	}

	// 发送数据格式 data[0]command : data[1]-data[2] size :　data[3] - data[size-1]
	// send data
	@Override
	public void run() {
		boolean flag = false;
		int num = 3;
		while(!flag) {
			DatagramSocket udpSocket = null;
			try {
				udpSocket = new DatagramSocket();
				udpSocket.setSoTimeout(20000);
			} catch (SocketException e) {
				if(udpSocket != null) {
					udpSocket.close();
					udpSocket = null;
				}
				return;
			}
			flag = requestCameraId(udpSocket);
			Log.d(TAG,	"requestCameraId result " + flag);
			if (flag) {
				flag = sendInterActiveData(udpSocket);
				Log.d(TAG,	"sendInterActiveData result " + flag);
				if(flag) {
					flag = sendCreatePortCommond(udpSocket);
					Log.d(TAG,	"sendCreatePortCommond result " + flag);
					if(flag) {
						byte[] buf = new byte[64];
						DatagramPacket rp = new DatagramPacket(buf, 64);
						try {
							udpSocket.receive(rp);
							if(buf[0] == SendUDTCommon.NET_CAMERA_PEER_PORTS.ordinal()) {
								int packetLength = rp.getLength(); // 13
								byte[] receSize = new byte[2];//
								System.arraycopy(buf, 1, receSize, 0, 2);
								int receContentLength = ByteUtil.bytesToShort(receSize);
								if((receContentLength + 3) == packetLength) {
									byte[] ipByte = new byte[4];
									byte[] port1Byte = new byte[2];
									byte[] port2Byte  = new byte[2];
									byte[] port3Byte  = new byte[2];
									System.arraycopy(buf, 3, ipByte, 0, 4);
									System.arraycopy(buf, 7, port1Byte , 0, 2);
									System.arraycopy(buf, 9, port2Byte , 0, 2);
									System.arraycopy(buf, 11, port3Byte , 0, 2);
									//int ip = ByteUtil.bytesToInt(ipByte);
									int port1 = ByteUtil.bytesToShort(port1Byte);
									int port2 = ByteUtil.bytesToShort(port2Byte);
									int port3 = ByteUtil.bytesToShort(port3Byte);
									Bundle bundle = new Bundle();
									/*System.out.println(ipByte[0] + " " + ipByte[1] + " " +ipByte[2] + " " + ipByte[2]);
									System.out.println(port1Byte[0] + " " + port1Byte[1]);
									System.out.println(port2Byte[0] + " " + port2Byte[1]);
									System.out.println(port3Byte[0] + " " + port3Byte[1]);*/
									bundle.putString("IPADDRESS", InetAddress.getByAddress(ipByte).toString());
									bundle.putInt("PORT1", port1>0?port1:-port1);
									bundle.putInt("PORT2", port2>0?port2:-port2);
									bundle.putInt("PORT3", port3>0?port3:-port3);
									Message msg = handler.obtainMessage();
									msg.what = 1;
									msg.setData(bundle);
									handler.sendMessage(msg);
									flag = true; 
									Log.d(TAG,	"ThroughNetUtil get ip and tree port from server success  " + flag);
									break;
								}else {
									flag = false;	
									Log.d(TAG,	"ThroughNetUtil get unlegal package from server when quest ip and tree port" + flag);
								}
							}else {
								System.out.println(buf[0] + " " + buf[1] + " " + buf[2] + " " + buf[3] + " " + buf[4] + " " + buf[5] + " " + buf[6] + " " + buf[7] + " " + buf[8] + " " + buf[9] + " " + buf[10] + " " + buf[11] + " " + buf[12]);
								Log.d(TAG,	"ThroughNetUtil get unfull package from server when quest ip and tree port" + flag);
								flag = false;
							}
						} catch (IOException e) {
							Log.d(TAG, "ThroughNetUtil receive ip and port info error ! " + e.getLocalizedMessage());
							flag = false;
						}
					}
				}
			}
			num--;
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(num<=0) {
				flag = true;
			}
			Log.d(TAG,	"ThroughNetUtil try connect num = " + num);
		}
	}

	private boolean sendCreatePortCommond(DatagramSocket udpSocket) {
		// 1、构造UDP数据包 请求连接到指定ID的camera
		byte[] connCameraId = new byte[] { (byte) SendUDTCommon.NET_CAMERA_PORT.ordinal() };
		// 2、send data content
		byte[] sendDataContent = ByteUtil.intToBytes(0);
		// 3、send data length
		byte[] sendDataLength = ByteUtil.shortToBytes((short) 0);

		int l1 = connCameraId.length;
		int l2 = sendDataContent.length;
		int l3 = sendDataLength.length;
		byte[] sendData = new byte[l1 + l2 + l3];
		// copy commid to send data
		System.arraycopy(connCameraId, 0, sendData, 0, l1);
		// copy data length to send data
		System.arraycopy(sendDataLength, 0, sendData, l1, l3);
		// copy data content to send data
		System.arraycopy(sendDataContent, 0, sendData, l1 + l3, l2);
		DatagramPacket datagramPacket;
		try {
			DatagramSocket port1 = new DatagramSocket(); 
			datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
					InetAddress.getByName(Common.SERVER_IP),
					Common.INTERACTIVE_PORT);
			port1.setSoTimeout(20000);
			port1.send(datagramPacket);
			Log.d(TAG,	"send port 1 success");
			port1.close();
			port1 = null;
		} catch (Exception e) {
			Log.d(TAG, "ThroughNetUtil send port 1 error! " + e.getLocalizedMessage());
			return false;
		}
		byte[] buf = new byte[64];
		DatagramPacket rp = new DatagramPacket(buf, 64);
		try {
			udpSocket.receive(rp);
			Log.d(TAG,	"receive relay port 1 ");
			byte[] rece = rp.getData();
			if (rece[0] == SendUDTCommon.NET_CAMERA_OK.ordinal()) {
				try {
					DatagramSocket port2 = new DatagramSocket(); 
					datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
							InetAddress.getByName(Common.SERVER_IP),
							Common.INTERACTIVE_PORT);
					port2.setSoTimeout(20000);
					port2.send(datagramPacket);
					Log.d(TAG,	"send port 2 success");
					port2.close();
					port2 = null;
				} catch (Exception e) {
					Log.d(TAG, "ThroughNetUtil send port 2 error! " + e.getLocalizedMessage());
					return false;
				}
				try {
					udpSocket.receive(rp);
					Log.d(TAG,	"receive relay port 2 ");
					if (rece[0] == SendUDTCommon.NET_CAMERA_OK.ordinal()) {
						try {
							DatagramSocket port3 = new DatagramSocket(); 
							datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
									InetAddress.getByName(Common.SERVER_IP),
									Common.INTERACTIVE_PORT);
							port3.setSoTimeout(20000);
							port3.send(datagramPacket);
							Log.d(TAG,	"send port 3 success");
							port3.close();
							port3 = null;
						} catch (Exception e) {
							Log.d(TAG, "ThroughNetUtil send port 3 error! " + e.getLocalizedMessage());
							return false;
						}
						try {
							udpSocket.receive(rp);
							Log.d(TAG,	"receive relay port 3 ");
							if (rece[0] == SendUDTCommon.NET_CAMERA_OK.ordinal()) {
								return true;
							}
						} catch (IOException e) {
							Log.d(TAG,
									"ThroughNetUtil request connect camera by id receive data error! "
											+ e.getLocalizedMessage());
							return false;
						}
					}
				} catch (IOException e) {
					Log.d(TAG, "ThroughNetUtil request connect camera by id receive data error! " + e.getLocalizedMessage());
					return false;
				}
			}
		} catch (IOException e) {
			Log.d(TAG,	"receive relay port error ! ");
			return false;
		}
		return false;
	}

	public boolean sendInterActiveData(DatagramSocket udpSocket) {
		// 1、构造UDP数据包 请求连接到指定ID的camera
		byte[] connCameraId = new byte[] { (byte) 0 };
		// 2、send data content
		byte[] sendDataContent = ByteUtil.intToBytes(0);
		// 3、send data length
		byte[] sendDataLength = ByteUtil.shortToBytes((short) 0);

		int l1 = connCameraId.length;
		int l2 = sendDataContent.length;
		int l3 = sendDataLength.length;
		byte[] sendData = new byte[l1 + l2 + l3];
		// copy commid to send data
		System.arraycopy(connCameraId, 0, sendData, 0, l1);
		// copy data length to send data
		System.arraycopy(sendDataLength, 0, sendData, l1, l3);
		// copy data content to send data
		System.arraycopy(sendDataContent, 0, sendData, l1 + l3, l2);
		DatagramPacket datagramPacket;
		try {
			datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
					InetAddress.getByName(Common.SERVER_IP),
					Common.INTERACTIVE_PORT);
			udpSocket.send(datagramPacket);
		} catch (Exception e) {
			Log.d(TAG,
					"ThroughNetUtil request inter active fail! "
							+ e.getLocalizedMessage());
			return false;
		}
		byte[] buf = new byte[64];
		DatagramPacket rp = new DatagramPacket(buf, 64);
		try {
			udpSocket.receive(rp);
			byte[] rece = rp.getData();
			if (rece[0] == SendUDTCommon.NET_CAMERA_SEND_PORTS.ordinal()) {
				return true;
			}
		} catch (IOException e) {
			Log.d(TAG,
					"ThroughNetUtil request connect camera by id receive data error! "
							+ e.getLocalizedMessage());
			return false;
		}
		return false;
	}

	private boolean requestCameraId(DatagramSocket udpSocket) {
		// 1、构造UDP数据包 请求连接到指定ID的camera
		byte[] connCameraId = new byte[] { (byte) SendUDTCommon.NET_CAMERA_ID
				.ordinal() };// common id
		// 2、send data content
		byte[] sendDataContent = ByteUtil.intToBytes(1);
		// 3、send data length
		byte[] sendDataLength = ByteUtil
				.shortToBytes((short) sendDataContent.length);

		int l1 = connCameraId.length;
		int l2 = sendDataContent.length;
		int l3 = sendDataLength.length;
		byte[] sendData = new byte[l1 + l2 + l3];
		// copy commid to send data
		System.arraycopy(connCameraId, 0, sendData, 0, l1);
		// copy data length to send data
		System.arraycopy(sendDataLength, 0, sendData, l1, l3);
		// copy data content to send data
		System.arraycopy(sendDataContent, 0, sendData, l1 + l3, l2);
		DatagramPacket datagramPacket;
		try {
			datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
					InetAddress.getByName(Common.SERVER_IP),
					Common.CLIENT_WATCH_PORT);
			udpSocket.send(datagramPacket);
		} catch (Exception e) {
			Log.d(TAG, "ThroughNetUtil request connect camera id fail! "
							+ e.getLocalizedMessage());
			return false;
		}
		byte[] buf = new byte[64];
		DatagramPacket rp = new DatagramPacket(buf, 64);
		try {
			udpSocket.receive(rp);
			byte[] rece = rp.getData();
			if (rece[0] == SendUDTCommon.NET_CAMERA_OK.ordinal()) {
				return true;
			}
		} catch (IOException e) {
			Log.d(TAG,
					"ThroughNetUtil request connect camera by id receive data error! "
							+ e.getLocalizedMessage());
			return false;
		}
		return false;
	}
}
