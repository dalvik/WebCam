package com.iped.ipcam.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.Scroller;

public class ScrollLayout extends ViewGroup {

	private static final String TAG = "ScrollLayout";
	private Scroller mScroller;

	private OnViewChangeListener mOnViewChangeListener;

	private int mCurScreen;
	
	private int mDefaultScreen = 0;
	
	private int mTouchSlop;
	
	private boolean isScroll = true;

	public void setIsScroll(boolean b) {
		this.isScroll = b;
	}

	public ScrollLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ScrollLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void init(Context context) {
		mScroller = new Scroller(context);
		mCurScreen = mDefaultScreen;
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0, childLeft + childWidth,
						childView.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.e(TAG, "onMeasure");
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException("ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException("ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		// Log.e(TAG, "moving to screen "+mCurScreen);
		scrollTo(mCurScreen * width, 0);
	}
	
	public void snapToScreen(int whichScreen) {
		// ÊÇ·ñ¿É»¬¶¯
		if (!isScroll) {
			this.setToScreen(whichScreen);
			return;
		}
		scrollToScreen(whichScreen);
	}
	

	public void scrollToScreen(int whichScreen) {		
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		if (getScrollX() != (whichScreen * getWidth())) {
			final int delta = whichScreen * getWidth() - getScrollX();
			mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 1);//³ÖÐø¹ö¶¯Ê±¼ä ÒÔºÁÃëÎªµ¥Î»
			mCurScreen = whichScreen;
			invalidate(); // Redraw the layout
			if (mOnViewChangeListener != null) {
            	mOnViewChangeListener.OnViewChange(mCurScreen);
            }
		}
	}
	
	public void setToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		mCurScreen = whichScreen;
		scrollTo(whichScreen * getWidth(), 0);
		
        if (mOnViewChangeListener != null)
        {
        	mOnViewChangeListener.OnViewChange(mCurScreen);
        }
	}

	/**
	 * ÉèÖÃÆÁÄ»ÇÐ»»¼àÌýÆ÷
	 * 
	 * @param listener
	 */
	public void SetOnViewChangeListener(OnViewChangeListener listener) {
		mOnViewChangeListener = listener;
	}

	/**
	 * ÆÁÄ»ÇÐ»»¼àÌýÆ÷
	 * 
	 * @author liux
	 */
	public interface OnViewChangeListener {
		public void OnViewChange(int view);
	}
}
