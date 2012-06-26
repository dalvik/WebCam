package com.iped.ipcam.utils;

import java.lang.reflect.Field;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.iped.ipcam.gui.R;
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
		LayoutInflater factory = LayoutInflater.from(context);
        final View MyDialogView = factory.inflate(R.layout.layout_login_dialog, null);
        dlg = new AlertDialog.Builder(context).setTitle(context.getResources().getString(R.string.login_dialog_str))
        .setView(MyDialogView)
        .setPositiveButton(context.getResources().getString(R.string.login_str),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	  final EditText userName = (EditText) MyDialogView.findViewById(R.id.firstPassword);
            	  final EditText password = (EditText) MyDialogView.findViewById(R.id.secondPassword);
            	  String name = userName.getText().toString().trim();
                  String pwd = password.getText().toString().trim();
                  if(name== null || pwd == null || name.length()<=0 || pwd.length()<=0) {
                	  if(name == null || name.length()<=0) {
                         AnimUtil.animEff(context, userName, R.anim.shake_anim);
                	  } else {
                       	  AnimUtil.animEff(context, password, R.anim.shake_anim);
                	  }
                	  ToastUtils.showToast(context, R.string.password_is_null);
                	try {
                		keepDialog(dialog, dlg);
                	} catch  (Exception e) {
                		Log.v(TAG, e.getMessage());
                	}
                  } else if(!name.equalsIgnoreCase(pwd)) {
                	  ToastUtils.showToast(context, R.string.password_not_equal);
                      try {
                    		keepDialog(dialog, dlg);
                    	} catch  (Exception e) {
                    		Log.v(TAG, e.getMessage());
                    	}
                 } else {
                	int result = PackageUtil.setPwd(device, CamCmdListHelper.SetCmd_Pwd_State + ":PSWD=" + name + "\0");
                	if(result == 0) {
                		ToastUtils.showToast(context, R.string.device_manager_pwd_set_error);
                        try {
                      		keepDialog(dialog, dlg);
                      	} catch  (Exception e) {
                      		Log.v(TAG, e.getMessage());
                      	}
                	} else if(result == 1) {
                		 try {
                			Message message = handler.obtainMessage();
     	                	message.obj  = name;
     	                	message.what = msgType;
     	                	handler.sendMessage(message);
                     	    dismissDialog(dialog, dlg);
                     	    ToastUtils.showToast(context, R.string.password_set_ok);
                     	} catch  (Exception e) {
                     		Log.v(TAG, e.getMessage());
                     	}
                	} else {
                		ToastUtils.showToast(context, R.string.device_manager_pwd_set_time_out);
                        try {
                      		keepDialog(dialog, dlg);
                      	} catch  (Exception e) {
                      		Log.v(TAG, e.getMessage());
                      	}
                	}
                 }
            }
        }).setNegativeButton(context.getResources().getString(R.string.cancle_login_str), 
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	 try {
            		 handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
            		 dismissDialog(dialog, dlg);
             	} catch  (Exception e) {
             		Log.v(TAG, e.getMessage());
             	}
            	
            }
        })
        .create();
        dlg.show();
		
	}
	
	private static void keepDialog(DialogInterface dialog, Dialog dlg) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field  =  dlg.getClass() .getSuperclass().getDeclaredField("mShowing");
  	    field.setAccessible( true );
  	    field.set(dialog, false);
  	    dialog.dismiss();
	}
	
	public static void dismissDialog(DialogInterface dialog, Dialog dlg) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field  =  dlg.getClass() .getSuperclass().getDeclaredField("mShowing");
 	    field.setAccessible( true );
 	    field.set(dialog, true);
 	    dialog.dismiss();
	}
	
	public static void inputOnePasswordDialog(final Context context,final Handler handler, final int msgType) {
		LayoutInflater factory = LayoutInflater.from(context);
        final View MyDialogView = factory.inflate(R.layout.layout_login2_dialog, null);
        dlg = new AlertDialog.Builder(context).setTitle(context.getResources().getString(R.string.login_dialog_str))
        .setView(MyDialogView)
        .setPositiveButton(context.getResources().getString(R.string.login_str),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	  final EditText userName = (EditText) MyDialogView.findViewById(R.id.firstPassword);
            	  String name = userName.getText().toString().trim();
                  if(name== null || name.length()<=0 ) {
                	  AnimUtil.animEff(context, userName, R.anim.shake_anim);
                	  ToastUtils.showToast(context, R.string.password_is_null);
                	try {
                		keepDialog(dialog, dlg);
                	} catch  (Exception e) {
                		Log.v(TAG, e.getMessage());
                	}
                  } else {
	                try {
	                	Message message = handler.obtainMessage();
	                	message.obj  = name;
	                	message.what = msgType;
	                	handler.sendMessage(message);
	                	dismissDialog(dialog, dlg);
                	    //ToastUtils.showToast(context, R.string.password_set_ok);
                	} catch  (Exception e) {
                		Log.v(TAG, e.getMessage());
                	}
                 }
            }
        }).setNegativeButton(context.getResources().getString(R.string.cancle_login_str), 
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	 try {
            		 handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
            		 dismissDialog(dialog, dlg);
             	} catch  (Exception e) {
             		Log.v(TAG, e.getMessage());
             	}
            	
            }
        })
        .create();
        dlg.show();
	}
	
	
	/*public static void gotoPageDialog(Context context, String message, String title, int page) {
		LayoutInflater factory = LayoutInflater.from(context);
        final View MyDialogView = factory.inflate(R.layout.layout_goto_page, null);
        final EditText userName = (EditText) MyDialogView.findViewById(R.id.goto_book_page_num);
        userName.setText(String.valueOf(page));
        final Button minusPageButton = (Button) MyDialogView.findViewById(R.id.minus_page_num);
        final Button addPageButton = (Button) MyDialogView.findViewById(R.id.add_page_num);
        minusPageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				userName.setText(String.valueOf(Integer.parseInt(userName.getText().toString()) - 1));
			}
		});
        addPageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				userName.setText(String.valueOf(Integer.parseInt(userName.getText().toString()) + 1));	
			}
		});
        dlg = new AlertDialog.Builder(context).setTitle(title)
        .setIcon(0)
        .setView(MyDialogView)
        .setPositiveButton(("go"),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	  int gotoPage = Integer.valueOf(userName.getText().toString());
            	  Jni.gotoPage(gotoPage);
	                try {
                	    Field field  =  dlg.getClass() .getSuperclass().getDeclaredField("mShowing");
                	    field.setAccessible( true );
                	    field.set(dialog, true);
                	    dialog.dismiss();
                	} catch  (Exception e) {
                	}
                 }
        }).setNegativeButton("concle", 
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	 try {
             	    Field field  =  dlg.getClass() .getSuperclass().getDeclaredField("mShowing");
             	    field.setAccessible( true );
             	    field.set(dialog, true);
             	    dialog.dismiss();
             	} catch  (Exception e) {
             	}
            }
        })
        .create();
        dlg.show();
	}*/
	
}
