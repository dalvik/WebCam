package com.iped.ipcam.gui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.iped.ipcam.gui.Panel.PanelClosedEvent;
import com.iped.ipcam.gui.Panel.PanelOpenedEvent;

public class LeftVideoView extends Activity {  
    public Panel panel;  
    public LinearLayout container;  
    public ImageView gridview;  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.left_view);  
        gridview = (ImageView) findViewById(R.id.gridview);  
        container=(LinearLayout)findViewById(R.id.container);  
        panel=new Panel(this,gridview,230,LayoutParams.FILL_PARENT);  
        container.addView(panel);//¼ÓÈëPanel¿Ø¼þ   
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
        
        panel.fillPanelContainer(view);
        panel.setPanelClosedEvent(panelClosedEvent);  
        panel.setPanelOpenedEvent(panelOpenedEvent);  
          
        gridview.setBackgroundResource(R.drawable.shutdown_bg);
    }  
  
    PanelClosedEvent panelClosedEvent =new PanelClosedEvent(){  
  
        @Override  
        public void onPanelClosed(View panel) {  
            Log.e("panelClosedEvent","panelClosedEvent");  
        }  
          
    };  
      
    PanelOpenedEvent panelOpenedEvent =new PanelOpenedEvent(){  
  
        @Override  
        public void onPanelOpened(View panel) {  
            Log.e("panelOpenedEvent","panelOpenedEvent");  
        }  
          
    };  
}