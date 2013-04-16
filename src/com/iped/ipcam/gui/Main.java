package com.iped.ipcam.gui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.utils.DeviceAdapter;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.view.PullToRefreshListView;
import com.iped.ipcam.view.PullToRefreshListView.OnRefreshListener;
import com.iped.ipcam.view.ScrollLayout;

public class Main extends Activity {

	private final static int device_auto_search = 0;
	private final static int device_manul_add = 1;
	private final static int device_param_set = 2;
	private final static int device_clear_all = 3;

	private int curImageCatalog = device_auto_search;
	private int lastSelected = 0;
	
	private boolean DEBUG = true;
	private String TAG = "Main";
	private ICamManager camManager = null;
	
	private ScrollLayout mScrollLayout;
	private RadioButton[] mButtons;
	private String[] mHeadTitles;
	private int mViewCount;
	private int mCurSel;

	
	private ImageView mHeadLogo;
	private TextView mHeadTitle;
	private ProgressBar mHeadProgress;

	// footer
	private RadioButton fbImage;
	private RadioButton fbVideo;

	// top buttons
	private Button frameAutoSearchButton;
	private Button frameManulAddButton;
	private Button frameParamSetButton;
	private Button frameClearAllButton;

	//handler
	private Handler deviceManagerHandler;
	private Handler playBackHandler;
	
	//local image contentvew
	private PullToRefreshListView listView;
	private DeviceAdapter adapter = null;
	private View deiceListViewFooter;
	private TextView deviceListViewFootMore;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);
		camManager = CamMagFactory.getCamManagerInstance();
		this.initHeadView();
		this.initFootBar();
		this.initPageScroll();
		this.initFrameButton();
		this.initFrameListView();
	}

	private void initHeadView() {
		mHeadLogo = (ImageView) findViewById(R.id.main_head_logo);
		mHeadTitle = (TextView) findViewById(R.id.main_head_title);
		mHeadProgress = (ProgressBar) findViewById(R.id.main_head_progress);
	}

	private void initFootBar() {
		fbImage = (RadioButton) findViewById(R.id.main_footbar_image);
		fbVideo = (RadioButton) findViewById(R.id.main_footbar_video);
	}

	private void initFrameButton() {
		frameAutoSearchButton = (Button) findViewById(R.id.frame_btn_local);
		frameManulAddButton = (Button) findViewById(R.id.frame_btn_beauty);
		frameParamSetButton = (Button) findViewById(R.id.frame_btn_scenery);
		frameClearAllButton = (Button) findViewById(R.id.frame_btn_other);

		frameAutoSearchButton.setOnClickListener(frameNewsBtnClick(frameAutoSearchButton,
				device_auto_search));
		frameManulAddButton.setOnClickListener(frameNewsBtnClick(
				frameManulAddButton, device_manul_add));
		frameParamSetButton.setOnClickListener(frameNewsBtnClick(
				frameParamSetButton, device_param_set));
		frameClearAllButton.setOnClickListener(frameNewsBtnClick(frameClearAllButton,
				device_clear_all));

		frameAutoSearchButton.setFocusable(true);
		frameAutoSearchButton.setEnabled(false);
	}

	private void initFrameListView() {
		// 初始化listview控件
		this.initDeviceListView();
		// 加载listview数据
		this.initFrameListViewData();
	}

	private void initPageScroll() {
		mScrollLayout = (ScrollLayout) findViewById(R.id.main_scrolllayout);
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.main_linearlayout_footer);
		mHeadTitles = getResources().getStringArray(R.array.head_titles);
		mViewCount = mScrollLayout.getChildCount();
		mButtons = new RadioButton[mViewCount];
		if (BuildConfig.DEBUG && DEBUG) {
			Log.d(TAG, "### mViewCount= " + mViewCount);
		}
		for (int i = 0; i < mViewCount; i++) {
			mButtons[i] = (RadioButton) linearLayout.getChildAt(i * 2);
			mButtons[i].setTag(i);
			mButtons[i].setChecked(false);
			mButtons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					int pos = (Integer) (v.getTag());
					if (mCurSel == pos) {
						switch (pos) {
						case 0:// image
							listView.clickRefresh();
							// beautyImageListView.clickRefresh();
							break;
						case 1:// video
							break;
						case 2:// settings
							break;
						default:
							break;
						}
					}
					setCurPoint(pos);
					mScrollLayout.snapToScreen(pos);
				}
			});
		}
		// 设置第一显示屏
		mCurSel = 0;
		mButtons[mCurSel].setEnabled(true);
		mButtons[mCurSel].setChecked(true);
		/*mScrollLayout.SetOnViewChangeListener(new ScrollLayout.OnViewChangeListener() {
					public void OnViewChange(int viewIndex) {
						setCurPoint(viewIndex);
					}
				});*/
	}

	// init local image listview
	private void initDeviceListView() {
		adapter = new DeviceAdapter(camManager.getCamList(), this);
		camManager.getCamList().addAll(FileUtil.fetchDeviceFromFile(this));
		listView = (PullToRefreshListView) findViewById(R.id.frame_device_list_view);
		deiceListViewFooter = getLayoutInflater().inflate(R.layout.layout_list_view_footer, null);
		deviceListViewFootMore = (TextView) deiceListViewFooter.findViewById(R.id.list_view_foot_more);
		listView.addFooterView(deiceListViewFooter);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int index, long id) {
				if(index == 0) {
					return true;
				}
				listView.requestFocusFromTouch();
				lastSelected = index;
				listView.setSelection(index);
				camManager.setSelectInde(index-1);
				adapter.setChecked(index-1);
				adapter.notifyDataSetChanged();	
				return false;
			}
		});
		listView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				listView.onScrollStateChanged(view, scrollState);
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				listView.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				if(camManager.getCamList().size() <=0) {
					listView.onScrollStateChanged(view, SCROLL_STATE_TOUCH_SCROLL );
				}
			}
		});
		listView.setOnRefreshListner(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				camManager.startThread(deviceManagerHandler);
			}
		});
		if(camManager.getCamList().size() >0) {
			deiceListViewFooter.setVisibility(View.GONE);
		} 
		
	}

	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int index,
				long arg3) {
			if(index == 0) {
				return;
			}
			listView.requestFocusFromTouch();
			lastSelected = index;
			listView.setSelection(index);
			camManager.setSelectInde(index-1);
			adapter.setChecked(index-1);
			adapter.notifyDataSetChanged();	
		}
	};
	
	private View.OnClickListener frameNewsBtnClick(final Button btn,
			final int catalog) {
		return new View.OnClickListener() {
			public void onClick(View v) {
				if (btn == frameAutoSearchButton) {
					frameAutoSearchButton.setEnabled(false);
				} else {
					frameAutoSearchButton.setEnabled(true);
				}
				if (btn == frameManulAddButton) {
					frameManulAddButton.setEnabled(false);
				} else {
					frameManulAddButton.setEnabled(true);
				}
				if (btn == frameParamSetButton) {
					frameParamSetButton.setEnabled(false);
				} else {
					frameParamSetButton.setEnabled(true);
				}
				if (btn == frameClearAllButton) {
					frameClearAllButton.setEnabled(false);
				} else {
					frameClearAllButton.setEnabled(true);
				}
				curImageCatalog = catalog;
				if (btn == frameAutoSearchButton) {
				} else if (btn == frameManulAddButton) {
				} else if (btn == frameParamSetButton) {
				} else if (btn == frameClearAllButton) {
				}
			}
		};
	}
	
    private void setCurPoint(int index) {
    	if (index < 0 || index > mViewCount - 1 || mCurSel == index)
    		return;
    	mButtons[mCurSel].setChecked(false);
    	mButtons[index].setChecked(true);    
    	mHeadTitle.setText(mHeadTitles[index]);    	
    	mCurSel = index;
    	if(index == 0){
    		mHeadLogo.setImageResource(R.drawable.frame_logo_image);
    	} else if(index == 1){
    		mHeadLogo.setImageResource(R.drawable.frame_logo_video);
    	}
    }
    
    private void initFrameListViewData() {
   	   //初始化Handler
    	deviceManagerHandler = this.getListViewHandler(listView, adapter, deviceListViewFootMore, null, 100);
      /* playBackHandler = this.getListViewHandler(beautyImageListView, beautyImageListViewAdapter, beautyImageListViewFootMore, beautyImageListViewFootProgress, AppContext.PAGE_SIZE);
      */
   }
    
    private Handler getListViewHandler(final PullToRefreshListView imageListView,final BaseAdapter adapter,final TextView more,final ProgressBar progress, final int pageSize){
    	return new Handler(){
    		@Override
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			/*if(msg.what>0) {
    				handleImageListData(msg.what, msg.obj, msg.arg2, msg.arg1);
    				if(msg.what<pageSize) {
    					imageListView.setTag(UIHelper.LISTVIEW_DATA_FULL);
    					adapter.notifyDataSetChanged();
    					more.setText(R.string.load_full);
    				}else if(msg.what == pageSize) {
    					imageListView.setTag(UIHelper.LISTVIEW_DATA_MORE);
    					adapter.notifyDataSetChanged();
    					more.setText(R.string.load_more);
    				}
    			}else if(msg.what == -1) {
    				imageListView.setTag(UIHelper.LISTVIEW_DATA_MORE);
    				more.setText(R.string.load_error);
    				
    			}
    			if(adapter.getCount() ==0) {
    				imageListView.setTag(UIHelper.LISTVIEW_DATA_EMPTY);
    				more.setText(R.string.load_empty);
    			}
    			progress.setVisibility(ProgressBar.GONE);
    			mHeadProgress.setVisibility(ProgressBar.GONE);
    			if(msg.arg1 == UIHelper.LISTVIEW_ACTION_REFRESH) {

    			}else if(msg.arg1 == UIHelper.LISTVIEW_ACTION_CHANGE_CATALOG) {
    				imageListView.onRefreshComplete();
    				imageListView.setSelection(0);
    			}*/
    		}
    	};
    }

}
