package com.iped.ipcam.engine;

import android.os.Handler;

import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.ThroughNetUtil;

public interface ICamParasSet {

	public CamParasSetImp getCamPara(Device device, Handler handler);
	
}
