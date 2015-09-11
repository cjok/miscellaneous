package com.android.settings.fingerprint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.UEventObserver;
import android.util.Log;

import egistec.fingerauth.api.SettingLib;
import egistec.fingerauth.api.FPAuthListeners.StatusListener;


public class AutoInterrpt
{
    private static final String TAG = "AutoInterrpt";
    
    private static String DEV_PATH = "DEVPATH=/devices/soc.0/78b7000.spi/spi_master/spi0/spi0.0/fp/esfp0";

    public static final int RES_OK = 0;
    public static final int RES_FAILED = -1;
    public static final int BINARY_FINISH = 0;
    public static final int CALCULATE_START = 1;

    public static final int TIME_DELAY_CHECKINT = 60;
    public static final int TIME_WAIT_UEVENT_DONE = 15;
    public static final int INTERRUPT_TRIGGERED_COUNT = 3;
    public static final int FINISH_DELAY = 300;
    
    public static final int NON_UI_MSG_START_DTVRT = 1;
    public static final int NON_UI_MSG_CALIBRATION_DTVRT = 2;
    public static final int NON_UI_MSG_FINISH_DTVRT = 3;
    public static final int NON_UI_MSG_RCV_UEVENT = 4;
    public static final int NON_UI_MSG_CALIBRATION_FINISH = 5;    
    
    public static final int NON_UI_MSG_FINISH_SUCESS = 6;
    public static final int NON_UI_MSG_FINISH_FAIL = 7;
    
    public static final int INT_DC_UPPER_BOUND = 0x3F;
    public static final int INT_DC_LOWER_BOUND = 0x36;
    
    public static final int INT_TH_MAX_BOUND = 0x3F;
    
    private final int m_dc_count = (INT_DC_UPPER_BOUND - INT_DC_LOWER_BOUND) + 1;    

    private int mThreshold = INT_TH_MAX_BOUND;
    private int mIntCount = 0;
    
    public GetTHDCValueListener autoListener;

    private Context mContext = null;
    
    private boolean mFound = false;
    private boolean minitial = true;
    private boolean misInterrupt = false;
    
    int mDCOffset = INT_DC_UPPER_BOUND;
    int mIndex = 0;
    int mthresholdTable[] = new int[m_dc_count];
    int mdcoffsetTable[] = new int[m_dc_count];
    int mcountTable[] = new int[m_dc_count];
    String mstrLogOut = "";

    int high = INT_TH_MAX_BOUND;
    int low = 0;
    int mid = (high + low) / 2;
    
    SettingLib mLib;
    
    private UEventObserver mET310Observer = new UEventObserver() {
        @Override
        public void onUEvent(UEvent event) {
            if (misInterrupt) {
                mIntCount++;
            }
        }
    };
    
    public interface GetTHDCValueListener
    {
        void getTHDCValue(int iDCvalue, int iTHvalue);

        void OnSuccess();

        void OnFail();
    }    
    

    int startDTVRT() {
        mFound = false;
        high = INT_TH_MAX_BOUND;
        low = 0;
        mid = (high + low) / 2;

        mIntCount = 0;
        mLib.setIntThreshold(mid);
        mIntCount = 0;
        misInterrupt = true;

        try {
            Thread.sleep(TIME_DELAY_CHECKINT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        calibrationDTVRT();
        return 0;
    }

    void calibrationDTVRT() {
        mLib.setIntThreshold(INT_TH_MAX_BOUND);
        misInterrupt = false;
        Message mesg = new Message();
        int ueventCount = mIntCount;
        if (mIntCount > 0 && mIntCount < INTERRUPT_TRIGGERED_COUNT)
            Log.d(TAG, " th:" + mid + "--- Int too less:" + ueventCount);

        if (mIntCount < INTERRUPT_TRIGGERED_COUNT) {
            high = mid;
            mid = (high + low) / 2;
        } else {
            mIntCount = 0;
            low = mid;
            mid = (high + low) / 2;
            mFound = true;
        }
        if (high - low <= 1) {
            if (mFound) {
                mThreshold = high;
                mIntCount = 0;
            } else {
                mLib.saveIntThreshold(INT_TH_MAX_BOUND);
                mThreshold = INT_TH_MAX_BOUND;
            }
            mesg.what = NON_UI_MSG_FINISH_DTVRT;
            mHandler.sendMessageDelayed(mesg, FINISH_DELAY);
            return;
        }

        mLib.setIntThreshold(mid);
        try {
            Thread.sleep(TIME_WAIT_UEVENT_DONE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mIntCount = 0;
        misInterrupt = true;
        try {
            Thread.sleep(TIME_DELAY_CHECKINT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        calibrationDTVRT();
    }

    int finishDTVRT() {
        misInterrupt = false;
        if (mFound) {
            Log.d(TAG, "finishDTVRT() : RESULT TH= " + mThreshold + " DC= "
                    + mDCOffset);
        } else {
            Log.d(TAG, "finishDTVRT() : NOT FOUND set threshold as "
                    + mThreshold);
        }
        mIntCount = 0;

        calculateDTVRT(BINARY_FINISH);

        return 0;
    }

    void calculateDTVRT(int isStart) {
        if (isStart != CALCULATE_START) {
            if (!mFound) {
                mThreshold = INT_TH_MAX_BOUND;
                mDCOffset = INT_DC_UPPER_BOUND;
                mLib.setIntThreshold(mThreshold);
                mLib.saveIntThreshold(mThreshold);
                mLib.setIntDCOffset(mDCOffset);
                mLib.saveIntDCOffset(mDCOffset);
                Log.d(TAG, "mfound false iStart = " + isStart
                        + " NOT FOUND !!!!!!!!!!!!!!!!!!!!! set threshold as "
                        + mThreshold + " dcoffset as " + mDCOffset);

                mLib.finishInterruptCalibration();
                finish(false);
                Log.d(TAG, "[Calculate !fount]END_INIT_CALLED");
                return;
            } else {
                mdcoffsetTable[mIndex] = mDCOffset;
                mthresholdTable[mIndex] = mThreshold;
            }
            mDCOffset--;
            mIndex++;
        }

        if ((INT_DC_UPPER_BOUND - mDCOffset) == (m_dc_count)) {
            int preTH = 0;
            int nowTH = 0;
            int nextTH = 0;
            for (int i = 0; i < m_dc_count - 1; i++) {
                if (i > 0)
                    preTH = mthresholdTable[i - 1];
                else
                    preTH = mthresholdTable[i];
                
                nowTH = mthresholdTable[i];
                nextTH = mthresholdTable[i + 1];

                if ((preTH == nextTH) && (preTH != nowTH)) {
                    mthresholdTable[i] = preTH;
                    Log.d(TAG, "Change TH:" + nowTH + " to" + preTH);
                }
            }

            int tempTH = 0;
            int maxCntTH = 0;
            int tempCnt = 0;
            int maxCnt = 0;

            for (int resultIndex = 0; resultIndex < m_dc_count; resultIndex++) {
                Log.d(TAG, " " + resultIndex + " "
                        + mdcoffsetTable[resultIndex] + " "
                        + mthresholdTable[resultIndex]);
            }
            
            // finding result
            for (int i = 0; i < m_dc_count - 1; i++) {
                if (mthresholdTable[i] == mthresholdTable[i + 1]) {
                    tempCnt++;
                    tempTH = mthresholdTable[i];
                    if (i == m_dc_count - 1)
                        tempCnt++;
                } else {
                    tempCnt = 0;
                }

                if (tempCnt >= maxCnt) {
                    maxCnt = tempCnt;
                    maxCntTH = tempTH;
                }
            }
            int baseDC = -1;
            for (int i = 0; i < m_dc_count; i++) {
                if (mthresholdTable[i] == maxCntTH) {
                    baseDC = mdcoffsetTable[i];
                }
            }
            mDCOffset =  baseDC + 2;//baseDC + 1;
            mThreshold = maxCntTH;

            if (baseDC == -1) // failed.
            {
                Log.d(TAG, "dcoffset = " + mDCOffset + " mIndex = " + mIndex);
                Log.d(TAG, "not found interrupt threshold");
                mThreshold = INT_TH_MAX_BOUND;
                mDCOffset = INT_DC_UPPER_BOUND;
                mLib.setIntThreshold(mThreshold);
                mLib.saveIntThreshold(mThreshold);
                mLib.setIntDCOffset(mDCOffset);
                mLib.saveIntDCOffset(mDCOffset);
                
                Log.d(TAG, "NOT FOUND !!!!!!!!!!!!!!!!!!!!! set threshold as "
                        + mThreshold + " dcoffset as " + mDCOffset);
                mLib.finishInterruptCalibration();
                finish(false);
                Log.d(TAG, "END_INIT_CALLED");
                return;
            }

            Log.d(TAG, "FINAL TH= " + mThreshold + " DC= " + mDCOffset);
            misInterrupt = false;
            mLib.saveIntThreshold(mThreshold);
            mLib.setIntDCOffset(mDCOffset);
            mLib.saveIntDCOffset(mDCOffset);
            mLib.finishInterruptCalibration();
            finish(true);
            return;
        } else {
            if (!minitial) {
                mLib.setIntDCOffset(mDCOffset);
                Message mesg = new Message();
                mesg.what = NON_UI_MSG_START_DTVRT;
                mHandler.sendMessageDelayed(mesg, 0);
                return;
            }
            minitial = false;
            int ret = mLib.startInterruptCalibration();
            if (ret == RES_OK) {
                mLib.setIntDCOffset(mDCOffset);
                Message mesg = new Message();
                mesg.what = NON_UI_MSG_START_DTVRT;
                mHandler.sendMessageDelayed(mesg, 0);
            }
        }
        Log.d(TAG, "calculateDTVRT 3");
    };
    
    private void finish(boolean ret)
    {
        mET310Observer.stopObserving();
        mLib.setPowerOffMode();
        
        Message msge = new Message();
        if (!ret){            
            msge.what = NON_UI_MSG_FINISH_FAIL;            
        } else {
            msge.what = NON_UI_MSG_FINISH_SUCESS;
        }

        if (mLib != null) {
            mLib.unbind();
            mLib = null;
        }

        mHandler.sendMessageDelayed(msge, 15);    
    }    
    
    @SuppressLint("HandlerLeak") 
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                
            case NON_UI_MSG_START_DTVRT: {
                startDTVRT();
            }
                break;
                
            case NON_UI_MSG_CALIBRATION_DTVRT: {
                calibrationDTVRT();
            }
                break;
                
            case NON_UI_MSG_FINISH_DTVRT: {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finishDTVRT();
                    }
                });
            }
                break;
                
            case NON_UI_MSG_FINISH_SUCESS:{
                autoListener.OnSuccess();
                autoListener.getTHDCValue(mThreshold, mDCOffset);
            }
                break;
                
            case NON_UI_MSG_FINISH_FAIL:{
                autoListener.OnFail();
            }
                break;

            default:
                throw new IllegalStateException("unhandled message: " + msg.what);
            }
        }
    };
    
    public AutoInterrpt(Context context)
    {
        mContext = context;    
    }
    
    public void GetAutoTHDCValue()
    {
        mET310Observer.startObserving(DEV_PATH);
        initLib();
    }    

    public void setGetTHDCValueListener(GetTHDCValueListener listener)
    {
        this.autoListener = listener;
    }

    private void initLib()
    {
        mLib = new SettingLib(mContext);
        mLib.setStatusListener(new StatusListener()
        {
            @Override
            public void onUserAbort()
            {
            }

            @Override
            public void onStatus(int status)
            {
            }

            @Override
            public void onServiceDisConnected()
            {
                // TODO Auto-generated method stub
                finish(false);
            }

            @Override
            public void onServiceConnected()
            {
                // TODO Auto-generated method stubs
                calculateDTVRT(CALCULATE_START);
            }

            @Override
            public void onFingerImageGetted()
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void onFingerFetch()
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void onBadImage(int arg0)
            {
                // TODO Auto-generated method stub
            }
        });

        mLib.bind();
    }

    private static void logi(String strs) {
        Log.i(TAG, strs);
    }

    private static void logd(String strs) {
        Log.d(TAG, strs);
    }

    private static void logw(String strs) {
        Log.w(TAG, strs);
    }

    private static void loge(String strs) {
        Log.e(TAG, strs);
    }
}
