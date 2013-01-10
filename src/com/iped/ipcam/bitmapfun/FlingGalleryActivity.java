package com.iped.ipcam.bitmapfun;

import com.iped.ipcam.bitmapfun.ImageWorker.OnLoadImageListener;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class FlingGalleryActivity extends FragmentActivity implements OnLoadImageListener {
	
    private static final String IMAGE_CACHE_DIR = "images";
    
    public static final String EXTRA_IMAGE = "extra_image";
    
    public static final String LIST_SIZE = "list_size";
    
	private FlingGallery mGallery;
	
    private ImageFetcher mImageFetcher;
    
	private ImageGalleryAdapter imageAdapter;
	
    @Override
    public boolean onTouchEvent(MotionEvent event)
	{
    	return mGallery.onTouchEvent(event);
    }

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        final int longest = (height > width ? height : width) / 2;

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(this, 0.25f); // Set memory cache to 25% of mem class

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);
        mImageFetcher.setOnLoadImageListener(this);
        mGallery = new FlingGallery(this, displayMetrics);
        mGallery.setPaddingWidth(100);
        imageAdapter = new ImageGalleryAdapter(FlingGalleryActivity.this, mImageFetcher, ImageGrid.getImageList());
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
        setContentView(layout);
    }	
    
    @Override
	public void updateResolution(String path, int w, int h) {
    	mGallery.updateResolution(path, w, h);
	}
}
