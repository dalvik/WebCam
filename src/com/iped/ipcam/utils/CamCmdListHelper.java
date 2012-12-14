package com.iped.ipcam.utils;

import java.util.HashMap;
import java.util.Map;

public class CamCmdListHelper {

	public final static String QueryCmd_Online = "get_firmware_info:0";
	public final static String QueryCmd_Video = "get_transport_type:1";
	public final static String SetCmd_StartVideo_Tcp = "set_transport_type:tcp:PSWD=";
	public final static String SetCmd_StartVideo_Udp = "set_transport_type:rtp:PSWD=";
	public final static String SetCmd_ResetVideo = "restart_server:0";
	public final static String GetCmd_Config = "GetConfig:1:PSWD=";
	public final static String SetCmd_Config = "SetConfig:1 ";
	public final static String ReSetCmd_Config = "SetConfig:0 ";
	public final static String GetCmd_Time = "GetTime:0";
	public final static String GetCmd_Statue = "GetRecordStatue:0 ";
	public final static String SetCmd_Statue = "SetRecordStatue:";
	public final static String SetCmd_ChangeIP = "SetIpAddress:";
	public final static String GetCmd_NetFiles = "GetNandRecordFile:00000000";
	public final static String SetCmd_PlayNetFiles = "ReplayRecord:";
	public final static String SetCmd_SearchWireless = "search_wifi:0 ";
	public final static String SetCmdPTZ = "Rs485Cmd:";
	public final static String SetCmp_Set_Brightness = "set_brightness:";
	public final static String SetCmp_Set_Contrast = "set_contrast:";
	public final static String SetCmp_Set_Volume = "set_volume:";
	
	public final static String DelCmd_DeleteFiles = "DeleteFile:";
	
	// new cmd line
	public final static String CheckCmd_Pwd_State = "pswd_state:";
	
	public final static String CheckCmd_PWD = "check_pswd:PSWD=";
	
	public final static String SetCmd_Pwd_State = "set_pswd:";
	
	public final static String SetCmd_Set_Time = "SetTime:";
	
	public final static String SetCmd_Play_Back = "ReplayRecord:";
	
	public final static String SetCmd_Seek = "pb_set_status:SEEK:";
	
	public final static String SetAudioTalkOn = "AudioTalkOnn";
	
	public final static String SetAudioTalkOff = "AudioTalkOfff";
	
	public final static String SetAudioTalkVolume = "TalkVolume:";//"TalkVolume:100:11";
	
	public final static String SetVideoResol = "set_video_fmt:";
	
	public final static String[] resolArr = {"qvga", "vga", "720p"};
	
	public final static String[][] audioTalk = {{"1","100"},{"2","110"},{"2","126"}};
	
	public static Map<Integer, byte[]> ptzMap = new HashMap<Integer, byte[]>();
	
	//public static byte[] ptzHeader = new byte[]{0x8,(byte) 0xa0,0x0};
	
	static {
		ptzMap.put(WinTaiCmd.PTZ_CMD_UP.ordinal(), new byte[]{0x8,(byte) 0xa0,0x0,0x00,0x08,0x00,0x30,(byte) 0xaf});
		ptzMap.put(WinTaiCmd.PTZ_CMD_DOWN.ordinal(), new byte[]{0x8,(byte) 0xa0,0x0,0x00,0x10,0x00,0x30,(byte) 0xaf});
		ptzMap.put(WinTaiCmd.PTZ_CMD_LEFT.ordinal(), new byte[]{0x8,(byte) 0xa0,0x0,0x00,0x04,0x30,0x00,(byte) 0xaf});
		ptzMap.put(WinTaiCmd.PTZ_CMD_RIGHT.ordinal(), new byte[]{0x8,(byte) 0xa0,0x0,0x00,0x02,0x30,0x00,(byte) 0xaf});
		ptzMap.put(WinTaiCmd.PTZ_CMD_STOP.ordinal(), new byte[]{0x8,(byte) 0xa0,0x0, 0x00,0x00,0x00,0x00,(byte) 0xaf});
	}
	
}
