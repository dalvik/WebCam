package com.iped.ipcam.engine;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.CamConfig;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;

public class CamParasSetImp implements ICamParasSet {

	@Override
	public CamParasSetImp getCamPara(String ip, Handler handler) {
		new Thread(new CamGetParas(ip, handler)).start();
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
			String rece = PackageUtil.CMDPackage2(CamCmdListHelper.GetCmd_Statue, ip, Constants.UDPPORT);
			System.out.println("*/"  +rece.trim()+"/0");
			if(rece != null) {
				String[] info = rece.split("\n");
				for(String s:info) {
					System.out.println("-" + s.trim());
				}
				if(info.length>4) {
					CamConfig camConfig = new CamConfig();
					camConfig.setVersion(info[0].trim());
					camConfig.setInTotalSpace(info[1].trim());
					camConfig.setOutTotalSpace(info[2].trim());
					camConfig.setAddrType(info[3].trim());
					camConfig.setValidRecordTime(info[4].trim());
					
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
				}
			} else {
				handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
			}
		}
	}
}
