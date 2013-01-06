package com.iped.ipcam.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iped.ipcam.gui.R;

public class PullToRefreshListView extends ListView implements OnScrollListener {

    // 下拉刷新标志   
    private final static int PULL_To_REFRESH = 0; 
    
    // 松开刷新标志   
    private final static int RELEASE_To_REFRESH = 1;
    
    // 正在刷新标志   
    private final static int REFRESHING = 2;
    
    // 刷新完成标志   
    private final static int REFRESH_DONE = 3;  
  
    private LayoutInflater inflater;
    
    private LinearLayout headView;  
    private TextView tipsTextview;  
    private TextView lastUpdatedTextView;  
    private ImageView arrowImageView;  
    private ProgressBar progressBar;  
    // 用来设置箭头图标动画效果   
    private RotateAnimation animation;  
    private RotateAnimation reverseAnimation;  
    
    // 用于保证startY的值在一个完整的touch事件中只被记录一次   
    private boolean isRecored;  
  
    private int headContentWidth;  
    private int headContentHeight;  
    private int headContentOriginalTopPadding;
    
    private int startY;  
    private int firstItemIndex;  
    private int currentScrollState;
    
	private int state;  
	   
	private boolean isBack;  
	   
    public OnRefreshListener refreshListener;  
    
	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {   
		 animation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		 animation.setInterpolator(new LinearInterpolator());
		 animation.setDuration(100);
		 animation.setFillAfter(true);
		 
		 reverseAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);  
	     reverseAnimation.setInterpolator(new LinearInterpolator());  
	     reverseAnimation.setDuration(100);  
	     reverseAnimation.setFillAfter(true);  
	        
		 inflater = LayoutInflater.from(context);
		 headView = (LinearLayout) inflater.inflate(R.layout.layout_pull_to_refresh_head, null);
		 arrowImageView = (ImageView) headView.findViewById(R.id.head_arrowImageView);
		 arrowImageView.setMaxWidth(50);
		 arrowImageView.setMaxHeight(50);
		 progressBar = (ProgressBar) headView.findViewById(R.id.head_progressBar);
		 tipsTextview = (TextView) headView.findViewById(R.id.head_tipsTextView);
		 lastUpdatedTextView = (TextView) headView.findViewById(R.id.head_lastUpdatedTextView);
		 headContentOriginalTopPadding = headView.getPaddingTop();  
		 measureView(headView);
		 headContentHeight = headView.getMeasuredHeight();  
	     headContentWidth = headView.getMeasuredWidth(); 
	     headView.setPadding(headView.getPaddingLeft(), -1 * headContentHeight, headView.getPaddingRight(), headView.getPaddingBottom());  
	     headView.invalidate();  
	     addHeaderView(headView);        
	     setOnScrollListener(this); 
	}
	
	// 计算headView的width及height值  
    private void measureView(View child) {  
    	ViewGroup.LayoutParams vl = child.getLayoutParams();
    	if(vl == null) {
    		vl = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    	}
    	int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0+0, vl.width);
    	int lpHeight = vl.height;
    	int childHeightSpec;
    	if(lpHeight >0) {
    		childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
    	} else {
    		childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    	}
    	child.measure(childWidthSpec, childHeightSpec);
    }
    
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		currentScrollState  = scrollState;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		firstItemIndex = firstVisibleItem;
	}
	
	public void clickRefresh() {
		setSelection(0);
		state = REFRESHING;  
		changeHeaderViewByState();
		onRefresh(); 
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch(ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if(firstItemIndex == 0 && !isRecored) {// 只有在列表的第一项时才允许下拉
				startY = (int) ev.getY();
				isRecored = true;
			}
			break;
		case MotionEvent.ACTION_CANCEL://失去焦点&取消动作
		case MotionEvent.ACTION_UP:
			if(state != REFRESHING) {
				if(state == REFRESH_DONE){
					
				}else if(state == PULL_To_REFRESH) {
					state = REFRESH_DONE;
					changeHeaderViewByState();
				}else if(state == RELEASE_To_REFRESH) {
					state = REFRESHING;
					changeHeaderViewByState();
					onRefresh();
				}
			}
			isRecored = false;
			isBack = false;
			break;
		case MotionEvent.ACTION_MOVE:
			int tempY = (int) ev.getY();
			if(!isRecored && firstItemIndex == 0) {
				isRecored = true;
				startY = tempY;
			}
			if(state != REFRESHING && isRecored) {
				// 可以松开刷新了   
				if(state == RELEASE_To_REFRESH) {
					// 往上推，推到屏幕足够掩盖head的程度，但还没有全部掩盖  
					if((tempY - startY <headContentHeight + 20) && (tempY - startY) >0) {
						state = PULL_To_REFRESH;
						changeHeaderViewByState();     
					}else if(tempY - startY <=0){// 一下子推到顶  
						state = REFRESH_DONE;
						changeHeaderViewByState();
					}// 往下拉，或者还没有上推到屏幕顶部掩盖head   
                    else {  
                        // 不用进行特别的操作，只用更新paddingTop的值就行了   
                    } 
				}else if( state == PULL_To_REFRESH) {
					// 下拉到可以进入RELEASE_TO_REFRESH的状态   
					if(tempY - startY >= headContentHeight + 20 && currentScrollState == SCROLL_STATE_TOUCH_SCROLL){
						state = RELEASE_To_REFRESH;
						isBack = true;
						changeHeaderViewByState();
					}else if(tempY - startY <=0 ) {	// 上推到顶了
						state = REFRESH_DONE;
						changeHeaderViewByState();
					}
				}else if(state == REFRESH_DONE) { // done状态下   
					if(tempY - startY > 0) {
						state = PULL_To_REFRESH;
						changeHeaderViewByState();
					}
				}
				
				if(state== PULL_To_REFRESH) {// 更新headView的size   
					int topPadding = (int) ((-1 * headContentHeight + (tempY - startY)));
					headView.setPadding(headView.getPaddingLeft(), topPadding, headView.getPaddingRight(), headView.getPaddingBottom());
					headView.invalidate();
				}
				if(state == RELEASE_To_REFRESH) {
					int topPadding = (int) ((tempY - startY - headContentHeight));
					headView.setPadding(headView.getPaddingLeft(), topPadding, headView.getPaddingRight(), headView.getPaddingBottom());
					headView.invalidate();
				}
			}
			break;
		}
		return super.onTouchEvent(ev);
	}
  
	private void changeHeaderViewByState() {  
		 switch(state) {
			case RELEASE_To_REFRESH:
				arrowImageView.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
				
				arrowImageView.clearAnimation();
				arrowImageView.startAnimation(animation);
				tipsTextview.setText(R.string.pull_to_refresh_release_label);
				break;
			case PULL_To_REFRESH:
				progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.VISIBLE);
				if(isBack) {
					isBack = false;
					arrowImageView.clearAnimation();
					arrowImageView.startAnimation(reverseAnimation);
				}
				tipsTextview.setText(R.string.pull_to_refresh_pull_label);
				break;
			case REFRESHING:
				headView.setPadding(headView.getPaddingLeft(), headContentOriginalTopPadding, headView.getPaddingRight(), headView.getPaddingBottom());
				headView.invalidate();
				progressBar.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.GONE);
				tipsTextview.setText(R.string.pull_to_refresh_refreshing_label);
				lastUpdatedTextView.setVisibility(View.GONE);
				break;
			case REFRESH_DONE:
				headView.setPadding(headView.getPaddingLeft(), -1 * headContentHeight, headView.getPaddingRight(), headView.getPaddingBottom());
				headView.invalidate();
				progressBar.setVisibility(View.GONE);
				arrowImageView.clearAnimation();
				arrowImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow);
				tipsTextview.setText(R.string.pull_to_refresh_pull_label);  
	            lastUpdatedTextView.setVisibility(View.VISIBLE);  
				break;
			}
	 }
	
	
	public void onRefreshComplete(String update) {
		lastUpdatedTextView.setText(update);
		onRefreshComplete();
	}
	
	public void onRefreshComplete() {
		state  = REFRESH_DONE;
		changeHeaderViewByState();
	}
	
	public void onRefresh() {
		if(refreshListener != null) {
			refreshListener.onRefresh();
		}
	}
	
	public void setOnRefreshListner(OnRefreshListener refreshListener) {
		this.refreshListener = refreshListener;
	}
	
	public interface OnRefreshListener {
		public void onRefresh();
	}
}
