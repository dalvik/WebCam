package com.iped.ipcam.pojo;

public class Device {

	private String deviceName;
	
	private String deviceType;
	
	private String deviceIp;
	
	private String deviceTcpPort;
	
	private String deviceUdpPort;
	
	private String deviceGateWay;

	public Device() {
		super();
	}

	public Device(String deviceName, String deviceType, String deviceIp,
			String deviceTcpPort, String deviceUdpPort, String deviceGateWay) {
		super();
		this.deviceName = deviceName;
		this.deviceType = deviceType;
		this.deviceIp = deviceIp;
		this.deviceTcpPort = deviceTcpPort;
		this.deviceUdpPort = deviceUdpPort;
		this.deviceGateWay = deviceGateWay;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceIp() {
		return deviceIp;
	}

	public void setDeviceIp(String deviceIp) {
		this.deviceIp = deviceIp;
	}

	public String getDeviceTcpPort() {
		return deviceTcpPort;
	}

	public void setDeviceTcpPort(String deviceTcpPort) {
		this.deviceTcpPort = deviceTcpPort;
	}

	public String getDeviceUdpPort() {
		return deviceUdpPort;
	}

	public void setDeviceUdpPort(String deviceUdpPort) {
		this.deviceUdpPort = deviceUdpPort;
	}

	public String getDeviceGateWay() {
		return deviceGateWay;
	}

	public void setDeviceGateWay(String deviceGateWay) {
		this.deviceGateWay = deviceGateWay;
	}
	
}
