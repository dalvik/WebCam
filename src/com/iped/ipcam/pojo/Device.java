package com.iped.ipcam.pojo;

public class Device {

	private String deviceName;
	
	private String deviceType;
	
	private String deviceIp;
	
	private int deviceTcpPort;
	
	private int deviceUdpPort;
	
	private String deviceGateWay;

	public Device() {
		super();
	}

	public Device(String deviceName, String deviceType, String deviceIp,
			int deviceTcpPort, int deviceUdpPort, String deviceGateWay) {
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

	public int getDeviceTcpPort() {
		return deviceTcpPort;
	}

	public void setDeviceTcpPort(int deviceTcpPort) {
		this.deviceTcpPort = deviceTcpPort;
	}

	public int getDeviceUdpPort() {
		return deviceUdpPort;
	}

	public void setDeviceUdpPort(int deviceUdpPort) {
		this.deviceUdpPort = deviceUdpPort;
	}

	public String getDeviceGateWay() {
		return deviceGateWay;
	}

	public void setDeviceGateWay(String deviceGateWay) {
		this.deviceGateWay = deviceGateWay;
	}

	@Override
	public String toString() {
		return "Device [deviceGateWay=" + deviceGateWay + ", deviceIp="
				+ deviceIp + ", deviceName=" + deviceName + ", deviceTcpPort="
				+ deviceTcpPort + ", deviceType=" + deviceType
				+ ", deviceUdpPort=" + deviceUdpPort + "]";
	}

}
