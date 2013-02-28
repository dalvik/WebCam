package com.iped.ipcam.utils;

public class Constants {

	// device list
	public final static String DEVICELIST = "device_list.conf";

	public final static int DEFAULTUSERSELECT = 2010;

	public final static int HIDETEAUTOSEARCH = 2011;

	public final static int UPDATEAUTOSEARCH = 2012;

	public final static int UPDATEDEVICELIST = 2013;

	public final static int VIDEOAUTOSEARCH = 2014;

	public final static int UPDATEVIDEOLIST = 2015;

	public final static int DISSMISVIDEOSEARCHDLG = 2016;

	public final static int MAXVALUE = 255;

	public final static int LOCALVIDEOPORT = 1234;

	public final static int LOCALCMDPORT = 60000;

	public final static int LOCALAUDIOPORT = 5000;

	public final static String DEFAULTSEARCHIP = "192.168.1.";

	public final static String DEFAULTWAY = "192.168.1.1";

	public final static int COMMNICATEBUFFERSIZE = 1024 * 3 / 2;

	public final static int DEVICESEARCHTIMEOUT = 1000;

	public final static int VIDEOSEARCHTIMEOUT = 15000;

	public final static String VIDEOPREVIEW = "VIDEOPREVIEW";

	public final static int SHOWCONNDIALOG = 2017;

	public final static int HIDECONNDIALOG = 2018;

	public final static int CONNECTTING = 2019;

	public final static int CONNECTERROR = 2020;

	public final static int CONNECTERRORINFO = 2025;
	
	public final static int RECONNECT = 2030;

	public final static int DELETEFILES = 2040;

	public final static int DELETEFILESUCCESS = 2050;

	public final static int DELETEFILEERROR = 2060;

	public final static int CLEARFILES = 2070;

	// ------------------------------------------- config

	public final static int SHOWQUERYCONFIGDLG = 2080;

	public final static int HIDEQUERYCONFIGDLG = 2090;

	public final static int QUERYCONFIGERROR = 2100;

	public final static int SETCONFIGDLG = 2101;

	public final static int SENDDATAWHENMODIFYCONFIG = 2102;

	public final static int SENDSETCONFIGSUCCESSMSG = 2103;

	public final static int SENDSETCONFIGERRORMSG = 2104;

	public final static int RETSETCONFIGDLG = 12102;

	public final static int RETSETCONFIGSUCCESS = 12103;

	public final static int RETSETCONFIGERROR = 12104;

	public final static int SENDSEARCHWIRELESSMSG = 13104;

	public final static int SENDSEARCHWIRELESSSUCCESSMSG = 14104;

	public final static int SENDSEARCHWIRELESSERRORMSG = 15104;

	public final static int SENDCONFIGMSG = 15204;

	// show two input field
	public final static int SEND_SHOW_ONE_PWD_FIELD_CONFIG_MSG = 16000;

	public final static int SEND_SHOW_TWO_PWD_FIELD_CONFIG_MSG = 16001;

	// show one input field
	public final static int SEND_SHOW_ONE_PWD_FIELD_PREVIEW_MSG = 16010;

	public final static int SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG = 16011;

	// through net and get config msg
	public final static int SEND_GET_CONFIG_MSG = 17000;

	public final static int SEND_REQUERY_CONFIG_PWD_ERROR = 17001;

	// add new device by ip
	public final static int SEND_ADD_NEW_DEVICE_BY_IP_MSG = 18000;

	public final static int SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG = 18001;

	public final static int SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG = 18002;

	// update new pasword
	public final static int SEND_UPDATE_NEW_PASSWORD_MSG = 19000;

	// update bcv info
	public final static int SEND_UPDATE_BCV_INFO_MSG = 19500;

	// ---------------------------------------------

	public final static int SHOWPREVIEWDIRDLG = 2110;

	public final static int SHOWBINPREVIEWDIRDLG = 2120;

	//
	public final static int SHOWTOASTMSG = 4100;

	public final static int SHOWRESULTDIALOG = 4200;

	// 发送UDP打洞成功的消息
	public final static int SENDGETTHREEPORTMSG = 4500;

	public final static int SENDGETTHREEPORTTIMOUTMSG = 4600;

	public final static int SENDGETUNFULLPACKAGEMSG = 4700;

	public final static int SEND_UPDATE_DEVICE_LIST_MSG = 5000;

	public final static int WEB_CAM_CONNECT_INIT_MSG = 5050;
	
	//用户密码校验
	public final static int WEB_CAM_CHECK_PWD_STATE_MSG = 5100;
	
	public final static int WEB_CAM_CHECK_PWD_MSG = 5150;
	
	public final static int WEB_CAM_THROUGH_NET_MSG = 5200;
	
	public final static int WEB_CAM_SHOW_CHECK_PWD_DLG_MSG = 5300;
	
	public final static int WEB_CAM_HIDE_CHECK_PWD_DLG_MSG = 5400;
	
	public final static int WEB_CAM_RECONNECT_MSG = 5500;
	
	//update new version
	public final static int WEB_CAM_CHECK_VERSION = 5700;
	
	// update play back
	public final static int UPDATE_PLAY_BACK_TIME = 5800;
	
	//send show popup tips dialog mes
	public final static int SHOW_POP_UP_TIPS_DIA_MSG = 6800;
	
	//
	public final static int BINDLOCALPORT1 = 60001;

	public final static int BINDLOCALPORT2 = 60002;

	public final static int BINDLOCALPORT3 = 60003;


}
