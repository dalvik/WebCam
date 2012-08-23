package com.iped.ipcam.mail;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Looper;

import com.iped.ipcam.gui.R;

public class ExceptionHandler implements UncaughtExceptionHandler {

	private Context context;
	
	private static ExceptionHandler INSTANCE = new ExceptionHandler();  

	private Thread.UncaughtExceptionHandler defaultHandler;

	private ExceptionHandler() {  
		
    }
	
	private ExceptionHandler(Context context) {
		this.context = context;
	}

	public void init(Context context) {  
		this.context = context; 
		defaultHandler = Thread.getDefaultUncaughtExceptionHandler();  
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);  
    }  
	
	public static ExceptionHandler getInstance() {  
        return INSTANCE;  
    } 
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		sendCrashReport(ex);
	}

	private void sendCrashReport(Throwable ex) {
		final StringBuffer exceptionStr = new StringBuffer();
		exceptionStr.append(ex.getMessage());
		StackTraceElement[] elements = ex.getStackTrace();
		for (int i = 0; i < elements.length; i++) {
			exceptionStr.append(elements[i].toString() +"\n\r");
		}
		
		new Thread() {  
            @Override  
            public void run() {  
            	try {
					new SendEmail(context).sendEmail(exceptionStr.toString());
				} catch (Exception e) {
					e.printStackTrace();
				} 
            }
		}.start();
		// 发送收集到的Crash信息到服务器
		handleException();
	}

	private void handleException() {
		new Thread() {  
            @Override  
            public void run() {  
                Looper.prepare();  
                new AlertDialog.Builder(context).setTitle(context.getResources().getString(R.string.exception_tips_title_str)).setCancelable(false)  
                        .setMessage(context.getResources().getString(R.string.exception_tips_message_str)).setNeutralButton(context.getResources().getString(R.string.exception_commit_button_str), new OnClickListener() {  
                            @Override  
                            public void onClick(DialogInterface dialog, int which) {  
                            	int sdk_Version = android.os.Build.VERSION.SDK_INT;
                        		if (sdk_Version >= 8) {
                        			Intent startMain = new Intent(Intent.ACTION_MAIN);
                        			startMain.addCategory(Intent.CATEGORY_HOME);
                        			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        			context.startActivity(startMain);
                        			System.exit(0);
                        		} else if (sdk_Version < 8) {
                        			ActivityManager activityMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        			activityMgr.restartPackage(context.getPackageName());
                        		}
                            }  
                        })  
                        .create().show();
                Looper.loop();  
            }  
        }.start();  
        //ToastUtils.showToast(context, R.string.accor_an_import_exception_str);
	}

}
