package com.iped.ipcam.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;

public class LazyScrollView extends ScrollView implements OnTouchListener {

	private View view;
	
	private Handler handler;
	
	private OnScrollListener onScrollListener;
	
	public LazyScrollView(Context context) {
		super(context);
	}

	public LazyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	
	public LazyScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected int computeVerticalScrollRange() {
		return super.computeHorizontalScrollRange();
	}
	
	@Override
	protected int computeVerticalScrollOffset() {
		return super.computeVerticalScrollOffset();
	}
	
	/**
     * 获得参考的View，主要是为了获得它的MeasuredHeight，然后和滚动条的ScrollY+getHeight作比较。
     */
    public void getView(){
    	this.view=getChildAt(0);
    	if(view!=null){
    		init();
    	}
    }
    
    private void init(){
    	this.setOnTouchListener(this);
    	handler=new Handler(){
    		@Override
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			switch(msg.what) {
    			case 1:
					if(view.getMeasuredHeight() <= getScrollY() + getHeight()) {
						if(onScrollListener!=null){
							onScrollListener.onBottom();
						}
						
					}else if(getScrollY()==0){
						if(onScrollListener!=null){
							onScrollListener.onTop();
						}
					}else{
						if(onScrollListener!=null){
							onScrollListener.onScroll();
						}
					}
					break;
				default:
					break;
    			}
    		}
    	};
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	switch(event.getAction()){
    	case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_UP:
			if(view!=null && onScrollListener!=null){
				handler.sendMessageDelayed(handler.obtainMessage(1), 150);
			}
			break;
		default:
			break;
    	}
    	return false;
    }
    
	 public interface OnScrollListener{
	   	void onBottom();
	  	void onTop();
	   	void onScroll();
	 }
	 
	 public void setOnScrollListener(OnScrollListener onScrollListener){
	   	this.onScrollListener=onScrollListener;
	 }
		
}
