package com.iped.ipcam.gui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;

public class LogoutDialog extends Activity implements OnClickListener {
	
	private ImageButton btn_close;
	private Button btn_logout_sure;
	private Button btn_logout_cancle;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logout_dialog);
        
        btn_close = (ImageButton)findViewById(R.id.login_close_button);
        btn_close.setOnClickListener(this);        
        
        btn_logout_sure = (Button)findViewById(R.id.sure_btn_exit);
        btn_logout_sure.setOnClickListener(this);
        btn_logout_cancle = (Button)findViewById(R.id.cancle_btn_exit);
        btn_logout_cancle.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
    	switch(v.getId()) {
    	case R.id.sure_btn_exit:
    		ICamManager camManager = CamMagFactory.getCamManagerInstance();
    		camManager.clearCamList();
    		//UdtTools.exit();
    		UdtTools.close();
    		UdtTools.cleanUp();
    		int sdk_Version = android.os.Build.VERSION.SDK_INT;
    		if (sdk_Version >= 8) {
    			Intent startMain = new Intent(Intent.ACTION_MAIN);
    			startMain.addCategory(Intent.CATEGORY_HOME);
    			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(startMain);
    			System.exit(0);
    		} else if (sdk_Version < 8) {
    			ActivityManager activityMgr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    			activityMgr.restartPackage(getPackageName());
    		}
    		break;
    	case R.id.login_close_button:
    	case R.id.cancle_btn_exit:
    		LogoutDialog.this.finish();
    		break;
    		default:
    			break;
    	}
    }
}
