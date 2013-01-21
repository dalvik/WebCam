package com.iped.ipcam.bitmapfun;

import java.io.IOException;
import java.util.GregorianCalendar;

import android.app.WallpaperManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.iped.ipcam.bitmapfun.ImageWorker.OnLoadImageListener;
import com.iped.ipcam.gui.R;

public class ScrollyGalleryActivity extends FragmentActivity implements OnLoadImageListener {
	
    private static final String IMAGE_CACHE_DIR = "images";
    
    public static final String EXTRA_IMAGE = "extra_image";
    
    public static final String LIST_SIZE = "list_size";

    public static final String CATA_LOG = "cata_log";
    
	private FlingGallery mGallery;
	
    private ImageFetcher mImageFetcher;
    
	private ImageGalleryAdapter imageAdapter;
	
	private ScrollLayout imageScrollLayout = null;
	
    @Override
    public boolean onTouchEvent(MotionEvent event)
	{
    	//return mGallery.onTouchEvent(event);
    	return imageScrollLayout.onTouchEvent(event);
    }

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_scroll_image);
        imageScrollLayout = (ScrollLayout) findViewById(R.id.main_scrolllayout);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        System.out.println("screen w = " + width + " h = " + height + " densityDpi=  " + displayMetrics.densityDpi + " scaledDensity = " + displayMetrics.scaledDensity + " density = " + displayMetrics.density + " "+ displayMetrics.xdpi + " " + displayMetrics.ydpi);
        final int longest = (height > width ? height : width)/2 ;

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(this, 0.25f); // Set memory cache to 25% of mem class

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);
        mImageFetcher.setOnLoadImageListener(this);
        imageScrollLayout.setImageFetcher(mImageFetcher);
        if(getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
        	imageScrollLayout.setOrientation(1);
        }else {
        	imageScrollLayout.setOrientation(0);
        }
        final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, 0);
        imageScrollLayout.setAdapter(ImageGrid.getImageList(), extraCurrentItem);
        imageScrollLayout.updateMetrics(displayMetrics);
        ImageView preView = (ImageView)imageScrollLayout.findViewById(R.id.preImageView);
        ImageView nextView = (ImageView)imageScrollLayout.findViewById(R.id.nextImageView);
        ImageView[] views = {preView, nextView}; 
        imageScrollLayout.initImageArr(views);
        imageScrollLayout.loadInit();
/*        mGallery = new FlingGallery(this, displayMetrics);
        int pagerMargin = getResources().getDimensionPixelSize(R.dimen.image_detail_pager_margin);
        mGallery.setPaddingWidth(pagerMargin);
        imageAdapter = new ImageGalleryAdapter(ScrollyGalleryActivity.this, mImageFetcher, ImageGrid.getImageList());
        final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
        mGallery.setAdapter(imageAdapter, extraCurrentItem);
        mGallery.setIsGalleryCircular(true);
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

		//layoutParams.setMargins(10, 10, 10, 10);
		layoutParams.weight = 1.0f;
        layout.addView(mGallery, layoutParams);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(layout);*/
    }	
    
    @Override
	public void updateResolution(String path, int w, int h) {
    	imageScrollLayout.updateResolution(path, w, h);
    	/*if(w>h) {//land
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	}else if(w<h) {//port
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	}*/
    	/*final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        final int longest = (height > width ? height : width) / 2;
        mImageFetcher.setImageSize(longest);
        imageScrollLayout.updateMetrics(displayMetrics);*/
	}
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        final int longest = (height > width ? height : width) / 2;
        mImageFetcher.setImageSize(longest);
        imageScrollLayout.updateMetrics(displayMetrics);
        System.out.println("onConfigurationChanged");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_detail, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.set_wallpaper:
                //NavUtils.navigateUpFromSameTask(this);
            	Bitmap b = mImageFetcher.processBitmap(ImageGrid.getFilePath(imageScrollLayout.getCurrentItem()));
            	System.out.println("b= " + b);
            	if(b == null) {
            		return false;
            	}
				try {
					WallpaperManager.getInstance(ScrollyGalleryActivity.this).setBitmap(b);
					Toast.makeText(this, R.string.set_wall_paper_menu_toast,Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Log.d("ImageDetailActivity", "### " + e.getLocalizedMessage());
				}
                return true;
            case R.id.clear_cache:
                mImageFetcher.clearCache();
                Toast.makeText(this, R.string.clear_cache_complete_toast,Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
