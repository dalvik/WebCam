package com.iped.ipcam.pojo;

import java.io.Serializable;

public class BCVInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private int brightness;
	
	private int contrast;
	
	private int volume;

	public BCVInfo() {
		super();
	}

	public BCVInfo(int brightness, int contrast, int volume) {
		super();
		this.brightness = brightness;
		this.contrast = contrast;
		this.volume = volume;
	}

	public int getBrightness() {
		return brightness;
	}

	public void setBrightness(int brightness) {
		this.brightness = brightness;
	}

	public int getContrast() {
		return contrast;
	}

	public void setContrast(int contrast) {
		this.contrast = contrast;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	@Override
	public String toString() {
		return "BCVInfo [brightness=" + brightness + ", contrast=" + contrast
				+ ", volume=" + volume + "]";
	}
	
	
}
