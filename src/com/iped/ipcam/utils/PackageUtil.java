package com.iped.ipcam.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
			DatagramPacket rece = new DatagramPacket(tem, tem.length);
			datagramSocket.receive(rece);
			Log.d(TAG, "receive inof //////////////" + ip);
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
}
