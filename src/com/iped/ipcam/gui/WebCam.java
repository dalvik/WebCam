package com.iped.ipcam.gui;

import com.iped.ipcam.utils.AnimUtil;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class WebCam extends Activity implements OnClickListener{

	private Button loginButton = null;
	
	private EditText userName = null;
	
	private EditText password = null;
	
	private CheckBox keepPwd = null;
	
	private SharedPreferences settings = null;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_login);
        settings = getSharedPreferences(WebCam.class.getName(), 0);
        userName = (EditText) findViewById(R.id.webview_username);
        password = (EditText) findViewById(R.id.webview_password);
        keepPwd = (CheckBox) findViewById(R.id.webview_keepuserpwd);
        loginButton = (Button) findViewById(R.id.webview_userLogin);
        Button userExit = (Button) findViewById(R.id.webview_user_exit);
        boolean flag = settings.getBoolean("KEEP_USER_INFO", false);
        keepPwd.setChecked(flag);
        if(flag) {
        	userName.setText(settings.getString("USERNAME", ""));
        	password.setText(settings.getString("PASSWORD", ""));
        }
        loginButton.setOnClickListener(this);
        userExit.setOnClickListener(this);
        Intent intent = new Intent(WebCam.this, WebTabWidget.class);
       // Intent intent = new Intent(WebCam.this, CamVideoH264.class);
		//startActivity(intent);
		//WebCam.this.finish();
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.webview_userLogin:
			String username = userName.getText().toString().trim();
			if(null == username || "".equalsIgnoreCase(username)) {
				AnimUtil.animEff(this, userName, R.anim.shake_anim);
				Toast.makeText(WebCam.this, getResources().getString(R.string.username_null), Toast.LENGTH_SHORT).show();
				return;
			}
			String pwd = password.getText().toString().trim();
			if(null == password || "".equalsIgnoreCase(pwd)) {
				AnimUtil.animEff(this, password, R.anim.shake_anim);
				Toast.makeText(WebCam.this, getResources().getString(R.string.username_null), Toast.LENGTH_SHORT).show();
				return;
			}
			System.out.println("username=" + username);
			System.out.println("pwd=" + pwd);
			if(!settings.getString("USERNAME", "admin").equals(username)) {
				Toast.makeText(WebCam.this, getResources().getString(R.string.user_or_password_is_not_correct_str), Toast.LENGTH_SHORT).show();
				return;
			}
			if(!settings.getString("PASSWORD", "admin").equals(pwd)) {
				Toast.makeText(WebCam.this, getResources().getString(R.string.user_or_password_is_not_correct_str), Toast.LENGTH_SHORT).show();
				return;
			}
			if(keepPwd.isChecked()) {
				//AnimUtil.animEff(this, password, R.anim.shake_anim);
				saveUserInfo(username, pwd, keepPwd.isChecked());
			} else {
				saveUserInfo("", "", keepPwd.isChecked());
			}
			Intent intent = new Intent(WebCam.this, WebTabWidget.class);
			startActivity(intent);
			WebCam.this.finish();
			break;
		case R.id.webview_user_exit:
			WebCam.this.finish();
			break;

		default:
			break;
		}
	}
	
	public void saveUserInfo(String username, String pwd, boolean flag) {
		settings.edit().putString("USERNAME", username).putString("PASSWORD", pwd).putBoolean("KEEP_USER_INFO", flag).commit();
	}
	
}