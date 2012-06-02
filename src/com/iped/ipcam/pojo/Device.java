package com.iped.ipcam.pojo;

import java.io.Serializable;

public class Device implements Serializable{

	private static final long serialVersionUID = 1L;

	private String deviceName;
	
	private String deviceID;
	
	private String diviceVersion;
	
	private String deviceEthIp;
	
	private String deviceEthGateWay;
	
	private String deviceEthMask;
	
	private String deviceEthDNS1;
	
	private String deviceEthDNS2;
	
	private String deviceWlanIp;
	
	private String deviceWlanGateWay;
	
	private String deviceWlanMask;
	
	private String deviceWlanDNS1;
	
	private String deviceWlanDNS2;
	
	private int deviceLocalCmdPort;
	
	private int deviceLocalVideoPort;
	
	private int deviceLocalAudioPort;
	
	private int deviceRemoteCmdPort;
	
	private int deviceRemoteVideoPort;
	
	private int deviceRemoteAudioPort;
	
	private boolean deviceNetType;// 内网还是外网
	
	private String unDefine1;
	
	private String unDefine2;
	
	private String unDefine3;

	public Device() {
		super();
	}

	public Device(String deviceName, String deviceID) {
		super();
		this.deviceName = deviceName;
		this.deviceID = deviceID;
	}

	public Device(String deviceName, String deviceID,
			String deviceEthIp, String deviceEthGateWay, String deviceEthMask,
			String deviceEthDNS1, String deviceEthDNS2) {
		super();
		this.deviceName = deviceName;
		this.deviceID = deviceID;
		this.deviceEthIp = deviceEthIp;
		this.deviceEthGateWay = deviceEthGateWay;
		this.deviceEthMask = deviceEthMask;
		this.deviceEthDNS1 = deviceEthDNS1;
		this.deviceEthDNS2 = deviceEthDNS2;
	}

	public Device(String deviceWlanIp, String deviceWlanGateWay,
			String deviceWlanMask, String deviceWlanDNS1, String deviceWlanDNS2) {
		super();
		this.deviceWlanIp = deviceWlanIp;
		this.deviceWlanGateWay = deviceWlanGateWay;
		this.deviceWlanMask = deviceWlanMask;
		this.deviceWlanDNS1 = deviceWlanDNS1;
		this.deviceWlanDNS2 = deviceWlanDNS2;
	}

	public Device(String deviceName, String deviceID,
			String deviceEthIp, String deviceWlanIp, int deviceLocalCmdPort,
			int deviceLocalVideoPort, int deviceLocalAudioPort,
			int deviceRemoteCmdPort, int deviceRemoteVideoPort,
			int deviceRemoteAudioPort) {
		super();
		this.deviceName = deviceName;
		this.deviceID = deviceID;
		this.deviceEthIp = deviceEthIp;
		this.deviceWlanIp = deviceWlanIp;
		this.deviceLocalCmdPort = deviceLocalCmdPort;
		this.deviceLocalVideoPort = deviceLocalVideoPort;
		this.deviceLocalAudioPort = deviceLocalAudioPort;
		this.deviceRemoteCmdPort = deviceRemoteCmdPort;
		this.deviceRemoteVideoPort = deviceRemoteVideoPort;
		this.deviceRemoteAudioPort = deviceRemoteAudioPort;
	}

	public Device(String deviceName, String deviceID,
			String deviceEthIp, String deviceEthGateWay, String deviceEthMask,
			String deviceEthDNS1, String deviceEthDNS2, String deviceWlanIp,
			String deviceWlanGateWay, String deviceWlanMask,
			String deviceWlanDNS1, String deviceWlanDNS2,
			int deviceLocalCmdPort, int deviceLocalVideoPort,
			int deviceLocalAudioPort, int deviceRemoteCmdPort,
			int deviceRemoteVideoPort, int deviceRemoteAudioPort,
			boolean deviceNetType, String unDefine1, String unDefine2,
			String unDefine3) {
		super();
		this.deviceName = deviceName;
		this.deviceID = deviceID;
		this.deviceEthIp = deviceEthIp;
		this.deviceEthGateWay = deviceEthGateWay;
		this.deviceEthMask = deviceEthMask;
		this.deviceEthDNS1 = deviceEthDNS1;
		this.deviceEthDNS2 = deviceEthDNS2;
		this.deviceWlanIp = deviceWlanIp;
		this.deviceWlanGateWay = deviceWlanGateWay;
		this.deviceWlanMask = deviceWlanMask;
		this.deviceWlanDNS1 = deviceWlanDNS1;
		this.deviceWlanDNS2 = deviceWlanDNS2;
		this.deviceLocalCmdPort = deviceLocalCmdPort;
		this.deviceLocalVideoPort = deviceLocalVideoPort;
		this.deviceLocalAudioPort = deviceLocalAudioPort;
		this.deviceRemoteCmdPort = deviceRemoteCmdPort;
		this.deviceRemoteVideoPort = deviceRemoteVideoPort;
		this.deviceRemoteAudioPort = deviceRemoteAudioPort;
		this.deviceNetType = deviceNetType;
		this.unDefine1 = unDefine1;
		this.unDefine2 = unDefine2;
		this.unDefine3 = unDefine3;
	}

	public Device(String deviceName, 
			String deviceEthIp, String deviceEthGateWay, String deviceEthMask,
			String deviceEthDNS1, String deviceEthDNS2, String deviceWlanIp,
			String deviceWlanGateWay, String deviceWlanMask,
			String deviceWlanDNS1, String deviceWlanDNS2,
			int deviceLocalCmdPort, int deviceLocalVideoPort,
			int deviceLocalAudioPort, int deviceRemoteCmdPort,
			int deviceRemoteVideoPort, int deviceRemoteAudioPort,
			boolean deviceNetType) {
		super();
		this.deviceName = deviceName;
		this.deviceEthIp = deviceEthIp;
		this.deviceEthGateWay = deviceEthGateWay;
		this.deviceEthMask = deviceEthMask;
		this.deviceEthDNS1 = deviceEthDNS1;
		this.deviceEthDNS2 = deviceEthDNS2;
		this.deviceWlanIp = deviceWlanIp;
		this.deviceWlanGateWay = deviceWlanGateWay;
		this.deviceWlanMask = deviceWlanMask;
		this.deviceWlanDNS1 = deviceWlanDNS1;
		this.deviceWlanDNS2 = deviceWlanDNS2;
		this.deviceLocalCmdPort = deviceLocalCmdPort;
		this.deviceLocalVideoPort = deviceLocalVideoPort;
		this.deviceLocalAudioPort = deviceLocalAudioPort;
		this.deviceRemoteCmdPort = deviceRemoteCmdPort;
		this.deviceRemoteVideoPort = deviceRemoteVideoPort;
		this.deviceRemoteAudioPort = deviceRemoteAudioPort;
		this.deviceNetType = deviceNetType;
	}

	
	public Device(String deviceName, String deviceWlanIp,
			String deviceWlanGateWay, String deviceWlanMask,
			String deviceWlanDNS1, String deviceWlanDNS2, boolean deviceNetType) {
		super();
		this.deviceName = deviceName;
		this.deviceWlanIp = deviceWlanIp;
		this.deviceWlanGateWay = deviceWlanGateWay;
		this.deviceWlanMask = deviceWlanMask;
		this.deviceWlanDNS1 = deviceWlanDNS1;
		this.deviceWlanDNS2 = deviceWlanDNS2;
		this.deviceNetType = deviceNetType;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public String getDiviceVersion() {
		return diviceVersion;
	}

	public void setDiviceVersion(String diviceVersion) {
		this.diviceVersion = diviceVersion;
	}

	public String getDeviceEthIp() {
		return deviceEthIp;
	}

	public void setDeviceEthIp(String deviceEthIp) {
		this.deviceEthIp = deviceEthIp;
	}

	public String getDeviceEthGateWay() {
		return deviceEthGateWay;
	}

	public void setDeviceEthGateWay(String deviceEthGateWay) {
		this.deviceEthGateWay = deviceEthGateWay;
	}

	public String getDeviceEthMask() {
		return deviceEthMask;
	}

	public void setDeviceEthMask(String deviceEthMask) {
		this.deviceEthMask = deviceEthMask;
	}

	public String getDeviceEthDNS1() {
		return deviceEthDNS1;
	}

	public void setDeviceEthDNS1(String deviceEthDNS1) {
		this.deviceEthDNS1 = deviceEthDNS1;
	}

	public String getDeviceEthDNS2() {
		return deviceEthDNS2;
	}

	public void setDeviceEthDNS2(String deviceEthDNS2) {
		this.deviceEthDNS2 = deviceEthDNS2;
	}

	public String getDeviceWlanIp() {
		return deviceWlanIp;
	}

	public void setDeviceWlanIp(String deviceWlanIp) {
		this.deviceWlanIp = deviceWlanIp;
	}

	public String getDeviceWlanGateWay() {
		return deviceWlanGateWay;
	}

	public void setDeviceWlanGateWay(String deviceWlanGateWay) {
		this.deviceWlanGateWay = deviceWlanGateWay;
	}

	public String getDeviceWlanMask() {
		return deviceWlanMask;
	}

	public void setDeviceWlanMask(String deviceWlanMask) {
		this.deviceWlanMask = deviceWlanMask;
	}

	public String getDeviceWlanDNS1() {
		return deviceWlanDNS1;
	}

	public void setDeviceWlanDNS1(String deviceWlanDNS1) {
		this.deviceWlanDNS1 = deviceWlanDNS1;
	}

	public String getDeviceWlanDNS2() {
		return deviceWlanDNS2;
	}

	public void setDeviceWlanDNS2(String deviceWlanDNS2) {
		this.deviceWlanDNS2 = deviceWlanDNS2;
	}

	public int getDeviceLocalCmdPort() {
		return deviceLocalCmdPort;
	}

	public void setDeviceLocalCmdPort(int deviceLocalCmdPort) {
		this.deviceLocalCmdPort = deviceLocalCmdPort;
	}

	public int getDeviceLocalVideoPort() {
		return deviceLocalVideoPort;
	}

	public void setDeviceLocalVideoPort(int deviceLocalVideoPort) {
		this.deviceLocalVideoPort = deviceLocalVideoPort;
	}

	public int getDeviceLocalAudioPort() {
		return deviceLocalAudioPort;
	}

	public void setDeviceLocalAudioPort(int deviceLocalAudioPort) {
		this.deviceLocalAudioPort = deviceLocalAudioPort;
	}

	public int getDeviceRemoteCmdPort() {
		return deviceRemoteCmdPort;
	}

	public void setDeviceRemoteCmdPort(int deviceRemoteCmdPort) {
		this.deviceRemoteCmdPort = deviceRemoteCmdPort;
	}

	public int getDeviceRemoteVideoPort() {
		return deviceRemoteVideoPort;
	}

	public void setDeviceRemoteVideoPort(int deviceRemoteVideoPort) {
		this.deviceRemoteVideoPort = deviceRemoteVideoPort;
	}

	public int getDeviceRemoteAudioPort() {
		return deviceRemoteAudioPort;
	}

	public void setDeviceRemoteAudioPort(int deviceRemoteAudioPort) {
		this.deviceRemoteAudioPort = deviceRemoteAudioPort;
	}

	public boolean getDeviceNetType() {
		return deviceNetType;
	}

	public void setDeviceNetType(boolean deviceNetType) {
		this.deviceNetType = deviceNetType;
	}

	public String getUnDefine1() {
		return unDefine1;
	}

	public void setUnDefine1(String unDefine1) {
		this.unDefine1 = unDefine1;
	}

	public String getUnDefine2() {
		return unDefine2;
	}

	public void setUnDefine2(String unDefine2) {
		this.unDefine2 = unDefine2;
	}

	public String getUnDefine3() {
		return unDefine3;
	}

	public void setUnDefine3(String unDefine3) {
		this.unDefine3 = unDefine3;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((deviceID == null) ? 0 : deviceID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		if (deviceID == null) {
			if (other.deviceID != null)
				return false;
		} else if (!deviceID.equals(other.deviceID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Device [deviceName=" + deviceName + ", deviceID=" + deviceID
				+ ", diviceVersion=" + diviceVersion + ", deviceEthIp="
				+ deviceEthIp + ", deviceEthGateWay=" + deviceEthGateWay
				+ ", deviceEthMask=" + deviceEthMask + ", deviceEthDNS1="
				+ deviceEthDNS1 + ", deviceEthDNS2=" + deviceEthDNS2
				+ ", deviceWlanIp=" + deviceWlanIp + ", deviceWlanGateWay="
				+ deviceWlanGateWay + ", deviceWlanMask=" + deviceWlanMask
				+ ", deviceWlanDNS1=" + deviceWlanDNS1 + ", deviceWlanDNS2="
				+ deviceWlanDNS2 + ", deviceLocalCmdPort=" + deviceLocalCmdPort
				+ ", deviceLocalVideoPort=" + deviceLocalVideoPort
				+ ", deviceLocalAudioPort=" + deviceLocalAudioPort
				+ ", deviceRemoteCmdPort=" + deviceRemoteCmdPort
				+ ", deviceRemoteVideoPort=" + deviceRemoteVideoPort
				+ ", deviceRemoteAudioPort=" + deviceRemoteAudioPort
				+ ", deviceNetType=" + deviceNetType + ", unDefine1="
				+ unDefine1 + ", unDefine2=" + unDefine2 + ", unDefine3="
				+ unDefine3 + "]";
	}
	

}
