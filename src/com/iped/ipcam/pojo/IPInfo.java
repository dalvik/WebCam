package com.iped.ipcam.pojo;

public class IPInfo {

	private int id;
	
	private String ip;
	
	private int port1;
	
	private int port2;
	
	private int port3;

	public IPInfo() {
		super();
	}

	public IPInfo(String ip, int port1, int port2, int port3) {
		super();
		this.ip = ip;
		this.port1 = port1;
		this.port2 = port2;
		this.port3 = port3;
	}


	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort1() {
		return port1;
	}

	public void setPort1(int port1) {
		this.port1 = port1;
	}

	public int getPort2() {
		return port2;
	}

	public void setPort2(int port2) {
		this.port2 = port2;
	}

	public int getPort3() {
		return port3;
	}

	public void setPort3(int port3) {
		this.port3 = port3;
	}
	
	
}
