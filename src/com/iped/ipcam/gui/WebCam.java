package com.iped.ipcam.gui;

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
        setContentView(R.layout.login);
        settings = getSharedPreferences(WebCam.class.getName(), 0);
        userName = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        keepPwd = (CheckBox) findViewById(R.id.keepuserpwd);
        loginButton = (Button) findViewById(R.id.userLogin);
        Button userExit = (Button) findViewById(R.id.user_exit);
        boolean flag = settings.getBoolean("KEEP_USER_INFO", false);
        keepPwd.setChecked(flag);
        if(flag) {
        	userName.setText(settings.getString("USERNAME", ""));
        	password.setText(settings.getString("PASSWORD", ""));
        }
        loginButton.setOnClickListener(this);
        userExit.setOnClickListener(this);
        Intent intent = new Intent(WebCam.this, WebTabWidget.class);
        //Intent intent = new Intent(WebCam.this, CamVideoH264.class);
		startActivity(intent);
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.userLogin:
			String username = userName.getText().toString();
			if(null == username || "".equalsIgnoreCase(username)) {
				Toast.makeText(WebCam.this, getResources().getString(R.string.username_null), Toast.LENGTH_SHORT).show();
				return;
			}
			if(keepPwd.isChecked()) {
				saveUserInfo(username, password.getText().toString(), keepPwd.isChecked());
			} else {
				saveUserInfo("", "", keepPwd.isChecked());
			}
			Intent intent = new Intent(WebCam.this, CamVideoH264.class);
			startActivity(intent);
			WebCam.this.finish();
			break;
		case R.id.user_exit:
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