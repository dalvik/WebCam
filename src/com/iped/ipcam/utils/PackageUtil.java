package com.iped.ipcam.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import android.util.Log;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;

public class PackageUtil {

	private static String TAG = "PackageUtil";

	private static byte[] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];

	public static String CMDPackage(String cmdType, String ip, int port) {
		byte[] tem = cmdType.getBytes();
		DatagramSocket datagramSocket = null;

		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem,
					cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			datagramSocket.receive(rece);
			int receLength = rece.getLength();
			String receStr = new String(buffTemp, 0, receLength);
			Log.d(TAG, "Receive inof //////////////" + receStr);
			return receStr;
		} catch (SocketException e) {
			// Log.d(TAG, "CamManagerImp isoffline : " +
			// (Constants.DEFAULTSEARCHIP + ip) + " " +
			// e.getLocalizedMessage());
		} catch (UnknownHostException e) {
			// Log.d(TAG, "CamManagerImp isoffline : " +
			// (Constants.DEFAULTSEARCHIP + ip) + " " +
			// e.getLocalizedMessage());
		} catch (IOException e) {
			// Log.d(TAG, "CamManagerImp isoffline : " +
			// (Constants.DEFAULTSEARCHIP + ip) + " " +
			// e.getLocalizedMessage());
		} finally {
			if (datagramSocket != null) {
				datagramSocket.disconnect();
				datagramSocket.close();
				datagramSocket = null;
			}
		}
		return null;
	}

	public static String CMDPackage2(ThroughNetUtil netUtil, String cmdType,
			String ip, int port) throws CamManagerException {
		byte[] tem = cmdType.getBytes();
		byte[] receArr = new byte[Constants.COMMNICATEBUFFERSIZE];
		DatagramSocket datagramSocket = null;
		datagramSocket = netUtil.getPort1(); // new DatagramSocket();
		String tmp = null;
		DatagramPacket rece = new DatagramPacket(receArr,
				Constants.COMMNICATEBUFFERSIZE);
		if (datagramSocket == null) {
			return null;
		}
		boolean flag = true;
		try {
			datagramSocket.setSoTimeout(100);
			while (flag) {
				datagramSocket.receive(rece);
			}
		} catch (IOException e) {
			flag = false;
			e.printStackTrace();
		}
		try {
			// netUtil.clearRecvBuffer();
			datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem,
					cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			datagramSocket.receive(rece);
			int l = rece.getLength();
			if (l <= 10) {
				return new String(receArr, 0, l);
			}
			byte[] ipByte = new byte[4];
			System.arraycopy(receArr, 0, ipByte, 0, 4);
			tmp = new String(receArr, 4, l - 4).trim();
			Log.d(TAG, "Receive inof //////////////" + l + " " + tmp);
			return tmp;
			/*
			 * int recvLength = 0; while(true) { datagramSocket.receive(rece);
			 * int l = rece.getLength(); byte[] ipByte = new byte[4];
			 * System.arraycopy(receArr, 0, ipByte, 0, 4); recvLength +=
			 * Integer.parseInt(new String(ipByte).trim()); tmp = new
			 * String(receArr,4,l-4).trim(); sb.append(tmp); Log.d(TAG,
			 * "Receive inof //////////////" + l + " " + tmp);
			 * if(recvLength>1000) { break; } } return sb.toString();
			 */
		} catch (IOException e) {
			Log.d(TAG,
					"PackageUtil CMDPackage2 : " + ip + " "
							+ e.getLocalizedMessage());
			throw new CamManagerException();
		}
	}

	public static String sendPackageByIp(String cmdType, String ip, int port)
			throws CamManagerException {
		byte[] tem = cmdType.getBytes();
		byte[] receArr = new byte[Constants.COMMNICATEBUFFERSIZE];
		DatagramSocket datagramSocket = null;
		String tmp = null;
		try {
			datagramSocket = new DatagramSocket();
			System.out.println(datagramSocket.getLocalPort() + "  "
					+ datagramSocket.getPort());
			DatagramPacket datagramPacket = new DatagramPacket(tem,
					cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(receArr,
					Constants.COMMNICATEBUFFERSIZE);
			datagramSocket.receive(rece);
			int l = rece.getLength();
			if (l <= 10) {
				return new String(receArr, 0, l);
			}
			byte[] ipByte = new byte[4];
			System.arraycopy(receArr, 0, ipByte, 0, 4);
			tmp = new String(receArr, 4, l - 4).trim();
			Log.d(TAG, "Receive inof //////////////" + l + " " + tmp);
			return tmp;
		} catch (IOException e) {
			Log.d(TAG, "PackageUtil CMDPackage2 : " + ip + " " + e.getMessage());
			throw new CamManagerException();
		}
	}

	public static void sendPackageNoRecvByIp(String cmdType, String ip, int port)
			throws CamManagerException {
		byte[] tem = cmdType.getBytes();
		DatagramSocket datagramSocket = null;
		try {
			System.out.println(cmdType + " " + ip + " " + port);
			datagramSocket = new DatagramSocket();
			DatagramPacket datagramPacket = new DatagramPacket(tem,
					cmdType.length(), InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			Log.d(TAG, "sendPackageNoRecvByIp : "
					+ (Constants.DEFAULTSEARCHIP + ip) + " " + e.getMessage());
			throw new CamManagerException("sendPackageNoRecvByIp : " + ip + " "
					+ e.getMessage());
		} finally {
			if (datagramSocket != null) {
				datagramSocket.close();
				datagramSocket = null;
			}
		}
	}

	public static boolean pingTest(String cmdType, String ip, int port) {
		byte[] tem = cmdType.getBytes();
		DatagramSocket datagramSocket = null;
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(500);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length,
					InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			datagramSocket.receive(datagramPacket);
			return true;
		} catch (IOException e) {
			Log.d(TAG, "pingTest : " + ip + " " + e.getMessage());
			return false;
		} finally {
			if (datagramSocket != null) {
				datagramSocket.close();
			}
		}
	}

	public static byte[] deleteZero(byte[] files) {
		int index = 0;
		int length = files.length;
		if (length < 8) {
			return files;
		}
		for (int i = 8; i < length; i++) {
			if (files[i] == 0) {
				index = i;
				break;
			}
			index = i;
		}
		byte[] temp = new byte[index - 8];
		System.arraycopy(files, 8, temp, 0, index - 8);
		return temp;
	}

	public static int checkPwdState(String id) {
		String tem = CamCmdListHelper.CheckCmd_Pwd_State;
		int res = UdtTools.sendCmdMsgById(id, tem, tem.length());
		Log.d(TAG, "checkPwdState = " + res);
		if(res <=0) {
			return -2; // time out
		}
		int bufLength = 100;
		byte[] recvBuf = new byte[bufLength];
		int recvLength = UdtTools.recvCmdMsgById(id, recvBuf, bufLength);
		Log.d(TAG, "### check pwd state length " + recvLength);
		if(recvLength<=0) {
			return -2; // time out
		}
		String recvStr = new String(recvBuf,0, recvLength);
		Log.d(TAG, "### check pwd state info " + recvStr);
		if ("PSWD_SET".equals(recvStr)) {
			return 1; // had set
		} else if ("PSWD_NOT_SET".equals(recvStr)) {
			return 0; // not set
		}
		return -2; // time out
	}

	public static int checkPwd(String id, String  pwd) {
		String tem = (CamCmdListHelper.CheckCmd_PWD + pwd);
		int res = UdtTools.sendCmdMsgById(id, tem, tem.length());
		Log.d(TAG, "checkPwd = " + res);
		if(res <= 0) {
			return -2;
		}
		int bufLength = 100;
		byte[] recvBuf = new byte[bufLength];
		int recvLength = UdtTools.recvCmdMsgById(id, recvBuf, bufLength);
		Log.d(TAG, "### check pwd recv length " + recvLength);
		if(recvLength<=0) {
			return -2; // time out
		}
		String recvStr = new String(recvBuf,0, recvLength);
		Log.d(TAG, "### checkPwd " + recvStr);
		if ("PSWD_OK".equals(recvStr)) {
			return 1; // PSWD_OK
		} else {
			return -1; // PSWD_FALL
		}
	}

	public static int setPwd(Device device, String common) {
		String id = device.getDeviceID();
		int res = UdtTools.sendCmdMsgById(id,common, common.length());
		Log.d(TAG, "setPwd = " + res);
		if(res <=0) {
			return -2; // time out
		}
		int bufLength = 100;
		byte[] recvBuf = new byte[bufLength];
		int recvLength = UdtTools.recvCmdMsgById(id, recvBuf, bufLength);
		Log.d(TAG, "### check set pwd recv length " + recvLength);
		if(recvLength<=0) {
			return -2; // time out
		}
		String recvStr = new String(recvBuf,0, recvLength);
		Log.d(TAG, "### check set pwd recv info " + recvStr);
		if ("PSWD_OK".equals(recvStr)) {
			return 1; // set success
		} else if ("PSWD_FAIL".equals(recvStr)) {
			return 0; // set error
		} else {
			return -1; // unknown
		}
	}

	public static boolean sendPTZCommond(int commId) {
		byte[] ptzCommonName = CamCmdListHelper.SetCmdPTZ.getBytes();// name
		int ptzCommonNameLength = ptzCommonName.length;
		byte[] direction = CamCmdListHelper.ptzMap.get(commId);// comm
		int commLength = direction.length;
		byte[] ptzCommonByte = new byte[ptzCommonNameLength + commLength + 2];
		byte check = 0;
		for (int i = 1; i < commLength; i++) {
			check ^= direction[i];
		}
		ptzCommonByte[ptzCommonByte.length - 2] = check;
		System.arraycopy(ptzCommonName, 0, ptzCommonByte, 0, ptzCommonNameLength);
		System.arraycopy(direction, 0, ptzCommonByte, ptzCommonNameLength, commLength);
		int res = UdtTools.sendPTZMsg(ptzCommonByte);
		//int res = UdtTools.sendCmdMsgById(id, new String(ptzCommonByte,0,ptzCommonNameLength + commLength + 2), ptzCommonByte.length);
		Log.d(TAG, "#### sendPTZCommond res = " + res + "  " + ptzCommonByte.length+ " " + new String(ptzCommonByte));
		return false;
	}

	public static int setBCV(String id, String comm, String value) {
		String BCVCommon = (comm + value);
		return UdtTools.sendCmdMsg(BCVCommon, BCVCommon.length());
	}
	
	public static String getConfigMode(Device device) {
	   String cmdStr = CamCmdListHelper.GetCmd_Config+device.getUnDefine2();
	   int res = UdtTools.sendCmdMsgById(device.getDeviceID(), cmdStr, cmdStr.length());
	   Log.d(TAG, "### get web cam config result = " + res);
	   if(res <= 0) {
		   return null;
		}
		int bufLength = 1500;
		byte[] recvBuf = new byte[bufLength];
		int recvLength = UdtTools.recvCmdMsgById(device.getDeviceID(), recvBuf, bufLength);
		Log.d(TAG, "### check pwd recv length " + recvLength);
		if(recvLength<=0) {
			return null;
		}
		String recvConfigStr = new String(recvBuf,0, recvLength);
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "### recvConfigStr " + recvConfigStr);
		}
		if(recvConfigStr != null && recvConfigStr.length()>=5) {
			Map<String, String> paraMap = new LinkedHashMap<String, String>();
			ParaUtil.putParaByString(recvConfigStr.substring(4), paraMap);
			if(paraMap.containsKey("model")){
				return paraMap.get("model");
			}
		}
		return null;
	}
}
