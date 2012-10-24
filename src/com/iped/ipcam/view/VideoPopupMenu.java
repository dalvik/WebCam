package com.iped.ipcam.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.PopupWindow;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.utils.VideoPopupMenuAdaptor;

public class VideoPopupMenu extends PopupWindow implements OnItemClickListener {

	private LayoutInflater inflater = null;

	private GridView videoPopupMenu;

	private Handler handler;
	
	private VideoPopupMenuAdaptor adaptor;
	
	public VideoPopupMenu(Context conext, Handler handler, int[] imageId) {
		super(conext);
		this.handler = handler;
		inflater = LayoutInflater.from(conext);
		View vitureKeyBoard = inflater.inflate(R.layout.video_pop_window_layout, null);
		videoPopupMenu = (GridView) vitureKeyBoard.findViewById(R.id.video_popup_gridview);
		adaptor = new VideoPopupMenuAdaptor(conext, imageId);
		videoPopupMenu.setAdapter(adaptor);
		setContentView(vitureKeyBoard);
		setWidth(LayoutParams.WRAP_CONTENT);
		setHeight(LayoutParams.WRAP_CONTENT);
		this.setFocusable(true);//
		//videoPopupMenu.setOnItemLongClickListener(this);
		videoPopupMenu.setOnItemClickListener(this);

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if(position == 0) {
			Message msg = handler.obtainMessage();
			//msg.arg1 = adaptor.getItem(position);
			msg.what = 7001;
			handler.sendMessage(msg);
		}
		this.dismiss();
	}

	/*@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		handler.sendEmptyMessage(1);
		this.dismiss();
		return true;
	}*/
}
