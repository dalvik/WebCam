/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iped.ipcam.bitmapfun;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.iped.ipcam.gui.R;

/**
 * This fragment will populate the children of the ViewPager from {@link ImageDetail}.
 */
public class ImageDetailFragment extends Fragment {
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private String mImageUrl;
    private ImageView mImageView;
    private ImageFetcher mImageFetcher;

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    DisplayMetrics dm;
    Bitmap bitmap;

    float minScaleR;// 最小缩放比例
    float curScaleR = 0;
    static float MAX_SCALE = 4f;// 最大缩放比例

    static final int NONE = 0;// 初始状态
    static final int DRAG = 1;// 拖动
    static final int ZOOM = 2;// 缩放
    int mode = NONE;

    PointF prev = new PointF();
    PointF mid = new PointF();
    float dist = 1f;
    
    
    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl) {
        final ImageDetailFragment f = new ImageDetailFragment();
        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        //mImageView.setOnTouchListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageDetailActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((ImageDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(mImageUrl, mImageView);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }

    /*@Override
    public boolean onTouch(View v, MotionEvent event) {
    	 switch (event.getAction() & MotionEvent.ACTION_MASK) {
         case MotionEvent.ACTION_DOWN:
             savedMatrix.set(matrix);
             prev.set(event.getX(), event.getY());
             dm = new DisplayMetrics();
             final DisplayMetrics displayMetrics = new DisplayMetrics();
             bitmap = mImageFetcher.getBitmap(mImageUrl);
             System.out.println("bitmap = " + bitmap);
             //getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
             minZoom();
             center();
             mImageView.setImageMatrix(matrix);
             mode = DRAG;
             break;
         case MotionEvent.ACTION_POINTER_DOWN:
             dist = spacing(event);
             if (spacing(event) > 10f) {
                 savedMatrix.set(matrix);
                 midPoint(mid, event);
                 mode = ZOOM;
             }
             break;
         case MotionEvent.ACTION_UP:
         case MotionEvent.ACTION_POINTER_UP:
     		 Matrix m = new Matrix();
             m.set(matrix);
             RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
             m.mapRect(rectF);
             mode = NONE;
             break;
         case MotionEvent.ACTION_MOVE:
             if (mode == DRAG) {
                 matrix.set(savedMatrix);
                 float dx = (event.getX() - prev.x);
                 float dy = event.getY() - prev.y;
                 matrix.postTranslate(dx, dy);
             } else if (mode == ZOOM) {
                 float newDist = spacing(event);
                 if (newDist > 10f) {
                     matrix.set(savedMatrix);
                     float tScale = newDist / dist;
                     matrix.postScale(tScale, tScale, mid.x, mid.y);
                 }
             }
             break;
         }
         mImageView.setImageMatrix(matrix);
         CheckView();
         return false;
    }*/
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }
    
    private void minZoom() {
        minScaleR = Math.max((float) dm.widthPixels / (float) bitmap.getWidth(), (float) dm.heightPixels / (float) bitmap.getHeight());
        curScaleR = minScaleR;
        MAX_SCALE = Math.max((float) bitmap.getWidth() / (float) dm.widthPixels, (float) bitmap.getHeight() / (float) dm.heightPixels);;
        if (minScaleR < 1.0) {
            matrix.postScale(minScaleR, minScaleR);
        }
    }

    private void center() {
        center(true, true);
    }

    protected void center(boolean horizontal, boolean vertical) {

        Matrix m = new Matrix();
        m.set(matrix);
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        m.mapRect(rect);
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0, deltaY = 0;

        if (vertical) {
            int screenHeight = dm.heightPixels;
            if (height <= screenHeight) {
                deltaY = (screenHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < screenHeight) {
                deltaY = screenHeight - rect.bottom;
            }
        }

        if (horizontal) {
            int screenWidth = dm.widthPixels;
            if (width <= screenWidth) {
                deltaX = (screenWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < screenWidth) {
                deltaX = screenWidth - rect.right;
            }
        }
        matrix.postTranslate(deltaX, deltaY);
    }

    private void CheckView() {
        float p[] = new float[9];
        matrix.getValues(p);
        curScaleR = p[0];
        if (mode == ZOOM) {
            if (curScaleR < minScaleR) {
                matrix.setScale(minScaleR, minScaleR);
            }
            if (curScaleR > MAX_SCALE) {
                matrix.set(savedMatrix);
            }
        }
        center();
    }
     
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
