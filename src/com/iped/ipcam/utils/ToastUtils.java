package com.iped.ipcam.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ToastUtils {

	public static void showToast(Context context, int id) {
		Toast.makeText(context, context.getText(id), Toast.LENGTH_SHORT).show();
	}
	
	  public static void setListViewHeightBasedOnChildren(ListView listView) {   
          ListAdapter listAdapter = listView.getAdapter();    
          if (listAdapter == null) {   
              return;   
          }   

          int totalHeight = 0;   
          int count = listAdapter.getCount();
           for (int i = 0; i < count; i++) {   
               View listItem = listAdapter.getView(i, null, listView);   
               listItem.measure(0, 0);   
               totalHeight += listItem.getMeasuredHeight();   
           }   
 
           ViewGroup.LayoutParams params = listView.getLayoutParams();   
           params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1)) + 25;   
           listView.setLayoutParams(params);   
       }   
}
