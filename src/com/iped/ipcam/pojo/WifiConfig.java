package com.iped.ipcam.pojo;

public class WifiConfig {

	private String ssid;
	
	private String signal_level;
	
	private String proto;
	
	private String key_mgmt;
	
	private String pairwise;
	
	private String group;

	public WifiConfig() {
		super();
	}

	public WifiConfig(String ssid, String signal_level, String proto,
			String key_mgmt, String pairwise, String group) {
		super();
		this.ssid = ssid;
		this.signal_level = signal_level;
		this.proto = proto;
		this.key_mgmt = key_mgmt;
		this.pairwise = pairwise;
		this.group = group;
	}

	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getSignal_level() {
		return signal_level;
	}

	public void setSignal_level(String signal_level) {
		this.signal_level = signal_level;
	}

	public String getProto() {
		return proto;
	}

	public void setProto(String proto) {
		this.proto = proto;
	}

	public String getKey_mgmt() {
		return key_mgmt;
	}

	public void setKey_mgmt(String key_mgmt) {
		this.key_mgmt = key_mgmt;
	}

	public String getPairwise() {
		return pairwise;
	}

	public void setPairwise(String pairwise) {
		this.pairwise = pairwise;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return "ssid=" + ssid + " signal_level=" + signal_level + " proto=" + proto;
	}
	
}
