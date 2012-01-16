package com.iped.ipcam.pojo;

import android.os.Parcel;
import android.os.Parcelable;

public class CamConfig implements Parcelable{

	private String version;
	
	private String addrType;
	
	private String inTotalSpace;
	
	private String outTotalSpace;
	
	private String validRecordTime;

	
	private String frameRate;
	
	private String compression;
	
	private String resolution;
	
	private String gop;
	
	private String bitRate;

	public CamConfig() {
		super();
	}

	public CamConfig(String version, String addrType, String inTotalSpace,
			String outTotalSpace, String validRecordTime) {
		super();
		this.version = version;
		this.addrType = addrType;
		this.inTotalSpace = inTotalSpace;
		this.outTotalSpace = outTotalSpace;
		this.validRecordTime = validRecordTime;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAddrType() {
		return addrType;
	}

	public void setAddrType(String addrType) {
		this.addrType = addrType;
	}

	public String getInTotalSpace() {
		return inTotalSpace;
	}

	public void setInTotalSpace(String inTotalSpace) {
		this.inTotalSpace = inTotalSpace;
	}

	public String getOutTotalSpace() {
		return outTotalSpace;
	}

	public void setOutTotalSpace(String outTotalSpace) {
		this.outTotalSpace = outTotalSpace;
	}

	public String getValidRecordTime() {
		return validRecordTime;
	}

	public void setValidRecordTime(String validRecordTime) {
		this.validRecordTime = validRecordTime;
	}

	@Override
	public String toString() {
		return "CamConfig [addrType=" + addrType + ", bitRate=" + bitRate
				+ ", compression=" + compression + ", frameRate=" + frameRate
				+ ", gop=" + gop + ", inTotalSpace=" + inTotalSpace
				+ ", outTotalSpace=" + outTotalSpace + ", resolution="
				+ resolution + ", validRecordTime=" + validRecordTime
				+ ", version=" + version + "]";
	}

	public String getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
	}

	public String getCompression() {
		return compression;
	}

	public void setCompression(String compression) {
		this.compression = compression;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getGop() {
		return gop;
	}

	public void setGop(String gop) {
		this.gop = gop;
	}

	public String getBitRate() {
		return bitRate;
	}

	public void setBitRate(String bitRate) {
		this.bitRate = bitRate;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
	}
	
}
