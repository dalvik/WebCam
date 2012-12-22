package com.iped.ipcam.factory;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public abstract class ICustomDialog extends Dialog {

	public ICustomDialog(Context context) {
		super(context);
	}

	protected ICustomDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	public ICustomDialog(Context context, int theme) {
		super(context, theme);
	}
	
	public void show() {
		super.show();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);	
		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
		layoutParams.width = metrics.widthPixels * 20 / 36;
		//layoutParams.height = metrics.heightPixels * 20 / 36;
		getWindow().setAttributes(layoutParams);
	}
}
