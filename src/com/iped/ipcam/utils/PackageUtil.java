package com.iped.ipcam.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.iped.ipcam.exception.CamManagerException;

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
		String tmp = null;
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(receArr, Constants.COMMNICATEBUFFERSIZE);
			datagramSocket.receive(rece);
			tmp = new String(receArr);
			//Log.d(TAG, "Receive inof //////////////" + tmp);
			return tmp.trim();
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
