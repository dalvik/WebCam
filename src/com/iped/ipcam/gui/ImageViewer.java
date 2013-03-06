package com.iped.ipcam.gui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.pojo.ImageInfo;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.ImageZoom;
import com.iped.ipcam.view.ImageLoaderTask;
import com.iped.ipcam.view.LazyScrollView;
import com.iped.ipcam.view.LazyScrollView.OnScrollListener;

public class ImageViewer extends Activity implements OnClickListener  {

	//private GridView gridView = null;

	private LazyScrollView waterFallScrollView;
	
	private LinearLayout waterFallContainer;
	
	private List<LinearLayout> waterFallItems;
	
	private ImageAdapter adapter = null;

	private List<ImageInfo> imageList = new ArrayList<ImageInfo>();

	private Context context;
	
	private ProgressDialog progressDialog = null;

	private int column_count = 3;// 显示列数
	
	private int page_count = 15;// 每次加载15张图片

	private int current_page = 0;
	
	public static int itemWidth;
	
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case update:
				Log.d(TAG, "update");
				adapter.notifyDataSetChanged();
				break;

			default:
				break;
			}
		}
	};;
	
	private final int update = 1;
	
	private boolean debug = true;
	
	private String TAG = "ImageViewer";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.all_image_layout);
		Display display = this.getWindowManager().getDefaultDisplay();
		itemWidth = display.getWidth() / column_count;// 根据屏幕大小计算每列大小
		//gridView = (GridView) findViewById(R.id.all_image_gridview);
		//progressDialog = ProgressDialog.show(context, context.getText(R.string.webcam_load_pic_title_str), context.getText(R.string.webcam_load_pic_message_str));
		initLayout();
		//adapter = new ImageAdapter(context, imageList);
		//gridView.setAdapter(adapter); 
		//gridView.setOnItemClickListener(new OpenImage()); 
		//gridView.setSelection(0);
		//new LoadImageTask().execute();
	}

	private void initLayout() {
		waterFallScrollView = (LazyScrollView) findViewById(R.id.waterFallScrollView);
		waterFallContainer = (LinearLayout) findViewById(R.id.waterFallContainer);
		waterFallItems = new ArrayList<LinearLayout>();
		waterFallScrollView.getView();
		waterFallScrollView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onTop() {
				Log.d(TAG, "### onTop");
			}
			
			@Override
			public void onScroll() {
				Log.d(TAG, "### onScroll");
			}
			
			@Override
			public void onBottom() {
				Log.d(TAG, "### onBottom");
				addItemToContainer(++current_page, page_count);
			}
		});
		
		for(int i=0;i<column_count;i++) {
			LinearLayout itemLayout = new LinearLayout(this);
			LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(itemWidth, LayoutParams.WRAP_CONTENT);
			itemLayout.setPadding(2, 2, 2, 2);
			itemLayout.setOrientation(LinearLayout.VERTICAL);
			itemLayout.setLayoutParams(itemParam);
			waterFallItems.add(itemLayout);
			waterFallContainer.addView(itemLayout);
		}
		// 第一次加载
		loadImageFiles();
		addItemToContainer(current_page, page_count);
	}
	
	@Override
	public void onClick(View v) {
		Integer index = (Integer) v.getTag();
		startActivity(imageList.get(index).intent);
		
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
	
	private void addItemToContainer(int pageindex, int pagecount) {
		int j = 0;
		int imageCount = imageList.size();
		for (int i = pageindex * pagecount; i < pagecount * (pageindex + 1)
				&& i < imageCount; i++) {
			j = j >= column_count ? j = 0 : j;
			addImage(imageList.get(i), i, j++);
		}
	}
	
	private void addImage(ImageInfo imageInfo, int index, int columnIndex) {
		ImageView imageViewItem = (ImageView) LayoutInflater.from(this).inflate(
				R.layout.waterfallitem, null);
		waterFallItems.get(columnIndex).addView(imageViewItem);
		imageViewItem.setTag(index);
		imageViewItem.setOnClickListener(this);
		imageInfo.imageView = imageViewItem;
		ImageLoaderTask imageLoaderTask = new ImageLoaderTask(imageViewItem);
		imageLoaderTask.execute(imageInfo);

	}
	
	private void loadImageFiles() {
		if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// 获取跟目录
			File file = new File(sdDir + FileUtil.parentPath + FileUtil.picForder);
			if(!file.exists()) {
				file.mkdirs();
				File noMedia = new File(sdDir + FileUtil.parentPath + FileUtil.picForder + ".nomedia");
				try {
					noMedia.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			File[] files = file.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if(pathname.isDirectory()) {
						return true;
					}
					String name = pathname.getName().toLowerCase();
					return name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif");
				}
			});
           // String[] paths = new String[files.length];
            //Bitmap bitmap;
            //Bitmap newBitmap;
            for(int i = 0; i < files.length; i++){
            	//paths[i] = files[i].getPath();
            	ImageInfo imageInfo = new ImageInfo();
            	imageInfo.path = files[i].getPath();
            	Uri uri = Uri.parse("file://"+files[i].getPath());
            	imageInfo.setActivity(uri, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            	imageList.add(imageInfo);
            	/*try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;
                    bitmap = BitmapFactory.decodeFile(paths[i],options);
                    newBitmap = ImageZoom.extractThumbnail(bitmap, 90, 100);
                    if(!newBitmap.isRecycled()) {
                    	bitmap.recycle();
                    	bitmap = null;
                    }
                    if(newBitmap != null) {
                    	ImageInfo imageInfo = new ImageInfo();
                    	imageInfo.title = files[i].getName();
                    	//BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);      
                    	//Drawable drawable = (Drawable)bitmapDrawable; 
                    	//imageInfo.icon = newBitmap;//drawable;
                    	Uri uri = Uri.parse("file://"+files[i].getPath());
                    	imageInfo.setActivity(uri, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    	imageList.add(imageInfo);
                    	handler.sendEmptyMessage(update);
                    	if(debug){
                    		Log.d(TAG, imageInfo.toString());
                    	}
                    }
            	}catch(Exception e){
            		if(debug){
            			Log.d(TAG, "### Exception==>" + e.getLocalizedMessage());
            		}
            	}
*/
            }
		}
	}
	
	private class ImageAdapter extends ArrayAdapter<ImageInfo> {

		public ImageAdapter(Context context, ArrayList<ImageInfo> apps) {
			super(context, 0, apps);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageInfo info = imageList.get(position);
			ViewHolder holder = null;
			holder = new ViewHolder();
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.image_item, parent,
						false);
				holder.icon = (ImageView) convertView
						.findViewById(R.id.app_icon);
				holder.name = (TextView) convertView
						.findViewById(R.id.app_text);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			//holder.icon.setImageBitmap(BitmapUtil.resizeApplicationIcon(info.icon, 150, 150));
			//holder.icon.setImageBitmap(info.icon);
			holder.name.setText(info.title);
			convertView.setTag(holder);
			return convertView;
		}
	}

	private class OpenImage implements
			AdapterView.OnItemClickListener {

		public void onItemClick(AdapterView parent, View v, int position,
				long id) {
			ImageInfo app = (ImageInfo) parent.getItemAtPosition(position);
			try {
				context.startActivity(app.intent);
			} catch (Exception e) {
			} finally {
			}
		}
	}

	class LoadImageTask extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// 获取跟目录
				File file = new File(sdDir + FileUtil.parentPath + FileUtil.picForder);
				if(!file.exists()) {
					file.mkdirs();
					File noMedia = new File(sdDir + FileUtil.parentPath + FileUtil.picForder + ".nomedia");
					try {
						noMedia.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				File[] files = file.listFiles(new FileFilter() {
					
					@Override
					public boolean accept(File pathname) {
						if(pathname.isDirectory()) {
							return true;
						}
						String name = pathname.getName().toLowerCase();
						return name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif");
					}
				});
                String[] paths = new String[files.length];
                Bitmap bitmap;
                Bitmap newBitmap;
                for(int i = 0; i < files.length; i++){
                	paths[i] = files[i].getPath();
                	try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 10;
                        bitmap = BitmapFactory.decodeFile(paths[i],options);
                        newBitmap = ImageZoom.extractThumbnail(bitmap, 90, 100);
                        if(!newBitmap.isRecycled()) {
                        	bitmap.recycle();
                        	bitmap = null;
                        }
                        if(newBitmap != null) {
                        	ImageInfo imageInfo = new ImageInfo();
                        	imageInfo.title = files[i].getName();
                        	//BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);      
                        	//Drawable drawable = (Drawable)bitmapDrawable; 
                        	//imageInfo.icon = newBitmap;//drawable;
                        	Uri uri = Uri.parse("file://"+files[i].getPath());
                        	imageInfo.setActivity(uri, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        	imageList.add(imageInfo);
                        	handler.sendEmptyMessage(update);
                        	if(debug){
                        		Log.d(TAG, imageInfo.toString());
                        	}
                        }
                	}catch(Exception e){
                		if(debug){
                			Log.d(TAG, "### Exception==>" + e.getLocalizedMessage());
                		}
                	}

                }
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
		}
	}

	static class ViewHolder {

		public ImageView icon;

		public TextView name;
	}
}
