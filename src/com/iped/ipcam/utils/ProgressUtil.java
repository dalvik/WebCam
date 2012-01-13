package com.iped.ipcam.utils;

import android.app.ProgressDialog;
import android.content.Context;

public class ProgressUtil {

	private static ProgressDialog progressDialog = null;
	
	public static void showProgress(int id, Context context) {
		hideProgress();
		if(progressDialog == null) {
			progressDialog = new ProgressDialog(context);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
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
