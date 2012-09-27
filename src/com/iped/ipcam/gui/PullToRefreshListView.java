package com.iped.ipcam.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.Scroller;

public class PullToRefreshListView extends ListView implements OnScrollListener {

	private Scroller scroller;
	
	// 下拉刷新标志
	private final static int PULL_To_REFRESH = 0;
	
	// 松开刷新标志
	private final static int RELEASE_To_REFRESH = 1;
	
	// 正在刷新标志
	private final static int REFRESHING = 2;
	
	// 刷新完成标志
	private final static int DONE = 3;

	private int startY;  
	
	private int firstItemIndex;  
	    
	private int currentScrollState;
	  
	private int state;  
	 
	//用于保证startY的值在一个完整的touch事件中只被记录一次   
	private boolean isRecored;  
	    
	private String TAG = "PullToRefreshListView";

	public PullToRefreshListView(Context context) {
		super(context);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		scroller = new Scroller(context);
		setOnScrollListener(this);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if(firstItemIndex == 0 && !isRecored) {
				startY = (int) ev.getY();
				isRecored = true;
			}
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if(state != REFRESHING) {
				if(state == PULL_To_REFRESH) {
					state = DONE;
					//TODO
				} else if(state == RELEASE_To_REFRESH) {
					state = REFRESHING;
					//TODO
					
				}
			}
			isRecored = false;
			break;
		case MotionEvent.ACTION_MOVE:
			int tempY = (int) ev.getY();
			if(!isRecored && firstItemIndex == 0) {
				isRecored = true;
				startY = tempY;
			}
			if(state != REFRESHING && isRecored) {
				if(state == RELEASE_To_REFRESH) {
					if((tempY - startY < WebTabWidget.tabHeight + 20) && (tempY - startY) > 0) {
						state = PULL_To_REFRESH;
						//TODO
						
					}
				} else if(tempY - startY <= 0) {
					state = DONE;
					//TODO
				} else {
					
				}
			} else if(state == PULL_To_REFRESH) {
				if(tempY - startY > WebTabWidget.tabHeight + 20 && currentScrollState == SCROLL_STATE_TOUCH_SCROLL) {
					state = RELEASE_To_REFRESH;
					//TODO
				} else if(tempY - startY <=0) {
					state = DONE;
					//TODO
					
				}
			}else if(state == DONE) {
				if(tempY - startY > 0) {
					state = PULL_To_REFRESH;
					//TODO
				}
			}
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		currentScrollState = scrollState;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		firstItemIndex = firstVisibleItem;
	}

}
