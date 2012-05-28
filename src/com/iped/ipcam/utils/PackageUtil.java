package com.iped.ipcam.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.gui.CamVideoH264;

import android.util.Log;

public class PackageUtil {

	private static String TAG = "PackageUtil";
	
	public static boolean CMDPackage(String cmdType, String ip, int port) {
		byte [] tem = cmdType.getBytes();
		DatagramSocket datagramSocket = null;
		
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
			DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			datagramSocket.receive(rece);
			Log.d(TAG, "Receive inof //////////////" + new String(buffTemp));
			return true;
		} catch (SocketException e) {
			//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} catch (UnknownHostException e) {
			//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} catch (IOException e) {
			//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} finally {
			if(datagramSocket != null) {
				datagramSocket.disconnect();
				datagramSocket.close();
				datagramSocket = null;
			}
		}
		return false;
	}
	
	public static boolean isOnline(String ip, int port) throws CamManagerException {
		DatagramSocket datagramSocket = null;
		 byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
		
		byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(5000);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			datagramSocket.receive(rece);
			Log.d(TAG, "receive inof ");
			return true;
		} catch (SocketException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} catch (UnknownHostException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} catch (IOException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} finally {
			if(datagramSocket != null) {
				datagramSocket.close();
				datagramSocket = null;
			}
		}
	}
	
	public static String CMDPackage2(String cmdType, String ip, int port) {
		byte [] tem = cmdType.getBytes();
		byte[] receArr = new byte[Constants.COMMNICATEBUFFERSIZE];
		DatagramSocket datagramSocket = null;
		ThroughNetUtil netUtil = CamVideoH264.getInstance();
		if(netUtil == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		String tmp = null;
		try {
			datagramSocket = netUtil.getPort1(); //new DatagramSocket();
			if(datagramSocket == null) {
				return null;
			}
			netUtil.clearRecvBuffer();
			DatagramPacket datagramPacket = new DatagramPacket(tem, cmdType.length(), InetAddress.getByName(CamVideoH264.currIpAddress), CamVideoH264.port1);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(receArr, Constants.COMMNICATEBUFFERSIZE);
			int recvLength = 0;
			while(true) {
				datagramSocket.receive(rece);
				int l = rece.getLength();
				byte[] ipByte = new byte[4];
				System.arraycopy(receArr, 0, ipByte, 0, 4);
				recvLength += Integer.parseInt(new String(ipByte).trim());
				tmp = new String(receArr,4,l-4).trim();
				sb.append(tmp);
				Log.d(TAG, "Receive inof //////////////"  + l + " " + tmp);
				if(recvLength>1000) {
					break;
				}
			}
			return sb.toString();
		} catch (SocketException e) {
			Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} catch (UnknownHostException e) {
			Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
		} 
		return tmp;
	}
	
	public static byte[] deleteZero(byte[]files) {
		int index = 0;
		int length = files.length;
		if(length<8) {
			return files;
		}
		for(int i=8;i<length;i++){
			if(files[i] == 0) {
				break;
			}
			index = i;
		}
		byte[] temp = new byte[index-8];
		System.arraycopy(files, 8, temp, 0, index-8);
		return temp;
	}
}
