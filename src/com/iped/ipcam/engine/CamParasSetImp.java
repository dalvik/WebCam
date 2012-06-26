package com.iped.ipcam.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Handler;
import android.util.Log;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.ThroughNetUtil;

public class CamParasSetImp implements ICamParasSet {

	private Thread getCamParaThread = null;
	
	private ThroughNetUtil netUtil = null; 
			
	private Map<String, String> paraMap = new LinkedHashMap<String, String>();
	
	@Override
	public CamParasSetImp getCamPara(Device device, Handler handler) {
		if(getCamParaThread != null) {
			try {
				getCamParaThread.join(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
			}
			getCamParaThread = null;
		}
		getCamParaThread = new Thread(new CamGetParas(device, handler));
		getCamParaThread.start();
		return null;
	}
	
	@Override
	public ThroughNetUtil getThroughNetUtil() {
		return netUtil;
	}
	class CamGetParas implements Runnable {
		
		private Device device;
		
		private Handler handler;
		
		private String TAG = "CamGetParas";
		
		public CamGetParas(Device device, Handler handler) {
			this.device = device;
			this.handler = handler;
		}
		
		@Override
		public void run() {
				if(device.getDeviceNetType()) {// out 
					Log.d(TAG, "<=== CamGetParas run method = " + device.getDeviceID());
					netUtil = new ThroughNetUtil(handler,true,Integer.parseInt(device.getDeviceID(),16));
					new Thread(netUtil).start();
				} else { // int 
					String ethIp = device.getDeviceEthIp();
					if(ethIp != null) {
						String rece;
						try {
							rece = PackageUtil.sendPackageByIp(CamCmdListHelper.GetCmd_Config+device.getUnDefine2()+"\0", ethIp, Constants.LOCALCMDPORT);
							if("PSWD_NOT_SET".equals(rece)) {
								Log.d(TAG, "CamParasSetImp PSWD_not set");
							} else if("PSWD_FAIL".equals(rece)) {
								Log.d(TAG, "CamParasSetImp PSWD_FAIL");
							} else {
								ParaUtil.putParaByString(rece, paraMap);
							}
							handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
						} catch (CamManagerException e) {
							getConfigByWlan(device.getDeviceEthIp());
						}
					} else {
						getConfigByWlan(device.getDeviceEthIp());
					}
				}
		}
		
		private void getConfigByWlan(String wlan) {
			String rece;
			try {
				rece = PackageUtil.sendPackageByIp(CamCmdListHelper.GetCmd_Config+device.getUnDefine2()+"\0", wlan, Constants.LOCALCMDPORT);
				Log.d(TAG, "getConfigByWlan wlan = " + wlan + "  recv===="+ rece);
				if("PSWD_NOT_SET".equals(rece)) {
					Log.d(TAG, "CamParasSetImp getConfigByWlan PSWD_not set");
				} else if("PSWD_FAIL".equals(rece)) {
					Log.d(TAG, "CamParasSetImp getConfigByWlan PSWD_FAIL");
				} else {
					ParaUtil.putParaByString(rece, paraMap);
				}
			} catch (CamManagerException e) {
				handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
			} finally {
				handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
			}
		}
	}

	public Map<String, String> getParaMap() {
		return paraMap;
	}

}
