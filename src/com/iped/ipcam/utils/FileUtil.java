package com.iped.ipcam.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.iped.ipcam.gui.R;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.iped.ipcam.pojo.Device;
/** 文件处理工具类 **/
public class FileUtil {

	private static String TAG = "FileUtil";
	
	/** 获取SD路径 **/
	public static String getDefaultPath() {
		if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
			return sdDir.getPath();
		}
		return "/";
	}

	public static List<String> getFiles(Activity activity, String path, boolean flag) {
		File f = new File(path);
		List<String> fileList = new ArrayList<String>();
		File[] files = f.listFiles();
		if (files == null) {
			//Toast.makeText(activity,String.format(activity.getString(R.string.file_cannotopen), path),Toast.LENGTH_SHORT).show();
			return fileList;
		}

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String name = file.getAbsolutePath();
			if(flag) {
				if(file.isDirectory()) {
					fileList.add(name);
				}
			} else {
				if(file.isDirectory()) {
					fileList.add(name);
				} else {
					String temp = name.toLowerCase();
					if(temp.endsWith("exe") || temp.endsWith("txt")|| temp.endsWith("jpeg")) {
						fileList.add(name);
					}
				}
			}
		}
		// 排序
		Collections.sort(fileList, new FileComparator());
		return fileList;
	}

	/** 合并路径 **/
	public static String combinPath(String path, String fileName) {
		return path + (path.endsWith(File.separator) ? "" : File.separator) + fileName;
	}
	
	public static String formetFileSize(long fileS) {
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (fileS < 1024) {
			fileSizeString = fileS + " B";
		} else if (fileS < 1048576) {
			fileSizeString = df.format((double) fileS / 1024) + " K";
		} else if (fileS < 1073741824) {
			fileSizeString = df.format((double) fileS / 1048576) + " M";
		} else {
			fileSizeString = df.format((double) fileS / 1073741824) + " G";
		}
		return fileSizeString;
	}
	
	/** 复制文件 **/
	public static boolean copyFile(File src, File tar) throws Exception {
		if (src.isFile()) {
			InputStream is = new FileInputStream(src);
			OutputStream op = new FileOutputStream(tar);
			BufferedInputStream bis = new BufferedInputStream(is);
			BufferedOutputStream bos = new BufferedOutputStream(op);
			byte[] bt = new byte[1024 * 8];
			int len = bis.read(bt);
			while (len != -1) {
				bos.write(bt, 0, len);
				len = bis.read(bt);
			}
			bis.close();
			bos.close();
		}
		if (src.isDirectory()) {
			File[] f = src.listFiles();
			tar.mkdir();
			for (int i = 0; i < f.length; i++) {
				copyFile(f[i].getAbsoluteFile(), new File(tar.getAbsoluteFile() + File.separator
						+ f[i].getName()));
			}
		}
		return true;
	}

	/** 获取MIME类型 **/
	public static String getMIMEType(String name) {
		String type = "";
		String end = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();
		if (end.equals("apk")) {
			return "application/vnd.android.package-archive";
		} else if (end.equals("mp4") || end.equals("avi") || end.equals("3gp")
				|| end.equals("rmvb")) {
			type = "video";
		} else if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") || end.equals("xmf")
				|| end.equals("ogg") || end.equals("wav")) {
			type = "audio";
		} else if (end.equals("jpg") || end.equals("gif") || end.equals("png")
				|| end.equals("jpeg") || end.equals("bmp")) {
			type = "image";
		} else if (end.equals("txt") || end.equals("log")) {
			type = "text";
		} else {
			type = "*";
		}
		type += "/*";
		return type;
	}
	
	
	public static void persistentDevice(Context context, List<Device> deviceList) {
		File file = new File(context.getFilesDir().getPath() + File.separator +  Constants.DEVICELIST);
		if(file.exists()) {
			file.delete();
		}
		FileOutputStream fos = null;
		try {
			file.createNewFile();
			StringBuffer sb = new StringBuffer();
			for(Device device:deviceList) {
				String unDefine2 = (device.getUnDefine2() == null || device.getUnDefine2().length()<=0)?"null":device.getUnDefine2();
				boolean netType = device.getDeviceNetType();
				sb.append(device.getDeviceName() + "&" + device.getDeviceID() + "&" + device.getUnDefine1() + "&" + netType + "&");
				if(netType){
					sb.append(device.getUnDefine1() + "&" + device.getDeviceEthGateWay()+ "&" + device.getDeviceRemoteCmdPort() + "&" + device.getDeviceRemoteVideoPort() + "&" + device.getDeviceRemoteAudioPort() + "&" + unDefine2 +"\n");
				} else {
					sb.append(device.getDeviceEthIp() + "&" + device.getDeviceEthGateWay() + "&" + device.getDeviceLocalCmdPort() + "&" + device.getDeviceLocalVideoPort() + "&" + device.getDeviceLocalAudioPort() + "&" + unDefine2 +"\n");
				}
			}
			//System.out.println(sb.toString());
			fos = new FileOutputStream(file);
			fos.write(sb.toString().getBytes());
			fos.flush();
		} catch (IOException e) {
			Log.d(TAG, "FileUtil persistentDevice " + e.getStackTrace());
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				fos = null;
			}
		}
	}
	
	public static List<Device> fetchDeviceFromFile(Context context) {
		List<Device> deviceList = new ArrayList<Device>();
		File file = new File(context.getFilesDir().getPath() + File.separator + Constants.DEVICELIST);
		if(!file.exists()) {
			return deviceList;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String str = null;
			while((str = br.readLine())!= null) {
				String[] info = str.split("&");
				int l = info.length;
				//System.out.println("device info str=" + str + " length="  + l);
				if(l<8) {
					continue;
				}
				Device device = new Device();
				device.setDeviceName(info[0]);
				device.setDeviceID(info[1]);
				device.setUnDefine1(info[2]);
				if("true".equals(info[3])) {
					device.setDeviceNetType(true);
					device.setDeviceEthIp(info[4]);
					device.setDeviceEthGateWay(info[5]);
				}else {
					device.setDeviceNetType(false);
					device.setDeviceEthIp(info[4]);
					device.setDeviceEthGateWay(info[5]);
				}
				device.setDeviceRemoteCmdPort(Integer.parseInt(info[6]));
				device.setDeviceRemoteVideoPort(Integer.parseInt(info[7]));
				device.setDeviceRemoteAudioPort(Integer.parseInt(info[8]));
				device.setUnDefine2(info[9].equals("null")?null:info[9]);
				deviceList.add(device);
				//System.out.println("get device form file "  + device);
			}
		} catch (FileNotFoundException e) {
			Log.d(TAG, "FileUtil fetchDeviceFromFile " + e.getStackTrace());
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return deviceList;
	}

	public static String parentPath = File.separator + "IPED" + File.separator;
	
	public static String picForder = "Image" + File.separator;
	
	private static String lastImageName  = "";
	
	public static boolean takePicture(Bitmap video, String fileName) {
		if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// 获取跟目录
			File file = new File(sdDir + parentPath + picForder);
			if(!file.exists()) {
				file.mkdirs();
				File noMedia = new File(sdDir + FileUtil.parentPath + FileUtil.picForder + ".nomedia");
				try {
					noMedia.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				FileOutputStream fos = new FileOutputStream(sdDir + parentPath + picForder + fileName);
				video.compress(CompressFormat.JPEG, 90, fos);
				fos.flush();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public static void openImage(Context context) {
		if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// 获取跟目录
			File file = new File(sdDir + parentPath + picForder);
			if(!file.exists()) {
				file.mkdirs();
			}
			Intent i = new Intent(Intent.ACTION_VIEW);
			Uri uri = Uri.parse("file://"+file.getPath());
			i.setDataAndType(uri, "image/*"); 
			context.startActivity(i);
		}
	}
}
