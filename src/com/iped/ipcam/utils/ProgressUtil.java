package com.iped.ipcam.utils;

import android.content.Context;

import com.iped.ipcam.gui.CustomProgressDialog;
import com.iped.ipcam.gui.R;

public class ProgressUtil {

	private static CustomProgressDialog progressDialog = null;
	
	public static void showProgress(int id, Context context) {
		dismissProgress();
		if(progressDialog == null) {
			//progressDialog = new ProgressDialog(context);
			progressDialog = CustomProgressDialog.createDialog(context, R.style.CustomProgressDialog);;
			//progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		progressDialog.setMessage(context.getResources().getString(id));
		progressDialog.show();
	}
	
	public static void hideProgress() {
		if(progressDialog != null) {
			progressDialog.hide();
		}
	}
	
	public static void dismissProgress() {
		if(progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}
	
}
