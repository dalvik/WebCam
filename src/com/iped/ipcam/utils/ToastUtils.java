package com.iped.ipcam.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

	public static void showToast(Context context, int id) {
		Toast.makeText(context, context.getText(id), Toast.LENGTH_SHORT).show();
	}
}
