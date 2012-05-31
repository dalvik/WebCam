package com.iped.ipcam.utils;

public class CamCmdListHelper {

	public final static String QueryCmd_Online = "get_firmware_info:0";
	public final static String QueryCmd_Video = "get_transport_type:1";
	public final static String SetCmd_StartVideo_Tcp = "set_transport_type:tcp\n";
	public final static String SetCmd_StartVideo_Udp = "set_transport_type:rtp\n";
	public final static String SetCmd_ResetVideo = "restart_server:0";
	public final static String GetCmd_Config = "GetConfig:1 ";
	public final static String SetCmd_Config = "SetConfig:1 ";
	public final static String ReSetCmd_Config = "SetConfig:0 ";
	public final static String GetCmd_Time = "GetTime:0";
	public final static String GetCmd_Statue = "GetRecordStatue:0 ";
	public final static String SetCmd_Statue = "SetRecordStatue:";
	public final static String SetCmd_ChangeIP = "SetIpAddress:";
	public final static String GetCmd_NetFiles = "GetNandRecordFile:";
	public final static String SetCmd_PlayNetFiles = "ReplayRecord:";
	
	public final static String DelCmd_DeleteFiles = "DeleteFile:";
	
}
