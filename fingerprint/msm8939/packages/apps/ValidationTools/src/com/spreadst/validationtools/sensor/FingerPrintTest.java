package com.spreadst.validationtools.sensor;

import com.spreadst.validationtools.R;
import com.spreadst.validationtools.BaseActivity;

import egistec.fingerauth.api.FPAuthListeners;
import egistec.fingerauth.api.SettingLib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.widget.ImageView;
import android.util.Log;



public class FingerPrintTest extends BaseActivity implements FPAuthListeners.StatusListener, FPAuthListeners.GetRawDataListener{
    private static final String TAG = "FingerPrintTest";

    private Context mContext = null;
    private static SettingLib mLib;
    
    private static boolean mbStart = false;

         private static boolean bConnect = false;
    private boolean  hasShowButton = false;

    private ImageView mFingerPrintBmp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
           super.showYesButton = false;    
        super.showNoButton = true;
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_in_line_base);        
        setTitle(R.string.fingerprint_test);
        mFingerPrintBmp = (ImageView)findViewById(R.id.tv_capture_image_show);
        mFingerPrintBmp.setVisibility(View.GONE);
        mbStart = false;        
        mContext = this;        
        mLib = new SettingLib(mContext);        
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if(!mbStart){            
            mbStart = true;      
            mLib.setStatusListener(this);
             mLib.setGetRawDataListener(this);        
              mLib.bind();
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(mbStart){            
            mLib.abort();    
            new Handler().postDelayed(new Runnable(){   
                            public void run() {
                                  
                            mLib.disconnectDevice();
                            mLib.setStatusListener(null);
                            mLib.setGetRawDataListener(null);    
                            mLib.unbind();
                            }   
                    }, 200);
            mbStart = false;
        }
    }
    
         private void doCaputerTest(){
                   mLib.removeCalibration();
                   mLib.disconnectDevice();                  
                   bConnect = mLib.connectDevice();
                   new Handler().postDelayed(new Runnable(){   
                            public void run() {
                                  if(bConnect){
                                      mLib.captureRawData();
                                 }        
                            }   
                    }, 3000);
                    
         }        


    @Override
    public void onBadImage(int status) {
        // TODO Auto-generated method stub
        Log.i(TAG, "Enter onBadImage!");
        
    }

    @Override
    public void onServiceConnected() {
        // TODO Auto-generated method stub        
        Log.i(TAG, "Enter onServiceConnected!");
        doCaputerTest();
    }

    @Override
    public void onServiceDisConnected() {
        // TODO Auto-generated method stub
        
        Log.i(TAG, "Enter onServiceDisConnected!");
    }

    @Override
    public void onFingerFetch() {
        // TODO Auto-generated method stub
        
        Log.i(TAG, "Enter onFingerFetch!");
        
    }

    @Override
    public void onFingerImageGetted() {
        // TODO Auto-generated method stub        
        if (mLib.NativeCheckImage())
        {
            Log.i(TAG, "Enter onFingerImageGetted!");
            if (!hasShowButton){
                 createButton(true);
                 hasShowButton = true;
            }                      
        }
    }

    @Override
    public void onUserAbort() {
        // TODO Auto-generated method stub
        
        Log.i(TAG, "Enter onUserAbort!");
        
    }

    @Override
    public void onStatus(int status) {
        // TODO Auto-generated method stub
        
        Log.i(TAG, "Enter onStatus status:"+status);
        
    }

    
    @Override
    public void onGetRawData(byte[] rawData, int width, int height) {
     // TODO Auto-generated method stub
     Log.i(TAG, "Enter onGetRawData width:"+width+" height:"+height);
      try {
        Bitmap FingerPrintBmp = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
        if (FingerPrintBmp != null) {       
           mFingerPrintBmp.setImageBitmap(FingerPrintBmp);
        }
     } catch (Exception e) {
         e.printStackTrace();
         }
        
    }
}
