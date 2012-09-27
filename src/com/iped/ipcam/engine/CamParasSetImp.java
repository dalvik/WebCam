package com.iped.ipcam.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ParaUtil;

public class CamParasSetImp implements ICamParasSet {

	private Thread getCamParaThread = null;
	
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
			   String cmdStr = CamCmdListHelper.GetCmd_Config+device.getUnDefine2()+"\0";
			   int res = UdtTools.sendCmdMsgById(device.getDeviceID(), cmdStr, cmdStr.length());
			   Log.d(TAG, "### get web cam config result = " + res);
			   if(res < 0) {
					//return -2;
				   handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
				   return;
				}
				int bufLength = 1500;
				byte[] recvBuf = new byte[bufLength];
				int recvLength = UdtTools.recvCmdMsgById(device.getDeviceID(), recvBuf, bufLength);
				Log.d(TAG, "### check pwd recv length " + recvLength);
				if(recvLength<0) {
					//return -2; // time out
					handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
					return;
				}
				String recvConfigStr = new String(recvBuf,0, recvLength);
				Log.d(TAG, "### recvConfigStr " + recvConfigStr);
				int rs = 0;
				if("PSWD_NOT_SET".equals(recvConfigStr)) {
					rs = -1;
					Log.d(TAG, "CamParasSetImp PSWD_not set");
				} else if("PSWD_FAIL".equals(recvConfigStr)) {
					rs = -2;
					Log.d(TAG, "CamParasSetImp PSWD_FAIL");
				} else {
					ParaUtil.putParaByString(recvConfigStr, paraMap);
				}
				Message msg = handler.obtainMessage();
				msg.what = Constants.HIDEQUERYCONFIGDLG;
				msg.arg1 = rs; 
				handler.sendMessage(msg);
		}
		
	}

	public Map<String, String> getParaMap() {
		return paraMap;
	}

}
