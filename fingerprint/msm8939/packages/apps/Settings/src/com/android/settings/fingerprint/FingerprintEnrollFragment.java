package com.android.settings.fingerprint;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;
import android.graphics.Color;
import android.database.Cursor;
import android.content.ContentValues;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.Window;
import android.view.Gravity;

import java.lang.Thread;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.widget.LockPatternUtils;

import egistec.fingerauth.api.FPAuthListeners;
import egistec.fingerauth.api.FPAuthListeners.EnrollListener;
import egistec.fingerauth.api.FPAuthListeners.EnrollMapProgressListener;
import egistec.fingerauth.api.FPAuthListeners.StatusListener;
import egistec.fingerauth.api.SettingLib;

public class FingerprintEnrollFragment extends SettingsPreferenceFragment implements ViewFactory {
    private static final String TAG = "FingerprintEnrollFragment";

    private TextSwitcher mTvStatusTitle;
    private TextView mTvStatusHint;
    private ImageView mIvFPMap;
    private ImageView mIvFPIdentify;
    private TextView mTvProgress;
    private Button mBtnContinue;
    private Button mBtnRename;

    private WakeLock wakeLock ;
    private int mFingerIndex;
    private String mFingerPrintName;

    public static final int GETTED_GOOD_IMAGE = 1008;
    public static final int EXTRACTING_FEATURE = 1007;
    public static final int START_OPERATION = 3000;
    public static final int END_OPERATION = 3001;
    public static final int GETTED_BAD_IMAGE = 1009;
    public static final int GETTED_IMAGE_FAIL = 1022;
    public static final int GETTED_IMAGE_TOO_SHORT = 1015;
    public static final int FP_RES_FINGER_DETECTED = 1073;
    public static final int FP_RES_FINGER_REMOVED = 1074;
    public static final int FP_RES_FINGER_WAIT_FPON = 1075;
    public static final int FP_RES_REDUNDANT = 1094;
    public static final int FP_RES_ENROLL_OVERTIME = 1097; //long press
    public static final int STATUS_IMAGE_BAD = 5;
    public static final int STATUS_FEATURE_LOW = 6;
    public static final int STATUS_SWIPE_TOO_SHORT = 14;
    public static final int DEV_STATE_CHANGE = 2000;
    public static final int DEV_STATE_DISCONNECTED = 2001;
    public static final int DEV_STATE_CONNECTING = 2002;
    public static final int DEV_STATE_CONNECTED = 2003;
    public static final int DEV_EXTRA_PERMISSION_GRANTED = 2004;
    public static final int DEV_ACTION_USB_DEVICE_ATTACHED = 2005;
    public static final int DEV_ACTION_USB_DEVICE_DEATTACHED = 2006;
    public static final int DEV_STATE_NOT_FOUND = -2000;

    private static final int CONFIRM_EXISTING_REQUEST = 100;
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;

    public static final String SET_FOR_FP = "set_for_fp";
    private static final int MSG_PROGRESS = 0;
    private static final int MSG_ENROLL_OK = 1;
    private static final int MSG_ENROLL_FAIL = 2;
    private static final int MSG_INIT_UI_IDENTIFY = 3;
    private static final int MSG_INIT_UI_ENROLL = 4;
    private static final int MSG_START_TO_ENROLL = 5;
    private static final int MSG_ENROLL_PUT = 6;
    private static final int MSG_ENROLL_LEFT = 7;
    private static final int MSG_ENROLL_REDUNDANT = 8;
    private static final int MSG_ENROLL_PARTIAL_IMG = 9;
    private boolean isLeft = true;
    private boolean mProgressSuccess = false;
    private int mProgress;
    private int mTimeCount;

    private boolean mIsProgress80 = false;
    private int mPressSameLocationCount;

    private boolean mAlreadyIdentify = false;

    // use to check if the password is not set and user press back key to destroy this activity
    private boolean mNormal;

    private Context mContext;
    private SettingLib mLib;
    private LockPatternUtils mLockPatternUtils;

    private PowerManager powerManager;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mTvProgress.setVisibility(View.VISIBLE);
            mTvProgress.setText(String.valueOf(mProgress) + " %");

            switch (msg.what) {
            case MSG_PROGRESS:
                switch (mProgress) {
                case 0:
                    if (mProgress > 0) {
                        //mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_bg);
                    }
                    break;

                case 7:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_01);
                    break;

                case 14:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_02);
                    break;

                case 21:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_03);
                    break;

                case 28:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_04);
                    break;

                case 35:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_05);
                    break;

                case 42:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_06);
                    break;

                case 49:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_3));
                    mTvStatusHint.setTextColor(Color.parseColor("#18B4ED"));
                    mTvStatusHint.setTextSize(16);

                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_07);
                    break;

                case 56:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);

                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_08);
                    break;

                case 63:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_09);
                    break;

                case 70:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_10);
                    break;

                case 75:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_11);
                    break;

                case 80:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_12);
                    break;

                case 85:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_13);
                    break;

                case 90:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_14);
                    break;

                case 95:
                    mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                    mTvStatusHint.setTextColor(Color.BLACK);
                    mTvStatusHint.setTextSize(16);
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_15);
                    break;
                    
                case 100:
                    mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_16);
                    break;

                default:
                    break;
                }

                vibrateShort();
                break;

            case MSG_ENROLL_OK:
                mIvFPMap.setBackgroundResource(R.drawable.finger_print_enroll_progress_16);
                mTvProgress.setVisibility(View.GONE);
                mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_5));
                mTvStatusHint.setTextColor(Color.parseColor("#18B4ED"));
                mTvStatusHint.setTextSize(16);

                mTvStatusTitle.setText(mContext.getString(R.string.fingerprint_enroll_step3));
                mBtnContinue.setVisibility(View.VISIBLE);

                if (FingerPrintUtils.isPasswordQualityNone(mLockPatternUtils)) {
                    // no any fingeprint yet and the password has not been set
                    mBtnRename.setVisibility(View.GONE);
                    mBtnContinue.setText(R.string.fingerprint_enroll_finish_first);
                } else {
                    mBtnRename.setVisibility(View.VISIBLE);
                    mBtnContinue.setText(R.string.fingerprint_enroll_finish);
                }
                break;

            case MSG_ENROLL_FAIL:
                //rootView.setBackgroundColor(Color.parseColor("#FFC1C1"));
                mIvFPMap.setBackgroundResource(R.drawable.finger_print_enroll_progress_bg);
                mTvProgress.setVisibility(View.GONE);
                mTvStatusHint.setText(mContext.getString(R.string.fingerprint_enrolled_failed));
                mTvStatusHint.setTextColor(Color.RED);
                mTvStatusHint.setTextSize(25);
                break;

            case MSG_INIT_UI_IDENTIFY:
                mTvProgress.setVisibility(View.GONE);
                mTvProgress.setText("");

                mIvFPIdentify.setVisibility(View.VISIBLE);
                mIvFPMap.setVisibility(View.GONE);
                mIvFPMap.setImageResource(R.drawable.finger_print_enroll_progress_bg);

                mTvStatusHint.setTextColor(Color.BLACK);
                mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_1));
                mTvStatusHint.setTextSize(16);
                mTvStatusHint.setVisibility(View.VISIBLE);

                mTvStatusTitle.setText(mContext.getString(R.string.fingerprint_enroll_step1));
                break;

            case MSG_INIT_UI_ENROLL:
                mTvProgress.setVisibility(View.VISIBLE);

                mIvFPIdentify.setVisibility(View.GONE);
                mIvFPMap.setVisibility(View.VISIBLE);

                mTvStatusHint.setTextColor(Color.BLACK);
                mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_2));
                mTvStatusHint.setTextSize(16);
                mTvStatusHint.setVisibility(View.VISIBLE);

                mTvStatusTitle.setText(mContext.getString(R.string.fingerprint_enroll_step2));
                break;

            case MSG_ENROLL_REDUNDANT:
                mTvStatusHint.setTextColor(Color.BLACK);
                mTvStatusHint.setText(mContext.getString(R.string.fingerprint_prompt_6));
                mTvStatusHint.setTextSize(16);
                mTvStatusHint.setVisibility(View.VISIBLE);
                break;

            case MSG_ENROLL_PARTIAL_IMG:
                showBadImageDLG();
                break;

            case MSG_START_TO_ENROLL:
                this.sendEmptyMessage(MSG_INIT_UI_ENROLL);

                //mLib.abort();
                //sleep(100);
                mLib.enroll(FingerPrintSettings.FP_PRE + "_" + mFingerIndex);
                break;

            case MSG_ENROLL_PUT:
                if(isLeft && !mProgressSuccess) {
                    isLeft = false;
                    mTvStatusTitle.setText(mContext.getString(R.string.fingerprint_enroll_step4));
                }
                break;

            case MSG_ENROLL_LEFT:
                if(!mProgressSuccess) {
                    if(!isLeft) {
                        isLeft = true;
                        mTvStatusTitle.setText(mContext.getString(R.string.fingerprint_enroll_step2));
                    }
                }else {
                    mTvProgress.setVisibility(View.GONE);
                    mTvProgress.setText("");
                }
                break;

            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    private void showBadImageDLG() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        //builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(R.string.fingerprint_bad_image_title);
        builder.setMessage(R.string.fingerprint_bad_image_msg);
        builder.setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
              }
        });

        builder.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
    
        builder.create();

        AlertDialog dlg = builder.create();
        Window window = dlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        dlg.show();
    }

    private void sleep(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void checkPwd() {
        if (FingerPrintUtils.isPasswordQualityNone(mLockPatternUtils)) {
            logd("[checkPwd] no any pattern yet.");

            final Bundle extras = new Bundle();
            extras.putBoolean(SET_FOR_FP, true);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, extras);            
        } else {
            // if there is at least one fingeprint, open the feature
            mLockPatternUtils.saveLockFingerprint();
            mNormal = true;
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logd("[onActivityResult] requestCode : " + requestCode + ", resultCode : " + resultCode);
        if (requestCode == SET_OR_CHANGE_LOCK_METHOD_REQUEST) {
            if (resultCode == Activity.RESULT_CANCELED) {
                logd("[onActivityResult] pwd check failed. remove the pre-finger print.");
                deleteFPByIndex();
            } else {
                logd("[onActivityResult] pwd check ok.");

                // if there is at least one fingeprint, open the feature
                mLockPatternUtils.saveLockFingerprint();
            }

            mNormal = true;
            finish();
        }
    }

    @Override
    public View makeView() {
        // TODO Auto-generated method stub
        TextView t = new TextView(this.getActivity());
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setTextSize(25);
         return t;
    }
    public void deleteFPByIndex() {  //wujiacheng modified for Fingerprint 20150806 
        if (mLib.deleteFeature(FingerPrintSettings.FP_PRE + "_" + mFingerIndex)) {
            logd("[deleteFPByIndex] lib delete success! So delete it in the databases now.");
            FingerPrintUtils.deleteFingerPrintByIndex(mContext, mFingerIndex);
        } else {
            loge("[deleteFPByIndex] lib delete failed! This finger print still exists in the database!");
        }
    }

    private String getMsgStr(int msg) {
        String msgStr = "UNKNOWN";

        switch (msg) {
        case GETTED_GOOD_IMAGE:
            msgStr = "GETTED_GOOD_IMAGE";
            break;

        case EXTRACTING_FEATURE:
            msgStr = "EXTRACTING_FEATURE";
            break;

        case START_OPERATION:
            msgStr = "START_OPERATION";
            break;

        case END_OPERATION:
            msgStr = "END_OPERATION";
            break;

        case GETTED_BAD_IMAGE:
            msgStr = "GETTED_BAD_IMAGE";
            break;

        case GETTED_IMAGE_FAIL:
            msgStr = "GETTED_IMAGE_FAIL";
            break;

        case GETTED_IMAGE_TOO_SHORT:
            msgStr = "GETTED_IMAGE_TOO_SHORT";
            break;

        case FP_RES_FINGER_DETECTED:
            msgStr = "FP_RES_FINGER_DETECTED";
            break;

        case FP_RES_FINGER_REMOVED:
            msgStr = "FP_RES_FINGER_REMOVED";
            break;

        case FP_RES_FINGER_WAIT_FPON:
            msgStr = "FP_RES_FINGER_WAIT_FPON";
            break;

        case FP_RES_REDUNDANT:
            msgStr = "FP_RES_REDUNDANT";
            break;

        case FP_RES_ENROLL_OVERTIME:
            msgStr = "FP_RES_ENROLL_OVERTIME";
            break;

        case STATUS_IMAGE_BAD:
            msgStr = "STATUS_IMAGE_BAD";
            break;

        case STATUS_FEATURE_LOW:
            msgStr = "STATUS_FEATURE_LOW";
            break;

        case STATUS_SWIPE_TOO_SHORT:
            msgStr = "STATUS_SWIPE_TOO_SHORT";
            break;

        case DEV_STATE_CHANGE:
            msgStr = "DEV_STATE_CHANGE";
            break;

        case DEV_STATE_DISCONNECTED:
            msgStr = "DEV_STATE_DISCONNECTED";
            break;

        case DEV_STATE_CONNECTING:
            msgStr = "DEV_STATE_CONNECTING";
            break;

        case DEV_STATE_CONNECTED:
            msgStr = "DEV_STATE_CONNECTED";
            break;

        case DEV_EXTRA_PERMISSION_GRANTED:
            msgStr = "DEV_EXTRA_PERMISSION_GRANTED";
            break;

        case DEV_ACTION_USB_DEVICE_ATTACHED:
            msgStr = "DEV_ACTION_USB_DEVICE_ATTACHED";
            break;

        case DEV_ACTION_USB_DEVICE_DEATTACHED:
            msgStr = "DEV_ACTION_USB_DEVICE_DEATTACHED";
            break;

        case DEV_STATE_NOT_FOUND:
            msgStr = "DEV_STATE_NOT_FOUND";
            break;
        }

        return msgStr;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mIvFPMap = (ImageView) this.getActivity().findViewById(R.id.iv_fp_map);
        mIvFPIdentify = (ImageView) this.getActivity().findViewById(R.id.iv_fp_identify);
        mTvProgress = (TextView) this.getActivity().findViewById(R.id.tv_progress);
        mBtnContinue = (Button) this.getActivity().findViewById(R.id.btn_continue);
        mBtnContinue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkPwd();
            }
        });

        mBtnRename = (Button) this.getActivity().findViewById(R.id.btn_rename);
        mBtnRename.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(FingerPrintSettings.ENROLL_RESULT_INDEX, mFingerIndex);
                intent.putExtra(FingerPrintSettings.ENROLL_RESULT_NAME, mFingerPrintName);
                getActivity().setResult(FingerPrintSettings.ENROLL_RESULT, intent);
                mNormal = true;
                finish();
            }
        });

        mTvStatusTitle = (TextSwitcher) this.getActivity().findViewById(R.id.tv_status_title);
        mTvStatusTitle.setFactory(this);
        mTvStatusTitle.setInAnimation(this.getActivity(), R.anim.slide_in_right);
        mTvStatusTitle.setOutAnimation(this.getActivity(), R.anim.slide_out_left);
        mTvStatusHint = (TextView) this.getActivity().findViewById(R.id.tv_status_hint);

        mFingerIndex = findFreeFingerprintIndex();
        logd("[onViewCreated] mFingerIndex : " + mFingerIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.xml.fingerprint_enroll_activity, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getActivity().setTitle(R.string.fingerprint_settings_title);

        mContext = this.getActivity();

        mLockPatternUtils = new LockPatternUtils(this.getActivity());

        mLib = new SettingLib(mContext);

        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SET_FINGERPRINT_ENROLL_SHIELDHOME, 1);
    }

    @Override
    public void onResume() {
        super.onResume();    
        acquireWakeLock();

        bindAll();

        logd("[onResume] mAlreadyIdentify : " + mAlreadyIdentify);

        mHandler.sendEmptyMessage(MSG_INIT_UI_IDENTIFY);
    }

    private void bindAll() {
        initAllListener();

        logd("[bindAll]");
        mLib.bind();        
    }

    @Override
    public void onPause() {        
        super.onPause();

        releaseWakeLock();

        unBindLib();
        mProgress = 0; //wujiacheng add for bug73397 20150805
    }

    private void unBindLib(){
        if (mLib != null){
            mLib.abort();
            sleep(100);

            logd("[unBindLib] 1");

            mLib.disconnectDevice();
            logd("[unBindLib] 2");
            mLib.cleanListeners();
            mLib.unbind();
        }
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        //wujiacheng added for Fingerprint 20150806 
        Settings.System.putInt(this.getActivity().getContentResolver(), FingerprintEnrollFragment.SET_FOR_FP, 0);
	//wujiacheng added for Fingerprint 20150810
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SET_FINGERPRINT_ENROLL_SHIELDHOME, 0);
        logd("[onDestroy] mNormal : " + mNormal);
        if (!mNormal) {
            //deleteFPByIndex();
        }
        mLib = null;
    }

    private int findFreeFingerprintIndex() {
        int index = 1;
        /*wujiacheng add for bug70592 20150722 start*/
        int indexMax = 0;
        int[] keyArray = new int[5];
        for(int i=0; i<keyArray.length; i++) {
            keyArray[i] = 0;
        }
        /*wujiacheng add for bug70592 20150722 end*/
          
        Cursor cursor = mContext.getContentResolver().query(
                FingerPrintProvider.FINGER_PRINT_URI,
                FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION,
                null, null, FingerPrintProvider.ORDER_BY_INDEX_DESC);

        if (cursor == null) {
            logd("[findFreeFingerprintIndex] cursor is null! just return");
            return index;
        }

        /*wujiacheng add for bug70592 20150722 start*/
        try {
            while (cursor.moveToNext()) {
                int fingerprint_index = cursor.getInt(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX));
                keyArray[fingerprint_index-1] = fingerprint_index;
                indexMax = fingerprint_index > indexMax? fingerprint_index : indexMax;
            }
        } catch (Exception ex) {
            loge("[findFreeFingerprintIndex] exception! " + ex);
        } finally {
            cursor.close();
        }

        if(indexMax < 5) {
            index = indexMax + 1;
        }else {
            for(int i=0; i<keyArray.length; i++) {
                if(keyArray[i] == 0) {
                    keyArray[i] = i+1;
                    index = keyArray[i];
                    break;
                }
                 }
        }
        /*wujiacheng add for bug70592 20150722 end*/
          
        logd("[findFreeFingerprintIndex] cursor counts index:" + index);
        return index;
    }

    private void addFingerprint(int index) {
        mFingerPrintName = mContext.getString(R.string.fingerprint_title) + index;

        ContentValues values =  new ContentValues();
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME, mFingerPrintName);
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX, index);
        Uri uri = FingerPrintUtils.insertFingerPrint(mContext, values);
        logd("[addFingerprint] index [" + index + "] inserted, uri : " + uri);
    }

    private void showReadErrorDlg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(R.string.fingerprint_fp_read_error_title);
        builder.setMessage(R.string.fingerprint_fp_read_error_msg);
        builder.setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                  mLib.identify();
              }
        });

        AlertDialog dlg = builder.create();
        Window window = dlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        dlg.show();
    }

    private void showFPExistDlg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(R.string.fingerprint_already_exist_title);
        builder.setMessage(R.string.fingerprint_already_exist_msg);
        builder.setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                    /*wujiacheng modified for bug71649 20150727 start*/
                    dialog.dismiss();
                    //mLib.identify();
                    /*wujiacheng modified for bug71649 20150727 end*/
              }
        });
        /*wujiacheng add for bug71649 20150727 start*/
        builder.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                mLib.identify();
            }
        });
        /*wujiacheng add for bug71649 20150727 end*/        
        builder.create();

        AlertDialog dlg = builder.create();
        Window window = dlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        dlg.show();
    }

    private void initAllListener() {
        mLib.cleanListeners();

        // verify listener
        mLib.setVerifyListener(new FPAuthListeners.VerifyListener() {
            public void onSuccess() {
                logv("[VerifyListener] [onSuccess]  verify success!");
                mLib.abort();
                showFPExistDlg();
            }

            public void onFail() {
                loge("[VerifyListener] [onFail] verify failed.");
                mAlreadyIdentify = true;
                mLib.abort();
                mHandler.sendEmptyMessageDelayed(MSG_START_TO_ENROLL, 150);
            }
        });

        mLib.setStatusListener(new FPAuthListeners.StatusListener() {
            public void onBadImage(int status) {
                logd("[onBadImage] status : " + status);
                // mHandler.sendEmptyMessage(MSG_ENROLL_PARTIAL_IMG);
            }

            public void onFingerFetch() {
                logd("[onFingerFetch]");
            }

            public void onFingerImageGetted() {
                logd("[onFingerImageGetted]");
            }

            public void onServiceConnected() {
                logi("[onServiceConnected] start to identify...");
                mLib.connectDevice();
                mLib.identify();
            }

            public void onServiceDisConnected() {
                logd("[onServiceDisConnected]");
            }

            public void onStatus(int status) {
                logd("[onStatus] mAlreadyIdentify : " + mAlreadyIdentify + ", status = " + getMsgStr(status));
                if(status == FP_RES_ENROLL_OVERTIME){
                    //long press
                    mTimeCount++;
                    if(mTimeCount >= 30) {
                        mHandler.sendEmptyMessage(MSG_ENROLL_PUT);
                    }
                } else if(status == FP_RES_FINGER_REMOVED){
                    //finger move 
                    mTimeCount = 0;
                    mHandler.sendEmptyMessage(MSG_ENROLL_LEFT);
                } else if(status == FP_RES_REDUNDANT){ 
                    mHandler.sendEmptyMessage(MSG_ENROLL_REDUNDANT);
                } else if(status == FP_RES_FINGER_DETECTED){

                }
            }

            public void onUserAbort() {
                logd("[onUserAbort]");
            }
        });

        // enroll listener
        mLib.setEnrollListener(new FPAuthListeners.EnrollListener(){
            public void onFail() {
                loge("[EnrollListener] [onFail] enroll failed!");
                mHandler.sendEmptyMessage(MSG_ENROLL_FAIL);
            }

            public void onProgress() {
                logd("[EnrollListener] onProgress");
            }

            public void onSuccess() {
                logv("[EnrollListener] [onSuccess] enroll success! Try to check if the pwd has been set or not.");

                vibrateShort();
                mProgressSuccess = true;
                mHandler.sendEmptyMessage(MSG_ENROLL_OK);
                addFingerprint(mFingerIndex);
            }
        });

        // enroll progress listener
        mLib.setEnrollMapProgressListener(new FPAuthListeners.EnrollMapProgressListener() {
            public void onEnrollMapProgress(int progress){
                logd("[onEnrollMapProgress] progress : " + progress);
                mProgress = progress;
                mHandler.sendEmptyMessage(MSG_PROGRESS);
            }
        });
    }

    private void acquireWakeLock() {
        if (powerManager == null) {
            powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        }

        if (wakeLock != null) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

    private void vibrateShort() {
        Vibrator vb = (Vibrator)mContext.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null) {
            vb.vibrate(100);
        }
    }

    private static void logv(String strs) {
        Log.v(TAG, strs);
    }

    private static void logi(String strs) {
        Log.i(TAG, strs);
    }

    private static void logd(String strs) {
        Log.d(TAG, strs);
    }

    private static void loge(String strs) {
        Log.e(TAG, strs);
    }
}

