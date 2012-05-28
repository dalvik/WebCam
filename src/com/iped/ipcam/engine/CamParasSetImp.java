package com.iped.ipcam.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.CamConfig;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;

public class CamParasSetImp implements ICamParasSet {

	private Thread getCamParaThread = null;
	
	private Map<String, String> paraMap = new HashMap<String, String>();
	
	@Override
	public CamParasSetImp getCamPara(String ip, Handler handler) {
		System.out.println("getCampara....");
		if(getCamParaThread != null) {
			try {
				getCamParaThread.join(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			getCamParaThread = null;
		}
		getCamParaThread = new Thread(new CamGetParas(ip, handler));
		getCamParaThread.start();
		return null;
	}
	
	class CamGetParas implements Runnable {
		
		private String ip;
		
		private Handler handler;
		
		public CamGetParas(String ip, Handler handler) {
			this.ip = ip;
			this.handler = handler;
		}
		
		@Override
		public void run() {
			try {
				PackageUtil.isOnline(ip, Constants.UDPPORT);
			} catch (CamManagerException e) {
				e.printStackTrace();
				handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
				return;
			}
			String rece = PackageUtil.CMDPackage2(CamCmdListHelper.GetCmd_Config, ip, Constants.UDPPORT);
			System.out.println("recv===="+ rece);
			if(rece != null) {
				ParaUtil.putParaByString(rece, paraMap);
				handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
				/*Set<String> s = paraMap.keySet();
				for(String ss:s){
					System.out.println(ss + " " + paraMap.get(ss));
				}*/
				/*if(info.length>4) {
					CamConfig camConfig = new CamConfig();
					camConfig.setVersion(info[0].trim());
					camConfig.setInTotalSpace(info[1].trim());
					camConfig.setOutTotalSpace(info[2].trim());
					camConfig.setAddrType(info[3].trim());
					camConfig.setValidRecordTime(info[4].trim());
					System.out.println(ip + " " + Constants.UDPPORT);
					rece = PackageUtil.CMDPackage2(CamCmdListHelper.GetCmd_Config, ip, Constants.UDPPORT);
					if(rece != null) {
						String[] info2 = rece.split("\n");
						for(String s:info2) {
							System.out.println("-" + s.trim());
						}
						if(info2.length>8) {
							camConfig.setFrameRate(info2[0].substring(info2[0].indexOf("=")+1));
							camConfig.setCompression(info2[1].substring(info2[1].indexOf("=")+1));
							camConfig.setResolution(info2[2].substring(info2[2].indexOf("=")+1));
							camConfig.setGop(info2[3].substring(info2[3].indexOf("=")+1));
							camConfig.setBitRate(info2[7].substring(info2[7].indexOf("=")+1));
							Message msg = handler.obtainMessage();
							Bundle data = new Bundle();
							data.putParcelable("CAMPARAMCONFIG", camConfig);
							msg.setData(data);
							msg.what = Constants.HIDEQUERYCONFIGDLG;
							handler.sendMessage(msg);
						}else {
							handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
						}
					} else {
						handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
					}
				}else {
					handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
				}*/
			} else {
				handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
			}
		}
	}

	public Map<String, String> getParaMap() {
		return paraMap;
	}

}
