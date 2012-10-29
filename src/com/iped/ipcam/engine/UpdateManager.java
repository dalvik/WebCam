package com.iped.ipcam.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.iped.ipcam.gui.BCVControlProgressBar;
import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.UpdateInfo;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.NetworkUtil;
import com.iped.ipcam.utils.URLs;

public class UpdateManager {
	
	private static final int DOWN_NOSDCARD = 0;
	
	private static final int DOWN_UPDATE = 1;
	
	private static final int DOWN_OVER = 2;

	private static UpdateManager updateManager;

	private Context context;
	
	private Dialog noticeDialog;
	
	private Dialog downloadDialog;
	
	private BCVControlProgressBar downLoadProgressBar;
	
	private ProgressDialog queryDialog;
	
	private int downLoadProgressValue;
	
	private Thread downLoadThread;
	
	private boolean interceptFlag = false;
	
	private String updateMsg = "";
	
	private String apkUrl;
	
	private String savePath;
	
	private String apkFilePath;
	
	private int curVersionCode;
	
	private UpdateInfo updateInfo;
	
	public static final String UTF_8 = "UTF-8";
	
	private final static int TIMEOUT_CONNECTION = 20000;
	
	private final static int TIMEOUT_SOCKET = 20000;
	
	private final static int RETRY_TIME = 3;
	
	private String TAG = "UpdateManager";
	
	private Handler handler = new Handler(){
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case DOWN_UPDATE:
				downLoadProgressBar.setProgress(downLoadProgressValue);
				break;
			case DOWN_OVER:
				downloadDialog.dismiss();
				Log.d(TAG, "###### installApk");
				installApk();
				break;
			case DOWN_NOSDCARD:
				downloadDialog.dismiss();
				Toast.makeText(context, "无法下载安装文件，请检查SD卡是否挂载", 3000).show();
				break;
			}
    	};
    };
	private UpdateManager() {
		
	}
	
	public static UpdateManager getUpdateManager() {
		if(updateManager == null) {
			updateManager = new UpdateManager();
		}
		return updateManager;
	}
	
	public void checkAppUpdate(final Context context, final boolean isShowMsg) {
		this.context = context;
		getCurrentVersion();
		if(isShowMsg) {
			queryDialog = ProgressDialog.show(context, null, context.getText(R.string.webcam_check_version_str), true, true);
		}
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if(isShowMsg && queryDialog != null) {
					queryDialog.dismiss();
				}
				if(msg.what == Constants.WEB_CAM_CHECK_VERSION) {
					updateInfo = (UpdateInfo) msg.obj;
					if(updateInfo != null) {
						Log.d(TAG, curVersionCode + "  " + updateInfo.getVersionCode());
						if(curVersionCode < updateInfo.getVersionCode()) {
							apkUrl = updateInfo.getDownloadUrl();
							updateMsg = updateInfo.getUpdateLog();
							showNoticeDialog();
						} else if(isShowMsg) {
							new AlertDialog.Builder(context)
							.setTitle(context.getText(R.string.webcam_check_version_result_title_str))
							.setMessage(context.getText(R.string.webcam_check_version_result_success_message_str))
							.setPositiveButton(context.getText(R.string.webcam_check_version_result_ok_str), null)
							.create()
							.show();
						}
					}
				} else if(isShowMsg) {
					new AlertDialog.Builder(context)
					.setTitle(context.getText(R.string.webcam_check_version_result_title_str))
					.setMessage(context.getText(R.string.webcam_check_version_result_error_message_str))
					.setPositiveButton(context.getText(R.string.webcam_check_version_result_ok_str), null)
					.create()
					.show();
				}
			}
		};
		if(NetworkUtil.checkNetwokEnable(context)) {
			new Thread() {
				public void run() {
					Message msg = new Message();
					try {
						UpdateInfo updateInfo = checkVersion();
						msg.what = Constants.WEB_CAM_CHECK_VERSION;
						msg.obj = updateInfo;
						//Log.d(TAG, "### " + updateInfo.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					handler.sendMessage(msg);
				}
			}.start();
		}
	}
	
	public UpdateInfo checkVersion() throws IOException {
		try {
			return UpdateInfo.parse(http_get(URLs.UPDATE_VERSION));
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * 显示版本更新通知对话框
	 */
	private void showNoticeDialog(){
		AlertDialog.Builder builder = new Builder(context);
		builder.setTitle(context.getText(R.string.webcam_check_version_dialog_title_str));
		builder.setMessage(updateMsg);
		builder.setPositiveButton(context.getText(R.string.webcam_check_version_dialog_ok_button_str), new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				showDownloadDialog();			
			}
		});
		builder.setNegativeButton(context.getText(R.string.webcam_check_version_dialog_cancle_button_str), new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
			}
		});
		noticeDialog = builder.create();
		noticeDialog.show();
	}
	
	private InputStream http_get(String url) throws IOException {
		HttpClient httpClient = null;
		GetMethod httpGet = null;
		String responseBody = "";
		int time = 0;
		do {
			try {
				httpClient = getHttpClient();
				httpGet = getHttpGet(url, "", "");
				int statusCode = httpClient.executeMethod(httpGet);
				if(statusCode != HttpStatus.SC_OK) {
					throw new IOException();
				}
				responseBody = httpGet.getResponseBodyAsString();
				break;
			} catch (Exception e) {
				time++;
				if(time <RETRY_TIME) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			} finally {
				if(httpGet != null) {
					httpGet.releaseConnection();
					httpGet = null;
				}
			}
		}while(time<RETRY_TIME);
		//Log.d(TAG, "### " + responseBody);
		//responseBody = responseBody.replace('', '?');
		//if(responseBody.contains("result") && responseBody.contains("errorCode")) {
			
		//}
		return new ByteArrayInputStream(responseBody.getBytes());
	}
	
	private HttpClient getHttpClient() {
		HttpClient httpClient = new HttpClient();
		// 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		// 设置 默认的超时重试处理策略
		httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		// 设置 连接超时时间
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(TIMEOUT_CONNECTION);
		// 设置 读数据超时时间
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpClient.getParams().setContentCharset(UTF_8);
		return httpClient;
	}
	
	private GetMethod getHttpGet(String url, String cookie, String userAgent) {
		GetMethod httpGetMethod = new GetMethod(url);
		httpGetMethod.getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpGetMethod.setRequestHeader("Host",URLs.HOST);
		httpGetMethod.setRequestHeader("Connection","Keep-Alive");
		httpGetMethod.setRequestHeader("Cookie",cookie);
		httpGetMethod.setRequestHeader("User-Agent",userAgent);
		return httpGetMethod;
	}
	
	private void getCurrentVersion() {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			curVersionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 显示下载对话框
	 */
	private void showDownloadDialog(){
		AlertDialog.Builder builder = new Builder(context);
		builder.setTitle(context.getText(R.string.webcam_loading_new_str));
		
		final LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.update_progress, null);
		downLoadProgressBar = (BCVControlProgressBar)v.findViewById(R.id.update_progress);
		downLoadProgressBar.init(context.getText(R.string.webcam_loading_str).toString());
		downLoadProgressBar.setProgress(0);
		builder.setView(v);
		builder.setNegativeButton(context.getText(R.string.webcam_loading_new_cancle_str), new OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				interceptFlag = true;
			}
		});
		downloadDialog = builder.create();
		downloadDialog.show();

		downloadApk();
	}
	
	/**
	* 下载apk
	* @param url
	*/	
	private void downloadApk(){
		downLoadThread = new Thread(downApkRunnable);
		downLoadThread.start();
	}
	
	private Runnable downApkRunnable = new Runnable() {	
		@Override
		public void run() {
			try {
				String apkName = "WebCam_"+updateInfo.getVersionName()+".apk";
				//判断是否挂载了SD卡
				String storageState = Environment.getExternalStorageState();		
				if(storageState.equals(Environment.MEDIA_MOUNTED)){
					savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + FileUtil.parentPath + "update" + File.separator;
					File file = new File(savePath);
					Log.d(TAG, "### save path = " + savePath);
					if(!file.exists()){
						file.mkdirs();
					}
					apkFilePath = savePath + apkName;
				}
				
				//没有挂载SD卡，无法下载文件
				if(apkFilePath == null || apkFilePath == ""){
					handler.sendEmptyMessage(DOWN_NOSDCARD);
					return;
				}
				
				File ApkFile = new File(apkFilePath);
				//是否已下载更新文件
				if(ApkFile.exists()){
					downloadDialog.dismiss();
					Log.d(TAG, "### installApk");
					installApk();
					return;
				}
				
				FileOutputStream fos = new FileOutputStream(ApkFile);
				
				URL url = new URL(apkUrl);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.connect();
				int length = conn.getContentLength();
				InputStream is = conn.getInputStream();
				int count = 0;
				byte buf[] = new byte[1024];
				
				do{   		   		
		    		int numread = is.read(buf);
		    		count += numread;
		    		downLoadProgressValue =(int)(((float)count / length) * 100);
		    	    //更新进度
		    	    handler.sendEmptyMessage(DOWN_UPDATE);
		    		if(numread <= 0){	
		    			//下载完成通知安装
		    			handler.sendEmptyMessage(DOWN_OVER);
		    			break;
		    		}
		    		fos.write(buf,0,numread);
		    	}while(!interceptFlag);//点击取消就停止下载
				
				fos.close();
				is.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch(IOException e){
				e.printStackTrace();
			}
			
		}
	};
	
	/**
	    * 安装apk
	    * @param url
	    */
		private void installApk(){
			File apkfile = new File(apkFilePath);
	        if (!apkfile.exists()) {
	            return;
	        }    
	        Intent i = new Intent(Intent.ACTION_VIEW);
	        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive"); 
	        context.startActivity(i);
		}
}
