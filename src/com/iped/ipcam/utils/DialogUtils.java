package com.iped.ipcam.utils;

import java.net.DatagramSocket;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.iped.ipcam.factory.ICustomDialog;
import com.iped.ipcam.gui.CustomAlertDialog;
import com.iped.ipcam.gui.R;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;

public class DialogUtils {


	private static String TAG = "DialogUtils";
	

	private static AlertDialog dlg = null;

	public static void createTipsDialog(Context context, String message, String title) {
		new AlertDialog.Builder(context)
		.setIcon(0)
		.setTitle(title)
		.setMessage(message)
		.setPositiveButton("ok", null)
		.create().show();
	}
	
	public static void inputTwoPasswordDialog(final Context context,final Device device, final Handler handler, final int msgType) {
        final ICustomDialog customDialog = new CustomAlertDialog(context, R.style.thems_customer_alert_dailog);
        customDialog.setContentView(R.layout.layout_login_dialog);
        customDialog.setTitle(context.getResources().getString(R.string.login_dialog_str));
        customDialog.show();
        customDialog.findViewById(R.id.web_cam_sure_fisrt_login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				 final EditText userName = (EditText) customDialog.findViewById(R.id.firstPassword);
           	  	 final EditText password = (EditText) customDialog.findViewById(R.id.secondPassword);
           	  	 String name = userName.getText().toString().trim();
                 String pwd = password.getText().toString().trim();
                 if(name== null || pwd == null || name.length()<=0 || pwd.length()<=0) {
	               	  if(name == null || name.length()<=0) {
	                      AnimUtil.animEff(context, userName, R.anim.shake_anim);
	               	  } else {
	                      AnimUtil.animEff(context, password, R.anim.shake_anim);
	               	  }
               	  	  ToastUtils.showToast(context, R.string.password_is_null);
                 } else if(!name.equalsIgnoreCase(pwd)) {
               	  	ToastUtils.showToast(context, R.string.password_not_equal);
                } else {
	               	int result = PackageUtil.setPwd(device, CamCmdListHelper.SetCmd_Pwd_State + ":PSWD=" + name + "\0");
	               	if(result == 0) {
	               		ToastUtils.showToast(context, R.string.device_manager_pwd_set_error);
	               	} else if(result == 1) {
	               		Message message = handler.obtainMessage();
	               		message.obj  = name;
	               		message.what = msgType;
	               		handler.sendMessage(message);
	               		ToastUtils.showToast(context, R.string.password_set_ok);
	               		customDialog.dismiss();
	               	} else {
	               		ToastUtils.showToast(context, R.string.device_manager_pwd_set_time_out);
	               	}
               }
			}
        });
         
        customDialog.findViewById(R.id.web_cam_cancl_first_login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
				UdtTools.freeCmdSocket();
				customDialog.dismiss();
			}
        });
	}
	
	public static void inputThreadPasswordDialog(final Context context,final Device device, final Handler handler, final int msgType, final DatagramSocket tmpDatagramSocket, final String ip, final int port1) {
		final ICustomDialog customDialog = new CustomAlertDialog(context, R.style.thems_customer_alert_dailog);
        customDialog.setContentView(R.layout.layout_modify_pwd_dialog);
        customDialog.setTitle(context.getResources().getString(R.string.password_modify_title_str));
        customDialog.show();
        customDialog.findViewById(R.id.web_cam_sure_modify).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final EditText oldPassword = (EditText) customDialog.findViewById(R.id.firstPassword);
            	  final EditText newPassword = (EditText) customDialog.findViewById(R.id.secondPassword);
            	  final EditText repeadNewPassword = (EditText) customDialog.findViewById(R.id.thirdPassword);
            	  String oldPwd = oldPassword.getText().toString().trim();
                  String newPwd1 = newPassword.getText().toString().trim();
                  String newPwd2 = repeadNewPassword.getText().toString().trim();
                  if(oldPwd== null || oldPwd.length()<=0){
                	  AnimUtil.animEff(context, oldPassword, R.anim.shake_anim);
                	  ToastUtils.showToast(context, R.string.password_is_null);
	                	return;
                  } 
                  if(!oldPwd.equalsIgnoreCase(device.getUnDefine2())) {
                	  AnimUtil.animEff(context, oldPassword, R.anim.shake_anim);
                	  ToastUtils.showToast(context, R.string.old_input_password_error);
                	  return;
            	  }
                  if( newPwd1 == null || newPwd1.length()<=0 ){
                	  AnimUtil.animEff(context, newPassword, R.anim.shake_anim);
                	  ToastUtils.showToast(context, R.string.password_is_null);
                	  return;
                  }
                  if(newPwd2 == null ||  newPwd2.length()<=0) {
                	  ToastUtils.showToast(context, R.string.password_is_null);
                	  ToastUtils.showToast(context, R.string.password_is_null);
                	  return;
                  } 
                  if(!newPwd1.equalsIgnoreCase(newPwd2)) {
                	  ToastUtils.showToast(context, R.string.password_not_equal);
                 } else {
                	 String common = CamCmdListHelper.SetCmd_Pwd_State + "PSWD=" + oldPwd + ":PSWD=" + newPwd1 + "\0";
                	 String id = device.getDeviceID();
                	 int res =  UdtTools.sendCmdMsgById(id, common, common.length());
                	 if(res > 0 ) {
                		 byte[] b = new byte[100];
                		 int r = UdtTools.recvCmdMsgById(id, b, 100);
                		 if(r > 0) {
                			 String rece = new String(b,0,r);
                			 if("PSWD_OK".equalsIgnoreCase(rece)) {
 								ToastUtils.showToast(context, R.string.password_modify_success_str);
 	                			Message message = handler.obtainMessage();
 	     	                	message.obj  = newPwd1;
 	     	                	message.what = msgType;
 	     	                	handler.sendMessage(message);
 	     	                	UdtTools.freeCmdSocket();
 	     	                	customDialog.dismiss();
 							}else {
 								 ToastUtils.showToast(context, R.string.password_modify_error_str);
 							}
                		 }else{
                			 ToastUtils.showToast(context, R.string.password_modify_error_str);
                		 }
                	 } else {
                		 ToastUtils.showToast(context, R.string.password_modify_error_str);
                	 }
                 }
			}
        });
        customDialog.findViewById(R.id.web_cam_cancl_modify).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UdtTools.freeCmdSocket();
				customDialog.dismiss();
			}
        });
	}
	
	public static void inputOnePasswordDialog(final Context context,final Handler handler, final int msgType) {
        final ICustomDialog customDialog = new CustomAlertDialog(context, R.style.thems_customer_alert_dailog);
        customDialog.setContentView(R.layout.layout_login2_dialog);
        customDialog.setTitle(context.getResources().getString(R.string.login_dialog_str));
        customDialog.show();
        customDialog.findViewById(R.id.web_cam_sure_second_login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				 final EditText userName = (EditText) customDialog.findViewById(R.id.firstPassword);
				 String name = userName.getText().toString().trim();
                 if(name== null || name.length()<=0 ) {
               	  	AnimUtil.animEff(context, userName, R.anim.shake_anim);
               	  	ToastUtils.showToast(context, R.string.password_is_null);
                 } else {
                	 Message message = handler.obtainMessage();
                	 message.obj  = name;
                	 message.what = msgType;
                	 handler.sendMessage(message);
                	 customDialog.dismiss();
                }
			}
		});
        customDialog.findViewById(R.id.web_cam_cancl_second_login).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				customDialog.dismiss();
			}
		});
	}
	
}
