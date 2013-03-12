package com.iped.ipcam.gui;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.utils.AnimUtil;

public class WebCam extends Activity implements OnClickListener{

	private Button loginButton = null;
	
	private EditText userName = null;
	
	private EditText password = null;
	
	private CheckBox keepPwd = null;
	
	private SharedPreferences settings = null;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View view = View.inflate(this, R.layout.webview_login, null);
        setContentView(view);

        //����չʾ������
      	AlphaAnimation aa = new AlphaAnimation(0.3f,1.0f);
      	aa.setDuration(1500);
		view.startAnimation(aa);
        settings = getSharedPreferences(WebCam.class.getName(), 0);
        userName = (EditText) findViewById(R.id.webview_username);
        password = (EditText) findViewById(R.id.webview_password);
        keepPwd = (CheckBox) findViewById(R.id.webview_keepuserpwd);
        loginButton = (Button) findViewById(R.id.webview_userLogin);
        Button userExit = (Button) findViewById(R.id.webview_user_exit);
        findViewById(R.id.welcom_top_title).setAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_toptext_rotate));
       // ImageView iv = (ImageView)findViewById(R.id.copy_right);
        //iv.setImageBitmap(createTxtImage(getText(R.string.cory_right).toString(), 20));
        try {
        	PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	TextView tv = (TextView)findViewById(R.id.web_version);
        	tv.setText(packageInfo.versionName);
        } catch (NameNotFoundException e) {
        	e.printStackTrace();
        }
        boolean flag = settings.getBoolean("KEEP_USER_INFO", true);
        keepPwd.setChecked(flag);
        if(flag) {
        	userName.setText(settings.getString("USERNAME", "admin"));
        	password.setText(settings.getString("PASSWORD", "admin"));
        }else {
        	password.setText("");
        }
        loginButton.setOnClickListener(this);
        userExit.setOnClickListener(this);
        //Intent intent = new Intent(WebCam.this, WebTabWidget.class);
        //Intent intent = new Intent(WebCam.this, CamVideoH264.class);
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
			//System.out.println("username=" + username + "  pwd=" + pwd);
			if(!settings.getString("USERNAME", "admin").equals(username)) {
				AnimUtil.animEff(this, password, R.anim.shake_anim);
				Toast.makeText(WebCam.this, getResources().getString(R.string.user_or_password_is_not_correct_str), Toast.LENGTH_SHORT).show();
				return;
			}
			//System.out.println(settings.getString("PASSWORD", "admin") + " "  + pwd);
			if(!settings.getString("PASSWORD", "admin").equals(pwd)) {
				Toast.makeText(WebCam.this, getResources().getString(R.string.user_or_password_is_not_correct_str), Toast.LENGTH_SHORT).show();
				return;
			}
			/*if(keepPwd.isChecked()) {
				//AnimUtil.animEff(this, password, R.anim.shake_anim);
			} else {
				saveUserInfo("", "", keepPwd.isChecked());
			}*/
			saveUserInfo(username, pwd, keepPwd.isChecked());
			boolean shutCutFlag = settings.getBoolean("CREATE_SHUT_CUT", false);
	        if(!shutCutFlag) {
	        	createShutcut(this, "com.iped.ipcam.gui");
	        }
			Intent intent = new Intent(WebCam.this, WebTabWidget.class);
			startActivity(intent);
			overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);	
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
	
	private void createShutcut(Context context, String pkg) {
		// ��ݷ�ʽ��  
	    String title = "unknown";  
	   // MainActivity������  
	   String mainAct = null;  
	   // Ӧ��ͼ���ʶ  
	   int iconIdentifier = 0;  
	   // ���ݰ���Ѱ��MainActivity  
	   PackageManager pkgMag = context.getPackageManager();  
	   Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);  
	   queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);  
	   List<ResolveInfo> list = pkgMag.queryIntentActivities(queryIntent,  
	           PackageManager.GET_ACTIVITIES);  
	   for (int i = 0; i < list.size(); i++) {  
	       ResolveInfo info = list.get(i);  
	       if (info.activityInfo.packageName.equals(pkg)) {  
	           title = info.loadLabel(pkgMag).toString();  
	           mainAct = info.activityInfo.name;  
	           iconIdentifier = info.activityInfo.applicationInfo.icon;  
	           break;  
	       }  
	   }  
	  
	   Intent shortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");  
	   // ��ݷ�ʽ������  
	   shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.app_name));  
	   //�������ظ�����  
	   shortcutIntent.putExtra("duplicate", false);   
	   ComponentName comp = new ComponentName(pkg, "com.iped.ipcam.gui.WebCam");  
	   shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,  
	           new Intent(Intent.ACTION_MAIN).setComponent(comp));  
	   // ��ݷ�ʽ��ͼ��  
	   ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher);  
	   shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);  
	   // ���͹㲥���ý����ߴ�����ݷ�ʽ  
	   // ��Ȩ��<uses-permission  
	   // android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />  
	   context.sendBroadcast(shortcutIntent);  
	   settings.edit().putBoolean("CREATE_SHUT_CUT", true).commit();
	}
	
	public static Bitmap createTxtImage(String txt, int txtSize) {
		Bitmap mbmpTest = Bitmap.createBitmap(txt.length() * txtSize + 4,
				txtSize + 4, Config.ARGB_8888);
		Canvas canvasTemp = new Canvas(mbmpTest);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.BLACK);
		p.setTextSize(txtSize);
		canvasTemp.drawText(txt, 2, txtSize - 2, p);
		return mbmpTest;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		StatService.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
}