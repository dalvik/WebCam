package com.iped.ipcam.gui;

import java.lang.reflect.Field;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.iped.ipcam.utils.AnimUtil;
import com.iped.ipcam.utils.ToastUtils;

public class LoginDialog extends Dialog {

	private Context context;

	private AlertDialog dlg = null;
	
	private String TAG = "LoginActivity";

	public LoginDialog(Context context) {
		super(context);
		this.context = context;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	public void firstLogin() {
        
	}




	
}
